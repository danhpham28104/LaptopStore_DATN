package com.laptopstore.laptopstore.Controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptopstore.laptopstore.Repository.PaymentRepository;
import com.laptopstore.laptopstore.Service.OrderService;
import com.laptopstore.laptopstore.Service.PaymentService;
import com.laptopstore.laptopstore.entity.Order;
import com.laptopstore.laptopstore.enums.OrderStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/sepay")
public class SePayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SePayWebhookController.class);
    private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("DH\\d{8}-\\d{4}");

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.laptopstore.laptopstore.Service.AnalyticsEventService analyticsEventService;

    @Value("${payment.sepay.secret-key:}")
    private String secretKey;

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(
            @RequestHeader(value = "X-SePay-Signature", required = false) String signature,
            @RequestBody String rawBody,
            HttpServletRequest request
    ) {
        log.info("Webhook từ SePay: signature={}, rawBody={}", signature, rawBody);

        // 🔹 1. Xác thực HMAC-SHA256 Signature
        if (!paymentService.validateWebhookSignature(rawBody, signature)) {
            // Check legacy secretKey header fallback if present
            boolean validLegacy = false;
            if (secretKey != null && !secretKey.isBlank()) {
                String authHeader = request.getHeader("Authorization");
                String customSecret = request.getHeader("X-SePay-Secret");
                validLegacy = (authHeader != null && authHeader.contains(secretKey)) || secretKey.equals(customSecret);
            }
            if (!validLegacy) {
                log.warn("⚠️ Unauthorized SePay Webhook attempt: Invalid signature!");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid signature"));
            }
        }

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(rawBody, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("❌ Invalid JSON payload in webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().body("INVALID_PAYLOAD");
        }

        if (payload == null || !payload.containsKey("description") || !payload.containsKey("trans_amount")) {
            return ResponseEntity.badRequest().body("INVALID_PAYLOAD");
        }

        // 🔹 Idempotency check
        String transactionId = payload.containsKey("id") ? payload.get("id").toString()
                : (payload.containsKey("transactionId") ? payload.get("transactionId").toString() : null);

        if (transactionId != null && !transactionId.isBlank() && paymentRepository.existsByTransactionId(transactionId)) {
            log.info("ℹ️ Transaction {} already processed", transactionId);
            return ResponseEntity.ok("Already processed");
        }

        String rawDescription = payload.get("description").toString();
        long amount;
        try {
            amount = Long.parseLong(payload.get("trans_amount").toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("INVALID_AMOUNT_FORMAT");
        }

        // 🔹 2. Bóc tách mã đơn hàng bằng Regex (DH20260827-1234)
        String orderCode = extractOrderCode(rawDescription);
        if (orderCode == null) {
            log.warn("⚠️ Không tìm thấy định dạng mã đơn trong description: {}", rawDescription);
            return ResponseEntity.ok("ORDER_CODE_NOT_FOUND");
        }

        Order order = orderService.getByOrderCode(orderCode);

        if (order == null) {
            log.warn("⚠️ Không tìm thấy đơn hàng trong hệ thống: {}", orderCode);
            return ResponseEntity.ok("ORDER_NOT_FOUND");
        }

        // Kiểm tra số tiền
        if (order.getTotalAmount().longValue() != amount) {
            log.warn("⚠️ Số tiền không khớp cho đơn {}: mong đợi {}, nhận {}", orderCode, order.getTotalAmount(), amount);
            return ResponseEntity.ok("INVALID_AMOUNT");
        }

        // 🔹 3. Cập nhật trạng thái đơn hàng & xác nhận kho chuẩn xác
        if (order.getOrderStatus().isPendingPayment()) {
            orderService.confirmQrPayment(order.getId(), "SePay Webhook");
            order.setOrderStatus(OrderStatus.CONFIRMED);
            if (order.getPayment() != null) {
                order.getPayment().setStatus(com.laptopstore.laptopstore.enums.PaymentStatus.PAID);
                order.getPayment().setTransactionId(transactionId != null ? transactionId : "SEPAY_" + System.currentTimeMillis());
            }
            orderService.saveOrder(order);

            // 🔹 Track PAYMENT_SUCCESS
            analyticsEventService.trackPaymentSuccess(order.getId());

            log.info("✅ Đã xác nhận thanh toán tự động qua SePay cho đơn hàng: {}", orderCode);
        }

        return ResponseEntity.ok("OK");
    }

    private String extractOrderCode(String description) {
        if (description == null) return null;
        Matcher matcher = ORDER_CODE_PATTERN.matcher(description);
        if (matcher.find()) {
            return matcher.group();
        }
        return description.trim(); // fallback
    }
}
