# Hướng dẫn kiểm tra Payment API bằng Postman

Tài liệu này hướng dẫn kiểm tra các chức năng `FR-PAY-01` đến `FR-PAY-18` cho payment của booking và order.

## 1. Phạm vi và phân quyền

| Actor | Role tương ứng | Phạm vi |
| --- | --- | --- |
| A1 | `CUSTOMER` | Xem và tạo thanh toán chuyển khoản/điện tử cho booking hoặc order của chính mình |
| A2 | `STAFF` | Xem, tạo mọi phương thức, cập nhật trạng thái, phân bổ, thu tiền tại ca và hoàn tiền |
| A3 | `OWNER` | Có toàn bộ permission, bao gồm xem lịch sử và thao tác quản trị payment |
| Hệ thống | Gateway/backend | Cập nhật mã giao dịch, trạng thái và thời điểm thanh toán thông qua luồng callback được bảo vệ bằng `payment:write` |

Permission được sử dụng:

- `payment:read`: xem summary/lịch sử và tạo payment không phải tiền mặt cho booking/order thuộc khách hàng hiện tại.
- `payment:write`: thao tác như nhân viên/owner trên mọi booking/order, ghi nhận CASH, cập nhật trạng thái, phân bổ và refund.

## 2. Chạy backend

Mở terminal tại:

```text
D:\BackEnd\DangNhap_API
```

Chạy:

```powershell
.\mvnw.cmd spring-boot:run
```

Base URL mặc định:

```text
http://localhost:7000
```

## 3. Tạo Postman environment

Tạo environment `DangNhap API Local` với các biến:

| Variable | Giá trị ban đầu |
| --- | --- |
| `baseUrl` | `http://localhost:7000` |
| `customerToken` | Để trống |
| `staffToken` | Để trống |
| `ownerToken` | Để trống |
| `bookingId` | ID booking cần test |
| `bookingDetailId` | ID detail thuộc booking |
| `orderId` | ID order cần test |
| `paymentId` | Để trống |

## 4. Chuẩn bị dữ liệu

Booking dùng để test cần:

- Thuộc tài khoản `CUSTOMER` sẽ dùng để test A1.
- Có trạng thái `PENDING`, `CONFIRMED` hoặc `COMPLETED`, không phải `CANCELLED`.
- Có `totalAmount > 0`.
- Có ít nhất một booking detail và biết `id`, `subtotal` của detail đó.
- Chưa được thanh toán hết.

Order dùng để test cần thuộc customer tương ứng hoặc là order tại quầy, có `totalAmount > 0`, chưa `CANCELLED` và chưa thanh toán hết.

Để test CASH, nhân viên phải có ca `OPEN` tại đúng venue của booking/order. Nếu module quản lý ca chưa có API tạo ca, chuẩn bị dữ liệu PostgreSQL:

```sql
INSERT INTO cashier_shifts (staff_id, venue_id, opening_cash, status)
VALUES (<staff_user_id>, <venue_id>, 500000.00, 'OPEN');
```

Mỗi nhân viên chỉ có tối đa một ca `OPEN`. `staff_id` phải đúng với tài khoản tạo payment CASH.

Đăng nhập từng actor:

```http
POST {{baseUrl}}/api/auth/login
Content-Type: application/json
```

```json
{
  "email": "customer@example.com",
  "password": "123456"
}
```

Lưu `data.accessToken` vào biến token tương ứng. Mọi request bên dưới cần header:

```http
Authorization: Bearer {{customerToken}}
Content-Type: application/json
```

Đổi sang `staffToken` hoặc `ownerToken` theo actor của test case.

## 5. Danh sách endpoint

| Method | Endpoint | Quyền chính | Chức năng |
| --- | --- | --- | --- |
| `GET` | `/api/payments/bookings/{bookingId}/summary` | A1, A2, A3 | Tổng tiền, đã trả, đang chờ, đã refund và còn lại |
| `GET` | `/api/payments/bookings/{bookingId}` | A1, A2, A3 | Lịch sử payment của booking |
| `POST` | `/api/payments/bookings/{bookingId}` | A1, A2 | Tạo CASH/BANK_TRANSFER/MOMO/VNPAY và thanh toán nhiều lần |
| `GET` | `/api/payments/orders/{orderId}/summary` | A1, A2, A3 | Xem tổng tiền và công nợ order |
| `GET` | `/api/payments/orders/{orderId}` | A1, A2, A3 | Xem lịch sử payment của order |
| `POST` | `/api/payments/orders/{orderId}` | A1, A2 | Tạo payment và thanh toán nhiều lần cho order |
| `PATCH` | `/api/payments/{paymentId}/status` | A2/Hệ thống | Chuyển `PENDING` sang `PAID` hoặc `FAILED` |
| `POST` | `/api/payments/{paymentId}/allocations` | A2 | Phân bổ thêm xuống booking detail |
| `PATCH` | `/api/payments/{paymentId}/refund` | A2 | Chuyển payment `PAID` sang `REFUNDED` |

## 6. FR-PAY-01 - Xem số tiền cần thanh toán

```http
GET {{baseUrl}}/api/payments/bookings/{{bookingId}}/summary
Authorization: Bearer {{customerToken}}
```

Kết quả mong đợi: `200 OK`.

```json
{
  "success": true,
  "message": "Lay tong quan thanh toan thanh cong",
  "data": {
    "bookingId": 1,
    "bookingCode": "BKG20260917-123456",
    "totalAmount": 300000.00,
    "paidAmount": 100000.00,
    "pendingAmount": 50000.00,
    "refundedAmount": 0.00,
    "remainingAmount": 200000.00
  }
}
```

Lưu ý:

- `remainingAmount = totalAmount - paidAmount` và không âm.
- Payment `PENDING` hiển thị riêng, chưa được tính vào `paidAmount`.
- Khách dùng booking của người khác phải nhận lỗi `400` với thông báo không có quyền truy cập.

Với order, gọi endpoint tương ứng:

```http
GET {{baseUrl}}/api/payments/orders/{{orderId}}/summary
Authorization: Bearer {{customerToken}}
```

Response dùng `orderId`, `orderCode`; hai trường `bookingId`, `bookingCode` bằng `null`.

## 7. FR-PAY-02 - Thanh toán tiền mặt

Chỉ `STAFF`/`OWNER` có `payment:write` được ghi nhận CASH.

```http
POST {{baseUrl}}/api/payments/bookings/{{bookingId}}
Authorization: Bearer {{staffToken}}
Content-Type: application/json
```

```json
{
  "amount": 100000.00,
  "paymentMethod": "CASH",
  "transactionCode": "CASH-COUNTER-001",
  "allocations": [
    {
      "bookingDetailId": 1,
      "amount": 100000.00
    }
  ]
}
```

Kết quả mong đợi: `201 Created`. CASH được tạo trực tiếp với:

- `paymentStatus = PAID`.
- `paidAt` được backend tự ghi nhận.
- `allocatedAmount = 100000.00`.
- `receivedById` và `receivedByName` là nhân viên đang thao tác.
- `cashierShiftId` là ca `OPEN` của nhân viên tại venue booking.
- Bảng `cash_movements` có một dòng `movement_type = CASH_IN`, `source_type = BOOKING_PAYMENT`.

Khách hàng gửi request CASH phải bị từ chối. Nhân viên không có ca `OPEN` đúng venue nhận lỗi `Nhan vien khong co ca thu ngan OPEN tai co so cua giao dich`.

Postman Tests để lưu payment ID:

```javascript
const body = pm.response.json();
pm.environment.set("paymentId", body.data.id);
```

## 8. FR-PAY-03 - Thanh toán chuyển khoản

Khách hàng hoặc nhân viên tạo payment:

```http
POST {{baseUrl}}/api/payments/bookings/{{bookingId}}
Authorization: Bearer {{customerToken}}
Content-Type: application/json
```

```json
{
  "amount": 50000.00,
  "paymentMethod": "BANK_TRANSFER",
  "transactionCode": null,
  "allocations": null
}
```

Kết quả mong đợi: `201 Created`, trạng thái ban đầu `PENDING`, `paidAt = null`.

Khách hàng chỉ được tạo cho booking của chính mình và không được tự gửi `allocations`.

## 9. FR-PAY-04, FR-PAY-07, FR-PAY-08 - MOMO/VNPAY và callback

Tạo yêu cầu thanh toán điện tử bằng `MOMO` hoặc `VNPAY`:

```json
{
  "amount": 75000.00,
  "paymentMethod": "VNPAY",
  "transactionCode": null,
  "allocations": null
}
```

Payment được tạo ở trạng thái `PENDING`. Sau khi gateway báo thành công, mô phỏng callback bằng tài khoản có `payment:write`:

```http
PATCH {{baseUrl}}/api/payments/{{paymentId}}/status
Authorization: Bearer {{staffToken}}
Content-Type: application/json
```

```json
{
  "status": "PAID",
  "transactionCode": "VNPAY-20260917-ABC123"
}
```

Kết quả mong đợi: `200 OK`:

- `paymentStatus = PAID`.
- `transactionCode` được lưu.
- `paidAt` được backend ghi nhận tại thời điểm xác nhận thành công.

Với phương thức khác CASH, xác nhận `PAID` mà không có transaction code phải bị từ chối nếu payment chưa lưu code trước đó.

Mô phỏng giao dịch thất bại:

```json
{
  "status": "FAILED",
  "transactionCode": "VNPAY-FAILED-001"
}
```

Payment `FAILED` không chiếm công nợ và không thể được phân bổ thêm.

## 10. FR-PAY-05 - Thanh toán nhiều lần

1. Gọi summary để lấy `remainingAmount`.
2. Tạo payment lần một nhỏ hơn công nợ.
3. Xác nhận payment lần một thành `PAID` nếu không phải CASH.
4. Tạo payment lần hai cho phần còn lại.
5. Gọi lại summary và kiểm tra `remainingAmount = 0.00`.

Quy trình này áp dụng cho cả booking và order. Tổng các payment `PENDING + PAID` của từng đối tượng không được vượt `totalAmount`. Vì vậy một payment đang chờ cũng giữ phần công nợ tương ứng, tránh tạo nhiều giao dịch song song vượt tổng tiền.

## 11. FR-PAY-06 - Cập nhật trạng thái

Luồng trạng thái hợp lệ:

```text
PENDING -> PAID
PENDING -> FAILED
PAID -> REFUNDED (qua endpoint refund)
```

Không cho phép:

- Sửa lại payment đã `FAILED`.
- Sửa trực tiếp payment đã `PAID` bằng endpoint status.
- Gửi `REFUNDED` qua endpoint status.
- Refund payment chưa `PAID`.

## 12. FR-PAY-09 - Xem lịch sử thanh toán

```http
GET {{baseUrl}}/api/payments/bookings/{{bookingId}}
Authorization: Bearer {{customerToken}}
```

Kết quả mong đợi: `200 OK`. Danh sách sắp xếp mới nhất trước, mỗi phần tử có:

- Thông tin booking.
- Số tiền, phương thức và trạng thái.
- Mã giao dịch, `paidAt`, `createdAt`.
- Tổng đã phân bổ, chưa phân bổ và danh sách allocation.

`STAFF` và `OWNER` có thể xem mọi booking. `CUSTOMER` chỉ xem booking của mình.

## 13. FR-PAY-10 và FR-PAY-11 - Phân bổ và tách hóa đơn

### Phân bổ ngay khi tạo payment

Nhân viên gửi `allocations` trong request tạo payment. Tổng allocation có thể nhỏ hơn hoặc bằng amount của payment.

### Phân bổ bổ sung

```http
POST {{baseUrl}}/api/payments/{{paymentId}}/allocations
Authorization: Bearer {{staffToken}}
Content-Type: application/json
```

```json
{
  "allocations": [
    {
      "bookingDetailId": 1,
      "amount": 50000.00
    },
    {
      "bookingDetailId": 2,
      "amount": 100000.00
    }
  ]
}
```

Kết quả mong đợi: `201 Created`.

Để tách hóa đơn, tạo một payment có amount bằng phần cần thanh toán của một detail và chỉ phân bổ xuống detail đó:

```json
{
  "amount": 150000.00,
  "paymentMethod": "CASH",
  "allocations": [
    {
      "bookingDetailId": 2,
      "amount": 150000.00
    }
  ]
}
```

## 14. FR-PAY-12 - Hoàn tiền

```http
PATCH {{baseUrl}}/api/payments/{{paymentId}}/refund
Authorization: Bearer {{staffToken}}
```

Kết quả mong đợi: `200 OK`, `paymentStatus = REFUNDED`.

Sau refund:

- Payment không còn được tính vào `paidAmount`.
- Số tiền trở lại `remainingAmount`.
- Allocation của payment refund không còn chiếm công nợ của detail.
- Chỉ payment đang `PAID` mới được refund.
- Nếu là CASH, nhân viên hoàn tiền phải có ca `OPEN` đúng venue; hệ thống tạo `CASH_OUT` với `source_type = REFUND` trong `cash_movements`.

## 15. FR-PAY-13 - Kiểm tra giới hạn số tiền

Kiểm tra các trường hợp sau đều phải nhận `400 Bad Request`:

1. `amount <= 0`.
2. Amount hoặc allocation có quá 2 chữ số thập phân.
3. Tổng payment `PENDING + PAID` vượt công nợ booking.
4. Tổng allocation của một payment vượt amount payment.
5. Tổng allocation đang hoạt động của một detail vượt `subtotal` detail.
6. Allocation trỏ đến detail của booking khác.
7. Một detail xuất hiện hai lần trong cùng request.
8. Tạo payment cho booking `CANCELLED`.
9. Tạo payment cho order `CANCELLED`.
10. Tổng payment `PENDING + PAID` vượt công nợ order.
11. Gửi `allocations` cho payment của order.

Ví dụ request vượt công nợ:

```json
{
  "amount": 999999999.00,
  "paymentMethod": "CASH",
  "allocations": null
}
```

Response mẫu:

```json
{
  "success": false,
  "message": "So tien thanh toan vuot qua cong no con lai",
  "data": 400
}
```

## 16. FR-PAY-14 - Thanh toán đơn hàng

### Xem công nợ order

```http
GET {{baseUrl}}/api/payments/orders/{{orderId}}/summary
Authorization: Bearer {{customerToken}}
```

### Tạo payment BANK_TRANSFER cho order

```http
POST {{baseUrl}}/api/payments/orders/{{orderId}}
Authorization: Bearer {{customerToken}}
Content-Type: application/json
```

```json
{
  "amount": 50000.00,
  "paymentMethod": "BANK_TRANSFER",
  "transactionCode": null,
  "allocations": null
}
```

Kết quả `201 Created`, response có `orderId`, `orderCode`, `bookingId = null`, trạng thái `PENDING`.

### Thanh toán CASH cho order tại quầy

```http
POST {{baseUrl}}/api/payments/orders/{{orderId}}
Authorization: Bearer {{staffToken}}
Content-Type: application/json
```

```json
{
  "amount": 100000.00,
  "paymentMethod": "CASH",
  "transactionCode": "ORDER-CASH-001",
  "allocations": null
}
```

Kết quả `201 Created`, trạng thái `PAID`, có `receivedById`, `cashierShiftId`; bảng `cash_movements` có `CASH_IN/ORDER_PAYMENT`.

Order không hỗ trợ `allocations` vì allocation hiện chỉ áp dụng cho `booking_details`.

### Xem lịch sử payment order

```http
GET {{baseUrl}}/api/payments/orders/{{orderId}}
Authorization: Bearer {{customerToken}}
```

Customer chỉ truy cập order của mình; `STAFF/OWNER` có `payment:write` truy cập mọi order.

## 17. FR-PAY-15 đến FR-PAY-18 - Nhân viên, ca và dòng tiền

Kiểm tra response payment CASH:

```json
{
  "receivedById": 20,
  "receivedByName": "Nhan Vien A",
  "cashierShiftId": 5,
  "paymentStatus": "PAID"
}
```

Đối chiếu database sau khi thu tiền:

```sql
SELECT movement_type, source_type, amount, reference_id, created_by
FROM cash_movements
WHERE cashier_shift_id = 5
ORDER BY created_at DESC;
```

- Thu tiền booking: `CASH_IN / BOOKING_PAYMENT`.
- Thu tiền order: `CASH_IN / ORDER_PAYMENT`.
- Hoàn payment CASH: `CASH_OUT / REFUND`.
- `reference_id` là ID payment và `created_by` là nhân viên thực hiện.
- Nếu ghi payment hoặc dòng tiền thất bại, transaction rollback toàn bộ, không để dữ liệu thu tiền dở dang.

## 18. Thứ tự test đề xuất

1. Tạo booking có hai booking detail.
2. Dùng CUSTOMER gọi summary và history.
3. Dùng CUSTOMER tạo BANK_TRANSFER nhỏ hơn tổng tiền.
4. Dùng STAFF xác nhận payment thành `PAID` kèm transaction code.
5. Dùng STAFF tạo CASH và phân bổ cho detail thứ nhất.
6. Dùng STAFF tạo payment riêng cho detail thứ hai để kiểm tra tách hóa đơn.
7. Gọi history và summary, đối chiếu các tổng tiền.
8. Thử các request vượt giới hạn của FR-PAY-13.
9. Refund một payment PAID và kiểm tra summary lần cuối.
10. Tạo order, thanh toán hai lần và kiểm tra summary/history order.
11. Dùng STAFF có ca OPEN tạo CASH cho booking và order, đối chiếu `received_by`, `cashier_shift_id`, `CASH_IN`.
12. Refund payment CASH, đối chiếu `CASH_OUT`.

## 19. Checklist kết quả

- [ ] CUSTOMER chỉ xem/thanh toán booking của mình.
- [ ] CUSTOMER không thể ghi nhận CASH hoặc tự phân bổ.
- [ ] STAFF/OWNER ghi nhận CASH thành `PAID` ngay lập tức.
- [ ] BANK_TRANSFER/MOMO/VNPAY được tạo ở trạng thái `PENDING`.
- [ ] Xác nhận `PAID` lưu transaction code và `paidAt`.
- [ ] Một booking có thể có nhiều payment.
- [ ] Một order có thể có nhiều payment và không vượt công nợ order.
- [ ] Customer chỉ xem/thanh toán order của mình.
- [ ] CASH lưu đúng nhân viên và ca thu ngân đang `OPEN` tại venue.
- [ ] CASH booking/order tạo đúng `CASH_IN`.
- [ ] Refund CASH tạo đúng `CASH_OUT`.
- [ ] Lịch sử trả đúng payment và allocation.
- [ ] Phân bổ/tách hóa đơn không vượt payment hoặc subtotal detail.
- [ ] Payment vượt công nợ bị từ chối.
- [ ] Refund chỉ áp dụng cho payment `PAID` và cập nhật lại summary.
