# Hướng dẫn kiểm tra Cashier Shift API bằng Postman

Tài liệu này kiểm tra `FR-CSH-01` đến `FR-CSH-16` và sự tích hợp dòng tiền với Payment API.

## 1. Chuẩn bị

Chạy backend:

```powershell
.\mvnw.cmd spring-boot:run
```

Base URL:

```text
http://localhost:7000
```

Cần chuẩn bị:

- Một tài khoản role `STAFF`, có `staff_profiles.position = CASHIER` và trạng thái `ACTIVE`.
- Một tài khoản `OWNER` có permission `staff:manage`.
- Một venue trạng thái `ACTIVE`.
- Một booking hoặc order tại venue đó để kiểm tra Payment CASH.

Tạo biến Postman:

| Biến | Giá trị |
| --- | --- |
| `baseUrl` | `http://localhost:7000` |
| `staffToken` | Token của CASHIER |
| `ownerToken` | Token OWNER |
| `venueId` | ID venue ACTIVE |
| `shiftId` | Lấy từ response mở ca |
| `bookingId` | Booking cùng venue |
| `orderId` | Order cùng venue |

Đăng nhập:

```http
POST {{baseUrl}}/api/auth/login
Content-Type: application/json
```

```json
{
  "email": "cashier01@example.com",
  "password": "123456"
}
```

## 2. Danh sách endpoint

| Method | Endpoint | Actor | Chức năng |
| --- | --- | --- | --- |
| `POST` | `/api/cashier-shifts/open` | A2 | Mở ca và nhập tiền đầu ca |
| `GET` | `/api/cashier-shifts/current/status` | A2 | Kiểm tra có ca OPEN hay không |
| `GET` | `/api/cashier-shifts/current` | A2 | Xem ca hiện tại và tiền dự kiến |
| `GET` | `/api/cashier-shifts/current/movements` | A2 | Xem dòng tiền trong ca |
| `POST` | `/api/cashier-shifts/current/cash-out` | A2 | Ghi nhận điều chỉnh tiền ra |
| `PATCH` | `/api/cashier-shifts/current/close` | A2 | Nhập tiền thực tế và đóng ca |
| `GET` | `/api/cashier-shifts/me?from=&to=` | A2 | Lịch sử ca cá nhân |
| `GET` | `/api/cashier-shifts?staffId=&venueId=&status=&from=&to=` | A3 | Lịch sử toàn bộ ca |
| `GET` | `/api/cashier-shifts/{id}` | A3 | Chi tiết đối soát ca |

Các API A2 yêu cầu role `STAFF` và service kiểm tra thêm `position = CASHIER`. API A3 yêu cầu `staff:manage`.

## 3. FR-CSH-01/02/03/04 - Mở ca

```http
POST {{baseUrl}}/api/cashier-shifts/open
Authorization: Bearer {{staffToken}}
Content-Type: application/json
```

```json
{
  "venueId": 1,
  "openingCash": 500000.00,
  "notes": "Nhan ban giao ket ca sang"
}
```

Kết quả mong đợi: `201 Created`.

```json
{
  "success": true,
  "message": "Mo ca thu ngan thanh cong",
  "data": {
    "id": 1,
    "employeeCode": "NV-001",
    "venueId": 1,
    "openingCash": 500000.00,
    "cashIn": 0.00,
    "cashOut": 0.00,
    "expectedClosingCash": 500000.00,
    "actualClosingCash": null,
    "cashDifference": null,
    "status": "OPEN"
  }
}
```

Lưu `data.id` vào `shiftId`. Gọi lại endpoint mở ca khi ca đầu vẫn `OPEN` phải bị từ chối với `Nhan vien da co ca thu ngan OPEN`. Constraint database cũng bảo đảm một nhân viên chỉ có một ca OPEN.

Kiểm tra trạng thái:

```http
GET {{baseUrl}}/api/cashier-shifts/current/status
Authorization: Bearer {{staffToken}}
```

```json
{
  "success": true,
  "data": {
    "open": true,
    "shiftId": 1,
    "venueId": 1
  }
}
```

## 4. FR-CSH-05/09 - Xem ca và tiền dự kiến

```http
GET {{baseUrl}}/api/cashier-shifts/current
Authorization: Bearer {{staffToken}}
```

Response có venue, thời gian mở, tiền đầu ca và công thức:

```text
expectedClosingCash = openingCash + cashIn - cashOut
```

Khi ca chưa đóng, `expectedClosingCash` vẫn được tính theo dữ liệu mới nhất; `actualClosingCash` và `cashDifference` bằng `null`.

## 5. FR-CSH-06 - CASH_IN từ Payment

Sau khi mở ca, tạo payment CASH cho booking cùng venue:

```http
POST {{baseUrl}}/api/payments/bookings/{{bookingId}}
Authorization: Bearer {{staffToken}}
Content-Type: application/json
```

```json
{
  "amount": 100000.00,
  "paymentMethod": "CASH",
  "transactionCode": "CASH-BKG-001",
  "allocations": null
}
```

Hoặc payment CASH cho order:

```http
POST {{baseUrl}}/api/payments/orders/{{orderId}}
Authorization: Bearer {{staffToken}}
Content-Type: application/json
```

```json
{
  "amount": 50000.00,
  "paymentMethod": "CASH",
  "transactionCode": "CASH-ORD-001",
  "allocations": null
}
```

Payment phải trả `cashierShiftId = {{shiftId}}`. Dòng tiền tương ứng là:

- Booking: `CASH_IN / BOOKING_PAYMENT`.
- Order: `CASH_IN / ORDER_PAYMENT`.

Nếu chưa mở ca hoặc ca thuộc venue khác, Payment CASH phải bị từ chối.

## 6. FR-CSH-07 - Ghi nhận CASH_OUT

Điều chỉnh tiền ra thủ công:

```http
POST {{baseUrl}}/api/cashier-shifts/current/cash-out
Authorization: Bearer {{staffToken}}
Content-Type: application/json
```

```json
{
  "amount": 20000.00,
  "description": "Rut tien mua vat tu"
}
```

Kết quả `201 Created`, movement có `CASH_OUT / CASH_ADJUSTMENT`. Số tiền chi không được vượt tiền dự kiến đang có trong ca.

Refund một Payment CASH qua `/api/payments/{paymentId}/refund` sẽ tự động tạo `CASH_OUT / REFUND` trong ca OPEN hiện tại.

## 7. FR-CSH-08 - Lịch sử dòng tiền

```http
GET {{baseUrl}}/api/cashier-shifts/current/movements
Authorization: Bearer {{staffToken}}
```

Danh sách được sắp theo `createdAt` tăng dần và gồm:

- `movementType`: `CASH_IN` hoặc `CASH_OUT`.
- `sourceType`: `BOOKING_PAYMENT`, `ORDER_PAYMENT`, `REFUND`, `CASH_ADJUSTMENT`.
- Số tiền, payment tham chiếu, mô tả và nhân viên thực hiện.

## 8. FR-CSH-10/11/12/13 - Đóng ca

Gọi lại `/current` và lấy `expectedClosingCash`, sau đó đếm tiền thực tế:

```http
PATCH {{baseUrl}}/api/cashier-shifts/current/close
Authorization: Bearer {{staffToken}}
Content-Type: application/json
```

```json
{
  "actualClosingCash": 630000.00,
  "notes": "Thieu 10000 do tra lai tien le"
}
```

Hệ thống tính:

```text
cashDifference = actualClosingCash - expectedClosingCash
```

Response có `status = CLOSED`, `closedAt`, `expectedClosingCash`, `actualClosingCash` và `cashDifference`.

Nếu chênh lệch khác `0`, `notes` bắt buộc phải có nguyên nhân. Đóng ca dùng khóa database nên Payment CASH không thể chen ngang làm sai kết quả đối soát.

Sau khi đóng, `/current/status` trả:

```json
{
  "open": false,
  "shiftId": null,
  "venueId": null
}
```

## 9. FR-CSH-14 - Lịch sử ca cá nhân

```http
GET {{baseUrl}}/api/cashier-shifts/me?from=2026-09-01&to=2026-09-30
Authorization: Bearer {{staffToken}}
```

`from` và `to` không bắt buộc, định dạng `yyyy-MM-dd`, tính bao gồm toàn bộ ngày `to`. Nhân viên chỉ nhận các ca của chính mình.

## 10. FR-CSH-15 - Chủ sân xem tất cả ca

```http
GET {{baseUrl}}/api/cashier-shifts?staffId=10&venueId=1&status=CLOSED&from=2026-09-01&to=2026-09-30
Authorization: Bearer {{ownerToken}}
```

Tất cả query parameter đều không bắt buộc. Có thể lọc theo nhân viên, venue, `OPEN/CLOSED` và khoảng ngày mở ca.

## 11. FR-CSH-16 - Chi tiết đối soát

```http
GET {{baseUrl}}/api/cashier-shifts/{{shiftId}}
Authorization: Bearer {{ownerToken}}
```

Response phải có đầy đủ:

- Nhân viên, mã nhân viên và venue.
- Thời gian mở/đóng ca.
- `openingCash`, tổng `cashIn`, tổng `cashOut`.
- `expectedClosingCash`, `actualClosingCash`, `cashDifference`.
- Trạng thái và ghi chú mở/đóng ca.

## 12. Các trường hợp lỗi quan trọng

- STAFF không có `position = CASHIER`: từ chối mọi thao tác ca.
- Venue không tồn tại hoặc `INACTIVE`: không mở ca.
- Tiền đầu/cuối ca âm hoặc quá hai chữ số thập phân: lỗi validation.
- Mở ca thứ hai khi còn ca OPEN: từ chối.
- Xem dòng tiền hoặc đóng ca khi không có ca OPEN: từ chối.
- CASH_OUT lớn hơn tiền dự kiến trong két: từ chối.
- Đóng ca có chênh lệch nhưng không ghi chú: từ chối.
- `from` sau `to`: từ chối.
- STAFF gọi endpoint quản trị A3 hoặc OWNER gọi endpoint A2: `403 Forbidden`.

## 13. Thứ tự test đề xuất

1. Đăng nhập CASHIER và OWNER.
2. Kiểm tra `/current/status` đang `false`.
3. Mở ca với tiền đầu ca.
4. Thử mở ca lần hai và xác nhận bị chặn.
5. Tạo Payment CASH cho booking và order.
6. Refund một Payment CASH hoặc tạo điều chỉnh CASH_OUT.
7. Xem movements và đối chiếu `/current`.
8. Thử đóng ca có chênh lệch nhưng không ghi chú.
9. Đóng ca hợp lệ.
10. Xem lịch sử cá nhân.
11. Dùng OWNER lọc tất cả ca và xem chi tiết đối soát.

## 14. Checklist

- [ ] Mỗi CASHIER chỉ có một ca OPEN.
- [ ] Tiền đầu ca được lưu đúng.
- [ ] Payment CASH tạo CASH_IN đúng ca và venue.
- [ ] Điều chỉnh/refund tạo CASH_OUT.
- [ ] Movement hiển thị đúng nguồn và người thao tác.
- [ ] Tiền dự kiến bằng tiền đầu ca cộng thu trừ chi.
- [ ] Chênh lệch bằng tiền thực tế trừ tiền dự kiến.
- [ ] Ca chênh lệch bắt buộc có ghi chú.
- [ ] Đóng ca lưu thời gian và trạng thái CLOSED.
- [ ] Nhân viên chỉ xem lịch sử của mình; OWNER xem và lọc toàn bộ.
