package com.laptopstore.laptopstore.dto;

public class OrderStatusDistDto {

    private String status;
    private String displayName;
    private Long count;

    public OrderStatusDistDto() {}

    public OrderStatusDistDto(String status, String displayName, Long count) {
        this.status = status;
        this.displayName = displayName;
        this.count = count;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
}
