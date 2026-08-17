package com.techstore.techstore.Controller;

import com.techstore.techstore.Service.OtpService;
import com.techstore.techstore.Service.OrderService;
import com.techstore.techstore.Service.UserService;
import com.techstore.techstore.entity.Order;
import com.techstore.techstore.entity.User;
import com.techstore.techstore.Repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * OTP Controller - Xử lý OTP verification flow
 */
@Controller
@RequestMapping("/otp")
public class OtpController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Trang nhập OTP
     */
    @GetMapping("/verify")
    public String showOtpPage(@RequestParam(required = false) Long orderId,
                              Model model,
                              Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        if (orderId == null) {
            return "redirect:/";
        }

        // Lấy order để hiển thị thông tin
        Order order = orderService.getOrderById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Kiểm tra order thuộc user hiện tại
        if (!order.getUser().getUsername().equals(principal.getName())) {
            return "redirect:/orders";
        }

        User user = userRepository.findByUsername(principal.getName()).get();

        model.addAttribute("order", order);
        model.addAttribute("phone", user.getPhone());
        model.addAttribute("orderId", orderId);
        model.addAttribute("pageTitle", "Xác thực OTP – LaptopStore");

        return "otp_verification";
    }

    /**
     * API: Gửi OTP SMS
     * POST /otp/send?phone=0123456789
     */
    @PostMapping("/send")
    @ResponseBody
    public ResponseEntity<?> sendOtp(@RequestParam String phone,
                                     Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("success", false, "message", "Chưa đăng nhập"));
            }

            // 🔹 Validate phone format (Vietnam)
            if (!phone.matches("^0\\d{9}$")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Số điện thoại không hợp lệ"));
            }

            // 🔹 Gửi OTP
            otpService.sendOtp(phone);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OTP đã gửi đến số điện thoại của bạn"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Lỗi gửi OTP: " + e.getMessage()));
        }
    }

    /**
     * API: Xác thực OTP
     * POST /otp/verify?phone=0123456789&code=123456&orderId=1
     */
    @PostMapping("/verify")
    @ResponseBody
    public ResponseEntity<?> verifyOtp(@RequestParam String phone,
                                       @RequestParam String code,
                                       @RequestParam Long orderId,
                                       Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("success", false, "message", "Chưa đăng nhập"));
            }

            if (code.isEmpty() || code.length() < 6) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "OTP phải có 6 chữ số"));
            }

            // 🔹 Xác thực OTP
            otpService.verifyOtp(phone, code);

            // 🔹 Cập nhật order
            Order order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            order.setOtpVerified(true);

            String redirectUrl = "/orders/" + orderId;
            if (order.getPayment() != null) {
                if (order.getPayment().getMethod() == com.techstore.techstore.enums.PaymentMethod.SEPAY) {
                    redirectUrl = "/checkout/sepay?orderId=" + orderId;
                } else if (order.getPayment().getMethod() == com.techstore.techstore.enums.PaymentMethod.COD) {
                    order.setOrderStatus("Confirmed");
                    redirectUrl = "/orders/success";
                }
            }
            orderService.saveOrder(order);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OTP xác thực thành công",
                    "redirectUrl", redirectUrl
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * API: Gửi lại OTP
     * POST /otp/resend?phone=0123456789
     */
    @PostMapping("/resend")
    @ResponseBody
    public ResponseEntity<?> resendOtp(@RequestParam String phone,
                                       Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("success", false, "message", "Chưa đăng nhập"));
            }

            otpService.sendOtp(phone);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OTP mới đã được gửi"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
