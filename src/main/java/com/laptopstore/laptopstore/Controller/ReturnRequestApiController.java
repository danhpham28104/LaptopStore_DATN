package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.ReturnRequestService;
import com.laptopstore.laptopstore.Service.UserService;
import com.laptopstore.laptopstore.entity.ReturnRequest;
import com.laptopstore.laptopstore.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("isAuthenticated()")
public class ReturnRequestApiController {

    private final ReturnRequestService returnRequestService;
    private final UserService userService;

    public ReturnRequestApiController(ReturnRequestService returnRequestService, UserService userService) {
        this.returnRequestService = returnRequestService;
        this.userService = userService;
    }

    @PostMapping("/{orderId}/return-request")
    public ResponseEntity<?> submitReturnRequest(@PathVariable Long orderId,
                                                 @RequestBody Map<String, String> body,
                                                 Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Vui lòng đăng nhập để thực hiện thao tác."));
            String reason = body.getOrDefault("reason", "Khách hàng không hài lòng với sản phẩm.");

            ReturnRequest req = returnRequestService.createReturnRequest(orderId, user.getId(), reason);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã gửi yêu cầu trả hàng thành công. Chúng tôi sẽ phản hồi sớm nhất!",
                    "requestId", req.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
