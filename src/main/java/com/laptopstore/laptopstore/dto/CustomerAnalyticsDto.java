package com.laptopstore.laptopstore.dto;

import java.math.BigDecimal;

public class CustomerAnalyticsDto {

    private Long newCustomers;
    private Long returningCustomers;
    private Long customersWithOrders;
    private Long customersWithoutOrders;
    private Double avgOrdersPerCustomer;
    private BigDecimal avgRevenuePerCustomer;

    public CustomerAnalyticsDto() {}

    public CustomerAnalyticsDto(Long newCustomers, Long returningCustomers, Long customersWithOrders,
                                Long customersWithoutOrders, Double avgOrdersPerCustomer, BigDecimal avgRevenuePerCustomer) {
        this.newCustomers = newCustomers;
        this.returningCustomers = returningCustomers;
        this.customersWithOrders = customersWithOrders;
        this.customersWithoutOrders = customersWithoutOrders;
        this.avgOrdersPerCustomer = avgOrdersPerCustomer;
        this.avgRevenuePerCustomer = avgRevenuePerCustomer;
    }

    public Long getNewCustomers() { return newCustomers; }
    public void setNewCustomers(Long newCustomers) { this.newCustomers = newCustomers; }

    public Long getReturningCustomers() { return returningCustomers; }
    public void setReturningCustomers(Long returningCustomers) { this.returningCustomers = returningCustomers; }

    public Long getCustomersWithOrders() { return customersWithOrders; }
    public void setCustomersWithOrders(Long customersWithOrders) { this.customersWithOrders = customersWithOrders; }

    public Long getCustomersWithoutOrders() { return customersWithoutOrders; }
    public void setCustomersWithoutOrders(Long customersWithoutOrders) { this.customersWithoutOrders = customersWithoutOrders; }

    public Double getAvgOrdersPerCustomer() { return avgOrdersPerCustomer; }
    public void setAvgOrdersPerCustomer(Double avgOrdersPerCustomer) { this.avgOrdersPerCustomer = avgOrdersPerCustomer; }

    public BigDecimal getAvgRevenuePerCustomer() { return avgRevenuePerCustomer; }
    public void setAvgRevenuePerCustomer(BigDecimal avgRevenuePerCustomer) { this.avgRevenuePerCustomer = avgRevenuePerCustomer; }
}
