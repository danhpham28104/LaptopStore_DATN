package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.CartService;
import com.laptopstore.laptopstore.Service.UserService;
import com.laptopstore.laptopstore.Service.VoucherService;
import com.laptopstore.laptopstore.entity.User;
import com.laptopstore.laptopstore.entity.Voucher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.util.List;

@ControllerAdvice
public class GlobalAttributes {

//    @Autowired
//    private VoucherService voucherService;
//
//    @ModelAttribute("vouchers")
//    public List<Voucher> userVouchers(@AuthenticationPrincipal User user) {
//        if (user == null) {
//            return List.of();
//        }
//        System.out.println(voucherService.getAvailableVouchers(user.getId()).;);
//
//        return voucherService.getAvailableVouchers(user.getId());
//    }

    @Autowired
    private VoucherService voucherService;
    @Autowired
    private UserService userService;
    @Autowired
    private CartService cartService;

    @ModelAttribute("vouchers")
    public List<Voucher> userVouchers() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            System.out.println("⚠ User chưa đăng nhập → không load voucher");
            return List.of();
        }

        // Lấy username từ principal
        String username = auth.getName();
        User user = userService.findByUsername(username).orElse(null);

        if (user == null) {
            System.out.println("❌ Không tìm thấy user trong DB");
            return List.of();
        }

        List<Voucher> list = voucherService.getAvailableVouchers(user.getId());

        System.out.println("===== 🎁 VOUCHER CỦA USER " + username + " =====");
        if (list.isEmpty()) {
            System.out.println("⚠ Không có voucher nào");
        } else {
            list.forEach(v -> System.out.println("• " + v.getCode()));
        }
        System.out.println("==================================");

        return list;
    }

    @ModelAttribute("cartCount")
    public int getCartItemCount() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Nếu chưa đăng nhập
        if (auth == null || !auth.isAuthenticated() ||
                auth.getPrincipal().equals("anonymousUser")) {
            return 0;
        }

        String username = auth.getName();

        return cartService.getCartItemCount(username);
    }

}
