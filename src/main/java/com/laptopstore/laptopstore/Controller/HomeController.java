package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.BrandService;
import com.laptopstore.laptopstore.Service.ProductService;
import com.laptopstore.laptopstore.Service.VoucherService;
import com.laptopstore.laptopstore.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controller cho trang chủ và danh sách sản phẩm
 */
@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private com.laptopstore.laptopstore.Service.UserService userService;

    @Autowired
    private com.laptopstore.laptopstore.Service.RecommendationService recommendationService;

    /** Trang chủ: hiển thị sản phẩm + thương hiệu */
    @GetMapping({"/", "/home"})
    public String home(@RequestParam(defaultValue = "0") int page, Model model) {
        int pageSize = 12;
        Page<Product> productPage = productService.getPaginatedProducts(page, pageSize);
        model.addAttribute("brands", brandService.getAllBrands());
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("siteName", "LaptopStore");

        // Lấy sản phẩm đề cử nếu người dùng đã đăng nhập
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String username = auth.getName();
            com.laptopstore.laptopstore.entity.User user = userService.findByUsername(username).orElse(null);
            if (user != null) {
                List<Product> recommendations = recommendationService.getRecommendationsForUser(user, 12);
                model.addAttribute("recommendations", recommendations);
            }
        }

        return "home";
    }
}
