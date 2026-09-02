package com.laptopstore.laptopstore.enums;

/**
 * Trạng thái thanh toán — tách biệt hoàn toàn với OrderStatus.
 *
 * <p>Luồng chính:
 * UNPAID → PENDING → PAID
 *
 * <p>Nhánh thất bại:
 * PENDING → FAILED
 *
 * <p>Nhánh hoàn tiền:
 * PAID → REFUND_PENDING → REFUNDED
 *
 * <p>Backward compatibility:
 * - SUCCESS → alias của PAID (giữ để không phá DB cũ)
 */
public enum PaymentStatus {
    // ── Trạng thái chuẩn mới ────────────────────────────────────────────────
    UNPAID,         // Chưa thanh toán (COD chờ giao)
    PENDING,        // Đang chờ thanh toán (QR đã tạo, chờ transfer)
    PAID,           // Đã thanh toán thành công
    FAILED,         // Thanh toán thất bại
    REFUND_PENDING, // Đang chờ hoàn tiền
    REFUNDED,       // Đã hoàn tiền

    // ── Backward-compatible alias ─────────────────────────────────────────
    @Deprecated
    SUCCESS;        // Thanh toán thành công (cũ)

    /** Kiểm tra đã thanh toán thành công chưa (bao gồm cả alias cũ). */
    public boolean isPaid() {
        return this == PAID || this == SUCCESS;
    }

    /** Kiểm tra trạng thái cuối của payment. */
    public boolean isTerminal() {
        return this == PAID || this == SUCCESS || this == FAILED || this == REFUNDED;
    }

    public static PaymentStatus fromString(String text) {
        if (text == null || text.isBlank()) return PENDING;
        for (PaymentStatus s : values()) {
            if (s.name().equalsIgnoreCase(text.trim())) return s;
        }
        if ("SUCCESS".equalsIgnoreCase(text)) return PAID;
        return PENDING;
    }
}
