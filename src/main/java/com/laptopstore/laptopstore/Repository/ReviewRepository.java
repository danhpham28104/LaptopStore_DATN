package com.laptopstore.laptopstore.Repository;

import com.laptopstore.laptopstore.entity.Review;
import com.laptopstore.laptopstore.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, ReviewStatus status);

    Page<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status, Pageable pageable);

    List<Review> findAllByOrderByCreatedAtDesc();

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.status = :status")
    Double findAverageRatingByProductIdAndStatus(@Param("productId") Long productId, @Param("status") ReviewStatus status);

    Long countByProductIdAndStatus(Long productId, ReviewStatus status);

    boolean existsByUserIdAndOrderItemId(Long userId, Long orderItemId);

    Optional<Review> findByUserIdAndOrderItemId(Long userId, Long orderItemId);

    // Đếm review theo từng mức sao
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.status = :status GROUP BY r.rating")
    List<Object[]> countByProductIdAndStatusGroupByRating(@Param("productId") Long productId, @Param("status") ReviewStatus status);

    // Lấy review theo rating cụ thể
    List<Review> findByProductIdAndStatusAndRatingOrderByCreatedAtDesc(Long productId, ReviewStatus status, Integer rating);

    // Lấy review có rating <= rating (dành cho nhóm <=2 sao)
    List<Review> findByProductIdAndStatusAndRatingLessThanEqualOrderByCreatedAtDesc(Long productId, ReviewStatus status, Integer rating);
}
