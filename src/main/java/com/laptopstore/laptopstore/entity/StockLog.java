package com.laptopstore.laptopstore.entity;

import com.laptopstore.laptopstore.enums.StockLogType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity lưu lịch sử biến động kho hàng.
 * Ghi lại mọi thao tác nhập/xuất/khoá/hoàn kho.
 */
@Entity
@Table(name = "stock_log",
    indexes = {
        @Index(name = "idx_stock_log_product", columnList = "product_id"),
        @Index(name = "idx_stock_log_created_at", columnList = "created_at")
    }
)
public class StockLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sản phẩm liên quan
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Biến thể (nếu có)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    // Đơn hàng liên quan (nếu có)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    // Loại biến động kho
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StockLogType type;

    // Số lượng thay đổi (dương = nhập/hoàn, âm = xuất/khoá)
    @Column(nullable = false)
    private Integer quantity;

    // Tồn kho sau khi thay đổi (snapshot)
    @Column(name = "stock_after")
    private Integer stockAfter;

    // Người thực hiện (username hoặc "SYSTEM")
    @Column(length = 100)
    private String performedBy;

    // Ghi chú thêm
    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ===== Constructors =====
    public StockLog() {}

    public StockLog(Product product, ProductVariant variant, Order order,
                    StockLogType type, Integer quantity, Integer stockAfter,
                    String performedBy, String note) {
        this.product = product;
        this.variant = variant;
        this.order = order;
        this.type = type;
        this.quantity = quantity;
        this.stockAfter = stockAfter;
        this.performedBy = performedBy;
        this.note = note;
    }

    // ===== Getters & Setters =====
    public Long getId() { return id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public StockLogType getType() { return type; }
    public void setType(StockLogType type) { this.type = type; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getStockAfter() { return stockAfter; }
    public void setStockAfter(Integer stockAfter) { this.stockAfter = stockAfter; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
