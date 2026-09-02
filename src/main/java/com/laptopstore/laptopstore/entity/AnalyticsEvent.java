package com.laptopstore.laptopstore.entity;

import com.laptopstore.laptopstore.enums.EventType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity lưu mọi hành động của người dùng (cả đã đăng nhập lẫn guest).
 *
 * <p>Đây là nền tảng dữ liệu cho:
 * <ul>
 *   <li>Conversion Funnel Analysis</li>
 *   <li>AI Analytics (AI-assisted orders)</li>
 *   <li>Customer Behavior Analytics</li>
 *   <li>Product Performance</li>
 * </ul>
 *
 * <p>Quy tắc track:
 * <ul>
 *   <li>Ghi event KHÔNG ĐỒNG BỘ (async) — không block request chính.</li>
 *   <li>sessionId dùng để theo dõi guest và funnel.</li>
 *   <li>userId null nếu guest.</li>
 *   <li>Chống spam: mỗi (sessionId, eventType, productId) không ghi quá 1 lần trong 60 giây.</li>
 * </ul>
 */
@Entity
@Table(
    name = "analytics_event",
    indexes = {
        @Index(name = "idx_ae_created_at",  columnList = "created_at"),
        @Index(name = "idx_ae_event_type",  columnList = "event_type"),
        @Index(name = "idx_ae_user_id",     columnList = "user_id"),
        @Index(name = "idx_ae_session_id",  columnList = "session_id"),
        @Index(name = "idx_ae_product_id",  columnList = "product_id"),
        @Index(name = "idx_ae_order_id",    columnList = "order_id")
    }
)
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Session ID — theo dõi cả guest lẫn logged-in user trong cùng 1 phiên.
     * Lấy từ HttpSession.getId().
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * ID người dùng đã đăng nhập. Null nếu guest.
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Loại sự kiện — không thể null.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 50, nullable = false)
    private EventType eventType;

    /**
     * ID sản phẩm liên quan (nullable — một số event không liên quan đến product).
     */
    @Column(name = "product_id")
    private Long productId;

    /**
     * ID biến thể sản phẩm (nullable).
     */
    @Column(name = "variant_id")
    private Long variantId;

    /**
     * ID đơn hàng (nullable — chỉ dùng cho ORDER_CREATED, PAYMENT_SUCCESS,...).
     */
    @Column(name = "order_id")
    private Long orderId;

    /**
     * JSON tự do để lưu metadata bổ sung.
     * Ví dụ: { "keyword": "laptop gaming", "resultCount": 15 }
     */
    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    /**
     * IP của client (nullable — dùng để chống spam và debugging).
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AnalyticsEvent() {}

    // ── Builder-style factory ────────────────────────────────────────────────

    public static AnalyticsEvent of(EventType type, String sessionId, Long userId) {
        AnalyticsEvent e = new AnalyticsEvent();
        e.eventType  = type;
        e.sessionId  = sessionId;
        e.userId     = userId;
        return e;
    }

    public AnalyticsEvent product(Long productId) {
        this.productId = productId;
        return this;
    }

    public AnalyticsEvent variant(Long variantId) {
        this.variantId = variantId;
        return this;
    }

    public AnalyticsEvent order(Long orderId) {
        this.orderId = orderId;
        return this;
    }

    public AnalyticsEvent metadata(String json) {
        this.metadataJson = json;
        return this;
    }

    public AnalyticsEvent ip(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
