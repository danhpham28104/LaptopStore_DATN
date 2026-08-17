package com.techstore.techstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Đại diện cho một sản phẩm được AI RAG gợi ý trong response.
 * Các field camelCase khớp với cấu trúc JSON FastAPI trả về.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedProductDto {

    /** ID sản phẩm trong hệ thống LaptopStore */
    private Long id;

    /** Tên sản phẩm */
    private String name;

    /** Giá sản phẩm (VNĐ) */
    private Double price;

    /** URL trang chi tiết sản phẩm */
    private String url;

    /** URL ảnh đại diện sản phẩm */
    private String imageUrl;

    /** Lý do AI gợi ý sản phẩm này */
    private String reason;
}
