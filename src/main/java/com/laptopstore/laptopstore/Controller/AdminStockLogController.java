package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Repository.ProductRepository;
import com.laptopstore.laptopstore.Repository.ProductVariantRepository;
import com.laptopstore.laptopstore.Service.ProductService;
import com.laptopstore.laptopstore.Service.StockLogService;
import com.laptopstore.laptopstore.entity.Product;
import com.laptopstore.laptopstore.entity.ProductVariant;
import com.laptopstore.laptopstore.entity.StockLog;
import com.laptopstore.laptopstore.enums.StockLogType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller quản lý lịch sử xuất/nhập kho (Stock Log) & Nhập hàng thủ công.
 * Dành cho ROLE_ADMIN và ROLE_WAREHOUSE.
 */
@Controller
@RequestMapping("/admin/stock")
public class AdminStockLogController {

    @Autowired private StockLogService stockLogService;
    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository productVariantRepository;

    /**
     * Trang danh sách lịch sử kho, hỗ trợ lọc theo type, ngày, chỉ số thống kê & Modal nhập hàng.
     */
    @GetMapping
    public String listStockLog(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        StockLogType logType = null;
        if (type != null && !type.isBlank()) {
            try { logType = StockLogType.valueOf(type.toUpperCase()); } catch (Exception ignored) {}
        }

        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.atTime(23, 59, 59) : null;

        Page<StockLog> logs = stockLogService.filter(null, logType, fromDt, toDt,
            PageRequest.of(page, size, Sort.by("createdAt").descending()));

        // Tính toán 4 chỉ số thống kê trong trang hiện tại / lọc
        long totalImportQty = 0;
        long totalExportQty = 0;
        long totalRestoreQty = 0;
        long totalAdjustCount = 0;

        for (StockLog log : logs.getContent()) {
            if (log.getType() == StockLogType.IMPORT) {
                totalImportQty += log.getQuantity();
            } else if (log.getType() == StockLogType.EXPORT_ORDER || log.getType() == StockLogType.CONFIRM_RESERVE) {
                totalExportQty += Math.abs(log.getQuantity());
            } else if (log.getType() == StockLogType.CANCEL_RESTORE) {
                totalRestoreQty += Math.abs(log.getQuantity());
            } else if (log.getType() == StockLogType.ADJUSTMENT) {
                totalAdjustCount++;
            }
        }

        model.addAttribute("logs", logs);
        model.addAttribute("logTypes", StockLogType.values());
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("selectedType", type);
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logs.getTotalPages());

        // Thống kê metrics
        model.addAttribute("totalImportQty", totalImportQty);
        model.addAttribute("totalExportQty", totalExportQty);
        model.addAttribute("totalRestoreQty", totalRestoreQty);
        model.addAttribute("totalAdjustCount", totalAdjustCount);

        model.addAttribute("active", "stock");
        model.addAttribute("pageTitle", "Lịch Sử & Quản Lý Kho Hàng – LaptopStore Admin");
        return "admin/stock_log";
    }

    /**
     * 📥 Thực hiện Nhập hàng thủ công hoặc Điều chỉnh kho kiểm kê
     */
    @PostMapping("/import")
    public String manualImport(
            @RequestParam Long productId,
            @RequestParam(required = false) Long variantId,
            @RequestParam StockLogType type,
            @RequestParam int quantity,
            @RequestParam(required = false) BigDecimal importPrice,
            @RequestParam(required = false) String note,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        String performedBy = (principal != null) ? principal.getName() : "ADMIN";
        Product product = productService.getProductById(productId).orElse(null);

        if (product == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
            return "redirect:/admin/stock";
        }

        if (quantity == 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Số lượng biến động phải khác 0!");
            return "redirect:/admin/stock";
        }

        String noteText = (note != null && !note.isBlank()) ? note : (type == StockLogType.IMPORT ? "Nhập kho thủ công" : "Điều chỉnh kiểm kê");

        if (variantId != null && variantId > 0) {
            ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
            if (variant != null) {
                variant.setStock(variant.getStock() + quantity);
                if (importPrice != null && importPrice.compareTo(BigDecimal.ZERO) > 0) {
                    variant.setImportPrice(importPrice);
                }
                productVariantRepository.save(variant);
                product.updateTotalStock();
                productRepository.save(product);

                stockLogService.logVariant(product, variant, null, type, quantity, variant.getStock(), performedBy, noteText);
            }
        } else {
            product.setStock(product.getStock() + quantity);
            if (importPrice != null && importPrice.compareTo(BigDecimal.ZERO) > 0) {
                product.setImportPrice(importPrice);
            }
            productRepository.save(product);

            stockLogService.log(product, null, type, quantity, product.getStock(), performedBy, noteText);
        }

        redirectAttributes.addFlashAttribute("successMessage", "✅ Đã cập nhật tồn kho và ghi nhận nhật ký thành công!");
        return "redirect:/admin/stock";
    }

    /**
     * 📄 Xuất Báo Cáo Nhật Ký Kho (File CSV UTF-8 BOM)
     */
    @GetMapping("/export/csv")
    public void exportStockLogCsv(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletResponse response
    ) throws Exception {
        StockLogType logType = null;
        if (type != null && !type.isBlank()) {
            try { logType = StockLogType.valueOf(type.toUpperCase()); } catch (Exception ignored) {}
        }

        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.atTime(23, 59, 59) : null;

        Page<StockLog> logs = stockLogService.filter(null, logType, fromDt, toDt,
            PageRequest.of(0, 5000, Sort.by("createdAt").descending()));

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"NhatKyKho_" + LocalDate.now() + ".csv\"");

        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF'); // UTF-8 BOM for Excel display
        writer.println("ID,Thời Gian,Loại Biến Động,Sản Phẩm,Biến Thể,Biến Động,Tồn Sau,Mã Đơn,Người Thực Hiện,Ghi Chú");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (StockLog log : logs.getContent()) {
            String time = log.getCreatedAt() != null ? log.getCreatedAt().format(fmt) : "";
            String logTypeLabel = log.getType() != null ? log.getType().getLabel() : "";
            String prodName = log.getProduct() != null ? log.getProduct().getName().replace(",", " ") : "";
            String variantInfo = log.getVariant() != null ? (log.getVariant().getColor() + " - " + log.getVariant().getStorage()) : "";
            String changeQty = (log.getQuantity() > 0 ? "+" : "") + log.getQuantity();
            String stockAfter = String.valueOf(log.getStockAfter());
            String orderCode = log.getOrder() != null ? log.getOrder().getOrderCode() : "";
            String performedBy = log.getPerformedBy() != null ? log.getPerformedBy() : "";
            String note = log.getNote() != null ? log.getNote().replace(",", " ") : "";

            writer.println(String.format("%d,%s,%s,\"%s\",\"%s\",%s,%s,%s,%s,\"%s\"",
                log.getId(), time, logTypeLabel, prodName, variantInfo, changeQty, stockAfter, orderCode, performedBy, note));
        }
        writer.flush();
    }
}
