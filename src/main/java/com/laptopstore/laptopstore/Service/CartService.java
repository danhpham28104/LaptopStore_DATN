package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.CartItemRepository;
import com.laptopstore.laptopstore.Repository.CartRepository;
import com.laptopstore.laptopstore.Repository.ProductRepository;
import com.laptopstore.laptopstore.entity.Cart;
import com.laptopstore.laptopstore.entity.CartItem;
import com.laptopstore.laptopstore.entity.Product;
import com.laptopstore.laptopstore.entity.ProductVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * Lấy cart theo userId (có thể null)
     */
    @Transactional(readOnly = true)
    public Optional<Cart> getCartByUserId(Long userId) {
        return cartRepository.findByUser_Id(userId);
    }

    /**
     * Tạo cart mới cho user (dùng khi user chưa có cart — e.g. user mới hoặc
     * được tạo thủ công mà chưa qua luồng đăng ký có init cart).
     */
    @Transactional
    public Cart createCartForUser(com.laptopstore.laptopstore.entity.User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        Cart saved = cartRepository.save(cart);
        log.info("[CartService] Tạo cart mới cho user={} → cartId={}", user.getUsername(), saved.getId());
        return saved;
    }

    /**
     * ✅ FIX LỖI ADMIN:
     * - Admin không có cart → trả về 0
     * - User chưa có cart → trả về 0
     */
    public int getCartItemCount(String username) {
        if (username == null) {
            return 0;
        }

        Cart cart = cartRepository.findByUser_Username(username).orElse(null);

        if (cart == null || cart.getItems() == null) {
            return 0;
        }

        return cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * Thêm / gộp sản phẩm vào giỏ
     */
    @Transactional
    public Cart addToCart(Cart cart, Product product, ProductVariant variant, int quantity) {
        int addQty = Math.max(1, quantity);

        Optional<CartItem> existingOpt = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId())
                        && ((variant == null && i.getVariant() == null)
                        || (variant != null && i.getVariant() != null
                        && i.getVariant().getId().equals(variant.getId()))))
                .findFirst();

        CartItem item;
        if (existingOpt.isPresent()) {
            // Đã có → cộng dồn
            item = existingOpt.get();
            item.setQuantity(item.getQuantity() + addQty);
            item.recalc();
        } else {
            // Chưa có → tạo mới
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setVariant(variant);
            item.setQuantity(addQty);
            item.setUnitPriceAtAdd(product.getFinalPrice());
            item.recalc();
            cart.getItems().add(item);
        }

        cart.recalcTotals();
        return cartRepository.save(cart);
    }

    /**
     * Xóa toàn bộ cart
     */
    @Transactional
    public void clearCart(Long cartId) {
        cartItemRepository.deleteAll(cartItemRepository.findByCart_Id(cartId));
        cartRepository.findById(cartId).ifPresent(c -> {
            c.getItems().clear();
            c.recalcTotals();
            cartRepository.save(c);
        });
    }
}
