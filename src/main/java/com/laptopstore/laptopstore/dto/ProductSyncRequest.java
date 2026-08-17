package com.laptopstore.laptopstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đại diện cho thông tin đồng bộ sản phẩm sang FastAPI RAG.
 * Khớp cấu trúc camelCase/snake_case của FastAPI ProductSyncRequest.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSyncRequest {

    private Long id;
    private String name;
    private String brand;
    private Double price;
    private Integer ram;
    private String cpu;
    private String gpu;
    private String storage;
    private Double screenSize;
    private String screenResolution;
    private String batteryLife;
    private Double weight;
    private String operatingSystem;
    private String url;
    private String imageUrl;
    private String description;
    private Boolean isHot;
    private String useCaseTags;
    private Integer stock;
    private String category;
}
