package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.OrderService;
import com.laptopstore.laptopstore.Service.UserService;
import com.laptopstore.laptopstore.entity.Order;
import com.laptopstore.laptopstore.entity.User;
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
        model.addAttribute("pageTitle", "Đơn hàng của bạn – LaptopStore");
        return "orders"; // ↔ templates/orders.html
    }

    /** 🔹 Chi tiết một đơn hàng */
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        // 🛡️ BẢO MẬT: Kiểm tra quyền sở hữu đơn hàng
        if (order.getUser() == null || !order.getUser().getUsername().equals(principal.getName())) {
            return "redirect:/orders?error=unauthorized";
        }

        model.addAttribute("order", order);

        // Thêm thông tin thanh toán nếu có
        if (order.getPayment() != null) {
            model.addAttribute("payment", order.getPayment());
        }

        model.addAttribute("pageTitle", "Chi tiết đơn hàng #" + id);
        return "order_detail"; // ↔ templates/order_detail.html
    }

    /** 🔹 Hủy đơn hàng */
    @PostMapping("/cancel/{id}")
    public String cancelOrder(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";

        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        // 🛡️ BẢO MẬT: Kiểm tra quyền sở hữu đơn hàng
        if (order.getUser() == null || !order.getUser().getUsername().equals(principal.getName())) {
            return "redirect:/orders?error=unauthorized";
        }

        orderService.cancelOrder(id);
        return "redirect:/orders?cancelSuccess=true";
    }


    /** 🔹 Trang xác nhận thành công */
    @GetMapping("/success")
    public String orderSuccess(Model model) {
        model.addAttribute("pageTitle", "Đặt hàng thành công – LaptopStore");
        return "order_success"; // ↔ templates/order_success.html
    }
}
