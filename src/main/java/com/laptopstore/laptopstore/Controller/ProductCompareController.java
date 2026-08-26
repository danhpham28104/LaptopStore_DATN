package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.ProductCompareService;
import com.laptopstore.laptopstore.Service.ReviewService;
import com.laptopstore.laptopstore.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProductCompareController {

    @Autowired
    private ProductCompareService productCompareService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/products/compare")
    public String compareProducts(@RequestParam(name = "ids", required = false) List<Long> ids, Model model) {
        if (ids == null) {
            ids = List.of();
        }

        List<Product> products = productCompareService.getProductsForComparison(ids);

        // Tính điểm đánh giá trung bình cho từng sản phẩm trong danh sách so sánh
        Map<Long, Double> ratings = new HashMap<>();
        Map<Long, Long> reviewCounts = new HashMap<>();
        for (Product product : products) {
            ratings.put(product.getId(), reviewService.getAverageRating(product.getId()));
            reviewCounts.put(product.getId(), reviewService.getReviewCount(product.getId()));
        }

        model.addAttribute("products", products);
        model.addAttribute("productIds", ids);
        model.addAttribute("ratings", ratings);
        model.addAttribute("reviewCounts", reviewCounts);

        return "product_compare";
    }
}
