package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.StockLogRepository;
import com.laptopstore.laptopstore.entity.*;
import com.laptopstore.laptopstore.enums.StockLogType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockLogService {

    @Autowired
    private StockLogRepository stockLogRepository;

    /**
     * Ghi log biến động kho cho sản phẩm (không có biến thể)
     */
    @Transactional
    public StockLog log(Product product, Order order, StockLogType type,
                        int quantity, int stockAfter, String performedBy, String note) {
        StockLog sl = new StockLog(product, null, order, type, quantity, stockAfter, performedBy, note);
        return stockLogRepository.save(sl);
    }

    /**
     * Ghi log biến động kho cho biến thể sản phẩm
     */
    @Transactional
    public StockLog logVariant(Product product, ProductVariant variant, Order order,
                               StockLogType type, int quantity, int stockAfter,
                               String performedBy, String note) {
        StockLog sl = new StockLog(product, variant, order, type, quantity, stockAfter, performedBy, note);
        return stockLogRepository.save(sl);
    }

    /**
     * Lấy toàn bộ lịch sử kho theo sản phẩm
     */
    @Transactional(readOnly = true)
    public List<StockLog> getByProductId(Long productId) {
        return stockLogRepository.findByProduct_IdOrderByCreatedAtDesc(productId);
    }

    /**
     * Lấy toàn bộ lịch sử kho (phân trang)
     */
    @Transactional(readOnly = true)
    public Page<StockLog> getAll(Pageable pageable) {
        return stockLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * Lọc lịch sử kho theo điều kiện
     */
    @Transactional(readOnly = true)
    public Page<StockLog> filter(Long productId, StockLogType type,
                                 LocalDateTime from, LocalDateTime to,
                                 Pageable pageable) {
        return stockLogRepository.filterLogs(productId, type, from, to, pageable);
    }
}
