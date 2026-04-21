package com.techstore.techstore.Service;

import com.techstore.techstore.Repository.ProductVariantRepository;
import com.techstore.techstore.entity.ProductVariant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductVariantService {

    @Autowired
    private ProductVariantRepository productVariantRepository;

    /** 🔍 Lấy tất cả các biến thể theo productId */
    @Transactional(readOnly = true)
    public List<ProductVariant> getVariantsByProduct(Long productId) {
        return productVariantRepository.findByProduct_Id(productId);
    }

    /** 🔍 Lấy 1 biến thể theo ID */
    @Transactional(readOnly = true)
    public Optional<ProductVariant> getVariantById(Long id) {
        return productVariantRepository.findById(id);
    }

    /** 💾 Lưu hoặc cập nhật biến thể */
    @Transactional
    public ProductVariant save(ProductVariant variant) {
        return productVariantRepository.save(variant);
    }

    /** ❌ Xóa biến thể */
    @Transactional
    public void delete(Long id) {
        productVariantRepository.deleteById(id);
    }

    /** 🔍 Tìm biến thể theo (productId + color + storage) */
    @Transactional(readOnly = true)
    public Optional<ProductVariant> findByColorAndStorage(Long productId, String color, String storage) {
        return productVariantRepository
                .findByProduct_IdAndColorIgnoreCaseAndStorageIgnoreCase(productId, color, storage);
    }

    /** 🔍 Kiểm tra biến thể tồn tại hay không */
    @Transactional(readOnly = true)
    public boolean exists(Long productId, String color, String storage) {
        return productVariantRepository
                .findByProduct_IdAndColorIgnoreCaseAndStorageIgnoreCase(productId, color, storage)
                .isPresent();
    }
}
