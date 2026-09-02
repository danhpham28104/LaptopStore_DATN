package com.laptopstore.laptopstore.dto;

import java.math.BigDecimal;

public class RevenueTrendDto {

    private String period; // "2026-08-25" or "Week 34" or "2026-08"
    private BigDecimal revenue;
    private Long orderCount;
    private BigDecimal aov;

    public RevenueTrendDto() {}

    public RevenueTrendDto(String period, BigDecimal revenue, Long orderCount, BigDecimal aov) {
        this.period = period;
        this.revenue = revenue;
        this.orderCount = orderCount;
        this.aov = aov;
    }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

    public Long getOrderCount() { return orderCount; }
    public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }

    public BigDecimal getAov() { return aov; }
    public void setAov(BigDecimal aov) { this.aov = aov; }
}
