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

    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.product.id = :productId AND oi.order.orderStatus IN (com.laptopstore.laptopstore.enums.OrderStatus.CONFIRMED, com.laptopstore.laptopstore.enums.OrderStatus.PACKING, com.laptopstore.laptopstore.enums.OrderStatus.SHIPPING, com.laptopstore.laptopstore.enums.OrderStatus.DELIVERED)")
    Optional<Long> sumSoldQuantityByProductId(@Param("productId") Long productId);

    @Query("""
        SELECT COUNT(oi) > 0 FROM OrderItem oi 
        WHERE oi.order.user.id = :userId 
          AND oi.product.id = :productId 
          AND oi.order.orderStatus = com.laptopstore.laptopstore.enums.OrderStatus.DELIVERED
    """)
    boolean existsByUserIdAndProductIdAndDeliveredOrder(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("""
        SELECT oi FROM OrderItem oi 
        WHERE oi.order.user.id = :userId 
          AND oi.id = :orderItemId 
          AND oi.order.orderStatus = com.laptopstore.laptopstore.enums.OrderStatus.DELIVERED
    """)
    Optional<OrderItem> findDeliveredOrderItem(@Param("userId") Long userId, @Param("orderItemId") Long orderItemId);

    /** Tổng số sản phẩm đã bán trong khoảng ngày (chỉ tính đơn thành công) */
    @Query("""
        SELECT COALESCE(SUM(oi.quantity), 0)
        FROM OrderItem oi
        WHERE oi.order.createdAt >= :start AND oi.order.createdAt <= :end
          AND oi.order.orderStatus IN :statuses
    """)
    Long sumTotalItemsSold(
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end,
            @Param("statuses") List<com.laptopstore.laptopstore.enums.OrderStatus> statuses);

    /** Doanh thu và số lượng bán theo Danh Mục (Category) */
    @Query("""
        SELECT c.name, COALESCE(SUM(oi.lineTotal), 0), COALESCE(SUM(oi.quantity), 0), COUNT(DISTINCT oi.order.id)
        FROM OrderItem oi
        JOIN oi.product p
        JOIN p.category c
        WHERE oi.order.createdAt >= :start AND oi.order.createdAt <= :end
          AND oi.order.orderStatus IN :statuses
        GROUP BY c.id, c.name
        ORDER BY SUM(oi.lineTotal) DESC
    """)
    List<Object[]> findCategorySalesStats(
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end,
            @Param("statuses") List<com.laptopstore.laptopstore.enums.OrderStatus> statuses);

    /** Doanh thu và số lượng bán theo Thương Hiệu (Brand) */
    @Query("""
        SELECT b.name, COALESCE(SUM(oi.lineTotal), 0), COALESCE(SUM(oi.quantity), 0), COUNT(DISTINCT oi.order.id)
        FROM OrderItem oi
        JOIN oi.product p
        JOIN p.brand b
        WHERE oi.order.createdAt >= :start AND oi.order.createdAt <= :end
          AND oi.order.orderStatus IN :statuses
        GROUP BY b.id, b.name
        ORDER BY SUM(oi.lineTotal) DESC
    """)
    List<Object[]> findBrandSalesStats(
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end,
            @Param("statuses") List<com.laptopstore.laptopstore.enums.OrderStatus> statuses);

}
