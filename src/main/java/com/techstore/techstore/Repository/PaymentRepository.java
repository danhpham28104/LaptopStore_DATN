package com.techstore.techstore.Repository;

import com.techstore.techstore.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findByOrder_Id(Long orderId);
}
