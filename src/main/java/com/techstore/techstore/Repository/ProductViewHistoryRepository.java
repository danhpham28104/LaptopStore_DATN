package com.techstore.techstore.Repository;

import com.techstore.techstore.entity.ProductViewHistory;
import com.techstore.techstore.entity.User;
import com.techstore.techstore.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductViewHistoryRepository extends JpaRepository<ProductViewHistory, Long> {

    Optional<ProductViewHistory> findByUserAndProduct(User user, Product product);

    @Query("SELECT pvh FROM ProductViewHistory pvh WHERE pvh.user.id = :userId ORDER BY pvh.viewCount DESC, pvh.lastViewedAt DESC")
    List<ProductViewHistory> findTopViewedByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT pvh FROM ProductViewHistory pvh WHERE pvh.user.id = :userId ORDER BY pvh.lastViewedAt DESC")
    List<ProductViewHistory> findRecentlyViewedByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT SUM(pvh.viewCount) FROM ProductViewHistory pvh")
    Optional<Long> sumTotalViews();

    @Query("SELECT COUNT(DISTINCT pvh.product.id) FROM ProductViewHistory pvh WHERE pvh.product.isDeleted = false")
    long countDistinctViewedProducts();

    @Query("""
        SELECT pvh.product, SUM(pvh.viewCount), MAX(pvh.lastViewedAt)
        FROM ProductViewHistory pvh
        WHERE pvh.product.isDeleted = false
        GROUP BY pvh.product
        ORDER BY SUM(pvh.viewCount) DESC
    """)
    List<Object[]> findTopViewedProductsGrouped();

    @Query("""
        SELECT b.name, SUM(pvh.viewCount)
        FROM ProductViewHistory pvh
        JOIN pvh.product p
        JOIN p.brand b
        WHERE p.isDeleted = false
        GROUP BY b.name
        ORDER BY SUM(pvh.viewCount) DESC
    """)
    List<Object[]> findBrandViewAnalyticsGrouped();
}
