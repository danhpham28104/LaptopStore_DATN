package com.laptopstore.laptopstore.Repository;

import com.laptopstore.laptopstore.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {

    Optional<VoucherUsage> findByOrder_Id(Long orderId);

    List<VoucherUsage> findByVoucher_Id(Long voucherId);

    List<VoucherUsage> findByUser_Id(Long userId);

    boolean existsByVoucher_IdAndUser_Id(Long voucherId, Long userId);

    long countByVoucher_Id(Long voucherId);

    long countByVoucher_IdAndUser_Id(Long voucherId, Long userId);

    /** Tổng discount amount của một voucher trong khoảng ngày. */
    @Query("""
        SELECT COALESCE(SUM(vu.discountAmount), 0)
        FROM VoucherUsage vu
        WHERE vu.voucher.id = :voucherId
          AND vu.usedAt BETWEEN :from AND :to
        """)
    BigDecimal sumDiscountByVoucherAndDateRange(
            @Param("voucherId") Long voucherId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Tổng discount của tất cả voucher trong kỳ — để tính chi phí khuyến mãi. */
    @Query("""
        SELECT COALESCE(SUM(vu.discountAmount), 0)
        FROM VoucherUsage vu
        WHERE vu.usedAt BETWEEN :from AND :to
        """)
    BigDecimal sumTotalDiscountInDateRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Voucher analytics: GROUP BY voucher với số lần dùng, tổng discount.
     * Result: [voucherId, voucherCode, usageCount, totalDiscount]
     */
    @Query("""
        SELECT vu.voucher.id, vu.voucher.code,
               COUNT(vu.id), COALESCE(SUM(vu.discountAmount), 0)
        FROM VoucherUsage vu
        WHERE vu.usedAt BETWEEN :from AND :to
        GROUP BY vu.voucher.id, vu.voucher.code
        ORDER BY COUNT(vu.id) DESC
        """)
    List<Object[]> findVoucherUsageStats(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
