package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Repository.ProductRepository;
import com.laptopstore.laptopstore.Service.CompareService;
import com.laptopstore.laptopstore.entity.Product;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CompareController {

    @Autowired
    private CompareService compareService;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping({"/compare", "/product_compare"})
    public String comparePage(
            @RequestParam(value = "ids", required = false) String ids,
            HttpSession session,
            Model model
    ) {
        List<Product> products = new ArrayList<>();
        if (ids != null && !ids.isBlank()) {
            String[] idArr = ids.split(",");
            for (String sId : idArr) {
                try {
                    Long pId = Long.parseLong(sId.trim());
                    productRepository.findById(pId).ifPresent(products::add);
                } catch (NumberFormatException ignored) {}
            }
        } else {
            products = compareService.getCompareProducts(session);
        }

        model.addAttribute("compareProducts", products);
        model.addAttribute("pageTitle", "So sánh sản phẩm Laptop – LaptopStore");
        return "product_compare";
    }

    @GetMapping("/api/compare/products")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getCompareProductsApi(
            @RequestParam(value = "ids", required = false) String ids,
            HttpSession session
    ) {
        List<Product> products = new ArrayList<>();
        if (ids != null && !ids.isBlank()) {
            String[] idArr = ids.split(",");
            for (String sId : idArr) {
                try {
                    Long pId = Long.parseLong(sId.trim());
                    productRepository.findById(pId).ifPresent(products::add);
                } catch (NumberFormatException ignored) {}
            }
        } else {
            products = compareService.getCompareProducts(session);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("price", p.getFinalPrice());
            map.put("image", (p.getImages() != null && !p.getImages().isBlank()) ? p.getImages().split(",")[0] : "/images/default-avatar.png");
            map.put("brand", p.getBrand() != null ? p.getBrand().getName() : "N/A");
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/compare/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addCompareApi(
            @RequestParam("productId") Long productId,
            HttpSession session
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean added = compareService.addProductToCompare(productId, session);
            List<Long> compareIds = compareService.getCompareList(session);
            response.put("success", true);
            response.put("added", added);
            response.put("count", compareIds.size());
            response.put("message", added ? "Đã thêm sản phẩm vào bảng so sánh" : "Sản phẩm đã có trong danh sách so sánh");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/compare/remove")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeCompareApi(
            @RequestParam("productId") Long productId,
            HttpSession session
    ) {
        compareService.removeProductFromCompare(productId, session);
        List<Long> compareIds = compareService.getCompareList(session);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", compareIds.size());
        response.put("message", "Đã xóa sản phẩm khỏi danh sách so sánh");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/compare/remove/{productId}")
    public String removeCompareForm(
            @PathVariable Long productId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        compareService.removeProductFromCompare(productId, session);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi bảng so sánh.");
        return "redirect:/product_compare";
    }

    @PostMapping("/compare/clear")
    public String clearCompare(HttpSession session, RedirectAttributes redirectAttributes) {
        compareService.clearCompareList(session);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa toàn bộ danh sách so sánh.");
        return "redirect:/product_compare";
    }
}
