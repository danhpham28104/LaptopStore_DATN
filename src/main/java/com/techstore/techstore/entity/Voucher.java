package com.techstore.techstore.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "voucher")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mã voucher (duy nhất)
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    // Giảm theo phần trăm (ví dụ 10%) hoặc theo tiền (ví dụ 100000)
    @Column(precision = 10, scale = 2)
    private BigDecimal discountValue;

    // Kiểu giảm: "PERCENT" hoặc "AMOUNT"
    @Column(length = 20, nullable = false)
    private String discountType;

    // Giá trị đơn hàng tối thiểu để áp dụng
    @Column(precision = 10, scale = 2)
    private BigDecimal minOrderValue;

    // Ngày bắt đầu - kết thúc
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Số lượng còn lại
    @Column(nullable = false)
    private Integer quantity = 0;

    // Trạng thái hoạt động
    @Column(nullable = false)
    private Boolean active = true;

    // Mô tả ngắn
    @Column(length = 255)
    private String description;

    @CreationTimestamp
    @Column( nullable = true, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


    public Voucher() {
    }

    public Voucher(Long id, String code, BigDecimal discountValue, String discountType, BigDecimal minOrderValue, LocalDateTime startDate, LocalDateTime endDate, Integer quantity, Boolean active, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.discountValue = discountValue;
        this.discountType = discountType;
        this.minOrderValue = minOrderValue;
        this.startDate = startDate;
        this.endDate = endDate;
        this.quantity = quantity;
        this.active = active;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;

    }

    // ===== Getter & Setter =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public BigDecimal getMinOrderValue() { return minOrderValue; }
    public void setMinOrderValue(BigDecimal minOrderValue) { this.minOrderValue = minOrderValue; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Voucher{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", discountValue=" + discountValue +
                ", discountType='" + discountType + '\'' +
                ", minOrderValue=" + minOrderValue +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", quantity=" + quantity +
                ", active=" + active +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
