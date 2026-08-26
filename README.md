# 🛒💻 LaptopStore - Nền Tảng Thương Mại Điện Tử Laptop & Điện Máy Thông Minh

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Python RAG](https://img.shields.io/badge/AI%20Microservice-FastAPI%20%7C%20ChromaDB-blue.svg)](https://fastapi.tiangolo.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> **LaptopStore** là hệ thống thương mại điện tử bán lẻ laptop và thiết bị công nghệ hiện đại được phát triển trên nền tảng **Spring Boot 3** và kiến trúc **Microservice AI**. Hệ thống tích hợp **Trợ lý AI Copilot (RAG - Retrieval-Augmented Generation)** tư vấn cấu hình chuẩn xác 100%, cổng **Thanh toán tự động qua VietQR / SePay**, hệ thống **Xác thực mã OTP qua Email**, và hỗ trợ triển khai **Docker / Render Cloud**.

---

## 🌟 Tính Năng Nổi Bật

### 👤 1. Dành Cho Khách Hàng (Client Interface)

- 🔍 **Tìm kiếm & Bộ lọc đa tiêu chí nâng cao**:
  - Lọc sản phẩm theo thương hiệu (Apple, Dell, Asus, Lenovo, Acer, MSI, HP, Gigabyte...).
  - Lọc chi tiết theo mức giá, nhu cầu sử dụng (*Gaming, Đồ họa - Kỹ thuật, Mỏng nhẹ - Doanh nhân, Học tập - Văn phòng*).
  - Lọc theo thông số phần cứng: Dung lượng RAM (8GB, 16GB, 32GB...), Card đồ họa (NVIDIA RTX, GTX, Intel Iris Xe...), Loại CPU, Ổ cứng SSD.

- 💻 **Xem Chi tiết & Lựa chọn Biến thể sản phẩm**:
  - Hỗ trợ xem nhiều biến thể cấu hình (RAM/SSD/Màu sắc) với mức giá cập nhật linh hoạt theo thời gian thực.
  - Hiển thị tình trạng còn hàng / hết hàng trong kho.

- ⚖️ **So sánh Sản phẩm Trực quan (Product Comparison)**:
  - Cho phép chọn nhiều mẫu laptop để đưa vào bảng so sánh đối đầu thông số kỹ thuật (CPU, RAM, GPU, Màn hình, Trọng lượng, Giá cả) side-by-side giúp khách hàng dễ dàng đưa ra quyết định mua sắm.

- ❤️ **Danh sách Yêu thích (Wishlist)**:
  - Lưu lại các sản phẩm quan tâm vào danh sách cá nhân để theo dõi biến động giá và mua lại nhanh chóng.

- ⭐ **Hệ thống Đánh giá & Bình luận (Reviews & Ratings)**:
  - Khách hàng có thể gửi đánh giá từ 1 đến 5 sao, viết nhận xét và chia sẻ trải nghiệm sử dụng thực tế.

- 🛒 **Quản lý Giỏ hàng & Áp mã Khuyến mãi (Voucher)**:
  - Thêm/sửa/xóa sản phẩm trong giỏ hàng, tự động tính tổng tiền.
  - Áp dụng **Mã giảm giá (Voucher)** theo phần trăm (%) hoặc số tiền cố định, tự động kiểm tra điều kiện đơn hàng tối thiểu và thời hạn hiệu lực.

- 💳 **Thanh toán Tự động VietQR / SePay (Real-time Payment Gateway)** `[MỚI]`:
  - Tự động sinh mã VietQR chuẩn NAPAS theo từng đơn hàng với nội dung chuyển khoản duy nhất (`ORDER_xxx`).
  - Hệ thống tích hợp **Webhook SePay** lắng nghe thông báo biến động số dư ngân hàng và tự động cập nhật trạng thái đơn hàng thành `PAID` **chỉ trong vài giây** mà không cần xác nhận thủ công.

- 🔐 **Xác thực OTP & Bảo mật Tài khoản** `[MỚI]`:
  - Đăng ký tài khoản, đăng nhập an toàn với Spring Security 6.
  - Xác thực qua **Mã OTP 6 chữ số** gửi qua Email (SendGrid / Spring Mail Integration) cho các tính năng đăng ký tài khoản và khôi phục / quên mật khẩu.

- 🤖 **Trợ lý AI Copilot tư vấn mua sắm (RAG Chatbot)** `[MỚI]`:
  - **Tư vấn thông minh dựa trên nhu cầu**: Phân tích ngữ cảnh câu hỏi bằng ngôn ngữ tự nhiên (ví dụ: *"Tôi có 20 triệu cần mua laptop học CNTT và chơi game FC Online"*).
  - **Chế độ so sánh AI (Comparison Mode)**: Tự động so sánh ưu/nhược điểm các dòng máy khi người dùng hỏi dạng *"So sánh Asus ROG Strix và Lenovo Legion 5"*.
  - **Chống ảo giác (Anti-hallucination Pipeline)**: Áp dụng cơ chế *Metadata Pre-filtering*, *Scoring Re-ranking Engine* và *Validation Layer* giúp đảm bảo 100% thông tin cấu hình, giá tiền và tình trạng kho hàng được gợi ý luôn trùng khớp với dữ liệu thực tế tại cửa hàng.

---

### 🛡️ 2. Dành Cho Quản Trị Viên (Admin Dashboard)

- 📊 **Thống kê & Báo cáo Analytics (Business Intelligence)**:
  - Biểu đồ theo dõi tổng doanh thu, số lượng đơn hàng, số khách hàng mới, sản phẩm bán chạy nhất và cảnh báo sản phẩm sắp hết hàng trong kho.

- 📦 **Quản lý Sản phẩm & Hãng (Product & Brand Management)**:
  - Quản lý danh mục thương hiệu và toàn bộ sản phẩm.
  - Quản lý chi tiết biến thể (Variant), thông số kỹ thuật, hình ảnh sản phẩm và giá niêm yết/giá khuyến mãi.

- 📑 **Quản lý Đơn hàng (Order Workflow Management)**:
  - Quản lý quy trình xử lý đơn hàng chuyên nghiệp: `PENDING` (Chờ thanh toán) ➔ `PAID` (Đã thanh toán) ➔ `SHIPPING` (Đang giao) ➔ `DELIVERED` (Đã giao) ➔ `CANCELLED` (Đã hủy).
  - Xem chi tiết nhật ký giao dịch Webhook từ ngân hàng SePay.

- 🎟️ **Quản lý Mã giảm giá (Voucher Management)**:
  - Tạo mới và quản lý mã voucher khống chế số lần sử dụng, số tiền giảm tối đa, giá trị đơn tối thiểu và thời hạn phát hành.

- 💬 **Quản lý & Duyệt Đánh giá (Review Moderation)**:
  - Quản trị viên duyệt, ẩn hoặc phản hồi lại các bình luận/đánh giá từ khách hàng.

- 📤 **Xuất dữ liệu Báo cáo đa định dạng (Data Export)** `[MỚI]`:
  - Xuất báo cáo danh sách đơn hàng, doanh thu, danh sách sản phẩm và tồn kho ra các định dạng **Excel (.xlsx)**, **PDF**, và **CSV** phục vụ công tác kiểm toán.

- ⚡ **Đồng bộ Vector Database AI (ChromaDB Sync)** `[MỚI]`:
  - Tự động đồng bộ dữ liệu sản phẩm từ MySQL sang ChromaDB Vector Store khi thêm/sửa/xóa sản phẩm.
  - Hỗ trợ **Bulk Sync** (Đồng bộ hàng loạt) toàn bộ sản phẩm chỉ với 1 click.

---

## 🏗️ Kiến Trúc Hệ Thống (Hybrid Microservices)

```text
 ┌───────────────────────────────────────────────────────────┐
 │                  Khách Hàng / Administrator               │
 └─────────────────────────────┬─────────────────────────────┘
                               │ HTTP / HTML / REST API
                               ▼
 ┌───────────────────────────────────────────────────────────┐
 │               Spring Boot Web Application                 │
 │   (MVC Controllers, Security, Service Layer, JPA Repo)    │
 └──────────┬──────────────────┬──────────────────┬──────────┘
            │                  │                  │
   Thanh toán Webhook          │ REST Client      │ JPA/SQL
            ▼                  ▼                  ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│  SePay / VietQR  │ │  FastAPI RAG AI  │ │  MySQL Database  │
│   Payment Gateway│ │   Microservice   │ │ (Aiven Cloud/Local)
└──────────────────┘ └─────────┬────────┘ └──────────────────┘
                               │
                               ▼
                     ┌──────────────────┐
                     │ ChromaDB Vector  │
                     │  & Gemini/OpenAI │
                     └──────────────────┘
```

---

## 🛠️ Công Nghệ Sử Dụng

### 🟢 Backend Core (Java Spring Boot)
* **Java 17 (LTS)** & **Spring Boot 3.5.6**
* **Spring Security 6**: Phân quyền theo vai trò (`ROLE_USER`, `ROLE_ADMIN`), bảo mật CSRF và mã hóa BCrypt.
* **Spring Data JPA & Hibernate**: Thao tác ORM với cơ sở dữ liệu quan hệ.
* **Spring Mail & SendGrid API**: Gửi email xác nhận đơn hàng và mã xác thực OTP.
* **Apache POI & iText PDF**: Xuất báo cáo dữ liệu định dạng Excel & PDF.
* **Lombok & Guava**: Tối ưu hóa mã nguồn và tiện ích dữ liệu.

### 🐍 AI Microservice (Python FastAPI)
* **Python 3.10+** & **FastAPI**
* **ChromaDB**: CSDL Vector chuyên dụng lưu trữ embedding sản phẩm.
* **Google Gemini API / OpenAI API**: Xử lý ngôn ngữ tự nhiên và tổng hợp câu tư vấn.
* **Pydantic**: Ràng buộc dữ liệu đầu ra Structured Output tránh lỗi parse JSON.
* **Mock Mode (`MOCK_EMBEDDINGS=true`)**: Hỗ trợ chạy thử nghiệm AI Offline không cần Internet hay API Key.

### 🎨 Frontend
* **Thymeleaf Template Engine**: SSR Render giao diện linh hoạt.
* **HTML5, CSS3, JavaScript (ES6+) & Bootstrap**: Giao diện responsive tối ưu cho cả Desktop & Mobile.
* **Thymeleaf Extras Spring Security 6**: Hiển thị menu động theo quyền người dùng.

### 🐳 Container & Deployment `[MỚI]`
* **Docker & Dockerfile**: Multi-stage build tối ưu hóa dung lượng image.
* **Cloud Deployment Support**: Sẵn sàng triển khai lên **Render / Railway / Heroku** kết hợp **Aiven Cloud MySQL**.

---

## 📂 Cấu Trúc Thư Mục

```text
LaptopStore/
├── src/
│   ├── main/
│   │   ├── java/com/laptopstore/laptopstore/
│   │   │   ├── Controller/          # Rest & MVC Controllers (Admin, Client, Auth, Cart, Order, RAG AI, Webhook, Export, OTP...)
│   │   │   ├── Service/             # Xử lý nghiệp vụ chính (Order, SePay Payment, RAG Sync, Auth, OTP, Review...)
│   │   │   ├── Repository/          # Spring Data JPA Repositories
│   │   │   ├── entity/              # JPA Entities (User, Product, Order, Voucher, Brand, Review, Otp...)
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── security/            # Cấu hình Spring Security & UserDetailsService
│   │   │   ├── config/              # Các Bean cấu hình (WebClient, Mail, Async...)
│   │   │   └── enums/               # Enums (OrderStatus, PaymentStatus, Role...)
│   │   └── resources/
│   │       ├── templates/           # Giao diện Thymeleaf HTML (Client & Admin)
│   │       ├── static/              # Assets (CSS, JS, Images, Icons)
│   │       ├── application.properties # Cấu hình môi trường (Local / Render)
│   │       └── laptops_sample.csv   # Dữ liệu mẫu laptop
├── docs/                            # Tài liệu kỹ thuật chi tiết, API Spec & Hướng dẫn tích hợp AI RAG
├── Dockerfile                       # File cấu hình đóng gói Docker Multi-stage
├── pom.xml                          # Quản lý thư viện Maven
└── README.md                        # Document hướng dẫn dự án
```

---

## ⚙️ Hướng Dẫn Cài Đặt & Chạy Dự Án

### 📋 Yêu cầu môi trường
* **Java Development Kit (JDK)**: Phiên bản 17 trở lên.
* **Apache Maven**: Phiên bản 3.8+ (hoặc dùng `mvnw` đi kèm).
* **MySQL Server**: Phiên bản 8.0+ (hoặc kết nối Aiven Cloud MySQL).
* **Python**: 3.10+ (Nếu muốn chạy AI Microservice).
* **Docker** *(Tùy chọn nếu muốn chạy qua Container)*.

---

### 🚀 Cách 1: Chạy Local (Maven + MySQL)

#### 1️⃣ Clone Repository
```bash
git clone https://github.com/danhpham28104/LaptopStore.git
cd LaptopStore
```

#### 2️⃣ Cấu hình Cơ sở dữ liệu MySQL
Tạo cơ sở dữ liệu mới trong MySQL:
```sql
CREATE DATABASE LAPTOPSTORE_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Chỉnh sửa thông tin kết nối trong `src/main/resources/application.properties`:
```properties
# Configuration MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/LAPTOPSTORE_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Ho_Chi_Minh
spring.datasource.username=root
spring.datasource.password=your_password

# Cấu hình SePay Webhook Payment Gateway
payment.sepay.va-account=YOUR_VA_ACCOUNT
payment.sepay.bank-code=MBBank

# Cấu hình RAG AI Service (Chạy local FastAPI)
rag.service.url=http://127.0.0.1:8000
```

#### 3️⃣ Khởi động Ứng dụng Spring Boot
```bash
# Windows
mvn spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Truy cập ứng dụng tại:
* 🌐 **Trang chủ Khách hàng**: `http://localhost:8080/`
* 🔐 **Trang Quản trị Admin**: `http://localhost:8080/admin`

---

### 🐳 Cách 2: Chạy Bằng Docker Container `[MỚI]`

Hệ thống đã chuẩn bị sẵn `Dockerfile` hỗ trợ build nhanh:

#### 1️⃣ Build Docker Image
```bash
docker build -t laptopstore-app .
```

#### 2️⃣ Run Container
```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://your-db-host:3306/LAPTOPSTORE_db \
  -e SPRING_DATASOURCE_USERNAME=your_username \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  laptopstore-app
```

---

## 🌐 Các API Endpoints Chính

| Phương thức | Đường dẫn API | Mô tả |
| :--- | :--- | :--- |
| `GET` | `/` | Trang chủ hiển thị sản phẩm nổi bật & khuyến mãi |
| `GET` | `/products` | Trang tìm kiếm & lọc sản phẩm nâng cao |
| `GET` | `/products/{id}` | Xem chi tiết sản phẩm & biến thể cấu hình |
| `GET` | `/compare` | Trang so sánh đối đầu thông số sản phẩm |
| `POST` | `/cart/add` | Thêm sản phẩm & biến thể vào giỏ hàng |
| `GET` | `/checkout` | Trang đặt hàng, chọn phương thức & áp mã giảm giá |
| `POST` | `/api/otp/send` | Gửi mã OTP xác minh qua Email |
| `POST` | `/api/otp/verify` | Kiểm tra tính hợp lệ của mã OTP |
| `POST` | `/api/rag/chat` | Endpoint tương tác với Trợ lý AI Copilot |
| `POST` | `/api/sepay/webhook` | Webhook tiếp nhận thanh toán tự động từ SePay VietQR |
| `GET` | `/admin` | Dashboard thống kê doanh thu & đơn hàng |
| `GET` | `/admin/products` | Quản lý danh sách sản phẩm |
| `GET` | `/admin/analytics/export/excel` | Xuất báo cáo doanh thu ra file Excel |
| `GET` | `/admin/analytics/export/pdf` | Xuất báo cáo doanh thu ra file PDF |

---

## ✍️ Tác Giả & Liên Hệ

* **Tác giả**: Phạm Danh
* **GitHub**: [@danhpham28104](https://github.com/danhpham28104)
* **Project Repository**: [LaptopStore_DATN](https://github.com/danhpham28104/LaptopStore)

---

## 📄 Giấy Phép (License)

Dự án được phát triển phục vụ mục đích học tập, nghiên cứu và Đồ án Tốt nghiệp / Portfolio cá nhân. Tự do tham khảo và phát triển thêm!
