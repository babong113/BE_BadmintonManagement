# Hướng dẫn kiểm tra Product/Equipment API bằng Postman

Tài liệu này dùng để kiểm tra các chức năng `FR-EQP-01` đến `FR-EQP-12`.

## 1. Actor và quyền

| Actor | Role | Chức năng chính |
| --- | --- | --- |
| A1 | `CUSTOMER` | Xem/tìm sản phẩm và thuê dụng cụ cho booking của mình |
| A2 | `STAFF` | Xem/tìm, cập nhật tồn kho và thêm dụng cụ vào booking |
| A3 | `OWNER` | Toàn bộ quyền quản lý sản phẩm và tồn kho |
| Hệ thống | Backend | Tính subtotal, trừ tồn và tự cập nhật `OUT_OF_STOCK` |

Các endpoint xem sản phẩm yêu cầu `equipment:read`. Thao tác quản lý dùng role cụ thể theo bảng yêu cầu.

## 2. Chạy backend và tạo environment

Tại thư mục project, chạy:

```powershell
.\mvnw.cmd spring-boot:run
```

Base URL:

```text
http://localhost:7000
```

Tạo Postman environment với các biến:

| Variable | Giá trị |
| --- | --- |
| `baseUrl` | `http://localhost:7000` |
| `customerToken` | Access token của CUSTOMER |
| `staffToken` | Access token của STAFF |
| `ownerToken` | Access token của OWNER |
| `productId` | ID sản phẩm sau khi tạo |
| `bookingDetailId` | ID booking detail dùng để thuê |

Đăng nhập bằng:

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

Lưu `data.accessToken` vào biến tương ứng.

## 3. Danh sách endpoint

| Method | Endpoint | Actor | Chức năng |
| --- | --- | --- | --- |
| `GET` | `/api/products` | A1, A2, A3 | Danh sách sản phẩm khả dụng |
| `GET` | `/api/products?keyword={name}` | A1, A2, A3 | Tìm theo tên, không phân biệt hoa thường |
| `GET` | `/api/products/{id}` | A1, A2, A3 | Chi tiết sản phẩm |
| `POST` | `/api/products` | A3 | Thêm sản phẩm |
| `PUT` | `/api/products/{id}` | A3 | Cập nhật sản phẩm/phân loại |
| `PATCH` | `/api/products/{id}/stock` | A2, A3 | Đặt số lượng tồn hiện tại |
| `PATCH` | `/api/products/{id}/deactivate` | A3 | Ngừng kinh doanh |
| `GET` | `/api/booking-details/{detailId}/equipment` | A1, A2, A3 | Xem dụng cụ thuê của detail |
| `POST` | `/api/booking-details/{detailId}/equipment` | A1, A2, A3 | Thuê dụng cụ cho detail |

## 4. FR-EQP-04 và FR-EQP-09 - Thêm và phân loại sản phẩm

```http
POST {{baseUrl}}/api/products
Authorization: Bearer {{ownerToken}}
Content-Type: application/json
```

Sản phẩm vừa bán vừa cho thuê:

```json
{
  "name": "Vợt cầu lông Yonex",
  "quantity": 10,
  "salePrice": 1500000.00,
  "rentalPrice": 50000.00,
  "isForSale": true,
  "isForRent": true
}
```

Kết quả mong đợi: `201 Created`, trạng thái `AVAILABLE`.

Postman Tests:

```javascript
const body = pm.response.json();
pm.environment.set("productId", body.data.id);
```

Chỉ bán:

```json
{
  "name": "Ống cầu lông",
  "quantity": 20,
  "salePrice": 350000.00,
  "rentalPrice": null,
  "isForSale": true,
  "isForRent": false
}
```

Chỉ thuê:

```json
{
  "name": "Vợt cầu lông cho thuê",
  "quantity": 5,
  "salePrice": null,
  "rentalPrice": 30000.00,
  "isForSale": false,
  "isForRent": true
}
```

Quy tắc validation:

- Phải bật ít nhất một trong `isForSale`, `isForRent`.
- `isForSale = true` thì `salePrice` bắt buộc.
- `isForRent = true` thì `rentalPrice` bắt buộc.
- Giá không âm và có tối đa hai chữ số thập phân.
- `quantity` không âm.
- Tạo với `quantity = 0` sẽ tự nhận `OUT_OF_STOCK`.

## 5. FR-EQP-01 - Xem danh sách khả dụng

```http
GET {{baseUrl}}/api/products
Authorization: Bearer {{customerToken}}
```

Kết quả mong đợi: `200 OK`. Danh sách chỉ chứa sản phẩm:

- Có `status = AVAILABLE`.
- Có `quantity > 0`.
- Được sắp xếp theo tên.

## 6. FR-EQP-02 - Xem chi tiết

```http
GET {{baseUrl}}/api/products/{{productId}}
Authorization: Bearer {{customerToken}}
```

Response mẫu:

```json
{
  "success": true,
  "message": "Lay chi tiet san pham thanh cong",
  "data": {
    "id": 1,
    "name": "Vợt cầu lông Yonex",
    "quantity": 10,
    "salePrice": 1500000.00,
    "rentalPrice": 50000.00,
    "isForSale": true,
    "isForRent": true,
    "status": "AVAILABLE",
    "createdAt": "2026-09-17T14:00:00+07:00"
  }
}
```

ID không tồn tại phải trả `400 Bad Request` với thông báo `Khong tim thay san pham`.

## 7. FR-EQP-03 - Tìm kiếm theo tên

```http
GET {{baseUrl}}/api/products?keyword=yonex
Authorization: Bearer {{customerToken}}
```

Kết quả mong đợi: `200 OK`; tìm kiếm không phân biệt chữ hoa/thường và chỉ trả sản phẩm đang khả dụng.

## 8. FR-EQP-05 - Cập nhật sản phẩm

```http
PUT {{baseUrl}}/api/products/{{productId}}
Authorization: Bearer {{ownerToken}}
Content-Type: application/json
```

```json
{
  "name": "Vợt cầu lông Yonex Astrox",
  "salePrice": 1750000.00,
  "rentalPrice": 60000.00,
  "isForSale": true,
  "isForRent": true,
  "status": "AVAILABLE"
}
```

Kết quả mong đợi: `200 OK`.

Không được gửi `status = OUT_OF_STOCK` ở endpoint này vì trạng thái đó do backend quản lý theo số lượng. Owner có thể chọn `AVAILABLE` hoặc `INACTIVE`; nếu chọn `AVAILABLE` nhưng quantity bằng 0, kết quả vẫn là `OUT_OF_STOCK`.

## 9. FR-EQP-06 và FR-EQP-08 - Cập nhật tồn kho

Đặt tồn kho hiện tại thành 5:

```http
PATCH {{baseUrl}}/api/products/{{productId}}/stock
Authorization: Bearer {{staffToken}}
Content-Type: application/json
```

```json
{
  "quantity": 5
}
```

Đánh dấu hết hàng tự động:

```json
{
  "quantity": 0
}
```

Kết quả mong đợi:

- Quantity bằng 0: `status = OUT_OF_STOCK`.
- Bổ sung quantity lớn hơn 0 cho sản phẩm `OUT_OF_STOCK`: `status = AVAILABLE`.
- Sản phẩm `INACTIVE` vẫn giữ `INACTIVE` khi chỉnh tồn kho; owner phải kích hoạt lại bằng endpoint update.
- CUSTOMER gọi endpoint này phải nhận `403 Forbidden`.

## 10. FR-EQP-07 - Ngừng kinh doanh

```http
PATCH {{baseUrl}}/api/products/{{productId}}/deactivate
Authorization: Bearer {{ownerToken}}
```

Kết quả mong đợi: `200 OK`, `status = INACTIVE`. Sản phẩm không còn xuất hiện trong danh sách/tìm kiếm khả dụng và không thể được thuê.

## 11. Chuẩn bị booking để thuê dụng cụ

Booking detail dùng để test phải:

- Thuộc booking của CUSTOMER đang đăng nhập, hoặc request được gửi bởi STAFF/OWNER.
- Booking chưa `CANCELLED` hoặc `COMPLETED`.
- Detail có trạng thái `ACTIVE`.
- Sản phẩm có `isForRent = true`, có `rentalPrice`, `status = AVAILABLE` và đủ tồn kho.

## 12. FR-EQP-10 và FR-EQP-11 - Thuê và tính tiền

```http
POST {{baseUrl}}/api/booking-details/{{bookingDetailId}}/equipment
Authorization: Bearer {{customerToken}}
Content-Type: application/json
```

```json
{
  "productId": 1,
  "quantity": 2
}
```

Nếu `rentalPrice = 50000.00`, kết quả mong đợi là `201 Created`:

```json
{
  "success": true,
  "message": "Them dung cu thue vao booking detail thanh cong",
  "data": {
    "id": 1,
    "bookingDetailId": 10,
    "productId": 1,
    "productName": "Vợt cầu lông Yonex",
    "quantity": 2,
    "unitPrice": 50000.00,
    "subtotal": 100000.00
  }
}
```

Backend thực hiện trong cùng transaction:

1. Khóa booking detail, booking và sản phẩm.
2. Kiểm tra actor và trạng thái booking.
3. Kiểm tra khả năng cho thuê và tồn kho.
4. Tính `subtotal = quantity × rentalPrice`.
5. Trừ quantity khỏi tồn kho.
6. Chuyển sản phẩm sang `OUT_OF_STOCK` nếu tồn kho về 0.
7. Cộng subtotal vào `booking.totalAmount`.

Sau đó gọi lại endpoint chi tiết booking và payment summary để xác nhận tổng tiền đã tăng.

## 13. Xem dụng cụ đã thuê của detail

```http
GET {{baseUrl}}/api/booking-details/{{bookingDetailId}}/equipment
Authorization: Bearer {{customerToken}}
```

Kết quả mong đợi: `200 OK`, trả danh sách rental theo thứ tự ID tăng dần. CUSTOMER không thể xem detail thuộc booking của người khác.

## 14. FR-EQP-12 - Kiểm tra tồn kho

Giả sử sản phẩm còn 2 đơn vị, thử thuê 3:

```json
{
  "productId": 1,
  "quantity": 3
}
```

Kết quả mong đợi: `400 Bad Request`.

```json
{
  "success": false,
  "message": "So luong thue vuot qua ton kho",
  "data": 400
}
```

Tồn kho và tổng tiền booking phải không thay đổi khi request thất bại. Khóa pessimistic bảo vệ tồn kho khi có nhiều request thuê đồng thời.

## 15. Các trường hợp lỗi quan trọng

| Trường hợp | Kết quả mong đợi |
| --- | --- |
| Không có Bearer token | `401/403` |
| CUSTOMER thêm/sửa/deactivate sản phẩm | `403` |
| STAFF thêm/sửa/deactivate sản phẩm | `403` |
| CUSTOMER cập nhật tồn kho | `403` |
| Sản phẩm không hỗ trợ thuê | `400` |
| Sản phẩm `INACTIVE` hoặc `OUT_OF_STOCK` | `400` |
| Quantity thuê bằng 0 hoặc âm | `400` validation |
| Quantity thuê vượt tồn | `400` |
| CUSTOMER thuê cho booking người khác | `400` |
| Booking/detail đã hủy hoặc hoàn tất | `400` |

## 16. Thứ tự test đề xuất

1. OWNER tạo ba sản phẩm: chỉ bán, chỉ thuê và bán + thuê.
2. CUSTOMER xem danh sách, chi tiết và tìm kiếm.
3. OWNER cập nhật tên, giá và phân loại.
4. STAFF đặt tồn kho về 0 và kiểm tra `OUT_OF_STOCK`.
5. STAFF bổ sung tồn và kiểm tra `AVAILABLE`.
6. CUSTOMER thuê sản phẩm cho booking detail của mình.
7. Kiểm tra subtotal, tồn kho và `booking.totalAmount`.
8. Thử thuê vượt tồn và kiểm tra dữ liệu không đổi.
9. OWNER deactivate sản phẩm và xác nhận không thể thuê.

## 17. Checklist

- [ ] Danh sách chỉ chứa sản phẩm khả dụng và còn tồn.
- [ ] Tìm kiếm theo tên không phân biệt hoa thường.
- [ ] Chỉ OWNER thêm/cập nhật/ngừng kinh doanh sản phẩm.
- [ ] STAFF và OWNER cập nhật được tồn kho.
- [ ] Trạng thái tự chuyển theo quantity.
- [ ] Phân loại chỉ bán/chỉ thuê/cả hai hoạt động đúng.
- [ ] CUSTOMER chỉ thuê cho booking của mình.
- [ ] Subtotal bằng `quantity × rentalPrice`.
- [ ] Tiền thuê được cộng vào tổng booking.
- [ ] Tồn kho bị trừ đúng và không thể âm.
- [ ] Request vượt tồn kho bị rollback toàn bộ.
