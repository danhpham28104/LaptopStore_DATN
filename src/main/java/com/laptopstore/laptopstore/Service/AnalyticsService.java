package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.*;
import com.laptopstore.laptopstore.dto.*;
import com.laptopstore.laptopstore.enums.EventType;
import com.laptopstore.laptopstore.enums.OrderStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.util.*;

/**
 * Service tổng hợp Business Intelligence (BI) & Analytics chuyên sâu cho Admin.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final List<OrderStatus> SUCCESS_STATUSES = List.of(
            OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.PACKING,
            OrderStatus.SHIPPING, OrderStatus.DELIVERED
    );

    private static final List<OrderStatus> CANCELLED_STATUSES = List.of(
            OrderStatus.CANCELLED, OrderStatus.RETURNED, OrderStatus.REFUNDED
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AnalyticsEventRepository analyticsEventRepository;
    private final OrderService orderService;
    private final ReviewService reviewService;

    public AnalyticsService(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            ProductRepository productRepository,
                            UserRepository userRepository,
                            AnalyticsEventRepository analyticsEventRepository,
                            OrderService orderService,
                            ReviewService reviewService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.analyticsEventRepository = analyticsEventRepository;
        this.orderService = orderService;
        this.reviewService = reviewService;
    }

    // ── 1. Dashboard Overview KPIs ───────────────────────────────────────────

    public DashboardOverviewDto getDashboardOverview(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        // Revenue & Orders
        BigDecimal totalRevenue = orderRepository.sumRevenueByDateRange(start, end, SUCCESS_STATUSES)
                .orElse(BigDecimal.ZERO);
        long paidOrdersCount = orderRepository.countOrdersByDateRangeAndStatuses(start, end, SUCCESS_STATUSES);
        long totalOrdersCount = orderRepository.countOrdersByDateRange(start, end);
        long cancelledCount = orderRepository.countOrdersByDateRangeAndStatuses(start, end, CANCELLED_STATUSES);

        // Items sold
        Long itemsSold = orderItemRepository.sumTotalItemsSold(start, end, SUCCESS_STATUSES);
        if (itemsSold == null) itemsSold = 0L;

        // AOV
        BigDecimal aov = paidOrdersCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(paidOrdersCount), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // New Customers (tạo tài khoản trong kỳ)
        long newCustomers = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isBefore(start) && !u.getCreatedAt().isAfter(end))
                .count();

        // Conversion Rate (Unique views vs Payment Success sessions hoặc paid orders)
        long uniqueViews = analyticsEventRepository.countDistinctSessionsByEventType(EventType.PRODUCT_VIEW, start, end);
        long paymentSuccessSessions = analyticsEventRepository.countDistinctSessionsByEventType(EventType.PAYMENT_SUCCESS, start, end);
        double conversionRate = uniqueViews > 0
                ? (double) paymentSuccessSessions / uniqueViews * 100.0
                : (totalOrdersCount > 0 ? (double) paidOrdersCount / totalOrdersCount * 100.0 : 0.0);

        // Cancellation Rate
        double cancellationRate = totalOrdersCount > 0
                ? (double) cancelledCount / totalOrdersCount * 100.0
                : 0.0;

        // Low stock count (threshold = 5)
        long lowStockCount = productRepository.countLowStockProducts(5);

        // Gross Profit & Margin
        BigDecimal totalCogs = orderService.getCogsInRange(from, to);
        BigDecimal totalDiscount = orderService.getTotalDiscountInRange(from, to);
        BigDecimal grossProfit = totalRevenue.subtract(totalCogs).subtract(totalDiscount);
        BigDecimal grossProfitMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return new DashboardOverviewDto(
                totalRevenue, paidOrdersCount, itemsSold, aov,
                newCustomers, Math.round(conversionRate * 100.0) / 100.0,
                Math.round(cancellationRate * 100.0) / 100.0, lowStockCount,
                grossProfit, grossProfitMargin
        );
    }

    // ── 2. Revenue Trend ─────────────────────────────────────────────────────

    public List<RevenueTrendDto> getRevenueTrend(LocalDate from, LocalDate to, String groupBy) {
        List<RevenueTrendDto> list = new ArrayList<>();
        LocalDate curr = from;

        while (!curr.isAfter(to)) {
            LocalDateTime dayStart = curr.atStartOfDay();
            LocalDateTime dayEnd = curr.atTime(LocalTime.MAX);

            BigDecimal rev = orderRepository.sumRevenueByDateRange(dayStart, dayEnd, SUCCESS_STATUSES).orElse(BigDecimal.ZERO);
            long count = orderRepository.countOrdersByDateRangeAndStatuses(dayStart, dayEnd, SUCCESS_STATUSES);
            BigDecimal aov = count > 0 ? rev.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            list.add(new RevenueTrendDto(curr.toString(), rev, count, aov));
            curr = curr.plusDays(1);
        }

        return list;
    }

    // ── 3. Sales By Category ─────────────────────────────────────────────────

    public List<CategorySalesDto> getSalesByCategory(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        List<Object[]> raw = orderItemRepository.findCategorySalesStats(start, end, SUCCESS_STATUSES);
        List<CategorySalesDto> dtos = new ArrayList<>();

        for (Object[] row : raw) {
            String name = (String) row[0];
            BigDecimal rev = (BigDecimal) row[1];
            Long units = ((Number) row[2]).longValue();
            Long orderCount = ((Number) row[3]).longValue();
            dtos.add(new CategorySalesDto(name, rev, units, orderCount));
        }

        return dtos;
    }

    // ── 4. Sales By Brand ────────────────────────────────────────────────────

    public List<BrandSalesDto> getSalesByBrand(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        List<Object[]> raw = orderItemRepository.findBrandSalesStats(start, end, SUCCESS_STATUSES);
        List<BrandSalesDto> dtos = new ArrayList<>();

        for (Object[] row : raw) {
            String name = (String) row[0];
            BigDecimal rev = (BigDecimal) row[1];
            Long units = ((Number) row[2]).longValue();
            Long orderCount = ((Number) row[3]).longValue();
            dtos.add(new BrandSalesDto(name, rev, units, orderCount));
        }

        return dtos;
    }

    // ── 5. Conversion Funnel ─────────────────────────────────────────────────

    public ConversionFunnelDto getConversionFunnel(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        long views = analyticsEventRepository.countDistinctSessionsByEventType(EventType.PRODUCT_VIEW, start, end);
        long carts = analyticsEventRepository.countDistinctSessionsByEventType(EventType.ADD_TO_CART, start, end);
        long checkouts = analyticsEventRepository.countDistinctSessionsByEventType(EventType.BEGIN_CHECKOUT, start, end);
        long orders = analyticsEventRepository.countDistinctSessionsByEventType(EventType.ORDER_CREATED, start, end);
        long payments = analyticsEventRepository.countDistinctSessionsByEventType(EventType.PAYMENT_SUCCESS, start, end);

        // Fallback sang Order table nếu chưa có nhiều event
        if (orders == 0) {
            orders = orderRepository.countOrdersByDateRange(start, end);
        }
        if (payments == 0) {
            payments = orderRepository.countOrdersByDateRangeAndStatuses(start, end, SUCCESS_STATUSES);
        }

        return new ConversionFunnelDto(views, carts, checkouts, orders, payments);
    }

    // ── 6. Order Status Distribution ─────────────────────────────────────────

    public List<OrderStatusDistDto> getOrderStatusDistribution(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        List<Object[]> raw = orderRepository.countOrdersGroupedByStatus(start, end);
        List<OrderStatusDistDto> dtos = new ArrayList<>();

        for (Object[] row : raw) {
            OrderStatus status = (OrderStatus) row[0];
            Long count = ((Number) row[1]).longValue();
            dtos.add(new OrderStatusDistDto(status.name(), status.getDisplayName(), count));
        }

        return dtos;
    }

    // ── 7. Top Products Performance ──────────────────────────────────────────

    public List<ProductPerformanceDto> getTopProducts(LocalDate from, LocalDate to, String sortBy, int limit) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        var products = productRepository.findAll();
        List<ProductPerformanceDto> list = new ArrayList<>();

        for (var p : products) {
            long uniqueViews = analyticsEventRepository.countUniqueViewsByProduct(p.getId(), start, end);
            long cartAdds = analyticsEventRepository.countCartAddsByProduct(p.getId(), start, end);
            Long unitsSold = orderItemRepository.sumSoldQuantityByProductId(p.getId()).orElse(0L);

            BigDecimal revenue = BigDecimal.valueOf(unitsSold).multiply(p.getFinalPrice());
            double convRate = uniqueViews > 0 ? (double) unitsSold / uniqueViews * 100.0 : 0.0;
            Double avgRating = reviewService.getAverageRating(p.getId());

            list.add(new ProductPerformanceDto(
                    p.getId(),
                    p.getName(),
                    p.getBrand() != null ? p.getBrand().getName() : "N/A",
                    p.getCategory() != null ? p.getCategory().getName() : "N/A",
                    uniqueViews,
                    uniqueViews,
                    cartAdds,
                    unitsSold,
                    unitsSold,
                    revenue,
                    Math.round(convRate * 100.0) / 100.0,
                    p.getStock() != null ? p.getStock() : 0,
                    avgRating
            ));
        }

        // Sorting
        Comparator<ProductPerformanceDto> comp;
        switch (sortBy != null ? sortBy.toLowerCase() : "revenue") {
            case "quantity":
            case "unitssold":
                comp = Comparator.comparing(ProductPerformanceDto::getUnitsSold).reversed();
                break;
            case "views":
                comp = Comparator.comparing(ProductPerformanceDto::getViews).reversed();
                break;
            case "conversion":
                comp = Comparator.comparing(ProductPerformanceDto::getConversionRate).reversed();
                break;
            case "revenue":
            default:
                comp = Comparator.comparing(ProductPerformanceDto::getRevenue).reversed();
                break;
        }

        list.sort(comp);
        if (list.size() > limit) {
            return list.subList(0, limit);
        }
        return list;
    }
}
