# Product-Comparison-RAG 🤖💻

Microservice **RAG (Retrieval-Augmented Generation)** cho hệ thống **Tư vấn và So sánh Laptop**, xây dựng bằng Python. Giao tiếp với hệ thống Web chính (Java Spring Boot) qua REST API.

## 🏗️ Kiến trúc hệ thống

```
Spring Boot (Frontend/Backend)
        │
        │ REST API (JSON)
        ▼
┌─────────────────────────────────┐
│     FastAPI RAG Microservice    │
│                                 │
│  ┌──────────────────────────┐   │
│  │  1. Query Parser         │   │  ← Phân tích câu hỏi tự nhiên
│  │  2. Metadata Filter      │   │  ← Lọc cứng theo giá/brand/RAM
│  │  3. Vector Search        │   │  ← Tìm kiếm ngữ nghĩa
│  │  4. Scoring Engine       │   │  ← Chấm điểm & xếp hạng
│  │  5. LLM Generator        │   │  ← Tạo câu trả lời JSON
│  └──────────────────────────┘   │
│                                 │
│  ChromaDB (Vector Store Local)  │
│  Gemini / OpenAI (LLM + Embed)  │
└─────────────────────────────────┘
```

## 📂 Cấu trúc dự án

```
Product-Comparison-RAG/
├── config.py          # Cấu hình & biến môi trường (Pydantic Settings)
├── database.py        # ChromaDB Manager, CRUD, Vector Query
├── schemas.py         # Pydantic Models (API Request/Response, LLM Output)
├── rag_service.py     # RAG Pipeline chính (6 module)
├── main.py            # FastAPI App & API Endpoints
├── seed_data.py       # Script thêm dữ liệu mẫu (15 laptop)
├── requirements.txt   # Python dependencies
├── .env.example       # Mẫu biến môi trường
├── .gitignore
└── README.md
```

## ⚡ Cài đặt & Chạy

### Yêu cầu
- Python 3.10+
- Google Gemini API Key (hoặc OpenAI API Key)

### Bước 1: Tạo môi trường ảo

```bash
# Windows
python -m venv .venv
.venv\Scripts\activate

# macOS / Linux
python3 -m venv .venv
source .venv/bin/activate
```

### Bước 2: Cài đặt dependencies

```bash
pip install -r requirements.txt
```

### Bước 3: Cấu hình môi trường

```bash
# Sao chép file cấu hình mẫu
cp .env.example .env
```

Mở file `.env` và điền thông tin:

```env
# Chọn provider (gemini hoặc openai)
LLM_PROVIDER=gemini

# Nếu dùng Gemini - lấy key tại https://aistudio.google.com/app/apikey
GOOGLE_API_KEY=your-actual-api-key-here

# Nếu dùng OpenAI
# OPENAI_API_KEY=sk-your-key-here
# LLM_MODEL_NAME=gpt-4o-mini
# EMBEDDING_MODEL_NAME=text-embedding-3-small
```

### Bước 4: Chạy server

```bash
python main.py
```

Server khởi động tại: `http://localhost:8000`

### Bước 5: Thêm dữ liệu mẫu

Mở terminal mới (giữ nguyên server đang chạy):

```bash
python seed_data.py
```

Script sẽ thêm 15 laptop mẫu vào ChromaDB.

## 🔌 API Endpoints

### 1. Chat - Tư vấn Laptop

```http
POST /api/chat
Content-Type: application/json

{
  "message": "Tôi cần laptop gaming dưới 25 triệu, RAM 16GB, thương hiệu Asus hoặc MSI",
  "session_id": "user-123"
}
```

**Response:**
```json
{
  "answer": "Dựa trên nhu cầu gaming của bạn với ngân sách 25 triệu...",
  "confidenceScore": 0.87,
  "citations": [
    "Thông số kỹ thuật MSI Katana 15 từ TechStore ID #2",
    "Thông số kỹ thuật Lenovo LOQ 15 từ TechStore ID #3"
  ],
  "missingInformation": [
    "Bạn ưu tiên màn hình lớn (15.6\") hay màn hình nhỏ hơn để di chuyển?",
    "Bạn có cần pin trâu để dùng ngoài nhà không?"
  ],
  "recommendedProducts": [
    {
      "id": 2,
      "name": "MSI Katana 15 B13VFK",
      "price": 24990000,
      "url": "/product/2",
      "imageUrl": "/images/msi-katana-15.jpg",
      "reason": "RTX 4060 mạnh nhất trong tầm giá, i7-13620H hiệu năng cao"
    },
    {
      "id": 3,
      "name": "Lenovo LOQ 15APH9",
      "price": 21990000,
      "url": "/product/3",
      "imageUrl": "/images/lenovo-loq-15.jpg",
      "reason": "Giá tốt nhất phân khúc, Ryzen 7 7745HX mạnh mẽ"
    }
  ]
}
```

### 2. Đồng bộ sản phẩm (Spring Boot → RAG)

```http
POST /api/sync-product
Content-Type: application/json

{
  "id": 16,
  "name": "Asus TUF Gaming F15 FX507ZC4",
  "brand": "Asus",
  "price": 18990000,
  "ram": 8,
  "cpu": "Intel Core i5-12500H",
  "gpu": "NVIDIA GeForce RTX 3050 4GB",
  "storage": "512GB NVMe SSD",
  "screen_size": 15.6,
  "is_hot": false,
  "use_case_tags": "gaming",
  "url": "/product/16",
  "image_url": "/images/asus-tuf-f15.jpg"
}
```

### 3. Xóa sản phẩm

```http
DELETE /api/sync-product/16
```

### 4. Health Check

```http
GET /health
```

### 5. Stats

```http
GET /api/stats
```

## 📖 API Documentation

Khi server đang chạy, truy cập:
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

## 🧠 Cách hoạt động (RAG Pipeline)

```
Câu hỏi: "Tôi cần laptop gaming dưới 25 triệu RAM 16GB"
          │
          ▼
┌─────────────────────────────────────────┐
│ 1. QUERY PARSER (Rule-based)            │
│    max_price: 25,000,000 VND            │
│    min_ram: 16 GB                       │
│    use_case: "gaming"                   │
│    semantic_query: "laptop gaming hiệu  │
│    năng cao GPU RTX card rời..."        │
└─────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────┐
│ 2. METADATA FILTER (ChromaDB where)     │
│    { price: {$lte: 25000000},           │
│      ram: {$gte: 16} }                  │
└─────────────────────────────────────────┘
          │ Pre-filtered products
          ▼
┌─────────────────────────────────────────┐
│ 3. VECTOR SEARCH (Semantic Similarity)  │
│    Embedding query → Find top-10        │
│    similar products từ tập đã lọc       │
└─────────────────────────────────────────┘
          │ 10 candidates
          ▼
┌─────────────────────────────────────────┐
│ 4. SCORING ENGINE (Re-ranking)          │
│    + Vector similarity × 5             │
│    + Brand match: +3 điểm              │
│    + Price range: +2 điểm              │
│    + CPU match: +2 điểm                │
│    + HOT product: +1 điểm              │
└─────────────────────────────────────────┘
          │ Top-5 ranked
          ▼
┌─────────────────────────────────────────┐
│ 5. LLM GENERATION (Gemini/OpenAI)       │
│    Structured Output (Pydantic)         │
│    → JSON với answer, confidence,       │
│       citations, missingInfo, products  │
└─────────────────────────────────────────┘
```

## 🔗 Tích hợp với Spring Boot

Gọi RAG service từ Java:

```java
// ProductRAGClient.java
@Service
public class ProductRAGClient {

    @Value("${rag.service.url:http://localhost:8000}")
    private String ragServiceUrl;

    private final RestTemplate restTemplate;

    // Gọi API chat
    public RAGResponse chat(String userMessage) {
        ChatRequest request = new ChatRequest(userMessage);
        return restTemplate.postForObject(
            ragServiceUrl + "/api/chat",
            request,
            RAGResponse.class
        );
    }

    // Đồng bộ sản phẩm sau khi admin thêm/sửa
    public void syncProduct(Product product) {
        restTemplate.postForObject(
            ragServiceUrl + "/api/sync-product",
            toSyncRequest(product),
            SyncResponse.class
        );
    }

    // Xóa sản phẩm
    public void deleteProduct(Long productId) {
        restTemplate.delete(
            ragServiceUrl + "/api/sync-product/" + productId
        );
    }
}
```

## ⚙️ Cấu hình nâng cao

| Biến | Mặc định | Mô tả |
|------|----------|-------|
| `LLM_PROVIDER` | `gemini` | Provider: `gemini` hoặc `openai` |
| `LLM_MODEL_NAME` | `gemini-2.0-flash` | Tên LLM model |
| `EMBEDDING_MODEL_NAME` | `models/text-embedding-004` | Tên embedding model |
| `CHROMA_DB_PATH` | `./chroma_db` | Đường dẫn lưu ChromaDB |
| `TOP_K_RESULTS` | `10` | Số sản phẩm lấy từ vector search |
| `MAX_RECOMMENDED_PRODUCTS` | `5` | Số sản phẩm tối đa trả về |
| `DEBUG` | `false` | Bật debug mode |

## 🧪 Test nhanh với curl

```bash
# Chat
curl -X POST http://localhost:8000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Laptop gaming dưới 25 triệu tốt nhất là gì?"}'

# Stats
curl http://localhost:8000/api/stats

# Health
curl http://localhost:8000/health
```
