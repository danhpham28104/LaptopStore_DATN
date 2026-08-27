package com.laptopstore.laptopstore.enums;

/**
 * Loại biến động kho hàng
 */
public enum StockLogType {
    /** Nhập kho thủ công (Admin thêm hàng) */
    IMPORT("Nhập kho"),

    /** Xuất kho do đơn hàng được tạo (đơn COD trừ thẳng) */
    EXPORT_ORDER("Xuất kho – Đơn hàng"),

    /** Khoá tạm thời khi tạo đơn QR */
    RESERVE("Khoá tạm – Chờ thanh toán QR"),

    /** Xác nhận thanh toán QR → trừ stock chính thức */
    CONFIRM_RESERVE("Xác nhận thanh toán – Trừ kho"),

    /** Nhả kho khi hủy đơn hoặc QR timeout */
    CANCEL_RESTORE("Hoàn kho – Hủy đơn"),

    /** Điều chỉnh kho thủ công */
    ADJUSTMENT("Điều chỉnh thủ công");

    private final String label;

    StockLogType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
