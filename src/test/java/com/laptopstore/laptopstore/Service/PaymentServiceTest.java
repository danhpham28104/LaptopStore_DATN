package com.laptopstore.laptopstore.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTest {

    private PaymentService paymentService;
    private final String testSecret = "my_secret_key_123";

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
        ReflectionTestUtils.setField(paymentService, "webhookSecret", testSecret);
    }

    @Test
    @DisplayName("Xác thực Webhook Signature thành công với HMAC-SHA256 hợp lệ")
    void testValidateWebhookSignature_Success() throws Exception {
        String payload = "{\"orderCode\":\"DH12345678-0001\",\"transactionId\":\"TXN999\",\"amount\":100000}";
        
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(testSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        String validSignature = HexFormat.of().formatHex(hash);

        boolean isValid = paymentService.validateWebhookSignature(payload, validSignature);
        assertTrue(isValid, "Signature phải hợp lệ");
    }

    @Test
    @DisplayName("Từ chối Webhook Signature khi signature không khớp hoặc bị fake")
    void testValidateWebhookSignature_InvalidSignature() {
        String payload = "{\"orderCode\":\"DH12345678-0001\",\"transactionId\":\"TXN999\",\"amount\":100000}";
        String fakeSignature = "invalid_signature_hash_value";

        boolean isValid = paymentService.validateWebhookSignature(payload, fakeSignature);
        assertFalse(isValid, "Signature fake phải bị từ chối");
    }
}
