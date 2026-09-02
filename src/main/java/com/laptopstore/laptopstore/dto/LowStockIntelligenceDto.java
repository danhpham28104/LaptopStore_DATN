package com.laptopstore.laptopstore.dto;

public class LowStockIntelligenceDto {

    private Long productId;
    private String productName;
    private Integer currentStock;
    private Double avgDailySales;
    private Double estimatedDaysRemaining;
    private String recommendation;

    public LowStockIntelligenceDto() {}

    public LowStockIntelligenceDto(Long productId, String productName, Integer currentStock,
                                  Double avgDailySales, Double estimatedDaysRemaining, String recommendation) {
        this.productId = productId;
        this.productName = productName;
        this.currentStock = currentStock;
        this.avgDailySales = avgDailySales;
        this.estimatedDaysRemaining = estimatedDaysRemaining;
        this.recommendation = recommendation;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getCurrentStock() { return currentStock; }
    public void setCurrentStock(Integer currentStock) { this.currentStock = currentStock; }

    public Double getAvgDailySales() { return avgDailySales; }
    public void setAvgDailySales(Double avgDailySales) { this.avgDailySales = avgDailySales; }

    public Double getEstimatedDaysRemaining() { return estimatedDaysRemaining; }
    public void setEstimatedDaysRemaining(Double estimatedDaysRemaining) { this.estimatedDaysRemaining = estimatedDaysRemaining; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
}
