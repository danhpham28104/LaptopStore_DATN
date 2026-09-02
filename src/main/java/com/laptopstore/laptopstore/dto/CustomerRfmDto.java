package com.laptopstore.laptopstore.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CustomerRfmDto {

    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private LocalDateTime lastOrderDate;
    private Long recencyDays;
    private Long frequencyOrders;
    private BigDecimal monetaryTotal;
    private String rfmSegment;
    private String recommendedAction;

    public CustomerRfmDto() {}

    public CustomerRfmDto(Long userId, String username, String fullName, String email,
                          LocalDateTime lastOrderDate, Long recencyDays, Long frequencyOrders,
                          BigDecimal monetaryTotal, String rfmSegment, String recommendedAction) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.lastOrderDate = lastOrderDate;
        this.recencyDays = recencyDays;
        this.frequencyOrders = frequencyOrders;
        this.monetaryTotal = monetaryTotal;
        this.rfmSegment = rfmSegment;
        this.recommendedAction = recommendedAction;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getLastOrderDate() { return lastOrderDate; }
    public void setLastOrderDate(LocalDateTime lastOrderDate) { this.lastOrderDate = lastOrderDate; }

    public Long getRecencyDays() { return recencyDays; }
    public void setRecencyDays(Long recencyDays) { this.recencyDays = recencyDays; }

    public Long getFrequencyOrders() { return frequencyOrders; }
    public void setFrequencyOrders(Long frequencyOrders) { this.frequencyOrders = frequencyOrders; }

    public BigDecimal getMonetaryTotal() { return monetaryTotal; }
    public void setMonetaryTotal(BigDecimal monetaryTotal) { this.monetaryTotal = monetaryTotal; }

    public String getRfmSegment() { return rfmSegment; }
    public void setRfmSegment(String rfmSegment) { this.rfmSegment = rfmSegment; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
}
