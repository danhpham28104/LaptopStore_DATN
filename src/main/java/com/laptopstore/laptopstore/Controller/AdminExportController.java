package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.OrderService;
import com.laptopstore.laptopstore.Service.ProductService;
import com.laptopstore.laptopstore.entity.Order;
import com.laptopstore.laptopstore.entity.Product;
import com.laptopstore.laptopstore.entity.ProductVariant;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/export")
@RequiredArgsConstructor
public class AdminExportController {

    private final OrderService orderService;
    private final ProductService productService;

    /**
     * 1. Export CSV Danh sách đơn hàng theo khoảng ngày
     */
    @GetMapping("/orders")
    public void exportOrdersCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response
    ) throws IOException {
        LocalDate now = LocalDate.now();
        if (endDate == null) endDate = now;
        if (startDate == null) startDate = endDate.minusDays(30);

        List<Order> orders = orderService.getOrdersByDateRange(startDate, endDate);

        response.setContentType("text/csv; charset=UTF-8");
        String fileName = "don_hang_" + startDate + "_den_" + endDate + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            // Write UTF-8 BOM for Excel compatibility
            writer.write('\uFEFF');

            // Header CSV
            writer.println("Mã đơn,Người nhận,Số điện thoại,Địa chỉ,Ngày đặt,Tổng tiền (VNĐ),Trạng thái,Xác thực OTP,Phương thức thanh toán");

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (Order o : orders) {
                String code = escapeCsv(o.getOrderCode());
                String receiver = escapeCsv(o.getReceiverName());
                String phone = escapeCsv(o.getReceiverPhone());
                String address = escapeCsv(o.getShippingAddress());
                String dateStr = o.getCreatedAt() != null ? o.getCreatedAt().format(fmt) : "";
                String total = o.getTotalAmount() != null ? o.getTotalAmount().toString() : "0";
                String status = escapeCsv(o.getOrderStatus() != null ? o.getOrderStatus().getDisplayName() : "");
                String otp = (o.getOtpVerified() != null && o.getOtpVerified()) ? "Đã xác thực" : "Chưa xác thực";
                String paymentMethod = (o.getPayment() != null && o.getPayment().getMethod() != null)
                        ? o.getPayment().getMethod().name() : "N/A";

                writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        code, receiver, phone, address, dateStr, total, status, otp, paymentMethod));
            }
            writer.flush();
        }
    }

    /**
     * 2. Export CSV Báo cáo doanh thu theo từng ngày
     */
    @GetMapping("/revenue")
    public void exportRevenueCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response
    ) throws IOException {
        LocalDate now = LocalDate.now();
        if (endDate == null) endDate = now;
        if (startDate == null) startDate = endDate.minusDays(30);

        Map<String, Object> chartData = orderService.getDailyRevenueDataInRange(startDate, endDate);
        @SuppressWarnings("unchecked")
        List<String> labels = (List<String>) chartData.get("labels");
        @SuppressWarnings("unchecked")
        List<BigDecimal> revenues = (List<BigDecimal>) chartData.get("revenues");

        response.setContentType("text/csv; charset=UTF-8");
        String fileName = "doanh_thu_" + startDate + "_den_" + endDate + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write('\uFEFF');
            writer.println("Ngày,Doanh thu (VNĐ)");

            if (labels != null && revenues != null) {
                for (int i = 0; i < labels.size(); i++) {
                    String dateLabel = labels.get(i);
                    BigDecimal rev = revenues.get(i);
                    writer.println(String.format("%s,%s", dateLabel, rev != null ? rev.toString() : "0"));
                }
            }
            writer.flush();
        }
    }

    /**
     * 3. Export CSV Danh sách sản phẩm và tồn kho
     */
    @GetMapping("/products")
    public void exportProductsCsv(HttpServletResponse response) throws IOException {
        List<Product> products = productService.getAllProducts();

        response.setContentType("text/csv; charset=UTF-8");
        String fileName = "ton_kho_san_pham_" + LocalDate.now() + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write('\uFEFF');
            writer.println("ID,Mẫu Model,Tên sản phẩm,Thương hiệu,Giá niêm yết (VNĐ),Giảm giá (%),Giá bán (VNĐ),Tồn kho tổng,Chi tiết biến thể,Trạng thái kho");

            for (Product p : products) {
                String id = p.getId() != null ? p.getId().toString() : "";
                String model = escapeCsv(p.getModel());
                String name = escapeCsv(p.getName());
                String brand = p.getBrand() != null ? escapeCsv(p.getBrand().getName()) : "Khác";
                String price = p.getPrice() != null ? p.getPrice().toString() : "0";
                String salePercent = p.getSalePercent() != null ? p.getSalePercent().toString() : "0";
                String finalPrice = p.getFinalPrice() != null ? p.getFinalPrice().toString() : "0";
                int stock = p.getStock() != null ? p.getStock() : 0;

                // Tóm tắt biến thể
                StringBuilder variantSb = new StringBuilder();
                if (p.getVariants() != null && !p.getVariants().isEmpty()) {
                    for (ProductVariant v : p.getVariants()) {
                        variantSb.append(v.getColor()).append("/").append(v.getStorage())
                                 .append("(SL:").append(v.getStock()).append(") ");
                    }
                } else {
                    variantSb.append("Không có biến thể");
                }
                String variantStr = escapeCsv(variantSb.toString().trim());
                String stockStatus = stock <= 3 ? "Sắp hết hàng" : "Còn hàng";

                writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%d,%s,%s",
                        id, model, name, brand, price, salePercent, finalPrice, stock, variantStr, stockStatus));
            }
            writer.flush();
        }
    }

    private String escapeCsv(String text) {
        if (text == null) return "";
        String escaped = text.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
