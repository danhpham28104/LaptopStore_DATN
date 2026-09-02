package com.laptopstore.laptopstore.enums;

/**
 * Trạng thái vận hành của đơn hàng — tách biệt hoàn toàn với PaymentStatus.
 *
 * <p>Luồng chính:
 * PENDING → CONFIRMED → PROCESSING → SHIPPING → DELIVERED
 *
 * <p>Nhánh hủy:
 * PENDING → CANCELLED
 *
 * <p>Nhánh hoàn trả:
 * DELIVERED → RETURN_REQUESTED → RETURNED
 *
 * <p>Backward compatibility:
 * - PENDING_PAYMENT  → alias của PENDING  (giữ để không phá DB cũ)
 * - PACKING          → alias của PROCESSING (giữ để không phá DB cũ)
 * - REFUNDED         → giữ để không phá DB cũ (nghiệp vụ mới dùng RETURNED)
 */
public enum OrderStatus {
    // ── Trạng thái chuẩn mới ────────────────────────────────────────────────
    PENDING("Chờ xác nhận", "bg-warning text-dark"),
    CONFIRMED("Đã xác nhận", "bg-info text-white"),
    PROCESSING("Đang xử lý", "bg-primary text-white"),
    SHIPPING("Đang giao hàng", "bg-primary text-white"),
    DELIVERED("Đã giao hàng", "bg-success text-white"),
    CANCELLED("Đã hủy", "bg-danger text-white"),
    RETURN_REQUESTED("Yêu cầu hoàn trả", "bg-warning text-dark"),
    RETURNED("Đã hoàn trả", "bg-secondary text-white"),

    // ── Backward-compatible aliases (giữ nguyên để không phá dữ liệu cũ) ──
    @Deprecated
    PENDING_PAYMENT("Chờ thanh toán", "bg-warning text-dark"),
    @Deprecated
    PACKING("Đang đóng gói", "bg-primary text-white"),
    @Deprecated
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

    /**
     * Chuyển đổi linh hoạt từ string sang enum — hỗ trợ cả tên cũ lẫn tên mới.
     */
    public static OrderStatus fromString(String text) {
        if (text == null || text.trim().isEmpty()) return PENDING;
        String t = text.trim();
        for (OrderStatus s : OrderStatus.values()) {
            if (s.name().equalsIgnoreCase(t) || s.displayName.equalsIgnoreCase(t)) {
                return s;
            }
        }
        // Legacy string mappings
        switch (t.toUpperCase()) {
            case "PENDING_PAYMENT": return PENDING;
            case "PACKING":         return PROCESSING;
            case "PENDING":         return PENDING;
            case "PAID":            return CONFIRMED;
            case "SHIPPED":         return SHIPPING;
            case "COMPLETED":       return DELIVERED;
            case "PAYMENT TIMEOUT":
            case "PAYMENT_TIMEOUT":
            case "PAYMENT FAILED":
            case "PAYMENT_FAILED":  return CANCELLED;
            case "REFUNDED":        return RETURNED;
            default:                return PENDING;
        }
    }

    /** Kiểm tra đây có phải trạng thái "chờ thanh toán" không (gồm cả alias cũ). */
    public boolean isPendingPayment() {
        return this == PENDING || this == PENDING_PAYMENT;
    }

    /** Kiểm tra đây có phải trạng thái đơn hàng thành công (dùng cho analytics). */
    public boolean isSuccessful() {
        return this == CONFIRMED || this == PROCESSING || this == PACKING
                || this == SHIPPING || this == DELIVERED;
    }

    /** Kiểm tra đây có phải trạng thái cuối (không thể thay đổi thêm). */
    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == RETURNED || this == REFUNDED;
    }
}
