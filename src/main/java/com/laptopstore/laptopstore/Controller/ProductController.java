package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.BrandService;
import com.laptopstore.laptopstore.Service.ProductService;
import com.laptopstore.laptopstore.Service.ProductVariantService;
import com.laptopstore.laptopstore.entity.Product;
import com.laptopstore.laptopstore.entity.ProductVariant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/product")
public class ProductController {

    @Autowired private ProductService productService;
    @Autowired private BrandService brandService;
    @Autowired private ProductVariantService productVariantService;
    @Autowired private com.laptopstore.laptopstore.Service.UserService userService;
    @Autowired private com.laptopstore.laptopstore.Service.ProductViewHistoryService productViewHistoryService;
    @Autowired private com.laptopstore.laptopstore.Service.RecommendationService recommendationService;

    //  Danh sách sản phẩm (Thymeleaf)
    @GetMapping
    public String listProducts(@RequestParam(required = false) String q,
                               @RequestParam(required = false) String brand,
                               Model model) {

        model.addAttribute("brands", brandService.getAllBrands());

        List<Product> products;
        if (q != null && !q.isBlank()) {
            products = productService.searchByName(q);
            model.addAttribute("searchQuery", q);
            model.addAttribute("isSearch", true);
        } else if (brand != null && !brand.isBlank()) {
            products = productService.searchByBrandName(brand);
            model.addAttribute("searchBrand", brand);
            model.addAttribute("isSearch", true);
        } else {
            products = productService.getAllProducts();
            model.addAttribute("isSearch", false);
        }

        model.addAttribute("products", products);
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", 1);

        return "home";
    }

// chi tiết sản phẩm

    @GetMapping("/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        List<String> imageList = List.of(product.getImages().split(","));
        List<String> variantImages = product.getVariants()
                .stream()
                .map(ProductVariant::getImage)
                .filter(Objects::nonNull)
                .toList();

        List<String> allImages = new ArrayList<>();
        allImages.addAll(imageList);
        allImages.addAll(variantImages);

        model.addAttribute("allImages", allImages);
        model.addAttribute("product", product);
        model.addAttribute("variants", productVariantService.getVariantsByProduct(id));
        model.addAttribute("brands", brandService.getAllBrands());

        // Lịch sử xem và gợi ý đề cử sản phẩm
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String username = auth.getName();
            com.laptopstore.laptopstore.entity.User user = userService.findByUsername(username).orElse(null);
            if (user != null) {
                // Tự động ghi nhận lượt xem
                productViewHistoryService.trackView(user, product);
                
                // Đề cử sản phẩm tương tự
                List<Product> recommendations = recommendationService.getRecommendationsForUser(user, 4);
                model.addAttribute("recommendations", recommendations);
            }
        }

        return "product_detail";
    }

}
