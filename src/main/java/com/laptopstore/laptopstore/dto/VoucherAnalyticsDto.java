package com.laptopstore.laptopstore.dto;

import java.math.BigDecimal;

public class VoucherAnalyticsDto {

    private String code;
    private Long usageCount;
    private Long orderCount;
    private BigDecimal revenueGenerated;
    private BigDecimal totalDiscount;
    private BigDecimal avgOrderValue;
    private Double conversionRate;

    public VoucherAnalyticsDto() {}

    public VoucherAnalyticsDto(String code, Long usageCount, Long orderCount, BigDecimal revenueGenerated,
                               BigDecimal totalDiscount, BigDecimal avgOrderValue, Double conversionRate) {
        this.code = code;
        this.usageCount = usageCount;
        this.orderCount = orderCount;
        this.revenueGenerated = revenueGenerated;
        this.totalDiscount = totalDiscount;
        this.avgOrderValue = avgOrderValue;
        this.conversionRate = conversionRate;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Long getUsageCount() { return usageCount; }
    public void setUsageCount(Long usageCount) { this.usageCount = usageCount; }

    public Long getOrderCount() { return orderCount; }
    public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }

    public BigDecimal getRevenueGenerated() { return revenueGenerated; }
    public void setRevenueGenerated(BigDecimal revenueGenerated) { this.revenueGenerated = revenueGenerated; }

    public BigDecimal getTotalDiscount() { return totalDiscount; }
    public void setTotalDiscount(BigDecimal totalDiscount) { this.totalDiscount = totalDiscount; }

    public BigDecimal getAvgOrderValue() { return avgOrderValue; }
    public void setAvgOrderValue(BigDecimal avgOrderValue) { this.avgOrderValue = avgOrderValue; }

    public Double getConversionRate() { return conversionRate; }
    public void setConversionRate(Double conversionRate) { this.conversionRate = conversionRate; }
}
