    package com.techstore.techstore.Service;

    import com.techstore.techstore.Repository.*;
    import com.techstore.techstore.entity.*;
    import com.techstore.techstore.enums.PaymentMethod;
    import com.techstore.techstore.enums.PaymentStatus;
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




        @Transactional
        public void updateStatus(Long id, String newStatus) {
            orderRepository.findById(id).ifPresent(order -> {
                order.setOrderStatus(newStatus);
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

                // 🔻 Trừ tồn kho
                if (variant != null) {
                    variant.setStock(variant.getStock() - reqQty);
                    p.updateTotalStock();
                    productVariantRepository.save(variant);
                } else {
                    p.setStock(p.getStock() - reqQty);
                    productRepository.save(p);
                }
            }

            // 🎟 Áp dụng voucher
            total = applyVoucherIfValid(order, total, voucherCode);

            // 💰 Lưu tổng tiền
            order.setTotalAmount(total);

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


        /** Tính tiền giảm giá */
        private BigDecimal calculateDiscount(BigDecimal total, Voucher v) {
            if ("PERCENT".equalsIgnoreCase(v.getDiscountType())) {
                return total.multiply(v.getDiscountValue().divide(BigDecimal.valueOf(100)));
            } else {
                return v.getDiscountValue();
            }
        }

        @Transactional
        public void cancelOrder(Long orderId) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // chỉ cho phép hủy khi đang xử lý
            if (!order.getOrderStatus().equals("Pending")) {
                throw new RuntimeException("Không thể hủy đơn này!");
            }

            order.setOrderStatus("Cancelled");
            orderRepository.save(order);
        }


        // ===========================
        // 🔹 CRUD
        // ===========================

        public List<Order> getAllOrders() { return orderRepository.findAll(); }

        public List<Order> getOrdersByUser(Long userId) { return orderRepository.findByUser_Id(userId); }

        public Optional<Order> getOrderById(Long id) { return orderRepository.findById(id); }

        public void deleteOrder(Long id) { orderRepository.deleteById(id); }

        public Order saveOrder(Order order) { return orderRepository.save(order); }

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

        public Order getByOrderCode(String code) {
            return orderRepository.findByOrderCode(code)
                    .orElse(null);
        }
        private String generateOrderCode() {
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            int random = (int)(Math.random() * 9000) + 1000; // random 4 số
            return "DH" + date + "-" + random;
        }

    }
