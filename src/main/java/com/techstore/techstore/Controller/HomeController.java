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

/**
 * Controller cho trang chủ và danh sách sản phẩm
 */
@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

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

        return "home";
    }
}
