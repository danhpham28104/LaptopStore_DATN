package com.techstore.techstore.Controller;

import com.techstore.techstore.Service.CartItemService;
import com.techstore.techstore.entity.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cart-items")
public class CartItemController {

    @Autowired
    private CartItemService cartItemService;

    /**
     * Lấy tất cả sản phẩm trong giỏ hàng của 1 cart cụ thể
     */
    @GetMapping("/cart/{cartId}")
    public List<CartItem> getItemsByCart(@PathVariable Long cartId) {
        return cartItemService.getCartItemsByCartId(cartId);
    }

    /**
     * Lấy 1 sản phẩm trong giỏ hàng (theo cartId và productId)
     */
    @GetMapping("/cart/{cartId}/product/{productId}")
    public Optional<CartItem> getCartItem(@PathVariable Long cartId, @PathVariable Long productId) {
        return cartItemService.getCartItemByCartAndProduct(cartId, productId);
    }

    /**
     * Thêm hoặc cập nhật sản phẩm trong giỏ hàng
     */
    @PostMapping
    public CartItem addOrUpdateCartItem(@RequestBody CartItem cartItem) {
        return cartItemService.saveCartItem(cartItem);
    }

    /**
     * Xóa 1 sản phẩm trong giỏ hàng
     */
    @DeleteMapping("/cart/{cartId}/product/{productId}")
    public void deleteCartItem(@PathVariable Long cartId, @PathVariable Long productId) {
        cartItemService.deleteCartItem(cartId, productId);
    }

    /**
     * Xóa toàn bộ giỏ hàng (tất cả sản phẩm)
     */
    @DeleteMapping("/cart/{cartId}")
    public void clearCart(@PathVariable Long cartId) {
        cartItemService.deleteAllByCartId(cartId);
    }
}
