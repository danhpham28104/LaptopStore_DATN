package com.techstore.techstore.Service;

import com.techstore.techstore.Repository.ProductViewHistoryRepository;
import com.techstore.techstore.entity.Product;
import com.techstore.techstore.entity.ProductViewHistory;
import com.techstore.techstore.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductViewHistoryService {

    @Autowired
    private ProductViewHistoryRepository productViewHistoryRepository;

    @Transactional
    public void trackView(User user, Product product) {
        if (user == null || product == null) {
            return;
        }
        Optional<ProductViewHistory> historyOpt = productViewHistoryRepository.findByUserAndProduct(user, product);
        if (historyOpt.isPresent()) {
            ProductViewHistory history = historyOpt.get();
            history.incrementViewCount();
            productViewHistoryRepository.save(history);
        } else {
            ProductViewHistory history = new ProductViewHistory(user, product);
            productViewHistoryRepository.save(history);
        }
    }

    @Transactional(readOnly = true)
    public List<ProductViewHistory> getTopViewedProducts(Long userId, int limit) {
        return productViewHistoryRepository.findTopViewedByUserId(userId, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<ProductViewHistory> getRecentlyViewedProducts(Long userId, int limit) {
        return productViewHistoryRepository.findRecentlyViewedByUserId(userId, PageRequest.of(0, limit));
    }
}
