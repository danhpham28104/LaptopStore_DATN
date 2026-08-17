package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Service.ProductService;
import com.laptopstore.laptopstore.entity.Product;
import com.laptopstore.laptopstore.entity.ProductVariant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/products")
public class ProductApiController {

    @Autowired
    private ProductService productService;

    /**
     * Lay chi tiet san pham + danh sach bien the dang JSON de hien thi Quick Select Modal
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProductDetailsApi(@PathVariable Long id) {
        Optional<Product> opt = productService.getProductById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product p = opt.get();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", p.getId());
        res.put("name", p.getName());
        res.put("brandName", p.getBrand() != null ? p.getBrand().getName() : "Khác");
        res.put("price", p.getPrice());
        res.put("finalPrice", p.getFinalPrice());
        res.put("salePercent", p.getSalePercent() != null ? p.getSalePercent() : 0);
        res.put("stock", p.getStock() != null ? p.getStock() : 0);

        String mainImg = "/images/default-avatar.png";
        if (p.getImages() != null && !p.getImages().isBlank()) {
            mainImg = p.getImages().split(",")[0].trim();
        }
        res.put("image", mainImg);

        // List variants
        List<Map<String, Object>> variantList = new ArrayList<>();
        if (p.getVariants() != null) {
            for (ProductVariant v : p.getVariants()) {
                Map<String, Object> vMap = new HashMap<>();
                vMap.put("id", v.getId());
                vMap.put("color", v.getColor() != null ? v.getColor() : "");
                vMap.put("storage", v.getStorage() != null ? v.getStorage() : "");
                vMap.put("stock", v.getStock() != null ? v.getStock() : 0);
                vMap.put("image", v.getImage() != null && !v.getImage().isBlank() ? v.getImage() : mainImg);
                variantList.add(vMap);
            }
        }
        res.put("variants", variantList);

        return ResponseEntity.ok(res);
    }
}
