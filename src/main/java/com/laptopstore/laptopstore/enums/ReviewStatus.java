package com.laptopstore.laptopstore.enums;

public enum ReviewStatus {
    PENDING("Chờ duyệt", "bg-warning text-dark"),
    APPROVED("Đã duyệt", "bg-success text-white"),
    REJECTED("Từ chối", "bg-danger text-white");

    private final String displayName;
    private final String badgeClass;

    ReviewStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
