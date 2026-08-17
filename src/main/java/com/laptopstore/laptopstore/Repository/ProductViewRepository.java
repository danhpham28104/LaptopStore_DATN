package com.laptopstore.laptopstore.Repository;

import com.laptopstore.laptopstore.entity.ProductView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductViewRepository extends JpaRepository<ProductView, Long> {

    Optional<ProductView> findByUser_IdAndProduct_Id(Long userId, Long productId);

    List<ProductView> findByUser_IdOrderByViewCountDescLastViewedAtDesc(Long userId, Pageable pageable);
}
