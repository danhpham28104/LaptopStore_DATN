package com.techstore.techstore.Controller;

import com.techstore.techstore.Service.BrandService;
import com.techstore.techstore.Service.ProductService;
import com.techstore.techstore.Service.ProductVariantService;
import com.techstore.techstore.entity.Brand;
import com.techstore.techstore.entity.Product;
import com.techstore.techstore.entity.ProductVariant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    @Autowired private ProductService productService;
    @Autowired private BrandService brandService;
    @Autowired private ProductVariantService variantService;

    /** 🔹 Danh sách sản phẩm */
    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model
    ) {
        Page<Product> productPage = productService.getPaginatedProducts(page, size);

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("pageSize", size);

        model.addAttribute("brands", brandService.getAllBrands());
        model.addAttribute("active", "products");
        model.addAttribute("pageTitle", "Quản lý sản phẩm - LaptopStore Admin");

        return "admin/products";
    }



    @PostMapping("/add")
    public String addProduct(
            @RequestParam String name,
            @RequestParam String modelName,
            @RequestParam BigDecimal price,
            @RequestParam Integer stock,
            @RequestParam Long brandId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String ram,
            @RequestParam(required = false) String display,
            @RequestParam(required = false) String cpu,
            @RequestParam(required = false) String gpu,
            @RequestParam(required = false) String battery,
            @RequestParam(required = false) String dimensions,
            @RequestParam(required = false) String material,
            @RequestParam(required = false) String badge,
            @RequestParam(required = false, defaultValue = "0") Integer salePercent,
            @RequestParam(required = false) MultipartFile[] imageFiles,
            @RequestParam(required = false) MultipartFile[] variantImages,
            HttpServletRequest request
    ) throws IOException {

        Brand brand = brandService.getById(brandId).orElse(null);
        if (brand == null) return "redirect:/admin/products?error=brand";

        Product p = new Product();
        p.setName(name);
        p.setModel(modelName);
        p.setPrice(price);
        p.setStock(stock);
        p.setDescription(description);
        p.setBrand(brand);
        p.setBadge(badge);
        p.setSalePercent(salePercent);




        // --- SET CÁC FIELD KỸ THUẬT ---
        p.setRam(ram);
        p.setDisplay(display);
        p.setCpu(cpu);
        p.setGpu(gpu);
        p.setBattery(battery);
        p.setDimensions(dimensions);
        p.setMaterial(material);

        // --- LƯU ẢNH CHÍNH ---
        if (imageFiles != null && imageFiles.length > 0) {

            StringBuilder imageList = new StringBuilder();

            for (MultipartFile file : imageFiles) {
                if (!file.isEmpty()) {
                    String savedPath = saveImage(file);
                    imageList.append(savedPath).append(",");
                }
            }

            // xoá dấu phẩy cuối
            if (imageList.length() > 0) {
                imageList.setLength(imageList.length() - 1);
            }

            p.setImages(imageList.toString());
        }



        productService.save(p);

        // --- XỬ LÝ BIẾN THỂ ---
        String[] colors = request.getParameterValues("variantColors");
        String[] storages = request.getParameterValues("variantStorages");
        String[] stocks = request.getParameterValues("variantStocks");

        if (colors != null) {
            for (int i = 0; i < colors.length; i++) {
                if (colors[i].isBlank() || storages[i].isBlank()) continue;

                ProductVariant v = new ProductVariant();
                v.setProduct(p);
                v.setColor(colors[i]);
                v.setStorage(storages[i]);

                try {
                    v.setStock(Integer.parseInt(stocks[i]));
                } catch (Exception e) {
                    v.setStock(0);
                }

                if (variantImages != null && variantImages.length > i && !variantImages[i].isEmpty()) {
                    v.setImage(saveImage(variantImages[i]));
                }



                variantService.save(v);
            }
        }

        return "redirect:/admin/products?added=true";

    }


    @PostMapping("/edit/{id}")
    public String editProduct(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String modelName,
            @RequestParam BigDecimal price,
            @RequestParam Integer stock,
            @RequestParam Long brandId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String ram,
            @RequestParam(required = false) String display,
            @RequestParam(required = false) String cpu,
            @RequestParam(required = false) String gpu,
            @RequestParam(required = false) String battery,
            @RequestParam(required = false) String dimensions,
            @RequestParam(required = false) String material,
            @RequestParam(required = false) String badge,
            @RequestParam(required = false, defaultValue = "0") Integer salePercent,
            @RequestParam(required = false) MultipartFile[] imageFiles,

            // biến thể cũ
            @RequestParam(required = false) Long[] variantIds,
            @RequestParam(required = false) String[] variantOldColors,
            @RequestParam(required = false) String[] variantOldStorages,
            @RequestParam(required = false) Integer[] variantOldStocks,
            @RequestParam(required = false) MultipartFile[] variantOldImages,
            @RequestParam(required = false) int[] deleteOldVariant,

            // biến thể mới
            @RequestParam(required = false) String[] variantColors,
            @RequestParam(required = false) String[] variantStorages,
            @RequestParam(required = false) Integer[] variantStocks,
            @RequestParam(required = false) MultipartFile[] variantImages
    ) throws IOException {

        Product p = productService.getProductById(id).orElse(null);
        if (p == null) return "redirect:/admin/products?error=notfound";

        // --- cập nhật product ---
        p.setName(name);
        p.setModel(modelName);
        p.setPrice(price);
        p.setStock(stock);
        p.setDescription(description);
        p.setRam(ram);
        p.setDisplay(display);
        p.setCpu(cpu);
        p.setGpu(gpu);
        p.setBattery(battery);
        p.setDimensions(dimensions);
        p.setMaterial(material);
        p.setBadge(badge);
        p.setSalePercent(salePercent);



        if (imageFiles != null && imageFiles.length > 0) {

            StringBuilder list = new StringBuilder();

            for (MultipartFile file : imageFiles) {
                if (!file.isEmpty()) {
                    list.append(saveImage(file)).append(",");
                }
            }

            if (list.length() > 0) {
                list.setLength(list.length() - 1); // xoá dấu phẩy cuối
                p.setImages(list.toString());
            }
        }



        productService.save(p);

        // --- xử lý biến thể cũ ---
        if (variantIds != null) {
            for (int i = 0; i < variantIds.length; i++) {

                ProductVariant v = variantService
                        .getVariantById(variantIds[i])
                        .orElseThrow(() -> new RuntimeException("Variant not found"));


                if (deleteOldVariant[i] == 1) {
                    variantService.delete(variantIds[i]);
                    continue;
                }

                v.setColor(variantOldColors[i]);
                v.setStorage(variantOldStorages[i]);
                v.setStock(variantOldStocks[i]);

                if (variantOldImages != null && variantOldImages.length > i && !variantOldImages[i].isEmpty()) {

                    v.setImage(saveImage(variantOldImages[i]));
                }



                variantService.save(v);
            }
        }

        // --- thêm biến thể mới ---
        if (variantColors != null) {
            for (int i = 0; i < variantColors.length; i++) {
                if (variantColors[i].isBlank()) continue;

                ProductVariant v = new ProductVariant();
                v.setProduct(p);
                v.setColor(variantColors[i]);
                v.setStorage(variantStorages[i]);
                v.setStock(variantStocks[i]);

                if (variantImages != null && variantImages.length > i && !variantImages[i].isEmpty()) {

                    v.setImage(saveImage(variantImages[i]));
                }


                variantService.save(v);
            }
        }

        return "redirect:/admin/products?updated=true";

    }


    /** 🔹 Xoá sản phẩm */
    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return "redirect:/admin/products?deleted=true";

    }

    @GetMapping("/search")
    public String listProducts(@RequestParam(required = false) String q,
                               Model model) {

        List<Product> products;

        if (q != null && !q.isBlank()) {

            // Nếu người dùng nhập số → tìm theo ID
            if (q.matches("\\d+")) {
                Optional<Product> p = productService.getProductById(Long.parseLong(q));
                products = p.map(List::of).orElse(List.of());
            }
            // Còn lại → tìm theo tên
            else {
                products = productService.searchByName(q);
            }

            model.addAttribute("q", q); // giữ lại giá trị trong ô tìm kiếm
        }
        else {
            products = productService.getAllProducts();
        }

        model.addAttribute("products", products);
        model.addAttribute("brands", brandService.getAllBrands());
        model.addAttribute("active", "products");
        model.addAttribute("pageTitle", "Quản lý sản phẩm - LaptopStore Admin");

        return "admin/products";
    }

    private String saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String uploadDir = new ClassPathResource("static/images/products/").getFile().getAbsolutePath();
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        File dest = new File(uploadDir + File.separator + fileName);

        file.transferTo(dest);
        return "/images/products/" + fileName;
    }

}
