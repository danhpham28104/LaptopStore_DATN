# 🛒💻 TechStore - Hệ Thống Bán Lẻ Laptop & Điện Máy Thông Minh

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Python RAG](https://img.shields.io/badge/AI%20Microservice-FastAPI%20%7C%20ChromaDB-blue.svg)](https://fastapi.tiangolo.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> **LaptopStore** là nền tảng thương mại điện tử chuyên nghiệp cung cấp các sản phẩm laptop, máy tính cá nhân và linh kiện công nghệ. Hệ thống nổi bật nhờ việc tích hợp **Trợ lý AI Copilot (RAG - Retrieval-Augmented Generation)** tư vấn thông minh theo thời gian thực và cổng **Thanh toán tự động qua VietQR / SePay**.

---

## 🌟 Tính Năng Nổi Bật

### 👤 1. Dành Cho Khách Hàng (Client Interface)
- 🔍 **Tìm kiếm & Lọc sản phẩm đa tiêu chí**: Tìm kiếm theo tên sản phẩm, thương hiệu (Apple, Asus, Dell, Lenovo, Acer, MSI...), khoảng giá, nhu cầu sử dụng và thông số kỹ thuật (RAM, CPU, GPU, SSD...).
- 💻 **Xem chi tiết & Cấu hình biến thể**: Hỗ trợ nhiều lựa chọn biến thể sản phẩm (RAM, ổ cứng, màu sắc) với giá cập nhật linh hoạt.
- 🛒 **Quản lý Giỏ hàng & Thanh toán**: Thêm/sửa/xóa sản phẩm trong giỏ hàng, áp dụng **Mã giảm giá (Voucher)**.
- 💳 **Thanh toán tự động VietQR / SePay**: Tự động tạo mã VietQR theo từng đơn hàng. Hệ thống tự động lắng nghe Webhook ngân hàng và xác nhận đơn hàng **chỉ trong vài giây** mà không cần xác nhận thủ công.
- 🤖 **Trợ lý AI Copilot (RAG Chatbot)**:
  - **Tư vấn thông minh**: Giải đáp thắc mắc, gợi ý sản phẩm chính xác dựa trên ngân sách và nhu cầu (Gaming, Đồ họa, Văn phòng, Học tập).
  - **Chế độ so sánh (Comparison Mode)**: So sánh đối đầu các mẫu laptop về thông số, ưu/nhược điểm và mức giá.
  - **Chống ảo giác (Anti-hallucination)**: Đảm bảo thông tin cấu hình, giá và tình trạng hàng trong kho luôn khớp 100% với dữ liệu cửa hàng.
- 🔐 **Xác thực an toàn & OTP**: Đăng ký, đăng nhập, quên mật khẩu an toàn với mã xác minh OTP gửi qua Email/SMS (SendGrid integration).

### 🛡️ 2. Dành Cho Quản Trị Viên (Admin Dashboard)
- 📊 **Báo cáo & Thống kê (Analytics)**: Biểu đồ theo dõi doanh thu, tổng số đơn hàng, khách hàng mới, sản phẩm bán chạy và thống kê tồn kho.
- 📦 **Quản lý Sản phẩm & Hãng (Product & Brand Management)**: Thêm, sửa, xóa sản phẩm, hãng sản xuất, quản lý hình ảnh, thông số chi tiết và biến thể.
- 📑 **Quản lý Đơn hàng (Order Management)**: Quản lý toàn bộ quy trình xử lý đơn (Chờ thanh toán, Đã thanh toán, Đang giao, Đã giao, Hủy), xem chi tiết giao dịch Webhook.
- 🎟️ **Quản lý Mã giảm giá (Voucher Management)**: Tạo và phát hành voucher theo phần trăm (%) hoặc số tiền cố định, giới hạn số lần sử dụng và thời hạn áp dụng.
- 📤 **Xuất dữ liệu Báo cáo (Data Export)**: Hỗ trợ xuất dữ liệu đơn hàng, sản phẩm và báo cáo ra các định dạng **Excel (XLSX)**, **PDF**, **CSV**.
- ⚡ **Đồng bộ dữ liệu AI (Vector Store Sync)**: Tự động đồng bộ (hoặc Bulk Sync) dữ liệu sản phẩm từ MySQL sang ChromaDB Vector Store khi có thay đổi.

---

## 🏗️ Kiến Trúc Hệ Thống

Dự án được xây dựng theo kiến trúc **Hybrid Microservices**:

```text
 ┌───────────────────────────────────────────────────────────┐
 │                   Người Dùng (Browser)                    │
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
│  SePay / VietQR  │ │  FastAPI RAG AI  │ │     MySQL DB     │
│   Payment Gateway│ │   Microservice   │ │ (Hệ thống dữ liệu│
└──────────────────┘ └─────────┬────────┘ │    chính)        │
                               │          └──────────────────┘
                               ▼
                     ┌──────────────────┐
                     │ ChromaDB Vector  │
                     │  & Gemini/OpenAI │
                     └──────────────────┘
```

---

## 🛠️ Công Nghệ Sử Dụng

### Backend (Web Core)
* **Java 17** & **Spring Boot 3.5.6**
* **Spring Security 6**: Phân quyền Role-based (ROLE_USER, ROLE_ADMIN) & bảo mật đường dẫn.
* **Spring Data JPA & Hibernate**: Quản lý thao tác cơ sở dữ liệu.
* **Spring Mail & SendGrid API**: Gửi email thông báo và mã xác thực OTP.
* **Lombok & Google Guava**: Tối ưu mã nguồn và các tiện ích data structure.

### Frontend
* **Thymeleaf Template Engine**: Render giao diện server-side linh hoạt.
* **HTML5, CSS3, JavaScript (ES6+)**: Giao diện hiện đại, responsive thích ứng mọi thiết bị.
* **Thymeleaf Extras Spring Security 6**: Phân quyền hiển thị theo vai trò người dùng trên UI.

### AI Microservice (Tùy chọn kết nối)
* **Python 3.10+** & **FastAPI**
* **ChromaDB**: CSDL Vector lưu trữ thông tin sản phẩm và embedding.
* **Google Gemini API / OpenAI API**: Xử lý ngôn ngữ tự nhiên và tổng hợp câu trả lời tư vấn.

### Database & Build Tools
* **MySQL 8.0** (Môi trường Production/Dev) / **H2 Database** (Môi trường Test).
* **Apache Maven**: Quản lý thư viện và build project.

---

## 📂 Cấu Trúc Thư Mục

```text
LaptopStore/
├── src/
│   ├── main/
│   │   ├── java/com/techstore/techstore/
│   │   │   ├── Controller/          # REST & MVC Controllers (Admin, Product, Cart, Order, RAG AI, Webhook)
│   │   │   ├── Service/             # Xử lý nghiệp vụ chính (Order, Payment, RAG, Auth, OTP, Product...)
│   │   │   ├── Repository/          # Spring Data JPA Repositories
│   │   │   ├── entity/              # JPA Entities (User, Product, Order, Voucher, Brand...)
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── security/            # Cấu hình Spring Security, Authentication Provider
│   │   │   ├── config/              # Các cấu hình hệ thống (Web, Mail, Async, WebClient...)
│   │   │   └── enums/               # Các hằng số định danh (OrderStatus, PaymentStatus, Role...)
│   │   └── resources/
│   │       ├── templates/           # Giao diện Thymeleaf HTML (Client & Admin)
│   │       ├── static/              # Tài nguyên tĩnh (CSS, JS, Images)
│   │       ├── application.properties # File cấu hình môi trường
│   │       └── laptops_sample.csv   # Dữ liệu mẫu laptop
├── docs/                            # Tài liệu kỹ thuật chi tiết & Hướng dẫn tích hợp AI RAG
├── pom.xml                          # File quản lý dependencies Maven
└── README.md                        # Tài liệu hướng dẫn dự án
```

---

## ⚙️ Hướng Dẫn Cài Đặt & Chạy Dự Án

### 📋 Yêu cầu môi trường
* **Java Development Kit (JDK)**: phiên bản 17 trở lên.
* **Apache Maven**: phiên bản 3.8+ (hoặc dùng `mvnw` đi kèm).
* **MySQL Server**: phiên bản 8.0 trở lên.
* **Git**.

---

### 1️⃣ Clone Repository
```bash
git clone https://github.com/danhpham28104/LaptopStore.git
cd LaptopStore
```

---

### 2️⃣ Cấu hình Cơ sở dữ liệu & Biến môi trường
Tạo cơ sở dữ liệu trong MySQL:
```sql
CREATE DATABASE techstore_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Mở file `src/main/resources/application.properties` và chỉnh sửa các thông tin kết nối DB (hoặc thiết lập biến môi trường tương ứng):

```properties
# MySQL Datasource
spring.datasource.url=jdbc:mysql://localhost:3306/techstore_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Ho_Chi_Minh
spring.datasource.username=root
spring.datasource.password=your_mysql_password

# JPA configuration
spring.jpa.hibernate.ddl-auto=update

# SePay Payment Gateway Configuration
payment.sepay.va-account=5282587777
payment.sepay.bank-code=MBBank

# RAG AI Microservice (nếu sử dụng)
rag.service.url=http://127.0.0.1:8000
```

---

### 3️⃣ Chạy Ứng Dụng Spring Boot

Sử dụng Maven Wrapper:

**Trên Windows (PowerShell / CMD):**
```powershell
.\mvnw.cmd spring-boot:run
```

**Trên Linux / macOS:**
```bash
./mvnw spring-boot:run
```

---

### 4️⃣ Truy Cập Ứng Dụng
Sau khi ứng dụng khởi động thành công tại cổng `8080`:

* 🌐 **Trang chủ Khách hàng**: [http://localhost:8080/](http://localhost:8080/)
* 🔐 **Trang Quản trị Admin**: [http://localhost:8080/admin](http://localhost:8080/admin)

*(Gợi ý: Dữ liệu mẫu sẽ tự động được khởi tạo nếu cấu hình `app.seed.demo=true`)*.

---

## 🌐 Các API Endpoints Chính

| Phương thức | Đường dẫn API | Mô tả |
| :--- | :--- | :--- |
| `GET` | `/` | Trang chủ hiển thị danh sách sản phẩm |
| `GET` | `/products/{id}` | Xem chi tiết sản phẩm & cấu hình biến thể |
| `POST` | `/cart/add` | Thêm sản phẩm vào giỏ hàng |
| `GET` | `/checkout` | Trang xác nhận đơn hàng & áp mã giảm giá |
| `POST` | `/api/rag/chat` | Endpoint giao tiếp với Trợ lý AI Copilot |
| `POST` | `/api/sepay/webhook` | Webhook tiếp nhận thông báo thanh toán tự động từ SePay |
| `GET` | `/admin` | Dashboard quản trị doanh thu & đơn hàng |
| `GET` | `/admin/products` | Quản lý danh sách sản phẩm |
| `GET` | `/admin/analytics/export/excel` | Xuất báo cáo doanh thu ra file Excel |

---


## ✍️ Tác Giả & Liên Hệ

* **Phạm Danh**
* **GitHub**: [@danhpham28104](https://github.com/danhpham28104)
* **Project Repository**: [LaptopStore_DATN](https://github.com/danhpham28104/LaptopStore)

---

## 📄 Giấy Phép (License)

Dự án được phát triển phục vụ mục đích học tập, nghiên cứu và làm Đồ án / Portfolio cá nhân. Tự do tham khảo và phát triển thêm!

