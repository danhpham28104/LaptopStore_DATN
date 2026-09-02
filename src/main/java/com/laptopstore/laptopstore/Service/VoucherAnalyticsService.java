package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.OrderRepository;
import com.laptopstore.laptopstore.Repository.VoucherRepository;
import com.laptopstore.laptopstore.Repository.VoucherUsageRepository;
import com.laptopstore.laptopstore.dto.VoucherAnalyticsDto;
import com.laptopstore.laptopstore.entity.Voucher;
import com.laptopstore.laptopstore.enums.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class VoucherAnalyticsService {

    private static final List<OrderStatus> SUCCESS_STATUSES = List.of(
            OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.PACKING,
            OrderStatus.SHIPPING, OrderStatus.DELIVERED
    );

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final OrderRepository orderRepository;

    public VoucherAnalyticsService(VoucherRepository voucherRepository,
                                   VoucherUsageRepository voucherUsageRepository,
                                   OrderRepository orderRepository) {
        this.voucherRepository = voucherRepository;
        this.voucherUsageRepository = voucherUsageRepository;
        this.orderRepository = orderRepository;
    }

    public List<VoucherAnalyticsDto> getVoucherAnalytics(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        List<Voucher> vouchers = voucherRepository.findAll();
        List<VoucherAnalyticsDto> list = new ArrayList<>();

        for (Voucher v : vouchers) {
            long usageCount = voucherUsageRepository.countByVoucher_Id(v.getId());
            BigDecimal totalDiscount = voucherUsageRepository.sumDiscountByVoucherAndDateRange(v.getId(), start, end);
            if (totalDiscount == null) totalDiscount = BigDecimal.ZERO;

            // Tính tổng doanh thu tạo ra từ voucher này trong kỳ
            List<Object[]> stats = voucherUsageRepository.findVoucherUsageStats(start, end);
            BigDecimal revenue = BigDecimal.ZERO;
            long ordersCount = 0;

            var ordersWithVoucher = orderRepository.findAll().stream()
                    .filter(o -> o.getVoucher() != null && o.getVoucher().getId().equals(v.getId())
                            && o.getCreatedAt() != null && !o.getCreatedAt().isBefore(start) && !o.getCreatedAt().isAfter(end)
                            && SUCCESS_STATUSES.contains(o.getOrderStatus()))
                    .toList();

            for (var o : ordersWithVoucher) {
                revenue = revenue.add(o.getTotalAmount());
                ordersCount++;
            }

            BigDecimal avgOrderValue = ordersCount > 0
                    ? revenue.divide(BigDecimal.valueOf(ordersCount), 0, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            list.add(new VoucherAnalyticsDto(
                    v.getCode(),
                    usageCount,
                    ordersCount,
                    revenue,
                    totalDiscount,
                    avgOrderValue,
                    0.0
            ));
        }

        return list;
    }
}
