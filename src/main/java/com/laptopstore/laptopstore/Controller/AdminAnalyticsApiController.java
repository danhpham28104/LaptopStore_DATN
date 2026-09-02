package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.AnalyticsService;
import com.laptopstore.laptopstore.dto.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API Endpoints cho Admin Analytics & BI Dashboard.
 * Tất cả endpoints trả về JSON và yêu cầu ROLE_ADMIN.
 */
@RestController
@RequestMapping("/admin/api/analytics")
@PreAuthorize("hasAnyRole('ADMIN', 'SALE', 'WAREHOUSE')")
public class AdminAnalyticsApiController {

    private final AnalyticsService analyticsService;
    private final com.laptopstore.laptopstore.Service.InventoryAnalyticsService inventoryAnalyticsService;
    private final com.laptopstore.laptopstore.Service.VoucherAnalyticsService voucherAnalyticsService;
    private final com.laptopstore.laptopstore.Service.CustomerAnalyticsService customerAnalyticsService;
    private final com.laptopstore.laptopstore.Service.AiAnalyticsService aiAnalyticsService;

    public AdminAnalyticsApiController(AnalyticsService analyticsService,
                                      com.laptopstore.laptopstore.Service.InventoryAnalyticsService inventoryAnalyticsService,
                                      com.laptopstore.laptopstore.Service.VoucherAnalyticsService voucherAnalyticsService,
                                      com.laptopstore.laptopstore.Service.CustomerAnalyticsService customerAnalyticsService,
                                      com.laptopstore.laptopstore.Service.AiAnalyticsService aiAnalyticsService) {
        this.analyticsService = analyticsService;
        this.inventoryAnalyticsService = inventoryAnalyticsService;
        this.voucherAnalyticsService = voucherAnalyticsService;
        this.customerAnalyticsService = customerAnalyticsService;
        this.aiAnalyticsService = aiAnalyticsService;
    }

    /** GET /admin/api/analytics/overview?from=2026-08-01&to=2026-08-31 */
    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewDto> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate now = LocalDate.now();
        if (to == null) to = now;
        if (from == null) from = to.minusDays(29); // Mặc định 30 ngày gần nhất

        return ResponseEntity.ok(analyticsService.getDashboardOverview(from, to));
    }

    /** GET /admin/api/analytics/revenue?from=...&to=...&groupBy=day */
    @GetMapping("/revenue")
    public ResponseEntity<List<RevenueTrendDto>> getRevenueTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String groupBy) {

        LocalDate now = LocalDate.now();
        if (to == null) to = now;
        if (from == null) from = to.minusDays(29);

        return ResponseEntity.ok(analyticsService.getRevenueTrend(from, to, groupBy));
    }

    /** GET /admin/api/analytics/categories?from=...&to=... */
    @GetMapping("/categories")
    public ResponseEntity<List<CategorySalesDto>> getSalesByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate now = LocalDate.now();
        if (to == null) to = now;
        if (from == null) from = to.minusDays(29);

        return ResponseEntity.ok(analyticsService.getSalesByCategory(from, to));
    }

    /** GET /admin/api/analytics/brands?from=...&to=... */
    @GetMapping("/brands")
    public ResponseEntity<List<BrandSalesDto>> getSalesByBrand(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate now = LocalDate.now();
        if (to == null) to = now;
        if (from == null) from = to.minusDays(29);

        return ResponseEntity.ok(analyticsService.getSalesByBrand(from, to));
    }

    /** GET /admin/api/analytics/top-products?from=...&to=...&sortBy=revenue&limit=10 */
    @GetMapping("/top-products")
    public ResponseEntity<List<ProductPerformanceDto>> getTopProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "revenue") String sortBy,
            @RequestParam(defaultValue = "10") int limit) {

        LocalDate now = LocalDate.now();
        if (to == null) to = now;
        if (from == null) from = to.minusDays(29);

        return ResponseEntity.ok(analyticsService.getTopProducts(from, to, sortBy, limit));
    }

    /** GET /admin/api/analytics/funnel?from=...&to=... */
    @GetMapping("/funnel")
    public ResponseEntity<ConversionFunnelDto> getConversionFunnel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate now = LocalDate.now();
        if (to == null) to = now;
        if (from == null) from = to.minusDays(29);

        return ResponseEntity.ok(analyticsService.getConversionFunnel(from, to));
    }

    /** GET /admin/api/analytics/order-status-dist?from=...&to=... */
    @GetMapping("/order-status-dist")
    public ResponseEntity<List<OrderStatusDistDto>> getOrderStatusDist(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate now = LocalDate.now();
        if (to == null) to = now;
        if (from == null) from = to.minusDays(29);

        return ResponseEntity.ok(analyticsService.getOrderStatusDistribution(from, to));
    }

    // ── Phase 3 Advanced Analytics Endpoints ─────────────────────────────────

    /** GET /admin/api/analytics/inventory */
    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<List<InventoryAnalyticsDto>> getInventoryAnalytics() {
        return ResponseEntity.ok(inventoryAnalyticsService.getAllInventoryMetrics());
    }

    /** GET /admin/api/analytics/inventory/low-stock */
    @GetMapping("/inventory/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<List<LowStockIntelligenceDto>> getLowStockIntelligence() {
        return ResponseEntity.ok(inventoryAnalyticsService.getLowStockIntelligence());
    }

    /** GET /admin/api/analytics/inventory/dead-stock */
    @GetMapping("/inventory/dead-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<List<DeadStockDto>> getDeadStock() {
        return ResponseEntity.ok(inventoryAnalyticsService.getDeadStock());
    }

    /** GET /admin/api/analytics/vouchers?from=...&to=... */
    @GetMapping("/vouchers")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALE')")
    public ResponseEntity<List<VoucherAnalyticsDto>> getVoucherAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate now = LocalDate.now();
        if (to == null) to = now;
        if (from == null) from = to.minusDays(29);

        return ResponseEntity.ok(voucherAnalyticsService.getVoucherAnalytics(from, to));
    }

    /** GET /admin/api/analytics/customers?from=...&to=... */
    @GetMapping("/customers")
    public ResponseEntity<CustomerAnalyticsDto> getCustomerAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate now = LocalDate.now();
        if (to == null) to = now;
        if (from == null) from = to.minusDays(29);

        return ResponseEntity.ok(customerAnalyticsService.getCustomerAnalytics(from, to));
    }

    // ── Phase 4 AI Analytics Endpoint ───────────────────────────────────────

    /** GET /admin/api/analytics/ai-chat?from=...&to=... */
    @GetMapping("/ai-chat")
    public ResponseEntity<AiAnalyticsDto> getAiAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate now = LocalDate.now();
        if (to == null) to = now;
        if (from == null) from = to.minusDays(29);

        return ResponseEntity.ok(aiAnalyticsService.getAiAnalytics(from, to));
    }
}
