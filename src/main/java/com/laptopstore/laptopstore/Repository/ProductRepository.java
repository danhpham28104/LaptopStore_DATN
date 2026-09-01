package com.laptopstore.laptopstore.Repository;

import com.laptopstore.laptopstore.dto.BestSellerDTO;
import com.laptopstore.laptopstore.entity.Product;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByBrand_Id(Long id);

    List<Product> findByBrand_NameContainingIgnoreCase(String brandName);

    List<Product> findByNameContainingIgnoreCase(String name);

    Optional<Product> findByModel(String model);

    @Query(value = "SELECT COUNT(*) FROM product WHERE LOWER(model) = LOWER(:model) AND (:excludeId IS NULL OR id != :excludeId)", nativeQuery = true)
    long countByModelIgnoreCaseExcludingId(@Param("model") String model, @Param("excludeId") Long excludeId);

    @Query(value = "SELECT * FROM product WHERE LOWER(model) = LOWER(:model) OR LOWER(model) LIKE LOWER(CONCAT(:model, '_deleted_%')) ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Optional<Product> findAnyByModelIncludingDeleted(@Param("model") String model);

    @Query("""
    SELECT DISTINCT p FROM Product p
    LEFT JOIN p.brand b
    LEFT JOIN p.variants v
    WHERE (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')))
      AND (:brand IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :brand, '%')))
      AND (:ram IS NULL OR LOWER(p.ram) = LOWER(:ram))
      AND (:cpu IS NULL OR LOWER(p.cpu) LIKE LOWER(CONCAT('%', :cpu, '%')))
      AND (:color IS NULL OR LOWER(v.color) LIKE LOWER(CONCAT('%', :color, '%')))
      AND (:storage IS NULL OR LOWER(v.storage) = LOWER(:storage))
      AND (:minPrice IS NULL OR p.price >= :minPrice)
      AND (:maxPrice IS NULL OR p.price <= :maxPrice)
    """)
    List<Product> searchAdvanced(
            @Param("q") String q,
            @Param("brand") String brand,
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
