package com.laptopstore.laptopstore.dto;

import java.math.BigDecimal;

public class CategorySalesDto {

    private String categoryName;
    private BigDecimal revenue;
    private Long unitsSold;
    private Long orderCount;

    public CategorySalesDto() {}

    public CategorySalesDto(String categoryName, BigDecimal revenue, Long unitsSold, Long orderCount) {
        this.categoryName = categoryName;
        this.revenue = revenue;
        this.unitsSold = unitsSold;
        this.orderCount = orderCount;
    }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

    public Long getUnitsSold() { return unitsSold; }
    public void setUnitsSold(Long unitsSold) { this.unitsSold = unitsSold; }

    public Long getOrderCount() { return orderCount; }
    public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
}
