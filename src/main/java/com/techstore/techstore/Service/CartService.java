package com.techstore.techstore.Service;

import com.techstore.techstore.Repository.CartItemRepository;
import com.techstore.techstore.Repository.CartRepository;
import com.techstore.techstore.Repository.ProductRepository;
import com.techstore.techstore.entity.Cart;
import com.techstore.techstore.entity.CartItem;
import com.techstore.techstore.entity.Product;
import com.techstore.techstore.entity.ProductVariant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CartService {

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
            item.setUnitPriceAtAdd(product.getPrice());
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
