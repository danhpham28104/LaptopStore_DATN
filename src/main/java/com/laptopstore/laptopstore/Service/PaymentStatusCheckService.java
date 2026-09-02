package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.OrderRepository;
import com.laptopstore.laptopstore.Repository.PaymentRepository;
import com.laptopstore.laptopstore.entity.Order;
import com.laptopstore.laptopstore.entity.Payment;
import com.laptopstore.laptopstore.enums.OrderStatus;
import com.laptopstore.laptopstore.enums.PaymentStatus;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Payment Status Check Service - Kiểm tra trạng thái thanh toán định kỳ
 * 
 * Chức năng:
 * - Kiểm tra payment chưa hoàn tất sau 30 giây
 * - Xóa order hết hạn thanh toán
 * - Đồng bộ với webhook từ payment provider
 */
@Service
public class PaymentStatusCheckService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentStatusCheckService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    /**
     * 🔄 Scheduled Task: Kiểm tra trạng thái thanh toán mỗi 30 giây
     * 
     * Dùng @Scheduled để tự động poll payment status từ payment provider
     * Nếu có webhook, không cần scheduled task, chỉ cần webhook endpoint
     */
    @Scheduled(fixedDelay = 30000) // 30 giây
    @Transactional
    public void checkPendingPayments() {
        logger.info("🔄 Checking pending payments...");

        try {
            // 🔹 Tìm tất cả payment chưa hoàn tất
            List<Payment> pendingPayments = paymentRepository.findByStatus(PaymentStatus.PENDING);

            for (Payment payment : pendingPayments) {
                checkPaymentStatusForOrder(payment);
            }

            logger.info("✅ Payment check completed. Found {} pending payments", pendingPayments.size());

        } catch (Exception e) {
            logger.error("❌ Error checking payments: {}", e.getMessage(), e);
        }
    }

    /**
     * 🗑️ Scheduled Task: Xóa order hết hạn thanh toán (15 phút)
     */
    @Scheduled(fixedDelay = 60000) // Mỗi 1 phút
    @Transactional
    public void cleanupOverduePayments() {
        logger.info("🗑️  Cleaning up overdue payments...");

        try {
            LocalDateTime now = LocalDateTime.now();
            List<Payment> overduePayments = paymentRepository.findOverduePayments(now);

            for (Payment payment : overduePayments) {
                Order order = payment.getOrder();
                if (order != null && order.getOrderStatus() != OrderStatus.CANCELLED) {
                    try {
                        orderService.cancelOrder(order.getId());
                    } catch (Exception ex) {
                        logger.warn("Order {} already cancelled or cannot be cancelled: {}", order.getOrderCode(), ex.getMessage());
                    }
                    order.setOrderStatus(OrderStatus.CANCELLED);
                    payment.setStatus(PaymentStatus.FAILED);

                    orderRepository.save(order);
                    paymentRepository.save(payment);

                    logger.info("⏰ Marked order {} as timeout & restored stock", order.getOrderCode());
                }
            }

            logger.info("✅ Cleanup completed. Found {} overdue payments", overduePayments.size());

        } catch (Exception e) {
            logger.error("❌ Error cleaning up payments: {}", e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra trạng thái thanh toán cho một order cụ thể
     */
    @Transactional
    public void checkPaymentStatusForOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null && order.getPayment() != null) {
            checkPaymentStatusForOrder(order.getPayment());
        }
    }

    /**
     * Helper: Kiểm tra trạng thái thanh toán cho một payment
     */
    @Transactional
    private void checkPaymentStatusForOrder(Payment payment) {
        if (payment.getStatus() != null && payment.getStatus().isPaid()) {
            // Đã thanh toán rồi, không cần check
            return;
        }

        Order order = payment.getOrder();
        if (order == null) {
            return;
        }

        // 🔹 TODO: Gọi API ngân hàng/payment provider để kiểm tra
        // Hiện tại là mock - trong thực tế gọi SEPAY API hoặc VNPay API

        logger.debug("Checking payment status for order: {}", order.getOrderCode());

        // Nếu payment đã được xác nhận thành công từ provider
        if (isPaymentConfirmed(payment)) {
            payment.setStatus(PaymentStatus.PAID);
            order.setOrderStatus(OrderStatus.CONFIRMED);

            paymentRepository.save(payment);
            orderRepository.save(order);

            logger.info("✅ Order {} marked as PAID", order.getOrderCode());
        }
    }

    /**
     * Helper: Kiểm tra xem payment đã được xác nhận hay chưa
     * 
     * Thay thế bằng API gọi payment provider thật
     */
    private boolean isPaymentConfirmed(Payment payment) {
        // 🔹 Mock: Nếu transactionId là "TEST_SUCCESS" thì coi như đã thanh toán
        String transactionId = payment.getTransactionId();
        if (transactionId != null && transactionId.contains("TEST_SUCCESS")) {
            return true;
        }

        return false;
    }

    /**
     * Webhook endpoint callback (được gọi từ payment provider)
     * 
     * Nếu payment provider (SEPAY, VNPay...) support webhook,
     * không cần scheduled task, chỉ cần endpoint này
     */
    @Transactional
    public void handlePaymentWebhook(String orderCode, String transactionId, String status, long amount) {
        logger.info("🪝 Webhook received: orderCode={}, status={}, transactionId={}", 
                orderCode, status, transactionId);

        try {
            // 🔹 Tìm order theo orderCode
            Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
            if (order == null) {
                logger.warn("⚠️  Order not found: {}", orderCode);
                return;
            }

            Payment payment = order.getPayment();
            if (payment == null) {
                logger.warn("⚠️  Payment not found for order: {}", orderCode);
                return;
            }

            // 🔹 Cập nhật payment status dựa trên webhook
            if ("SUCCESS".equalsIgnoreCase(status)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setTransactionId(transactionId);
                order.setOrderStatus(OrderStatus.CONFIRMED);

                logger.info("✅ Payment confirmed via webhook for order: {}", orderCode);
            } else if ("FAILED".equalsIgnoreCase(status)) {
                payment.setStatus(PaymentStatus.FAILED);
                order.setOrderStatus(OrderStatus.CANCELLED);

                logger.warn("❌ Payment failed for order: {}", orderCode);
            }

            paymentRepository.save(payment);
            orderRepository.save(order);

        } catch (Exception e) {
            logger.error("❌ Error processing webhook: {}", e.getMessage(), e);
        }
    }
}
