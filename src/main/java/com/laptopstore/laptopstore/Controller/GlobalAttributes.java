package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.CategoryService;
import com.laptopstore.laptopstore.Service.CartService;
import com.laptopstore.laptopstore.Service.UserService;
import com.laptopstore.laptopstore.Service.VoucherService;
import com.laptopstore.laptopstore.Service.WishlistService;
import com.laptopstore.laptopstore.entity.Category;
import com.laptopstore.laptopstore.entity.User;
import com.laptopstore.laptopstore.entity.Voucher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalAttributes {

    @Autowired
    private VoucherService voucherService;
    @Autowired
    private UserService userService;
    @Autowired
    private CartService cartService;
    @Autowired
    private WishlistService wishlistService;
    @Autowired
    private CategoryService categoryService;

    @ModelAttribute("categories")
    public List<Category> getActiveCategories() {
        return categoryService.getActiveCategories();
    }

    @ModelAttribute("vouchers")
    public List<Voucher> userVouchers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return List.of();
        }

        String username = auth.getName();
        User user = userService.findByUsername(username).orElse(null);

        if (user == null) {
            return List.of();
        }

        return voucherService.getAvailableVouchers(user.getId());
    }

    @ModelAttribute("cartCount")
    public int getCartItemCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return 0;
        }

        String username = auth.getName();
        return cartService.getCartItemCount(username);
    }

    @ModelAttribute("wishlistCount")
    public long getWishlistItemCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return 0L;
        }

        String username = auth.getName();
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) return 0L;

        return wishlistService.getWishlistCount(user);
    }
}
