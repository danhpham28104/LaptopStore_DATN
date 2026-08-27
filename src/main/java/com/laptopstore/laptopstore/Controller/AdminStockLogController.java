package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.StockLogService;
import com.laptopstore.laptopstore.entity.StockLog;
import com.laptopstore.laptopstore.enums.StockLogType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Controller quản lý lịch sử xuất/nhập kho (Stock Log).
 * Chỉ dành cho ROLE_ADMIN và ROLE_WAREHOUSE.
 */
@Controller
@RequestMapping("/admin/stock")
public class AdminStockLogController {

    @Autowired
    private StockLogService stockLogService;

    /**
     * Trang danh sách lịch sử kho, hỗ trợ lọc theo type và ngày.
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

        model.addAttribute("logs", logs);
        model.addAttribute("logTypes", StockLogType.values());
        model.addAttribute("selectedType", type);
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logs.getTotalPages());
        model.addAttribute("active", "stock");
        model.addAttribute("pageTitle", "Lịch sử Kho Hàng – LaptopStore Admin");
        return "admin/stock_log";
    }
}
