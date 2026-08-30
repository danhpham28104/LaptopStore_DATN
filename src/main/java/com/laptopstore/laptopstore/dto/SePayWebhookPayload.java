package com.laptopstore.laptopstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO đại diện cho webhook payload từ SePay
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SePayWebhookPayload {
    private String transactionId;
    private String orderCode;
    private BigDecimal amount;
    private String bankCode;
    private String signature;
}
