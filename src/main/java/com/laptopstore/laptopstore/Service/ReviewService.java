package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.OrderItemRepository;
import com.laptopstore.laptopstore.Repository.ProductRepository;
import com.laptopstore.laptopstore.Repository.ReviewRepository;
import com.laptopstore.laptopstore.dto.ReviewRequestDto;
import com.laptopstore.laptopstore.entity.OrderItem;
import com.laptopstore.laptopstore.entity.Product;
import com.laptopstore.laptopstore.entity.Review;
import com.laptopstore.laptopstore.entity.User;
import com.laptopstore.laptopstore.enums.ReviewStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    public List<Review> getApprovedReviewsByProduct(Long productId) {
        return reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(productId, ReviewStatus.APPROVED);
    }

    public Double getAverageRating(Long productId) {
        Double avg = reviewRepository.findAverageRatingByProductIdAndStatus(productId, ReviewStatus.APPROVED);
        if (avg == null) {
            return 5.0; // Mặc định 5.0 nếu chưa có đánh giá
        }
        return Math.round(avg * 10.0) / 10.0;
    }

    public Long getReviewCount(Long productId) {
        return reviewRepository.countByProductIdAndStatus(productId, ReviewStatus.APPROVED);
    }

    public boolean canUserReviewProduct(User user, Long productId, Long orderItemId) {
        if (user == null) return false;

        if (orderItemId != null) {
            boolean alreadyReviewed = reviewRepository.existsByUserIdAndOrderItemId(user.getId(), orderItemId);
            if (alreadyReviewed) return false;

            return orderItemRepository.findDeliveredOrderItem(user.getId(), orderItemId).isPresent();
        }

        return orderItemRepository.existsByUserIdAndProductIdAndDeliveredOrder(user.getId(), productId);
    }

    @Transactional
    public Review addReview(User user, ReviewRequestDto dto) {
        if (user == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập để đánh giá sản phẩm!");
        }

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm!"));

        OrderItem orderItem = null;
        if (dto.getOrderItemId() != null) {
            orderItem = orderItemRepository.findDeliveredOrderItem(user.getId(), dto.getOrderItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Đơn hàng chưa giao thành công hoặc không hợp lệ!"));

            if (reviewRepository.existsByUserIdAndOrderItemId(user.getId(), dto.getOrderItemId())) {
                throw new IllegalArgumentException("Bạn đã gửi đánh giá cho sản phẩm trong đơn hàng này rồi!");
            }
        } else {
            if (!orderItemRepository.existsByUserIdAndProductIdAndDeliveredOrder(user.getId(), product.getId())) {
                throw new IllegalArgumentException("Chỉ khách hàng đã mua và nhận sản phẩm thành công mới có thể gửi đánh giá!");
            }
        }

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setOrderItem(orderItem);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review.setImages(dto.getImages());
        review.setStatus(ReviewStatus.APPROVED); // Mặc định duyệt tự động, Admin có thể ẩn/từ chối sau

        return reviewRepository.save(review);
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Review updateReviewStatus(Long reviewId, ReviewStatus status) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá!"));
        review.setStatus(status);
        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        reviewRepository.deleteById(reviewId);
    }
}
