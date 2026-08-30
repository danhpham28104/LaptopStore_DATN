package com.laptopstore.laptopstore.enums;

public enum OrderStatus {
    PENDING_PAYMENT("Chờ thanh toán", "bg-warning text-dark"),
    CONFIRMED("Đã xác nhận", "bg-info text-white"),
    PACKING("Đang đóng gói", "bg-primary text-white"),
    SHIPPING("Đang giao hàng", "bg-primary text-white"),
    DELIVERED("Đã giao hàng", "bg-success text-white"),
    CANCELLED("Đã hủy", "bg-danger text-white"),
    REFUNDED("Đã hoàn tiền", "bg-secondary text-white");

    private final String displayName;
    private final String badgeClass;

    OrderStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public static OrderStatus fromString(String text) {
        if (text == null || text.trim().isEmpty()) return PENDING_PAYMENT;
        for (OrderStatus b : OrderStatus.values()) {
            if (b.name().equalsIgnoreCase(text) || b.displayName.equalsIgnoreCase(text)) {
                return b;
            }
        }
        if ("Pending".equalsIgnoreCase(text)) return PENDING_PAYMENT;
        if ("Paid".equalsIgnoreCase(text)) return CONFIRMED;
        if ("Shipped".equalsIgnoreCase(text)) return SHIPPING;
        if ("Completed".equalsIgnoreCase(text)) return DELIVERED;
        if ("Payment Timeout".equalsIgnoreCase(text) || "Payment Failed".equalsIgnoreCase(text)) return CANCELLED;
        return PENDING_PAYMENT;
    }
}
