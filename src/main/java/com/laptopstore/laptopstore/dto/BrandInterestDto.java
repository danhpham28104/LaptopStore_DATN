package com.laptopstore.laptopstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrandInterestDto {
    private String brandName;
    private Long totalViews;
}
