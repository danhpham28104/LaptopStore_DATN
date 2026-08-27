package com.laptopstore.laptopstore.Repository;

import com.laptopstore.laptopstore.entity.StockLog;
import com.laptopstore.laptopstore.enums.StockLogType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockLogRepository extends JpaRepository<StockLog, Long> {

    List<StockLog> findByProduct_IdOrderByCreatedAtDesc(Long productId);

    Page<StockLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT sl FROM StockLog sl WHERE " +
           "(:productId IS NULL OR sl.product.id = :productId) AND " +
           "(:type IS NULL OR sl.type = :type) AND " +
           "(:from IS NULL OR sl.createdAt >= :from) AND " +
           "(:to IS NULL OR sl.createdAt <= :to) " +
           "ORDER BY sl.createdAt DESC")
    Page<StockLog> filterLogs(
        @Param("productId") Long productId,
        @Param("type") StockLogType type,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );
}
