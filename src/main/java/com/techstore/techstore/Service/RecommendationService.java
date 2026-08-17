package com.techstore.techstore.Service;

import com.techstore.techstore.Repository.ProductRepository;
import com.techstore.techstore.entity.Product;
import com.techstore.techstore.entity.ProductViewHistory;
import com.techstore.techstore.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductViewHistoryService productViewHistoryService;

    @Transactional(readOnly = true)
    public List<Product> getRecommendationsForUser(User user, int limit) {
        if (user == null) {
            return Collections.emptyList();
        }

        // 1. Lấy top 5 sản phẩm xem nhiều nhất của người dùng
        List<ProductViewHistory> viewHistory = productViewHistoryService.getTopViewedProducts(user.getId(), 5);
        if (viewHistory.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Thu thập các thông tin đặc trưng của sản phẩm đã xem
        List<Long> excludeIds = new ArrayList<>();
        List<Long> brandIds = new ArrayList<>();
        List<BigDecimal> prices = new ArrayList<>();
        Map<String, Integer> cpuKeywordsCount = new HashMap<>();

        for (ProductViewHistory history : viewHistory) {
            Product p = history.getProduct();
            if (p == null || p.isDeleted()) {
                continue;
            }
            excludeIds.add(p.getId());
            if (p.getBrand() != null) {
                brandIds.add(p.getBrand().getId());
            }
            if (p.getPrice() != null) {
                prices.add(p.getPrice());
            }
            String cpuKeyword = extractCpuKeyword(p.getCpu());
            if (cpuKeyword != null) {
                cpuKeywordsCount.put(cpuKeyword, cpuKeywordsCount.getOrDefault(cpuKeyword, 0) + 1);
            }
        }

        // Nếu danh sách exclude trống (đề phòng), thêm giá trị dummy để tránh lỗi SQL NOT IN
        if (excludeIds.isEmpty()) {
            excludeIds.add(-1L);
        }
        if (brandIds.isEmpty()) {
            brandIds.add(-1L);
        }

        // Tính toán khoảng giá dựa trên giá trung bình của các sản phẩm đã xem (±35%)
        BigDecimal minPrice = BigDecimal.ZERO;
        BigDecimal maxPrice = BigDecimal.valueOf(999999999);
        if (!prices.isEmpty()) {
            BigDecimal sum = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avgPrice = sum.divide(BigDecimal.valueOf(prices.size()), java.math.RoundingMode.HALF_UP);
            minPrice = avgPrice.multiply(BigDecimal.valueOf(0.65));
            maxPrice = avgPrice.multiply(BigDecimal.valueOf(1.35));
        }

        // Tìm từ khóa CPU phổ biến nhất
        String targetCpuKeyword = cpuKeywordsCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // 3. Truy vấn các sản phẩm tương tự từ Database
        List<Product> candidates = productRepository.findSimilarProducts(
                excludeIds, brandIds, minPrice, maxPrice, targetCpuKeyword);

        // 4. Tính toán độ tương quan (similarity score) cho từng ứng viên
        // Tạo một map để lưu điểm số
        Map<Product, Integer> scoredCandidates = new HashMap<>();
        for (Product candidate : candidates) {
            int score = calculateSimilarityScore(candidate, brandIds, prices, targetCpuKeyword);
            scoredCandidates.put(candidate, score);
        }

        // 5. Sắp xếp theo điểm số giảm dần và giới hạn số lượng trả về
        return scoredCandidates.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // Điểm cao xếp trước
                .map(Map.Entry::getKey)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private int calculateSimilarityScore(Product candidate, List<Long> brandIds, List<BigDecimal> watchedPrices, String targetCpuKeyword) {
        int score = 0;

        // 1. Cùng brand: +3 điểm
        if (candidate.getBrand() != null && brandIds.contains(candidate.getBrand().getId())) {
            score += 3;
        }

        // 2. Tầm giá tương đương: kiểm tra xem có gần với giá của sản phẩm nào đã xem không (trong khoảng ±20%)
        if (candidate.getPrice() != null && !watchedPrices.isEmpty()) {
            boolean inPriceRange = false;
            for (BigDecimal watchedPrice : watchedPrices) {
                BigDecimal diff = candidate.getPrice().subtract(watchedPrice).abs();
                BigDecimal threshold = watchedPrice.multiply(BigDecimal.valueOf(0.20));
                if (diff.compareTo(threshold) <= 0) {
                    inPriceRange = true;
                    break;
                }
            }
            if (inPriceRange) {
                score += 2;
            }
        }

        // 3. Cùng dòng CPU: +2 điểm
        String candidateCpuKeyword = extractCpuKeyword(candidate.getCpu());
        if (targetCpuKeyword != null && targetCpuKeyword.equalsIgnoreCase(candidateCpuKeyword)) {
            score += 2;
        }

        // 4. Sản phẩm nổi bật (HOT badge): +1 điểm thưởng
        if (candidate.getBadge() != null && candidate.getBadge().equalsIgnoreCase("HOT")) {
            score += 1;
        }

        return score;
    }

    private String extractCpuKeyword(String cpu) {
        if (cpu == null) return null;
        String lower = cpu.toLowerCase();
        if (lower.contains("i3")) return "i3";
        if (lower.contains("i5")) return "i5";
        if (lower.contains("i7")) return "i7";
        if (lower.contains("i9")) return "i9";
        if (lower.contains("ryzen 3") || lower.contains("r3")) return "ryzen 3";
        if (lower.contains("ryzen 5") || lower.contains("r5")) return "ryzen 5";
        if (lower.contains("ryzen 7") || lower.contains("r7")) return "ryzen 7";
        if (lower.contains("ryzen 9") || lower.contains("r9")) return "ryzen 9";
        if (lower.contains("apple m1") || lower.contains("m1")) return "m1";
        if (lower.contains("apple m2") || lower.contains("m2")) return "m2";
        if (lower.contains("apple m3") || lower.contains("m3")) return "m3";
        if (lower.contains("apple silicon")) return "apple silicon";
        return null;
    }
}
