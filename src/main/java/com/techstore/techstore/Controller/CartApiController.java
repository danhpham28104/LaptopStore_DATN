package com.techstore.techstore.Controller;

import com.techstore.techstore.Repository.CartRepository;
import com.techstore.techstore.Repository.UserRepository;
import com.techstore.techstore.Service.CartService;
import com.techstore.techstore.Service.ProductService;
import com.techstore.techstore.Service.ProductVariantService;
import com.techstore.techstore.entity.Cart;
import com.techstore.techstore.entity.Product;
import com.techstore.techstore.entity.ProductVariant;
import com.techstore.techstore.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * REST API endpoint cho giỏ hàng — được gọi từ chatbot AI via fetch().
 * CSRF đã bỏ qua cho /api/** theo SecurityConfig.
 * <p>
 * POST /api/cart/add  — thêm sản phẩm vào giỏ, trả về JSON.
 */
@RestController
@RequestMapping("/api/cart")
public class CartApiController {

    private static final Logger log = LoggerFactory.getLogger(CartApiController.class);

    private final CartService cartService;
    private final ProductService productService;
    private final ProductVariantService productVariantService;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;

    public CartApiController(CartService cartService,
                              ProductService productService,
                              ProductVariantService productVariantService,
                              UserRepository userRepository,
                              CartRepository cartRepository) {
        this.cartService = cartService;
        this.productService = productService;
        this.productVariantService = productVariantService;
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
    }

    /**
     * Thêm sản phẩm vào giỏ hàng qua API JSON (dùng từ chatbot).
     *
     * @param body      Map chứa productId, variantId (optional), quantity (optional, default = 1)
     * @param principal Principal của Spring Security (null nếu chưa đăng nhập)
     * @return JSON {success, message, cartCount} hoặc {success, redirect, message} khi chưa login
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addToCart(@RequestBody Map<String, Object> body,
                                                          Principal principal) {
        // Kiểm tra đăng nhập
        if (principal == null) {
            log.debug("[CartApi] Người dùng chưa đăng nhập, yêu cầu login.");
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "redirect", "/login",
                    "message", "Vui lòng đăng nhập để thêm vào giỏ hàng"
            ));
        }

        // Parse productId
        Long productId;
        try {
            productId = Long.parseLong(body.get("productId").toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Thiếu hoặc sai định dạng productId"
            ));
        }

        // Parse variantId (optional)
        Long variantId = null;
        if (body.get("variantId") != null && !body.get("variantId").toString().isBlank()) {
            try {
                variantId = Long.parseLong(body.get("variantId").toString());
            } catch (Exception ignored) {
                // bỏ qua nếu sai định dạng
            }
        }

        // Parse quantity (default = 1)
        int quantity = 1;
        if (body.get("quantity") != null) {
            try {
                quantity = Math.max(1, Integer.parseInt(body.get("quantity").toString()));
            } catch (Exception ignored) {}
        }

        try {
            String username = principal.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user: " + username));

            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID=" + productId));

            Cart cart = cartService.getCartByUserId(user.getId())
                    .orElseGet(() -> cartService.createCartForUser(user));

            // Xác định variant
            ProductVariant variant = null;
            if (variantId != null) {
                variant = productVariantService.getVariantById(variantId).orElse(null);
            }

            // Nếu không có variantId, tự chọn variant còn hàng
            if (variant == null && product.getVariants() != null && !product.getVariants().isEmpty()) {
                // Ưu tiên variant có stock > 0
                variant = product.getVariants().stream()
                        .filter(v -> v.getStock() != null && v.getStock() > 0)
                        .findFirst()
                        .orElse(product.getVariants().get(0));
            }

            // Kiểm tra hàng tồn kho
            if (variant != null && (variant.getStock() == null || variant.getStock() <= 0)) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Sản phẩm này hiện đã hết hàng"
                ));
            }

            cartService.addToCart(cart, product, variant, quantity);

            // Đếm tổng số item trong giỏ
            int cartCount = cartService.getCartItemCount(username);

            log.info("[CartApi] Đã thêm productId={} vào giỏ của user={} | cartCount={}",
                    productId, username, cartCount);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã thêm sản phẩm vào giỏ hàng",
                    "cartCount", cartCount
            ));

        } catch (Exception e) {
            log.error("[CartApi] Lỗi khi thêm vào giỏ: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Có lỗi xảy ra: " + e.getMessage()
            ));
        }
    }
}
