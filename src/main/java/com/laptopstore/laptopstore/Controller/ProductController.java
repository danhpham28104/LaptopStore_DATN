package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.*;
import com.laptopstore.laptopstore.entity.Product;
import com.laptopstore.laptopstore.entity.ProductVariant;
import com.laptopstore.laptopstore.entity.Review;
import com.laptopstore.laptopstore.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/product")
public class ProductController {

    @Autowired private ProductService productService;
    @Autowired private BrandService brandService;
    @Autowired private ProductVariantService productVariantService;
    @Autowired private UserService userService;
    @Autowired private ProductViewHistoryService productViewHistoryService;
    @Autowired private RecommendationService recommendationService;
    @Autowired private ReviewService reviewService;
    @Autowired private WishlistService wishlistService;
    @Autowired private AnalyticsEventService analyticsEventService;

    // Danh sách sản phẩm (Thymeleaf)
    @GetMapping
    public String listProducts(@RequestParam(required = false) String q,
                               @RequestParam(required = false) String brand,
                               @RequestParam(required = false) String categorySlug,
                               @RequestParam(required = false) String ram,
                               @RequestParam(required = false) String cpu,
                               @RequestParam(required = false) String color,
                               @RequestParam(required = false) String storage,
                               @RequestParam(required = false) Double minPrice,
                               @RequestParam(required = false) Double maxPrice,
                               Model model,
                               HttpServletRequest request) {

        model.addAttribute("brands", brandService.getAllBrands());

        List<Product> products;
        boolean hasFilter = (q != null && !q.isBlank()) ||
                            (brand != null && !brand.isBlank()) ||
                            (categorySlug != null && !categorySlug.isBlank()) ||
                            (ram != null && !ram.isBlank()) ||
                            (cpu != null && !cpu.isBlank()) ||
                            (color != null && !color.isBlank()) ||
                            (storage != null && !storage.isBlank()) ||
                            minPrice != null || maxPrice != null;

        if (hasFilter) {
            products = productService.advancedSearch(q, brand, categorySlug, ram, cpu, color, storage, minPrice, maxPrice);
            model.addAttribute("searchQuery", q);
            model.addAttribute("searchBrand", brand);
            model.addAttribute("selectedCategorySlug", categorySlug);
            model.addAttribute("searchColor", color);
            model.addAttribute("isSearch", true);

            // 🔹 Track search event (async, non-blocking)
            if (q != null && !q.isBlank()) {
                analyticsEventService.trackSearch(
                        AnalyticsEventService.extractSessionId(request),
                        getCurrentUserId(),
                        q,
                        products.size()
                );
            }
        } else {
            products = productService.getAllProducts();
            model.addAttribute("isSearch", false);
        }

        model.addAttribute("products", products);
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", 1);

        return "home";
    }

    // Chi tiết sản phẩm
    @GetMapping({"/detail/{id}", "/{id}"})
    public String productDetail(@PathVariable Long id, Model model, HttpServletRequest request) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        List<String> imageList = (product.getImages() != null && !product.getImages().isBlank())
                ? List.of(product.getImages().split(","))
                : List.of("/images/default-avatar.png");
        List<String> variantImages = product.getVariants()
                .stream()
                .map(ProductVariant::getImage)
                .filter(Objects::nonNull)
                .toList();

        List<String> allImages = new ArrayList<>();
        allImages.addAll(imageList);
        allImages.addAll(variantImages);

        model.addAttribute("allImages", allImages);
        model.addAttribute("product", product);
        model.addAttribute("variants", productVariantService.getVariantsByProduct(id));
        model.addAttribute("brands", brandService.getAllBrands());

        // Thông tin Đánh giá & Wishlist
        List<Review> approvedReviews = reviewService.getApprovedReviewsByProduct(id);
        Double averageRating = reviewService.getAverageRating(id);
        Long reviewCount = reviewService.getReviewCount(id);

        model.addAttribute("reviews", approvedReviews);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("reviewCount", reviewCount);

        boolean isWishlisted = false;
        boolean canReview = false;

        // Lịch sử xem và gợi ý đề cử sản phẩm
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String username = auth.getName();
            User user = userService.findByUsername(username).orElse(null);
            if (user != null) {
                userId = user.getId();
                // Tự động ghi nhận lượt xem (cho logged-in user)
                productViewHistoryService.trackView(user, product);

                // Đề cử sản phẩm tương tự
                List<Product> recommendations = recommendationService.getRecommendationsForUser(user, 4);
                model.addAttribute("recommendations", recommendations);

                // Wishlist & Review status
                isWishlisted = wishlistService.isWishlisted(user, id);
                canReview = reviewService.canUserReviewProduct(user, id, null);
            }
        }

        // 🔹 Track PRODUCT_VIEW (async, cả guest lẫn logged-in)
        analyticsEventService.trackProductView(
                AnalyticsEventService.extractSessionId(request),
                userId,
                id,
                AnalyticsEventService.extractClientIp(request)
        );

        model.addAttribute("isWishlisted", isWishlisted);
        model.addAttribute("canReview", canReview);

        return "product_detail";
    }

    /** Lấy userId của user đang đăng nhập (null nếu guest). */
    private Long getCurrentUserId() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        try {
            return userService.findByUsername(auth.getName()).map(u -> u.getId()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}

