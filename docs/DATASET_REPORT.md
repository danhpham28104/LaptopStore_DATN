# Báo cáo Bộ Dữ liệu Demo LaptopStore (`DATASET_REPORT`)

## 1. Tổng quan Phân tích Schema & Ràng buộc Hiện tại

Trước khi sinh dữ liệu, toàn bộ các Entity (`Product`, `ProductVariant`, `Brand`, `Category`, `Review`, `StockLog`, `Voucher`, `Order`, `OrderItem`, `User`, `Address`, `Payment`, `OrderStatusHistory`, `VoucherUsage`) và Repository/Service liên quan đã được kiểm tra:

- **Cơ chế lưu hình ảnh**: Project lưu danh sách đường dẫn ảnh sản phẩm ngăn cách bằng dấu `,` (không khoảng trắng) trong `product.images` (`TEXT`) và ảnh biến thể trong `product_variant.image` (`TEXT`). Không tồn tại bảng `product_image` tách rời.
- **Tương thích `DemoDataSeeder` & `RagIntegrationService`**:
  - Giữ nguyên giá trị `product.model` của 12 sản phẩm mẫu đầu tiên (`id` 1..12) để `DemoDataSeeder` không sinh bản ghi trùng khi khởi động.
  - Chuẩn hóa `product.ram` dạng số GB (`8GB`, `16GB`, `24GB`, `32GB`) để `RagIntegrationService.parseRam()` bóc tách chính xác, và chuẩn hóa `product.display` bắt đầu bằng kích thước màn hình (inch) để `parseScreenSize()` đọc đúng.
- **Tương thích bộ lọc giao diện (`home.html` & `ProductRepository.searchAdvanced`)**:
  - Tên màu biến thể (`product_variant.color`) sử dụng chuẩn tiếng Anh (`Gray`, `Black`, `Silver`, `White`, `Blue`, `Gold`) và `product_variant.storage` chuẩn hóa dạng `RAM / SSD` (ví dụ `16GB / 512GB`, `24GB / 1TB`) để bộ lọc theo màu sắc, RAM và dung lượng SSD hoạt động chính xác.
- **Quy tắc đánh giá (`ReviewService` & `DataInitializer`)**:
  - Đảm bảo 100% đánh giá gắn duy nhất với `(user_id, product_id, order_item_id)` thuộc các đơn hàng đã hoàn tất (`HOAN_THANH` / `DA_GIAO_HANG`), đồng bộ chính xác `product.average_rating` và `product.review_count`.
- **Biến thể (`product_variant`)**:
  - Bổ sung idempotent hai cột nullable `sku` (chỉ mục duy nhất `uk_variant_sku`) và `price` (`DECIMAL(19,2)`) để mỗi biến thể có **SKU riêng** và **Giá riêng**, vẫn giữ nguyên tương thích 100% với JPA/Hibernate `ddl-auto=update`.

---

## 2. Thống kê Chi tiết Bộ Dữ liệu

| Chỉ số | Trước khi seed | Bổ sung / Sinh mới | Tổng sau khi import |
| :--- | :---: | :---: | :---: |
| **Sản phẩm (`product`)** | 37 (đã cập nhật đầy đủ thông số) | **+83** | **120** |
| **Biến thể (`product_variant`)** | 21 (đã chuẩn hóa SKU, giá, màu, SSD) | **+262** | **283** |
| **Đường dẫn ảnh sản phẩm (`product.images`)** | 49 | +551 | **600** (4–5 ảnh/SP) |
| **File ảnh vật lý duy nhất được sử dụng** | 29 | +262 | **291** (tổng 883 tham chiếu gồm ảnh biến thể) |
| **Đánh giá (`reviews`)** | 1 | **+320** | **321** |
| **Khách hàng (`users` ROLE_USER)** | 0 (chỉ có 2 admin/kho) | **+40** | **42** (40 khách hàng) |
| **Đơn hàng (`orders`)** | 2 | **+200** | **202** |
| **Chi tiết đơn hàng (`orderitem`)** | 2 | **+320** | **322** |
| **Tổng tồn kho (`SUM(product.stock)`)** | 186 | — | **2,412 máy** |
| **Nhật ký kho (`stock_log`)** | 2 | **+283** | **285** |
| **Mã giảm giá (`voucher`)** | 1 | **+8** | **9** |
| **Thương hiệu (`brand`)** | 8 | **+1 (Gigabyte)** | **9** |
| **Danh mục (`category`)** | 4 | **+1 (Laptop Doanh nhân)** | **5** |

---

## 3. Phân bổ theo Thương hiệu & Phân khúc

### 3.1. Phân bổ theo Thương hiệu (120 sản phẩm)

| Thương hiệu | Số lượng sản phẩm | Tỷ lệ |
| :--- | :---: | :---: |
| **ASUS** | 18 | 15.0% |
| **Dell** | 17 | 14.2% |
| **Lenovo** | 17 | 14.2% |
| **HP** | 17 | 14.2% |
| **Acer** | 17 | 14.2% |
| **MSI** | 17 | 14.2% |
| **Gigabyte** | 14 | 11.7% |
| **Apple** | 2 | 1.7% |
| **LG** | 1 | 0.8% |

### 3.2. Phân bổ theo Phân khúc (Danh mục)

| Danh mục | Slug | Số lượng sản phẩm |
| :--- | :--- | :---: |
| **Laptop Gaming** | `laptop-gaming` | 38 |
| **Laptop Đồ họa** | `laptop-do-hoa` | 25 |
| **Laptop Sinh viên** | `laptop-sinh-vien` | 20 |
| **Laptop Văn phòng** | `laptop-van-phong` | 19 |
| **Laptop Doanh nhân** | `laptop-doanh-nhan` | 18 |

---

## 4. Phân bố Tồn kho, Giá bán & Đánh giá

### 4.1. Phân bố Tồn kho (Ngẫu nhiên, không đồng nhất)
- **Tổng tồn kho toàn hệ thống**: **2,412 máy** (đồng bộ 100% `product.stock = SUM(product_variant.stock)`).
- **Laptop phổ thông / tầm trung (69 model)**: dao động **12–44 máy/model** (trung bình 28.1 máy).
- **Laptop cao cấp / Workstation / Flagship (51 model)**: dao động **4–15 máy/model** (trung bình 9.3 máy).
- **Biến thể cấu hình hiếm / cao cấp (24GB/32GB/64GB RAM, 1TB/2TB SSD, màu đặc biệt)**: tồn kho **1–8 máy/biến thể** (180 biến thể có tồn kho ≤ 8).

### 4.2. Dải Giá bán Biến thể
- Khoảng giá bán niêm yết trên 283 biến thể: **12,190,000 ₫ – 80,490,000 ₫**.
- Mỗi sản phẩm có **2–4 biến thể** khác nhau về màu sắc, dung lượng RAM/SSD, **SKU riêng** (`SKU-LP...`) và **giá bán/giá nhập riêng**.

### 4.3. Phân bố Đánh giá Tiếng Việt (321 đánh giá)
- Toàn bộ đánh giá đều liên kết hợp lệ với đơn hàng đã giao/hoàn thành (`DA_GIAO_HANG` / `HOAN_THANH`) của 40 tài khoản khách hàng (`khachhang01`..`khachhang40`).
- Phân bố số sao thực tế:
  - **5 sao**: 186 lượt (57.9%)
  - **4 sao**: 81 lượt (25.2%)
  - **3 sao**: 32 lượt (10.0%)
  - **2 sao**: 13 lượt (4.0%)
  - **1 sao**: 9 lượt (2.8%)

---

## 5. Danh sách Ảnh & Cơ chế Lưu trữ

- Toàn bộ hình ảnh được lưu nội bộ dưới thư mục `src/main/resources/static/images/products/` và logo thương hiệu tại `src/main/resources/static/images/brands/gigabyte.png`.
- Mỗi sản phẩm có **4–5 góc ảnh chính** (`-front.png`, `-angle.png`, `-keyboard.png`, `-ports.png` và ảnh đại diện màu sắc) cộng thêm ảnh riêng theo từng biến thể màu (`-gray.png`, `-black.png`, `-silver.png`, `-white.png`, `-blue.png`, `-gold.png`), đảm bảo hiển thị **5–8 ảnh** sắc nét trên trang chi tiết và **0 link hỏng / 0 lỗi 404**.

---

## 6. Nguồn Dữ liệu Tham khảo

Thông số kỹ thuật (CPU, GPU, RAM, SSD, độ phân giải màn hình, trọng lượng, kích thước, dung lượng pin, cổng kết nối) và giá bán tham khảo tại Việt Nam được đối chiếu từ các hệ thống bán lẻ và trang chính hãng uy tín:
1. **CellphoneS** (`cellphones.com.vn`) — Thông số & ảnh sản phẩm các dòng ASUS, Lenovo, Acer, Dell, HP, MSI, Gigabyte, Apple.
2. **Thế Giới Di Động & Điện Máy Xanh** (`thegioididong.com`, `dienmayxanh.com`) — Thông số kích thước, trọng lượng, màn hình OLED/WUXGA.
3. **FPT Shop & Phong Vũ** (`fptshop.com.vn`, `phongvu.vn`) — Thông số Laptop Gaming (TUF, Nitro, LOQ, Victus, Katana, Gigabyte G5/G6/AORUS).
4. **Trang chính hãng các nhà sản xuất**: Lenovo PSREF, ASUS Global/VN, Acer Official, Dell Support/Specs, HP Official Specs, MSI Global, Gigabyte Official (`gigabyte.com`), Apple (`apple.com`), LG (`lg.com`).
