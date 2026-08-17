package com.techstore.techstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO chứa response từ FastAPI RAG service trả về cho frontend.
 * Các field camelCase khớp với JSON FastAPI phản hồi.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDto {

    /** Câu trả lời của AI */
    private String answer;

    /**
     * Điểm tin cậy của câu trả lời (0.0 – 1.0).
     * 0.0 khi xảy ra lỗi fallback.
     */
    private Double confidenceScore;

    /** Danh sách nguồn tham chiếu (tên sản phẩm, tài liệu, v.v.) */
    private List<String> citations;

    /** Thông tin còn thiếu hoặc thông báo lỗi thân thiện */
    private List<String> missingInformation;

    /** Danh sách sản phẩm được AI gợi ý */
    private List<RecommendedProductDto> recommendedProducts;
}
