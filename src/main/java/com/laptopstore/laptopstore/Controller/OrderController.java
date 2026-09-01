package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Repository.OrderRepository;
import com.laptopstore.laptopstore.Repository.OrderStatusHistoryRepository;
import com.laptopstore.laptopstore.Service.CartService;
import com.laptopstore.laptopstore.Service.OrderService;
import com.laptopstore.laptopstore.Service.UserService;
import com.laptopstore.laptopstore.entity.*;
import com.laptopstore.laptopstore.enums.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
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
    @Autowired
    private CartService cartService;
    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Autowired
    private OrderRepository orderRepository;


    /** 🔹 Danh sách đơn hàng của người dùng */
    @GetMapping
    public String listOrders(
            @RequestParam(required = false) String status,
            Principal principal,
            Model model) {
        if (principal == null) return "redirect:/login";

        OrderStatus filterStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                filterStatus = OrderStatus.valueOf(status);
            } catch (Exception ignored) {}
        }

        List<Order> orders;
        if (filterStatus == null) {
            orders = orderRepository.findByUser_UsernameOrderByCreatedAtDesc(principal.getName());
        } else if (filterStatus == OrderStatus.PENDING_PAYMENT) {
            orders = orderRepository.findByUser_UsernameAndOrderStatusInOrderByCreatedAtDesc(
                    principal.getName(),
                    List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CONFIRMED, OrderStatus.PACKING)
            );
        } else {
            orders = orderRepository.findByUser_UsernameAndOrderStatusOrderByCreatedAtDesc(
                    principal.getName(),
                    filterStatus
            );
        }

        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status);
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

        List<OrderStatusHistory> histories = orderStatusHistoryRepository
                .findByOrderIdOrderByChangedAtAsc(order.getId());
        model.addAttribute("statusHistories", histories);

        // Thêm thông tin thanh toán nếu có
        if (order.getPayment() != null) {
            model.addAttribute("payment", order.getPayment());
        }

        model.addAttribute("isEligibleForReturn", orderService.isOrderEligibleForReturn(order));
        model.addAttribute("returnWindowDays", orderService.getReturnWindowDays());

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

    /** 🔹 Yêu cầu hoàn trả đơn hàng */
    @PostMapping("/return/{id}")
    public String requestReturnOrder(@PathVariable Long id, Principal principal, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        // 🛡️ BẢO MẬT: Kiểm tra quyền sở hữu đơn hàng
        if (order.getUser() == null || !order.getUser().getUsername().equals(principal.getName())) {
            return "redirect:/orders?error=unauthorized";
        }

        try {
            orderService.requestReturnOrder(id);
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu hoàn trả đơn hàng đã được xử lý thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/orders/" + id;
    }

    /** 🔹 Mua lại đơn hàng cũ */
    @PostMapping("/{id}/reorder")
    public String reorder(@PathVariable Long id, Principal principal,
                          RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        // 1. Lấy đơn hàng + kiểm tra ownership
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getUser() == null || !order.getUser().getUsername().equals(principal.getName())) {
            return "redirect:/orders";
        }

        // 2. Lấy cart của user
        User user = userService.getUserByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Cart cart = cartService.getOrCreateCart(user);

        int addedCount = 0;
        List<String> outOfStockItems = new ArrayList<>();

        // 3. Loop qua OrderItems, thêm vào cart
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                Product p = item.getProduct();
                ProductVariant v = item.getVariant();

                String pName = (p != null && p.getName() != null) ? p.getName() : "Sản phẩm";

                // Check product còn tồn tại và không bị xóa
                if (p == null || p.isDeleted()) {
                    outOfStockItems.add(pName + " (không còn bán)");
                    continue;
                }

                int available = (v != null) ? (v.getStock() != null ? v.getStock() : 0) : (p.getStock() != null ? p.getStock() : 0);
                if (available <= 0) {
                    outOfStockItems.add(pName + " (hết hàng)");
                    continue;
                }

                int qty = Math.min(item.getQuantity(), available);
                cartService.addToCart(cart, p, v, qty);
                addedCount++;
            }
        }

        // 4. Flash message
        if (addedCount > 0) {
            redirectAttributes.addFlashAttribute("success",
                    addedCount + " sản phẩm đã được thêm vào giỏ hàng");
            redirectAttributes.addFlashAttribute("successMessage",
                    addedCount + " sản phẩm đã được thêm vào giỏ hàng");
        }
        if (!outOfStockItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("warning",
                    "Một số sản phẩm không thể thêm: " + String.join(", ", outOfStockItems));
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Một số sản phẩm không thể thêm: " + String.join(", ", outOfStockItems));
        }

        return "redirect:/cart";
    }

    /** 🔹 Trang xác nhận thành công */
    @GetMapping("/success")
    public String orderSuccess(Model model) {
        model.addAttribute("pageTitle", "Đặt hàng thành công – LaptopStore");
        return "order_success"; // ↔ templates/order_success.html
    }
}
