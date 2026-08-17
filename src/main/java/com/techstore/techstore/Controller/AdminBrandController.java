package com.techstore.techstore.Controller;

import com.techstore.techstore.Service.BrandService;
import com.techstore.techstore.Repository.BrandRepository;
import com.techstore.techstore.Service.OrderService;
import com.techstore.techstore.entity.Brand;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Controller
    @RequestMapping("/admin/brands")
public class AdminBrandController {

    @Autowired
    private BrandService brandService;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private OrderService orderService;

    private final String UPLOAD_DIR = "uploads/brands/";

    @GetMapping
    public String listBrands(Model model) {
        model.addAttribute("brands", brandService.getAllBrands());
        model.addAttribute("active", "brands");
        model.addAttribute("pageTitle", "Quản lý Thương hiệu - LaptopStore Admin");
        return "admin/brands";
    }


    @PostMapping("/add")
    public String addBrand(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile logoFile
    ) throws IOException {
        if (brandRepository.existsByName(name)) {
            return "redirect:/admin/brands?error=exists";
        }

        Brand brand = new Brand();
        brand.setName(name);
        brand.setDescription(description);

        brand.setLogo(saveImage(logoFile));

        brandService.save(brand);
        return "redirect:/admin/brands?success=added";
    }

    /**
     *   SỬA BRAND
     *   */
    @PostMapping("/edit/{id}")
    public String editBrand(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile logoFile,
            @RequestParam(defaultValue = "true") boolean active

    ) throws IOException{
        Brand brand = brandService.getById(id).orElse(null);
        if (brand == null) return "redirect:/admin/brands?error=notfound";

        if (!brand.getName().equals(name) && brandRepository.existsByName(name)) {
            return "redirect:/admin/brands?error=exists";
        }

        brand.setName(name);
        brand.setDescription(description);
        brand.setActive(active);

        // nếu upload logo mới thì thay luôn
        if(logoFile != null ){
            brand.setLogo(saveImage(logoFile));
        }


        brandService.save(brand);

        return "redirect:/admin/brands?success=updated";
    }

    /** 
     *  XÓA BRAND
     *   */
    @PostMapping("/delete/{id}")
    public String deleteBrand(@PathVariable Long id) {
        brandService.delete(id);
        return "redirect:/admin/brands?success=deleted";
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
