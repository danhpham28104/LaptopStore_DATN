package com.laptopstore.laptopstore.dto;

import java.math.BigDecimal;

public class AiAnalyticsDto {

    private Long totalChats;
    private Long uniqueAiSessions;
    private Long aiAssistedOrdersCount;
    private BigDecimal aiAssistedRevenue;
    private Double aiConversionRate;
    private Long recommendedProductsCount;
    private Long recommendedProductClicks;
    private Double recommendationCtr;
    private Double avgConfidenceScore;
    private Long lowConfidenceQueryCount;

    public AiAnalyticsDto() {}

    public AiAnalyticsDto(Long totalChats, Long uniqueAiSessions, Long aiAssistedOrdersCount,
                         BigDecimal aiAssistedRevenue, Double aiConversionRate, Long recommendedProductsCount,
                         Long recommendedProductClicks, Double recommendationCtr, Double avgConfidenceScore,
                         Long lowConfidenceQueryCount) {
        this.totalChats = totalChats;
        this.uniqueAiSessions = uniqueAiSessions;
        this.aiAssistedOrdersCount = aiAssistedOrdersCount;
        this.aiAssistedRevenue = aiAssistedRevenue;
        this.aiConversionRate = aiConversionRate;
        this.recommendedProductsCount = recommendedProductsCount;
        this.recommendedProductClicks = recommendedProductClicks;
        this.recommendationCtr = recommendationCtr;
        this.avgConfidenceScore = avgConfidenceScore;
        this.lowConfidenceQueryCount = lowConfidenceQueryCount;
    }

    public Long getTotalChats() { return totalChats; }
    public void setTotalChats(Long totalChats) { this.totalChats = totalChats; }

    public Long getUniqueAiSessions() { return uniqueAiSessions; }
    public void setUniqueAiSessions(Long uniqueAiSessions) { this.uniqueAiSessions = uniqueAiSessions; }

    public Long getAiAssistedOrdersCount() { return aiAssistedOrdersCount; }
    public void setAiAssistedOrdersCount(Long aiAssistedOrdersCount) { this.aiAssistedOrdersCount = aiAssistedOrdersCount; }

    public BigDecimal getAiAssistedRevenue() { return aiAssistedRevenue; }
    public void setAiAssistedRevenue(BigDecimal aiAssistedRevenue) { this.aiAssistedRevenue = aiAssistedRevenue; }

    public Double getAiConversionRate() { return aiConversionRate; }
    public void setAiConversionRate(Double aiConversionRate) { this.aiConversionRate = aiConversionRate; }

    public Long getRecommendedProductsCount() { return recommendedProductsCount; }
    public void setRecommendedProductsCount(Long recommendedProductsCount) { this.recommendedProductsCount = recommendedProductsCount; }

    public Long getRecommendedProductClicks() { return recommendedProductClicks; }
    public void setRecommendedProductClicks(Long recommendedProductClicks) { this.recommendedProductClicks = recommendedProductClicks; }

    public Double getRecommendationCtr() { return recommendationCtr; }
    public void setRecommendationCtr(Double recommendationCtr) { this.recommendationCtr = recommendationCtr; }

    public Double getAvgConfidenceScore() { return avgConfidenceScore; }
    public void setAvgConfidenceScore(Double avgConfidenceScore) { this.avgConfidenceScore = avgConfidenceScore; }

    public Long getLowConfidenceQueryCount() { return lowConfidenceQueryCount; }
    public void setLowConfidenceQueryCount(Long lowConfidenceQueryCount) { this.lowConfidenceQueryCount = lowConfidenceQueryCount; }
}
