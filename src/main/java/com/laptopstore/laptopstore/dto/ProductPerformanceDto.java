package com.laptopstore.laptopstore.dto;

import java.math.BigDecimal;

public class ProductPerformanceDto {

    private Long productId;
    private String productName;
    private String brandName;
    private String categoryName;
    private Long views;
    private Long uniqueViews;
    private Long cartAdds;
    private Long orders;
    private Long unitsSold;
    private BigDecimal revenue;
    private Double conversionRate;
    private Integer currentStock;
    private Double averageRating;

    public ProductPerformanceDto() {}

    public ProductPerformanceDto(Long productId, String productName, String brandName, String categoryName,
                                 Long views, Long uniqueViews, Long cartAdds, Long orders, Long unitsSold,
                                 BigDecimal revenue, Double conversionRate, Integer currentStock, Double averageRating) {
        this.productId = productId;
        this.productName = productName;
        this.brandName = brandName;
        this.categoryName = categoryName;
        this.views = views;
        this.uniqueViews = uniqueViews;
        this.cartAdds = cartAdds;
        this.orders = orders;
        this.unitsSold = unitsSold;
        this.revenue = revenue;
        this.conversionRate = conversionRate;
        this.currentStock = currentStock;
        this.averageRating = averageRating;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Long getViews() { return views; }
    public void setViews(Long views) { this.views = views; }

    public Long getUniqueViews() { return uniqueViews; }
    public void setUniqueViews(Long uniqueViews) { this.uniqueViews = uniqueViews; }

    public Long getCartAdds() { return cartAdds; }
    public void setCartAdds(Long cartAdds) { this.cartAdds = cartAdds; }

    public Long getOrders() { return orders; }
    public void setOrders(Long orders) { this.orders = orders; }

    public Long getUnitsSold() { return unitsSold; }
    public void setUnitsSold(Long unitsSold) { this.unitsSold = unitsSold; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

    public Double getConversionRate() { return conversionRate; }
    public void setConversionRate(Double conversionRate) { this.conversionRate = conversionRate; }

    public Integer getCurrentStock() { return currentStock; }
    public void setCurrentStock(Integer currentStock) { this.currentStock = currentStock; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
}
