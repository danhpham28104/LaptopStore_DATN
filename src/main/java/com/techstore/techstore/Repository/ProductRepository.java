package com.techstore.techstore.Repository;

import com.techstore.techstore.dto.BestSellerDTO;
import com.techstore.techstore.entity.Product;
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

    @Query("""
    SELECT DISTINCT p FROM Product p
    LEFT JOIN p.brand b
    LEFT JOIN p.variants v
    WHERE (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')))
      AND (:brand IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :brand, '%')))
      AND (:ram IS NULL OR LOWER(p.ram) = LOWER(:ram))
      AND (:cpu IS NULL OR LOWER(p.cpu) LIKE LOWER(CONCAT('%', :cpu, '%')))
      AND (:color IS NULL OR LOWER(v.color) = LOWER(:color))
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
        SELECT new com.techstore.techstore.dto.BestSellerDTO(
            oi.product.name,
            SUM(oi.quantity)
        )
        FROM OrderItem oi
        GROUP BY oi.product.id
        ORDER BY SUM(oi.quantity) DESC
        LIMIT :limit
    """)
    List<BestSellerDTO> getTopBestSellers(@Param("limit") int limit);





}
