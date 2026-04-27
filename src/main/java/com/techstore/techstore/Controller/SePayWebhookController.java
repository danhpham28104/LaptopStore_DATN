package com.techstore.techstore.Controller;

import com.techstore.techstore.Service.OrderService;
import com.techstore.techstore.entity.Order;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.Map;

@RestController
@RequestMapping("/api/sepay")
public class SePayWebhookController {
    @Autowired
    private OrderService orderService;

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody Map<String, Object> payload) {

        System.out.println("Webhook từ SePay: " + payload);

        String orderCode = payload.get("description").toString();
        long amount = Long.parseLong(payload.get("trans_amount").toString());


        Order order = orderService.getByOrderCode(orderCode);

        if (order == null) {
            return ResponseEntity.ok("ORDER_NOT_FOUND");
        }

        // kiểm tra số tiền đúng
        if (order.getTotalAmount().longValue() != amount) {
            return ResponseEntity.ok("INVALID_AMOUNT");
        }

        // Cập nhật trạng thái đơn hàng
        order.setOrderStatus("PAID");
        orderService.saveOrder(order);

        return ResponseEntity.ok("OK");
    }
}
