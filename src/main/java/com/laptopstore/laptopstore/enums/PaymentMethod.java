package com.laptopstore.laptopstore.enums;

public enum PaymentMethod {
    COD("Thanh toán khi nhận hàng (COD)"),
    SEPAY("Chuyển khoản SePay"),
    QR_CODE("Thanh toán QR Code");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
