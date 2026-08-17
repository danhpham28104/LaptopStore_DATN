package com.techstore.techstore.Repository;

import com.techstore.techstore.entity.Payment;
import com.techstore.techstore.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findByOrder_Id(Long orderId);

    /**
     * Tìm payment theo order ID (Optional)
     */
    Optional<Payment> findByOrderId(Long orderId);

    /**
     * Tìm tất cả payment chưa hoàn tất (để check)
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Tìm tất cả payment chờ trong khoảng thời gian
     */
    @Query("""
        SELECT p FROM Payment p 
        WHERE p.status = :status 
        AND p.createdAt BETWEEN :startTime AND :endTime
    """)
    List<Payment> findPendingPaymentsBetween(
        @Param("status") PaymentStatus status,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Tìm payment hết hạn thanh toán
     */
    @Query("""
        SELECT p FROM Payment p 
        WHERE p.status = 'PENDING'
        AND p.order.paymentDeadline < :now
    """)
    List<Payment> findOverduePayments(@Param("now") LocalDateTime now);

    /**
     * Tìm theo transaction ID
     */
    Optional<Payment> findByTransactionId(String transactionId);
}

