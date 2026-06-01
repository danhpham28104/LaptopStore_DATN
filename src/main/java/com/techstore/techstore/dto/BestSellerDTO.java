package com.techstore.techstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BestSellerDTO {
    private String productName;
    private Long totalSold;
}