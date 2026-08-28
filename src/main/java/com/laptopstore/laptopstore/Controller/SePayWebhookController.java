package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.OrderService;
import com.laptopstore.laptopstore.entity.Order;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @Value("${payment.sepay.secret-key:}")
    private String secretKey;

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody Map<String, Object> payload, HttpServletRequest request) {

        log.info("Webhook từ SePay: {}", payload);

        // 🔹 1. Xác thực Secret Key (nếu được cấu hình)
        if (secretKey != null && !secretKey.isBlank()) {
            String authHeader = request.getHeader("Authorization");
            String customSecret = request.getHeader("X-SePay-Secret");
            boolean validAuth = (authHeader != null && authHeader.contains(secretKey)) || secretKey.equals(customSecret);
            if (!validAuth) {
                log.warn("⚠️ Unauthorized SePay Webhook attempt!");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED");
            }
        }

        if (payload == null || !payload.containsKey("description") || !payload.containsKey("trans_amount")) {
            return ResponseEntity.badRequest().body("INVALID_PAYLOAD");
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
        if ("PENDING_PAYMENT".equals(order.getOrderStatus()) || "Pending".equalsIgnoreCase(order.getOrderStatus())) {
            orderService.confirmQrPayment(order.getId(), "SePay Webhook");
            order.setOrderStatus("PAID");
            if (order.getPayment() != null) {
                order.getPayment().setStatus(com.laptopstore.laptopstore.enums.PaymentStatus.SUCCESS);
                order.getPayment().setTransactionId(payload.getOrDefault("id", "SEPAY_" + System.currentTimeMillis()).toString());
            }
            orderService.saveOrder(order);
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
