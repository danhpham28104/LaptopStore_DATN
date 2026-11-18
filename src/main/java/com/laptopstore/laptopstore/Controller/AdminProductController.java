package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Repository.CategoryRepository;
import com.laptopstore.laptopstore.Service.BrandService;
import com.laptopstore.laptopstore.Service.ProductService;
import com.laptopstore.laptopstore.Service.ProductVariantService;
import com.laptopstore.laptopstore.Service.RagIntegrationService;
import com.laptopstore.laptopstore.entity.Brand;
import com.laptopstore.laptopstore.entity.Category;
import com.laptopstore.laptopstore.entity.Product;
import com.laptopstore.laptopstore.entity.ProductVariant;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private static final Logger log = LoggerFactory.getLogger(AdminProductController.class);

    @Autowired private ProductService productService;
    @Autowired private BrandService brandService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductVariantService variantService;
    @Autowired private RagIntegrationService ragIntegrationService;

    /** 🔹 Danh sách sản phẩm */
    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "false") boolean lowStockOnly,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String search,
            Model model
    ) {
        boolean isLowStockFilter = lowStockOnly || "low-stock".equalsIgnoreCase(filter);
        long lowStockCount = productService.countLowStockProducts(5);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("lowStockOnly", isLowStockFilter);

        if (search != null && !search.isBlank()) {
            List<Product> searched = productService.searchByName(search.trim());
            model.addAttribute("products", searched);
            model.addAttribute("q", search.trim());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 1);
            model.addAttribute("pageSize", searched.size());
        } else if (isLowStockFilter) {
            List<Product> lowStockProducts = productService.getLowStockProducts(5);
            model.addAttribute("products", lowStockProducts);
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 1);
            model.addAttribute("pageSize", lowStockProducts.size());
        } else {
            Page<Product> productPage = productService.getPaginatedProducts(page, size);
            model.addAttribute("products", productPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", productPage.getTotalPages());
            model.addAttribute("pageSize", size);
        }

        model.addAttribute("brands", brandService.getAllBrands());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("active", "products");
        model.addAttribute("pageTitle", "Quản lý sản phẩm - LaptopStore Admin");

        return "admin/products";
    }



    @PostMapping("/add")
    public String addProduct(
            @RequestParam String productCode,
            @RequestParam String name,
            @RequestParam String modelName,
            @RequestParam BigDecimal price,
            @RequestParam Integer stock,
            @RequestParam Long brandId,
            @RequestParam(required = false) Long categoryId,
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
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) throws IOException {

        String cleanCode = productCode != null ? productCode.trim() : "";
        if (cleanCode.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập mã ID cho laptop!");
            return "redirect:/admin/products";
        }
        String cleanModel = modelName != null ? modelName.trim() : "";

        Brand brand = brandService.getById(brandId).orElse(null);
        if (brand == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thương hiệu!");
            return "redirect:/admin/products";
        }
        Category category = categoryId != null ? categoryRepository.findById(categoryId).orElse(null) : null;

        // --- KIỂM TRA TRÙNG MÃ ID XUẤT XƯỞNG ---
        if (productService.existsByProductCode(cleanCode, null)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không thể thêm sản phẩm: Mã ID '" + cleanCode + "' đã tồn tại! Mỗi máy cần có một mã ID riêng biệt.");
            return "redirect:/admin/products";
        }

        // Giải phóng nếu mã ID này từng nằm ở bản ghi đã xóa mềm
        productService.releaseDeletedProductCode(cleanCode);

        Product p = new Product();
        p.setProductCode(cleanCode);
        p.setName(name != null ? name.trim() : "");
        p.setModel(cleanModel);
        p.setPrice(price);
        p.setStock(stock);
        p.setDescription(description);
        p.setBrand(brand);
        p.setCategory(category);
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

            if (imageList.length() > 0) {
                imageList.setLength(imageList.length() - 1);
                p.setImages(imageList.toString());
            } else {
                p.setImages(null);
            }
        } else {
            p.setImages(null);
        }

        try {
            productService.save(p);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.error("[Admin] Lỗi trùng lặp dữ liệu khi thêm sản phẩm: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không thể thêm sản phẩm: Mã ID '" + cleanCode + "' đã bị trùng!");
            return "redirect:/admin/products";
        }

        // --- XỬ LÝ BIẾN THỂ ---
        String[] colors = request.getParameterValues("variantColors");
        String[] storages = request.getParameterValues("variantStorages");
        String[] stocks = request.getParameterValues("variantStocks");

        boolean hasVariants = false;
        if (colors != null) {
            for (int i = 0; i < colors.length; i++) {
                if (colors[i] == null || colors[i].isBlank() || storages == null || storages.length <= i || storages[i].isBlank()) continue;

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
                hasVariants = true;
            }
        }

        // Cập nhật tổng tồn kho từ biến thể nếu có biến thể
        if (hasVariants) {
            Product freshP = productService.getProductById(p.getId()).orElse(p);
            freshP.updateTotalStock();
            productService.save(freshP);
        }

        // Đồng bộ sang RAG
        Product freshProduct = productService.getProductById(p.getId()).orElse(p);
        ragIntegrationService.syncProduct(freshProduct);

        redirectAttributes.addFlashAttribute("successMessage",
                "Đã thêm sản phẩm '" + p.getName() + "' (ID: " + cleanCode + ") thành công!");
        return "redirect:/admin/products";

    }



    @PostMapping("/edit/{id}")
    public String editProduct(
            @PathVariable Long id,
            @RequestParam String productCode,
            @RequestParam String name,
            @RequestParam String modelName,
            @RequestParam BigDecimal price,
            @RequestParam Integer stock,
            @RequestParam Long brandId,
            @RequestParam(required = false) Long categoryId,
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
            @RequestParam(required = false) MultipartFile[] variantImages,

            // Giữ phân trang & tìm kiếm
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String q,
            RedirectAttributes redirectAttributes
    ) throws IOException {

        Product p = productService.getProductById(id).orElse(null);
        if (p == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
            return "redirect:/admin/products";
        }

        String cleanCode = productCode != null ? productCode.trim() : "";
        if (cleanCode.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập mã ID cho laptop!");
            if (q != null && !q.isBlank()) {
                return "redirect:/admin/products/search?q=" + java.net.URLEncoder.encode(q, StandardCharsets.UTF_8) + "&page=" + page;
            }
            return "redirect:/admin/products?page=" + page;
        }
        String cleanModel = modelName != null ? modelName.trim() : "";

        // --- KIỂM TRA TRÙNG MÃ ID XUẤT XƯỞNG ---
        if (productService.existsByProductCode(cleanCode, id)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Mã ID '" + cleanCode + "' đã được sử dụng bởi sản phẩm khác!");
            if (q != null && !q.isBlank()) {
                return "redirect:/admin/products/search?q=" + java.net.URLEncoder.encode(q, StandardCharsets.UTF_8) + "&page=" + page;
            }
            return "redirect:/admin/products?page=" + page;
        }

        productService.releaseDeletedProductCode(cleanCode);

        Brand brand = brandService.getById(brandId).orElse(p.getBrand());
        Category category = categoryId != null ? categoryRepository.findById(categoryId).orElse(null) : null;

        // --- cập nhật product ---
        p.setProductCode(cleanCode);
        p.setName(name != null ? name.trim() : "");
        p.setModel(cleanModel);
        p.setPrice(price);
        p.setStock(stock);
        p.setBrand(brand);
        p.setCategory(category);
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
                list.setLength(list.length() - 1);
                p.setImages(list.toString());
            }
        }

        try {
            productService.save(p);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.error("[Admin] Lỗi trùng lặp dữ liệu khi sửa sản phẩm ID={}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không thể cập nhật: Mã ID '" + cleanCode + "' đã bị trùng!");
            return "redirect:/admin/products?page=" + page;
        }

        // --- xử lý biến thể cũ ---
        if (variantIds != null) {
            for (int i = 0; i < variantIds.length; i++) {
                ProductVariant v = variantService
                        .getVariantById(variantIds[i])
                        .orElse(null);
                if (v == null) continue;

                if (deleteOldVariant != null && deleteOldVariant.length > i && deleteOldVariant[i] == 1) {
                    variantService.delete(variantIds[i]);
                    continue;
                }

                if (variantOldColors != null && variantOldColors.length > i) v.setColor(variantOldColors[i]);
                if (variantOldStorages != null && variantOldStorages.length > i) v.setStorage(variantOldStorages[i]);
                if (variantOldStocks != null && variantOldStocks.length > i) v.setStock(variantOldStocks[i]);

                if (variantOldImages != null && variantOldImages.length > i && !variantOldImages[i].isEmpty()) {
                    v.setImage(saveImage(variantOldImages[i]));
                }

                variantService.save(v);
            }
        }

        // --- thêm biến thể mới ---
        if (variantColors != null) {
            for (int i = 0; i < variantColors.length; i++) {
                if (variantColors[i] == null || variantColors[i].isBlank()) continue;

                ProductVariant v = new ProductVariant();
                v.setProduct(p);
                v.setColor(variantColors[i]);
                v.setStorage(variantStorages != null && variantStorages.length > i ? variantStorages[i] : "");
                v.setStock(variantStocks != null && variantStocks.length > i ? variantStocks[i] : 0);

                if (variantImages != null && variantImages.length > i && !variantImages[i].isEmpty()) {
                    v.setImage(saveImage(variantImages[i]));
                }

                variantService.save(v);
            }
        }

        // Cập nhật tổng tồn kho từ các biến thể
        Product freshP = productService.getProductById(p.getId()).orElse(p);
        if (freshP.getVariants() != null && !freshP.getVariants().isEmpty()) {
            freshP.updateTotalStock();
            productService.save(freshP);
        }

        // Đồng bộ sang RAG
        Product freshProduct = productService.getProductById(p.getId()).orElse(p);
        ragIntegrationService.syncProduct(freshProduct);

        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật sản phẩm ID=" + cleanCode + " thành công!");

        if (q != null && !q.isBlank()) {
            return "redirect:/admin/products/search?q=" + java.net.URLEncoder.encode(q, StandardCharsets.UTF_8) + "&page=" + page;
        }
        return "redirect:/admin/products?page=" + page;

    }



    /** 🔹 Xoá sản phẩm */
    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(required = false) String q,
                                RedirectAttributes redirectAttributes) {
        try {
            // Xóa khỏi RAG AI
            ragIntegrationService.deleteProductFromRag(id);

            // Xóa sản phẩm khỏi DB (Soft delete)
            productService.delete(id);

            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm ID=" + id + " thành công!");
        } catch (Exception e) {
            log.error("[Admin] Lỗi khi xóa sản phẩm ID={}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi xóa sản phẩm: " + e.getMessage());
        }

        if (q != null && !q.isBlank()) {
            return "redirect:/admin/products/search?q=" + java.net.URLEncoder.encode(q, StandardCharsets.UTF_8) + "&page=" + page;
        }
        return "redirect:/admin/products?page=" + page;
    }

    @GetMapping("/search")
    public String listProducts(@RequestParam(required = false) String q,
                               Model model) {

        List<Product> products;

        if (q != null && !q.isBlank()) {
            products = productService.searchByName(q.trim());
            model.addAttribute("q", q); // giữ lại giá trị trong ô tìm kiếm
        } else {
            products = productService.getAllProducts();
        }

        long lowStockCount = productService.countLowStockProducts(5);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("lowStockOnly", false);
        model.addAttribute("products", products);
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", 1);
        model.addAttribute("pageSize", products.size());
        model.addAttribute("brands", brandService.getAllBrands());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("active", "products");
        model.addAttribute("pageTitle", "Quản lý sản phẩm - LaptopStore Admin");

        return "admin/products";
    }

    /** 🔹 Đồng bộ toàn bộ sản phẩm sang RAG AI */
    @GetMapping("/sync-rag")
    public String syncAllProductsToRag(RedirectAttributes redirectAttributes) {
        try {
            List<Product> products = productService.getAllProducts();
            ragIntegrationService.syncAllProducts(products);
            redirectAttributes.addFlashAttribute("successMessage", "Đồng bộ toàn bộ " + products.size() + " sản phẩm sang AI RAG thành công!");
            return "redirect:/admin/products?syncSuccess=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đồng bộ thất bại: " + e.getMessage());
            return "redirect:/admin/products?syncError=true";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    /** 🔹 Đồng bộ sản phẩm đã chọn sang RAG AI */
    @PostMapping("/sync-rag-selected")
    public String syncSelectedProductsToRag(
            @RequestParam(required = false) List<Long> productIds,
            RedirectAttributes redirectAttributes) {

        if (productIds == null || productIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("warningMessage",
                    "Vui lòng chọn ít nhất 1 sản phẩm để đồng bộ.");
            return "redirect:/admin/products";
        }

        int success = 0;
        int failed = 0;
        List<Product> toSync = productService.getProductsByIds(productIds);

        for (Product p : toSync) {
            try {
                ragIntegrationService.syncProduct(p);
                success++;
                log.info("[Admin] Đồng bộ sản phẩm ID={} ({}) sang RAG thành công.", p.getId(), p.getName());
            } catch (Exception e) {
                failed++;
                log.error("[Admin] Lỗi sync sản phẩm ID={}: {}", p.getId(), e.getMessage());
            }
        }

        if (failed == 0) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã đồng bộ thành công " + success + "/" + productIds.size() + " sản phẩm sang AI RAG!");
        } else {
            redirectAttributes.addFlashAttribute("warningMessage",
                    "Đồng bộ " + success + " thành công, " + failed + " thất bại. Xem log để biết chi tiết.");
        }
        return "redirect:/admin/products";
    }

    // ─────────────────────────────────────────────────────────────────────────
    /** 🔹 Import sản phẩm từ file CSV */
    @PostMapping("/import-csv")
    public String importCsv(@RequestParam("csvFile") MultipartFile csvFile,
                             RedirectAttributes redirectAttributes) {
        if (csvFile == null || csvFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn file CSV.");
            return "redirect:/admin/products";
        }

        int added = 0, skipped = 0, errors = 0;
        List<Product> newlyAddedProducts = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine(); // bỏ qua header
            if (headerLine == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "File CSV rỗng.");
                return "redirect:/admin/products";
            }
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) continue;
                try {
                    String[] cols = parseCsvLine(line);
                    if (cols.length < 16) {
                        log.warn("[CSV Import] Dòng {} thiếu cột ({}): {}", lineNum, cols.length, line);
                        errors++;
                        continue;
                    }

                    String rawName = cols[0].trim();
                    if (rawName.startsWith("\uFEFF")) {
                        rawName = rawName.substring(1).trim();
                    }
                    if (rawName.isBlank()) {
                        errors++;
                        continue;
                    }
                    final String name = rawName;
                    String brandName = cols[1].trim();
                    BigDecimal price = new BigDecimal(cols[2].trim().replace(",", ""));
                    int salePct = parseIntSafe(cols[3], 0);
                    String cpu = cols[4].trim();
                    String ram = cols[5].trim();
                    String gpu = cols[6].trim();
                    String storage = cols[7].trim();
                    String screenSize = cols[8].trim();
                    String screenRes = cols[9].trim();
                    // cols[10] = weight (bỏ qua)
                    String battery = cols[11].trim();
                    // cols[12] = OS (bỏ qua vì Product không có field này)
                    String description = cols[13].trim();
                    String images = cols[14].trim();
                    int stock = parseIntSafe(cols[15], 0);

                    // Bỏ qua nếu trùng tên (kiểm tra đơn giản)
                    List<Product> existing = productService.searchByName(name);
                    boolean exactMatch = existing.stream()
                            .anyMatch(p -> p.getName().equalsIgnoreCase(name));
                    if (exactMatch) {
                        log.info("[CSV Import] Dòng {} - Bỏ qua (đã tồn tại): {}", lineNum, name);
                        skipped++;
                        continue;
                    }

                    // Lấy hoặc tạo Brand
                    Brand brand = brandService.getAllBrands().stream()
                            .filter(b -> b.getName().equalsIgnoreCase(brandName))
                            .findFirst()
                            .orElseGet(() -> {
                                Brand newBrand = new Brand();
                                newBrand.setName(brandName);
                                return brandService.save(newBrand);
                            });

                    // Tạo Product
                    Product p = new Product();
                    String generatedCode = generateModel(name) + "_" + lineNum;
                    p.setProductCode(generatedCode);
                    p.setName(name);
                    p.setModel(generatedCode);
                    p.setPrice(price);
                    p.setSalePercent(salePct);
                    p.setCpu(cpu);
                    p.setRam(ram);
                    p.setGpu(gpu);
                    p.setDisplay(screenSize + (screenRes.isBlank() ? "" : " " + screenRes));
                    p.setBattery(battery);
                    p.setDescription(description);
                    p.setImages(images);
                    p.setStock(stock);
                    p.setBrand(brand);
                    productService.save(p);

                    // Tạo ProductVariant mặc định
                    ProductVariant variant = new ProductVariant();
                    variant.setProduct(p);
                    variant.setColor("Mặc định");
                    variant.setStorage(storage.isBlank() ? "256GB" : storage);
                    variant.setStock(stock);
                    variantService.save(variant);

                    // Cập nhật tổng stock từ variant
                    p.updateTotalStock();
                    productService.save(p);

                    // Gom sản phẩm để đồng bộ bulk
                    Product freshProduct = productService.getProductById(p.getId()).orElse(p);
                    newlyAddedProducts.add(freshProduct);

                    added++;
                    log.info("[CSV Import] Dòng {} - Thêm mới thành công: {}", lineNum, name);

                } catch (Exception e) {
                    errors++;
                    log.error("[CSV Import] Lỗi dòng {}: {} | {}", lineNum, e.getMessage(), line);
                }
            }
        } catch (IOException e) {
            log.error("[CSV Import] Lỗi đọc file: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đọc file CSV: " + e.getMessage());
            return "redirect:/admin/products";
        }

        // Đồng bộ bất đồng bộ sang RAG hàng loạt để tránh treo UI
        if (!newlyAddedProducts.isEmpty()) {
            List<Product> finalProductsToSync = new ArrayList<>(newlyAddedProducts);
            new Thread(() -> {
                try {
                    log.info("[CSV Import] Bắt đầu đồng bộ bất đồng bộ {} sản phẩm mới lên RAG...", finalProductsToSync.size());
                    ragIntegrationService.syncAllProducts(finalProductsToSync);
                    log.info("[CSV Import] Đồng bộ bất đồng bộ thành công {} sản phẩm mới lên RAG.", finalProductsToSync.size());
                } catch (Exception e) {
                    log.error("[CSV Import] Lỗi đồng bộ bất đồng bộ lên RAG: {}", e.getMessage());
                }
            }).start();
        }

        redirectAttributes.addFlashAttribute("successMessage",
                String.format("Import CSV hoàn tất: %d thêm mới (đang đồng bộ RAG ngầm), %d bỏ qua (trùng), %d lỗi.",
                        added, skipped, errors));
        return "redirect:/admin/products";
    }

    /** Parse 1 dòng CSV an toàn với dấu ngoặc kép */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }

    private int parseIntSafe(String s, int defaultVal) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return defaultVal; }
    }

    /** Tạo model ngắn từ tên sản phẩm (dùng để tránh trùng unique constraint) */
    private String generateModel(String name) {
        String base = name.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (base.length() > 20) base = base.substring(0, 20);
        return base + "_" + System.currentTimeMillis() % 100000;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private String saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String uploadDir = new ClassPathResource("static/images/products/").getFile().getAbsolutePath();
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        File dest = new File(uploadDir + File.separator + fileName);

        file.transferTo(dest);
        return "/images/products/" + fileName;
    }

    @PostMapping("/bulk-toggle")
    @ResponseBody
    public java.util.Map<String, Object> bulkToggleStatus(@RequestBody java.util.Map<String, Object> body) {
        List<Long> ids = ((List<?>) body.get("ids")).stream()
                .map(o -> Long.parseLong(o.toString())).collect(java.util.stream.Collectors.toList());
        int updated = 0;
        for (Long id : ids) {
            try {
                productService.toggleActive(id);
                updated++;
            } catch (Exception ignored) {}
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("updated", updated);
        result.put("total", ids.size());
        return result;
    }

    @PostMapping("/bulk-delete")
    @ResponseBody
    public java.util.Map<String, Object> bulkDeleteProducts(@RequestBody java.util.Map<String, Object> body) {
        List<Long> ids = ((List<?>) body.get("ids")).stream()
                .map(o -> Long.parseLong(o.toString())).collect(java.util.stream.Collectors.toList());
        int updated = 0;
        for (Long id : ids) {
            try {
                productService.delete(id);
                updated++;
            } catch (Exception ignored) {}
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("updated", updated);
        result.put("deleted", updated);
        result.put("total", ids.size());
        return result;
    }

}
