package com.laptopstore.laptopstore.Service;

import com.laptopstore.laptopstore.dto.ChatRequestDto;
import com.laptopstore.laptopstore.dto.ChatResponseDto;
import com.laptopstore.laptopstore.dto.RecommendedProductDto;
import com.laptopstore.laptopstore.dto.ProductSyncRequest;
import com.laptopstore.laptopstore.entity.Product;
import com.laptopstore.laptopstore.entity.ProductVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service tích hợp với FastAPI Product-Comparison-RAG.
 * <p>
 * Gọi endpoint: POST {rag.service.url}/api/chat
 * <p>
 * Mapping quan trọng:
 * - Spring (frontend) dùng camelCase: {@code sessionId}, {@code topK}
 * - FastAPI nhận snake_case: {@code session_id}, {@code top_k}
 * Service tự build Map với snake_case trước khi gửi sang FastAPI.
 */
@Service
public class RagIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(RagIntegrationService.class);

    /** Timeout kết nối và đọc dữ liệu từ FastAPI (milliseconds) */
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS    = 15_000;

    /** Thông báo fallback khi FastAPI không sẵn sàng */
    private static final String FALLBACK_ANSWER =
            "Hiện tại hệ thống tư vấn AI chưa sẵn sàng. Vui lòng thử lại sau.";
    private static final String FALLBACK_ERROR_NOTE =
            "Không thể kết nối tới dịch vụ tư vấn AI. Dịch vụ có thể đang khởi động hoặc tạm thời gián đoạn.";

    private final RestClient restClient;

    public RagIntegrationService(@Value("${rag.service.url}") String ragServiceUrl) {
        // Cấu hình timeout thông qua SimpleClientHttpRequestFactory
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(ragServiceUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("[RAG] RagIntegrationService khởi tạo với baseUrl={}", ragServiceUrl);
    }

    /**
     * Gửi câu hỏi của người dùng tới FastAPI RAG service.
     *
     * @param request DTO từ controller, dùng camelCase
     * @return ChatResponseDto chứa câu trả lời và sản phẩm gợi ý;
     *         trả về fallback thân thiện nếu FastAPI lỗi hoặc timeout
     */
    public ChatResponseDto chat(ChatRequestDto request) {
        // Xây dựng request body với snake_case cho FastAPI
        Map<String, Object> fastapiBody = Map.of(
                "message",    request.getMessage(),
                "session_id", request.getSessionId() != null ? request.getSessionId() : "",
                "top_k",      request.getTopK() != null ? request.getTopK() : 5
        );

        try {
            log.debug("[RAG] Gửi request tới /api/chat | sessionId={} | message={}",
                    request.getSessionId(), request.getMessage());

            ChatResponseDto response = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(fastapiBody)
                    .retrieve()
                    .body(ChatResponseDto.class);

            if (response == null) {
                log.warn("[RAG] FastAPI trả về null response.");
                return buildFallback("FastAPI trả về phản hồi rỗng.");
            }

            log.debug("[RAG] Nhận response thành công | confidenceScore={} | products={}",
                    response.getConfidenceScore(),
                    response.getRecommendedProducts() != null
                            ? response.getRecommendedProducts().size() : 0);

            return response;

        } catch (ResourceAccessException ex) {
            // Timeout hoặc không thể kết nối tới FastAPI
            log.warn("[RAG] Không thể kết nối FastAPI (timeout/connection refused): {}", ex.getMessage());
            return buildFallback(FALLBACK_ERROR_NOTE);

        } catch (Exception ex) {
            // Lỗi khác: parse JSON, HTTP error, v.v.
            log.error("[RAG] Lỗi không xác định khi gọi FastAPI: {}", ex.getMessage(), ex);
            return buildFallback("Lỗi nội bộ khi xử lý câu trả lời từ AI. Vui lòng thử lại.");
        }
    }

    /**
     * Tạo ChatResponseDto fallback thân thiện với người dùng.
     *
     * @param errorNote thông báo kỹ thuật để gắn vào missingInformation
     */
    private ChatResponseDto buildFallback(String errorNote) {
        List<RecommendedProductDto> emptyProducts = Collections.emptyList();
        List<String> emptyCitations = Collections.emptyList();
        List<String> missingInfo = List.of(errorNote);

        return new ChatResponseDto(
                FALLBACK_ANSWER,
                0.0,
                emptyCitations,
                missingInfo,
                emptyProducts
        );
    }

    /**
     * Đồng bộ thông tin một sản phẩm sang RAG.
     */
    public void syncProduct(Product product) {
        try {
            ProductSyncRequest syncRequest = mapToSyncRequest(product);
            log.info("[RAG] Đang đồng bộ sản phẩm ID={} ({}) sang RAG...", product.getId(), product.getName());
            
            restClient.post()
                    .uri("/api/sync-product")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(syncRequest)
                    .retrieve()
                    .toBodilessEntity();
            
            log.info("[RAG] Đồng bộ sản phẩm ID={} thành công.", product.getId());
        } catch (Exception e) {
            log.error("[RAG] Lỗi khi đồng bộ sản phẩm ID={}: {}", product.getId(), e.getMessage());
        }
    }

    /**
     * Đồng bộ hàng loạt sản phẩm sang RAG.
     */
    public void syncAllProducts(List<Product> products) {
        if (products == null || products.isEmpty()) {
            log.info("[RAG] Không có sản phẩm nào để đồng bộ.");
            return;
        }
        try {
            List<ProductSyncRequest> syncRequests = new ArrayList<>();
            for (Product p : products) {
                syncRequests.add(mapToSyncRequest(p));
            }
            log.info("[RAG] Đang đồng bộ hàng loạt {} sản phẩm sang RAG...", products.size());
            
            Map<String, Object> body = Map.of("products", syncRequests);
            
            restClient.post()
                    .uri("/api/sync-product/bulk")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            
            log.info("[RAG] Đồng bộ hàng loạt {} sản phẩm thành công.", products.size());
        } catch (Exception e) {
            log.error("[RAG] Lỗi khi đồng bộ hàng loạt sản phẩm: {}", e.getMessage());
        }
    }

    /**
     * Xóa sản phẩm khỏi RAG.
     */
    public void deleteProductFromRag(Long id) {
        try {
            log.info("[RAG] Đang xóa sản phẩm ID={} khỏi RAG...", id);
            
            restClient.delete()
                    .uri("/api/sync-product/{id}", id)
                    .retrieve()
                    .toBodilessEntity();
            
            log.info("[RAG] Xóa sản phẩm ID={} khỏi RAG thành công.", id);
        } catch (Exception e) {
            log.error("[RAG] Lỗi khi xóa sản phẩm ID={} khỏi RAG: {}", id, e.getMessage());
        }
    }

    /**
     * Map từ entity Product sang ProductSyncRequest
     */
    private ProductSyncRequest mapToSyncRequest(Product product) {
        ProductSyncRequest req = new ProductSyncRequest();
        req.setId(product.getId());
        req.setName(product.getName());
        req.setBrand(product.getBrand() != null ? product.getBrand().getName() : "Unknown");
        
        // Price
        if (product.getFinalPrice() != null) {
            req.setPrice(product.getFinalPrice().doubleValue());
        } else {
            req.setPrice(product.getPrice() != null ? product.getPrice().doubleValue() : 0.0);
        }
        
        // RAM
        req.setRam(parseRam(product.getRam()));
        req.setCpu(product.getCpu() != null ? product.getCpu() : "Unknown");
        req.setGpu(product.getGpu() != null ? product.getGpu() : "Unknown");
        
        // Storage
        String storage = "Unknown";
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            ProductVariant targetVariant = product.getVariants().stream()
                    .filter(v -> v.getStock() != null && v.getStock() > 0)
                    .findFirst()
                    .orElse(product.getVariants().get(0));
            if (targetVariant != null && targetVariant.getStorage() != null) {
                storage = targetVariant.getStorage();
            }
        }
        req.setStorage(storage);
        
        // Display details
        req.setScreenSize(parseScreenSize(product.getDisplay()));
        req.setScreenResolution(product.getDisplay() != null ? product.getDisplay() : "Unknown");
        req.setBatteryLife(product.getBattery() != null ? product.getBattery() : "Unknown");
        req.setWeight(1.8); // Default fallback weight in kg
        
        // OS
        String os = "Windows 11";
        if (product.getName() != null && product.getName().toLowerCase().contains("macbook")) {
            os = "macOS";
        }
        req.setOperatingSystem(os);
        
        // URL
        req.setUrl("/product/" + product.getId());
        
        // Image URL
        String imageUrl = "";
        if (product.getImages() != null && !product.getImages().isBlank()) {
            String[] imgArray = product.getImages().split(",");
            if (imgArray.length > 0) {
                imageUrl = imgArray[0].trim();
            }
        }
        req.setImageUrl(imageUrl);
        req.setDescription(product.getDescription() != null ? product.getDescription() : "");
        
        // isHot
        boolean isHot = product.getBadge() != null && product.getBadge().equalsIgnoreCase("HOT");
        req.setIsHot(isHot);
        
        // Stock
        int stock = 0;
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            stock = product.getVariants().stream()
                    .mapToInt(v -> v.getStock() != null ? v.getStock() : 0)
                    .sum();
        } else if (product.getStock() != null) {
            stock = product.getStock();
        }
        req.setStock(stock);
        req.setCategory("laptop");
        
        // Use case tags
        req.setUseCaseTags(inferUseCaseTags(product, req.getRam()));
        
        return req;
    }

    private Integer parseRam(String ramStr) {
        if (ramStr == null || ramStr.isBlank()) return 8;
        try {
            String cleaned = ramStr.replaceAll("[^0-9]", "");
            if (!cleaned.isEmpty()) {
                return Integer.parseInt(cleaned);
            }
        } catch (Exception e) {
            // ignore
        }
        return 8;
    }

    private Double parseScreenSize(String displayStr) {
        if (displayStr == null || displayStr.isBlank()) return 15.6;
        try {
            Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)");
            Matcher matcher = pattern.matcher(displayStr);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {
            // ignore
        }
        return 15.6;
    }

    private String inferUseCaseTags(Product product, Integer ramVal) {
        String gpuLower = (product.getGpu() != null) ? product.getGpu().toLowerCase() : "";
        String nameLower = (product.getName() != null) ? product.getName().toLowerCase() : "";
        String cpuLower = (product.getCpu() != null) ? product.getCpu().toLowerCase() : "";
        
        List<String> tags = new ArrayList<>();
        
        boolean isGaming = gpuLower.contains("rtx") || gpuLower.contains("gtx") || gpuLower.contains("radeon") || gpuLower.contains("gaming")
                || nameLower.contains("gaming") || nameLower.contains("loq") || nameLower.contains("tuf") || nameLower.contains("rog")
                || nameLower.contains("katana") || nameLower.contains("g15");
        if (isGaming) {
            tags.add("gaming");
        }
        
        boolean isCpuStrong = cpuLower.contains("i7") || cpuLower.contains("i9") || cpuLower.contains("ryzen 7") || cpuLower.contains("ryzen 9") 
                || cpuLower.contains("m1 pro") || cpuLower.contains("m2 pro") || cpuLower.contains("m3 pro") 
                || cpuLower.contains("m1 max") || cpuLower.contains("m2 max") || cpuLower.contains("m3 max") 
                || cpuLower.contains("m3 ultra") || cpuLower.contains("ultra 7") || cpuLower.contains("ultra 9");
        if (ramVal >= 16 && isCpuStrong) {
            tags.add("programming");
        }
        
        boolean isThinAndLight = nameLower.contains("gram") || nameLower.contains("air") || nameLower.contains("slim") 
                || nameLower.contains("zenbook") || nameLower.contains("vivobook") || nameLower.contains("inspiron") 
                || nameLower.contains("pavilion") || nameLower.contains("yoga");
        if (!isGaming || isThinAndLight) {
            tags.add("office");
            tags.add("student");
        }
        
        return String.join(", ", tags);
    }
}
