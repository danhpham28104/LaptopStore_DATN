package com.laptopstore.laptopstore.config;

import com.laptopstore.laptopstore.enums.OrderStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter cho OrderStatus.
 *
 * <p>Khi đọc từ DB: gọi {@link OrderStatus#fromString(String)} — hỗ trợ cả giá trị cũ
 * (PENDING_PAYMENT, PACKING, REFUNDED) lẫn giá trị mới (PENDING, PROCESSING, RETURNED).
 *
 * <p>Khi ghi vào DB: lưu {@link Enum#name()} của enum value (không thay đổi).
 */
@Converter(autoApply = true)
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrderStatus attribute) {
        if (attribute == null) {
            return OrderStatus.PENDING.name();
        }
        return attribute.name();
    }

    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return OrderStatus.PENDING;
        }
        // fromString() xử lý cả giá trị cũ lẫn mới
        return OrderStatus.fromString(dbData);
    }
}
