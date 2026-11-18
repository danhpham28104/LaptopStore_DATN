package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.enums.OrderStatus;
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

    /** 🔹 Cập nhật trạng thái đơn hàng (qua Service Layer, có validation chuyển trạng thái) */
    @PostMapping("/update-status/{id}")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String redirectTo,
            @RequestParam(required = false) String note,
            java.security.Principal principal,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
    ) {
        String performedBy = (principal != null) ? ("Admin: " + principal.getName()) : "Admin";
        try {
            OrderStatus newStatus = OrderStatus.fromStringStrict(status);
            Order updated = orderService.updateOrderStatus(id, newStatus, performedBy, note);
            String code = updated.getOrderCode() != null ? updated.getOrderCode() : ("#" + id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã chuyển đơn hàng " + code + " sang trạng thái \"" + newStatus.getDisplayName() + "\".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        if ("detail".equalsIgnoreCase(redirectTo)) {
            return "redirect:/admin/orders/" + id;
        }
        return "redirect:/admin/orders";
    }

    /** 🔹 Cập nhật ghi chú nội bộ Admin */
    @PostMapping("/{id}/note")
    public String updateNote(
            @PathVariable Long id,
            @RequestParam(required = false) String adminNote,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
    ) {
        try {
            orderService.updateAdminNote(id, adminNote);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu ghi chú nội bộ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    /** 🔹 Cập nhật thông tin giao hàng (bị khóa khi đơn đã Hoàn thành) */
    @PostMapping("/{id}/shipping")
    public String updateShippingInfo(
            @PathVariable Long id,
            @RequestParam String receiverName,
            @RequestParam String receiverPhone,
            @RequestParam String shippingAddress,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
    ) {
        try {
            orderService.updateOrderShippingInfo(id, receiverName, receiverPhone, shippingAddress);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin giao hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    /** 🔹 Cập nhật số lượng sản phẩm trong đơn (bị khóa khi đơn đã Hoàn thành) */
    @PostMapping("/{id}/items/{itemId}/quantity")
    public String updateOrderItemQuantity(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestParam int quantity,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes
    ) {
        try {
            orderService.updateOrderItemQuantity(id, itemId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật số lượng sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
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
            @RequestParam(required = false) String q,
            Model model
    ) {
        List<Order> results = orderService.searchOrders(q);
        model.addAttribute("orders", results);
        model.addAttribute("q", q);
        model.addAttribute("searchMode", true); // 👈 tắt phân trang
        model.addAttribute("active", "orders");

        return "admin/orders";
    }

    @GetMapping("/api/suggest")
    @ResponseBody
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> suggestOrders(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "limit", defaultValue = "8") int limit
    ) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 15));
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        return orderService.searchOrders(q.trim()).stream()
                .limit(safeLimit)
                .map(o -> {
                    java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
                    item.put("id", o.getId());
                    item.put("orderCode", o.getOrderCode() != null ? o.getOrderCode() : ("#" + o.getId()));
                    item.put("receiverName", o.getReceiverName() != null ? o.getReceiverName() : "");
                    item.put("receiverPhone", o.getReceiverPhone() != null ? o.getReceiverPhone() : "");
                    item.put("totalAmount", o.getTotalAmount() != null ? o.getTotalAmount() : java.math.BigDecimal.ZERO);
                    item.put("statusDisplay", o.getOrderStatus() != null ? o.getOrderStatus().getDisplayName() : "");
                    item.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().format(fmt) : "");
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @PostMapping("/bulk-status")
    @ResponseBody
    public java.util.Map<String, Object> bulkUpdateStatus(
            @RequestBody java.util.Map<String, Object> body,
            java.security.Principal principal
    ) {
        List<Long> ids = ((List<?>) body.get("ids")).stream()
                .map(o -> Long.parseLong(o.toString())).collect(java.util.stream.Collectors.toList());
        String status = (String) body.get("status");
        String performedBy = (principal != null) ? ("Admin: " + principal.getName()) : "Admin";

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.fromStringStrict(status);
        } catch (Exception e) {
            result.put("updated", 0);
            result.put("failed", ids.size());
            result.put("total", ids.size());
            result.put("errors", List.of(e.getMessage()));
            return result;
        }

        int updated = 0;
        List<String> errors = new java.util.ArrayList<>();
        for (Long id : ids) {
            try {
                orderService.updateOrderStatus(id, newStatus, performedBy, null);
                updated++;
            } catch (Exception e) {
                errors.add("Đơn #" + id + ": " + e.getMessage());
            }
        }

        result.put("updated", updated);
        result.put("failed", errors.size());
        result.put("total", ids.size());
        result.put("errors", errors);
        return result;
    }

}
