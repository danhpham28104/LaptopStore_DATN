package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Repository.ReviewRepository;
import com.laptopstore.laptopstore.Service.ReviewService;
import com.laptopstore.laptopstore.Service.UserService;
import com.laptopstore.laptopstore.dto.ReviewRequestDto;
import com.laptopstore.laptopstore.entity.Review;
import com.laptopstore.laptopstore.entity.User;
import com.laptopstore.laptopstore.enums.ReviewStatus;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReviewRepository reviewRepository;

    @PostMapping("/reviews/add")
    public String submitReview(@Valid @ModelAttribute ReviewRequestDto reviewDto,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập để gửi nhận xét!");
            return "redirect:/login";
        }

        try {
            User user = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin tài khoản!"));

            reviewService.addReview(user, reviewDto);
            redirectAttributes.addFlashAttribute("success", "Cảm ơn bạn! Đánh giá của bạn đã được ghi nhận.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/product/" + reviewDto.getProductId();
    }

    @PostMapping("/api/reviews/add")
    @ResponseBody
    public ResponseEntity<?> submitReviewApi(@Valid @RequestBody ReviewRequestDto reviewDto, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        if (principal == null) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập để gửi đánh giá!");
            return ResponseEntity.status(401).body(response);
        }

        try {
            User user = userService.findByUsername(principal.getName()).orElse(null);
            Review review = reviewService.addReview(user, reviewDto);
            response.put("success", true);
            response.put("message", "Đánh giá của bạn đã gửi thành công!");
            response.put("reviewId", review.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/reviews/product/{productId}")
    @ResponseBody
    public Map<String, Object> getReviews(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer rating) {

        List<Review> reviews;
        if (rating != null) {
            if (rating <= 2) {
                reviews = reviewRepository.findByProductIdAndStatusAndRatingLessThanEqualOrderByCreatedAtDesc(
                        productId, ReviewStatus.APPROVED, 2);
            } else {
                reviews = reviewRepository.findByProductIdAndStatusAndRatingOrderByCreatedAtDesc(
                        productId, ReviewStatus.APPROVED, rating);
            }
        } else {
            reviews = reviewService.getApprovedReviewsByProduct(productId);
        }

        // Đếm theo từng sao
        Map<Integer, Long> starCounts = new HashMap<>();
        for (int i = 1; i <= 5; i++) starCounts.put(i, 0L);
        reviewRepository.countByProductIdAndStatusGroupByRating(productId, ReviewStatus.APPROVED)
                .forEach(row -> {
                    if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                        starCounts.put((Integer) row[0], (Long) row[1]);
                    }
                });

        List<Map<String, Object>> reviewList = reviews.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("rating", r.getRating());
            map.put("comment", r.getComment());
            map.put("createdAt", r.getCreatedAt());
            map.put("user", Map.of("fullName", r.getUser() != null ? (r.getUser().getFullName() != null && !r.getUser().getFullName().isBlank() ? r.getUser().getFullName() : r.getUser().getUsername()) : "Khách hàng TechStore"));
            return map;
        }).toList();

        Double avgRating = reviewService.getAverageRating(productId);
        Long totalReviews = reviewService.getReviewCount(productId);

        Map<String, Object> result = new HashMap<>();
        result.put("reviews", reviewList);
        result.put("starCounts", starCounts);
        result.put("total", reviewList.size());
        result.put("totalReviews", totalReviews);
        result.put("averageRating", avgRating);
        return result;
    }
}
