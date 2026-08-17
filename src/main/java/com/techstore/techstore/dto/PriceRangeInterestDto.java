package com.techstore.techstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceRangeInterestDto {
    private String rangeLabel;
    private Long totalViews;
}
