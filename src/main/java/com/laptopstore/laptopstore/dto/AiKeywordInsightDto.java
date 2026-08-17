package com.laptopstore.laptopstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiKeywordInsightDto {
    private String keyword;
    private Long count;
}
