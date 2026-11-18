package com.laptopstore.laptopstore.enums;

/**
 * Loại biến động kho hàng
 */
public enum StockLogType {
    /** Nhập kho thủ công (Admin thêm hàng) */
    IMPORT("Nhập kho"),

    /** Xuất kho khi xác nhận đơn hàng (trạng thái DA_XAC_NHAN) */
    EXPORT_ORDER("Xuất kho – Xác nhận đơn hàng"),

    /** Khoá tạm thời (dữ liệu lịch sử cũ) */
    RESERVE("Khoá tạm – Chờ thanh toán"),

    /** Xác nhận thanh toán → trừ stock */
    CONFIRM_RESERVE("Xác nhận thanh toán – Trừ kho"),

    /** Tự động hoàn kho khi hủy đơn hoặc hoàn trả hàng */
    CANCEL_RESTORE("Hoàn kho – Hủy / Hoàn trả đơn"),

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
