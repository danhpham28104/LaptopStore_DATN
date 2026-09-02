package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.OrderItemRepository;
import com.laptopstore.laptopstore.Repository.ProductRepository;
import com.laptopstore.laptopstore.dto.DeadStockDto;
import com.laptopstore.laptopstore.dto.InventoryAnalyticsDto;
import com.laptopstore.laptopstore.dto.LowStockIntelligenceDto;
import com.laptopstore.laptopstore.entity.Product;
import com.laptopstore.laptopstore.enums.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryAnalyticsService {

    private static final List<OrderStatus> VALID_STATUSES = List.of(
            OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.PACKING,
            OrderStatus.SHIPPING, OrderStatus.DELIVERED
    );

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public InventoryAnalyticsService(ProductRepository productRepository, OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /** Danh sách phân tích kho của tất cả sản phẩm. */
    public List<InventoryAnalyticsDto> getAllInventoryMetrics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime days7Ago = now.minusDays(7);
        LocalDateTime days30Ago = now.minusDays(30);
        LocalDateTime days90Ago = now.minusDays(90);

        List<Product> products = productRepository.findAll();
        List<InventoryAnalyticsDto> list = new ArrayList<>();

        for (Product p : products) {
            Long sold7d = orderItemRepository.sumTotalItemsSold(days7Ago, now, VALID_STATUSES);
            Long sold30d = orderItemRepository.sumTotalItemsSold(days30Ago, now, VALID_STATUSES);
            Long sold90d = orderItemRepository.sumTotalItemsSold(days90Ago, now, VALID_STATUSES);

            if (sold7d == null) sold7d = 0L;
            if (sold30d == null) sold30d = 0L;
            if (sold90d == null) sold90d = 0L;

            int currentStock = p.getStock() != null ? p.getStock() : 0;
            int reservedStock = p.getReservedStock() != null ? p.getReservedStock() : 0;

            double avgDailySales = (double) sold30d / 30.0;
            double daysOfInventory = avgDailySales > 0 ? (double) currentStock / avgDailySales : 999.0;
            double sellThroughRate = (sold30d + currentStock) > 0 ? (double) sold30d / (sold30d + currentStock) * 100.0 : 0.0;

            String statusLabel = "Bình thường";
            if (currentStock <= 5 || daysOfInventory < 7) {
                statusLabel = "⚠ Nguy cơ hết hàng";
            } else if (sold90d <= 1 && currentStock >= 10) {
                statusLabel = "❄ Hàng tồn đọng (Dead stock)";
            }

            list.add(new InventoryAnalyticsDto(
                    p.getId(),
                    p.getName(),
                    p.getBrand() != null ? p.getBrand().getName() : "N/A",
                    currentStock,
                    reservedStock,
                    sold7d,
                    sold30d,
                    sold90d,
                    Math.round(avgDailySales * 100.0) / 100.0,
                    Math.round(daysOfInventory * 10.0) / 10.0,
                    Math.round(sellThroughRate * 100.0) / 100.0,
                    statusLabel
            ));
        }

        list.sort(Comparator.comparing(InventoryAnalyticsDto::getDaysOfInventory));
        return list;
    }

    /** Danh sách cảnh báo rủi ro hết hàng (Stockout Risk). */
    public List<LowStockIntelligenceDto> getLowStockIntelligence() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime days30Ago = now.minusDays(30);

        List<Product> products = productRepository.findAll();
        List<LowStockIntelligenceDto> list = new ArrayList<>();

        for (Product p : products) {
            Long sold30d = orderItemRepository.sumTotalItemsSold(days30Ago, now, VALID_STATUSES);
            if (sold30d == null) sold30d = 0L;

            int currentStock = p.getStock() != null ? p.getStock() : 0;
            double avgDailySales = (double) sold30d / 30.0;
            double estimatedDays = avgDailySales > 0 ? (double) currentStock / avgDailySales : 999.0;

            if (currentStock <= 5 || estimatedDays < 7) {
                int recommendedOrderQty = Math.max(10, (int) Math.ceil(avgDailySales * 30) - currentStock);
                String rec = String.format("Nên nhập thêm khoảng %d máy để đảm bảo kinh doanh trong 30 ngày tới.", recommendedOrderQty);

                list.add(new LowStockIntelligenceDto(
                        p.getId(), p.getName(), currentStock,
                        Math.round(avgDailySales * 100.0) / 100.0,
                        Math.round(estimatedDays * 10.0) / 10.0,
                        rec
                ));
            }
        }

        list.sort(Comparator.comparing(LowStockIntelligenceDto::getEstimatedDaysRemaining));
        return list;
    }

    /** Danh sách Hàng Tồn Đọng (Dead Stock: bán <= 1 máy trong 90 ngày nhưng tồn kho >= 10). */
    public List<DeadStockDto> getDeadStock() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime days90Ago = now.minusDays(90);

        List<Product> products = productRepository.findAll();
        List<DeadStockDto> list = new ArrayList<>();

        for (Product p : products) {
            Long sold90d = orderItemRepository.sumTotalItemsSold(days90Ago, now, VALID_STATUSES);
            if (sold90d == null) sold90d = 0L;

            int currentStock = p.getStock() != null ? p.getStock() : 0;

            if (sold90d <= 1 && currentStock >= 10) {
                BigDecimal tiedCapital = p.getFinalPrice().multiply(BigDecimal.valueOf(currentStock));
                list.add(new DeadStockDto(p.getId(), p.getName(), currentStock, sold90d, tiedCapital));
            }
        }

        list.sort(Comparator.comparing(DeadStockDto::getTiedUpCapital).reversed());
        return list;
    }
}
