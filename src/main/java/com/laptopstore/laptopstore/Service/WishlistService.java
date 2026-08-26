package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.ProductRepository;
import com.laptopstore.laptopstore.Repository.WishlistRepository;
import com.laptopstore.laptopstore.entity.Product;
import com.laptopstore.laptopstore.entity.User;
import com.laptopstore.laptopstore.entity.Wishlist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<Wishlist> getUserWishlist(User user) {
        if (user == null) return Collections.emptyList();
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public boolean isWishlisted(User user, Long productId) {
        if (user == null || productId == null) return false;
        return wishlistRepository.existsByUserIdAndProductId(user.getId(), productId);
    }

    @Transactional
    public boolean toggleWishlist(User user, Long productId) {
        if (user == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập để lưu sản phẩm yêu thích!");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm!"));

        Optional<Wishlist> existing = wishlistRepository.findByUserIdAndProductId(user.getId(), productId);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return false; // Đã bỏ yêu thích
        } else {
            Wishlist wishlist = new Wishlist(user, product);
            wishlistRepository.save(wishlist);
            return true; // Đã thêm yêu thích
        }
    }

    @Transactional
    public void removeFromWishlist(User user, Long productId) {
        if (user != null && productId != null) {
            wishlistRepository.deleteByUserIdAndProductId(user.getId(), productId);
        }
    }

    public Long getWishlistCount(User user) {
        if (user == null) return 0L;
        return wishlistRepository.countByUserId(user.getId());
    }
}
