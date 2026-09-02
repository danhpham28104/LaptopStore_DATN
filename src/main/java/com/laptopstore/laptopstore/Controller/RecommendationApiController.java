package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.AnalyticsEventService;
import com.laptopstore.laptopstore.Service.RecommendationService;
import com.laptopstore.laptopstore.Service.UserService;
import com.laptopstore.laptopstore.entity.Product;
import com.laptopstore.laptopstore.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationApiController {

    private final RecommendationService recommendationService;
    private final UserService userService;
    private final AnalyticsEventService analyticsEventService;

    public RecommendationApiController(RecommendationService recommendationService,
                                       UserService userService,
                                       AnalyticsEventService analyticsEventService) {
        this.recommendationService = recommendationService;
        this.userService = userService;
        this.analyticsEventService = analyticsEventService;
    }

    /** GET /api/recommendations/personalized?limit=6 */
    @GetMapping("/personalized")
    public ResponseEntity<List<Product>> getPersonalizedRecommendations(
            @RequestParam(defaultValue = "6") int limit,
            Authentication authentication,
            HttpServletRequest request) {

        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            userId = userService.findByUsername(authentication.getName()).map(User::getId).orElse(null);
        }

        String sessionId = analyticsEventService.extractSessionId(request);
        List<Product> recommendations = recommendationService.getPersonalizedRecommendations(userId, sessionId, limit);

        return ResponseEntity.ok(recommendations);
    }

    /** GET /api/recommendations/similar/15?limit=4 */
    @GetMapping("/similar/{productId}")
    public ResponseEntity<List<Product>> getSimilarProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "4") int limit) {

        List<Product> similar = recommendationService.getSimilarProducts(productId, limit);
        return ResponseEntity.ok(similar);
    }
}
