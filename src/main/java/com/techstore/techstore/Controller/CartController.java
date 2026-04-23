package com.techstore.techstore.Controller;

import com.techstore.techstore.Repository.CartItemRepository;
import com.techstore.techstore.Repository.CartRepository;
import com.techstore.techstore.Repository.UserRepository;
import com.techstore.techstore.Service.CartService;
import com.techstore.techstore.Service.ProductService;
import com.techstore.techstore.Service.ProductVariantService;
import com.techstore.techstore.entity.Cart;
import com.techstore.techstore.entity.Product;
import com.techstore.techstore.entity.ProductVariant;
import com.techstore.techstore.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        model.addAttribute("siteName", "TechStore");
        model.addAttribute("pageTitle", "Giỏ hàng – TechStore");

        return "cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(required = false) Long variantId,
                            @RequestParam(defaultValue = "1") int quantity,
                            Principal principal) {

        if (principal == null) return "redirect:/login";

        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Cart cart = cartService.getCartByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        Product product = productService.getProductById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        ProductVariant variant = null;
        if (variantId != null) {
            variant = productVariantService.getVariantById(variantId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên bản"));
        }

        cartService.addToCart(cart, product, variant, quantity);

        return "redirect:/cart";
    }


    /**  Cập nhật số lượng sản phẩm trong giỏ */
    @PostMapping("/update")
    public String updateItem(@RequestParam Long itemId,
                             @RequestParam String action,
                             @RequestParam(required = false) Integer quantity) {

        cartItemRepository.findById(itemId).ifPresent(item -> {
            int current = item.getQuantity();

            if ("increase".equals(action)) item.setQuantity(current + 1);
            else if ("decrease".equals(action) && current > 1) item.setQuantity(current - 1);
            else if (quantity != null && quantity > 0) item.setQuantity(quantity);

            item.recalc();

            Cart cart = item.getCart();
            cart.recalcTotals();
            cartRepository.save(cart);
        });

        return "redirect:/cart";
    }

    /**  Xóa 1 sản phẩm khỏi giỏ */
    @PostMapping("/remove")
    public String removeItem(@RequestParam Long itemId) {
        cartItemRepository.findById(itemId).ifPresent(item -> {
            Cart cart = item.getCart();
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
            cart.recalcTotals();
            cartRepository.save(cart);
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
