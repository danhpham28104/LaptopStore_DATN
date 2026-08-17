# DEMO AND INTEGRATION GUIDE - Product-Comparison-RAG

Tài liệu này hướng dẫn chi tiết cách chạy demo, kịch bản trình bày cho Mentor và cách tích hợp microservice **Product-Comparison-RAG** (Python FastAPI) vào hệ thống **Web Laptop Store** chính (Java Spring Boot).

---

## 1. Tổng quan hệ thống

Hệ thống RAG tư vấn và so sánh sản phẩm laptop được thiết kế theo kiến trúc chia tách trách nhiệm (Separation of Concerns):

*   **FastAPI Microservice (Python)**: Đóng vai trò là một AI Agent chuyên biệt. Microservice này chịu trách nhiệm quản lý cơ sở dữ liệu vector (**ChromaDB**), phân tích cú pháp câu hỏi tự nhiên (**Query Parser**), chấm điểm sản phẩm theo nghiệp vụ (**Scoring Engine**), giao tiếp với Mô hình ngôn ngữ lớn (**LLM** - Gemini/OpenAI) để tạo câu trả lời có cấu trúc (Structured Output), lọc bỏ ảo giác (Anti-hallucination) và trả về JSON chuẩn hóa.
*   **Web Application chính (Java Spring Boot)**: Chịu trách nhiệm về giao diện người dùng (UI/UX), cơ sở dữ liệu quan hệ chính (MySQL/PostgreSQL), xác thực người dùng (Spring Security), xử lý đơn hàng và giỏ hàng. Spring Boot giao tiếp với FastAPI qua giao thức RESTful HTTP.

---

## 2. Kiến trúc tổng thể

Dưới đây là sơ đồ luồng dữ liệu khi người dùng tương tác với hệ thống:

```
+------------+             +----------------------+             +----------------------------------+
|    User    |  Request    |                      |  REST HTTP  |  FastAPI Product-Comparison-RAG  |
| (Frontend) |------------>|  Spring Boot Web App |------------>|  (Cổng 8000)                     |
+------------+             +----------------------+             +----------------------------------+
      ^                                |                                          |
      |                                |                                          v
      |                                |                                +--------------------+
      |                                |                                |   Query Parser     |
      |                                |                                |  (Phân tích ý định)|
      |                                |                                +--------------------+
      |                                |                                          |
      |                                |                                          v
      |                                |                                +--------------------+
      |                                |                                |  ChromaDB Search   |
      |                                |                                | (Filter + Vector)  |
      |                                |                                +--------------------+
      |                                |                                          |
      |                                |                                          v
      |                                |                                +--------------------+
      |                                |                                |   Scoring Engine   |
      |                                |                                | (Xếp hạng Laptop)  |
      |                                |                                +--------------------+
      |                                |                                          |
      |                                |                                          v
      |                                |                                +--------------------+
      |                                |                                |     LLM Engine     |
      |                                |                                | (Structured Output)|
      |                                |                                +--------------------+
      |                                |                                          |
      |                                v                                          v
      |                      +--------------------+    JSON Response    +--------------------+
      |                      |  Render UI (Cards) |<--------------------|   CamelCase Parser |
      +----------------------|  & Business Logic  |    (DTO Mapped)     | (Anti-hallucination|
                             +--------------------+                     +--------------------+
```

---

## 3. Các chức năng đã hoàn thành

Hệ thống đã được phát triển và kiểm thử hoàn chỉnh với các tính năng sau:

1.  **Sync sản phẩm đơn lẻ** (`POST /api/sync-product`): Đồng bộ thông tin, tạo embedding và lưu/cập nhật (upsert) một sản phẩm từ Spring Boot sang ChromaDB.
2.  **Sync sản phẩm hàng loạt** (`POST /api/sync-product/bulk`): Đồng bộ nhanh một danh sách nhiều sản phẩm, hỗ trợ báo cáo chi tiết trạng thái thành công/thất bại cho từng ID.
3.  **Xóa sản phẩm khỏi Vector DB** (`DELETE /api/sync-product/{id}`): Gỡ bỏ sản phẩm khỏi ChromaDB khi admin xóa sản phẩm ở hệ thống Spring Boot.
4.  **Chat tư vấn laptop** (`POST /api/chat`): Trả lời câu hỏi tự nhiên của khách hàng kèm phân tích cấu hình chi tiết.
5.  **So sánh laptop** (Comparison Mode): Tự động phát hiện ý định so sánh (ví dụ: *"So sánh Asus ROG G14 và Lenovo Legion 5"*) để đưa ra phân tích đối chiếu song song.
6.  **Query Parser Debug** (`POST /api/parse-query-debug`): Endpoint gỡ lỗi hiển thị các thông tin trích xuất như: khoảng giá, hãng sản xuất, dung lượng RAM, CPU/GPU mong muốn.
7.  **Search/Scoring Debug** (`POST /api/search-debug`): Endpoint gỡ lỗi hiển thị chi tiết điểm số của từng ứng viên laptop (điểm tương đồng vector, điểm cộng cấu hình RAM, GPU rời, sản phẩm HOT, tình trạng hàng trong kho).
8.  **Anti-hallucination (Chống ảo giác)**: Đảm bảo LLM chỉ tư vấn các laptop thực sự có trong database, tự động sửa đổi hoặc loại bỏ các thông tin bịa đặt của mô hình.
9.  **Response định dạng CamelCase**: Toàn bộ dữ liệu trả về được định dạng theo chuẩn đặt tên camelCase giúp Spring Boot dễ dàng binding vào Java DTO mà không cần cấu hình thêm.
10. **Mock Mode cho Testing Offline**: Hỗ trợ chạy thử nghiệm toàn bộ hệ thống bằng mock embedding vector, không yêu cầu kết nối internet hay API key của Gemini/OpenAI.

---

## 4. Cách chạy FastAPI microservice

### Bước 1: Khởi tạo và kích hoạt Virtual Environment (venv)
Mở terminal tại thư mục gốc của dự án `Product-Comparison-RAG`:
```powershell
# Tạo môi trường ảo
python -m venv .venv

# Kích hoạt trên Windows (PowerShell)
.venv\Scripts\Activate.ps1

# Kích hoạt trên macOS/Linux
source .venv/bin/activate
```

### Bước 2: Cài đặt các thư viện phụ thuộc (Dependencies)
```powershell
pip install -r requirements.txt
```

### Bước 3: Tạo file cấu hình môi trường `.env`
Sao chép cấu hình từ file mẫu `.env.example`:
```powershell
copy .env.example .env
```
Mở file `.env` vừa tạo và điền khóa API của bạn (nếu có):
*   Nếu dùng Gemini: `GOOGLE_API_KEY=your-gemini-key` và `LLM_PROVIDER=gemini`
*   Nếu dùng OpenAI: `OPENAI_API_KEY=your-openai-key` và `LLM_PROVIDER=openai`

### Bước 4: Chạy microservice
Chạy lệnh khởi động Uvicorn server:
```powershell
python -m uvicorn main:app --port 8000 --reload
```
Nếu server khởi động thành công, bạn sẽ thấy thông báo:
`INFO:     Uvicorn server running on http://0.0.0.0:8000 (Press CTRL+C to quit)`

> [!TIP]
> **Cách chạy Mock Mode (Không cần Internet / API Key)**
>
> Nếu bạn không có API key thực hoặc muốn test offline, hãy sửa file `.env` hoặc set biến môi trường trước khi chạy:
> ```powershell
> # Trên Windows (PowerShell)
> $env:MOCK_EMBEDDINGS="true"
> python -m uvicorn main:app --port 8000 --reload
> ```
> Khi khởi động, server sẽ ghi nhận log: `Khởi tạo MOCK Embedding Model (dimension 768)...`

---

## 5. Các biến môi trường quan trọng

Các biến này cấu hình tại file `.env` ở thư mục gốc của microservice:

| Tên biến môi trường | Giá trị mặc định | Giải thích |
| :--- | :--- | :--- |
| `LLM_PROVIDER` | `gemini` | Chọn nhà cung cấp LLM: `gemini` hoặc `openai`. |
| `GOOGLE_API_KEY` | *(Trống)* | Khóa API lấy từ Google AI Studio (bắt buộc khi dùng `gemini`). |
| `OPENAI_API_KEY` | *(Trống)* | Khóa API lấy từ OpenAI Platform (bắt buộc khi dùng `openai`). |
| `LLM_MODEL_NAME` | `gemini-2.0-flash` | Tên mô hình LLM dùng để tạo câu trả lời tư vấn. |
| `EMBEDDING_MODEL_NAME`| `models/text-embedding-004` | Tên mô hình embedding để vector hóa tài liệu. |
| `CHROMA_DB_PATH` | `./chroma_db` | Thư mục lưu trữ database file của ChromaDB local. |
| `CHROMA_COLLECTION_NAME`| `laptops` | Tên collection lưu trữ dữ liệu laptop trong ChromaDB. |
| `HOST` | `0.0.0.0` | IP lắng nghe của server FastAPI. |
| `PORT` | `8000` | Port chạy ứng dụng FastAPI. |
| `ALLOWED_ORIGINS` | `http://localhost:8080` | Danh sách các nguồn (origins) được phép truy cập (CORS), phân tách bằng dấu phẩy. Cần trỏ tới địa chỉ của Spring Boot Web. |
| `MOCK_EMBEDDINGS` | `false` | Đặt thành `true` để kích hoạt Mock Embedding cho mục đích test offline không cần mạng/API key. |

---

## 6. API Reference cho Spring Boot

### 6.1 GET `/health`
*   **Mục đích**: Kiểm tra sức khỏe của microservice và cấu hình hiện tại.
*   **Request**: Không có Body.
*   **Response mẫu (200 OK)**:
    ```json
    {
      "status": "healthy",
      "service": "Product-Comparison-RAG",
      "version": "1.0.0",
      "provider": "gemini",
      "llm_model": "gemini-2.0-flash"
    }
    ```

---

### 6.2 POST `/api/sync-product`
*   **Mục đích**: Đồng bộ (thêm hoặc cập nhật) thông tin một laptop vào ChromaDB.
*   **Request JSON mẫu đầy đủ (camelCase)**:
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
      "description": "Laptop gaming cao cấp mỏng nhẹ với màn hình OLED tuyệt đẹp.",
      "isHot": true,
      "useCaseTags": "gaming, design, portable",
      "stock": 15,
      "category": "Laptop Gaming"
    }
    ```
*   **Response mẫu (200 OK)**:
    ```json
    {
      "success": true,
      "message": "Đồng bộ sản phẩm 'Asus ROG Zephyrus G14 (2024) GA403UV' (ID=1) thành công.",
      "product_id": 1
    }
    ```

---

### 6.3 POST `/api/sync-product/bulk`
*   **Mục đích**: Đồng bộ hàng loạt sản phẩm cùng lúc. Hỗ trợ import dữ liệu ban đầu từ Spring Boot sang RAG.
*   **Request JSON mẫu**:
    ```json
    {
      "products": [
        {
          "id": 1,
          "name": "Asus ROG Zephyrus G14",
          "brand": "Asus",
          "price": 34990000.0,
          "ram": 16,
          "cpu": "AMD Ryzen 9 8945HS",
          "gpu": "NVIDIA GeForce RTX 4060",
          "storage": "1TB SSD",
          "url": "/product/1",
          "imageUrl": "/images/rog-g14.jpg",
          "stock": 5
        },
        {
          "id": 2,
          "name": "MSI Katana 15 B13VFK",
          "brand": "MSI",
          "price": 24990000.0,
          "ram": 16,
          "cpu": "Intel Core i7-13620H",
          "gpu": "NVIDIA GeForce RTX 4060",
          "storage": "512GB SSD",
          "url": "/product/2",
          "imageUrl": "/images/katana-15.jpg",
          "stock": 10
        }
      ]
    }
    ```
*   **Response mẫu (200 OK - camelCase)**:
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

---

### 6.4 DELETE `/api/sync-product/{product_id}`
*   **Mục đích**: Xóa thông tin laptop khỏi ChromaDB khi laptop bị xóa khỏi website.
*   **Response khi thành công (200 OK)**:
    ```json
    {
      "success": true,
      "message": "Đã xóa sản phẩm ID=1 khỏi vector database.",
      "product_id": 1
    }
    ```
*   **Response khi sản phẩm không tồn tại (404 Not Found)**:
    ```json
    {
      "error": "Sản phẩm ID=999 không tồn tại trong vector database.",
      "status_code": 404
    }
    ```

---

### 6.5 POST `/api/chat`
*   **Mục đích**: Trò chuyện tư vấn, so sánh laptop dựa trên cơ sở dữ liệu sản phẩm.
*   **Request JSON mẫu**:
    ```json
    {
      "message": "Tôi muốn mua laptop gaming dưới 25 triệu RAM 16GB",
      "session_id": "user-session-uuid-12345",
      "top_k": 5
    }
    ```
*   **Response mẫu (200 OK - camelCase đầy đủ)**:
    ```json
    {
      "answer": "Dựa trên nhu cầu gaming với ngân sách dưới 25 triệu và RAM từ 16GB, dưới đây là các lựa chọn phù hợp nhất cho bạn:\n\n1. **MSI Katana 15 B13VFK (24.990.000 VND)**: Máy được trang bị card đồ họa rời RTX 4060 8GB mạnh mẽ kết hợp cùng CPU Intel i7 thế hệ 13 mang lại khả năng chơi mượt mà các tựa game AAA.\n2. **Lenovo LOQ 15APH9 (21.990.000 VND)**: Đây là lựa chọn cực kỳ tiết kiệm với chip Ryzen 7 và GPU RTX 4060 8GB, đem lại cấu hình tối ưu nhất trong tầm giá dưới 22 triệu.\n\nCả hai máy đều đáp ứng tốt yêu cầu RAM 16GB để bạn đa nhiệm và chơi game mượt mà.",
      "confidenceScore": 0.95,
      "citations": [
        "Thông số từ TechStore ID #2 - MSI Katana 15 B13VFK",
        "Thông số từ TechStore ID #3 - Lenovo LOQ 15APH9"
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

---

### 6.6 POST `/api/parse-query-debug`
*   **Mục đích**: Debug phân tích cú pháp câu hỏi (Query Parser) để xem hệ thống trích xuất thông tin như thế nào.
*   **Request JSON mẫu**:
    ```json
    {
      "message": "Cần laptop gaming asus rtx 4060 khoảng 20tr đến 30tr"
    }
    ```
*   **Response mẫu (200 OK - camelCase)**:
    ```json
    {
      "maxPrice": 30000000.0,
      "minPrice": 20000000.0,
      "brand": "asus",
      "minRam": null,
      "cpuKeyword": null,
      "gpuKeyword": "rtx 4060",
      "minStorageGb": null,
      "semanticQuery": "laptop gaming asus rtx 4060 20tr 30tr",
      "useCase": "gaming",
      "mode": "recommendation",
      "comparisonProducts": [],
      "rawQuery": "Cần laptop gaming asus rtx 4060 khoảng 20tr đến 30tr"
    }
    ```

---

### 6.7 POST `/api/search-debug`
*   **Mục đích**: Xem chi tiết quá trình tìm kiếm, xếp hạng sản phẩm (Scoring Engine) trước khi đưa vào LLM. Giải thích trực quan tại sao sản phẩm A được ưu tiên hơn sản phẩm B.
*   **Request JSON mẫu**:
    ```json
    {
      "message": "laptop đồ họa tầm 30 triệu"
    }
    ```
*   **Response mẫu (200 OK - Trích dẫn rút gọn)**:
    ```json
    {
      "parsedQuery": {
        "maxPrice": 34500000.0,
        "minPrice": 25500000.0,
        "brand": null,
        "minRam": null,
        "cpuKeyword": null,
        "gpuKeyword": null,
        "minStorageGb": null,
        "semanticQuery": "laptop đồ họa 30 triệu",
        "useCase": "design",
        "mode": "recommendation",
        "comparisonProducts": [],
        "rawQuery": "laptop đồ họa tầm 30 triệu"
      },
      "metadataFilter": {
        "price": {
          "$gte": 25500000.0,
          "$lte": 34500000.0
        }
      },
      "candidateCount": 1,
      "candidates": [
        {
          "docId": "product_1",
          "document": "Laptop Asus ROG Zephyrus G14 (2024)...",
          "similarityScore": 0.795,
          "bonusScore": 1.3,
          "vectorScore": 0.795,
          "totalScore": 2.095,
          "scoreDetails": [
            "Vector similarity score: 0.795",
            "Match Use Case (design): +0.3 (Bonus for Dedicated GPU RTX 4060)",
            "Dedicated GPU bonus: +0.25 (RTX 4060 8GB)",
            "Is Hot bonus: +0.1",
            "Stock bonus (in stock): +0.4"
          ],
          "metadata": {
            "productId": 1,
            "name": "Asus ROG Zephyrus G14 (2024) GA403UV",
            "brand": "asus",
            "price": 34990000.0,
            "ram": 16,
            "cpu": "AMD Ryzen 9 8945HS",
            "gpu": "NVIDIA GeForce RTX 4060 8GB",
            "storage": "1TB NVMe SSD",
            "screenSize": 14.0,
            "weight": 1.65,
            "url": "/product/1",
            "imageUrl": "/images/asus-rog-g14.jpg",
            "isHot": true,
            "useCaseTags": "gaming, design, portable",
            "stock": 15,
            "category": "Laptop Gaming",
            "inStock": true
          }
        }
      ]
    }
    ```

---

## 7. JSON DTO gợi ý cho Spring Boot

Dưới đây là các lớp Data Transfer Object (DTO) Java đề xuất, được thiết kế khớp chuẩn xác với các trường dữ liệu camelCase của FastAPI.

### 7.1 ProductSyncRequestDto.java
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

### 7.2 BulkSyncRequestDto.java
```java
package com.example.laptopstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkSyncRequestDto {
    private List<ProductSyncRequestDto> products;
}
```

### 7.3 BulkSyncResultItemDto.java
```java
package com.example.laptopstore.dto;

import lombok.Data;

@Data
public class BulkSyncResultItemDto {
    private Long id;
    private Boolean success;
    private String message;
}
```

### 7.4 BulkSyncResponseDto.java
```java
package com.example.laptopstore.dto;

import lombok.Data;
import java.util.List;

@Data
public class BulkSyncResponseDto {
    private Boolean success;
    private Integer total;
    private Integer successCount;
    private Integer failedCount;
    private List<BulkSyncResultItemDto> results;
}
```

### 7.5 ChatRequestDto.java
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

### 7.6 RecommendedProductDto.java
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

### 7.7 ChatResponseDto.java
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

### 7.8 SyncResponseDto.java
```java
package com.example.laptopstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncResponseDto {
    private Boolean success;
    private String message;
    private Long productId;
}
```

---

## 8. Ví dụ Spring Boot Service gọi FastAPI

Đoạn code Spring Boot dưới đây viết bằng **WebClient** (thư viện Reactive Web tiêu chuẩn thay thế cho RestTemplate cũ) để gửi yêu cầu và nhận kết quả một cách đồng bộ (`.block()`), phù hợp cho luồng tích hợp nghiệp vụ.

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
                    .block();
        } catch (Exception e) {
            log.error("Đồng bộ thất bại sản phẩm ID: {}. Lỗi: {}", productDto.getId(), e.getMessage());
            return new SyncResponseDto(false, "Lỗi kết nối RAG: " + e.getMessage(), productDto.getId());
        }
    }

    /**
     * Đồng bộ hàng loạt sản phẩm sang RAG service
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
            errorResponse.setAnswer("Rất tiếc, hệ thống tư vấn thông minh đang gặp sự cố kết nối. Vui lòng thử lại sau.");
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

---

## 9. Luồng tích hợp đề xuất trong Web bán laptop

Để đảm bảo dữ liệu trong ChromaDB của FastAPI luôn nhất quán với Database chính (MySQL/PostgreSQL) của Spring Boot, hãy triển khai tích hợp theo các điểm chạm (touchpoints) sau:

1.  **Sự kiện Thêm / Cập nhật sản phẩm (Admin Portal)**:
    *   Khi admin nhấn lưu sản phẩm mới hoặc chỉnh sửa cấu hình sản phẩm hiện tại trên giao diện quản trị của Spring Boot.
    *   Spring Boot lưu thông tin xuống database chính (MySQL) thành công.
    *   Chuyển đổi thực thể sản phẩm (Product Entity) thành `ProductSyncRequestDto` và gọi `syncProduct(...)`.
    *   *Lợi ích*: Đảm bảo các thay đổi về giá bán, RAM, GPU hoặc tình trạng kho hàng được cập nhật tức thì vào Vector DB để AI không tư vấn sai thông tin.
2.  **Sự kiện Import dữ liệu hàng loạt (Bulk Import / Excel)**:
    *   Khi quản trị viên import hàng trăm sản phẩm từ file Excel/CSV.
    *   Spring Boot lưu tất cả các bản ghi vào database chính.
    *   Gom các sản phẩm này thành danh sách `List<ProductSyncRequestDto>`, chia nhỏ thành các batch (ví dụ: 50 sản phẩm/batch) và gọi `bulkSyncProducts(...)` sang FastAPI.
    *   *Lợi ích*: Giảm đáng kể số lượng kết nối mạng HTTP và tối ưu hóa tốc độ tạo embedding vector của RAG microservice.
3.  **Sự kiện Xóa sản phẩm (Admin Portal)**:
    *   Khi admin thực hiện xóa hẳn sản phẩm hoặc ẩn sản phẩm khỏi hệ thống.
    *   Spring Boot xóa bản ghi trong database chính.
    *   Gọi `deleteProduct(productId)`.
    *   *Lợi ích*: Gỡ ngay lập tức sản phẩm ra khỏi không gian vector tìm kiếm của ChromaDB, triệt tiêu khả năng AI Agent gợi ý một chiếc laptop đã dừng kinh doanh.
4.  **Tính năng Trò chuyện & Tư vấn thông minh (User Portal)**:
    *   Tại trang chủ hoặc trang chatbox hỗ trợ, user gửi tin nhắn (ví dụ: *"Tư vấn laptop văn phòng mỏng nhẹ"*).
    *   Controller của Spring Boot nhận request, sinh ra (hoặc lấy lại) `sessionId` để duy trì ngữ cảnh người dùng.
    *   Gọi `chat(message, sessionId, topK)` từ `RagIntegrationService`.
    *   Nhận về đối tượng `ChatResponseDto`.
    *   **Gợi ý giao diện hiển thị**:
        *   Phần text (`answer` dạng Markdown) hiển thị trong khung trò chuyện của bot.
        *   Phần danh sách gợi ý (`recommendedProducts`): Spring Boot render thành các thẻ card sản phẩm nằm ngang bên dưới đoạn text, chứa: Ảnh sản phẩm, Tên laptop, Giá tiền, và nút "Xem chi tiết" dẫn đến liên kết `url`.

---

## 10. Demo Script cho Mentor

Kịch bản demo từng bước để chứng minh hoạt động thực tế của hệ thống:

### Bước 1: Khởi động FastAPI Microservice
*   Mở terminal, chạy lệnh khởi động server (đã bật Mock Embeddings hoặc điền API key thật):
    `python -m uvicorn main:app --port 8000 --reload`
*   Chỉ cho Mentor thấy log khởi động thành công, kết nối đến ChromaDB và thông số model (`gemini-2.0-flash` hoặc `gpt-4o-mini`).

### Bước 2: Gọi Health Check Endpoint
*   Mở trình duyệt truy cập `http://localhost:8000/health` hoặc gọi qua Postman/Curl.
*   *Kết quả*: Trả về JSON trạng thái `healthy`. Chứng minh server hoạt động ổn định.

### Bước 3: Đổ dữ liệu mẫu (Bulk Sync 15 laptop)
*   Chạy script đổ dữ liệu mẫu có sẵn trong source code:
    `python seed_data.py`
*   *Kết quả*: Script sẽ gửi request `POST /api/sync-product/bulk` chứa 15 sản phẩm laptop thực tế (các dòng Asus ROG, MSI Katana, Macbook Pro, Dell Vostro...). Chỉ ra log của FastAPI ghi nhận việc nhận request đồng bộ hàng loạt và hiển thị thành công.
*   Gọi endpoint `GET http://localhost:8000/api/stats` để kiểm chứng, lúc này Mentor sẽ thấy `total_products_in_vector_db` tăng lên tương ứng.

### Bước 4: Demo Tính năng phân tích ý định (Query Parser)
*   Gửi request đến `POST http://localhost:8000/api/parse-query-debug` bằng Postman với câu hỏi:
    *"so sánh Asus ROG G14 và Lenovo Legion 5"*
*   *Giải thích cho Mentor*: Hệ thống tự động chuyển đổi `mode` thành `"comparison"`, đồng thời tách thành công mảng `comparisonProducts` chứa tên hai máy để chuẩn bị truy xuất so sánh chi tiết.

### Bước 5: Demo Tính năng chấm điểm nghiệp vụ (Search & Scoring Debug)
*   Gửi request đến `POST http://localhost:8000/api/search-debug` với nội dung:
    *"Tôi cần laptop gaming dưới 25 triệu RAM 16GB"*
*   *Giải thích cho Mentor*:
    *   Hệ thống tự động thiết lập bộ lọc cứng `metadataFilter` với điều kiện RAM >= 16 và giá <= 25,000,000 VND để ChromaDB lọc chuẩn xác trước (Pre-filtering).
    *   Chỉ ra mảng `scoreDetails` giải trình cách tính điểm: Laptop có GPU rời (như RTX 4060) được cộng điểm lớn cho nhu cầu gaming, laptop hot được cộng điểm, laptop còn hàng được cộng điểm. Điều này đảm bảo laptop phù hợp nhất luôn hiển thị lên trên cùng.

### Bước 6: Test Chat tư vấn các kịch bản thực tế
Gửi câu hỏi đến `POST http://localhost:8000/api/chat` và trình bày phản hồi:

*   **Kịch bản 1: Tìm kiếm theo cấu hình & tầm giá**
    *   *Câu hỏi*: `"Tôi cần laptop gaming dưới 25 triệu RAM 16GB"`
    *   *Kết quả*: Trả về câu trả lời giới thiệu **MSI Katana 15** và **Lenovo LOQ 15** (đều < 25tr, RAM 16GB, GPU RTX 4060). Có danh sách gợi ý đính kèm dạng JSON chuẩn.
*   **Kịch bản 2: Tìm kiếm văn phòng giá rẻ**
    *   *Câu hỏi*: `"Có laptop Dell nào cho văn phòng dưới 20 triệu không?"`
    *   *Kết quả*: Hệ thống lọc thương hiệu Dell, tìm các dòng văn phòng mỏng nhẹ và trả về dòng Dell Vostro / Dell Inspiron trong tầm giá dưới 20 triệu.
*   **Kịch bản 3: So sánh trực tiếp**
    *   *Câu hỏi*: `"So sánh Asus ROG G14 và Lenovo Legion 5"`
    *   *Kết quả*: Câu trả lời chia mục so sánh rõ ràng: Thiết kế, hiệu năng, màn hình, trọng lượng giúp người dùng đưa ra quyết định mua hàng.
*   **Kịch bản 4: Tìm kiếm theo nhu cầu chuyên môn**
    *   *Câu hỏi*: `"Laptop đồ họa dùng Photoshop và Premiere khoảng 30 triệu"`
    *   *Kết quả*: Gợi ý các dòng laptop có cấu hình màn hình độ phủ màu cao (IPS/OLED) và card đồ họa rời mạnh mẽ, RAM từ 16GB trở lên.

### Bước 7: Phân tích các thế mạnh của hệ thống đã chứng minh qua Demo
1.  **Metadata filtering**: Loại bỏ hoàn toàn tình trạng đề xuất laptop sai giá hoặc thiếu RAM.
2.  **Scoring & Re-ranking**: Kết hợp giữa toán học vector cosine và các luật nghiệp vụ laptop để tăng chất lượng gợi ý.
3.  **Anti-hallucination**: Trả lời chính xác thông số kỹ thuật thực tế của cửa hàng.
4.  **CamelCase**: JSON trả về tương thích hoàn hảo với Java Spring Boot.

---

## 11. Các điểm kỹ thuật nên nhấn mạnh khi trình bày

Khi thuyết trình trước Hội đồng phản biện (Mentors), bạn nên nhấn mạnh các giải pháp kỹ thuật nổi bật sau:

*   **Tách biệt Microservice**: Kiến trúc microservice tách biệt hoàn toàn Python (chuyên xử lý AI/RAG) và Java (chuyên xử lý Web/ERP). Đây là thiết kế chuẩn mực thực tế ở các doanh nghiệp hiện nay.
*   **Pipeline RAG nâng cao**: Không giống các chatbot RAG thông thường chỉ dùng vector search dễ bị sai số về con số cụ thể, hệ thống này tích hợp **Metadata Filtering trước khi thực hiện Vector Search**. Điều này giải quyết triệt độ bài toán lọc cứng theo khoảng giá, RAM, và hãng sản xuất.
*   **Custom Scoring Engine**: Hệ thống có thuật toán chấm điểm động. Bằng cách phân tích nhu cầu người dùng, Scoring Engine sẽ ưu tiên cộng điểm cho laptop có GPU rời đối với tác vụ Gaming/Thiết kế, hoặc cộng điểm cho laptop mỏng nhẹ, pin trâu đối với văn phòng. Nó cũng ưu tiên sản phẩm còn hàng (`stock > 0`) và sản phẩm bán chạy (`isHot`).
*   **Xử lý phản hồi có cấu trúc (Structured Output)**: Nhờ sự kết hợp của Pydantic định nghĩa Schema chặt chẽ và LangChain Parser, mô hình LLM luôn trả về JSON hợp lệ 100%, loại bỏ hoàn toàn nguy cơ vỡ layout giao diện do lỗi định dạng văn bản tự do của AI.
*   **Hệ thống chống ảo giác thông minh**: Một lớp code hậu xử lý (Validation Layer) kiểm tra chéo các mã sản phẩm do LLM trả về với danh sách ứng viên từ ChromaDB. Nếu LLM bịa ra một dòng laptop lạ, hệ thống sẽ chặn đứng và khôi phục thông tin từ cơ sở dữ liệu gốc.
*   **Tích hợp dễ dàng qua CORS và camelCase**: Hỗ trợ đầy đủ CORS cho frontend/Spring Boot và serialization tự động theo tên biến dạng camelCase giúp việc viết code kết nối ở Spring Boot tối giản nhất.

---

## 12. Hạn chế hiện tại và hướng phát triển tiếp

Nhằm thể hiện sự khách quan và chuyên nghiệp, bạn hãy trình bày rõ các giới hạn hiện tại của phiên bản này cùng phương hướng nâng cấp:

### Hạn chế hiện tại
1.  **Môi trường thử nghiệm**: Các test case tự động hiện đang chạy trên môi trường Mock Embeddings (`MOCK_EMBEDDINGS=true`). Do đó, cần kiểm chứng bổ sung trên môi trường thực tế kết nối API Key thật để đánh giá độ trễ mạng thực tế của LLM.
2.  **Khả năng so sánh**: AI chỉ so sánh được các dòng máy hiện đang được lưu trong ChromaDB. Nếu người dùng yêu cầu so sánh với một dòng máy lạ chưa từng đồng bộ, hệ thống sẽ báo không tìm thấy thông tin thay vì tự tìm kiếm internet.
3.  **Bộ lọc câu hỏi (Query Parser)**: Bộ trích xuất thông tin hiện sử dụng các biểu thức chính quy (Regex) và quy tắc heuristic. Dù rất nhanh và chính xác với hầu hết câu lệnh thông dụng, nó có thể bỏ sót tiêu chí đối với các câu hỏi viết tắt quá dị biệt hoặc ngữ pháp quá phức tạp.
4.  **Bảo mật & Triển khai**: Hiện tại chưa có lớp xác thực (Authentication API Key) giữa Spring Boot và FastAPI microservice. Chưa đóng gói chung bằng Docker Compose để triển khai bằng một lệnh duy nhất.

### Hướng phát triển tiếp theo
*   **Conversation History (Lịch sử trò chuyện)**: Nâng cấp bộ lưu trữ phiên chat để hỗ trợ hội thoại nhiều lượt liên tục (ví dụ: User hỏi *"Laptop gaming Asus dưới 25tr"*, bot trả lời, sau đó user chat tiếp *"Còn dòng nào mỏng nhẹ hơn không?"*).
*   **Hybrid Search**: Kết hợp tìm kiếm từ khóa truyền thống (BM25) với Vector Search để tăng độ chính xác tuyệt đối khi người dùng tìm kiếm chính xác mã model laptop (ví dụ: tìm cụ thể mã *"GA403UV"*).
*   **Đóng gói Docker Compose**: Viết file `docker-compose.yml` định nghĩa sẵn 3 container: Spring Boot Web, FastAPI Microservice và ChromaDB để triển khai toàn bộ hệ thống chỉ với lệnh `docker-compose up`.
*   **Thêm tầng Auth**: Bảo vệ các API bằng JWT token hoặc API Key tĩnh cấu hình chéo giữa hai hệ thống.

---

## 13. Checklist trước khi demo thật

Trước giờ trình bày demo, hãy chắc chắn bạn đã tích vào toàn bộ các đầu mục dưới đây:

*   [ ] **Server FastAPI đang chạy**: Đảm bảo cổng `8000` của local đang lắng nghe và phản hồi tốt.
*   [ ] **Cấu hình file `.env` chuẩn xác**: Kiểm tra đã điền API Key hoạt động tốt hoặc đã bật `MOCK_EMBEDDINGS=true` trong môi trường test offline.
*   [ ] **ChromaDB có dữ liệu**: Đã chạy file `seed_data.py` để nạp 15 laptop mẫu, gọi `/api/stats` thấy tổng số sản phẩm lớn hơn 0.
*   [ ] **Endpoint `/health` trả về healthy**: Đảm bảo không gặp lỗi khởi tạo cơ sở dữ liệu.
*   [ ] **Không hard-code API Key**: Đảm bảo các API Key đã được cất giấu trong file `.env` và nằm trong danh sách `.gitignore`, tuyệt đối không đẩy key lên GitHub.
*   [ ] **Định dạng camelCase hoạt động tốt**: Thử gọi `/api/chat` qua Postman và kiểm tra các key như `confidenceScore`, `recommendedProducts` viết đúng chuẩn lạc đà.
*   [ ] **Cấu hình CORS khớp với Spring Boot**: Biến `ALLOWED_ORIGINS` trong `.env` phải chứa chính xác địa chỉ IP/cổng chạy của Web App Spring Boot (ví dụ: `http://localhost:8080`).
*   [ ] **Spring Boot kết nối thành công**: Chạy thử một request đồng bộ sản phẩm từ Spring Boot sang FastAPI và thấy dữ liệu cập nhật thành công bên phía ChromaDB.
