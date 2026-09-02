package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.AiChatHistoryRepository;
import com.laptopstore.laptopstore.Repository.AnalyticsEventRepository;
import com.laptopstore.laptopstore.Repository.OrderRepository;
import com.laptopstore.laptopstore.dto.AiAnalyticsDto;
import com.laptopstore.laptopstore.entity.Order;
import com.laptopstore.laptopstore.enums.EventType;
import com.laptopstore.laptopstore.enums.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AiAnalyticsService {

    private static final List<OrderStatus> SUCCESS_STATUSES = List.of(
            OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.PACKING,
            OrderStatus.SHIPPING, OrderStatus.DELIVERED
    );

    private final AnalyticsEventRepository analyticsEventRepository;
    private final AiChatHistoryRepository aiChatHistoryRepository;
    private final OrderRepository orderRepository;

    public AiAnalyticsService(AnalyticsEventRepository analyticsEventRepository,
                              AiChatHistoryRepository aiChatHistoryRepository,
                              OrderRepository orderRepository) {
        this.analyticsEventRepository = analyticsEventRepository;
        this.aiChatHistoryRepository = aiChatHistoryRepository;
        this.orderRepository = orderRepository;
    }

    public AiAnalyticsDto getAiAnalytics(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        long totalChats = analyticsEventRepository.countEvents(EventType.AI_CHAT, start, end);
        long uniqueAiSessions = analyticsEventRepository.countUniqueAiSessions(start, end);

        // Fallback sang AiChatHistory nếu chưa có đủ analytics events
        if (totalChats == 0) {
            totalChats = aiChatHistoryRepository.countByRole("user");
        }

        // Lấy danh sách session IDs đã chat với AI
        List<String> aiSessionIds = analyticsEventRepository.findSessionIdsWithAiChat(start, end);

        // Đếm số đơn và doanh thu từ các session có AI chat
        long aiAssistedOrders = 0;
        BigDecimal aiAssistedRevenue = BigDecimal.ZERO;

        List<Order> ordersInPeriod = orderRepository.findOrdersByDateRange(start, end);
        for (Order o : ordersInPeriod) {
            if (SUCCESS_STATUSES.contains(o.getOrderStatus())) {
                // Kiểm tra xem order có event AI_CHAT trong cùng order hoặc session không
                var events = analyticsEventRepository.findByOrderId(o.getId());
                boolean hasAiChat = events.stream().anyMatch(e -> e.getEventType() == EventType.AI_CHAT);
                if (hasAiChat) {
                    aiAssistedOrders++;
                    aiAssistedRevenue = aiAssistedRevenue.add(o.getTotalAmount());
                }
            }
        }

        double aiConversionRate = uniqueAiSessions > 0
                ? (double) aiAssistedOrders / uniqueAiSessions * 100.0
                : 0.0;

        long recommendedProductsCount = analyticsEventRepository.countEvents(EventType.AI_PRODUCT_RECOMMENDED, start, end);
        long recommendedProductClicks = analyticsEventRepository.countEvents(EventType.AI_PRODUCT_CLICK, start, end);
        double recommendationCtr = recommendedProductsCount > 0
                ? (double) recommendedProductClicks / recommendedProductsCount * 100.0
                : 0.0;

        Double avgConfidence = aiChatHistoryRepository.findAvgConfidenceScore();
        if (avgConfidence == null) avgConfidence = 0.85; // Default mockup fallback

        long lowConfidenceCount = aiChatHistoryRepository.countLowConfidenceQueries(0.7);

        return new AiAnalyticsDto(
                totalChats,
                uniqueAiSessions,
                aiAssistedOrders,
                aiAssistedRevenue,
                Math.round(aiConversionRate * 100.0) / 100.0,
                recommendedProductsCount,
                recommendedProductClicks,
                Math.round(recommendationCtr * 100.0) / 100.0,
                Math.round(avgConfidence * 100.0) / 100.0,
                lowConfidenceCount
        );
    }
}
