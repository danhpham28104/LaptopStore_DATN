-- =============================================================================
-- LAPTOPSTORE — PHASE 1 MIGRATION & INDEXES SCRIPT
-- =============================================================================

-- 1. Chuẩn hóa OrderStatus và PaymentStatus column length
ALTER TABLE orders MODIFY COLUMN order_status VARCHAR(30) NOT NULL;
ALTER TABLE payment MODIFY COLUMN status VARCHAR(30) NOT NULL;

-- 2. Performance Indexes cho orders table
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_status ON orders(order_status);
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at);

-- 3. Performance Indexes cho order_item table
CREATE INDEX idx_oi_product_id ON order_item(product_id);
CREATE INDEX idx_oi_order_id ON order_item(order_id);

-- 4. Performance Indexes cho payment table
CREATE INDEX idx_payment_status ON payment(status);
CREATE INDEX idx_payment_created_at ON payment(created_at);

-- 5. Data Migration (Tùy chọn: Chuẩn hóa dữ liệu cũ sang enum name mới)
-- UPDATE orders SET order_status = 'PENDING' WHERE order_status = 'PENDING_PAYMENT';
-- UPDATE orders SET order_status = 'PROCESSING' WHERE order_status = 'PACKING';
-- UPDATE payment SET status = 'PAID' WHERE status = 'SUCCESS';
