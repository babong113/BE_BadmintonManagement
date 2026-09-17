# Hướng dẫn kiểm tra Order API bằng Postman

Tài liệu này hướng dẫn kiểm tra các chức năng `FR-ORD-01` đến `FR-ORD-16`.

## 1. Actor và quyền

| Actor | Role | Phạm vi |
| --- | --- | --- |
| A1 | `CUSTOMER` | Tạo, chỉnh sửa, hủy và xem order của chính mình |
| A2 | `STAFF` | Tạo order tại quầy, quản lý item, xác nhận và hoàn tất order |
| A3 | `OWNER` | Xem/quản lý danh sách order và có toàn bộ quyền của hệ thống |
| Hệ thống | Backend | Sinh mã, snapshot giá, tính subtotal/tổng và trừ tồn khi hoàn tất |

## 2. Chạy backend và environment

```powershell
cd D:\BackEnd\DangNhap_API
.\mvnw.cmd spring-boot:run
```

Base URL:

```text
http://localhost:7000
```

Tạo Postman environment:

| Variable | Giá trị |
| --- | --- |
| `baseUrl` | `http://localhost:7000` |
| `customerToken` | Token CUSTOMER |
| `staffToken` | Token STAFF |
| `ownerToken` | Token OWNER |
| `venueId` | ID venue đang `ACTIVE` |
| `customerId` | ID khách hàng |
| `productId` | ID product có `isForSale = true` |
| `orderId` | Lấy sau khi tạo order |
| `orderItemId` | Lấy từ response order |

Đăng nhập:

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

## 3. Danh sách endpoint

| Method | Endpoint | Actor | Chức năng |
| --- | --- | --- | --- |
| `POST` | `/api/orders` | A1, A2 | Tạo order |
| `GET` | `/api/orders/me` | A1 | Lịch sử order cá nhân |
| `GET` | `/api/orders/{id}` | A1, A2, A3 | Chi tiết order có kiểm tra quyền |
| `GET` | `/api/orders` | A2, A3 | Danh sách và lọc quản trị |
| `POST` | `/api/orders/{orderId}/items` | A1, A2 | Thêm sản phẩm |
| `PATCH` | `/api/orders/{orderId}/items/{itemId}` | A1, A2 | Cập nhật quantity |
| `DELETE` | `/api/orders/{orderId}/items/{itemId}` | A1, A2 | Xóa item |
| `PATCH` | `/api/orders/{id}/confirm` | A2 | Xác nhận order |
| `PATCH` | `/api/orders/{id}/complete` | A2 | Hoàn tất và trừ tồn |
| `PATCH` | `/api/orders/{id}/cancel` | A1, A2 | Hủy order chưa hoàn tất |

## 4. Chuẩn bị sản phẩm

Sản phẩm dùng để test phải có:

- `isForSale = true`.
- `salePrice` khác null.
- `status = AVAILABLE`.
- `quantity` đủ lớn.

Order bắt buộc thuộc một venue `ACTIVE` vì migration V2 quy định `orders.venue_id NOT NULL`.

## 5. FR-ORD-01, FR-ORD-03, FR-ORD-05 - Khách hàng tự tạo order

CUSTOMER không được truyền `customerId` hoặc thông tin khách vãng lai. Backend tự lấy user từ access token.

```http
POST {{baseUrl}}/api/orders
Authorization: Bearer {{customerToken}}
Content-Type: application/json
```

```json
{
  "venueId": 1,
  "customerId": null,
  "guestName": null,
  "guestPhone": null,
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ]
}
```

Kết quả mong đợi: `201 Created`.

```json
{
  "success": true,
  "message": "Tao order thanh cong",
  "data": {
    "id": 10,
    "orderCode": "ORD20260917-123456",
    "venueId": 1,
    "customerId": 5,
    "guestName": null,
    "guestPhone": null,
    "totalAmount": 700000.00,
    "status": "PENDING",
    "items": [
      {
        "id": 20,
        "productId": 1,
        "quantity": 2,
        "unitPrice": 350000.00,
        "subtotal": 700000.00
      }
    ]
  }
}
```

Lưu biến:

```javascript
const body = pm.response.json();
pm.environment.set("orderId", body.data.id);
pm.environment.set("orderItemId", body.data.items[0].id);
```

Backend sinh `orderCode` duy nhất theo dạng `ORDyyyyMMdd-xxxxxx`.

## 6. FR-ORD-02 và FR-ORD-03 - Nhân viên tạo cho customer

```http
POST {{baseUrl}}/api/orders
Authorization: Bearer {{staffToken}}
Content-Type: application/json
```

```json
{
  "venueId": 1,
  "customerId": 5,
  "guestName": null,
  "guestPhone": null,
  "items": [
    {
      "productId": 1,
      "quantity": 1
    }
  ]
}
```

`customerId` phải thuộc user có role `CUSTOMER`.

## 7. FR-ORD-04 - Order cho khách vãng lai

Chỉ STAFF/OWNER được dùng luồng này:

```json
{
  "venueId": 1,
  "customerId": null,
  "guestName": "Nguyễn Văn Khách",
  "guestPhone": "0901234567",
  "items": []
}
```

Phải nhập cả `guestName` và `guestPhone`. Không được đồng thời truyền `customerId` và thông tin guest.

Order có thể được tạo rỗng để nhân viên thêm item sau, nhưng không thể confirm khi chưa có sản phẩm.

## 8. FR-ORD-05 - Thêm sản phẩm vào order

Chỉ thực hiện khi order còn `PENDING`.

```http
POST {{baseUrl}}/api/orders/{{orderId}}/items
Authorization: Bearer {{customerToken}}
Content-Type: application/json
```

```json
{
  "productId": 2,
  "quantity": 3
}
```

Kết quả mong đợi: `201 Created`. Một product chỉ được xuất hiện một lần trong cùng order.

## 9. FR-ORD-06 - Cập nhật số lượng

```http
PATCH {{baseUrl}}/api/orders/{{orderId}}/items/{{orderItemId}}
Authorization: Bearer {{customerToken}}
Content-Type: application/json
```

```json
{
  "quantity": 4
}
```

Kết quả mong đợi: `200 OK`. Backend tính lại:

```text
item.subtotal = item.quantity × item.unitPrice
order.totalAmount = tổng subtotal của mọi item
```

`unitPrice` là giá bán được snapshot khi item được thêm, không tự thay đổi nếu giá product thay đổi sau đó.

## 10. FR-ORD-07 - Xóa sản phẩm

```http
DELETE {{baseUrl}}/api/orders/{{orderId}}/items/{{orderItemId}}
Authorization: Bearer {{customerToken}}
```

Kết quả mong đợi: `200 OK`; item bị xóa và `totalAmount` được tính lại. Chỉ order `PENDING` mới được sửa item.

## 11. FR-ORD-08 và FR-ORD-09 - Kiểm tra số tiền

Với dữ liệu:

| Product | Quantity | Unit price | Subtotal |
| --- | ---: | ---: | ---: |
| Ống cầu | 2 | 350,000 | 700,000 |
| Quấn cán | 3 | 50,000 | 150,000 |

Kết quả phải là:

```text
totalAmount = 850000.00
```

Client không gửi `unitPrice`, `subtotal` hoặc `totalAmount`; toàn bộ được backend tính.

## 12. FR-ORD-11 - Xác nhận order

```http
PATCH {{baseUrl}}/api/orders/{{orderId}}/confirm
Authorization: Bearer {{staffToken}}
```

Kết quả mong đợi: `200 OK`, status chuyển từ `PENDING` sang `CONFIRMED`.

Trước khi confirm, backend kiểm tra:

- Order có ít nhất một item.
- Mỗi product vẫn hỗ trợ bán.
- Product đang `AVAILABLE`.
- Quantity không vượt tồn kho hiện tại.

Sau khi confirm, không thể thêm, sửa hoặc xóa item.

## 13. FR-ORD-12 và FR-ORD-14 - Hoàn tất và trừ tồn kho

```http
PATCH {{baseUrl}}/api/orders/{{orderId}}/complete
Authorization: Bearer {{staffToken}}
```

Kết quả mong đợi: `200 OK`, status thành `COMPLETED`.

Backend thực hiện trong một transaction:

1. Chỉ nhận order `CONFIRMED`.
2. Khóa order.
3. Khóa các product theo thứ tự ID để hạn chế deadlock.
4. Kiểm tra lại trạng thái và quantity của từng product.
5. Trừ tồn kho.
6. Nếu quantity về 0, đặt product thành `OUT_OF_STOCK`.
7. Chuyển order thành `COMPLETED`.

Nếu bất kỳ product nào thiếu tồn, toàn bộ transaction rollback: không product nào bị trừ và order vẫn `CONFIRMED`.

## 14. FR-ORD-13 - Hủy order

CUSTOMER hủy order của mình:

```http
PATCH {{baseUrl}}/api/orders/{{orderId}}/cancel
Authorization: Bearer {{customerToken}}
```

STAFF có thể hủy mọi order hợp lệ bằng `staffToken`.

Cho phép hủy `PENDING` hoặc `CONFIRMED`. Không cho phép hủy `COMPLETED` hoặc hủy lại order đã `CANCELLED`.

Do tồn kho chỉ bị trừ khi complete nên việc hủy order chưa hoàn tất không cần hoàn tồn.

## 15. FR-ORD-15 - Lịch sử mua hàng

```http
GET {{baseUrl}}/api/orders/me
Authorization: Bearer {{customerToken}}
```

Kết quả mong đợi: `200 OK`, trả các order gắn với customer hiện tại, mới nhất trước, kèm đầy đủ item.

Chi tiết một order:

```http
GET {{baseUrl}}/api/orders/{{orderId}}
Authorization: Bearer {{customerToken}}
```

CUSTOMER không thể xem order của người khác hoặc order khách vãng lai.

## 16. FR-ORD-16 - Quản lý và lọc order

Danh sách tất cả:

```http
GET {{baseUrl}}/api/orders
Authorization: Bearer {{staffToken}}
```

Lọc theo trạng thái:

```http
GET {{baseUrl}}/api/orders?status=PENDING
```

Lọc theo venue:

```http
GET {{baseUrl}}/api/orders?venueId=1
```

Kết hợp:

```http
GET {{baseUrl}}/api/orders?status=COMPLETED&venueId=1
```

Các status hợp lệ: `PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`.

## 17. Trường hợp lỗi quan trọng

| Trường hợp | Kết quả |
| --- | --- |
| Không có token | `401/403` |
| CUSTOMER truyền customerId/guest | `400` |
| STAFF không truyền customerId hoặc đủ guest info | `400` |
| Venue không tồn tại hoặc INACTIVE | `400` |
| Product không hỗ trợ bán | `400` |
| Product INACTIVE/OUT_OF_STOCK | `400` |
| Quantity bằng 0 hoặc âm | `400` validation |
| Quantity vượt tồn | `400` |
| Product trùng trong order | `400` |
| Sửa item sau confirm | `400` |
| Confirm order rỗng | `400` |
| Complete order không phải CONFIRMED | `400` |
| Hủy order COMPLETED | `400` |
| CUSTOMER truy cập order người khác | `400` |

## 18. Thứ tự test đề xuất

1. OWNER tạo product để bán và đặt tồn kho đủ lớn.
2. CUSTOMER tạo order có item.
3. CUSTOMER thêm, sửa và xóa item.
4. Kiểm tra subtotal và totalAmount.
5. STAFF tạo order cho customer có tài khoản.
6. STAFF tạo order khách vãng lai.
7. STAFF confirm order.
8. Thử sửa item sau confirm và kiểm tra bị từ chối.
9. STAFF complete order và kiểm tra tồn kho giảm.
10. Tạo order khác vượt tồn, confirm/complete và kiểm tra rollback.
11. CUSTOMER xem lịch sử.
12. STAFF/OWNER lọc danh sách theo status và venue.

## 19. Checklist

- [ ] CUSTOMER tạo order gắn đúng tài khoản hiện tại.
- [ ] STAFF tạo được order cho customer và guest.
- [ ] Mã order được sinh tự động và duy nhất.
- [ ] Thêm/sửa/xóa item chỉ hoạt động khi PENDING.
- [ ] Unit price được snapshot từ product.
- [ ] Subtotal và totalAmount được tính đúng.
- [ ] Confirm kiểm tra order và tồn kho.
- [ ] Complete trừ tồn đúng một lần.
- [ ] Hết tồn tự chuyển OUT_OF_STOCK.
- [ ] Thiếu tồn khiến toàn bộ complete rollback.
- [ ] CUSTOMER chỉ xem/hủy order của mình.
- [ ] STAFF/OWNER xem và lọc được danh sách order.
