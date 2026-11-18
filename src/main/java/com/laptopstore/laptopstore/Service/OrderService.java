package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.*;
import com.laptopstore.laptopstore.entity.*;
import com.laptopstore.laptopstore.enums.OrderStatus;
import com.laptopstore.laptopstore.enums.PaymentMethod;
import com.laptopstore.laptopstore.enums.PaymentStatus;
import com.laptopstore.laptopstore.enums.StockLogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

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

    // =========================================================================
    // 🔹 HELPER KIỂM TRA TRẠNG THÁI & KHÓA DỮ LIỆU ĐƠN HÀNG
    // =========================================================================

    /**
     * Kiểm tra tính hợp lệ của bước chuyển trạng thái theo luồng chuẩn hóa.
     */
    public boolean isValidTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        return currentStatus.canTransitionTo(targetStatus);
    }

    /**
     * Validate bước chuyển trạng thái trước khi cập nhật.
     * Không cho phép chuyển lùi, nhảy bước hoặc chuyển sang trạng thái không hợp lệ.
     */
    public void validateStatusTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ.");
        }
        if (currentStatus == targetStatus) {
            return;
        }
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new IllegalStateException(String.format(
                    "Không thể chuyển trạng thái đơn hàng từ '%s' (%s) sang '%s' (%s). Luồng hợp lệ tiếp theo: %s",
                    currentStatus.getDisplayName(), currentStatus.name(),
                    targetStatus.getDisplayName(), targetStatus.name(),
                    currentStatus.getAllowedNextStatuses().isEmpty()
                            ? "Không còn (trạng thái kết thúc)"
                            : currentStatus.getAllowedNextStatuses().stream()
                                    .map(s -> s.getDisplayName() + " [" + s.name() + "]")
                                    .reduce((a, b) -> a + ", " + b).orElse("")
            ));
        }
    }

    /**
     * Khóa chỉnh sửa dữ liệu khi đơn hàng đã hoàn tất (HOAN_THANH) hoặc ở trạng thái ngoại lệ/hủy.
     * Chỉ cho phép xử lý quy trình hoàn trả.
     */
    public void validateOrderEditable(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng.");
        }
        if (order.isLockedForEdit()) {
            throw new IllegalStateException(
                    "Đơn hàng " + order.getOrderCode() + " đang ở trạng thái '"
                            + order.getOrderStatus().getDisplayName()
                            + "', dữ liệu đã bị khóa (không cho phép sửa thông tin đơn, sản phẩm hay số lượng). "
                            + "Chỉ cho phép xử lý quy trình hoàn trả nếu đủ điều kiện.");
        }
    }

    // =========================================================================
    // 🔹 NGHIỆP VỤ TRỪ KHO (KHI CHUYỂN SANG DA_XAC_NHAN) & HOÀN KHO TỰ ĐỘNG
    // =========================================================================

    /**
     * Kiểm tra tồn kho và trừ kho ngay lập tức khi đơn hàng chuyển sang DA_XAC_NHAN.
     * - Kiểm tra tồn kho tất cả sản phẩm trước khi trừ để tránh overselling.
     * - Idempotent: nếu inventoryDeducted = true thì không trừ kho thêm lần nữa.
     * - Ghi lịch sử xuất kho (StockLogType.EXPORT_ORDER).
     */
    private void deductInventoryForOrder(Order order, String performedBy) {
        if (order.isInventoryDeducted()) {
            log.warn("[Inventory][DEDUCT-SKIP] Đơn {} đã được trừ kho trước đó (inventoryDeducted=true). Bỏ qua trừ kho lặp lại.",
                    order.getOrderCode());
            return;
        }

        List<OrderItem> items = order.getOrderItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("Đơn hàng " + order.getOrderCode() + " không có sản phẩm nào để xác nhận.");
        }

        // Bước 1: Kiểm tra tồn kho tất cả sản phẩm trước khi trừ
        List<String> insufficientItems = new ArrayList<>();
        for (OrderItem item : items) {
            Product product = item.getProduct();
            ProductVariant variant = item.getVariant();
            int reqQty = item.getQuantity();

            if (product == null) {
                insufficientItems.add("Sản phẩm không tồn tại (SL cần: " + reqQty + ")");
                continue;
            }

            if (variant != null) {
                int available = variant.getStock() != null ? variant.getStock() : 0;
                if (available < reqQty) {
                    insufficientItems.add(String.format("%s (%s - %s): cần %d, còn %d",
                            product.getName(), variant.getColor(), variant.getStorage(), reqQty, available));
                }
            } else {
                int available = product.getStock() != null ? product.getStock() : 0;
                if (available < reqQty) {
                    insufficientItems.add(String.format("%s: cần %d, còn %d",
                            product.getName(), reqQty, available));
                }
            }
        }

        if (!insufficientItems.isEmpty()) {
            String message = "Không đủ tồn kho để xác nhận đơn hàng " + order.getOrderCode()
                    + ". Sản phẩm thiếu hàng: " + String.join("; ", insufficientItems);
            log.warn("[Inventory][INSUFFICIENT] {}", message);
            throw new IllegalStateException(message);
        }

        // Bước 2: Trừ kho cho toàn bộ sản phẩm trong đơn và ghi lịch sử xuất kho
        String actor = (performedBy != null && !performedBy.isBlank()) ? performedBy : "Admin";
        for (OrderItem item : items) {
            Product product = item.getProduct();
            ProductVariant variant = item.getVariant();
            int qty = item.getQuantity();

            if (variant != null) {
                int oldVariantStock = variant.getStock() != null ? variant.getStock() : 0;
                int newVariantStock = oldVariantStock - qty;
                variant.setStock(newVariantStock);
                productVariantRepository.save(variant);

                if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                    product.updateTotalStock();
                } else {
                    int oldProdStock = product.getStock() != null ? product.getStock() : 0;
                    product.setStock(Math.max(0, oldProdStock - qty));
                }
                productRepository.save(product);

                stockLogService.logVariant(
                        product, variant, order, StockLogType.EXPORT_ORDER,
                        -qty, newVariantStock, actor,
                        "Xuất kho khi xác nhận đơn hàng " + order.getOrderCode()
                );
                log.info("[Inventory][DEDUCT] Đơn={} | SP={} | Biến thể={} - {} | -{} | Tồn biến thể: {} -> {}",
                        order.getOrderCode(), product.getName(), variant.getColor(), variant.getStorage(),
                        qty, oldVariantStock, newVariantStock);
            } else {
                int oldStock = product.getStock() != null ? product.getStock() : 0;
                int newStock = oldStock - qty;
                product.setStock(newStock);
                productRepository.save(product);

                stockLogService.log(
                        product, order, StockLogType.EXPORT_ORDER,
                        -qty, newStock, actor,
                        "Xuất kho khi xác nhận đơn hàng " + order.getOrderCode()
                );
                log.info("[Inventory][DEDUCT] Đơn={} | SP={} | -{} | Tồn: {} -> {}",
                        order.getOrderCode(), product.getName(), qty, oldStock, newStock);
            }
        }

        order.setInventoryDeducted(true);
        log.info("[Inventory][DEDUCT-DONE] Đã trừ kho thành công và đánh dấu inventoryDeducted=true cho đơn {}", order.getOrderCode());
    }

    /**
     * Hoàn kho tự động khi hủy đơn (từ DA_XAC_NHAN / CHUAN_BI_HANG -> DA_HUY)
     * hoặc khi xác nhận hoàn trả hàng (YEU_CAU_HOAN_TRA -> DA_HOAN_TRA).
     * - Idempotent: chỉ hoàn kho khi inventoryDeducted = true; sau khi hoàn đặt inventoryDeducted = false.
     * - Ghi lịch sử nhập kho tương ứng.
     */
    private void restoreInventoryForOrder(Order order, String performedBy, StockLogType logType, String reasonNote) {
        if (!order.isInventoryDeducted()) {
            log.info("[Inventory][RESTORE-SKIP] Đơn {} chưa trừ kho (inventoryDeducted=false). Không hoàn kho.",
                    order.getOrderCode());
            return;
        }

        String actor = (performedBy != null && !performedBy.isBlank()) ? performedBy : "System";
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                ProductVariant variant = item.getVariant();
                int qty = item.getQuantity();
                if (product == null) continue;

                if (variant != null) {
                    int oldVariantStock = variant.getStock() != null ? variant.getStock() : 0;
                    int newVariantStock = oldVariantStock + qty;
                    variant.setStock(newVariantStock);
                    productVariantRepository.save(variant);

                    if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                        product.updateTotalStock();
                    } else {
                        int oldProdStock = product.getStock() != null ? product.getStock() : 0;
                        product.setStock(oldProdStock + qty);
                    }
                    productRepository.save(product);

                    stockLogService.logVariant(
                            product, variant, order, logType,
                            qty, newVariantStock, actor, reasonNote
                    );
                    log.info("[Inventory][RESTORE] Đơn={} | SP={} | Biến thể={} - {} | +{} | Tồn biến thể: {} -> {}",
                            order.getOrderCode(), product.getName(), variant.getColor(), variant.getStorage(),
                            qty, oldVariantStock, newVariantStock);
                } else {
                    int oldStock = product.getStock() != null ? product.getStock() : 0;
                    int newStock = oldStock + qty;
                    product.setStock(newStock);
                    productRepository.save(product);

                    stockLogService.log(
                            product, order, logType,
                            qty, newStock, actor, reasonNote
                    );
                    log.info("[Inventory][RESTORE] Đơn={} | SP={} | +{} | Tồn: {} -> {}",
                            order.getOrderCode(), product.getName(), qty, oldStock, newStock);
                }
            }
        }

        order.setInventoryDeducted(false);
        log.info("[Inventory][RESTORE-DONE] Đã hoàn kho và đặt inventoryDeducted=false cho đơn {}", order.getOrderCode());
    }

    // =========================================================================
    // 🔹 CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG (SERVICE LAYER DUY NHẤT)
    // =========================================================================

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, OrderStatus newStatus) {
        updateOrderStatus(id, newStatus, "Admin", null);
    }

    /**
     * Chuyển trạng thái đơn hàng có kiểm tra luồng hợp lệ, xử lý trừ/hoàn kho nguyên tử và ghi log đầy đủ.
     */
    @Transactional(rollbackFor = Exception.class)
    public Order updateOrderStatus(Long id, OrderStatus newStatus, String performedBy, String customNote) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + id));

        if (newStatus == null) {
            throw new IllegalArgumentException("Trạng thái đích không hợp lệ.");
        }

        OrderStatus oldStatus = order.getOrderStatus() != null ? order.getOrderStatus() : OrderStatus.CHO_XAC_NHAN;
        if (oldStatus == newStatus) {
            log.debug("[Order][STATUS-IDEMPOTENT] Đơn {} đã ở trạng thái {}, bỏ qua cập nhật trùng lặp.",
                    order.getOrderCode(), newStatus.name());
            return order;
        }

        // Validate bước chuyển trạng thái hợp lệ (không nhảy bước, không chuyển lùi)
        validateStatusTransition(oldStatus, newStatus);

        String actor = (performedBy != null && !performedBy.isBlank()) ? performedBy : "Admin";
        log.info("[Order][STATUS] Chuyển trạng thái đơn {}: {} -> {} (bởi {})",
                order.getOrderCode(), oldStatus.name(), newStatus.name(), actor);

        switch (newStatus) {
            case DA_XAC_NHAN:
                // Trừ kho ngay khi xác nhận đơn (chỉ trừ nếu inventoryDeducted == false)
                deductInventoryForOrder(order, actor);
                break;

            case CHUAN_BI_HANG:
            case DANG_GIAO_HANG:
                // Không trừ kho lại ở các bước xử lý / vận chuyển
                break;

            case DA_GIAO_HANG:
            case HOAN_THANH:
                // Không trừ kho khi hoàn thành; cập nhật thanh toán COD sang PAID nếu đang PENDING
                if (order.getPayment() != null && order.getPayment().getStatus() == PaymentStatus.PENDING) {
                    order.getPayment().setStatus(PaymentStatus.PAID);
                }
                break;

            case DA_HUY:
                log.info("[Order][CANCEL] Hủy đơn {} từ trạng thái {} bởi {}",
                        order.getOrderCode(), oldStatus.name(), actor);
                if (order.isInventoryDeducted()) {
                    restoreInventoryForOrder(
                            order, actor, StockLogType.CANCEL_RESTORE,
                            "Hoàn kho tự động khi hủy đơn " + order.getOrderCode()
                    );
                }
                break;

            case YEU_CAU_HOAN_TRA:
                log.info("[Order][RETURN_REQUEST] Ghi nhận yêu cầu hoàn trả đơn {} bởi {}",
                        order.getOrderCode(), actor);
                break;

            case DA_HOAN_TRA:
                if (order.isInventoryDeducted()) {
                    restoreInventoryForOrder(
                            order, actor, StockLogType.IMPORT,
                            "Nhập hoàn kho tự động từ đơn hoàn trả " + order.getOrderCode()
                    );
                }
                if (order.getPayment() != null && order.getPayment().getStatus() != PaymentStatus.REFUNDED) {
                    order.getPayment().setStatus(PaymentStatus.REFUND_PENDING);
                }
                log.info("[Order][RETURNED] Đã hoàn trả đơn {} thành công (inventoryDeducted={})",
                        order.getOrderCode(), order.isInventoryDeducted());
                break;

            case DA_HOAN_TIEN:
                if (order.getPayment() != null) {
                    order.getPayment().setStatus(PaymentStatus.REFUNDED);
                }
                log.info("[Order][REFUND] Đã hoàn tiền cho đơn {} | Số tiền={} | Thực hiện bởi={}",
                        order.getOrderCode(), order.getTotalAmount(), actor);
                break;

            default:
                break;
        }

        order.setOrderStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(savedOrder);
        history.setOldStatus(oldStatus.name());
        history.setStatus(newStatus);
        history.setChangedAt(LocalDateTime.now());
        history.setUpdatedBy(actor);
        history.setNote((customNote != null && !customNote.isBlank())
                ? customNote
                : ("Cập nhật trạng thái: " + oldStatus.getDisplayName() + " → " + newStatus.getDisplayName()));
        orderStatusHistoryRepository.save(history);

        return savedOrder;
    }

    public boolean isOrderEligibleForReturn(Order order) {
        if (order == null) {
            return false;
        }
        OrderStatus st = order.getOrderStatus();
        if (st != OrderStatus.DA_GIAO_HANG && st != OrderStatus.HOAN_THANH) {
            return false;
        }

        LocalDateTime deliveredAt = orderStatusHistoryRepository
                .findFirstByOrderIdAndNewStatusOrderByCreatedAtDesc(order.getId(), OrderStatus.DA_GIAO_HANG.name())
                .map(OrderStatusHistory::getCreatedAt)
                .orElseGet(() -> orderStatusHistoryRepository
                        .findFirstByOrderIdAndNewStatusOrderByCreatedAtDesc(order.getId(), OrderStatus.HOAN_THANH.name())
                        .map(OrderStatusHistory::getCreatedAt)
                        .orElse(order.getCreatedAt()));

        if (deliveredAt == null) {
            return false;
        }

        return !deliveredAt.plusDays(returnWindowDays).isBefore(LocalDateTime.now());
    }

    @Transactional(rollbackFor = Exception.class)
    public void requestReturnOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        OrderStatus st = order.getOrderStatus();
        if (st != OrderStatus.DA_GIAO_HANG && st != OrderStatus.HOAN_THANH) {
            throw new IllegalStateException("Chỉ đơn hàng đã giao hoặc hoàn thành mới có thể yêu cầu hoàn trả.");
        }

        if (!isOrderEligibleForReturn(order)) {
            throw new IllegalStateException(
                    "Chỉ có thể yêu cầu hoàn trả trong vòng " + returnWindowDays + " ngày sau khi nhận hàng");
        }

        updateOrderStatus(orderId, OrderStatus.YEU_CAU_HOAN_TRA, "Customer", "Khách hàng gửi yêu cầu hoàn trả đơn hàng");
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateAdminNote(Long id, String adminNote) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + id));
        validateOrderEditable(order);
        order.setAdminNote(adminNote);
        orderRepository.save(order);
    }

    /**
     * Cập nhật thông tin giao nhận đơn hàng (khóa khi đơn đã HOAN_THANH hoặc ở trạng thái kết thúc).
     */
    @Transactional(rollbackFor = Exception.class)
    public Order updateOrderShippingInfo(Long id, String receiverName, String receiverPhone, String shippingAddress) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + id));
        validateOrderEditable(order);
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setShippingAddress(shippingAddress);
        return orderRepository.save(order);
    }

    /**
     * Cập nhật số lượng sản phẩm trong đơn hàng (chỉ cho phép trước khi khóa/trừ kho; khóa hoàn toàn khi HOAN_THANH).
     */
    @Transactional(rollbackFor = Exception.class)
    public Order updateOrderItemQuantity(Long orderId, Long orderItemId, int newQuantity) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId));
        validateOrderEditable(order);
        if (order.isInventoryDeducted()) {
            throw new IllegalStateException("Đơn hàng đã trừ kho, không thể thay đổi số lượng sản phẩm.");
        }
        OrderItem target = order.getOrderItems().stream()
                .filter(i -> orderItemId.equals(i.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong đơn hàng."));
        target.setQuantity(Math.max(1, newQuantity));
        order.recalcTotal();
        if (order.getDiscount() != null && order.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            order.setTotalAmount(order.getTotalAmount().subtract(order.getDiscount()).max(BigDecimal.ZERO));
        }
        return orderRepository.save(order);
    }

    /**
     * ✅ Tạo đơn hàng từ giỏ hàng.
     * Lưu ý nghiệp vụ: Chỉ kiểm tra tồn kho sơ bộ khi đặt hàng, KHÔNG trừ kho ngay lúc tạo đơn.
     * Tồn kho sẽ được kiểm tra lại và trừ chính thức khi đơn chuyển sang DA_XAC_NHAN.
     */
    @Transactional(rollbackFor = Exception.class)
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
        order.setInventoryDeducted(false);
        order.setCreatedAt(LocalDateTime.now());

        boolean isOnlinePrepay = (paymentMethod == PaymentMethod.QR_CODE || paymentMethod == PaymentMethod.SEPAY);
        if (isOnlinePrepay) {
            order.setOrderStatus(OrderStatus.CHO_THANH_TOAN);
            order.setPaymentDeadline(LocalDateTime.now().plusMinutes(15));
        } else {
            order.setOrderStatus(OrderStatus.CHO_XAC_NHAN);
        }

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem ci : cart.getItems()) {
            Product p = ci.getProduct();
            ProductVariant variant = ci.getVariant();
            int reqQty = ci.getQuantity();

            if (variant != null) {
                int available = variant.getStock() != null ? variant.getStock() : 0;
                if (available < reqQty) {
                    throw new IllegalStateException("Biến thể '" + p.getName()
                            + " - " + variant.getColor() + " " + variant.getStorage()
                            + "' không đủ tồn kho (cần " + reqQty + ", còn " + available + ")");
                }
            } else {
                int available = p.getStock() != null ? p.getStock() : 0;
                if (available < reqQty) {
                    throw new IllegalStateException("Sản phẩm '" + p.getName()
                            + "' không đủ tồn kho (cần " + reqQty + ", còn " + available + ")");
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
        }

        total = applyVoucherIfValid(order, total, voucherCode);
        order.setTotalAmount(total);

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
        history.setNote("Đơn hàng đã được khởi tạo (" + saved.getOrderStatus().getDisplayName() + ")");
        history.setUpdatedBy(user != null ? user.getUsername() : "Customer");
        orderStatusHistoryRepository.save(history);

        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cart.recalcTotals();
        cartRepository.save(cart);

        log.info("[Order][CREATE] Tạo đơn hàng {} từ giỏ hàng | Status={} | inventoryDeducted=false",
                saved.getOrderCode(), saved.getOrderStatus().name());
        return saved;
    }

    /**
     * ✅ Tạo đơn hàng “Mua ngay”.
     * Chỉ kiểm tra tồn kho sơ bộ khi đặt hàng, KHÔNG trừ kho ngay (chỉ trừ khi chuyển sang DA_XAC_NHAN).
     */
    @Transactional(rollbackFor = Exception.class)
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
            int available = variant.getStock() != null ? variant.getStock() : 0;
            if (available < quantity) {
                throw new IllegalStateException("Biến thể '" + product.getName()
                        + " - " + variant.getColor() + " " + variant.getStorage()
                        + "' không đủ tồn kho (cần " + quantity + ", còn " + available + ")");
            }
        } else {
            int available = product.getStock() != null ? product.getStock() : 0;
            if (available < quantity) {
                throw new IllegalStateException("Sản phẩm '" + product.getName()
                        + "' không đủ tồn kho (cần " + quantity + ", còn " + available + ")");
            }
        }

        Order order = new Order();
        order.setOrderCode(generateOrderCode());
        order.setUser(user);
        order.setReceiverName(receiverName);
        order.setShippingAddress(shippingAddress);
        order.setReceiverPhone(receiverPhone);
        order.setInventoryDeducted(false);
        order.setCreatedAt(LocalDateTime.now());

        boolean isOnlinePrepay = (paymentMethod == PaymentMethod.QR_CODE || paymentMethod == PaymentMethod.SEPAY);
        if (isOnlinePrepay) {
            order.setOrderStatus(OrderStatus.CHO_THANH_TOAN);
            order.setPaymentDeadline(LocalDateTime.now().plusMinutes(15));
        } else {
            order.setOrderStatus(OrderStatus.CHO_XAC_NHAN);
        }

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
        history.setNote("Đơn hàng đã được khởi tạo - Mua ngay (" + savedOrder.getOrderStatus().getDisplayName() + ")");
        history.setUpdatedBy(user != null ? user.getUsername() : "Customer");
        orderStatusHistoryRepository.save(history);

        log.info("[Order][CREATE-INSTANT] Tạo đơn hàng {} | Status={} | inventoryDeducted=false",
                savedOrder.getOrderCode(), savedOrder.getOrderStatus().name());
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


    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId));

        if (order.getOrderStatus() == null || !order.getOrderStatus().isCancellable()) {
            throw new IllegalStateException("Không thể hủy đơn hàng ở trạng thái '"
                    + (order.getOrderStatus() != null ? order.getOrderStatus().getDisplayName() : "") + "'!");
        }

        updateOrderStatus(orderId, OrderStatus.DA_HUY, "Customer/System", "Đơn hàng đã bị hủy");
    }

    /**
     * ✅ Xác nhận thanh toán QR thành công → chuyển sang CHO_XAC_NHAN để Admin duyệt và trừ kho
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmQrPayment(Long orderId, String performedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId));

        if (!OrderStatus.CHO_THANH_TOAN.equals(order.getOrderStatus())) {
            log.debug("[Order][QR-SKIP] Đơn {} hiện đang ở trạng thái {}, bỏ qua confirmQrPayment.",
                    order.getOrderCode(), order.getOrderStatus());
            return;
        }

        if (order.getPayment() != null) {
            order.getPayment().setStatus(PaymentStatus.PAID);
        }
        String actor = (performedBy != null && !performedBy.isBlank()) ? performedBy : "System";
        updateOrderStatus(orderId, OrderStatus.CHO_XAC_NHAN, actor, "Thanh toán QR thành công – Chờ xác nhận đơn hàng");
    }

    // ===========================
    // 🔹 CRUD & QUERIES
    // ===========================

    public List<Order> getAllOrders() { return orderRepository.findAll(); }

    public List<Order> getOrdersByUser(Long userId) { return orderRepository.findByUser_Id(userId); }

    public Optional<Order> getOrderById(Long id) { return orderRepository.findById(id); }

    public List<Order> searchOrders(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return orderRepository.findAll(Sort.by("createdAt").descending());
        }
        return orderRepository.searchOrdersByKeyword(keyword.trim());
    }

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
        return orderRepository.sumRevenueByDateRange(start, end, OrderStatus.SUCCESS_STATUSES).orElse(BigDecimal.ZERO);
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
            targetStatuses = OrderStatus.SUCCESS_STATUSES;
        } else if (statusGroup.equalsIgnoreCase("pending")) {
            targetStatuses = OrderStatus.PENDING_STATUSES;
        } else if (statusGroup.equalsIgnoreCase("cancelled")) {
            targetStatuses = OrderStatus.CANCELLED_STATUSES;
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

        List<Order> orders = orderRepository.findOrdersByDateRange(start, end).stream()
            .filter(o -> o.getOrderStatus() != null && OrderStatus.SUCCESS_STATUSES.contains(o.getOrderStatus()))
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
        BigDecimal result = orderRepository.sumDiscountByDateRange(start, end, OrderStatus.SUCCESS_STATUSES);
        return result != null ? result : BigDecimal.ZERO;
    }

}
