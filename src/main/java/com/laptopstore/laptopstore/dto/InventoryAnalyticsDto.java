package com.laptopstore.laptopstore.dto;

public class InventoryAnalyticsDto {

    private Long productId;
    private String productName;
    private String brandName;
    private Integer currentStock;
    private Integer reservedStock;
    private Long soldLast7Days;
    private Long soldLast30Days;
    private Long soldLast90Days;
    private Double avgDailySales;
    private Double daysOfInventory;
    private Double sellThroughRate;
    private String statusLabel;

    public InventoryAnalyticsDto() {}

    public InventoryAnalyticsDto(Long productId, String productName, String brandName, Integer currentStock,
                                 Integer reservedStock, Long soldLast7Days, Long soldLast30Days, Long soldLast90Days,
                                 Double avgDailySales, Double daysOfInventory, Double sellThroughRate, String statusLabel) {
        this.productId = productId;
        this.productName = productName;
        this.brandName = brandName;
        this.currentStock = currentStock;
        this.reservedStock = reservedStock;
        this.soldLast7Days = soldLast7Days;
        this.soldLast30Days = soldLast30Days;
        this.soldLast90Days = soldLast90Days;
        this.avgDailySales = avgDailySales;
        this.daysOfInventory = daysOfInventory;
        this.sellThroughRate = sellThroughRate;
        this.statusLabel = statusLabel;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public Integer getCurrentStock() { return currentStock; }
    public void setCurrentStock(Integer currentStock) { this.currentStock = currentStock; }

    public Integer getReservedStock() { return reservedStock; }
    public void setReservedStock(Integer reservedStock) { this.reservedStock = reservedStock; }

    public Long getSoldLast7Days() { return soldLast7Days; }
    public void setSoldLast7Days(Long soldLast7Days) { this.soldLast7Days = soldLast7Days; }

    public Long getSoldLast30Days() { return soldLast30Days; }
    public void setSoldLast30Days(Long soldLast30Days) { this.soldLast30Days = soldLast30Days; }

    public Long getSoldLast90Days() { return soldLast90Days; }
    public void setSoldLast90Days(Long soldLast90Days) { this.soldLast90Days = soldLast90Days; }

    public Double getAvgDailySales() { return avgDailySales; }
    public void setAvgDailySales(Double avgDailySales) { this.avgDailySales = avgDailySales; }

    public Double getDaysOfInventory() { return daysOfInventory; }
    public void setDaysOfInventory(Double daysOfInventory) { this.daysOfInventory = daysOfInventory; }

    public Double getSellThroughRate() { return sellThroughRate; }
    public void setSellThroughRate(Double sellThroughRate) { this.sellThroughRate = sellThroughRate; }

    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
}
