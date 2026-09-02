package com.laptopstore.laptopstore.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptopstore.laptopstore.Repository.AiChatHistoryRepository;
import com.laptopstore.laptopstore.Repository.OrderItemRepository;
import com.laptopstore.laptopstore.Repository.ProductRepository;
import com.laptopstore.laptopstore.Repository.ProductViewHistoryRepository;
import com.laptopstore.laptopstore.dto.*;
import com.laptopstore.laptopstore.entity.AiChatHistory;
import com.laptopstore.laptopstore.entity.Product;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AdminAnalyticsController.class);

    private final ProductViewHistoryRepository productViewHistoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final AiChatHistoryRepository aiChatHistoryRepository;
    private final com.laptopstore.laptopstore.Service.AnalyticsService analyticsService;
    private final com.laptopstore.laptopstore.Service.AiAnalyticsService aiAnalyticsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Trang Analytics Overview tổng quan (V2 Dashboard Analytics)
     */
    @GetMapping("/overview")
    public String overview(Model model) {
        model.addAttribute("active", "analytics_overview");
        model.addAttribute("pageTitle", "Báo Cáo Phân Tích & BI - LaptopStore Admin");
        return "admin/analytics/overview";
    }

    /**
     * Trang Phân Tích Tồn Kho & Chẩn Đoán Đọng Vốn (Inventory Analytics)
     */
    @GetMapping("/inventory")
    public String inventoryAnalytics(Model model) {
        model.addAttribute("active", "analytics_inventory");
        model.addAttribute("pageTitle", "Phân Tích Tồn Kho & Chẩn Đoán Kho - LaptopStore Admin");
        return "admin/analytics/inventory";
    }

    /**
     * Trang Phân Tích Hiệu Quả Khuyến Mãi (Voucher Analytics)
     */
    @GetMapping("/vouchers")
    public String voucherAnalytics(Model model) {
        model.addAttribute("active", "analytics_vouchers");
        model.addAttribute("pageTitle", "Phân Tích Hiệu Quả Voucher - LaptopStore Admin");
        return "admin/analytics/vouchers";
    }

    /**
     * 1. Trang Phân Tích Lượt Xem & Nhu Cầu Sản Phẩm
     */
    @GetMapping("/products")
    public String productAnalytics(Model model) {
        // --- 🔵 Summary Cards ---
        Long totalViews = productViewHistoryRepository.sumTotalViews().orElse(0L);
        long distinctViewedCount = productViewHistoryRepository.countDistinctViewedProducts();

        List<Object[]> topViewedGrouped = productViewHistoryRepository.findTopViewedProductsGrouped();

        String topViewedProductName = "Chưa có dữ liệu";
        if (!topViewedGrouped.isEmpty() && topViewedGrouped.get(0)[0] != null) {
            Product p = (Product) topViewedGrouped.get(0)[0];
            topViewedProductName = p.getName();
        }

        List<Object[]> brandGrouped = productViewHistoryRepository.findBrandViewAnalyticsGrouped();
        String topBrandName = "Chưa có dữ liệu";
        if (!brandGrouped.isEmpty() && brandGrouped.get(0)[0] != null) {
            topBrandName = (String) brandGrouped.get(0)[0];
        }

        // --- 🔵 Bảng Top Xem Nhiều ---
        List<ProductViewInsightDto> topViewedProducts = new ArrayList<>();
        for (Object[] row : topViewedGrouped) {
            Product p = (Product) row[0];
            Long views = (Long) row[1];
            LocalDateTime lastViewed = (LocalDateTime) row[2];

            if (p != null) {
                String firstImg = (p.getImages() != null && !p.getImages().isBlank())
                        ? p.getImages().split(",")[0] : null;

                topViewedProducts.add(new ProductViewInsightDto(
                        p.getId(),
                        p.getName(),
                        p.getModel(),
                        p.getBrand() != null ? p.getBrand().getName() : "Khác",
                        firstImg,
                        p.getPrice(),
                        p.getStock(),
                        views,
                        lastViewed
                ));
            }
        }

        // --- 🔵 Bảng Xem Nhiều Mua Ít (High View - Low Conversion) ---
        List<ProductConversionInsightDto> conversionList = new ArrayList<>();
        for (Object[] row : topViewedGrouped) {
            Product p = (Product) row[0];
            Long views = (Long) row[1];

            if (p != null && views != null && views >= 10) {
                Long totalSold = orderItemRepository.sumSoldQuantityByProductId(p.getId()).orElse(0L);
                double rate = (totalSold * 100.0) / views;
                double roundedRate = Math.round(rate * 10.0) / 10.0;

                String recommendation;
                if (p.getStock() <= 3) {
                    recommendation = "Tồn kho thấp - Cần nhập thêm hàng";
                } else if (roundedRate < 15.0) {
                    recommendation = "Lượt xem cao, mua ít - Cân nhắc giảm giá / thêm quà tặng / sửa mô tả";
                } else {
                    recommendation = "Chuyển đổi tốt";
                }

                conversionList.add(new ProductConversionInsightDto(
                        p.getId(),
                        p.getName(),
                        p.getBrand() != null ? p.getBrand().getName() : "Khác",
                        p.getPrice(),
                        p.getStock(),
                        views,
                        totalSold,
                        roundedRate,
                        recommendation
                ));
            }
        }
        // Sắp xếp Conversion Rate tăng dần
        conversionList.sort(Comparator.comparingDouble(ProductConversionInsightDto::getConversionRate));

        // --- 🔵 Brand Interest List ---
        List<BrandInterestDto> brandInterests = new ArrayList<>();
        for (Object[] row : brandGrouped) {
            String bName = (String) row[0];
            Long bViews = (Long) row[1];
            brandInterests.add(new BrandInterestDto(bName != null ? bName : "Khác", bViews));
        }

        // --- 🔵 Phân Khúc Giá Được Quan Tâm ---
        Map<String, Long> priceRangeMap = new LinkedHashMap<>();
        priceRangeMap.put("Dưới 10 triệu", 0L);
        priceRangeMap.put("10 - 15 triệu", 0L);
        priceRangeMap.put("15 - 20 triệu", 0L);
        priceRangeMap.put("20 - 30 triệu", 0L);
        priceRangeMap.put("Trên 30 triệu", 0L);

        for (Object[] row : topViewedGrouped) {
            Product p = (Product) row[0];
            Long views = (Long) row[1];
            if (p != null && p.getPrice() != null && views != null) {
                double priceVal = p.getPrice().doubleValue();
                if (priceVal < 10_000_000) {
                    priceRangeMap.put("Dưới 10 triệu", priceRangeMap.get("Dưới 10 triệu") + views);
                } else if (priceVal < 15_000_000) {
                    priceRangeMap.put("10 - 15 triệu", priceRangeMap.get("10 - 15 triệu") + views);
                } else if (priceVal < 20_000_000) {
                    priceRangeMap.put("15 - 20 triệu", priceRangeMap.get("15 - 20 triệu") + views);
                } else if (priceVal < 30_000_000) {
                    priceRangeMap.put("20 - 30 triệu", priceRangeMap.get("20 - 30 triệu") + views);
                } else {
                    priceRangeMap.put("Trên 30 triệu", priceRangeMap.get("Trên 30 triệu") + views);
                }
            }
        }

        List<PriceRangeInterestDto> priceRanges = new ArrayList<>();
        priceRangeMap.forEach((k, v) -> priceRanges.add(new PriceRangeInterestDto(k, v)));

        // Truyền Attributes
        model.addAttribute("totalViews", totalViews);
        model.addAttribute("distinctViewedCount", distinctViewedCount);
        model.addAttribute("topViewedProductName", topViewedProductName);
        model.addAttribute("topBrandName", topBrandName);

        model.addAttribute("topViewedProducts", topViewedProducts);
        model.addAttribute("conversionList", conversionList);
        model.addAttribute("brandInterests", brandInterests);
        model.addAttribute("priceRanges", priceRanges);

        model.addAttribute("active", "analytics_products");
        model.addAttribute("pageTitle", "Phân Tích Xem & Nhu Cầu Sản Phẩm - LaptopStore Admin");

        return "admin/analytics_products";
    }

    /**
     * 2. Trang Phân Tích Ý Đồ Khách Hàng Từ AI Chatbot (RAG Insights)
     */
    @GetMapping("/ai-chat")
    public String aiChatAnalytics(Model model) {
        long totalUserQuestions = aiChatHistoryRepository.countByRole("user");
        long totalAssistantResponses = aiChatHistoryRepository.countByRole("assistant");

        var aiDto = aiAnalyticsService.getAiAnalytics(LocalDate.now().minusDays(30), LocalDate.now());
        model.addAttribute("aiAssistedOrdersCount", aiDto.getAiAssistedOrdersCount());
        model.addAttribute("aiAssistedRevenue", aiDto.getAiAssistedRevenue());

        List<AiChatHistory> userMessages = aiChatHistoryRepository.findByRoleOrderByCreatedAtDesc("user");
        List<AiChatHistory> assistantMessages = aiChatHistoryRepository.findByRoleOrderByCreatedAtDesc("assistant");

        // Map conversationKey -> User Question gần nhất
        Map<String, String> latestUserQuestionMap = new HashMap<>();
        for (AiChatHistory uMsg : userMessages) {
            if (uMsg.getConversationKey() != null && !latestUserQuestionMap.containsKey(uMsg.getConversationKey())) {
                latestUserQuestionMap.put(uMsg.getConversationKey(), uMsg.getMessage());
            }
        }

        // --- 🔵 1. Keyword Insights ---
        List<String> targetKeywords = List.of(
                "gaming", "đồ họa", "văn phòng", "sinh viên", "lập trình",
                "mỏng nhẹ", "pin trâu", "dưới 10 triệu", "dưới 15 triệu",
                "dưới 20 triệu", "dưới 25 triệu", "dưới 30 triệu",
                "ram 16gb", "rtx", "i5", "i7", "ryzen", "macbook"
        );

        Map<String, Long> keywordCounts = new HashMap<>();
        for (AiChatHistory uMsg : userMessages) {
            if (uMsg.getMessage() != null) {
                String textLower = uMsg.getMessage().toLowerCase();
                for (String kw : targetKeywords) {
                    if (textLower.contains(kw)) {
                        keywordCounts.put(kw, keywordCounts.getOrDefault(kw, 0L) + 1);
                    }
                }
            }
        }

        List<AiKeywordInsightDto> keywordInsights = new ArrayList<>();
        keywordCounts.forEach((kw, count) -> keywordInsights.add(new AiKeywordInsightDto(kw.toUpperCase(), count)));
        keywordInsights.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));

        // --- 🔵 2. Parse AI Responses JSON ---
        Map<Long, Long> recommendedFreqMap = new HashMap<>();
        List<AiLowConfidenceQueryDto> lowConfidenceQueries = new ArrayList<>();
        List<AiLowConfidenceQueryDto> unmatchedQueries = new ArrayList<>();
        long lowConfidenceCount = 0;
        long noRecommendationCount = 0;

        for (AiChatHistory aMsg : assistantMessages) {
            String jsonStr = aMsg.getResponseJson();
            if (jsonStr == null || jsonStr.isBlank()) {
                continue;
            }

            try {
                JsonNode root = objectMapper.readTree(jsonStr);

                // --- Extract Recommended Products ---
                JsonNode recNode = root.get("recommendedProducts");
                int recCount = 0;
                if (recNode != null && recNode.isArray()) {
                    recCount = recNode.size();
                    for (JsonNode pNode : recNode) {
                        if (pNode.has("id")) {
                            Long pId = pNode.get("id").asLong();
                            recommendedFreqMap.put(pId, recommendedFreqMap.getOrDefault(pId, 0L) + 1);
                        }
                    }
                }

                // --- Extract Confidence ---
                double confidence = 1.0;
                if (root.has("confidenceScore")) {
                    confidence = root.get("confidenceScore").asDouble();
                } else if (root.has("confidence")) {
                    confidence = root.get("confidence").asDouble();
                }

                // --- Extract Missing Info ---
                String missingInfo = "";
                if (root.has("missingInformation")) {
                    missingInfo = root.get("missingInformation").asText();
                } else if (root.has("reason")) {
                    missingInfo = root.get("reason").asText();
                }

                String userPrompt = latestUserQuestionMap.getOrDefault(aMsg.getConversationKey(), "Không tìm thấy câu hỏi");

                // Check Low Confidence (< 0.6)
                if (confidence < 0.6) {
                    lowConfidenceCount++;
                    lowConfidenceQueries.add(new AiLowConfidenceQueryDto(
                            aMsg.getId(),
                            aMsg.getConversationKey(),
                            userPrompt,
                            aMsg.getMessage(),
                            confidence,
                            recCount,
                            missingInfo,
                            aMsg.getCreatedAt(),
                            "Độ tin cậy AI thấp - Cần tinh chỉnh Prompt RAG"
                    ));
                }

                // Check No Recommendation (recCount == 0)
                if (recCount == 0) {
                    noRecommendationCount++;
                    unmatchedQueries.add(new AiLowConfidenceQueryDto(
                            aMsg.getId(),
                            aMsg.getConversationKey(),
                            userPrompt,
                            aMsg.getMessage(),
                            confidence,
                            0,
                            missingInfo,
                            aMsg.getCreatedAt(),
                            "Chưa gợi ý được máy - Cần bổ sung sản phẩm đúng nhu cầu vào DB"
                    ));
                }

            } catch (Exception e) {
                // Tránh crash trang nếu 1 bản ghi JSON lỗi format
                log.warn("[AI Analytics] Parse responseJson id={} thất bại: {}", aMsg.getId(), e.getMessage());
            }
        }

        // Top 5 sản phẩm AI hay gợi ý nhất
        List<AiRecommendedProductInsightDto> recommendedProductInsights = new ArrayList<>();
        List<Map.Entry<Long, Long>> topRecList = new ArrayList<>(recommendedFreqMap.entrySet());
        topRecList.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        String topRecommendedProductName = "Chưa có dữ liệu";
        for (int i = 0; i < Math.min(5, topRecList.size()); i++) {
            Map.Entry<Long, Long> entry = topRecList.get(i);
            Optional<Product> pOpt = productRepository.findById(entry.getKey());
            if (pOpt.isPresent()) {
                Product p = pOpt.get();
                if (i == 0) topRecommendedProductName = p.getName();
                recommendedProductInsights.add(new AiRecommendedProductInsightDto(
                        p.getId(),
                        p.getName(),
                        p.getModel(),
                        p.getPrice(),
                        entry.getValue()
                ));
            }
        }

        // Limit lists to top 15 items for UI readability
        if (lowConfidenceQueries.size() > 15) lowConfidenceQueries = lowConfidenceQueries.subList(0, 15);
        if (unmatchedQueries.size() > 15) unmatchedQueries = unmatchedQueries.subList(0, 15);

        // Model Attributes
        model.addAttribute("totalUserQuestions", totalUserQuestions);
        model.addAttribute("totalAssistantResponses", totalAssistantResponses);
        model.addAttribute("lowConfidenceCount", lowConfidenceCount);
        model.addAttribute("noRecommendationCount", noRecommendationCount);
        model.addAttribute("topRecommendedProductName", topRecommendedProductName);

        model.addAttribute("keywordInsights", keywordInsights);
        model.addAttribute("recommendedProductInsights", recommendedProductInsights);
        model.addAttribute("lowConfidenceQueries", lowConfidenceQueries);
        model.addAttribute("unmatchedQueries", unmatchedQueries);

        model.addAttribute("active", "analytics_aichat");
        model.addAttribute("pageTitle", "AI Chatbot Customer Insights - LaptopStore Admin");

        return "admin/analytics_aichat";
    }
}
