package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.OrderService;
import com.laptopstore.laptopstore.Service.ProductService;
import com.laptopstore.laptopstore.Service.UserService;
import com.laptopstore.laptopstore.dto.BestSellerDTO;
import com.laptopstore.laptopstore.entity.Order;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;

    // ===================== DASHBOARD =====================
    @GetMapping
    public String dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model
    ) {
        LocalDate now = LocalDate.now();

        // Mặc định 7 ngày gần nhất nếu chưa chọn
        if (endDate == null) {
            endDate = now;
        }
        if (startDate == null) {
            startDate = endDate.minusDays(6);
        }

        // Đảm bảo startDate <= endDate
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        // 🔵 1. Thống kê theo khoảng ngày
        BigDecimal rangeRevenue = orderService.getRevenueByDateRange(startDate, endDate);
        long totalOrders = orderService.countOrdersByDateRange(startDate, endDate);
        long successOrders = orderService.countOrdersByStatusesInRange(startDate, endDate, List.of("Paid", "Completed", "Delivered", "Shipping"));
        long pendingOrders = orderService.countOrdersByStatusesInRange(startDate, endDate, List.of("Pending", "Processing"));
        long cancelledOrders = orderService.countOrdersByStatusesInRange(startDate, endDate, List.of("Cancelled"));

        // AOV (Average Order Value)
        BigDecimal aov = (successOrders > 0)
                ? rangeRevenue.divide(BigDecimal.valueOf(successOrders), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 🔵 2. Tổng số người dùng & tồn kho
        long userCount = userService.countUsers();
        long stockCount = productService.sumTotalStock();
        long lowStockCount = productService.countLowStockProducts(3);

        // 🔵 3. Doanh thu hôm nay & Số đơn mới hôm nay (giữ lại compatibility)
        BigDecimal todayRevenue = orderService.getRevenueByDate(now);
        int newOrders = orderService.countOrdersByDate(now);

        // 🔵 4. Chuỗi dữ liệu biểu đồ doanh thu theo khoảng ngày
        Map<String, Object> chartData = orderService.getDailyRevenueDataInRange(startDate, endDate);

        // 🔵 5. Top 5 sản phẩm bán chạy & 5 đơn gần đây & 5 sản phẩm tồn kho thấp
        List<BestSellerDTO> bestSellers = productService.getTopBestSellers(5);
        List<Order> recentOrders = orderService.getRecentOrders(5);
        List<com.laptopstore.laptopstore.entity.Product> lowStockProducts = productService.getLowStockProducts(3);
        if (lowStockProducts.size() > 5) {
            lowStockProducts = lowStockProducts.subList(0, 5);
        }

        // Truyền model attributes
        model.addAttribute("startDate", startDate.toString());
        model.addAttribute("endDate", endDate.toString());

        model.addAttribute("rangeRevenue", rangeRevenue);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("successOrders", successOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("cancelledOrders", cancelledOrders);
        model.addAttribute("aov", aov);

        model.addAttribute("todayRevenue", todayRevenue);
        model.addAttribute("newOrders", newOrders);
        model.addAttribute("userCount", userCount);
        model.addAttribute("stockCount", stockCount);
        model.addAttribute("lowStockCount", lowStockCount);

        model.addAttribute("dailyLabels", chartData.get("labels"));
        model.addAttribute("dailyRevenue", chartData.get("revenues"));

        model.addAttribute("bestSellers", bestSellers);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("lowStockProducts", lowStockProducts);

        // Active menu
        model.addAttribute("active", "dashboard");
        model.addAttribute("pageTitle", "Dashboard Quản Lý & Phân Tích - LaptopStore Admin");

        return "admin/dashboard";
    }
}
