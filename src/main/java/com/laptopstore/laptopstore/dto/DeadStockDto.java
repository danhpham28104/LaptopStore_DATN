package com.laptopstore.laptopstore.dto;

import java.math.BigDecimal;

public class DeadStockDto {

    private Long productId;
    private String productName;
    private Integer currentStock;
    private Long soldLast90Days;
    private BigDecimal tiedUpCapital; // Tồn kho * Đơn giá = Vốn đang kẹt

    public DeadStockDto() {}

    public DeadStockDto(Long productId, String productName, Integer currentStock, Long soldLast90Days, BigDecimal tiedUpCapital) {
        this.productId = productId;
        this.productName = productName;
        this.currentStock = currentStock;
        this.soldLast90Days = soldLast90Days;
        this.tiedUpCapital = tiedUpCapital;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getCurrentStock() { return currentStock; }
    public void setCurrentStock(Integer currentStock) { this.currentStock = currentStock; }

    public Long getSoldLast90Days() { return soldLast90Days; }
    public void setSoldLast90Days(Long soldLast90Days) { this.soldLast90Days = soldLast90Days; }

    public BigDecimal getTiedUpCapital() { return tiedUpCapital; }
    public void setTiedUpCapital(BigDecimal tiedUpCapital) { this.tiedUpCapital = tiedUpCapital; }
}
