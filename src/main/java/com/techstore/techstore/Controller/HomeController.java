package com.techstore.techstore.Controller;

import com.techstore.techstore.Service.BrandService;
import com.techstore.techstore.Service.ProductService;
import com.techstore.techstore.Service.VoucherService;
import com.techstore.techstore.entity.Product;
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
    private com.techstore.techstore.Service.UserService userService;

    @Autowired
    private com.techstore.techstore.Service.RecommendationService recommendationService;

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
            com.techstore.techstore.entity.User user = userService.findByUsername(username).orElse(null);
            if (user != null) {
                List<Product> recommendations = recommendationService.getRecommendationsForUser(user, 12);
                model.addAttribute("recommendations", recommendations);
            }
        }

        return "home";
    }
}
