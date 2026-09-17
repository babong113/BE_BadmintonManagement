# Hướng dẫn kiểm tra Review API bằng Postman

Tài liệu này hướng dẫn kiểm tra các chức năng `FR-REV-01` đến `FR-REV-07`.

## 1. Actor và quyền

| Actor | Role | Quyền |
| --- | --- | --- |
| A1 | `CUSTOMER` | Xem review, xem điểm trung bình và review booking detail của mình |
| A2 | `STAFF` | Xem review và điểm trung bình |
| A3 | `OWNER` | Xem và kiểm duyệt comment vi phạm |

Permission sử dụng:

- `review:read`: CUSTOMER, STAFF, OWNER.
- `review:create`: CUSTOMER.
- `review:manage`: OWNER.

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
| `courtId` | ID sân cần xem đánh giá |
| `bookingDetailId` | Detail đã hoàn tất của CUSTOMER |
| `reviewId` | Lấy sau khi tạo review |

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
| `GET` | `/api/reviews/courts/{courtId}` | A1, A2, A3 | Danh sách review của sân |
| `GET` | `/api/reviews/courts/{courtId}/summary` | A1, A2, A3 | Điểm trung bình và số review |
| `POST` | `/api/reviews/booking-details/{bookingDetailId}` | A1 | Tạo review |
| `PATCH` | `/api/reviews/{reviewId}/moderate` | A3 | Gỡ comment vi phạm, giữ rating/review |

## 4. Chuẩn bị booking để review

Booking detail dùng để test phải đáp ứng:

- Booking có `customer_id` trùng với CUSTOMER đang đăng nhập.
- Detail có trạng thái `COMPLETED`.
- Detail chưa có review.
- Detail liên kết với court cần đánh giá.

Có thể dùng luồng booking sau:

1. CUSTOMER tạo booking.
2. STAFF confirm booking.
3. STAFF complete booking bằng `PATCH /api/bookings/{id}/complete`.
4. Lấy `bookingDetailId` trong response booking.

Khách vãng lai không có tài khoản/customer ID nên không thể tạo review.

## 5. FR-REV-01 - Xem đánh giá sân

```http
GET {{baseUrl}}/api/reviews/courts/{{courtId}}
Authorization: Bearer {{customerToken}}
```

Kết quả mong đợi: `200 OK`. Review mới nhất đứng trước.

```json
{
  "success": true,
  "message": "Lay danh sach danh gia thanh cong",
  "data": [
    {
      "id": 1,
      "bookingDetailId": 10,
      "courtId": 3,
      "courtName": "Sân A",
      "customerId": 5,
      "customerName": "Nguyễn Văn A",
      "rating": 5,
      "comment": "Sân tốt và sạch sẽ",
      "createdAt": "2026-09-17T14:00:00+07:00"
    }
  ]
}
```

CUSTOMER, STAFF và OWNER đều có thể gọi endpoint này bằng token tương ứng.

## 6. FR-REV-02 đến FR-REV-05 - Tạo review

```http
POST {{baseUrl}}/api/reviews/booking-details/{{bookingDetailId}}
Authorization: Bearer {{customerToken}}
Content-Type: application/json
```

```json
{
  "rating": 5,
  "comment": "Sân tốt, ánh sáng đầy đủ"
}
```

Kết quả mong đợi: `201 Created`.

```json
{
  "success": true,
  "message": "Danh gia san thanh cong",
  "data": {
    "id": 1,
    "bookingDetailId": 10,
    "courtId": 3,
    "courtName": "Sân A",
    "customerId": 5,
    "customerName": "Nguyễn Văn A",
    "rating": 5,
    "comment": "Sân tốt, ánh sáng đầy đủ",
    "createdAt": "2026-09-17T14:00:00+07:00"
  }
}
```

Lưu review ID:

```javascript
const body = pm.response.json();
pm.environment.set("reviewId", body.data.id);
```

Quy tắc:

- Rating bắt buộc từ 1 đến 5.
- Comment không bắt buộc, tối đa 2000 ký tự.
- Comment trống hoặc chỉ chứa khoảng trắng được lưu thành `null`.
- Chỉ customer thuộc booking mới được review.
- Chỉ detail `COMPLETED` mới được review.
- Một booking detail chỉ có tối đa một review.
- Backend khóa booking detail khi tạo và database có unique constraint, tránh review trùng khi gửi đồng thời.

## 7. Kiểm tra quyền sở hữu

Dùng token của CUSTOMER khác và gửi lại request tạo review cho cùng detail:

```http
POST {{baseUrl}}/api/reviews/booking-details/{{bookingDetailId}}
Authorization: Bearer <TOKEN_CUSTOMER_KHAC>
```

Kết quả mong đợi: `400 Bad Request`.

```json
{
  "success": false,
  "message": "Ban khong co quyen danh gia booking detail nay",
  "data": 400
}
```

## 8. Kiểm tra trạng thái COMPLETED

Thử review detail còn `ACTIVE` hoặc `CANCELLED`.

Kết quả mong đợi:

```json
{
  "success": false,
  "message": "Chi co the danh gia booking detail da hoan tat",
  "data": 400
}
```

## 9. Kiểm tra review trùng

Gửi lại request review lần thứ hai cho cùng `bookingDetailId`.

Kết quả mong đợi:

```json
{
  "success": false,
  "message": "Booking detail da duoc danh gia",
  "data": 400
}
```

## 10. FR-REV-06 - Quản lý nội dung vi phạm

OWNER gỡ comment vi phạm:

```http
PATCH {{baseUrl}}/api/reviews/{{reviewId}}/moderate
Authorization: Bearer {{ownerToken}}
```

Kết quả mong đợi: `200 OK`:

- `comment` trở thành `null`.
- Rating và thông tin review vẫn được giữ.
- Bản ghi review không bị xóa, nên booking detail vẫn không thể review lần thứ hai.
- Điểm trung bình của sân không thay đổi.

STAFF hoặc CUSTOMER gọi endpoint này phải nhận `403 Forbidden`.

## 11. FR-REV-07 - Điểm trung bình sân

```http
GET {{baseUrl}}/api/reviews/courts/{{courtId}}/summary
Authorization: Bearer {{customerToken}}
```

Response mẫu:

```json
{
  "success": true,
  "message": "Lay diem danh gia trung binh thanh cong",
  "data": {
    "courtId": 3,
    "courtName": "Sân A",
    "averageRating": 4.33,
    "reviewCount": 3
  }
}
```

Quy tắc:

- Average được làm tròn hai chữ số thập phân.
- Sân chưa có review trả `averageRating = 0.00` và `reviewCount = 0`.
- Court không tồn tại trả `400 Bad Request`.

Ví dụ ba rating `5`, `4`, `4`:

```text
(5 + 4 + 4) / 3 = 4.33
```

## 12. Validation rating

Rating bằng 0:

```json
{
  "rating": 0,
  "comment": "Invalid"
}
```

Rating bằng 6:

```json
{
  "rating": 6,
  "comment": "Invalid"
}
```

Cả hai phải trả `400 Bad Request` với lỗi validation trường `rating`.

## 13. Trường hợp lỗi quan trọng

| Trường hợp | Kết quả |
| --- | --- |
| Không có Bearer token | `401/403` |
| Court không tồn tại | `400` |
| Booking detail không tồn tại | `400` |
| Customer không thuộc booking | `400` |
| Booking khách vãng lai | `400` |
| Detail chưa COMPLETED | `400` |
| Detail đã có review | `400` |
| Rating ngoài 1–5 | `400` validation |
| Comment trên 2000 ký tự | `400` validation |
| STAFF/OWNER tạo review | `403` do không có `review:create` |
| CUSTOMER/STAFF kiểm duyệt | `403` |

## 14. Thứ tự test đề xuất

1. Tạo và hoàn tất booking có ít nhất một detail.
2. CUSTOMER khác thử review và xác nhận bị từ chối.
3. CUSTOMER sở hữu booking tạo review hợp lệ.
4. Gửi review lần hai và xác nhận bị từ chối.
5. CUSTOMER/STAFF/OWNER xem danh sách review.
6. Kiểm tra summary và tự tính average.
7. OWNER moderate comment.
8. Kiểm tra comment đã null nhưng rating, reviewCount và average không đổi.
9. Thử review lại sau moderation và xác nhận vẫn bị từ chối.

## 15. Checklist

- [ ] Ba actor xem được danh sách review.
- [ ] Chỉ CUSTOMER có `review:create` tạo được review.
- [ ] Customer chỉ review booking của mình.
- [ ] Chỉ detail COMPLETED được review.
- [ ] Rating chỉ nhận 1–5.
- [ ] Một detail không thể review hai lần.
- [ ] Request đồng thời không tạo review trùng.
- [ ] OWNER gỡ được comment vi phạm.
- [ ] Moderation không cho phép review lại.
- [ ] Average và reviewCount chính xác.
- [ ] Sân chưa có review trả average 0.00.
