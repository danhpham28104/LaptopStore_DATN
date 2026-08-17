package com.techstore.techstore.Repository;

import com.techstore.techstore.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder_Id(Long orderId);

    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.product.id = :productId AND LOWER(oi.order.orderStatus) IN ('paid', 'completed', 'delivered', 'shipping')")
    Optional<Long> sumSoldQuantityByProductId(@Param("productId") Long productId);
}
