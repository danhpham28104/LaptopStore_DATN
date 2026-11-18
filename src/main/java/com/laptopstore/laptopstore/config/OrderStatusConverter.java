package com.laptopstore.laptopstore.config;

import com.laptopstore.laptopstore.enums.OrderStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter cho OrderStatus.
 *
 * <p>Khi đọc từ DB: gọi {@link OrderStatus#fromString(String)} — hỗ trợ tự động chuyển đổi
 * cả giá trị cũ lẫn 11 giá trị chuẩn mới.
 *
 * <p>Khi ghi vào DB: lưu {@link Enum#name()} của enum chuẩn mới.
 */
@Converter(autoApply = true)
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrderStatus attribute) {
        if (attribute == null) {
            return OrderStatus.CHO_XAC_NHAN.name();
        }
        return attribute.name();
    }

    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return OrderStatus.CHO_XAC_NHAN;
        }
        return OrderStatus.fromString(dbData);
    }
}
