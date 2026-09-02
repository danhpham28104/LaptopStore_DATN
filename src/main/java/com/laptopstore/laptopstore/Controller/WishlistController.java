package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.UserService;
import com.laptopstore.laptopstore.Service.WishlistService;
import com.laptopstore.laptopstore.entity.User;
import com.laptopstore.laptopstore.entity.Wishlist;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private UserService userService;

    @Autowired
    private com.laptopstore.laptopstore.Service.AnalyticsEventService analyticsEventService;

    @GetMapping("/wishlist")
    public String viewWishlist(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        List<Wishlist> wishlist = wishlistService.getUserWishlist(user);
        model.addAttribute("wishlist", wishlist);
        return "wishlist";
    }

    @PostMapping("/api/wishlist/toggle")
    @ResponseBody
    public ResponseEntity<?> toggleWishlistApi(@RequestParam Long productId, Principal principal, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để lưu sản phẩm yêu thích!");
            return ResponseEntity.status(401).body(response);
        }

        try {
            User user = userService.findByUsername(principal.getName()).orElse(null);
            boolean isWishlisted = wishlistService.toggleWishlist(user, productId);
            Long count = wishlistService.getWishlistCount(user);

            // 🔹 Track Wishlist event
            if (user != null) {
                analyticsEventService.trackWishlist(
                        com.laptopstore.laptopstore.Service.AnalyticsEventService.extractSessionId(request),
                        user.getId(), productId, isWishlisted
                );
            }

            response.put("success", true);
            response.put("wishlisted", isWishlisted);
            response.put("count", count);
            response.put("message", isWishlisted ? "Đã thêm vào sản phẩm yêu thích" : "Đã xóa khỏi sản phẩm yêu thích");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/wishlist/remove")
    public String removeFromWishlist(@RequestParam Long productId, Principal principal, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (principal != null) {
            User user = userService.findByUsername(principal.getName()).orElse(null);
            if (user != null) {
                wishlistService.removeFromWishlist(user, productId);

                // 🔹 Track REMOVE_FROM_WISHLIST
                analyticsEventService.trackWishlist(
                        com.laptopstore.laptopstore.Service.AnalyticsEventService.extractSessionId(request),
                        user.getId(), productId, false
                );

                redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm khỏi danh sách yêu thích");
            }
        }
        return "redirect:/wishlist";
    }
}

