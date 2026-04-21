package com.techstore.techstore.Controller;

import com.techstore.techstore.Service.ProductService;
import com.techstore.techstore.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/fragments")
public class ProductFragmentController {

    @Autowired
    private ProductService productService;

    /** loc theo thuong hieu*/
    @GetMapping("/products/by-brand/{brandId}")
    public String getProductsByBrandFragment(@PathVariable Long brandId, Model model) {
        model.addAttribute("products", productService.getProductsByBrandId(brandId));
        return "fragments/product_list :: productList";
    }

    /**  theo RAM / ROM / Giá / Tên / Thương hiệu */
    @GetMapping("/products/filter")
    public String filterProductsFragment(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String ram,
            @RequestParam(required = false) String cpu,
            @RequestParam(required = false) String storage,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            Model model
    ) {
        List<Product> products = productService.advancedSearch(q, brand, ram, cpu, storage, minPrice, maxPrice);
        model.addAttribute("products", products);
        return "fragments/product_list :: productList";
    }

    /** 🔍 Tìm kiếm sản phẩm theo từ khóa (search bar trên header) */
    @GetMapping("/products/search")
    public String searchProductsFragment(@RequestParam(required = false) String q, Model model) {
        List<Product> products = (q == null || q.isBlank())
                ? productService.getAllProducts()
                : productService.searchByName(q.trim());
        model.addAttribute("products", products);
        model.addAttribute("searchQuery", q);
        return "fragments/product_list :: productList";
    }

    @GetMapping("/products/sort")
    public String sortProductsFragment(
            @RequestParam(required = false, defaultValue = "default") String sort,
            Model model
    ) {
        List<Product> products = productService.sortProducts(sort);
        model.addAttribute("products", products);
        return "fragments/product_list :: productList";
    }
}
