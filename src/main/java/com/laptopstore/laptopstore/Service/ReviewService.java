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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    public List<Review> getApprovedReviewsByProduct(Long productId) {
        List<Review> raw = reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(productId, ReviewStatus.APPROVED);
        return deduplicateReviews(raw);
    }

    public List<Review> getApprovedReviewsByProductAndRating(Long productId, Integer rating) {
        List<Review> raw;
        if (rating == null) {
            raw = reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(productId, ReviewStatus.APPROVED);
        } else if (rating <= 2) {
            raw = reviewRepository.findByProductIdAndStatusAndRatingLessThanEqualOrderByCreatedAtDesc(
                    productId, ReviewStatus.APPROVED, 2);
        } else {
            raw = reviewRepository.findByProductIdAndStatusAndRatingOrderByCreatedAtDesc(
                    productId, ReviewStatus.APPROVED, rating);
        }
        return deduplicateReviews(raw);
    }

    public Double getAverageRating(Long productId) {
        Double avg = reviewRepository.findAverageRatingByProductIdAndStatus(productId, ReviewStatus.APPROVED);
        if (avg == null) {
            return null; // null = chưa có đánh giá
        }
        return Math.round(avg * 10.0) / 10.0;
    }

    public Long getReviewCount(Long productId) {
        return reviewRepository.countByProductIdAndStatus(productId, ReviewStatus.APPROVED);
    }

    private void recalculateProductRating(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        recalculateProductRating(product);
    }

    private void recalculateProductRating(Product product) {
        Double avg = reviewRepository.findAverageRatingByProductIdAndStatus(product.getId(), ReviewStatus.APPROVED);
        Long count = reviewRepository.countByProductIdAndStatus(product.getId(), ReviewStatus.APPROVED);

        Double roundedAvg = (avg != null) ? Math.round(avg * 10.0) / 10.0 : null;
        int reviewCountVal = (count != null) ? count.intValue() : 0;

        product.setAverageRating(roundedAvg);
        product.setReviewCount(reviewCountVal);
        productRepository.save(product);
    }

    /**
     * Lấy danh sách OrderItem đã giao thành công / hoàn thành của user đối với sản phẩm.
     */
    @Transactional(readOnly = true)
    public List<OrderItem> getDeliveredOrderItemsForProduct(User user, Long productId) {
        if (user == null || user.getId() == null || productId == null) {
            return List.of();
        }
        List<OrderItem> items = orderItemRepository.findDeliveredOrderItemsByUserIdAndProductId(user.getId(), productId);
        return items != null ? items : List.of();
    }

    /**
     * Cho phép người dùng đã mua & nhận hàng thành công đánh giá mới hoặc đánh giá lại (cập nhật đánh giá hiện có).
     */
    @Transactional(readOnly = true)
    public boolean canUserReviewProduct(User user, Long productId, Long orderItemId) {
        if (user == null || user.getId() == null || productId == null) {
            return false;
        }

        if (orderItemId != null) {
            return orderItemRepository.findDeliveredOrderItem(user.getId(), orderItemId)
                    .filter(oi -> oi.getProduct() != null && Objects.equals(oi.getProduct().getId(), productId))
                    .isPresent();
        }

        List<OrderItem> deliveredItems = orderItemRepository.findDeliveredOrderItemsByUserIdAndProductId(user.getId(), productId);
        if (deliveredItems != null && !deliveredItems.isEmpty()) {
            return true;
        }
        return orderItemRepository.existsByUserIdAndProductIdAndDeliveredOrder(user.getId(), productId);
    }

    /**
     * Tìm OrderItem mặc định khi mở trang sản phẩm: ưu tiên đơn hàng chưa đánh giá, nếu tất cả đã đánh giá thì lấy đơn gần nhất.
     */
    @Transactional(readOnly = true)
    public Long resolveDefaultOrderItemId(User user, Long productId, Long requestedOrderItemId) {
        if (user == null || user.getId() == null || productId == null) {
            return null;
        }
        if (requestedOrderItemId != null) {
            Optional<OrderItem> requested = orderItemRepository.findDeliveredOrderItem(user.getId(), requestedOrderItemId);
            if (requested.isPresent()
                    && requested.get().getProduct() != null
                    && Objects.equals(requested.get().getProduct().getId(), productId)) {
                return requested.get().getId();
            }
        }
        List<OrderItem> deliveredItems = getDeliveredOrderItemsForProduct(user, productId);
        if (deliveredItems.isEmpty()) {
            return null;
        }
        for (OrderItem oi : deliveredItems) {
            if (!reviewRepository.existsByUserIdAndProductIdAndOrderItemId(user.getId(), productId, oi.getId())) {
                return oi.getId();
            }
        }
        return deliveredItems.get(0).getId();
    }

    /**
     * Lấy review hiện tại của user cho sản phẩm / orderItem để hiển thị sẵn khi đánh giá lại.
     */
    @Transactional(readOnly = true)
    public Optional<Review> getUserReviewForProduct(User user, Long productId, Long orderItemId) {
        if (user == null || user.getId() == null || productId == null) {
            return Optional.empty();
        }
        if (orderItemId != null) {
            Optional<Review> exact = reviewRepository.findByUserIdAndProductIdAndOrderItemId(user.getId(), productId, orderItemId);
            if (exact.isPresent()) {
                return exact;
            }
            Optional<Review> byOrderItem = reviewRepository.findByUserIdAndOrderItemId(user.getId(), orderItemId);
            if (byOrderItem.isPresent()) {
                return byOrderItem;
            }
            return Optional.empty();
        }

        List<OrderItem> deliveredItems = getDeliveredOrderItemsForProduct(user, productId);
        if (!deliveredItems.isEmpty()) {
            Long defaultOrderItemId = resolveDefaultOrderItemId(user, productId, null);
            if (defaultOrderItemId != null) {
                Optional<Review> byDefault = reviewRepository.findByUserIdAndProductIdAndOrderItemId(
                        user.getId(), productId, defaultOrderItemId);
                if (byDefault.isPresent()) {
                    return byDefault;
                }
                Optional<Review> byOrderItem = reviewRepository.findByUserIdAndOrderItemId(user.getId(), defaultOrderItemId);
                if (byOrderItem.isPresent()) {
                    return byOrderItem;
                }
            }
        }
        return reviewRepository.findFirstByUserIdAndProductIdAndOrderItemIsNullOrderByIdDesc(user.getId(), productId);
    }

    /**
     * Tạo mới hoặc cập nhật Review duy nhất theo khóa (user_id, product_id, order_item_id).
     * Nếu người dùng đánh giá lại trên cùng sản phẩm trong cùng đơn hàng: giữ nguyên ID review,
     * cập nhật rating, comment, images, updatedAt và tính lại thống kê sản phẩm.
     */
    @Transactional
    public Review addReview(User user, ReviewRequestDto dto) {
        if (user == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập để đánh giá sản phẩm!");
        }
        if (dto == null || dto.getProductId() == null) {
            throw new IllegalArgumentException("Thiếu thông tin sản phẩm đánh giá!");
        }

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm!"));

        OrderItem orderItem = resolveDeliveredOrderItemForSubmit(user, product, dto.getOrderItemId());
        Optional<Review> existingOpt = findExistingReview(user.getId(), product.getId(), orderItem);

        LocalDateTime now = LocalDateTime.now();
        Review review;
        if (existingOpt.isPresent()) {
            review = existingOpt.get();
            if (review.getOrderItem() == null && orderItem != null) {
                review.setOrderItem(orderItem);
            }
            review.setRating(dto.getRating());
            review.setComment(dto.getComment());
            if (dto.getImages() != null) {
                review.setImages(dto.getImages());
            }
            review.setStatus(ReviewStatus.APPROVED);
            review.setUpdatedAt(now);
            log.info("[Review][UPDATE] Cập nhật review id={} cho (userId={}, productId={}, orderItemId={}), rating={}",
                    review.getId(), user.getId(), product.getId(),
                    orderItem != null ? orderItem.getId() : null, dto.getRating());
        } else {
            review = new Review();
            review.setUser(user);
            review.setProduct(product);
            review.setOrderItem(orderItem);
            review.setRating(dto.getRating());
            review.setComment(dto.getComment());
            review.setImages(dto.getImages());
            review.setStatus(ReviewStatus.APPROVED); // Mặc định duyệt tự động, Admin có thể ẩn/từ chối sau
            review.setCreatedAt(now);
            review.setUpdatedAt(now);
            log.info("[Review][CREATE] Tạo review mới cho (userId={}, productId={}, orderItemId={}), rating={}",
                    user.getId(), product.getId(),
                    orderItem != null ? orderItem.getId() : null, dto.getRating());
        }

        Review saved = reviewRepository.save(review);
        recalculateProductRating(product);
        return saved;
    }

    private OrderItem resolveDeliveredOrderItemForSubmit(User user, Product product, Long requestedOrderItemId) {
        if (requestedOrderItemId != null) {
            OrderItem orderItem = orderItemRepository.findDeliveredOrderItem(user.getId(), requestedOrderItemId)
                    .orElseThrow(() -> new IllegalArgumentException("Đơn hàng chưa giao thành công hoặc không hợp lệ!"));
            if (orderItem.getProduct() == null || !Objects.equals(orderItem.getProduct().getId(), product.getId())) {
                throw new IllegalArgumentException("Sản phẩm đánh giá không thuộc đơn hàng đã chọn!");
            }
            return orderItem;
        }

        List<OrderItem> deliveredItems = orderItemRepository.findDeliveredOrderItemsByUserIdAndProductId(
                user.getId(), product.getId());
        if (deliveredItems != null && !deliveredItems.isEmpty()) {
            for (OrderItem oi : deliveredItems) {
                if (!reviewRepository.existsByUserIdAndProductIdAndOrderItemId(user.getId(), product.getId(), oi.getId())) {
                    return oi;
                }
            }
            return deliveredItems.get(0);
        }

        if (!orderItemRepository.existsByUserIdAndProductIdAndDeliveredOrder(user.getId(), product.getId())) {
            throw new IllegalArgumentException("Chỉ khách hàng đã mua và nhận sản phẩm thành công mới có thể gửi đánh giá!");
        }
        return null;
    }

    private Optional<Review> findExistingReview(Long userId, Long productId, OrderItem orderItem) {
        if (orderItem != null && orderItem.getId() != null) {
            Optional<Review> exact = reviewRepository.findByUserIdAndProductIdAndOrderItemId(
                    userId, productId, orderItem.getId());
            if (exact.isPresent()) {
                return exact;
            }
            Optional<Review> byOrderItem = reviewRepository.findByUserIdAndOrderItemId(userId, orderItem.getId());
            if (byOrderItem.isPresent()) {
                return byOrderItem;
            }
        }
        return reviewRepository.findFirstByUserIdAndProductIdAndOrderItemIsNullOrderByIdDesc(userId, productId);
    }

    /**
     * Lọc phòng vệ để đảm bảo trang sản phẩm chỉ hiển thị tối đa 1 review mới nhất cho mỗi (user_id, product_id, order_item_id).
     */
    private List<Review> deduplicateReviews(List<Review> rawReviews) {
        if (rawReviews == null || rawReviews.isEmpty()) {
            return List.of();
        }
        List<Review> sorted = new ArrayList<>(rawReviews);
        sorted.sort(newestFirstComparator());

        Map<String, Review> unique = new LinkedHashMap<>();
        for (Review r : sorted) {
            String key = buildUniqueKey(r, r.getOrderItem());
            unique.putIfAbsent(key, r);
        }
        return new ArrayList<>(unique.values());
    }

    /**
     * Migration dữ liệu cũ:
     * 1. Gán order_item_id cho review legacy nếu tìm thấy OrderItem đã giao tương ứng.
     * 2. Giữ lại duy nhất review mới nhất cho mỗi khóa (user_id, product_id, order_item_id) và xóa review trùng cũ.
     * 3. Tính lại review_count và average_rating cho toàn bộ sản phẩm.
     */
    @Transactional
    public void cleanupDuplicateReviewsAndRecalculate() {
        List<Review> allReviews = reviewRepository.findAll();
        if (allReviews != null && !allReviews.isEmpty()) {
            List<Review> sorted = new ArrayList<>(allReviews);
            sorted.sort(newestFirstComparator());

            Map<String, List<OrderItem>> deliveredCache = new HashMap<>();
            Map<String, Review> keptByKey = new LinkedHashMap<>();
            List<Review> duplicatesToDelete = new ArrayList<>();
            List<Review> updatedToSave = new ArrayList<>();

            for (Review r : sorted) {
                OrderItem resolvedOrderItem = r.getOrderItem();
                if (resolvedOrderItem == null && r.getUser() != null && r.getProduct() != null) {
                    String pairKey = r.getUser().getId() + ":" + r.getProduct().getId();
                    List<OrderItem> delivered = deliveredCache.computeIfAbsent(pairKey, k -> {
                        List<OrderItem> found = orderItemRepository.findDeliveredOrderItemsByUserIdAndProductId(
                                r.getUser().getId(), r.getProduct().getId());
                        return found != null ? found : List.of();
                    });
                    if (!delivered.isEmpty()) {
                        resolvedOrderItem = delivered.get(0);
                    }
                }

                String uniqueKey = buildUniqueKey(r, resolvedOrderItem);
                if (!keptByKey.containsKey(uniqueKey)) {
                    keptByKey.put(uniqueKey, r);
                    boolean changed = false;
                    if (r.getOrderItem() == null && resolvedOrderItem != null) {
                        r.setOrderItem(resolvedOrderItem);
                        changed = true;
                    }
                    if (r.getUpdatedAt() == null) {
                        r.setUpdatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : LocalDateTime.now());
                        changed = true;
                    }
                    if (changed) {
                        updatedToSave.add(r);
                    }
                } else {
                    duplicatesToDelete.add(r);
                }
            }

            if (!duplicatesToDelete.isEmpty()) {
                reviewRepository.deleteAll(duplicatesToDelete);
                reviewRepository.flush();
                log.info("[Review][Migration] Đã xóa {} review trùng lặp cũ (chỉ giữ bản ghi mới nhất).", duplicatesToDelete.size());
            }
            if (!updatedToSave.isEmpty()) {
                reviewRepository.saveAll(updatedToSave);
                reviewRepository.flush();
            }
        }

        List<Product> products = productRepository.findAll();
        if (products != null) {
            for (Product product : products) {
                recalculateProductRating(product);
            }
        }
    }

    private Comparator<Review> newestFirstComparator() {
        return (r1, r2) -> {
            LocalDateTime t1 = r1.getUpdatedAt() != null ? r1.getUpdatedAt() : r1.getCreatedAt();
            LocalDateTime t2 = r2.getUpdatedAt() != null ? r2.getUpdatedAt() : r2.getCreatedAt();
            if (t1 != null && t2 != null) {
                int cmp = t2.compareTo(t1);
                if (cmp != 0) {
                    return cmp;
                }
            } else if (t1 == null && t2 != null) {
                return 1;
            } else if (t1 != null) {
                return -1;
            }
            long id1 = r1.getId() != null ? r1.getId() : 0L;
            long id2 = r2.getId() != null ? r2.getId() : 0L;
            return Long.compare(id2, id1);
        };
    }

    private String buildUniqueKey(Review review, OrderItem orderItem) {
        Long userId = review.getUser() != null ? review.getUser().getId() : null;
        Long productId = review.getProduct() != null ? review.getProduct().getId() : null;
        Long orderItemId = orderItem != null ? orderItem.getId() : null;
        if (orderItemId == null && review.getId() != null && userId == null && productId == null) {
            return "REVIEW#" + review.getId();
        }
        return userId + ":" + productId + ":" + (orderItemId != null ? orderItemId : "NULL");
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Review updateReviewStatus(Long reviewId, ReviewStatus status) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá!"));
        review.setStatus(status);
        review.setUpdatedAt(LocalDateTime.now());
        Review saved = reviewRepository.save(review);
        recalculateProductRating(review.getProduct().getId());
        return saved;
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá!"));
        Long productId = review.getProduct().getId();
        reviewRepository.deleteById(reviewId);
        recalculateProductRating(productId);
    }
}
