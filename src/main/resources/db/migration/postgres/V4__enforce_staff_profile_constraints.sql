UPDATE staff_profiles
SET position = 'CASHIER'
WHERE position IS NULL OR BTRIM(position) = '';

DO $$
BEGIN
    IF EXISTS (
        SELECT LOWER(BTRIM(employee_code))
        FROM staff_profiles
        GROUP BY LOWER(BTRIM(employee_code))
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Khong the chuan hoa employee_code: ton tai ma trung khong phan biet hoa thuong';
    END IF;
END
$$;

UPDATE staff_profiles
SET employee_code = UPPER(BTRIM(employee_code)),
    position = UPPER(BTRIM(position));

ALTER TABLE staff_profiles
    ALTER COLUMN position SET DEFAULT 'CASHIER',
    ALTER COLUMN position SET NOT NULL;

CREATE UNIQUE INDEX uq_staff_employee_code_ci
    ON staff_profiles (LOWER(employee_code));
