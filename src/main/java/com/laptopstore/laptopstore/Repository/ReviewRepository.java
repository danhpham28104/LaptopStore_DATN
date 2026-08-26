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
}
