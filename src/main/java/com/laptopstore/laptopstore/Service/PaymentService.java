package com.laptopstore.laptopstore.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/**
 * Payment Service - Quản lý QR code và thanh toán
 * 
 * Hỗ trợ:
 * - SEPAY (Sync Economy Pay)
 * - VNPay
 * - Momo
 * - Mock QR cho testing
 */
@Service
public class PaymentService {

    @Value("${payment.sepay.webhook-secret:}")
    private String webhookSecret;

    // 🔹 SEPAY Config (thay bằng của bạn)
    private final String SEPAY_QR_BASE = "https://qr.sepay.vn/img";
    private final String VA_ACCOUNT = "5282587777"; // Thay bằng Virtual Account của bạn
    private final String BANK_CODE = "MBBank"; // Thay bằng mã ngân hàng

    /**
     * Tạo QR code thanh toán
     * 
     * @param amount Số tiền cần thanh toán (VNĐ)
     * @param orderCode Mã đơn hàng (dùng làm content chuyển khoản)
     * @return URL của QR code
     */
    public String generatePaymentQR(long amount, String orderCode) {
        return SEPAY_QR_BASE
                + "?acc=" + VA_ACCOUNT
                + "&bank=" + BANK_CODE
                + "&amount=" + amount
                + "&des=" + orderCode;
    }

    /**
     * Tạo QR Data theo chuẩn NAPAS (nếu cần lưu dạng text)
     * Format: |000|<version>|<amount>|<info>
     */
    public String generateQrData(long amount, String orderCode) {
        return String.format("|000|1|%d|%s", amount, orderCode);
    }

    /**
     * Mock: Kiểm tra trạng thái thanh toán (cho testing)
     * Trong production, gọi API ngân hàng hoặc webhook
     */
    public Map<String, Object> checkPaymentStatus(String transactionId, long expectedAmount) {
        Map<String, Object> result = new HashMap<>();
        
        // 🔹 Mock logic: Nếu transactionId chứa "TEST_SUCCESS" thì coi như đã thanh toán
        if (transactionId != null && transactionId.contains("TEST_SUCCESS")) {
            result.put("status", "SUCCESS");
            result.put("amount", expectedAmount);
            result.put("message", "Thanh toán thành công (Mock)");
        } else {
            result.put("status", "PENDING");
            result.put("message", "Chưa thanh toán");
        }

        return result;
    }

    /**
     * Xác thực webhook từ SEPAY bằng HMAC-SHA256
     */
    public boolean validateWebhookSignature(String payload, String receivedSignature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }
        if (receivedSignature == null || receivedSignature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return computed.equalsIgnoreCase(receivedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Format thông tin chuyển khoản hiển thị
     */
    public String formatPaymentInfo(String orderCode, long amount) {
        return String.format("Chuyển khoản với nội dung: '%s' | Số tiền: %,d VNĐ", 
                orderCode, amount);
    }
}

