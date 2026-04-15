package com.techstore.techstore.Repository;

import com.techstore.techstore.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    // 🔹 Lấy tất cả biến thể của 1 sản phẩm
    List<ProductVariant> findByProduct_Id(Long productId);

    // 🔹 Tìm 1 biến thể theo màu và dung lượng trong cùng sản phẩm
    Optional<ProductVariant> findByProduct_IdAndColorIgnoreCaseAndStorageIgnoreCase(
            Long productId,
            String color,
            String storage
    );

    // 🔹 Tìm tất cả biến thể có tồn kho > 0
    List<ProductVariant> findByStockGreaterThan(Integer minStock);



}
