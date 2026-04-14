# LaptopStore 🛒💻

LaptopStore là một **website bán laptop trực tuyến** được xây dựng bằng **Spring Boot**, phục vụ mục đích học tập và xây dựng portfolio cá nhân. Dự án mô phỏng đầy đủ các chức năng cơ bản của một hệ thống thương mại điện tử.

---

## 🚀 Chức năng chính

### 👤 Người dùng

* Xem danh sách sản phẩm laptop
* Xem chi tiết sản phẩm
* Thêm sản phẩm vào giỏ hàng
* Đặt hàng (Checkout)
* Mua nhanh (Buy Now)

### 🔐 Quản trị viên (Admin)

* Đăng nhập quản trị
* Quản lý sản phẩm (CRUD)
* Xem dashboard quản trị

---

## 🛠️ Công nghệ sử dụng

* **Backend**: Java, Spring Boot
* **Frontend**: Thymeleaf, HTML, CSS, JavaScript
* **Database**: MySQL (hoặc H2 cho môi trường test)
* **ORM**: Spring Data JPA (Hibernate)
* **Security**: Spring Security
* **Build Tool**: Maven

---

## 📂 Cấu trúc thư mục chính

```text
LaptopStore
├── src/main/java/com/techstore/techstore
│   ├── Controller
│   ├── Service
│   ├── Repository
│   ├── entity
│   ├── security
│   └── config
│
├── src/main/resources
│   ├── static
│   │   ├── css
│   │   ├── js
│   │   └── images
│   ├── templates
│   └── application.properties
│
├── pom.xml
└── README.md
```

---

## ⚙️ Cách chạy project

### 1️⃣ Clone repository

```bash
git clone https://github.com/danhpham28104/LaptopStore.git
cd LaptopStore
```

### 2️⃣ Cấu hình database

Chỉnh sửa file `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/laptopstore
spring.datasource.username=root
spring.datasource.password=your_password
```

### 3️⃣ Chạy ứng dụng

```bash
mvn spring-boot:run
```

Hoặc chạy trực tiếp từ IDE (IntelliJ / Eclipse).

---

## 🌐 Truy cập ứng dụng

* Trang chủ: `http://localhost:8080/`
* Trang admin: `http://localhost:8080/admin`

---

## 📌 Mục đích dự án

* Luyện tập **Spring Boot** và **Spring Security**
* Hiểu quy trình xây dựng **Web E-commerce**
* Sử dụng làm **project portfolio**

---

## ✍️ Tác giả

* **Pham Danh**
* GitHub: [@danhpham28104](https://github.com/danhpham28104)

---

## 📄 Giấy phép

Dự án được xây dựng cho mục đích học tập. Bạn có thể tự do tham khảo và phát triển thêm.
