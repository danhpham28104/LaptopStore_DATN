package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.AnalyticsEventRepository;
import com.laptopstore.laptopstore.Repository.ProductRepository;
import com.laptopstore.laptopstore.entity.AnalyticsEvent;
import com.laptopstore.laptopstore.entity.Product;
import com.laptopstore.laptopstore.enums.EventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private final ProductRepository productRepository;
    private final AnalyticsEventRepository analyticsEventRepository;

    public RecommendationService(ProductRepository productRepository, AnalyticsEventRepository analyticsEventRepository) {
        this.productRepository = productRepository;
        this.analyticsEventRepository = analyticsEventRepository;
    }

    /** Compatibility method cho HomeController & ProductController */
    public List<Product> getRecommendationsForUser(com.laptopstore.laptopstore.entity.User user, int limit) {
        Long userId = user != null ? user.getId() : null;
        return getPersonalizedRecommendations(userId, null, limit);
    }

    /** Gợi ý sản phẩm cá nhân hóa dựa trên lịch sử xem/thêm giỏ hàng của user/session. */
    public List<Product> getPersonalizedRecommendations(Long userId, String sessionId, int limit) {
        Set<Long> viewedProductIds = new HashSet<>();

        if (userId != null) {
            List<AnalyticsEvent> userEvents = analyticsEventRepository.findByUserId(userId);
            for (AnalyticsEvent e : userEvents) {
                if (e.getProductId() != null) viewedProductIds.add(e.getProductId());
            }
        }

        if (sessionId != null) {
            List<AnalyticsEvent> sessionEvents = analyticsEventRepository.findBySessionId(sessionId);
            for (AnalyticsEvent e : sessionEvents) {
                if (e.getProductId() != null) viewedProductIds.add(e.getProductId());
            }
        }

        if (viewedProductIds.isEmpty()) {
            return getFallbackTopProducts(limit);
        }

        // Lấy thông tin các sản phẩm đã xem để lọc theo Brand/Category tương tự
        List<Product> viewedProducts = productRepository.findAllById(viewedProductIds);
        Set<Long> categoryIds = viewedProducts.stream().filter(p -> p.getCategory() != null).map(p -> p.getCategory().getId()).collect(Collectors.toSet());
        Set<Long> brandIds = viewedProducts.stream().filter(p -> p.getBrand() != null).map(p -> p.getBrand().getId()).collect(Collectors.toSet());

        List<Product> candidates = productRepository.findAll().stream()
                .filter(p -> !viewedProductIds.contains(p.getId())) // Loại bỏ SP đã xem
                .filter(p -> (p.getCategory() != null && categoryIds.contains(p.getCategory().getId()))
                          || (p.getBrand() != null && brandIds.contains(p.getBrand().getId())))
                .sorted(Comparator.comparing(Product::getSoldQuantity, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .collect(Collectors.toList());

        if (candidates.size() < limit) {
            List<Product> fallbacks = getFallbackTopProducts(limit - candidates.size());
            for (Product f : fallbacks) {
                if (!viewedProductIds.contains(f.getId()) && candidates.stream().noneMatch(c -> c.getId().equals(f.getId()))) {
                    candidates.add(f);
                }
            }
        }

        return candidates;
    }

    /** Gợi ý sản phẩm tương tự với productId đang xem. */
    public List<Product> getSimilarProducts(Long productId, int limit) {
        Optional<Product> opt = productRepository.findById(productId);
        if (opt.isEmpty()) return getFallbackTopProducts(limit);

        Product current = opt.get();
        Long categoryId = current.getCategory() != null ? current.getCategory().getId() : null;
        Long brandId = current.getBrand() != null ? current.getBrand().getId() : null;

        return productRepository.findAll().stream()
                .filter(p -> !p.getId().equals(productId))
                .filter(p -> (categoryId != null && p.getCategory() != null && categoryId.equals(p.getCategory().getId()))
                          || (brandId != null && p.getBrand() != null && brandId.equals(p.getBrand().getId())))
                .sorted(Comparator.comparing(Product::getSoldQuantity, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<Product> getFallbackTopProducts(int limit) {
        return productRepository.findAll().stream()
                .sorted(Comparator.comparing(Product::getSoldQuantity, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
