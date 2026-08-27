    package com.laptopstore.laptopstore.Service;

    import com.laptopstore.laptopstore.Repository.*;
    import com.laptopstore.laptopstore.entity.*;
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
        @Autowired private PaymentRepository paymentRepository;
        @Autowired private ProductVariantRepository productVariantRepository;
        @Autowired private StockLogService stockLogService;




        @Transactional
        public void updateStatus(Long id, String newStatus) {
            orderRepository.findById(id).ifPresent(order -> {
                order.setOrderStatus(newStatus);
                orderRepository.save(order);
            });
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
            order.setOrderStatus("Pending");
            order.setCreatedAt(LocalDateTime.now());




            BigDecimal total = BigDecimal.ZERO;

            for (CartItem ci : cart.getItems()) {
                Product p = ci.getProduct();
                ProductVariant variant = ci.getVariant();          // 🔹 lấy biến thể từ cart item
                int reqQty = ci.getQuantity();

                // ✅ Check tồn kho theo variant nếu có
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
                oi.setVariant(variant);                    // 🔹 LƯU BIẾN THỂ VÀO ORDER ITEM
                oi.setQuantity(reqQty);
                oi.setPrice(ci.getUnitPriceAtAdd());
                oi.recalc();

                order.addItem(oi); // thiết lập 2 chiều
                total = total.add(oi.getLineTotal());

                // 🔻 Khoá kho hoặc trừ kho theo phương thức thanh toán
                if (paymentMethod == PaymentMethod.QR_CODE) {
                    // QR: chỉ khoá tạm (reservedStock), chưa trừ stock chính thức
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
                    // COD / Banking: trừ thẳng stock
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

            // 🎟 Áp dụng voucher
            total = applyVoucherIfValid(order, total, voucherCode);

            // 💰 Lưu tổng tiền
            order.setTotalAmount(total);

            // ⏰ Set payment deadline nếu QR (15 phút)
            if (paymentMethod == PaymentMethod.QR_CODE) {
                order.setPaymentDeadline(LocalDateTime.now().plusMinutes(15));
                order.setOrderStatus("PENDING_PAYMENT");
            }

            // 💳 Tạo bản ghi Payment
            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setAmount(total);
            payment.setMethod(paymentMethod);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setCreatedAt(LocalDateTime.now());
            order.setPayment(payment);

            Order saved = orderRepository.save(order);
            paymentRepository.save(payment);

            // 🧹 Xóa giỏ
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

            // ✅ Check tồn kho theo variant
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
            order.setOrderStatus("Pending");
            order.setCreatedAt(LocalDateTime.now());




            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setVariant(variant);          // 🔹 lưu biến thể
            orderItem.setQuantity(quantity);
            orderItem.setPrice(product.getFinalPrice());
            orderItem.recalc();

            order.addItem(orderItem);
            BigDecimal total = orderItem.getLineTotal();

            // 🎟 Áp dụng voucher
            total = applyVoucherIfValid(order, total, voucherCode);

            // 💰 Lưu tổng tiền
            order.setTotalAmount(total);

            // 🔻 Trừ tồn kho
            if (variant != null) {
                variant.setStock(variant.getStock() - quantity);
                product.updateTotalStock();
                productVariantRepository.save(variant);
            } else {
                product.setStock(product.getStock() - quantity);
                productRepository.save(product);
            }

            // 💳 Tạo payment
            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setAmount(total);
            payment.setMethod(paymentMethod);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setCreatedAt(LocalDateTime.now());
            order.setPayment(payment);

            Order savedOrder = orderRepository.save(order);
            paymentRepository.save(payment);

            return savedOrder;
        }



        /** Áp dụng voucher nếu hợp lệ */
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

            BigDecimal discount = calculateDiscount(total, v);

            // Không được vượt tổng
            if (discount.compareTo(total) > 0) discount = total;

            order.setVoucher(v);
            order.setDiscount(discount);

            // Giảm 1 lượt sử dụng
            v.setQuantity(v.getQuantity() - 1);
            voucherRepository.save(v);

            return total.subtract(discount);
        }


        /** Tính tiền giảm giá – hỗ trợ maxDiscountAmount */
        private BigDecimal calculateDiscount(BigDecimal total, Voucher v) {
            BigDecimal discount;
            if ("PERCENT".equalsIgnoreCase(v.getDiscountType())) {
                discount = total.multiply(v.getDiscountValue().divide(BigDecimal.valueOf(100)));
                // Giới hạn tối đa nếu có cài maxDiscountAmount
                if (v.getMaxDiscountAmount() != null && discount.compareTo(v.getMaxDiscountAmount()) > 0) {
                    discount = v.getMaxDiscountAmount();
                }
            } else {
                discount = v.getDiscountValue();
            }
            return discount;
        }

        @Transactional
        public void cancelOrder(Long orderId) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // chỉ cho phép hủy khi đang xử lý
            if (!order.getOrderStatus().equals("Pending") &&
                !order.getOrderStatus().equals("PENDING_PAYMENT")) {
                throw new RuntimeException("Không thể hủy đơn này!");
            }

            boolean wasQrPending = "PENDING_PAYMENT".equals(order.getOrderStatus());

            // 🔄 Nhả kho
            for (OrderItem item : order.getOrderItems()) {
                Product p = item.getProduct();
                ProductVariant variant = item.getVariant();
                int qty = item.getQuantity();

                if (wasQrPending) {
                    // Nhả reservedStock (QR chưa thanh toán)
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
                    // Hoàn kho thực (đơn COD đã trừ)
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

            order.setOrderStatus("Cancelled");
            orderRepository.save(order);
        }

        /**
         * ✅ Xác nhận thanh toán QR thành công → trừ kho chính thức
         */
        @Transactional
        public void confirmQrPayment(Long orderId, String performedBy) {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

            if (!"PENDING_PAYMENT".equals(order.getOrderStatus())) return;

            for (OrderItem item : order.getOrderItems()) {
                Product p = item.getProduct();
                ProductVariant variant = item.getVariant();
                int qty = item.getQuantity();

                if (variant != null) {
                    // Trừ stock chính thức, nhả reservedStock
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

            order.setOrderStatus("Confirmed");
            orderRepository.save(order);
        }


        // ===========================
        // 🔹 CRUD & QUERIES
        // ===========================

        public List<Order> getAllOrders() { return orderRepository.findAll(); }

        public List<Order> getOrdersByUser(Long userId) { return orderRepository.findByUser_Id(userId); }

        public Optional<Order> getOrderById(Long id) { return orderRepository.findById(id); }

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

        /** Doanh thu theo ngày */
        public BigDecimal getRevenueByDate(LocalDate date) {
            return orderRepository.sumRevenueByDate(date).orElse(BigDecimal.ZERO);
        }

        /** Số đơn theo ngày */
        public int countOrdersByDate(LocalDate date) {
            return orderRepository.countOrdersByDate(date);
        }

        /** Lấy N đơn gần nhất */
        public List<Order> getRecentOrders(int limit) {
            return orderRepository.findRecentOrders(limit);
        }

        public Page<Order> getPagedOrders(int page, int size) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            return orderRepository.findAll(pageable);
        }

        // ==========================================
        // 🔹 DATE RANGE STATS & FILTER (DASHBOARD CARDS)
        // ==========================================
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
            List<String> successStatuses = List.of("paid", "completed", "delivered", "shipping");
            return orderRepository.sumRevenueByDateRange(start, end, successStatuses).orElse(BigDecimal.ZERO);
        }

        public long countOrdersByDateRange(LocalDate startDate, LocalDate endDate) {
            if (startDate == null) startDate = LocalDate.now().minusDays(6);
            if (endDate == null) endDate = LocalDate.now();
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            return orderRepository.countOrdersByDateRange(start, end);
        }

        public long countOrdersByStatusesInRange(LocalDate startDate, LocalDate endDate, List<String> statuses) {
            if (startDate == null) startDate = LocalDate.now().minusDays(6);
            if (endDate == null) endDate = LocalDate.now();
            if (statuses == null || statuses.isEmpty()) return 0;
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            List<String> lowerStatuses = statuses.stream().map(String::toLowerCase).toList();
            return orderRepository.countOrdersByDateRangeAndStatuses(start, end, lowerStatuses);
        }

        /**
         * 🔹 Lọc đơn hàng theo Trạng thái & Khoảng ngày (Dành cho Clickable Cards Dashboard)
         */
        public List<Order> filterOrders(String statusGroup, LocalDate startDate, LocalDate endDate) {
            LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusYears(1).atStartOfDay();
            LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDate.now().atTime(23, 59, 59);

            List<Order> orders = orderRepository.findOrdersByDateRange(start, end);

            if (statusGroup == null || statusGroup.isBlank() || statusGroup.equalsIgnoreCase("all")) {
                return orders;
            }

            List<String> targetStatuses;
            if (statusGroup.equalsIgnoreCase("success")) {
                targetStatuses = List.of("paid", "completed", "delivered", "shipping");
            } else if (statusGroup.equalsIgnoreCase("pending")) {
                targetStatuses = List.of("pending", "processing", "pending_payment");
            } else if (statusGroup.equalsIgnoreCase("cancelled")) {
                targetStatuses = List.of("cancelled");
            } else {
                targetStatuses = List.of(statusGroup.toLowerCase());
            }

            return orders.stream()
                    .filter(o -> o.getOrderStatus() != null && targetStatuses.contains(o.getOrderStatus().toLowerCase()))
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

        // ==========================================
        // 🟢 GROSS PROFIT STATS (PHASE 2)
        // ==========================================

        /**
         * Tính tổng Giá vốn hàng bán (COGS) trong khoảng ngày.
         * COGS = SUM(orderItem.quantity * product.importPrice)
         */
        public BigDecimal getCogsInRange(LocalDate startDate, LocalDate endDate) {
            if (startDate == null) startDate = LocalDate.now().minusDays(6);
            if (endDate == null) endDate = LocalDate.now();
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            List<String> successStatuses = List.of("paid", "completed", "delivered", "shipping");

            // Lấy tất cả đơn thành công trong khoảng
            List<Order> orders = orderRepository.findOrdersByDateRange(start, end).stream()
                .filter(o -> successStatuses.contains(o.getOrderStatus().toLowerCase()))
                .toList();

            BigDecimal totalCogs = BigDecimal.ZERO;
            for (Order order : orders) {
                for (com.laptopstore.laptopstore.entity.OrderItem item : order.getOrderItems()) {
                    // Ưu tiên importPrice của variant, fallback về product
                    java.math.BigDecimal importPrice = null;
                    if (item.getVariant() != null && item.getVariant().getImportPrice() != null) {
                        importPrice = item.getVariant().getImportPrice();
                    } else if (item.getProduct() != null && item.getProduct().getImportPrice() != null) {
                        importPrice = item.getProduct().getImportPrice();
                    }
                    if (importPrice != null) {
                        totalCogs = totalCogs.add(importPrice.multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
                    }
                }
            }
            return totalCogs;
        }

        /**
         * Tổng tiền giảm giá (discount từ Voucher) trong khoảng ngày
         */
        public BigDecimal getTotalDiscountInRange(LocalDate startDate, LocalDate endDate) {
            if (startDate == null) startDate = LocalDate.now().minusDays(6);
            if (endDate == null) endDate = LocalDate.now();
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            List<String> successStatuses = List.of("paid", "completed", "delivered", "shipping");
            BigDecimal result = orderRepository.sumDiscountByDateRange(start, end, successStatuses);
            return result != null ? result : BigDecimal.ZERO;
        }

    }
