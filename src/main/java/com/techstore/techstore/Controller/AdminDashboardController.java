package com.techstore.techstore.Controller;

import com.techstore.techstore.Service.OrderService;
import com.techstore.techstore.Service.ProductService;
import com.techstore.techstore.Service.UserService;
import com.techstore.techstore.dto.BestSellerDTO;
import com.techstore.techstore.entity.Order;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;

    // ===================== DASHBOARD =====================
    @GetMapping
    public String dashboard(Model model) {

        // 🔵 1. Doanh thu hôm nay
        BigDecimal todayRevenue = orderService.getRevenueByDate(LocalDate.now());
        model.addAttribute("todayRevenue", todayRevenue);

        // 🔵 2. Số đơn hàng mới hôm nay
        int newOrders = orderService.countOrdersByDate(LocalDate.now());
        model.addAttribute("newOrders", newOrders);

        // 🔵 3. Tổng số người dùng
        long userCount = userService.countUsers();
        model.addAttribute("userCount", userCount);

        // 🔵 4. Tổng tồn kho
        long stockCount = productService.sumTotalStock();
        model.addAttribute("stockCount", stockCount);

        // ============================================================
        // 🔵 5. DOANH THU 7 NGÀY GẦN NHẤT (biểu đồ đường)
        // ============================================================
        List<String> labels = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) { // 7 ngày
            LocalDate day = today.minusDays(i);

            labels.add(day.format(fmt));   // Ví dụ: 09/11, 10/11,...

            BigDecimal rev = orderService.getRevenueByDate(day);
            revenues.add(rev != null ? rev : BigDecimal.ZERO);
        }

        model.addAttribute("dailyLabels", labels);
        model.addAttribute("dailyRevenue", revenues);
        // ============================================================


        // 🔵 6. Top 5 sản phẩm bán chạy
        List<BestSellerDTO> bestSellers = productService.getTopBestSellers(5);
        model.addAttribute("bestSellers", bestSellers);

        // 🔵 7. 5 đơn hàng gần nhất
        List<Order> recentOrders = orderService.getRecentOrders(5);
        model.addAttribute("recentOrders", recentOrders);

        // Active menu
        model.addAttribute("active", "dashboard");

        // Title
        model.addAttribute("pageTitle", "Dashboard - TechStore Admin");

        return "admin/dashboard";
    }
}
