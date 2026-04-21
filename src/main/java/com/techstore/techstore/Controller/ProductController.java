package com.techstore.techstore.Controller;

import com.techstore.techstore.Service.BrandService;
import com.techstore.techstore.Service.ProductService;
import com.techstore.techstore.Service.ProductVariantService;
import com.techstore.techstore.entity.Product;
import com.techstore.techstore.entity.ProductVariant;
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

        return "product_detail";
    }

}
