package com.techstore.techstore.Repository;

import com.techstore.techstore.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser_Id(Long userId);
    boolean existsByUser_Id(Long userId);
    Optional<Cart> findByUser_Username(String username);
}
