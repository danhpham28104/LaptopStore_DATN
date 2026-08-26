package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.Repository.ProductRepository;
import com.laptopstore.laptopstore.entity.Product;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class CompareService {

    private static final String COMPARE_SESSION_KEY = "COMPARE_PRODUCT_IDS";
    private static final int MAX_COMPARE_ITEMS = 4;

    @Autowired
    private ProductRepository productRepository;

    @SuppressWarnings("unchecked")
    public List<Long> getCompareList(HttpSession session) {
        List<Long> compareIds = (List<Long>) session.getAttribute(COMPARE_SESSION_KEY);
        if (compareIds == null) {
            compareIds = new ArrayList<>();
            session.setAttribute(COMPARE_SESSION_KEY, compareIds);
        }
        return compareIds;
    }

    public boolean addProductToCompare(Long productId, HttpSession session) {
        List<Long> compareIds = getCompareList(session);
        if (compareIds.contains(productId)) {
            return false; // Already in compare list
        }
        if (compareIds.size() >= MAX_COMPARE_ITEMS) {
            throw new IllegalStateException("Bạn chỉ có thể so sánh tối đa " + MAX_COMPARE_ITEMS + " sản phẩm cùng lúc.");
        }
        compareIds.add(productId);
        session.setAttribute(COMPARE_SESSION_KEY, compareIds);
        return true;
    }

    public void removeProductFromCompare(Long productId, HttpSession session) {
        List<Long> compareIds = getCompareList(session);
        compareIds.remove(productId);
        session.setAttribute(COMPARE_SESSION_KEY, compareIds);
    }

    public void clearCompareList(HttpSession session) {
        session.removeAttribute(COMPARE_SESSION_KEY);
    }

    public List<Product> getCompareProducts(HttpSession session) {
        List<Long> compareIds = getCompareList(session);
        if (compareIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Product> products = new ArrayList<>();
        for (Long id : compareIds) {
            productRepository.findById(id).ifPresent(products::add);
        }
        return products;
    }
}
