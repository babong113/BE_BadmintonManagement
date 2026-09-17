ALTER TABLE bookings
    ADD CONSTRAINT chk_booking_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'EXPIRED'));

CREATE INDEX idx_bookings_pending_expiration
    ON bookings(created_at)
    WHERE status = 'PENDING';
