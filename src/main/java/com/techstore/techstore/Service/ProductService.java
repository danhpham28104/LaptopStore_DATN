package com.techstore.techstore.Service;

import com.techstore.techstore.Repository.ProductRepository;
import com.techstore.techstore.dto.BestSellerDTO;
import com.techstore.techstore.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // Lấy toàn bộ sản phẩm
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /** Tổng tồn kho toàn hệ thống */
    public long sumTotalStock() {
        return productRepository.sumTotalStock();
    }

    /** Lấy top N sản phẩm bán chạy */
    public List<BestSellerDTO> getTopBestSellers(int limit) {
        return productRepository.getTopBestSellers(limit);
    }


    // Lấy sản phẩm theo ID
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    // Tìm theo tên
    @Transactional(readOnly = true)
    public List<Product> searchByName(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword);
    }

    // Phân trang
    @Transactional(readOnly = true)
    public Page<Product> getPaginatedProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findAll(pageable);
    }

    // Theo thương hiệu
    @Transactional(readOnly = true)
    public List<Product> searchByBrandName(String brandName) {
        return productRepository.findByBrand_NameContainingIgnoreCase(brandName);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByBrandId(Long brandId) {
        return productRepository.findByBrand_Id(brandId);
    }

    // Lưu hoặc cập nhật sản phẩm
    @Transactional
    public Product save(Product product) {
        if (product.getStock() == null) product.setStock(0);
        return productRepository.save(product);
    }

    // Xóa sản phẩm
    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    // Tìm kiếm nâng cao (Laptop)
    public List<Product> advancedSearch(String q, String brand, String ram, String cpu,
                                        String storage, Double minPrice, Double maxPrice) {

        String keyword = (q != null && !q.isBlank()) ? q.trim() : null;
        String brandName = (brand != null && !brand.isBlank()) ? brand.trim() : null;
        String ramValue = (ram != null && !ram.isBlank()) ? ram.trim() : null;
        String cpuValue = (cpu != null && !cpu.isBlank()) ? cpu.trim() : null;
        String storageValue = (storage != null && !storage.isBlank()) ? storage.trim() : null;

        // color có thể để null nếu chưa cần lọc theo màu
        return productRepository.searchAdvanced(keyword, brandName, ramValue, cpuValue, null, storageValue, minPrice, maxPrice);
    }

    @Transactional(readOnly = true)
    public List<Product> sortProducts(String sort) {
        List<Product> products = productRepository.findAll();

        if (sort == null || sort.equals("default")) {
            return products;
        }

        switch (sort) {

            case "priceAsc":
                products.sort((a, b) -> {
                    if (a.getPrice() == null || b.getPrice() == null) return 0;
                    return a.getPrice().compareTo(b.getPrice());
                });
                break;

            case "priceDesc":
                products.sort((a, b) -> {
                    if (a.getPrice() == null || b.getPrice() == null) return 0;
                    return b.getPrice().compareTo(a.getPrice());
                });
                break;

            case "newest":
                products.sort((a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
                break;

            case "hot":
                products.sort((a, b) -> {
                    boolean aHot = a.getBadge() != null && a.getBadge().equalsIgnoreCase("HOT");
                    boolean bHot = b.getBadge() != null && b.getBadge().equalsIgnoreCase("HOT");

                    if (aHot && !bHot) return -1;
                    if (!aHot && bHot) return 1;
                    return 0;
                });
                break;
        }

        return products;
    }




}
