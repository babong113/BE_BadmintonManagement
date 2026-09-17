# Hướng dẫn kiểm tra Report API bằng Postman

Tài liệu này hướng dẫn kiểm tra các chức năng `FR-RPT-01` đến `FR-RPT-14`.

## 1. Quyền truy cập

Toàn bộ endpoint report yêu cầu:

```text
report:read
```

Theo migration V1, chỉ `OWNER` (A3) có permission này. CUSTOMER và STAFF phải nhận `403 Forbidden`.

## 2. Chạy backend và environment

```powershell
cd D:\BackEnd\DangNhap_API
.\mvnw.cmd spring-boot:run
```

Base URL:

```text
http://localhost:7000
```

Tạo environment:

| Variable | Giá trị |
| --- | --- |
| `baseUrl` | `http://localhost:7000` |
| `ownerToken` | Access token OWNER |
| `fromDate` | `2026-09-01` |
| `toDate` | `2026-09-30` |

Đăng nhập OWNER:

```http
POST {{baseUrl}}/api/auth/login
Content-Type: application/json
```

```json
{
  "email": "owner@example.com",
  "password": "123456"
}
```

Mọi request report cần:

```http
Authorization: Bearer {{ownerToken}}
```

## 3. Quy tắc khoảng thời gian

Tất cả endpoint nhận hai query parameter không bắt buộc:

```text
fromDate=YYYY-MM-DD
toDate=YYYY-MM-DD
```

- Hai đầu mốc đều được tính trong báo cáo.
- Nếu không truyền `toDate`, backend dùng ngày hiện tại.
- Nếu không truyền `fromDate`, backend dùng ngày đầu tháng của `toDate`.
- Nếu `fromDate > toDate`, trả `400 Bad Request`.

Ví dụ:

```text
?fromDate=2026-09-01&toDate=2026-09-30
```

## 4. Định nghĩa dữ liệu báo cáo

- Doanh thu chỉ tính payment có `payment_status = PAID` và `paid_at` nằm trong khoảng ngày.
- `PENDING`, `FAILED` và `REFUNDED` không được tính là doanh thu.
- Doanh thu booking/order tổng thể lấy trực tiếp từ `payments.amount`.
- Doanh thu booking theo court/venue lấy từ `payment_allocations`.
- Doanh thu order theo venue lấy từ payment gắn với order và `orders.venue_id`.
- Payment booking chưa phân bổ vẫn xuất hiện trong tổng doanh thu nhưng không thể xuất hiện trong breakdown court/venue.
- Thống kê booking dùng `bookings.booking_date`.
- Thống kê review dùng `reviews.created_at`.
- Schema order chưa có `completed_at`, nên thống kê order/product bán dùng `orders.created_at` và chỉ lấy order đang `COMPLETED` khi tính hàng đã bán.
- Sản phẩm thuê chỉ tính booking detail `COMPLETED`.

## 5. Danh sách endpoint

| Endpoint | FR |
| --- | --- |
| `GET /api/reports/dashboard` | FR-RPT-01 |
| `GET /api/reports/revenue/summary` | FR-RPT-02 |
| `GET /api/reports/revenue/daily` | FR-RPT-03 |
| `GET /api/reports/revenue/monthly` | FR-RPT-04 |
| `GET /api/reports/revenue/venues` | FR-RPT-05 |
| `GET /api/reports/revenue/courts` | FR-RPT-06 |
| `GET /api/reports/bookings/summary` | FR-RPT-07 |
| `GET /api/reports/bookings/cancellation-rate` | FR-RPT-08 |
| `GET /api/reports/bookings/peak-hours` | FR-RPT-09 |
| `GET /api/reports/bookings/popular-courts` | FR-RPT-10 |
| `GET /api/reports/customers` | FR-RPT-11 |
| `GET /api/reports/products` | FR-RPT-12 |
| `GET /api/reports/orders` | FR-RPT-13 |
| `GET /api/reports/reviews` | FR-RPT-14 |

## 6. FR-RPT-01 - Dashboard tổng quan

```http
GET {{baseUrl}}/api/reports/dashboard?fromDate={{fromDate}}&toDate={{toDate}}
Authorization: Bearer {{ownerToken}}
```

Response mẫu:

```json
{
  "success": true,
  "message": "Lay dashboard thanh cong",
  "data": {
    "fromDate": "2026-09-01",
    "toDate": "2026-09-30",
    "totalRevenue": 15000000.00,
    "bookingRevenue": 12000000.00,
    "orderRevenue": 3000000.00,
    "totalBookings": 100,
    "completedBookings": 70,
    "cancelledBookings": 10,
    "cancellationRate": 10.00,
    "totalOrders": 30,
    "completedOrders": 24,
    "totalCustomers": 50,
    "activeVenues": 3,
    "activeCourts": 12,
    "averageRating": 4.35
  }
}
```

`cancellationRate` là phần trăm từ 0 đến 100.

## 7. FR-RPT-02 - Tổng doanh thu

```http
GET {{baseUrl}}/api/reports/revenue/summary?fromDate={{fromDate}}&toDate={{toDate}}
Authorization: Bearer {{ownerToken}}
```

```json
{
  "success": true,
  "message": "Lay tong doanh thu thanh cong",
  "data": {
    "fromDate": "2026-09-01",
    "toDate": "2026-09-30",
    "totalRevenue": 15000000.00,
    "bookingRevenue": 12000000.00,
    "orderRevenue": 3000000.00,
    "transactionCount": 85
  }
}
```

Kiểm tra:

```text
totalRevenue = bookingRevenue + orderRevenue
```

## 8. FR-RPT-03 - Doanh thu theo ngày

```http
GET {{baseUrl}}/api/reports/revenue/daily?fromDate={{fromDate}}&toDate={{toDate}}
Authorization: Bearer {{ownerToken}}
```

```json
{
  "success": true,
  "message": "Lay doanh thu theo ngay thanh cong",
  "data": [
    {
      "period": "2026-09-01",
      "revenue": 1500000.00,
      "transactionCount": 8
    }
  ]
}
```

Chỉ những ngày có payment PAID được trả về; danh sách tăng dần theo ngày.

## 9. FR-RPT-04 - Doanh thu theo tháng

```http
GET {{baseUrl}}/api/reports/revenue/monthly?fromDate=2026-01-01&toDate=2026-12-31
Authorization: Bearer {{ownerToken}}
```

```json
{
  "data": [
    {
      "period": "2026-09",
      "revenue": 15000000.00,
      "transactionCount": 85
    }
  ]
}
```

`period` có định dạng `YYYY-MM`.

## 10. FR-RPT-05 - Doanh thu theo cơ sở

```http
GET {{baseUrl}}/api/reports/revenue/venues?fromDate={{fromDate}}&toDate={{toDate}}
Authorization: Bearer {{ownerToken}}
```

```json
{
  "data": [
    {
      "venueId": 1,
      "venueName": "Sài Gòn Central Badminton",
      "revenue": 9000000.00
    }
  ]
}
```

Revenue venue gồm:

- Booking allocations thuộc các court của venue.
- Payment PAID của order thuộc venue.

Danh sách giảm dần theo doanh thu.

## 11. FR-RPT-06 - Doanh thu theo sân

```http
GET {{baseUrl}}/api/reports/revenue/courts?fromDate={{fromDate}}&toDate={{toDate}}
Authorization: Bearer {{ownerToken}}
```

```json
{
  "data": [
    {
      "courtId": 3,
      "courtName": "Sân A",
      "venueId": 1,
      "venueName": "Sài Gòn Central Badminton",
      "revenue": 4500000.00
    }
  ]
}
```

Chỉ payment allocation có thể xác định doanh thu theo court.

## 12. FR-RPT-07 - Thống kê booking

```http
GET {{baseUrl}}/api/reports/bookings/summary?fromDate={{fromDate}}&toDate={{toDate}}
Authorization: Bearer {{ownerToken}}
```

```json
{
  "data": {
    "fromDate": "2026-09-01",
    "toDate": "2026-09-30",
    "totalBookings": 100,
    "byStatus": [
      { "status": "CANCELLED", "total": 10 },
      { "status": "COMPLETED", "total": 70 },
      { "status": "CONFIRMED", "total": 15 },
      { "status": "PENDING", "total": 5 }
    ],
    "daily": [
      { "period": "2026-09-01", "total": 4 }
    ],
    "monthly": [
      { "period": "2026-09", "total": 100 }
    ]
  }
}
```

Kiểm tra tổng `byStatus` bằng `totalBookings`.

## 13. FR-RPT-08 - Tỷ lệ hủy

```http
GET {{baseUrl}}/api/reports/bookings/cancellation-rate?fromDate={{fromDate}}&toDate={{toDate}}
Authorization: Bearer {{ownerToken}}
```

```json
{
  "data": {
    "totalBookings": 100,
    "cancelledBookings": 10,
    "cancellationRate": 10.00
  }
}
```

Công thức:

```text
cancellationRate = cancelledBookings × 100 / totalBookings
```

Không có booking thì tỷ lệ trả `0.00`.

## 14. FR-RPT-09 - Khung giờ cao điểm

```http
GET {{baseUrl}}/api/reports/bookings/peak-hours?fromDate={{fromDate}}&toDate={{toDate}}
Authorization: Bearer {{ownerToken}}
```

```json
{
  "data": [
    {
      "hour": 18,
      "timeRange": "18:00-19:00",
      "bookingCount": 25
    }
  ]
}
```

- Mỗi detail được xếp theo giờ bắt đầu.
- Booking/detail `CANCELLED` bị loại.
- Kết quả giảm dần theo số lượt booking.

## 15. FR-RPT-10 - Sân phổ biến

```http
GET {{baseUrl}}/api/reports/bookings/popular-courts?fromDate={{fromDate}}&toDate={{toDate}}
Authorization: Bearer {{ownerToken}}
```

```json
{
  "data": [
    {
      "courtId": 3,
      "courtName": "Sân A",
      "venueId": 1,
      "venueName": "Sài Gòn Central Badminton",
      "bookingCount": 40
    }
  ]
}
```

Booking/detail đã hủy không được tính.

## 16. FR-RPT-11 - Thống kê khách hàng

```http
GET {{baseUrl}}/api/reports/customers?fromDate={{fromDate}}&toDate={{toDate}}
Authorization: Bearer {{ownerToken}}
```

```json
{
  "data": {
    "fromDate": "2026-09-01",
    "toDate": "2026-09-30",
    "totalCustomers": 2,
    "averageBookingsPerCustomer": 2.50,
    "customers": [
      {
        "customerId": 5,
        "customerName": "Nguyễn Văn A",
        "email": "customer@example.com",
        "bookingCount": 3,
        "completedBookingCount": 2,
        "lastBookingDate": "2026-09-20"
      }
    ]
  }
}
```

Danh sách chứa cả customer chưa booking trong khoảng thời gian, với count bằng 0.

## 17. FR-RPT-12 - Thống kê sản phẩm

```http
GET {{baseUrl}}/api/reports/products?fromDate={{fromDate}}&toDate={{toDate}}
Authorization: Bearer {{ownerToken}}
```

```json
{
  "data": [
    {
      "productId": 1,
      "productName": "Vợt cầu lông",
      "soldQuantity": 20,
      "salesValue": 10000000.00,
      "rentedQuantity": 12,
      "rentalValue": 600000.00,
      "totalQuantity": 32
    }
  ]
}
```

- Bán: item của order `COMPLETED`.
- Thuê: booking equipment thuộc detail `COMPLETED`.
- Product không phát sinh bán/thuê trong khoảng thời gian bị loại.
- Sắp xếp theo `totalQuantity` giảm dần.

## 18. FR-RPT-13 - Thống kê order

```http
GET {{baseUrl}}/api/reports/orders?fromDate={{fromDate}}&toDate={{toDate}}
Authorization: Bearer {{ownerToken}}
```

```json
{
  "data": {
    "fromDate": "2026-09-01",
    "toDate": "2026-09-30",
    "totalOrders": 30,
    "totalValue": 5000000.00,
    "completedValue": 4000000.00,
    "byStatus": [
      { "status": "COMPLETED", "total": 24 },
      { "status": "CANCELLED", "total": 2 }
    ]
  }
}
```

- `totalValue`: tổng giá trị tất cả order được tạo trong kỳ.
- `completedValue`: chỉ giá trị order đang `COMPLETED`.

## 19. FR-RPT-14 - Thống kê đánh giá

```http
GET {{baseUrl}}/api/reports/reviews?fromDate={{fromDate}}&toDate={{toDate}}
Authorization: Bearer {{ownerToken}}
```

```json
{
  "data": [
    {
      "venueId": 1,
      "venueName": "Sài Gòn Central Badminton",
      "courtId": 3,
      "courtName": "Sân A",
      "averageRating": 4.67,
      "reviewCount": 12
    }
  ]
}
```

Kết quả nhóm theo từng court và kèm venue, sắp xếp theo rating rồi số review giảm dần.

## 20. Kiểm tra phân quyền

Gọi bất kỳ endpoint report bằng CUSTOMER hoặc STAFF token:

```http
GET {{baseUrl}}/api/reports/dashboard
Authorization: Bearer {{customerToken}}
```

Kết quả mong đợi: `403 Forbidden`.

Không có token cũng phải bị từ chối.

## 21. Trường hợp lỗi

| Trường hợp | Kết quả |
| --- | --- |
| Không có token | `401/403` |
| CUSTOMER/STAFF gọi report | `403` |
| Sai định dạng ngày | `400` |
| `fromDate` sau `toDate` | `400`, `fromDate khong duoc sau toDate` |
| Không có dữ liệu | Tổng trả 0, danh sách trả `[]` |

## 22. Thứ tự test đề xuất

1. Tạo booking ở nhiều ngày, court và venue.
2. Tạo payment PAID, PENDING, FAILED và REFUNDED.
3. Phân bổ một số payment booking xuống detail.
4. Tạo và complete order ở nhiều venue.
5. Tạo product sale/rental và review.
6. Gọi revenue summary, daily, monthly.
7. Đối chiếu tổng doanh thu bằng các payment PAID.
8. Kiểm tra venue/court breakdown.
9. Kiểm tra booking status, cancellation, peak hours, popular courts.
10. Kiểm tra customer, product, order và review report.
11. Gọi dashboard và đối chiếu các chỉ số con.
12. Kiểm tra CUSTOMER/STAFF bị từ chối.

## 23. Checklist

- [ ] Chỉ OWNER truy cập được report.
- [ ] Date range inclusive và default đúng.
- [ ] Chỉ payment PAID được tính doanh thu.
- [ ] Tổng booking/order revenue khớp total revenue.
- [ ] Daily và monthly group đúng.
- [ ] Venue/court breakdown dùng allocation đúng.
- [ ] Booking status tổng bằng totalBookings.
- [ ] Cancellation rate đúng công thức phần trăm.
- [ ] Peak hour loại booking bị hủy.
- [ ] Popular court sắp xếp đúng.
- [ ] Customer frequency chính xác.
- [ ] Product sale/rental chỉ tính giao dịch hoàn tất.
- [ ] Order value/status chính xác.
- [ ] Review average và count chính xác.
- [ ] Không có dữ liệu trả số 0 hoặc danh sách rỗng.
