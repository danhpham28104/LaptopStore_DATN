package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.OrderItemRepository;
import com.laptopstore.laptopstore.Repository.ProductRepository;
import com.laptopstore.laptopstore.Repository.ReviewRepository;
import com.laptopstore.laptopstore.dto.ReviewRequestDto;
import com.laptopstore.laptopstore.entity.Order;
import com.laptopstore.laptopstore.entity.OrderItem;
import com.laptopstore.laptopstore.entity.Product;
import com.laptopstore.laptopstore.entity.Review;
import com.laptopstore.laptopstore.entity.User;
import com.laptopstore.laptopstore.enums.OrderStatus;
import com.laptopstore.laptopstore.enums.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User user1;
    private User user2;
    private Product product;
    private OrderItem orderItem1;
    private OrderItem orderItem2;
    private List<Review> storedReviews;
    private AtomicLong seq;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(1L);
        user1.setUsername("buyer1");
        user1.setFullName("Nguyen Van A");

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("buyer2");
        user2.setFullName("Tran Thi B");

        product = new Product();
        product.setId(10L);
        product.setName("Laptop Dell XPS 15");
        product.setPrice(BigDecimal.valueOf(35000000));

        Order order1 = new Order();
        order1.setId(100L);
        order1.setUser(user1);
        order1.setOrderStatus(OrderStatus.HOAN_THANH);

        orderItem1 = new OrderItem();
        orderItem1.setId(1001L);
        orderItem1.setOrder(order1);
        orderItem1.setProduct(product);

        Order order2 = new Order();
        order2.setId(101L);
        order2.setUser(user2);
        order2.setOrderStatus(OrderStatus.DA_GIAO_HANG);

        orderItem2 = new OrderItem();
        orderItem2.setId(1002L);
        orderItem2.setOrder(order2);
        orderItem2.setProduct(product);

        storedReviews = new ArrayList<>();
        seq = new AtomicLong(1L);
    }

    private void stubInMemoryReviewStore() {
        lenient().when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        lenient().when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        lenient().when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(seq.getAndIncrement());
                storedReviews.add(r);
            } else {
                storedReviews.removeIf(existing -> Objects.equals(existing.getId(), r.getId()));
                storedReviews.add(r);
            }
            return r;
        });

        lenient().when(reviewRepository.findByUserIdAndProductIdAndOrderItemId(anyLong(), anyLong(), anyLong()))
                .thenAnswer(inv -> {
                    Long uid = inv.getArgument(0);
                    Long pid = inv.getArgument(1);
                    Long oiId = inv.getArgument(2);
                    return storedReviews.stream()
                            .filter(r -> r.getUser() != null && Objects.equals(r.getUser().getId(), uid)
                                    && r.getProduct() != null && Objects.equals(r.getProduct().getId(), pid)
                                    && r.getOrderItem() != null && Objects.equals(r.getOrderItem().getId(), oiId))
                            .findFirst();
                });
    }

    private void stubQueriesConsistently() {
        stubInMemoryReviewStore();

        lenient().when(reviewRepository.findAverageRatingByProductIdAndStatus(10L, ReviewStatus.APPROVED))
                .thenAnswer(inv -> {
                    List<Review> matching = storedReviews.stream()
                            .filter(r -> r.getProduct() != null && Objects.equals(r.getProduct().getId(), 10L)
                                    && r.getStatus() == ReviewStatus.APPROVED)
                            .toList();
                    return matching.isEmpty() ? null : matching.stream().mapToInt(Review::getRating).average().getAsDouble();
                });

        lenient().when(reviewRepository.countByProductIdAndStatus(10L, ReviewStatus.APPROVED))
                .thenAnswer(inv -> storedReviews.stream()
                        .filter(r -> r.getProduct() != null && Objects.equals(r.getProduct().getId(), 10L)
                                && r.getStatus() == ReviewStatus.APPROVED)
                        .count());

        lenient().when(reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(10L, ReviewStatus.APPROVED))
                .thenAnswer(inv -> storedReviews.stream()
                        .filter(r -> r.getProduct() != null && Objects.equals(r.getProduct().getId(), 10L)
                                && r.getStatus() == ReviewStatus.APPROVED)
                        .toList());
    }

    @Test
    @DisplayName("Đánh giá lại cùng order_item KHÔNG tạo review thứ hai mà cập nhật review cũ (giữ nguyên ID)")
    void testAddReview_SecondTimeSameOrderItem_UpdatesExistingReviewWithoutCreatingDuplicate() {
        stubQueriesConsistently();
        when(orderItemRepository.findDeliveredOrderItem(1L, 1001L)).thenReturn(Optional.of(orderItem1));

        ReviewRequestDto firstDto = new ReviewRequestDto();
        firstDto.setProductId(10L);
        firstDto.setOrderItemId(1001L);
        firstDto.setRating(4);
        firstDto.setComment("Đánh giá lần đầu: màn hình đẹp");

        Review firstSaved = reviewService.addReview(user1, firstDto);
        Long originalId = firstSaved.getId();
        LocalDateTime createdTime = firstSaved.getCreatedAt();

        assertNotNull(originalId);
        assertEquals(1, storedReviews.size());
        assertEquals(1, product.getReviewCount());
        assertEquals(4.0, product.getAverageRating());

        // Người dùng đánh giá lại cho cùng orderItem1
        ReviewRequestDto secondDto = new ReviewRequestDto();
        secondDto.setProductId(10L);
        secondDto.setOrderItemId(1001L);
        secondDto.setRating(5);
        secondDto.setComment("Đánh giá lại: pin trâu, rất hài lòng");

        Review secondSaved = reviewService.addReview(user1, secondDto);

        assertEquals(originalId, secondSaved.getId()); // Giữ nguyên ID review
        assertEquals(1, storedReviews.size());         // Không thể tạo review thứ hai
        assertEquals(5, secondSaved.getRating());
        assertEquals("Đánh giá lại: pin trâu, rất hài lòng", secondSaved.getComment());
        assertEquals(createdTime, secondSaved.getCreatedAt());
        assertNotNull(secondSaved.getUpdatedAt());
        assertTrue(!secondSaved.getUpdatedAt().isBefore(createdTime));
    }

    @Test
    @DisplayName("Đánh giá lại cập nhật average_rating chính xác và không làm tăng review_count")
    void testAddReview_ReReview_UpdatesAverageRatingAndKeepsReviewCountUnchanged() {
        stubQueriesConsistently();
        when(orderItemRepository.findDeliveredOrderItem(1L, 1001L)).thenReturn(Optional.of(orderItem1));
        when(orderItemRepository.findDeliveredOrderItem(2L, 1002L)).thenReturn(Optional.of(orderItem2));

        // User 1 chấm 3 sao
        ReviewRequestDto dto1 = new ReviewRequestDto();
        dto1.setProductId(10L);
        dto1.setOrderItemId(1001L);
        dto1.setRating(3);
        dto1.setComment("Tạm ổn");
        reviewService.addReview(user1, dto1);

        // User 2 chấm 5 sao -> trung bình = (3 + 5) / 2 = 4.0, tổng = 2
        ReviewRequestDto dto2 = new ReviewRequestDto();
        dto2.setProductId(10L);
        dto2.setOrderItemId(1002L);
        dto2.setRating(5);
        dto2.setComment("Tuyệt vời");
        reviewService.addReview(user2, dto2);

        assertEquals(2, product.getReviewCount());
        assertEquals(4.0, product.getAverageRating());

        // User 1 sửa đánh giá từ 3 sao thành 4 sao -> trung bình phải là (4 + 5) / 2 = 4.5, review_count vẫn = 2
        ReviewRequestDto editDto1 = new ReviewRequestDto();
        editDto1.setProductId(10L);
        editDto1.setOrderItemId(1001L);
        editDto1.setRating(4);
        editDto1.setComment("Sau 1 tuần dùng thấy rất tốt");
        reviewService.addReview(user1, editDto1);

        assertEquals(2, storedReviews.size());
        assertEquals(2, product.getReviewCount());
        assertEquals(4.5, product.getAverageRating());
        assertEquals(2, reviewService.getApprovedReviewsByProduct(10L).size());
    }

    @Test
    @DisplayName("Đánh giá lại không truyền orderItemId vẫn tự khớp đơn đã mua để update, không tạo bản trùng")
    void testAddReview_WithoutExplicitOrderItemId_UpdatesExistingOrderReview() {
        stubQueriesConsistently();
        when(orderItemRepository.findDeliveredOrderItemsByUserIdAndProductId(1L, 10L))
                .thenReturn(List.of(orderItem1));

        ReviewRequestDto dto1 = new ReviewRequestDto();
        dto1.setProductId(10L);
        dto1.setRating(2);
        dto1.setComment("Lần 1");
        Review first = reviewService.addReview(user1, dto1);

        when(reviewRepository.existsByUserIdAndProductIdAndOrderItemId(1L, 10L, 1001L)).thenReturn(true);

        ReviewRequestDto dto2 = new ReviewRequestDto();
        dto2.setProductId(10L);
        dto2.setRating(5);
        dto2.setComment("Lần 2 đã đổi ý");
        Review second = reviewService.addReview(user1, dto2);

        assertEquals(first.getId(), second.getId());
        assertEquals(1, product.getReviewCount());
        assertEquals(5.0, product.getAverageRating());
    }

    @Test
    @DisplayName("Migration xóa các review trùng cũ, chỉ giữ bản mới nhất và recalculate review_count & average_rating")
    void testCleanupDuplicateReviewsAndRecalculate_KeepsLatestAndDeletesOldDuplicates() {
        stubQueriesConsistently();

        LocalDateTime now = LocalDateTime.now();

        Review oldDup1 = new Review(user1, product, orderItem1, 1, "Review cũ 1", null, ReviewStatus.APPROVED);
        oldDup1.setId(1L);
        oldDup1.setCreatedAt(now.minusDays(3));
        oldDup1.setUpdatedAt(now.minusDays(3));

        Review oldDup2 = new Review(user1, product, orderItem1, 2, "Review cũ 2", null, ReviewStatus.APPROVED);
        oldDup2.setId(2L);
        oldDup2.setCreatedAt(now.minusDays(2));
        oldDup2.setUpdatedAt(now.minusDays(2));

        Review latestForUser1 = new Review(user1, product, orderItem1, 5, "Review mới nhất", null, ReviewStatus.APPROVED);
        latestForUser1.setId(3L);
        latestForUser1.setCreatedAt(now.minusHours(2));
        latestForUser1.setUpdatedAt(now.minusHours(1));

        Review user2Review = new Review(user2, product, orderItem2, 3, "Bình thường", null, ReviewStatus.APPROVED);
        user2Review.setId(4L);
        user2Review.setCreatedAt(now.minusDays(1));
        user2Review.setUpdatedAt(now.minusDays(1));

        storedReviews.addAll(List.of(oldDup1, oldDup2, latestForUser1, user2Review));

        when(reviewRepository.findAll()).thenAnswer(inv -> new ArrayList<>(storedReviews));
        when(productRepository.findAll()).thenReturn(List.of(product));
        doAnswer(inv -> {
            List<Review> deleted = inv.getArgument(0);
            storedReviews.removeAll(deleted);
            return null;
        }).when(reviewRepository).deleteAll(anyList());

        reviewService.cleanupDuplicateReviewsAndRecalculate();

        assertEquals(2, storedReviews.size());
        assertTrue(storedReviews.contains(latestForUser1));
        assertTrue(storedReviews.contains(user2Review));
        assertFalse(storedReviews.contains(oldDup1));
        assertFalse(storedReviews.contains(oldDup2));
        assertEquals(2, product.getReviewCount());
        assertEquals(4.0, product.getAverageRating()); // (5 + 3) / 2 = 4.0
    }
}
