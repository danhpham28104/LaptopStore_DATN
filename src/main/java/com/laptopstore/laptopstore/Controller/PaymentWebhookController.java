package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.PaymentStatusCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Payment Webhook Controller - Nhận callback từ payment provider
 * 
 * Được gọi từ SEPAY, VNPay, Momo... khi thanh toán thành công/thất bại
 */
@Controller
@RequestMapping("/payment")
public class PaymentWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentWebhookController.class);

    @Autowired
    private PaymentStatusCheckService paymentStatusCheckService;

    /**
     * Webhook từ SEPAY
     * POST /payment/webhook/sepay
     * Body: { orderCode, transactionId, status, amount }
     */
    @PostMapping("/webhook/sepay")
    @ResponseBody
    public ResponseEntity<?> sepayWebhook(@RequestBody Map<String, Object> payload) {
        logger.info("🪝 SEPAY Webhook received: {}", payload);

        try {
            String orderCode = (String) payload.get("orderCode");
            String transactionId = (String) payload.get("transactionId");
            String status = (String) payload.get("status");
            Long amount = ((Number) payload.get("amount")).longValue();

            // 🔹 Xử lý webhook
            paymentStatusCheckService.handlePaymentWebhook(orderCode, transactionId, status, amount);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Webhook processed"
            ));

        } catch (Exception e) {
            logger.error("❌ Error processing SEPAY webhook: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Webhook từ VNPay
     * POST /payment/webhook/vnpay
     */
    @PostMapping("/webhook/vnpay")
    @ResponseBody
    public ResponseEntity<?> vnpayWebhook(@RequestBody Map<String, Object> payload) {
        logger.info("🪝 VNPay Webhook received: {}", payload);

        try {
            // VNPay format: vnp_TxnRef (order code), vnp_TransactionNo, vnp_ResponseCode (00 = success)
            String orderCode = (String) payload.get("vnp_TxnRef");
            String transactionId = (String) payload.get("vnp_TransactionNo");
            String responseCode = (String) payload.get("vnp_ResponseCode");
            Long amount = ((Number) payload.getOrDefault("vnp_Amount", 0)).longValue();

            String status = "00".equals(responseCode) ? "SUCCESS" : "FAILED";

            paymentStatusCheckService.handlePaymentWebhook(orderCode, transactionId, status, amount);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Webhook processed"
            ));

        } catch (Exception e) {
            logger.error("❌ Error processing VNPay webhook: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Webhook từ Momo
     * POST /payment/webhook/momo
     */
    @PostMapping("/webhook/momo")
    @ResponseBody
    public ResponseEntity<?> momoWebhook(@RequestBody Map<String, Object> payload) {
        logger.info("🪝 Momo Webhook received: {}", payload);

        try {
            String orderCode = (String) payload.get("orderInfo");
            String transactionId = (String) payload.get("transId");
            Integer resultCode = (Integer) payload.get("resultCode");
            Long amount = ((Number) payload.get("amount")).longValue();

            String status = resultCode == 0 ? "SUCCESS" : "FAILED";

            paymentStatusCheckService.handlePaymentWebhook(orderCode, transactionId, status, amount);

            return ResponseEntity.ok(Map.of(
                    "resultCode", 0,
                    "message", "Webhook processed"
            ));

        } catch (Exception e) {
            logger.error("❌ Error processing Momo webhook: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("resultCode", -1, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * API: Check payment status manually
     * GET /payment/check?orderId=1
     */
    @GetMapping("/check")
    @ResponseBody
    public ResponseEntity<?> checkPaymentStatus(@RequestParam Long orderId) {
        try {
            paymentStatusCheckService.checkPaymentStatusForOrder(orderId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment status checked"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
