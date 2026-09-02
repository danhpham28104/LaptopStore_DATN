package com.laptopstore.laptopstore.dto;

import java.math.BigDecimal;

public class BrandSalesDto {

    private String brandName;
    private BigDecimal revenue;
    private Long unitsSold;
    private Long orderCount;

    public BrandSalesDto() {}

    public BrandSalesDto(String brandName, BigDecimal revenue, Long unitsSold, Long orderCount) {
        this.brandName = brandName;
        this.revenue = revenue;
        this.unitsSold = unitsSold;
        this.orderCount = orderCount;
    }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

    public Long getUnitsSold() { return unitsSold; }
    public void setUnitsSold(Long unitsSold) { this.unitsSold = unitsSold; }

    public Long getOrderCount() { return orderCount; }
    public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
}
