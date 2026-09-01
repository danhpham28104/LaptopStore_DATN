package com.laptopstore.laptopstore.Repository;

import com.laptopstore.laptopstore.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findByOrderIdOrderByChangedAtAsc(Long orderId);

    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    Optional<OrderStatusHistory> findFirstByOrderIdAndNewStatusOrderByCreatedAtDesc(Long orderId, String newStatus);
}
