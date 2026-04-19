package com.techstore.techstore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "cartitem",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cartitem_cart_product_variant", columnNames = {"cart_id", "product_id", "variant_id"})
        }
)

public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Giỏ hàng chứa item này */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    @JsonIgnore
    private Cart cart;

    /** Sản phẩm chính */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    /** Phiên bản (màu / dung lượng) nếu có */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    /** Số lượng sản phẩm */
    @Column(nullable = false)
    private int quantity = 1;

    /** Giá tại thời điểm thêm vào giỏ */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPriceAtAdd = BigDecimal.ZERO;

    /** Thành tiền = unitPriceAtAdd * quantity */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;


    public CartItem() { }

    public CartItem(Long id, Cart cart, Product product, ProductVariant variant, int quantity, BigDecimal unitPriceAtAdd, BigDecimal lineTotal) {
        this.id = id;
        this.cart = cart;
        this.product = product;
        this.variant = variant;
        this.quantity = quantity;
        this.unitPriceAtAdd = unitPriceAtAdd;
        this.lineTotal = lineTotal;
    }

    public String getVariantDescription() {
        if (variant == null) return "";
        String color = variant.getColor() != null ? variant.getColor() : "";
        String storage = variant.getStorage() != null ? variant.getStorage() : "";
        return (color + " " + storage).trim();
    }


    @PrePersist
    @PreUpdate
    private void updateLineTotal() {
        // Nếu chưa set giá, cố lấy từ product.price (nếu có)
        if (unitPriceAtAdd == null || unitPriceAtAdd.compareTo(BigDecimal.ZERO) <= 0) {
            if (product != null && product.getPrice() != null) {
                unitPriceAtAdd = product.getPrice();
            } else {
                unitPriceAtAdd = BigDecimal.ZERO;
            }
        }
        recalc();
    }


    public void recalc() {
        if (quantity < 1) quantity = 1;
        if (unitPriceAtAdd == null) unitPriceAtAdd = BigDecimal.ZERO;
        this.lineTotal = unitPriceAtAdd.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = Math.max(1, quantity);
        recalc();
    }

    public BigDecimal getUnitPriceAtAdd() { return unitPriceAtAdd; }
    public void setUnitPriceAtAdd(BigDecimal unitPriceAtAdd) {
        this.unitPriceAtAdd = unitPriceAtAdd != null ? unitPriceAtAdd : BigDecimal.ZERO;
        recalc();
    }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }

    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public ProductVariant getVariant() {
        return variant;
    }

    public void setVariant(ProductVariant variant) {
        this.variant = variant;
    }

    @Override
    public String toString() {
        // Không in cart/product để tránh log dài & lazy-load
        return "CartItem{" +
                "id=" + id +
                ", quantity=" + quantity +
                ", unitPriceAtAdd=" + unitPriceAtAdd +
                ", lineTotal=" + lineTotal +
                '}';
    }
}
