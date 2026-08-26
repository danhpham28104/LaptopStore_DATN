package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.ProductRepository;
import com.laptopstore.laptopstore.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ProductCompareService {

    @Autowired
    private ProductRepository productRepository;

    public List<Product> getProductsForComparison(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Tối đa 4 sản phẩm
        List<Long> limitedIds = productIds.stream().distinct().limit(4).toList();
        List<Product> products = productRepository.findAllById(limitedIds);

        // Giữ đúng thứ tự ID yêu cầu
        List<Product> orderedProducts = new ArrayList<>();
        for (Long id : limitedIds) {
            products.stream()
                    .filter(p -> p.getId().equals(id))
                    .findFirst()
                    .ifPresent(orderedProducts::add);
        }

        return orderedProducts;
    }
}
