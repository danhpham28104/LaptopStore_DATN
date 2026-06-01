package com.techstore.techstore.Service;

import com.techstore.techstore.Repository.ProductRepository;
import com.techstore.techstore.Repository.ProductViewRepository;
import com.techstore.techstore.Repository.UserRepository;
import com.techstore.techstore.entity.Product;
import com.techstore.techstore.entity.ProductView;
import com.techstore.techstore.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RecommendationService {

    @Autowired
    private ProductViewRepository productViewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void recordView(String username, Product product) {
        if (username == null || product == null || product.getId() == null) {
            return;
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        LocalDateTime now = LocalDateTime.now();

        ProductView view = productViewRepository
                .findByUser_IdAndProduct_Id(user.getId(), product.getId())
                .orElseGet(() -> {
                    ProductView pv = new ProductView();
                    pv.setUser(user);
                    pv.setProduct(product);
                    pv.setFirstViewedAt(now);
                    return pv;
                });

        view.setViewCount(view.getViewCount() + 1);
        view.setLastViewedAt(now);
        productViewRepository.save(view);
    }

    @Transactional(readOnly = true)
    public List<Product> getRecommendationsForUser(String username, int limit) {
        if (username == null || limit <= 0) {
            return List.of();
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return List.of();
        }

        List<ProductView> topViews = productViewRepository.findByUser_IdOrderByViewCountDescLastViewedAtDesc(
                userOpt.get().getId(),
                PageRequest.of(0, 5)
        );

        if (topViews.isEmpty()) {
            return List.of();
        }

        return scoreSimilarProducts(topViews, null, limit);
    }

    @Transactional(readOnly = true)
    public List<Product> getSimilarProducts(Product source, int limit) {
        if (source == null || source.getId() == null || limit <= 0) {
            return List.of();
        }

        ProductView sourceView = new ProductView();
        sourceView.setProduct(source);
        sourceView.setViewCount(3);

        return scoreSimilarProducts(List.of(sourceView), source.getId(), limit);
    }

    private List<Product> scoreSimilarProducts(List<ProductView> interests, Long excludedProductId, int limit) {
        Set<Long> viewedProductIds = new HashSet<>();
        Map<Long, Integer> scores = new HashMap<>();
        Map<Long, Product> productById = new HashMap<>();
        List<Product> allProducts = productRepository.findAll();

        for (ProductView interest : interests) {
            Product viewed = interest.getProduct();
            if (viewed != null && viewed.getId() != null) {
                viewedProductIds.add(viewed.getId());
            }
        }

        for (ProductView interest : interests) {
            Product viewed = interest.getProduct();
            if (viewed == null || viewed.getId() == null) {
                continue;
            }

            int weight = Math.max(1, interest.getViewCount());

            for (Product candidate : allProducts) {
                if (candidate.getId() == null || candidate.getId().equals(viewed.getId())) {
                    continue;
                }
                if (excludedProductId != null && candidate.getId().equals(excludedProductId)) {
                    continue;
                }
                if (viewedProductIds.contains(candidate.getId())) {
                    continue;
                }

                int score = calculateSimilarityScore(viewed, candidate) * weight;
                if (score <= 0) {
                    continue;
                }

                scores.merge(candidate.getId(), score, Integer::sum);
                productById.put(candidate.getId(), candidate);
            }
        }

        return scores.entrySet()
                .stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> productById.get(entry.getKey()))
                .filter(Objects::nonNull)
                .toList();
    }

    private int calculateSimilarityScore(Product source, Product candidate) {
        int score = 0;

        if (sameBrand(source, candidate)) score += 5;
        if (sameText(source.getRam(), candidate.getRam())) score += 2;
        if (sameText(source.getCpu(), candidate.getCpu())) score += 2;
        if (sameText(source.getGpu(), candidate.getGpu())) score += 1;
        if (sameText(source.getDisplay(), candidate.getDisplay())) score += 1;

        return score;
    }

    private boolean sameBrand(Product a, Product b) {
        return a.getBrand() != null
                && b.getBrand() != null
                && Objects.equals(a.getBrand().getId(), b.getBrand().getId());
    }

    private boolean sameText(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }
}
