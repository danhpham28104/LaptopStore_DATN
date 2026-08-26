package com.laptopstore.laptopstore.Repository;

import com.laptopstore.laptopstore.entity.OrderItem;
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

    @Query("""
        SELECT COUNT(oi) > 0 FROM OrderItem oi 
        WHERE oi.order.user.id = :userId 
          AND oi.product.id = :productId 
          AND UPPER(oi.order.orderStatus) IN ('DELIVERED', 'COMPLETED')
    """)
    boolean existsByUserIdAndProductIdAndDeliveredOrder(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("""
        SELECT oi FROM OrderItem oi 
        WHERE oi.order.user.id = :userId 
          AND oi.id = :orderItemId 
          AND UPPER(oi.order.orderStatus) IN ('DELIVERED', 'COMPLETED')
    """)
    Optional<OrderItem> findDeliveredOrderItem(@Param("userId") Long userId, @Param("orderItemId") Long orderItemId);
}
