package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Repository.CartItemRepository;
import com.laptopstore.laptopstore.Repository.CartRepository;
import com.laptopstore.laptopstore.Repository.UserRepository;
import com.laptopstore.laptopstore.Service.CartService;
import com.laptopstore.laptopstore.Service.ProductService;
import com.laptopstore.laptopstore.Service.ProductVariantService;
import com.laptopstore.laptopstore.entity.Cart;
import com.laptopstore.laptopstore.entity.Product;
import com.laptopstore.laptopstore.entity.ProductVariant;
import com.laptopstore.laptopstore.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.laptopstore.laptopstore.entity.CartItem;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private CartService cartService;
    @Autowired private ProductService productService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductVariantService productVariantService;
    @Autowired private com.laptopstore.laptopstore.Service.AnalyticsEventService analyticsEventService;

    /**  Hiển thị giỏ hàng của user (phải đăng nhập) */
    @GetMapping
    public String showCart(Model model, HttpServletRequest request, Principal principal) {
        if (principal == null) return "redirect:/login"; //  Chưa đăng nhập → login

        //  Lấy token CSRF cho form Thymeleaf
        CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
        model.addAttribute("_csrf", csrfToken);

        //  Lấy user và giỏ hàng
        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        Cart cart = cartService.getCartByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng của user"));

        cart.setItems(cartItemRepository.findByCart_Id(cart.getId()));

        System.out.println("🛒 Cart ID: " + cart.getId() + " có " + cart.getItems().size() + " item(s)");


        //  Tính lại tổng tiền
        cart.recalcTotals();

        //  Truyền dữ liệu sang view
        model.addAttribute("cart", cart);
        model.addAttribute("siteName", "LaptopStore");
        model.addAttribute("pageTitle", "Giỏ hàng – LaptopStore");

        return "cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(required = false) Long variantId,
                            @RequestParam(defaultValue = "1") int quantity,
                            Principal principal,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {

        if (principal == null) return "redirect:/login";

        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Cart cart = cartService.getCartByUserId(user.getId())
                .orElseGet(() -> cartService.createCartForUser(user));

        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        ProductVariant variant = null;
        if (variantId != null) {
            variant = productVariantService.getVariantById(variantId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên bản"));
        }

        // Kiểm tra stock
        int availableStock = (variant != null) ? (variant.getStock() != null ? variant.getStock() : 0) : (product.getStock() != null ? product.getStock() : 0);
        int reservedStock = (variant != null) ? (variant.getReservedStock() != null ? variant.getReservedStock() : 0) : (product.getReservedStock() != null ? product.getReservedStock() : 0);
        int realAvailable = availableStock - reservedStock;

        int currentInCart = 0;
        if (cart.getItems() != null) {
            final Long targetVariantId = variantId;
            for (CartItem item : cart.getItems()) {
                boolean sameProduct = item.getProduct().getId().equals(productId);
                boolean sameVariant = (targetVariantId == null && item.getVariant() == null) ||
                        (targetVariantId != null && item.getVariant() != null && item.getVariant().getId().equals(targetVariantId));
                if (sameProduct && sameVariant) {
                    currentInCart += item.getQuantity();
                }
            }
        }

        if (quantity + currentInCart > realAvailable) {
            redirectAttributes.addFlashAttribute("error",
                    "Sản phẩm '" + product.getName() + "' không đủ tồn kho. Còn lại: " + realAvailable);
            return "redirect:/product/" + productId;
        }

        cartService.addToCart(cart, product, variant, quantity);

        // 🔹 Track ADD_TO_CART event
        analyticsEventService.trackAddToCart(
                com.laptopstore.laptopstore.Service.AnalyticsEventService.extractSessionId(request),
                user.getId(),
                productId,
                variantId,
                com.laptopstore.laptopstore.Service.AnalyticsEventService.extractClientIp(request)
        );

        return "redirect:/cart";
    }


    /**  Cập nhật số lượng sản phẩm trong giỏ */
    @PostMapping("/update")
    public String updateItem(@RequestParam Long itemId,
                             @RequestParam String action,
                             @RequestParam(required = false) Integer quantity,
                             RedirectAttributes redirectAttributes) {

        CartItem item = cartItemRepository.findById(itemId).orElse(null);
        if (item != null) {
            int current = item.getQuantity();
            int newQty = current;

            if ("increase".equals(action)) newQty = current + 1;
            else if ("decrease".equals(action) && current > 1) newQty = current - 1;
            else if (quantity != null && quantity > 0) newQty = quantity;

            Product product = item.getProduct();
            ProductVariant variant = item.getVariant();

            int availableStock = (variant != null) ? (variant.getStock() != null ? variant.getStock() : 0) : (product.getStock() != null ? product.getStock() : 0);
            int reservedStock = (variant != null) ? (variant.getReservedStock() != null ? variant.getReservedStock() : 0) : (product.getReservedStock() != null ? product.getReservedStock() : 0);
            int realAvailable = availableStock - reservedStock;

            if (newQty > realAvailable) {
                redirectAttributes.addFlashAttribute("error",
                        "Sản phẩm '" + product.getName() + "' không đủ tồn kho. Còn lại: " + realAvailable);
                return "redirect:/cart";
            }

            item.setQuantity(newQty);
            item.recalc();

            Cart cart = item.getCart();
            cart.recalcTotals();
            cartRepository.save(cart);
        }

        return "redirect:/cart";
    }

    /**  Xóa 1 sản phẩm khỏi giỏ */
    @PostMapping("/remove")
    public String removeItem(@RequestParam Long itemId, HttpServletRequest request) {
        cartItemRepository.findById(itemId).ifPresent(item -> {
            Cart cart = item.getCart();
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
            cart.recalcTotals();
            cartRepository.save(cart);

            // 🔹 Track REMOVE_FROM_CART event
            Long userId = cart.getUser() != null ? cart.getUser().getId() : null;
            Long productId = item.getProduct() != null ? item.getProduct().getId() : null;
            Long variantId = item.getVariant() != null ? item.getVariant().getId() : null;
            analyticsEventService.trackRemoveFromCart(
                    com.laptopstore.laptopstore.Service.AnalyticsEventService.extractSessionId(request),
                    userId, productId, variantId
            );
        });
        return "redirect:/cart";
    }


    /**  Xóa toàn bộ giỏ hàng */
    @PostMapping("/clear")
    public String clearCart(@RequestParam Long cartId) {
        cartService.clearCart(cartId);
        return "redirect:/cart";
    }
}
