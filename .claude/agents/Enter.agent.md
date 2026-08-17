# 🔌 API Documentation - OTP & Payment

---

## 📌 Base URL
```
http://localhost:8080
```

---

## 🔐 OTP API Endpoints

### 1. Gửi OTP SMS
**Endpoint:** `POST /otp/send`

**Parameters:**
```
phone: String (required) - Số điện thoại định dạng: 0xxxxxxxxx
```

**Example:**
```bash
curl -X POST "http://localhost:8080/otp/send?phone=0901234567" \
  -H "Content-Type: application/json"
```

**Response (Success - 200):**
```json
{
  "success": true,
  "message": "OTP đã gửi đến số điện thoại của bạn"
}
```

**Response (Error - 400):**
```json
{
  "success": false,
  "message": "Vui lòng chờ 45 giây trước khi gửi lại OTP"
}
```

**HTTP Status Codes:**
- `200` - OTP sent successfully
- `400` - Validation error (invalid phone, resend too fast)
- `401` - Not authenticated
- `500` - Server error

---

### 2. Xác thực OTP
**Endpoint:** `POST /otp/verify`

**Parameters:**
```
phone: String (required) - Số điện thoại
code: String (required)  - OTP 6 chữ số
orderId: Long (required) - ID đơn hàng
```

**Example:**
```bash
curl -X POST "http://localhost:8080/otp/verify" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "phone=0901234567&code=123456&orderId=1"
```

**Response (Success - 200):**
```json
{
  "success": true,
  "message": "OTP xác thực thành công",
  "redirectUrl": "/orders/1"
}
```

**Response (Error - 400):**
```json
{
  "success": false,
  "message": "OTP không chính xác. 3 lần thử còn lại."
}
```

**Possible Errors:**
- "OTP không tồn tại" - Chưa gửi OTP
- "OTP đã hết hạn" - Quá 5 phút
- "OTP không chính xác" - Nhập sai
- "Bạn đã nhập sai OTP 5 lần" - Vượt quá số lần

---

### 3. Gửi lại OTP
**Endpoint:** `POST /otp/resend`

**Parameters:**
```
phone: String (required) - Số điện thoại
```

**Example:**
```bash
curl -X POST "http://localhost:8080/otp/resend?phone=0901234567"
```

**Response (Success - 200):**
```json
{
  "success": true,
  "message": "OTP mới đã được gửi"
}
```

**Note:** Chỉ được gửi lại sau 60 giây

---

### 4. Trang OTP Verification (GET)
**Endpoint:** `GET /otp/verify`

**Parameters:**
```
orderId: Long (required) - ID đơn hàng
```

**Returns:** HTML form nhập OTP

---

## 💳 Payment API Endpoints

### 5. Hiển thị QR Thanh toán
**Endpoint:** `GET /checkout/sepay`

**Parameters:**
```
orderId: Long (required) - ID đơn hàng
```

**Example:**
```
GET /checkout/sepay?orderId=1
```

**Requirements:**
- Order phải có OTP verified = true
- Order phải có payment
- Payment status = PENDING

**Returns:** HTML page hiển thị QR code

---

### 6. Kiểm tra Trạng thái Thanh toán
**Endpoint:** `GET /payment/check`

**Parameters:**
```
orderId: Long (required) - ID đơn hàng
```

**Example:**
```bash
curl -X GET "http://localhost:8080/payment/check?orderId=1"
```

**Response (200):**
```json
{
  "success": true,
  "message": "Payment status checked"
}
```

**What it does:**
1. Tìm order by ID
2. Lấy payment của order
3. Kiểm tra trạng thái với payment provider
4. Update order status nếu thanh toán thành công

---

## 🪝 Webhook Endpoints (nhận callback từ Payment Provider)

### 7. Webhook từ SEPAY
**Endpoint:** `POST /payment/webhook/sepay`

**Content-Type:** `application/json`

**Request Body:**
```json
{
  "orderCode": "DH20240101-1234",
  "transactionId": "SEPAY_TXN_ABC123",
  "status": "SUCCESS",
  "amount": 500000
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Webhook processed"
}
```

**Notes:**
- `status` có thể là: "SUCCESS" hoặc "FAILED"
- `amount` phải match với order total amount
- Nếu status = "SUCCESS" → order.orderStatus = "Paid"

---

### 8. Webhook từ VNPay
**Endpoint:** `POST /payment/webhook/vnpay`

**Content-Type:** `application/json`

**Request Body:**
```json
{
  "vnp_TxnRef": "DH20240101-1234",
  "vnp_TransactionNo": "VNPAY_123456",
  "vnp_ResponseCode": "00",
  "vnp_Amount": 500000
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Webhook processed"
}
```

**Notes:**
- `vnp_ResponseCode` = "00" là thành công
- Nếu khác "00" → FAILED

---

### 9. Webhook từ Momo
**Endpoint:** `POST /payment/webhook/momo`

**Content-Type:** `application/json`

**Request Body:**
```json
{
  "orderInfo": "DH20240101-1234",
  "transId": "MOMO_123456",
  "resultCode": 0,
  "amount": 500000
}
```

**Response (200):**
```json
{
  "resultCode": 0,
  "message": "Webhook processed"
}
```

**Notes:**
- `resultCode` = 0 là thành công
- `resultCode` ≠ 0 là thất bại

---

## 🔀 Checkout & Order Flow

### Luồng tổng quan
```
1. POST /checkout/place
   → Tạo order (tạm thời, otpVerified = false)
   → Chuyển hướng: GET /otp/verify?orderId=X

2. GET /otp/verify
   → Hiển thị form OTP

3. POST /otp/send
   → Gửi OTP qua SMS

4. POST /otp/verify
   → Xác thực OTP
   → Set order.otpVerified = true
   → Chuyển hướng: GET /checkout/sepay?orderId=X

5. GET /checkout/sepay
   → Tạo QR code
   → Hiển thị trang thanh toán

6. Automatic/Webhook
   → GET /payment/check (mỗi 5 giây)
   → hoặc POST /payment/webhook/SEPAY
   → Update order.orderStatus = "Paid"
   → Set payment.status = "SUCCESS"
```

---

## 📊 Entity Relationships

### Order Entity
```java
@Entity
public class Order {
    Long id;
    String orderCode;              // Mã đơn hàng duy nhất
    User user;
    String orderStatus;            // Pending, Confirmed, Paid, Shipped...
    Boolean otpVerified;           // ✅ Mới
    LocalDateTime paymentDeadline; // ✅ Mới
    Payment payment;               // Có 1 payment
    ...
}
```

### Payment Entity
```java
@Entity
public class Payment {
    Long id;
    Order order;                   // Liên kết với 1 order
    PaymentMethod method;          // COD, SEPAY
    PaymentStatus status;          // PENDING, SUCCESS, FAILED
    BigDecimal amount;
    String transactionId;
    String qrCodeUrl;              // ✅ Mới
    String qrCodeData;             // ✅ Mới
    ...
}
```

### OtpVerification Entity ✅ Mới
```java
@Entity
public class OtpVerification {
    Long id;
    String phone;
    String otpHash;                // Hash bcrypt, không plain text
    LocalDateTime expiryTime;      // Hết hạn sau 5 phút
    Integer attemptCount;
    String status;                 // SENT, VERIFIED, EXPIRED, FAILED
    LocalDateTime createdAt;
    LocalDateTime lastResendAt;
    LocalDateTime verifiedAt;
    ...
}
```

---

## ⏱️ Configuration

### OTP Settings (application.properties)
```properties
otp.length=6                       # 6 chữ số
otp.expiry-minutes=5              # Hết hạn sau 5 phút
otp.max-attempts=5                # Nhập sai tối đa 5 lần
otp.resend-wait-seconds=60        # Gửi lại sau 60 giây
```

### Payment Settings
```properties
order.payment-deadline-minutes=10 # Hết hạn thanh toán sau 10 phút
payment.sepay.va-account=...      # SEPAY account
payment.sepay.bank-code=...       # Bank code
```

---

## 🔒 Security

### OTP Security
- ✅ OTP được hash với bcrypt
- ✅ So sánh OTP dùng passwordEncoder.matches()
- ✅ Giới hạn số lần nhập sai
- ✅ Kiểm tra hết hạn
- ✅ Không lưu plain text

### Payment Security
- ✅ Payment deadline kiểm tra
- ✅ Order status tracking
- ✅ Webhook signature verification (TODO)
- ✅ Transaction ID unique constraint

### Best Practices
- ❌ Không hard-code sensitive data
- ✅ Dùng environment variables
- ✅ Validate input
- ✅ Proper error handling

---

## 📝 Logging

### Enable Debug Logging
```properties
# application.properties
logging.level.com.techstore.techstore=DEBUG
logging.level.com.techstore.techstore.Service.OtpService=DEBUG
logging.level.com.techstore.techstore.Service.PaymentStatusCheckService=DEBUG
```

### View Logs
```bash
# Real-time logs
mvn spring-boot:run | grep -i "otp\|payment"

# Or check application logs
tail -f logs/application.log
```

---

## 🧪 Test with cURL

### Test OTP Flow
```bash
# 1. Send OTP
curl -X POST "http://localhost:8080/otp/send?phone=0901234567"

# 2. Check console for OTP code (example: 123456)

# 3. Verify OTP
curl -X POST "http://localhost:8080/otp/verify" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "phone=0901234567&code=123456&orderId=1"
```

### Test Payment Webhook (Mock)
```bash
# Mock SEPAY success
curl -X POST "http://localhost:8080/payment/webhook/sepay" \
  -H "Content-Type: application/json" \
  -d '{
    "orderCode": "DH20240101-1234",
    "transactionId": "TEST_SUCCESS",
    "status": "SUCCESS",
    "amount": 500000
  }'

# Mock SEPAY failure
curl -X POST "http://localhost:8080/payment/webhook/sepay" \
  -H "Content-Type: application/json" \
  -d '{
    "orderCode": "DH20240101-1234",
    "transactionId": "TEST_FAIL",
    "status": "FAILED",
    "amount": 500000
  }'
```

---

**Đó là toàn bộ API Documentation! 🎉**
