package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.AnalyticsEventRepository;
import com.laptopstore.laptopstore.entity.AnalyticsEvent;
import com.laptopstore.laptopstore.enums.EventType;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service thu thập analytics events KHÔNG ĐỒNG BỘ (async).
 *
 * <p>Mỗi method track() chạy trong thread riêng biệt (@Async), không block request chính.
 * Nếu ghi event thất bại, hệ thống chỉ log warning, không ném exception ra ngoài.
 *
 * <p>Chống spam:
 * <ul>
 *   <li>PRODUCT_VIEW: không ghi nếu cùng session đã xem cùng product trong 60 giây.</li>
 *   <li>BEGIN_CHECKOUT, ADD_TO_CART: không ghi nếu trùng session trong 5 giây.</li>
 *   <li>ORDER_CREATED, PAYMENT_SUCCESS: luôn ghi (idempotency đảm bảo bằng orderId).</li>
 * </ul>
 */
@Service
public class AnalyticsEventService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventService.class);

    /** Khoảng thời gian chống spam PRODUCT_VIEW (giây). */
    private static final int PRODUCT_VIEW_COOLDOWN_SECONDS = 60;

    /** Khoảng thời gian chống spam các event khác (giây). */
    private static final int DEFAULT_COOLDOWN_SECONDS = 5;

    private final AnalyticsEventRepository analyticsEventRepository;

    public AnalyticsEventService(AnalyticsEventRepository analyticsEventRepository) {
        this.analyticsEventRepository = analyticsEventRepository;
    }

    // ── Public track methods ─────────────────────────────────────────────────

    /**
     * Track xem sản phẩm.
     * Chống spam: 1 session + 1 product chỉ ghi 1 lần trong 60 giây.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackProductView(String sessionId, Long userId, Long productId, String ip) {
        try {
            if (isDuplicateProductEvent(sessionId, EventType.PRODUCT_VIEW, productId, PRODUCT_VIEW_COOLDOWN_SECONDS)) {
                return;
            }
            save(AnalyticsEvent.of(EventType.PRODUCT_VIEW, sessionId, userId)
                    .product(productId)
                    .ip(ip));
        } catch (Exception e) {
            log.warn("[Analytics] trackProductView failed: {}", e.getMessage());
        }
    }

    /** Track thêm vào giỏ hàng. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackAddToCart(String sessionId, Long userId, Long productId, Long variantId, String ip) {
        try {
            save(AnalyticsEvent.of(EventType.ADD_TO_CART, sessionId, userId)
                    .product(productId)
                    .variant(variantId)
                    .ip(ip));
        } catch (Exception e) {
            log.warn("[Analytics] trackAddToCart failed: {}", e.getMessage());
        }
    }

    /** Track xóa khỏi giỏ hàng. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackRemoveFromCart(String sessionId, Long userId, Long productId, Long variantId) {
        try {
            save(AnalyticsEvent.of(EventType.REMOVE_FROM_CART, sessionId, userId)
                    .product(productId)
                    .variant(variantId));
        } catch (Exception e) {
            log.warn("[Analytics] trackRemoveFromCart failed: {}", e.getMessage());
        }
    }

    /** Track bắt đầu checkout (vào trang checkout). */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackBeginCheckout(String sessionId, Long userId, String ip) {
        try {
            if (isDuplicateSessionEvent(sessionId, EventType.BEGIN_CHECKOUT, DEFAULT_COOLDOWN_SECONDS)) {
                return;
            }
            save(AnalyticsEvent.of(EventType.BEGIN_CHECKOUT, sessionId, userId).ip(ip));
        } catch (Exception e) {
            log.warn("[Analytics] trackBeginCheckout failed: {}", e.getMessage());
        }
    }

    /** Track tạo đơn hàng thành công. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackOrderCreated(String sessionId, Long userId, Long orderId, String ip) {
        try {
            save(AnalyticsEvent.of(EventType.ORDER_CREATED, sessionId, userId)
                    .order(orderId)
                    .ip(ip));
        } catch (Exception e) {
            log.warn("[Analytics] trackOrderCreated failed: {}", e.getMessage());
        }
    }

    /** Track hủy đơn hàng. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackOrderCancelled(String sessionId, Long userId, Long orderId) {
        try {
            save(AnalyticsEvent.of(EventType.ORDER_CANCELLED, sessionId, userId).order(orderId));
        } catch (Exception e) {
            log.warn("[Analytics] trackOrderCancelled failed: {}", e.getMessage());
        }
    }

    /** Track thanh toán thành công — ghi từ webhook. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackPaymentSuccess(Long orderId) {
        try {
            save(AnalyticsEvent.of(EventType.PAYMENT_SUCCESS, null, null).order(orderId));
        } catch (Exception e) {
            log.warn("[Analytics] trackPaymentSuccess failed: {}", e.getMessage());
        }
    }

    /** Track thanh toán thất bại. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackPaymentFailed(Long orderId) {
        try {
            save(AnalyticsEvent.of(EventType.PAYMENT_FAILED, null, null).order(orderId));
        } catch (Exception e) {
            log.warn("[Analytics] trackPaymentFailed failed: {}", e.getMessage());
        }
    }

    /** Track áp dụng voucher. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackApplyVoucher(String sessionId, Long userId, String voucherCode, boolean success) {
        try {
            String meta = String.format("{\"voucherCode\":\"%s\",\"success\":%b}", voucherCode, success);
            save(AnalyticsEvent.of(EventType.APPLY_VOUCHER, sessionId, userId).metadata(meta));
        } catch (Exception e) {
            log.warn("[Analytics] trackApplyVoucher failed: {}", e.getMessage());
        }
    }

    /** Track click sản phẩm được AI gợi ý. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackAiProductClick(String sessionId, Long userId, Long productId) {
        try {
            save(AnalyticsEvent.of(EventType.AI_PRODUCT_CLICK, sessionId, userId).product(productId));
        } catch (Exception e) {
            log.warn("[Analytics] trackAiProductClick failed: {}", e.getMessage());
        }
    }

    /** Track chat với AI. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackAiChat(String sessionId, Long userId, String ip) {
        try {
            save(AnalyticsEvent.of(EventType.AI_CHAT, sessionId, userId).ip(ip));
        } catch (Exception e) {
            log.warn("[Analytics] trackAiChat failed: {}", e.getMessage());
        }
    }

    /** Track AI gợi ý sản phẩm. metadata: danh sách productId được recommend. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackAiProductRecommended(String sessionId, Long userId, String recommendedProductIdsJson) {
        try {
            String meta = String.format("{\"recommendedProductIds\":%s}", recommendedProductIdsJson);
            save(AnalyticsEvent.of(EventType.AI_PRODUCT_RECOMMENDED, sessionId, userId).metadata(meta));
        } catch (Exception e) {
            log.warn("[Analytics] trackAiProductRecommended failed: {}", e.getMessage());
        }
    }

    /** Track thêm vào / xóa khỏi wishlist. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackWishlist(String sessionId, Long userId, Long productId, boolean isAdd) {
        try {
            EventType type = isAdd ? EventType.ADD_TO_WISHLIST : EventType.REMOVE_FROM_WISHLIST;
            save(AnalyticsEvent.of(type, sessionId, userId).product(productId));
        } catch (Exception e) {
            log.warn("[Analytics] trackWishlist failed: {}", e.getMessage());
        }
    }

    /** Track tìm kiếm sản phẩm. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackSearch(String sessionId, Long userId, String keyword, int resultCount) {
        try {
            String meta = String.format("{\"keyword\":\"%s\",\"resultCount\":%d}",
                    keyword != null ? keyword.replace("\"", "'") : "", resultCount);
            save(AnalyticsEvent.of(EventType.SEARCH, sessionId, userId).metadata(meta));
        } catch (Exception e) {
            log.warn("[Analytics] trackSearch failed: {}", e.getMessage());
        }
    }

    /** Track viết đánh giá sản phẩm. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackReviewCreated(String sessionId, Long userId, Long productId) {
        try {
            save(AnalyticsEvent.of(EventType.REVIEW_CREATED, sessionId, userId).product(productId));
        } catch (Exception e) {
            log.warn("[Analytics] trackReviewCreated failed: {}", e.getMessage());
        }
    }

    // ── Helper utilities ─────────────────────────────────────────────────────

    /**
     * Trích xuất session ID an toàn từ HTTP request.
     * Trả về null nếu không có session (không tạo session mới).
     */
    public static String extractSessionId(HttpServletRequest request) {
        if (request == null) return null;
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        return session != null ? session.getId() : null;
    }

    /**
     * Trích xuất IP của client (hỗ trợ proxy/load balancer).
     */
    public static String extractClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void save(AnalyticsEvent event) {
        analyticsEventRepository.save(event);
    }

    /** Kiểm tra trùng PRODUCT_VIEW (session + eventType + productId). */
    private boolean isDuplicateProductEvent(String sessionId, EventType type, Long productId, int cooldownSeconds) {
        if (sessionId == null || productId == null) return false;
        LocalDateTime since = LocalDateTime.now().minusSeconds(cooldownSeconds);
        return analyticsEventRepository
                .existsBySessionIdAndEventTypeAndProductIdAndCreatedAtAfter(sessionId, type, productId, since);
    }

    /** Kiểm tra trùng event theo session (không phân biệt product). */
    private boolean isDuplicateSessionEvent(String sessionId, EventType type, int cooldownSeconds) {
        if (sessionId == null) return false;
        LocalDateTime since = LocalDateTime.now().minusSeconds(cooldownSeconds);
        return analyticsEventRepository
                .existsBySessionIdAndEventTypeAndCreatedAtAfter(sessionId, type, since);
    }
}
