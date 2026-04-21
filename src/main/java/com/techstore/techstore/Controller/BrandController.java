package com.techstore.techstore.Controller;

import com.techstore.techstore.Service.BrandService;
import com.techstore.techstore.entity.Brand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/brand")
public class BrandController {

    @Autowired
    private BrandService brandService;

    @GetMapping
    public List<Brand> getAllBrands() {
        return brandService.getAllBrands();
    }




    @GetMapping("/{id}")
    public Optional<Brand> getBrandById(@PathVariable Long id) {
        return brandService.getById(id);
    }

    @PostMapping
    public Brand createBrand(@RequestBody Brand brand) {
        return brandService.save(brand);
    }

    @PutMapping("/{id}")
    public Brand updateBrand(@PathVariable Long id, @RequestBody Brand updated) {
        return brandService.getById(id)
                .map(b -> {
                    b.setName(updated.getName());
                    return brandService.save(b);
                })
                .orElseThrow(() -> new RuntimeException("Brand not found"));
    }

    @DeleteMapping("/{id}")
    public void deleteBrand(@PathVariable Long id) {
        brandService.delete(id);
    }
}
