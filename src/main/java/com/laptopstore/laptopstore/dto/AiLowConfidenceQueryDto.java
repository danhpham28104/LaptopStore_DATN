package com.laptopstore.laptopstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiLowConfidenceQueryDto {
    private Long id;
    private String conversationKey;
    private String userQuestion;
    private String assistantAnswer;
    private Double confidence;
    private Integer recommendedCount;
    private String missingInformation;
    private LocalDateTime createdAt;
    private String recommendation;
}
