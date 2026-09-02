package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.*;
import com.laptopstore.laptopstore.entity.*;
import com.laptopstore.laptopstore.enums.OrderStatus;
import com.laptopstore.laptopstore.enums.PaymentMethod;
import com.laptopstore.laptopstore.enums.PaymentStatus;
import com.laptopstore.laptopstore.enums.StockLogType;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private VoucherRepository voucherRepository;
    @Autowired private VoucherUsageRepository voucherUsageRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ProductVariantRepository productVariantRepository;
    @Autowired private StockLogService stockLogService;
    @Autowired private OrderStatusHistoryRepository orderStatusHistoryRepository;


    @Value("${order.return-window-days:7}")
    private int returnWindowDays = 7;

    public int getReturnWindowDays() {
        return returnWindowDays;
    }

    @Transactional
    public void updateStatus(Long id, OrderStatus newStatus) {
        orderRepository.findById(id).ifPresent(order -> {
            OrderStatus oldStatus = order.getOrderStatus();
            order.setOrderStatus(newStatus);
            orderRepository.save(order);

            OrderStatusHistory history = new OrderStatusHistory();
            history.setOrder(order);
            history.setOldStatus(oldStatus != null ? oldStatus.name() : null);
            history.setStatus(newStatus);
            history.setChangedAt(LocalDateTime.now());
            history.setNote("Cập nhật trạng thái sang " + newStatus.getDisplayName());
            history.setUpdatedBy("System/Admin");
            orderStatusHistoryRepository.save(history);
        });
    }

    public boolean isOrderEligibleForReturn(Order order) {
        if (order == null || !OrderStatus.DELIVERED.equals(order.getOrderStatus())) {
            return false;
        }

        LocalDateTime deliveredAt = orderStatusHistoryRepository
                .findFirstByOrderIdAndNewStatusOrderByCreatedAtDesc(order.getId(), OrderStatus.DELIVERED.name())
                .map(OrderStatusHistory::getCreatedAt)
                .orElse(order.getCreatedAt());

        if (deliveredAt == null) {
            return false;
        }

        return !deliveredAt.plusDays(returnWindowDays).isBefore(LocalDateTime.now());
    }

    @Transactional
    public void requestReturnOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        if (!OrderStatus.DELIVERED.equals(order.getOrderStatus())) {
            throw new RuntimeException("Đơn hàng chưa được giao");
        }

        LocalDateTime deliveredAt = orderStatusHistoryRepository
                .findFirstByOrderIdAndNewStatusOrderByCreatedAtDesc(order.getId(), OrderStatus.DELIVERED.name())
                .map(OrderStatusHistory::getCreatedAt)
                .orElse(order.getCreatedAt());

        if (deliveredAt == null) {
            throw new RuntimeException("Đơn hàng chưa được giao");
        }

        if (deliveredAt.plusDays(returnWindowDays).isBefore(LocalDateTime.now())) {
            throw new RuntimeException(
                    "Chỉ có thể yêu cầu hoàn trả trong vòng " + returnWindowDays + " ngày sau khi nhận hàng");
        }

        OrderStatus oldStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory(
                order,
                oldStatus.name(),
                OrderStatus.REFUNDED.name(),
                "Customer",
                "Yêu cầu hoàn trả đơn hàng thành công"
        );
        orderStatusHistoryRepository.save(history);
    }

    @Transactional
    public void updateAdminNote(Long id, String adminNote) {
        orderRepository.findById(id).ifPresent(order -> {
            order.setAdminNote(adminNote);
            orderRepository.save(order);
        });
    }

    /**
     * ✅ Tạo đơn hàng từ giỏ hàng
     */
    @Transactional
    public Order createOrderFromCart(
            User user,
            String receiverName,
            String shippingAddress,
            String receiverPhone,
            PaymentMethod paymentMethod,
            String voucherCode
    ) {
        Cart cart = cartRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống");
        }

        Order order = new Order();
        order.setOrderCode(generateOrderCode());
        order.setUser(user);
        order.setReceiverName(receiverName);
        order.setShippingAddress(shippingAddress);
        order.setReceiverPhone(receiverPhone);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem ci : cart.getItems()) {
            Product p = ci.getProduct();
            ProductVariant variant = ci.getVariant();
            int reqQty = ci.getQuantity();

            if (variant != null) {
                if (variant.getStock() < reqQty) {
                    throw new RuntimeException("Biến thể '" + p.getName() +
                            " - " + variant.getColor() + " " + variant.getStorage() + "' không đủ tồn kho");
                }
            } else {
                if (p.getStock() < reqQty) {
                    throw new RuntimeException("Sản phẩm '" + p.getName() + "' không đủ tồn kho");
                }
            }

            OrderItem oi = new OrderItem();
            oi.setProduct(p);
            oi.setVariant(variant);
            oi.setQuantity(reqQty);
            oi.setPrice(ci.getUnitPriceAtAdd());
            oi.recalc();

            order.addItem(oi);
            total = total.add(oi.getLineTotal());

            if (paymentMethod == PaymentMethod.QR_CODE) {
                if (variant != null) {
                    variant.setReservedStock(variant.getReservedStock() + reqQty);
                    productVariantRepository.save(variant);
                    stockLogService.logVariant(p, variant, null, StockLogType.RESERVE,
                        -reqQty, variant.getStock(), "SYSTEM",
                        "Khoá tạm kho chờ thanh toán QR – " + p.getName());
                } else {
                    p.setReservedStock(p.getReservedStock() + reqQty);
                    productRepository.save(p);
                    stockLogService.log(p, null, StockLogType.RESERVE,
                        -reqQty, p.getStock(), "SYSTEM",
                        "Khoá tạm kho chờ thanh toán QR – " + p.getName());
                }
            } else {
                if (variant != null) {
                    variant.setStock(variant.getStock() - reqQty);
                    p.updateTotalStock();
                    productVariantRepository.save(variant);
                    stockLogService.logVariant(p, variant, null, StockLogType.EXPORT_ORDER,
                        -reqQty, variant.getStock(), "SYSTEM",
                        "Xuất kho đơn hàng COD – " + p.getName());
                } else {
                    p.setStock(p.getStock() - reqQty);
                    productRepository.save(p);
                    stockLogService.log(p, null, StockLogType.EXPORT_ORDER,
                        -reqQty, p.getStock(), "SYSTEM",
                        "Xuất kho đơn hàng COD – " + p.getName());
                }
            }
        }

        total = applyVoucherIfValid(order, total, voucherCode);
        order.setTotalAmount(total);

        if (paymentMethod == PaymentMethod.QR_CODE) {
            order.setPaymentDeadline(LocalDateTime.now().plusMinutes(15));
            order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(total);
        payment.setMethod(paymentMethod);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        order.setPayment(payment);

        Order saved = orderRepository.save(order);
        paymentRepository.save(payment);

        // 🔹 Ghi VoucherUsage nếu đơn có dùng voucher
        saveVoucherUsageIfApplied(saved);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(saved);
        history.setStatus(saved.getOrderStatus());
        history.setChangedAt(LocalDateTime.now());
        history.setNote("Đơn hàng đã được khởi tạo");
        history.setUpdatedBy(user != null ? user.getUsername() : "Customer");
        orderStatusHistoryRepository.save(history);

        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cart.recalcTotals();
        cartRepository.save(cart);

        return saved;
    }

    /**
     * ✅ Tạo đơn hàng “Mua ngay”
     */
    @Transactional
    public Order createOrderInstant(
            User user,
            Product product,
            ProductVariant variant,
            int quantity,
            String receiverName,
            String shippingAddress,
            String receiverPhone,
            PaymentMethod paymentMethod,
            String voucherCode
    ) {
        if (user == null) throw new RuntimeException("Người dùng không hợp lệ");
        if (product == null) throw new RuntimeException("Sản phẩm không tồn tại");
        if (quantity <= 0) quantity = 1;

        if (variant != null) {
            if (variant.getStock() < quantity) {
                throw new RuntimeException("Biến thể '" + product.getName() +
                        " - " + variant.getColor() + " " + variant.getStorage() + "' không đủ tồn kho");
            }
        } else {
            if (product.getStock() < quantity) {
                throw new RuntimeException("Sản phẩm '" + product.getName() + "' không đủ tồn kho");
            }
        }

        Order order = new Order();
        order.setOrderCode(generateOrderCode());
        order.setUser(user);
        order.setReceiverName(receiverName);
        order.setShippingAddress(shippingAddress);
        order.setReceiverPhone(receiverPhone);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreatedAt(LocalDateTime.now());

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setVariant(variant);
        orderItem.setQuantity(quantity);
        orderItem.setPrice(product.getFinalPrice());
        orderItem.recalc();

        order.addItem(orderItem);
        BigDecimal total = orderItem.getLineTotal();

        total = applyVoucherIfValid(order, total, voucherCode);
        order.setTotalAmount(total);

        if (paymentMethod == PaymentMethod.QR_CODE) {
            if (variant != null) {
                variant.setReservedStock(variant.getReservedStock() + quantity);
                productVariantRepository.save(variant);
                stockLogService.logVariant(product, variant, null, StockLogType.RESERVE,
                    -quantity, variant.getStock(), "SYSTEM",
                    "Khoá tạm kho chờ thanh toán QR (Mua ngay) – " + product.getName());
            } else {
                product.setReservedStock(product.getReservedStock() + quantity);
                productRepository.save(product);
                stockLogService.log(product, null, StockLogType.RESERVE,
                    -quantity, product.getStock(), "SYSTEM",
                    "Khoá tạm kho chờ thanh toán QR (Mua ngay) – " + product.getName());
            }
            order.setPaymentDeadline(LocalDateTime.now().plusMinutes(15));
            order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        } else {
            if (variant != null) {
                variant.setStock(variant.getStock() - quantity);
                product.updateTotalStock();
                productVariantRepository.save(variant);
                stockLogService.logVariant(product, variant, null, StockLogType.EXPORT_ORDER,
                    -quantity, variant.getStock(), "SYSTEM",
                    "Xuất kho đơn hàng COD (Mua ngay) – " + product.getName());
            } else {
                product.setStock(product.getStock() - quantity);
                productRepository.save(product);
                stockLogService.log(product, null, StockLogType.EXPORT_ORDER,
                    -quantity, product.getStock(), "SYSTEM",
                    "Xuất kho đơn hàng COD (Mua ngay) – " + product.getName());
            }
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(total);
        payment.setMethod(paymentMethod);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        order.setPayment(payment);

        Order savedOrder = orderRepository.save(order);
        paymentRepository.save(payment);

        // 🔹 Ghi VoucherUsage nếu đơn có dùng voucher
        saveVoucherUsageIfApplied(savedOrder);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(savedOrder);
        history.setStatus(savedOrder.getOrderStatus());
        history.setChangedAt(LocalDateTime.now());
        history.setNote("Đơn hàng đã được khởi tạo (Mua ngay)");
        history.setUpdatedBy(user != null ? user.getUsername() : "Customer");
        orderStatusHistoryRepository.save(history);

        return savedOrder;
    }

    private BigDecimal applyVoucherIfValid(Order order, BigDecimal total, String voucherCode) {
        if (voucherCode == null || voucherCode.isBlank()) return total;

        Optional<Voucher> opt = voucherRepository.findByCode(voucherCode);
        if (opt.isEmpty()) return total;

        Voucher v = opt.get();

        boolean valid =
                v.getActive() &&
                        v.getQuantity() > 0 &&
                        (v.getStartDate() == null || v.getStartDate().isBefore(LocalDateTime.now())) &&
                        (v.getEndDate() == null || v.getEndDate().isAfter(LocalDateTime.now())) &&
                        (v.getMinOrderValue() == null || total.compareTo(v.getMinOrderValue()) >= 0);

        if (!valid) return total;

        if (v.getUsageLimitPerUser() != null && v.getUsageLimitPerUser() > 0) {
            long used = orderRepository.countVoucherUsageByUser(v.getId(), order.getUser().getId());
            if (used >= v.getUsageLimitPerUser()) {
                return total; // User đã dùng đủ số lần
            }
        }

        BigDecimal discount = calculateDiscount(total, v);

        if (discount.compareTo(total) > 0) discount = total;

        int updated = voucherRepository.decrementQuantityIfAvailable(v.getId());
        if (updated == 0) {
            // Voucher đã hết trong khi xử lý (race condition)
            return total; // Bỏ qua voucher, không giảm giá
        }
        // Cập nhật số lượng trong Java memory & reload từ DB để có state mới nhất
        v.setQuantity(v.getQuantity() - 1);
        v = voucherRepository.findById(v.getId()).orElse(v);

        order.setVoucher(v);
        order.setDiscount(discount);

        return total.subtract(discount);
    }

    private BigDecimal calculateDiscount(BigDecimal total, Voucher v) {
        BigDecimal discount;
        if ("PERCENT".equalsIgnoreCase(v.getDiscountType())) {
            discount = total.multiply(v.getDiscountValue().divide(BigDecimal.valueOf(100)));
            if (v.getMaxDiscountAmount() != null && discount.compareTo(v.getMaxDiscountAmount()) > 0) {
                discount = v.getMaxDiscountAmount();
            }
        } else {
            discount = v.getDiscountValue();
        }
        return discount;
    }

    /**
     * Ghi VoucherUsage record sau khi order đã được persist.
     * Phải gọi SAU orderRepository.save() để order.id không null.
     *
     * @param order  Đơn hàng đã được persist (có id)
     */
    public void saveVoucherUsageIfApplied(Order order) {
        if (order == null || order.getVoucher() == null || order.getDiscount() == null
                || order.getDiscount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        // Tránh ghi trùng nếu webhook gọi lại
        if (voucherUsageRepository.findByOrder_Id(order.getId()).isPresent()) {
            return;
        }
        try {
            VoucherUsage usage = new VoucherUsage(
                    order.getVoucher(),
                    order.getUser(),
                    order,
                    order.getDiscount()
            );
            voucherUsageRepository.save(usage);
        } catch (Exception e) {
            // Log nhưng không fail order flow
            org.slf4j.LoggerFactory.getLogger(OrderService.class)
                    .warn("[VoucherUsage] Không ghi được VoucherUsage cho order {}: {}",
                            order.getOrderCode(), e.getMessage());
        }
    }


    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getOrderStatus().equals(OrderStatus.PENDING_PAYMENT) && 
            !order.getOrderStatus().equals(OrderStatus.CONFIRMED)) {
            throw new RuntimeException("Không thể hủy đơn này!");
        }

        boolean wasQrPending = OrderStatus.PENDING_PAYMENT.equals(order.getOrderStatus());

        for (OrderItem item : order.getOrderItems()) {
            Product p = item.getProduct();
            ProductVariant variant = item.getVariant();
            int qty = item.getQuantity();

            if (wasQrPending) {
                if (variant != null) {
                    variant.setReservedStock(Math.max(0, variant.getReservedStock() - qty));
                    productVariantRepository.save(variant);
                    stockLogService.logVariant(p, variant, order, StockLogType.CANCEL_RESTORE,
                        qty, variant.getStock(), "SYSTEM",
                        "Hoàn kho – Hủy đơn QR timeout: " + order.getOrderCode());
                } else {
                    p.setReservedStock(Math.max(0, p.getReservedStock() - qty));
                    productRepository.save(p);
                    stockLogService.log(p, order, StockLogType.CANCEL_RESTORE,
                        qty, p.getStock(), "SYSTEM",
                        "Hoàn kho – Hủy đơn QR timeout: " + order.getOrderCode());
                }
            } else {
                if (variant != null) {
                    variant.setStock(variant.getStock() + qty);
                    p.updateTotalStock();
                    productVariantRepository.save(variant);
                    stockLogService.logVariant(p, variant, order, StockLogType.CANCEL_RESTORE,
                        qty, variant.getStock(), "SYSTEM",
                        "Hoàn kho – Hủy đơn: " + order.getOrderCode());
                } else {
                    p.setStock(p.getStock() + qty);
                    productRepository.save(p);
                    stockLogService.log(p, order, StockLogType.CANCEL_RESTORE,
                        qty, p.getStock(), "SYSTEM",
                        "Hoàn kho – Hủy đơn: " + order.getOrderCode());
                }
            }
        }

        OrderStatus oldStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldStatus(oldStatus != null ? oldStatus.name() : null);
        history.setStatus(OrderStatus.CANCELLED);
        history.setChangedAt(LocalDateTime.now());
        history.setNote("Đơn hàng đã bị hủy");
        history.setUpdatedBy("System/Customer");
        orderStatusHistoryRepository.save(history);
    }

    /**
     * ✅ Xác nhận thanh toán QR thành công → trừ kho chính thức
     */
    @Transactional
    public void confirmQrPayment(Long orderId, String performedBy) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!OrderStatus.PENDING_PAYMENT.equals(order.getOrderStatus())) return;

        for (OrderItem item : order.getOrderItems()) {
            Product p = item.getProduct();
            ProductVariant variant = item.getVariant();
            int qty = item.getQuantity();

            if (variant != null) {
                variant.setStock(variant.getStock() - qty);
                variant.setReservedStock(Math.max(0, variant.getReservedStock() - qty));
                p.updateTotalStock();
                productVariantRepository.save(variant);
                stockLogService.logVariant(p, variant, order, StockLogType.CONFIRM_RESERVE,
                    -qty, variant.getStock(), performedBy,
                    "Xác nhận QR → Trừ kho chính thức: " + order.getOrderCode());
            } else {
                p.setStock(p.getStock() - qty);
                p.setReservedStock(Math.max(0, p.getReservedStock() - qty));
                productRepository.save(p);
                stockLogService.log(p, order, StockLogType.CONFIRM_RESERVE,
                    -qty, p.getStock(), performedBy,
                    "Xác nhận QR → Trừ kho chính thức: " + order.getOrderCode());
            }
        }

        OrderStatus oldStatus2 = order.getOrderStatus();
        order.setOrderStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        OrderStatusHistory history2 = new OrderStatusHistory();
        history2.setOrder(order);
        history2.setOldStatus(oldStatus2 != null ? oldStatus2.name() : null);
        history2.setStatus(OrderStatus.CONFIRMED);
        history2.setChangedAt(LocalDateTime.now());
        history2.setNote("Thanh toán thành công & đã xác nhận đơn hàng");
        history2.setUpdatedBy(performedBy != null ? performedBy : "System");
        orderStatusHistoryRepository.save(history2);
    }

    // ===========================
    // 🔹 CRUD & QUERIES
    // ===========================

    public List<Order> getAllOrders() { return orderRepository.findAll(); }

    public List<Order> getOrdersByUser(Long userId) { return orderRepository.findByUser_Id(userId); }

    public Optional<Order> getOrderById(Long id) { return orderRepository.findById(id); }

    public Optional<Order> findById(Long id) { return orderRepository.findById(id); }

    public void deleteOrder(Long id) { orderRepository.deleteById(id); }

    public Order saveOrder(Order order) { return orderRepository.save(order); }

    public Order getByOrderCode(String code) {
        return orderRepository.findByOrderCode(code).orElse(null);
    }

    private String generateOrderCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = (int)(Math.random() * 9000) + 1000;
        return "DH" + date + "-" + random;
    }

    public BigDecimal getRevenueByDate(LocalDate date) {
        return orderRepository.sumRevenueByDate(date).orElse(BigDecimal.ZERO);
    }

    public int countOrdersByDate(LocalDate date) {
        return orderRepository.countOrdersByDate(date);
    }

    public List<Order> getRecentOrders(int limit) {
        return orderRepository.findRecentOrders(limit);
    }

    public Page<Order> getPagedOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderRepository.findAll(pageable);
    }

    public List<Order> getOrdersByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(6);
        if (endDate == null) endDate = LocalDate.now();
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        return orderRepository.findOrdersByDateRange(start, end);
    }

    public BigDecimal getRevenueByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(6);
        if (endDate == null) endDate = LocalDate.now();
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        List<OrderStatus> successStatuses = List.of(OrderStatus.CONFIRMED, OrderStatus.PACKING, OrderStatus.SHIPPING, OrderStatus.DELIVERED);
        return orderRepository.sumRevenueByDateRange(start, end, successStatuses).orElse(BigDecimal.ZERO);
    }

    public long countOrdersByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(6);
        if (endDate == null) endDate = LocalDate.now();
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        return orderRepository.countOrdersByDateRange(start, end);
    }

    public long countOrdersByStatusesInRange(LocalDate startDate, LocalDate endDate, List<OrderStatus> statuses) {
        if (startDate == null) startDate = LocalDate.now().minusDays(6);
        if (endDate == null) endDate = LocalDate.now();
        if (statuses == null || statuses.isEmpty()) return 0;
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        return orderRepository.countOrdersByDateRangeAndStatuses(start, end, statuses);
    }

    public List<Order> filterOrders(String statusGroup, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusYears(1).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDate.now().atTime(23, 59, 59);

        List<Order> orders = orderRepository.findOrdersByDateRange(start, end);

        if (statusGroup == null || statusGroup.isBlank() || statusGroup.equalsIgnoreCase("all")) {
            return orders;
        }

        List<OrderStatus> targetStatuses;
        if (statusGroup.equalsIgnoreCase("success")) {
            targetStatuses = List.of(OrderStatus.CONFIRMED, OrderStatus.PACKING, OrderStatus.SHIPPING, OrderStatus.DELIVERED);
        } else if (statusGroup.equalsIgnoreCase("pending")) {
            targetStatuses = List.of(OrderStatus.PENDING_PAYMENT);
        } else if (statusGroup.equalsIgnoreCase("cancelled")) {
            targetStatuses = List.of(OrderStatus.CANCELLED, OrderStatus.REFUNDED);
        } else {
            OrderStatus st = OrderStatus.fromString(statusGroup);
            targetStatuses = List.of(st);
        }

        return orders.stream()
                .filter(o -> o.getOrderStatus() != null && targetStatuses.contains(o.getOrderStatus()))
                .toList();
    }

    public java.util.Map<String, Object> getDailyRevenueDataInRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(6);
        if (endDate == null) endDate = LocalDate.now();
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        List<String> labels = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            labels.add(current.format(fmt));
            BigDecimal rev = getRevenueByDate(current);
            revenues.add(rev != null ? rev : BigDecimal.ZERO);
            current = current.plusDays(1);
        }

        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("labels", labels);
        map.put("revenues", revenues);
        return map;
    }

    public BigDecimal getCogsInRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(6);
        if (endDate == null) endDate = LocalDate.now();
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        List<OrderStatus> successStatuses = List.of(OrderStatus.CONFIRMED, OrderStatus.PACKING, OrderStatus.SHIPPING, OrderStatus.DELIVERED);

        List<Order> orders = orderRepository.findOrdersByDateRange(start, end).stream()
            .filter(o -> o.getOrderStatus() != null && successStatuses.contains(o.getOrderStatus()))
            .toList();

        BigDecimal totalCogs = BigDecimal.ZERO;
        for (Order order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                BigDecimal importPrice = null;
                if (item.getVariant() != null && item.getVariant().getImportPrice() != null) {
                    importPrice = item.getVariant().getImportPrice();
                } else if (item.getProduct() != null && item.getProduct().getImportPrice() != null) {
                    importPrice = item.getProduct().getImportPrice();
                }
                if (importPrice != null) {
                    totalCogs = totalCogs.add(importPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
        }
        return totalCogs;
    }

    public BigDecimal getTotalDiscountInRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(6);
        if (endDate == null) endDate = LocalDate.now();
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        List<OrderStatus> successStatuses = List.of(OrderStatus.CONFIRMED, OrderStatus.PACKING, OrderStatus.SHIPPING, OrderStatus.DELIVERED);
        BigDecimal result = orderRepository.sumDiscountByDateRange(start, end, successStatuses);
        return result != null ? result : BigDecimal.ZERO;
    }

}
