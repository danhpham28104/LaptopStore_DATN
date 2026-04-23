package com.techstore.techstore.Service;

import com.techstore.techstore.Repository.CartItemRepository;
import com.techstore.techstore.entity.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CartItemService {

    @Autowired
    private CartItemRepository cartItemRepository;

    /**
     * Lấy danh sách các sản phẩm trong giỏ hàng theo cartId
     */
    public List<CartItem> getCartItemsByCartId(Long cartId) {
        return cartItemRepository.findByCart_Id(cartId);
    }

    /**
     * Lấy 1 sản phẩm trong giỏ hàng theo cartId và productId
     */
    public Optional<CartItem> getCartItemByCartAndProduct(Long cartId, Long productId) {
        return cartItemRepository.findByCart_IdAndProduct_Id(cartId, productId);
    }

    /**
     * Lưu hoặc cập nhật sản phẩm trong giỏ hàng
     */
    public CartItem saveCartItem(CartItem cartItem) {
        return cartItemRepository.save(cartItem);
    }

    /**
     * Xóa 1 sản phẩm khỏi giỏ hàng theo cartId và productId
     */
    @Transactional
    public void deleteCartItem(Long cartId, Long productId) {
        cartItemRepository.deleteByCart_IdAndProduct_Id(cartId, productId);
    }

    /**
     * Xóa toàn bộ giỏ hàng
     */
    @Transactional
    public void deleteAllByCartId(Long cartId) {
        List<CartItem> items = cartItemRepository.findByCart_Id(cartId);
        cartItemRepository.deleteAll(items);
    }
}
