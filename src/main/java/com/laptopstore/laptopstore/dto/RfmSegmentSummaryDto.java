package com.laptopstore.laptopstore.dto;

import java.math.BigDecimal;

public class RfmSegmentSummaryDto {

    private String segmentName;
    private Long customerCount;
    private BigDecimal totalRevenue;
    private Double percentageOfCustomers;

    public RfmSegmentSummaryDto() {}

    public RfmSegmentSummaryDto(String segmentName, Long customerCount, BigDecimal totalRevenue, Double percentageOfCustomers) {
        this.segmentName = segmentName;
        this.customerCount = customerCount;
        this.totalRevenue = totalRevenue;
        this.percentageOfCustomers = percentageOfCustomers;
    }

    public String getSegmentName() { return segmentName; }
    public void setSegmentName(String segmentName) { this.segmentName = segmentName; }

    public Long getCustomerCount() { return customerCount; }
    public void setCustomerCount(Long customerCount) { this.customerCount = customerCount; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public Double getPercentageOfCustomers() { return percentageOfCustomers; }
    public void setPercentageOfCustomers(Double percentageOfCustomers) { this.percentageOfCustomers = percentageOfCustomers; }
}
