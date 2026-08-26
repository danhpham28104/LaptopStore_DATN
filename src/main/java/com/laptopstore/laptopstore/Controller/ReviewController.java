package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.ReviewService;
import com.laptopstore.laptopstore.Service.UserService;
import com.laptopstore.laptopstore.dto.ReviewRequestDto;
import com.laptopstore.laptopstore.entity.Review;
import com.laptopstore.laptopstore.entity.User;
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
    public ResponseEntity<?> getProductReviews(@PathVariable Long productId) {
        List<Review> reviews = reviewService.getApprovedReviewsByProduct(productId);
        Double avgRating = reviewService.getAverageRating(productId);
        Long count = reviewService.getReviewCount(productId);

        Map<String, Object> data = new HashMap<>();
        data.put("reviews", reviews);
        data.put("averageRating", avgRating);
        data.put("totalReviews", count);

        return ResponseEntity.ok(data);
    }
}
