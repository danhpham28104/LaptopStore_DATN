package com.techstore.techstore.Repository;

import com.techstore.techstore.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser_Id(Long userId);

    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findByUser_IdOrderByCreatedAtDesc(Long userId);

    // Lấy chi tiết 1 đơn: kèm items + product để render Thymeleaf không bị lazy
    @Query("""
      select o from Order o
      left join fetch o.orderItems oi
      left join fetch oi.product
      where o.id = :id
    """)
    Optional<Order> findDetail(@Param("id") Long id);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE DATE(o.createdAt) = :date")
    Optional<BigDecimal> sumRevenueByDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(o) FROM Order o WHERE DATE(o.createdAt) = :date")
    int countOrdersByDate(@Param("date") LocalDate date);


    // 5 đơn gần nhất
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC LIMIT :limit")
    List<Order> findRecentOrders(@Param("limit") int limit);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.createdAt >= :start AND o.createdAt < :end AND o.orderStatus = 'Completed'")
    BigDecimal sumRevenueByDate(LocalDateTime start, LocalDateTime end);


}
