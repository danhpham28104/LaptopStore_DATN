package com.laptopstore.laptopstore.dto;

import java.math.BigDecimal;

public class DashboardOverviewDto {

    private BigDecimal totalRevenue;
    private Long paidOrdersCount;
    private Long itemsSold;
    private BigDecimal aov;
    private Long newCustomersCount;
    private Double conversionRate;
    private Double cancellationRate;
    private Long lowStockCount;
    private BigDecimal grossProfit;
    private BigDecimal grossProfitMargin;

    public DashboardOverviewDto() {}

    public DashboardOverviewDto(BigDecimal totalRevenue, Long paidOrdersCount, Long itemsSold, BigDecimal aov,
                                Long newCustomersCount, Double conversionRate, Double cancellationRate,
                                Long lowStockCount, BigDecimal grossProfit, BigDecimal grossProfitMargin) {
        this.totalRevenue = totalRevenue;
        this.paidOrdersCount = paidOrdersCount;
        this.itemsSold = itemsSold;
        this.aov = aov;
        this.newCustomersCount = newCustomersCount;
        this.conversionRate = conversionRate;
        this.cancellationRate = cancellationRate;
        this.lowStockCount = lowStockCount;
        this.grossProfit = grossProfit;
        this.grossProfitMargin = grossProfitMargin;
    }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public Long getPaidOrdersCount() { return paidOrdersCount; }
    public void setPaidOrdersCount(Long paidOrdersCount) { this.paidOrdersCount = paidOrdersCount; }

    public Long getItemsSold() { return itemsSold; }
    public void setItemsSold(Long itemsSold) { this.itemsSold = itemsSold; }

    public BigDecimal getAov() { return aov; }
    public void setAov(BigDecimal aov) { this.aov = aov; }

    public Long getNewCustomersCount() { return newCustomersCount; }
    public void setNewCustomersCount(Long newCustomersCount) { this.newCustomersCount = newCustomersCount; }

    public Double getConversionRate() { return conversionRate; }
    public void setConversionRate(Double conversionRate) { this.conversionRate = conversionRate; }

    public Double getCancellationRate() { return cancellationRate; }
    public void setCancellationRate(Double cancellationRate) { this.cancellationRate = cancellationRate; }

    public Long getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(Long lowStockCount) { this.lowStockCount = lowStockCount; }

    public BigDecimal getGrossProfit() { return grossProfit; }
    public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }

    public BigDecimal getGrossProfitMargin() { return grossProfitMargin; }
    public void setGrossProfitMargin(BigDecimal grossProfitMargin) { this.grossProfitMargin = grossProfitMargin; }
}
