    package com.techstore.techstore.entity;

    import com.fasterxml.jackson.annotation.JsonIgnore;
    import jakarta.persistence.*;

    import java.math.BigDecimal;
    import java.util.ArrayList;
    import java.util.List;

    @Entity
    @Table(name = "cart")
    public class Cart {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        /** Mỗi User chỉ có 1 Cart */
        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", unique = true)
        @JsonIgnore
        private User user;

        /** Tổng giá trị sản phẩm trước giảm */
        @Column(precision = 19, scale = 2, nullable = false)
        private BigDecimal subtotal = BigDecimal.ZERO;

        /** Số tiền giảm (voucher...) */
        @Column(precision = 19, scale = 2, nullable = false)
        private BigDecimal discount = BigDecimal.ZERO;

        /** Tổng tiền phải trả */
        @Column(precision = 19, scale = 2, nullable = false)
        private BigDecimal total = BigDecimal.ZERO;

        /** Danh sách sản phẩm trong giỏ */
        @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
        @JsonIgnore
        private List<CartItem> items = new ArrayList<>();


        public Cart() { }

        public Cart(Long id, List<CartItem> items, User user) {
            this.id = id;
            if (items != null) this.items = items;
            this.user = user;
        }

        // ===== Helper tối thiểu cho quan hệ 2 chiều =====
        public void addItem(CartItem item) {
            if (item != null) {
                items.add(item);
                item.setCart(this);
            }
        }

        public void removeItem(CartItem item) {
            if (item != null) {
                items.remove(item);
                item.setCart(null);
            }
        }

        //  Tính lại tổng tiền (gọi ở service sau khi thêm/xóa/cập nhật item)
        public void recalcTotals() {
            // subtotal = sum(lineTotal)
            BigDecimal sum = BigDecimal.ZERO;
            for (CartItem i : items) {
                if (i.getLineTotal() != null) {
                    sum = sum.add(i.getLineTotal());
                }
            }
            this.subtotal = sum;
            // total = subtotal - discount + tax + shipping
            this.total = subtotal.subtract(discount);
        }

        // ===== Getters/Setters =====
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }

        public List<CartItem> getItems() { return items; }
        public void setItems(List<CartItem> items) { this.items = (items != null) ? items : new ArrayList<>(); }

        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = (subtotal != null) ? subtotal : BigDecimal.ZERO; }

        public BigDecimal getDiscount() { return discount; }
        public void setDiscount(BigDecimal discount) { this.discount = (discount != null) ? discount : BigDecimal.ZERO; }

        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = (total != null) ? total : BigDecimal.ZERO; }

        @Override
        public String toString() {
            //  Không in user/items để tránh log dài & lazy load
            return "Cart{" +
                    "id=" + id +
                    ", subtotal=" + subtotal +
                    ", discount=" + discount +
                    ", total=" + total +
                    '}';
        }
    }
