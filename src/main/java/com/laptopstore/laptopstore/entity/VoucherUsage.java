package com.laptopstore.laptopstore.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lưu lịch sử sử dụng voucher — source of truth cho Voucher Analytics.
 *
 * <p>Tại sao cần entity riêng thay vì chỉ dùng Order.voucher?
 * <ul>
 *   <li>Biết chính xác voucher nào được dùng, ai dùng, đơn nào dùng, giảm bao nhiêu tiền.</li>
 *   <li>Hỗ trợ analytics: usage count, discount amount, revenue generated.</li>
 *   <li>UNIQUE(order_id) đảm bảo mỗi order chỉ có 1 voucher usage record.</li>
 * </ul>
 */
@Entity
@Table(
    name = "voucher_usage",
    indexes = {
        @Index(name = "idx_vu_voucher_id", columnList = "voucher_id"),
        @Index(name = "idx_vu_user_id",    columnList = "user_id"),
        @Index(name = "idx_vu_used_at",    columnList = "used_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_vu_order_id", columnNames = "order_id")
    }
)
public class VoucherUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Số tiền thực tế được giảm (đã tính theo maxDiscountAmount nếu có).
     */
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @CreationTimestamp
    @Column(name = "used_at", nullable = false, updatable = false)
    private LocalDateTime usedAt;

    public VoucherUsage() {}

    public VoucherUsage(Voucher voucher, User user, Order order, BigDecimal discountAmount) {
        this.voucher        = voucher;
        this.user           = user;
        this.order          = order;
        this.discountAmount = discountAmount;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Voucher getVoucher() { return voucher; }
    public void setVoucher(Voucher voucher) { this.voucher = voucher; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public LocalDateTime getUsedAt() { return usedAt; }
}
