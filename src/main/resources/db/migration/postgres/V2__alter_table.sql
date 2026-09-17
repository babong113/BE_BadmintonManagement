-- =========================================================
-- 1. Bổ sung venue_id cho bảng ORDERS
-- (Để biết đơn bán lẻ thuộc chi nhánh nào)
-- =========================================================
ALTER TABLE orders
    ADD COLUMN venue_id BIGINT NOT NULL;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_venue
        FOREIGN KEY (venue_id) REFERENCES venues(id);


-- =========================================================
-- 2. Cập nhật bảng PAYMENTS
-- (Cho phép thanh toán cả Booking lẫn Order bán lẻ)
-- =========================================================
-- Cho phép booking_id có thể NULL
ALTER TABLE payments
    ALTER COLUMN booking_id DROP NOT NULL;

-- Thêm cột order_id
ALTER TABLE payments
    ADD COLUMN order_id BIGINT;

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;

-- Ràng buộc: Thanh toán phải thuộc về HOẶC Booking HOẶC Order
ALTER TABLE payments
    ADD CONSTRAINT chk_payment_target
        CHECK (
            (booking_id IS NOT NULL AND order_id IS NULL)
                OR
            (booking_id IS NULL AND order_id IS NOT NULL)
            );