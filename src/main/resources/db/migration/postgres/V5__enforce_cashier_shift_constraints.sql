ALTER TABLE cashier_shifts
    ADD CONSTRAINT chk_cashier_shift_status
        CHECK (status IN ('OPEN', 'CLOSED')),
    ADD CONSTRAINT chk_cashier_shift_closing_values
        CHECK (
            (
                status = 'OPEN'
                AND closed_at IS NULL
                AND expected_closing_cash IS NULL
                AND actual_closing_cash IS NULL
                AND cash_difference IS NULL
            )
            OR
            (
                status = 'CLOSED'
                AND closed_at IS NOT NULL
                AND expected_closing_cash IS NOT NULL
                AND actual_closing_cash IS NOT NULL
                AND cash_difference IS NOT NULL
                AND cash_difference = actual_closing_cash - expected_closing_cash
            )
        );

ALTER TABLE cash_movements
    ADD CONSTRAINT chk_cash_movement_source_type
        CHECK (
            source_type IN (
                'BOOKING_PAYMENT',
                'ORDER_PAYMENT',
                'REFUND',
                'CASH_ADJUSTMENT',
                'OTHER'
            )
        );

CREATE INDEX idx_cashier_shifts_staff_opened
    ON cashier_shifts(staff_id, opened_at DESC);

CREATE INDEX idx_cashier_shifts_venue_status_opened
    ON cashier_shifts(venue_id, status, opened_at DESC);

CREATE INDEX idx_cash_movements_shift_created
    ON cash_movements(cashier_shift_id, created_at);
