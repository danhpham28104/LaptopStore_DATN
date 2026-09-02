package com.laptopstore.laptopstore.Repository;

import com.laptopstore.laptopstore.entity.AnalyticsEvent;
import com.laptopstore.laptopstore.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    // ── Chống spam: kiểm tra event trùng gần đây ────────────────────────────

    /**
     * Kiểm tra đã có event cùng loại của session + product trong khoảng thời gian gần đây chưa.
     * Dùng để chống spam PRODUCT_VIEW khi user refresh nhiều lần.
     */
    boolean existsBySessionIdAndEventTypeAndProductIdAndCreatedAtAfter(
            String sessionId, EventType eventType, Long productId, LocalDateTime after);

    boolean existsBySessionIdAndEventTypeAndCreatedAtAfter(
            String sessionId, EventType eventType, LocalDateTime after);

    // ── Conversion Funnel ────────────────────────────────────────────────────

    /**
     * Đếm số phiên (session) DISTINCT có event thuộc loại type trong khoảng thời gian.
     * Dùng cho conversion funnel: Views → Cart → Checkout → Order → Payment.
     */
    @Query("""
        SELECT COUNT(DISTINCT ae.sessionId)
        FROM AnalyticsEvent ae
        WHERE ae.eventType = :eventType
          AND ae.createdAt BETWEEN :from AND :to
        """)
    long countDistinctSessionsByEventType(
            @Param("eventType") EventType eventType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // ── Product Analytics ────────────────────────────────────────────────────

    /** Đếm lượt xem (sessions unique) của sản phẩm cụ thể. */
    @Query("""
        SELECT COUNT(DISTINCT ae.sessionId)
        FROM AnalyticsEvent ae
        WHERE ae.eventType = 'PRODUCT_VIEW'
          AND ae.productId = :productId
          AND ae.createdAt BETWEEN :from AND :to
        """)
    long countUniqueViewsByProduct(
            @Param("productId") Long productId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
        SELECT COUNT(ae)
        FROM AnalyticsEvent ae
        WHERE ae.eventType = 'ADD_TO_CART'
          AND ae.productId = :productId
          AND ae.createdAt BETWEEN :from AND :to
        """)
    long countCartAddsByProduct(
            @Param("productId") Long productId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // ── AI Analytics ─────────────────────────────────────────────────────────

    /** Tổng số cuộc chat AI (mỗi tin nhắn = 1 event). */
    @Query("""
        SELECT COUNT(ae) FROM AnalyticsEvent ae
        WHERE ae.eventType = :eventType
          AND ae.createdAt BETWEEN :from AND :to
        """)
    long countByEventTypeAndDateRange(
            @Param("eventType") EventType eventType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Unique AI users trong kỳ. */
    @Query("""
        SELECT COUNT(DISTINCT ae.sessionId)
        FROM AnalyticsEvent ae
        WHERE ae.eventType = 'AI_CHAT'
          AND ae.createdAt BETWEEN :from AND :to
        """)
    long countUniqueAiSessions(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Các session có AI chat — dùng để xác định AI-assisted orders. */
    @Query("""
        SELECT DISTINCT ae.sessionId
        FROM AnalyticsEvent ae
        WHERE ae.eventType = 'AI_CHAT'
          AND ae.createdAt BETWEEN :from AND :to
        """)
    List<String> findSessionIdsWithAiChat(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // ── Search Analytics ─────────────────────────────────────────────────────

    /** Đếm event theo loại trong khoảng ngày — query chung. */
    @Query("""
        SELECT COUNT(ae) FROM AnalyticsEvent ae
        WHERE ae.eventType = :eventType
          AND ae.createdAt BETWEEN :from AND :to
        """)
    long countEvents(
            @Param("eventType") EventType eventType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    List<AnalyticsEvent> findByOrderId(Long orderId);

    List<AnalyticsEvent> findByUserId(Long userId);

    List<AnalyticsEvent> findBySessionId(String sessionId);

    List<AnalyticsEvent> findBySessionIdAndEventTypeOrderByCreatedAtDesc(
            String sessionId, EventType eventType);
}
