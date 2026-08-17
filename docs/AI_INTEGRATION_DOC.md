# TÀI LIỆU TÍCH HỢP HỆ THỐNG VÀ ĐẶC TẢ API (DÀNH CHO AI)
## Hướng dẫn dành cho AI đọc hiểu dự án Product-Comparison-RAG để tích hợp vào Web Laptop Store

Tài liệu này được biên soạn đặc biệt để cung cấp toàn bộ bối cảnh dự án, kiến trúc hệ thống, cơ chế hoạt động bên trong của RAG pipeline, và đặc tả API đầy đủ, chi tiết. Mục tiêu là giúp một AI khác (hoặc lập trình viên) đọc hiểu ngay lập tức và tiến hành tích hợp RAG microservice này vào một hệ thống Web bán Laptop viết bằng Java Spring Boot (hoặc bất kỳ ngôn ngữ nào) một cách trơn tru.

---

## 1. TỔNG QUAN HỆ THỐNG (SYSTEM OVERVIEW)

Hệ thống RAG tư vấn và so sánh sản phẩm laptop được thiết kế theo kiến trúc chia tách trách nhiệm (Separation of Concerns):

1. **Java Spring Boot (Hệ thống Web chính)**:
   - Quản lý giao diện người dùng (UI/UX), giỏ hàng, đơn hàng, người dùng (Spring Security) và cơ sở dữ liệu quan hệ (MySQL/PostgreSQL) lưu thông tin sản phẩm đầy đủ.
   - Đóng vai trò là hệ thống master. Khi có thay đổi về sản phẩm (thêm, sửa, xóa, thay đổi số lượng kho), Spring Boot sẽ đồng bộ sang RAG Service.
2. **Python FastAPI Microservice (AI RAG Engine)**:
   - Đóng vai trò là một AI Agent chuyên biệt. Microservice này quản lý cơ sở dữ liệu vector (**ChromaDB**), phân tích câu hỏi tự nhiên (**Query Parser**), chấm điểm sản phẩm (**Scoring Engine**), giao tiếp với Mô hình ngôn ngữ lớn (**LLM** - Gemini hoặc OpenAI) để sinh ra phản hồi có cấu trúc (Structured Output).
   - Tự động kiểm tra chéo và sửa chữa thông tin (Anti-hallucination) trước khi gửi kết quả về Spring Boot.

### Sơ đồ luồng hoạt động tích hợp:
```
┌──────────┐  1. Câu hỏi tự nhiên   ┌─────────────────────┐  2. REST HTTP POST  ┌─────────────────────────────┐
│ Khách    │────────────────────────>│  Spring Boot Web    │────────────────────>│ Python FastAPI RAG Service  │
│ Hàng     │<────────────────────────│  (Cổng 8080)        │<───────────────────│ (Cổng 8000)                 │
└──────────┘  6. Render câu trả lời  └─────────────────────┘   5. JSON Response  └─────────────────────────────┘
                 & sản phẩm gợi ý                                (camelCase DTO)               │
                                                                                               ▼
                                                                                 ┌─────────────────────────────┐
                                                                                 │ RAG Pipeline:               │
                                                                                 │ 1. Parse intent & filters   │
                                                                                 │ 2. Metadata filtering       │
                                                                                 │ 3. Vector search ChromaDB   │
                                                                                 │ 4. Scoring & Re-ranking     │
                                                                                 │ 5. LLM structured generation│
                                                                                 │ 6. Anti-hallucination repair│
                                                                                 └─────────────────────────────┘
```

---

## 2. RAG PIPELINE - CƠ CHẾ HOẠT ĐỘNG CHI TIẾT

Khi nhận được câu hỏi từ khách hàng qua endpoint `/api/chat`, RAG Service sẽ xử lý qua 6 bước (modules) nghiêm ngặt sau:

### Bước 1: Phân tích câu hỏi (Query Parser)
Sử dụng các biểu thức chính quy (Regex) và luật heuristic (rule-based) để phân tích câu hỏi tự nhiên thành các tiêu chí có cấu trúc mà không cần gọi API LLM (giúp tối ưu tốc độ và chi phí):
* **Trích xuất giá**: Nhận diện các cấu trúc giá như "dưới 20 triệu", "trên 15tr", hoặc "khoảng 25 triệu". Đối với cấu trúc "khoảng X triệu", hệ thống tự động tính toán biên độ dao động $\pm 15\%$ (ví dụ: "khoảng 20 triệu" $\rightarrow$ tối thiểu 17.000.000 VND và tối đa 23.000.000 VND).
* **Trích xuất RAM**: Nhận diện các cấu trúc RAM (ví dụ: "RAM 16GB", "16gb ram").
* **Trích xuất CPU/GPU**: Trích xuất các keyword CPU (như *i5, i7, ryzen 7, m3 pro*) và GPU (như *rtx 4060, gtx 1650*).
* **Xác định mục đích sử dụng (Use Case)**: Phân loại câu hỏi vào các nhóm: `gaming`, `design` (đồ họa), `office` (văn phòng), `student` (học tập), `programming` (lập trình), `lightweight` (mỏng nhẹ).
* **Xác định Intent Mode**:
  - `recommendation`: Tư vấn gợi ý sản phẩm phù hợp.
  - `comparison`: So sánh cụ thể. Chế độ này được kích hoạt khi câu hỏi chứa từ khóa so sánh (như *so sánh, vs, hay cái nào tốt hơn*) và trích xuất danh sách tên máy cần so sánh trong mảng `comparisonProducts`.

### Bước 2: Xây dựng bộ lọc cứng (Metadata Filter)
ChromaDB cho phép lọc siêu dữ liệu trước khi thực hiện tìm kiếm vector. RAG Service sẽ biên dịch các tiêu chí đã trích xuất từ Bước 1 thành biểu thức query của ChromaDB:
* Lọc thương hiệu: `{"brand": brand_name}`
* Lọc RAM tối thiểu: `{"ram": {"$gte": min_ram}}`
* Lọc khoảng giá: `{"$and": [{"price": {"$gte": min_price}}, {"price": {"$lte": max_price}}]}`

### Bước 3: Tìm kiếm Vector (Vector Search)
* Hệ thống tiến hành vector hóa câu truy vấn ngữ nghĩa đã được làm phong phú (Semantic Query) bằng mô hình embedding (`text-embedding-004` của Gemini hoặc `text-embedding-3-small` của OpenAI).
* Thực hiện tìm kiếm top-K sản phẩm tương đồng ngữ nghĩa nhất trong tập dữ liệu ChromaDB đã qua bộ lọc cứng ở Bước 2.
* **Chiến lược dự phòng (Fallback Strategy)**: Nếu bộ lọc quá chặt dẫn đến không tìm thấy sản phẩm nào, hệ thống tự động thực hiện lại tìm kiếm vector trên toàn bộ cơ sở dữ liệu mà không áp dụng bộ lọc cứng để tránh trả về kết quả trống.

### Bước 4: Bộ chấm điểm nghiệp vụ (Scoring Engine & Re-ranking)
Để khắc phục hạn chế của Vector Search thông thường (chỉ dựa trên sự tương đồng ngữ nghĩa), Scoring Engine sẽ xếp hạng lại danh sách sản phẩm theo công thức chấm điểm nghiệp vụ thực tế:
$$\text{Score} = (\text{Vector Cosine Similarity} \times 5.0) + \text{Bonus Scores} + \text{Penalties}$$

Các quy tắc tính điểm cụ thể:
1. **Cùng thương hiệu yêu cầu**: $+3.0$ điểm.
2. **Khớp tầm giá tham chiếu ($\pm 20\%$ giá yêu cầu)**: $+2.0$ điểm.
3. **Khớp dòng CPU**: $+2.0$ điểm.
4. **Khớp GPU chính xác (Keyword match)**: $+3.0$ điểm.
5. **Sản phẩm đang HOT (`isHot = true`)**: $+1.0$ điểm.
6. **Khớp dung lượng RAM**:
   - Nếu $\text{RAM} \ge \text{RAM yêu cầu}$: $+2.0$ điểm.
   - Nếu $\text{RAM} < \text{RAM yêu cầu}$: $-1.0$ điểm (phạt).
7. **Khớp GPU theo Use Case**:
   - Nhu cầu `gaming` hoặc `design` mà máy có card đồ họa rời (RTX, GTX, Radeon...): $+2.0$ điểm.
   - Nhu cầu `gaming` hoặc `design` mà máy chỉ dùng card tích hợp (Intel Iris Xe, Radeon Graphics...): $-1.0$ điểm (phạt).
8. **Khớp Use Case / Intent chi tiết**:
   - `gaming`: Máy có RAM $\ge 16GB$ ($+1.0$đ), CPU dòng hiệu năng cao ($+1.0$đ), tags chứa "gaming" ($+1.0$đ).
   - `office`: Giá máy $\le 25$ triệu ($+1.0$đ), RAM $\ge 8GB$ ($+1.0$đ), CPU tiết kiệm điện dòng U hoặc Core Ultra/Apple Silicon ($+1.0$đ), card đồ họa tích hợp ($+1.0$đ).
   - `design`: RAM $\ge 16GB$ ($+1.0$đ), CPU mạnh ($+1.0$đ), màn hình chất lượng cao như OLED, Retina, IPS 2K/4K, độ phủ màu rộng ($+1.0$đ).
   - `programming`: RAM $\ge 16GB$ ($+1.0$đ), ổ cứng SSD ($+1.0$đ), CPU mạnh ($+1.0$đ).
9. **Kiểm tra kho hàng (Stock status)**:
   - Nếu sản phẩm còn hàng (`stock > 0`): $+0.5$ điểm.
   - Nếu sản phẩm hết hàng (`stock = 0`): $-5.0$ điểm (phạt rất nặng để đẩy xuống cuối danh sách tư vấn).

### Bước 5: Gọi LLM tạo phản hồi có cấu trúc (LLM Generation)
* Đưa danh sách các laptop tốt nhất sau khi đã xếp hạng kèm theo thông số chi tiết của chúng vào context của Prompt.
* Sử dụng tính năng **Structured Output** của LangChain (`with_structured_output`) ép LLM (Gemini hoặc OpenAI) trả về chính xác định dạng JSON theo schema Pydantic, loại bỏ hoàn toàn việc trả về văn bản tự do dễ gây vỡ giao diện.

### Bước 6: Kiểm tra chống ảo giác & Sửa lỗi (Anti-hallucination & Repair Layer)
Để đảm bảo LLM không bao giờ bịa đặt (hallucinate) thông tin sản phẩm, RAG Service có một lớp hậu xử lý cứng:
* Duyệt qua danh sách `recommendedProducts` do LLM gợi ý.
* Lọc bỏ mọi sản phẩm có ID không nằm trong danh sách ứng viên thực tế tìm thấy trong ChromaDB ở Bước 3.
* Đối với các sản phẩm hợp lệ, lấy thông tin gốc từ database (Tên, Giá, URL, Image URL) để điền đè lên kết quả của LLM. Tránh trường hợp LLM sửa tên hoặc viết sai giá.
* Nếu toàn bộ sản phẩm bị LLM lọc bỏ hoặc không hợp lệ, hệ thống sẽ trả về danh sách trống và tự động thay đổi nội dung trả lời thành câu thông báo chuẩn hóa.

---

## 3. ĐẶC TẢ CHI TIẾT CÁC API ENDPOINTS

Mặc định, microservice chạy trên cổng `8000`. Toàn bộ dữ liệu JSON trao đổi đều sử dụng định dạng đặt tên **camelCase** để tương thích hoàn hảo với Java Spring Boot.

### 3.1 Health Check
* **Endpoint**: `GET /health`
* **Mục đích**: Kiểm tra tình trạng hoạt động và cấu hình hiện tại của microservice.
* **Response (200 OK)**:
  ```json
  {
    "status": "healthy",
    "service": "Product-Comparison-RAG",
    "version": "1.0.0",
    "provider": "gemini",
    "llm_model": "gemini-2.0-flash"
  }
  ```

### 3.2 Thống kê Database
* **Endpoint**: `GET /api/stats`
* **Mục đích**: Kiểm tra tổng số lượng sản phẩm đang có trong cơ sở dữ liệu vector.
* **Response (200 OK)**:
  ```json
  {
    "status": "success",
    "totalProductsInVectorDb": 15,
    "collectionName": "laptops"
  }
  ```

### 3.3 Đồng bộ một sản phẩm (Upsert Single Product)
* **Endpoint**: `POST /api/sync-product`
* **Mục đích**: Thêm mới hoặc cập nhật thông tin chi tiết của một sản phẩm laptop từ Spring Boot sang ChromaDB. Khi admin tạo/sửa sản phẩm, cần gọi API này.
* **Request Body (JSON)**:
  ```json
  {
    "id": 1,
    "name": "Asus ROG Zephyrus G14 (2024) GA403UV",
    "brand": "Asus",
    "price": 34990000.0,
    "ram": 16,
    "cpu": "AMD Ryzen 9 8945HS",
    "gpu": "NVIDIA GeForce RTX 4060 8GB",
    "storage": "1TB NVMe SSD",
    "screenSize": 14.0,
    "screenResolution": "2560x1600 OLED 120Hz",
    "batteryLife": "10 giờ",
    "weight": 1.65,
    "operatingSystem": "Windows 11 Home",
    "url": "/product/1",
    "imageUrl": "/images/asus-rog-g14.jpg",
    "description": "Laptop gaming cao cấp mỏng nhẹ màn hình OLED sắc nét.",
    "isHot": true,
    "useCaseTags": "gaming, design, portable",
    "stock": 15,
    "category": "Laptop Gaming"
  }
  ```
  *(Lưu ý: `id`, `name`, `brand`, `price`, `ram`, `cpu`, `storage`, `url` là các trường bắt buộc).*
* **Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Đồng bộ sản phẩm 'Asus ROG Zephyrus G14 (2024) GA403UV' (ID=1) thành công.",
    "productId": 1
  }
  ```

### 3.4 Đồng bộ hàng loạt sản phẩm (Bulk Sync Products)
* **Endpoint**: `POST /api/sync-product/bulk`
* **Mục đích**: Đồng bộ danh sách lớn sản phẩm cùng lúc. Dùng khi import dữ liệu ban đầu từ Spring Boot sang RAG.
* **Request Body (JSON)**:
  ```json
  {
    "products": [
      {
        "id": 1,
        "name": "Asus ROG Zephyrus G14",
        "brand": "Asus",
        "price": 34990000.0,
        "ram": 16,
        "cpu": "AMD Ryzen 9",
        "storage": "1TB SSD",
        "url": "/product/1",
        "imageUrl": "/images/rog-g14.jpg",
        "stock": 12
      },
      {
        "id": 2,
        "name": "MSI Katana 15 B13VFK",
        "brand": "MSI",
        "price": 24990000.0,
        "ram": 16,
        "cpu": "Intel Core i7",
        "storage": "512GB SSD",
        "url": "/product/2",
        "imageUrl": "/images/katana-15.jpg",
        "stock": 5
      }
    ]
  }
  ```
* **Response (200 OK)**:
  ```json
  {
    "success": true,
    "total": 2,
    "successCount": 2,
    "failedCount": 0,
    "results": [
      {
        "id": 1,
        "success": true,
        "message": "Synced successfully"
      },
      {
        "id": 2,
        "success": true,
        "message": "Synced successfully"
      }
    ]
  }
  ```

### 3.5 Xóa sản phẩm khỏi Vector DB
* **Endpoint**: `DELETE /api/sync-product/{id}`
* **Mục đích**: Gỡ bỏ sản phẩm ra khỏi không gian vector tìm kiếm ChromaDB khi admin xóa sản phẩm ở trang chính.
* **Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Đã xóa sản phẩm ID=1 khỏi vector database.",
    "productId": 1
  }
  ```
* **Response (404 Not Found)**:
  ```json
  {
    "error": "Sản phẩm ID=999 không tồn tại trong vector database.",
    "statusCode": 404
  }
  ```

### 3.6 Chat tư vấn & So sánh (Core Chat Service)
* **Endpoint**: `POST /api/chat`
* **Mục đích**: Nhận câu hỏi tự nhiên từ người dùng và trả về phân tích tư vấn hoặc so sánh laptop dưới dạng JSON cấu trúc.
* **Request Body (JSON)**:
  ```json
  {
    "message": "Tôi muốn mua laptop gaming dưới 25 triệu RAM 16GB",
    "sessionId": "user-session-uuid-12345",
    "topK": 5
  }
  ```
* **Response (200 OK)**:
  ```json
  {
    "answer": "Dựa trên nhu cầu gaming với ngân sách dưới 25 triệu và RAM từ 16GB, dưới đây là các lựa chọn phù hợp nhất cho bạn:\n\n1. **MSI Katana 15 B13VFK (24.990.000 VND)**: Máy được trang bị card đồ họa rời RTX 4060 8GB mạnh mẽ kết hợp cùng CPU Intel i7 thế hệ 13 mang lại khả năng chơi mượt mà các tựa game AAA.\n2. **Lenovo LOQ 15APH9 (21.990.000 VND)**: Đây là lựa chọn cực kỳ tiết kiệm với chip Ryzen 7 và GPU RTX 4060 8GB, đem lại cấu hình tối ưu nhất trong tầm giá dưới 22 triệu.\n\nCả hai máy đều đáp ứng tốt yêu cầu RAM 16GB để bạn đa nhiệm và chơi game mượt mà.",
    "confidenceScore": 0.95,
    "citations": [
      "Thông số từ LAPTOPSTORE ID #2 - MSI Katana 15 B13VFK",
      "Thông số từ LAPTOPSTORE ID #3 - Lenovo LOQ 15APH9"
    ],
    "missingInformation": [
      "Bạn có ưu tiên kích thước màn hình lớn (15.6 inch) hay cần mỏng nhẹ di động hơn?"
    ],
    "recommendedProducts": [
      {
        "id": 3,
        "name": "Lenovo LOQ 15APH9",
        "price": 21990000.0,
        "url": "/product/3",
        "imageUrl": "/images/lenovo-loq-15.jpg",
        "reason": "Mức giá cực tốt cho cấu hình RTX 4060 rời và RAM 16GB."
      },
      {
        "id": 2,
        "name": "MSI Katana 15 B13VFK",
        "price": 24990000.0,
        "url": "/product/2",
        "imageUrl": "/images/msi-katana-15.jpg",
        "reason": "Hiệu năng CPU i7 thế hệ 13 nhỉnh hơn giúp xử lý game nặng và livestream tốt hơn."
      }
    ]
  }
  ```

### 3.7 Debug Query Parser
* **Endpoint**: `POST /api/parse-query-debug`
* **Mục đích**: Xem thông tin trích xuất sau khi đi qua Bộ phân tích cú pháp (dành cho phát triển/gỡ lỗi).
* **Request**: `{"message": "Cần laptop gaming asus rtx 4060 khoảng 20tr đến 30tr"}`
* **Response**:
  ```json
  {
    "maxPrice": 30000000.0,
    "minPrice": 20000000.0,
    "brand": "asus",
    "minRam": null,
    "cpuKeyword": null,
    "gpuKeyword": "rtx 4060",
    "minStorageGb": null,
    "semanticQuery": "laptop gaming asus rtx 4060 20tr 30tr laptop gaming hiệu năng cao GPU RTX card rời frame rate cao",
    "useCase": "gaming",
    "mode": "recommendation",
    "comparisonProducts": [],
    "rawQuery": "Cần laptop gaming asus rtx 4060 khoảng 20tr đến 30tr"
  }
  ```

### 3.8 Debug Search & Scoring (Chuyên sâu)
* **Endpoint**: `POST /api/search-debug`
* **Mục đích**: Trả về danh sách ứng viên kèm theo diễn giải chi tiết cách tính điểm của Scoring Engine (dành cho kiểm thử độ chính xác).
* **Response**: Trả về thông tin phân tích cú pháp, metadata filter tương ứng và mảng `candidates` chứa chi tiết điểm số (`similarityScore`, `bonusScore`, `totalScore`, `scoreDetails`).

---

## 4. HƯỚNG DẪN TÍCH HỢP PHÍA WEB SPRING BOOT

Để tích hợp, nhà phát triển (hoặc AI) cần triển khai các phần sau trên dự án Java Spring Boot.

### 4.1 Định nghĩa các Data Transfer Objects (DTOs) tương thích

#### 1. ProductSyncRequestDto.java (Đồng bộ sản phẩm)
```java
package com.example.laptopstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSyncRequestDto {
    private Long id;
    private String name;
    private String brand;
    private BigDecimal price;
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
```

#### 2. ChatRequestDto.java
```java
package com.example.laptopstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDto {
    private String message;
    private String sessionId;
    private Integer topK;
}
```

#### 3. RecommendedProductDto.java (Sản phẩm gợi ý trong chat)
```java
package com.example.laptopstore.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RecommendedProductDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private String url;
    private String imageUrl;
    private String reason;
}
```

#### 4. ChatResponseDto.java (Nhận phản hồi từ RAG)
```java
package com.example.laptopstore.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatResponseDto {
    private String answer;
    private Double confidenceScore;
    private List<String> citations;
    private List<String> missingInformation;
    private List<RecommendedProductDto> recommendedProducts;
}
```

### 4.2 Thiết lập RagIntegrationService.java (Spring Boot Client)

Sử dụng `WebClient` của Spring Boot WebFlux để gửi nhận dữ liệu REST HTTP không đồng bộ / đồng bộ:

```java
package com.example.laptopstore.service;

import com.example.laptopstore.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class RagIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(RagIntegrationService.class);
    private final WebClient webClient;

    public RagIntegrationService(
            WebClient.Builder webClientBuilder,
            @Value("${rag.service.url:http://localhost:8000}") String ragServiceUrl) {
        this.webClient = webClientBuilder
                .baseUrl(ragServiceUrl)
                .build();
    }

    /**
     * Đồng bộ thông tin sản phẩm đơn lẻ sang RAG service
     */
    public SyncResponseDto syncProduct(ProductSyncRequestDto productDto) {
        log.info("Gửi yêu cầu đồng bộ sản phẩm ID: {}", productDto.getId());
        try {
            return this.webClient.post()
                    .uri("/api/sync-product")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(productDto)
                    .retrieve()
                    .bodyToMono(SyncResponseDto.class)
                    .block(); // Chặn đồng bộ để đơn giản hóa luồng nghiệp vụ admin
        } catch (Exception e) {
            log.error("Đồng bộ thất bại sản phẩm ID: {}. Lỗi: {}", productDto.getId(), e.getMessage());
            return new SyncResponseDto(false, "Lỗi kết nối RAG: " + e.getMessage(), productDto.getId());
        }
    }

    /**
     * Đồng bộ hàng loạt sản phẩm sang RAG service (Import ban đầu)
     */
    public BulkSyncResponseDto bulkSyncProducts(List<ProductSyncRequestDto> products) {
        log.info("Gửi yêu cầu đồng bộ hàng loạt {} sản phẩm", products.size());
        BulkSyncRequestDto request = new BulkSyncRequestDto(products);
        try {
            return this.webClient.post()
                    .uri("/api/sync-product/bulk")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(BulkSyncResponseDto.class)
                    .block();
        } catch (Exception e) {
            log.error("Đồng bộ hàng loạt thất bại. Lỗi: {}", e.getMessage());
            BulkSyncResponseDto errorResponse = new BulkSyncResponseDto();
            errorResponse.setSuccess(false);
            errorResponse.setTotal(products.size());
            errorResponse.setSuccessCount(0);
            errorResponse.setFailedCount(products.size());
            return errorResponse;
        }
    }

    /**
     * Gửi tin nhắn chat tư vấn khách hàng sang RAG service
     */
    public ChatResponseDto chat(String message, String sessionId, Integer topK) {
        log.info("Gửi câu hỏi tư vấn: '{}' | Session: {}", message, sessionId);
        ChatRequestDto request = ChatRequestDto.builder()
                .message(message)
                .sessionId(sessionId)
                .topK(topK)
                .build();
        try {
            return this.webClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatResponseDto.class)
                    .block();
        } catch (Exception e) {
            log.error("Lỗi khi gọi endpoint RAG chat: {}", e.getMessage());
            ChatResponseDto errorResponse = new ChatResponseDto();
            errorResponse.setAnswer("Rất tiếc, trợ lý ảo tư vấn laptop đang bận. Vui lòng thử lại sau ít phút.");
            errorResponse.setConfidenceScore(0.0);
            return errorResponse;
        }
    }

    /**
     * Xóa sản phẩm khỏi RAG Vector DB
     */
    public SyncResponseDto deleteProduct(Long productId) {
        log.info("Gửi yêu cầu xóa sản phẩm ID: {}", productId);
        try {
            return this.webClient.delete()
                    .uri("/api/sync-product/{id}", productId)
                    .retrieve()
                    .bodyToMono(SyncResponseDto.class)
                    .block();
        } catch (Exception e) {
            log.error("Xóa sản phẩm ID: {} khỏi RAG thất bại. Lỗi: {}", productId, e.getMessage());
            return new SyncResponseDto(false, "Lỗi khi xóa sản phẩm trên RAG: " + e.getMessage(), productId);
        }
    }
}
```

### 4.3 Các điểm chạm nghiệp vụ (Business Touchpoints) cần chèn code gọi RAG

1. **Khi Admin Thêm/Sửa sản phẩm**:
   - Sau khi Spring Boot lưu thành công vào MySQL/PostgreSQL, hãy lấy thực thể sản phẩm vừa được lưu, ánh xạ qua `ProductSyncRequestDto` và gọi `ragIntegrationService.syncProduct(dto)`.
2. **Khi Admin Xóa sản phẩm**:
   - Sau khi xóa thành công sản phẩm trong DB chính, gọi `ragIntegrationService.deleteProduct(productId)`.
3. **Khi Admin Import Excel sản phẩm**:
   - Sử dụng batch (ví dụ: gom 50 sản phẩm một lượt), gọi `ragIntegrationService.bulkSyncProducts(list)`.
4. **Tại Trang Giao Diện Chatbot Hỗ Trợ (User Portal)**:
   - Khi khách hàng gửi tin nhắn vào khung chat, Spring Boot Controller tiếp nhận và chuyển tiếp sang `ragIntegrationService.chat(...)`.
   - Lấy câu trả lời định dạng Markdown hiển thị lên khung chat cho khách hàng.
   - Kiểm tra xem mảng `recommendedProducts` có chứa sản phẩm gợi ý hay không. Nếu có, hãy render các sản phẩm này thành dạng **Thẻ Sản phẩm (Cards Carousel)** ngay dưới câu trả lời của bot, bao gồm: Ảnh sản phẩm (`imageUrl`), Tên sản phẩm (`name`), Giá tiền (`price`) và nút "Xem Chi Tiết" trỏ trực tiếp đến `url` của trang chi tiết laptop trên web chính.

---

## 5. CẤU HÌNH BIẾN MÔI TRƯỜNG & KIỂM THỬ NHANH

### 5.1 Cấu hình file `.env` (Đặt tại thư mục gốc của RAG Microservice)

```env
# Nhà cung cấp LLM: 'gemini' hoặc 'openai'
LLM_PROVIDER=gemini

# Điền Google API Key lấy từ Google AI Studio (bắt buộc nếu dùng gemini)
GOOGLE_API_KEY=your-actual-api-key-here

# Điền OpenAI API Key (bắt buộc nếu dùng openai)
# OPENAI_API_KEY=sk-your-openai-key-here

# Tên mô hình LLM & Embedding tương ứng
LLM_MODEL_NAME=gemini-2.0-flash
EMBEDDING_MODEL_NAME=models/text-embedding-004

# Cấu hình ChromaDB lưu trữ cục bộ
CHROMA_DB_PATH=./chroma_db
CHROMA_COLLECTION_NAME=laptops

# Cấu hình Host/Port cho FastAPI
HOST=0.0.0.0
PORT=8000

# CORS Allowed Origins: Trỏ về địa chỉ Frontend hoặc Spring Boot Web chính
ALLOWED_ORIGINS=http://localhost:8080

# Chế độ Mock: Đặt thành true để kiểm thử offline không cần API Key / Internet
MOCK_EMBEDDINGS=false
```

### 5.2 Hướng dẫn Test Offline (Mock Mode)
Nếu không có API Key hoặc mất kết nối mạng, hãy đặt biến môi trường `MOCK_EMBEDDINGS=true`.
* Server sẽ khởi tạo một Mock Embedding model ảo (trả về vector có độ dài cố định 768 chiều).
* LLM sẽ sử dụng Mock Structured output trả về phản hồi mẫu định sẵn, giúp bạn vẫn test được kết nối giữa Spring Boot và FastAPI bình thường mà không bị báo lỗi thiếu key.

### 5.3 Gọi Test nhanh bằng lệnh curl
* **Chat**:
  ```bash
  curl -X POST http://localhost:8000/api/chat \
    -H "Content-Type: application/json" \
    -d '{"message": "Laptop dưới 20 triệu", "sessionId": "test-session"}'
  ```
* **Stats**:
  ```bash
  curl http://localhost:8000/api/stats
  ```
* **Health Check**:
  ```bash
  curl http://localhost:8000/health
  ```
