package com.techstore.techstore.Controller;

import com.techstore.techstore.Service.OrderService;
import com.techstore.techstore.Service.UserService;
import com.techstore.techstore.entity.Order;
import com.techstore.techstore.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Controller quản lý Đơn hàng người dùng – tích hợp Checkout
 */
@Controller
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;


    /** 🔹 Danh sách đơn hàng của người dùng */
    @GetMapping
    public String userOrders(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        User user = userService.getUserByUsername(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));

        List<Order> orders = orderService.getOrdersByUser(user.getId());
        model.addAttribute("orders", orders);
        model.addAttribute("pageTitle", "Đơn hàng của bạn – TechStore");
        return "orders"; // ↔ templates/orders.html
    }

    /** 🔹 Chi tiết một đơn hàng */
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        model.addAttribute("order", order);

        //  Thêm nếu bạn muốn hiển thị thông tin thanh toán trong giao diện
        if (order.getPayment() != null) {
            model.addAttribute("payment", order.getPayment());
        }

        model.addAttribute("pageTitle", "Chi tiết đơn hàng #" + id);
        return "order_detail"; // ↔ templates/order_detail.html
    }


    @PostMapping("/cancel/{id}")
    public String cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return "redirect:/orders?cancelSuccess=true";
    }


    /** 🔹 Trang xác nhận thành công */
    @GetMapping("/success")
    public String orderSuccess(Model model) {
        model.addAttribute("pageTitle", "Đặt hàng thành công – TechStore");
        return "order_success"; // ↔ templates/order_success.html
    }
}
