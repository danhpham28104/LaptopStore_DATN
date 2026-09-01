package com.laptopstore.laptopstore.Repository;

import com.laptopstore.laptopstore.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCode(String code);
    boolean existsByCode(String code);

    @Query("""
    SELECT v FROM Voucher v
    WHERE v.active = true
      AND v.quantity > 0
""")
    List<Voucher> findAllAvailable();

    @Query("""
    SELECT v FROM Voucher v
    WHERE v.active = true
      AND v.quantity > 0
      AND (v.startDate IS NULL OR v.startDate <= :now)
      AND (v.endDate IS NULL OR v.endDate >= :now)
""")
    List<Voucher> findAllAvailableNow(@Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Voucher v SET v.quantity = v.quantity - 1 WHERE v.id = :id AND v.quantity > 0")
    int decrementQuantityIfAvailable(@Param("id") Long id);
}

