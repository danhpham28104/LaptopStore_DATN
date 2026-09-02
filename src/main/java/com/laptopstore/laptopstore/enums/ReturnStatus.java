package com.laptopstore.laptopstore.enums;

public enum ReturnStatus {
    PENDING("Chờ xử lý", "bg-warning text-dark"),
    APPROVED("Đã phê duyệt", "bg-info text-white"),
    REJECTED("Từ chối", "bg-danger text-white"),
    COMPLETED("Hoàn tất hoàn tiền", "bg-success text-white");

    private final String displayName;
    private final String badgeClass;

    ReturnStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() { return displayName; }
    public String getBadgeClass() { return badgeClass; }
}
