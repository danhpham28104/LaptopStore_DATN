package com.techstore.techstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận request chat từ frontend (JavaScript).
 * Dùng camelCase theo convention Spring Boot / JSON.
 * Service sẽ map sang snake_case khi gửi sang FastAPI.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDto {

    /** Nội dung câu hỏi của người dùng */
    private String message;

    /**
     * ID phiên hội thoại.
     * Nếu frontend không gửi, controller sẽ tự lấy từ HttpSession.
     */
    private String sessionId;

    /**
     * Số lượng sản phẩm tối đa muốn gợi ý (top-k).
     * Mặc định 5 nếu không được chỉ định.
     */
    private Integer topK = 5;
}
