package com.laptopstore.laptopstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductViewInsightDto {
    private Long productId;
    private String productName;
    private String model;
    private String brandName;
    private String image;
    private BigDecimal price;
    private Integer stock;
    private Long totalViews;
    private LocalDateTime lastViewedAt;
}
