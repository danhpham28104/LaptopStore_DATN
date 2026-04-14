package com.techstore.techstore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product_variant")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔹 Liên kết với sản phẩm chính
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    // 🔹 Các thuộc tính riêng của phiên bản
    @Column(length = 50)
    private String color;

    @Column(length = 50)
    private String storage;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(columnDefinition = "TEXT")
    private String image; // ảnh riêng cho màu này (nếu có)

    public ProductVariant() {}

    public ProductVariant(Product product, String color, String storage, Integer stock) {
        this.product = product;
        this.color = color;
        this.storage = storage;
        this.stock = stock;
    }

    // ========== GETTER / SETTER ==========
    public Long getId() { return id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getStorage() { return storage; }
    public void setStorage(String storage) { this.storage = storage; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    @Override
    public String toString() {
        return "ProductVariant{" +
                "id=" + id +
                ", color='" + color + '\'' +
                ", storage='" + storage + '\'' +
                ", stock=" + stock +
                '}';
    }
}
