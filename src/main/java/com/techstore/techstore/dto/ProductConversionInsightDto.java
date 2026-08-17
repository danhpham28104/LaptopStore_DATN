package com.techstore.techstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductConversionInsightDto {
    private Long productId;
    private String productName;
    private String brandName;
    private BigDecimal price;
    private Integer stock;
    private Long totalViews;
    private Long totalSold;
    private Double conversionRate;
    private String recommendation;
}
