package com.laptopstore.laptopstore.config;

import com.laptopstore.laptopstore.Repository.OrderRepository;
import com.laptopstore.laptopstore.Service.OrderService;
import com.laptopstore.laptopstore.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler tự động hủy đơn hàng QR quá hạn thanh toán (> 15 phút).
 * Chạy mỗi 60 giây để quét và nhả kho tương ứng.
 */
@Component
@EnableScheduling
public class InventoryReservationScheduler {

    private static final Logger log = LoggerFactory.getLogger(InventoryReservationScheduler.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    /**
     * Chạy mỗi 60 giây: Tìm và hủy các đơn PENDING_PAYMENT đã quá hạn.
     */
    @Scheduled(fixedRate = 60_000)
    public void releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();

        // Lấy tất cả đơn PENDING_PAYMENT có paymentDeadline < now
        List<Order> expiredOrders = orderRepository
            .findByOrderStatusAndPaymentDeadlineBefore("PENDING_PAYMENT", now);

        if (expiredOrders.isEmpty()) return;

        log.info("[InventoryScheduler] Tìm thấy {} đơn QR quá hạn – đang hủy...", expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                orderService.cancelOrder(order.getId());
                log.info("[InventoryScheduler] Đã hủy đơn {} và nhả kho.", order.getOrderCode());
            } catch (Exception e) {
                log.error("[InventoryScheduler] Lỗi khi hủy đơn {}: {}", order.getOrderCode(), e.getMessage());
            }
        }
    }
}
