package com.techstore.techstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendedProductInsightDto {
    private Long productId;
    private String productName;
    private String model;
    private BigDecimal price;
    private Long count;
}
