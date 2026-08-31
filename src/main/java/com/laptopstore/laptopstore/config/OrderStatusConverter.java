package com.laptopstore.laptopstore.config;

import com.laptopstore.laptopstore.enums.OrderStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter cho OrderStatus.
 * Tự động chuyển đổi mượt mà các chuỗi trạng thái cũ trong DB (như 'Delivered', 'Pending', 'Paid')
 * sang Enum OrderStatus tương ứng mà không bị crash IllegalArgumentException.
 */
@Converter(autoApply = true)
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrderStatus attribute) {
        if (attribute == null) {
            return OrderStatus.PENDING_PAYMENT.name();
        }
        return attribute.name();
    }

    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return OrderStatus.PENDING_PAYMENT;
        }
        return OrderStatus.fromString(dbData);
    }
}
