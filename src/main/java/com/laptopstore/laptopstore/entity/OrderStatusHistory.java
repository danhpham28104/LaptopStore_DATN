package com.laptopstore.laptopstore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.laptopstore.laptopstore.enums.OrderStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private OrderStatus status;

    @Column(length = 50)
    private String oldStatus;

    @Column(length = 50, nullable = false)
    private String newStatus;

    @Column(length = 100)
    private String updatedBy; // "System", "Admin: admin@...", "Customer"

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.changedAt == null) {
            this.changedAt = now;
        }
        if (this.createdAt == null) {
            this.createdAt = now;
        }
    }

    public OrderStatusHistory() {
        LocalDateTime now = LocalDateTime.now();
        this.changedAt = now;
        this.createdAt = now;
    }

    public OrderStatusHistory(Order order, String oldStatus, String newStatus, String updatedBy, String note) {
        this.order = order;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        if (newStatus != null) {
            this.status = OrderStatus.fromString(newStatus);
        }
        this.updatedBy = updatedBy;
        this.note = note;
        LocalDateTime now = LocalDateTime.now();
        this.changedAt = now;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public OrderStatus getStatus() {
        if (status != null) {
            return status;
        }
        if (newStatus != null) {
            return OrderStatus.fromString(newStatus);
        }
        return null;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
        if (status != null) {
            this.newStatus = status.name();
        }
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
        if (newStatus != null && this.status == null) {
            this.status = OrderStatus.fromString(newStatus);
        }
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getChangedAt() {
        if (changedAt != null) {
            return changedAt;
        }
        return createdAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
        this.createdAt = changedAt;
    }

    public LocalDateTime getCreatedAt() {
        if (createdAt != null) {
            return createdAt;
        }
        return changedAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        this.changedAt = createdAt;
    }
}

