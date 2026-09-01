package com.laptopstore.laptopstore.Controller;

import com.laptopstore.laptopstore.Repository.CategoryRepository;
import com.laptopstore.laptopstore.Repository.ProductRepository;
import com.laptopstore.laptopstore.Service.CategoryService;
import com.laptopstore.laptopstore.entity.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping
    public String listCategories(Model model) {
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("active", "categories");
        model.addAttribute("pageTitle", "Quản lý Danh mục - LaptopStore Admin");
        return "admin/categories";
    }

    @PostMapping("/add")
    public String addCategory(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) String slug
    ) {
        if (categoryRepository.existsByName(name)) {
            return "redirect:/admin/categories?error=exists";
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setIcon(icon != null && !icon.isBlank() ? icon : "bi-grid");

        if (slug == null || slug.isBlank()) {
            slug = categoryService.generateSlug(name);
        }
        category.setSlug(slug);

        categoryService.save(category);
        return "redirect:/admin/categories?success=added";
    }

    @PostMapping("/edit/{id}")
    public String editCategory(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String icon,
            @RequestParam(required = false) String slug,
            @RequestParam(defaultValue = "true") boolean active
    ) {
        Category category = categoryService.getById(id).orElse(null);
        if (category == null) return "redirect:/admin/categories?error=notfound";

        if (!category.getName().equals(name) && categoryRepository.existsByName(name)) {
            return "redirect:/admin/categories?error=exists";
        }

        category.setName(name);
        category.setDescription(description);
        if (icon != null && !icon.isBlank()) {
            category.setIcon(icon);
        }
        if (slug == null || slug.isBlank()) {
            slug = categoryService.generateSlug(name);
        }
        category.setSlug(slug);
        category.setActive(active);

        categoryService.save(category);
        return "redirect:/admin/categories?success=updated";
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        Category category = categoryService.getById(id).orElse(null);
        if (category == null) return "redirect:/admin/categories?error=notfound";

        long productCount = productRepository.countByCategory_Id(id);
        if (productCount > 0 || (category.getProducts() != null && !category.getProducts().isEmpty())) {
            return "redirect:/admin/categories?error=has_products";
        }

        categoryService.delete(id);
        return "redirect:/admin/categories?success=deleted";
    }
}
