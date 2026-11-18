package com.laptopstore.laptopstore.Repository;

import com.laptopstore.laptopstore.entity.Brand;
import com.laptopstore.laptopstore.dto.BestSellerDTO;
import com.laptopstore.laptopstore.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByBrandAndIsDeletedFalse(Brand brand, Pageable pageable);

    List<Product> findByBrand_Id(Long id);

    List<Product> findByBrand_NameContainingIgnoreCase(String brandName);

    long countByCategory_Id(Long categoryId);

    List<Product> findByCategory_SlugAndIsDeletedFalse(String categorySlug);

    Page<Product> findByCategory_SlugAndIsDeletedFalse(String categorySlug, Pageable pageable);

    List<Product> findByNameContainingIgnoreCase(String name);

    Optional<Product> findFirstByModel(String model);

    Optional<Product> findByProductCodeIgnoreCase(String productCode);

    @Query(value = "SELECT COUNT(*) FROM product WHERE LOWER(product_code) = LOWER(:productCode) AND is_deleted = false AND (:excludeId IS NULL OR id != :excludeId)", nativeQuery = true)
    long countByProductCodeIgnoreCaseExcludingId(@Param("productCode") String productCode, @Param("excludeId") Long excludeId);

    @Modifying
    @Query(value = "UPDATE product SET product_code = CONCAT(product_code, '_deleted_', :ts) WHERE LOWER(product_code) = LOWER(:productCode) AND is_deleted = true", nativeQuery = true)
    void releaseDeletedProductCode(@Param("productCode") String productCode, @Param("ts") long ts);

    @Query(value = "SELECT * FROM product WHERE LOWER(product_code) = LOWER(:productCode) OR LOWER(product_code) LIKE LOWER(CONCAT(:productCode, '_deleted_%')) ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Optional<Product> findAnyByProductCodeIncludingDeleted(@Param("productCode") String productCode);

    @Query(value = "SELECT COUNT(*) FROM product WHERE LOWER(model) = LOWER(:model) AND (:excludeId IS NULL OR id != :excludeId)", nativeQuery = true)
    long countByModelIgnoreCaseExcludingId(@Param("model") String model, @Param("excludeId") Long excludeId);

    @Query(value = "SELECT * FROM product WHERE LOWER(model) = LOWER(:model) OR LOWER(model) LIKE LOWER(CONCAT(:model, '_deleted_%')) ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Optional<Product> findAnyByModelIncludingDeleted(@Param("model") String model);

    @Query("""
    SELECT DISTINCT p FROM Product p
    LEFT JOIN p.brand b
    LEFT JOIN p.category c
    WHERE (:q IS NULL OR :q = ''
      OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
      OR (p.productCode IS NOT NULL AND LOWER(p.productCode) LIKE LOWER(CONCAT('%', :q, '%')))
      OR LOWER(p.model) LIKE LOWER(CONCAT('%', :q, '%'))
      OR (b.name IS NOT NULL AND LOWER(b.name) LIKE LOWER(CONCAT('%', :q, '%')))
      OR (c.name IS NOT NULL AND LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))
      OR (p.cpu IS NOT NULL AND LOWER(p.cpu) LIKE LOWER(CONCAT('%', :q, '%')))
      OR (p.gpu IS NOT NULL AND LOWER(p.gpu) LIKE LOWER(CONCAT('%', :q, '%')))
      OR (p.ram IS NOT NULL AND LOWER(p.ram) LIKE LOWER(CONCAT('%', :q, '%')))
      OR CAST(p.id AS string) LIKE CONCAT('%', :q, '%'))
    """)
    List<Product> searchByKeyword(@Param("q") String q);

    @Query("""
    SELECT DISTINCT p FROM Product p
    LEFT JOIN p.brand b
    LEFT JOIN p.category c
    LEFT JOIN p.variants v
    WHERE (:q IS NULL OR :q = ''
      OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
      OR (p.productCode IS NOT NULL AND LOWER(p.productCode) LIKE LOWER(CONCAT('%', :q, '%')))
      OR LOWER(p.model) LIKE LOWER(CONCAT('%', :q, '%'))
      OR (b.name IS NOT NULL AND LOWER(b.name) LIKE LOWER(CONCAT('%', :q, '%')))
      OR (c.name IS NOT NULL AND LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))
      OR (p.cpu IS NOT NULL AND LOWER(p.cpu) LIKE LOWER(CONCAT('%', :q, '%')))
      OR (p.gpu IS NOT NULL AND LOWER(p.gpu) LIKE LOWER(CONCAT('%', :q, '%')))
      OR (p.ram IS NOT NULL AND LOWER(p.ram) LIKE LOWER(CONCAT('%', :q, '%')))
      OR CAST(p.id AS string) LIKE CONCAT('%', :q, '%'))
      AND (:brand IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :brand, '%')))
      AND (:categorySlug IS NULL OR LOWER(c.slug) = LOWER(:categorySlug))
      AND (:ram IS NULL OR (p.ram IS NOT NULL AND LOWER(p.ram) LIKE LOWER(CONCAT('%', :ram, '%'))))
      AND (:cpu IS NULL OR (p.cpu IS NOT NULL AND LOWER(p.cpu) LIKE LOWER(CONCAT('%', :cpu, '%'))))
      AND (:color IS NULL OR (v.color IS NOT NULL AND LOWER(v.color) LIKE LOWER(CONCAT('%', :color, '%'))))
      AND (:storage IS NULL OR (v.storage IS NOT NULL AND LOWER(v.storage) LIKE LOWER(CONCAT('%', :storage, '%'))))
      AND (:minPrice IS NULL OR p.price >= :minPrice)
      AND (:maxPrice IS NULL OR p.price <= :maxPrice)
    """)
    List<Product> searchAdvanced(
            @Param("q") String q,
            @Param("brand") String brand,
            @Param("categorySlug") String categorySlug,
            @Param("ram") String ram,
            @Param("cpu") String cpu,
            @Param("color") String color,
            @Param("storage") String storage,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice
    );

    @Query("SELECT SUM(p.stock) FROM Product p")
    Long sumTotalStock();

    @Query("""
        SELECT new com.laptopstore.laptopstore.dto.BestSellerDTO(
            oi.product.name,
            SUM(oi.quantity)
        )
        FROM OrderItem oi
        GROUP BY oi.product.id
        ORDER BY SUM(oi.quantity) DESC
        LIMIT :limit
    """)
    List<BestSellerDTO> getTopBestSellers(@Param("limit") int limit);

    @Query("""
    SELECT DISTINCT p FROM Product p
    WHERE p.isDeleted = false
      AND p.id NOT IN :excludeIds
      AND (
        p.brand.id IN :brandIds
        OR (p.price BETWEEN :minPrice AND :maxPrice)
        OR (:cpuKeyword IS NOT NULL AND LOWER(p.cpu) LIKE LOWER(CONCAT('%', :cpuKeyword, '%')))
      )
    """)
    List<Product> findSimilarProducts(
            @Param("excludeIds") List<Long> excludeIds,
            @Param("brandIds") List<Long> brandIds,
            @Param("minPrice") java.math.BigDecimal minPrice,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            @Param("cpuKeyword") String cpuKeyword
    );

    @Query("""
    SELECT DISTINCT p FROM Product p
    LEFT JOIN p.variants v
    WHERE p.isDeleted = false
      AND (p.stock <= :threshold OR v.stock <= :threshold)
    """)
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    @Query("""
    SELECT COUNT(DISTINCT p) FROM Product p
    LEFT JOIN p.variants v
    WHERE p.isDeleted = false
      AND (p.stock <= :threshold OR v.stock <= :threshold)
    """)
    long countLowStockProducts(@Param("threshold") int threshold);
}
