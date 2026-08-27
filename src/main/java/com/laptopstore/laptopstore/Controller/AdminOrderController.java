package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.OrderService;
import com.laptopstore.laptopstore.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    /** 🔹 Danh sách tất cả đơn hàng (Hỗ trợ Lọc theo Trạng thái & Ngày từ Dashboard Cards) */
    @GetMapping
    public String listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        if ((status != null && !status.isBlank()) || startDate != null || endDate != null) {
            List<Order> filteredOrders = orderService.filterOrders(status, startDate, endDate);
            model.addAttribute("orders", filteredOrders);
            model.addAttribute("searchMode", true); // Tắt phân trang khi ở chế độ filter
            model.addAttribute("filterStatus", status);
            model.addAttribute("startDate", startDate != null ? startDate.toString() : "");
            model.addAttribute("endDate", endDate != null ? endDate.toString() : "");
        } else {
            Page<Order> pageData = orderService.getPagedOrders(page, size);
            model.addAttribute("orders", pageData.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", pageData.getTotalPages());
            model.addAttribute("pageSize", size);
            model.addAttribute("searchMode", false);  // 👈 chế độ phân trang
        }

        model.addAttribute("active", "orders");
        return "admin/orders";
    }

    /** 🔹 Cập nhật trạng thái đơn hàng */
    @PostMapping("/update-status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam String status) {
        orderService.updateStatus(id, status);
        return "redirect:/admin/orders?updated=true";
    }

    /** 🔹 Cập nhật ghi chú nội bộ Admin */
    @PostMapping("/{id}/note")
    public String updateNote(
            @PathVariable Long id,
            @RequestParam(required = false) String adminNote,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
    ) {
        orderService.updateAdminNote(id, adminNote);
        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu ghi chú nội bộ thành công!");
        return "redirect:/admin/orders/" + id;
    }

    /** 🔹 In hóa đơn HTML */
    @GetMapping("/{id}/print")
    public String printOrder(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id).orElse(null);
        if (order == null) return "redirect:/admin/orders?error=notfound";

        model.addAttribute("order", order);
        model.addAttribute("items", order.getOrderItems());
        model.addAttribute("pageTitle", "In hóa đơn - " + (order.getOrderCode() != null ? order.getOrderCode() : "#" + id));
        return "admin/order_print";
    }

    /** 🔹 Xem chi tiết đơn hàng */
    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id).orElse(null);
        if (order == null) return "redirect:/admin/orders?error=notfound";

        model.addAttribute("order", order);
        model.addAttribute("items", order.getOrderItems());
        model.addAttribute("pageTitle", "Chi tiết đơn hàng #" + id);
        return "admin/order_detail";
    }

    /** 🔹 Xóa đơn hàng */
    @PostMapping("/delete/{id}")
    public String deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return "redirect:/admin/orders";
    }

    @GetMapping("/search")
    public String searchOrders(
            @RequestParam String q,
            Model model
    ) {
        List<Order> results;

        if (q.matches("\\d+")) { // tìm theo ID
            orderService.getOrderById(Long.parseLong(q))
                    .ifPresentOrElse(
                            order -> model.addAttribute("orders", List.of(order)),
                            () -> model.addAttribute("orders", List.of())
                    );
        } else {
            model.addAttribute("orders", List.of());
        }

        model.addAttribute("q", q);
        model.addAttribute("searchMode", true); // 👈 tắt phân trang
        model.addAttribute("active", "orders");

        return "admin/orders";
    }

}
