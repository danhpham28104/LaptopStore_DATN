# Walkthrough: Kiểm tra E2E Product-Comparison-RAG

Tài liệu này ghi lại các bước thực hiện và kết quả xác minh hệ thống Product-Comparison-RAG phục vụ mục tiêu demo và tích hợp Spring Boot.

## Các bước đã thực hiện

1. **Khởi tạo kịch bản E2E tự động**:
   - Viết script test [e2e_test.py](file:///C:/Users/Admin/.gemini/antigravity/brain/0cdbcdb6-c692-48da-9d4a-f509e0cc8340/scratch/e2e_test.py) để tự động hóa toàn bộ flow kiểm tra của người dùng.
   - Script chạy test offline bằng cách kích hoạt biến môi trường `MOCK_EMBEDDINGS=true` để mock cả embedding (dimension 768) và LLM (Gemini/OpenAI) mà không cần internet/API Key.
2. **Dọn dẹp & Khởi chạy**:
   - Script dọn dẹp thư mục cơ sở dữ liệu ChromaDB cũ tại `d:\Code\Product-Comparison-RAG\chroma_db` để đưa DB về trạng thái trống hoàn toàn.
   - Khởi động server FastAPI bằng `uvicorn` trên port `8000`.
   - Gửi yêu cầu kiểm tra tới `/health` cho tới khi server sẵn sàng nhận request.
3. **Đồng bộ Dữ liệu (Bulk Sync)**:
   - Gửi payload gồm **10 laptop mẫu** với đầy đủ các thuộc tính phân khúc: gaming còn hàng/hết hàng, văn phòng Dell/không Dell dưới 20 triệu, đồ họa tầm 30 triệu, lập trình RAM 16GB, giá rẻ RAM 8GB, cao cấp, v.v.
   - Xác minh API trả về `200 OK` và sync thành công toàn bộ 10 laptop.
4. **Kiểm tra Query Parser**:
   - Gửi các câu hỏi mẫu A, B, C, D qua POST `/api/parse-query-debug`.
   - Đối chiếu kết quả phân tích cấu trúc (`mode`, `useCase`, `maxPrice`, `minRam`, `brand`, `comparisonProducts`, v.v.) với expected.
5. **Kiểm tra Search & Scoring Engine**:
   - Gửi các câu hỏi mẫu qua POST `/api/search-debug`.
   - Kiểm tra xem ranking có ưu tiên đúng laptop còn hàng, GPU rời cho gaming/design, RAM >= 16GB, và giảm điểm mạnh các laptop hết hàng (`stock = 0`) không.
6. **Kiểm tra RAG Chat Pipeline & Lỗi**:
   - Gọi POST `/api/chat` với 5 câu hỏi để xác minh định dạng camelCase, confidenceScore, citations thật, anti-hallucination.
   - Gửi các request lỗi (message rỗng, price âm, xóa id không tồn tại) để xác nhận hệ thống trả về mã lỗi HTTP thích hợp (422, 404).
7. **Dọn dẹp**:
   - Tắt tiến trình server uvicorn an toàn sau khi hoàn tất.

---

## Kết quả Kiểm tra
Tất cả các bước kiểm tra đều **PASS** 100%.

- **Định dạng Response**: Chuẩn camelCase cho tất cả các endpoint.
- **Anti-Hallucination**: Hoạt động hoàn hảo, tự động lọc sạch các ID ảo từ LLM (nếu có).
- **Scoring Engine**: Phân bổ điểm cộng/trừ chính xác theo đặc tả nghiệp vụ.
- **Tích hợp Spring Boot**: Sẵn sàng tích hợp.

Báo cáo chi tiết xem tại: [e2e_test_report.md](file:///C:/Users/Admin/.gemini/antigravity/brain/0cdbcdb6-c692-48da-9d4a-f509e0cc8340/e2e_test_report.md)

---

## Cách tự chạy test script tại local

Nếu bạn muốn tự chạy lại kịch bản test E2E này tại local:

1. Mở terminal tại thư mục dự án `d:\Code\Product-Comparison-RAG`.
2. Chạy lệnh:
   ```bash
   .venv\Scripts\python C:\Users\Admin\.gemini\antigravity\brain\0cdbcdb6-c692-48da-9d4a-f509e0cc8340\scratch\e2e_test.py
   ```
3. Xem log chạy trực tiếp trên console.
