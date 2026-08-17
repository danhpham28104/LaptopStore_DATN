package com.laptopstore.laptopstore.Repository;

import com.laptopstore.laptopstore.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCart_Id(Long cartId);


    Optional<CartItem> findByCart_IdAndProduct_Id(Long cartId, Long productId);

    void deleteByCart_IdAndProduct_Id(Long cartId, Long productId);
}
