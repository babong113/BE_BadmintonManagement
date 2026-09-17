# Hướng dẫn kiểm tra Staff API bằng Postman

Tài liệu này kiểm tra `FR-STF-01` đến `FR-STF-09` và `FR-CSH-17`.

## 1. Chạy backend và đăng nhập

Tại thư mục project:

```powershell
.\mvnw.cmd spring-boot:run
```

Base URL:

```text
http://localhost:7000
```

Đăng nhập bằng `OWNER` để lấy `ownerToken` và bằng `STAFF` để lấy `staffToken`:

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

Tạo các biến Postman:

| Biến | Giá trị |
| --- | --- |
| `baseUrl` | `http://localhost:7000` |
| `ownerToken` | Token tài khoản OWNER |
| `staffToken` | Token tài khoản STAFF |
| `staffId` | ID nhân viên được tạo |
| `venueId` | ID cơ sở cần kiểm tra ca trực |

Các API quản trị yêu cầu `staff:manage` và chỉ OWNER có permission này. API `/me` yêu cầu role `STAFF`.

## 2. Danh sách endpoint

| Method | Endpoint | Actor | Chức năng |
| --- | --- | --- | --- |
| `GET` | `/api/staff?status=ACTIVE` | A3 | Danh sách nhân viên, có thể lọc trạng thái |
| `POST` | `/api/staff` | A3 | Tạo tài khoản và hồ sơ STAFF |
| `GET` | `/api/staff/{id}` | A3 | Xem chi tiết nhân viên |
| `PUT` | `/api/staff/{id}` | A3 | Cập nhật tài khoản và hồ sơ |
| `PATCH` | `/api/staff/{id}/lock` | A3 | Khóa tài khoản |
| `PATCH` | `/api/staff/{id}/unlock` | A3 | Mở lại tài khoản |
| `GET` | `/api/staff/me` | A2 | Nhân viên xem hồ sơ của mình |
| `GET` | `/api/staff/on-duty?venueId={venueId}` | A3 | Thu ngân có ca OPEN tại cơ sở |

## 3. FR-STF-02/05/06 - Tạo nhân viên

```http
POST {{baseUrl}}/api/staff
Authorization: Bearer {{ownerToken}}
Content-Type: application/json
```

```json
{
  "email": "cashier01@example.com",
  "password": "123456",
  "phoneNumber": "0901234567",
  "fullName": "Nguyen Van Thu Ngan",
  "avatarUrl": null,
  "employeeCode": "nv-001",
  "identityCard": "012345678901",
  "hireDate": "2026-09-17",
  "position": "cashier",
  "notes": "Thu ngan ca sang"
}
```

Kết quả mong đợi: `201 Created`.

```json
{
  "success": true,
  "message": "Tao nhan vien thanh cong",
  "data": {
    "id": 10,
    "employeeCode": "NV-001",
    "fullName": "Nguyen Van Thu Ngan",
    "email": "cashier01@example.com",
    "phoneNumber": "0901234567",
    "identityCard": "012345678901",
    "hireDate": "2026-09-17",
    "position": "CASHIER",
    "status": "ACTIVE"
  }
}
```

Hệ thống tự động:

- Mã hóa mật khẩu bằng BCrypt.
- Gán role `STAFF`.
- Chuẩn hóa email thành chữ thường.
- Chuẩn hóa `employeeCode` và `position` thành chữ hoa.
- Đặt trạng thái tài khoản `ACTIVE`.

Lưu `data.id` vào `staffId`. Các trường email, số điện thoại, mã nhân viên và CCCD bị trùng phải trả lỗi `400`.

## 4. FR-STF-01 - Danh sách nhân viên

```http
GET {{baseUrl}}/api/staff?status=ACTIVE
Authorization: Bearer {{ownerToken}}
```

`status` không bắt buộc; chấp nhận `ACTIVE`, `BLOCKED`, `INACTIVE`. Không truyền thì mặc định chỉ trả nhân viên `ACTIVE` đang làm việc.

Kết quả mong đợi: `200 OK`, sắp xếp theo họ tên tăng dần.

## 5. FR-STF-03 - Chi tiết nhân viên

```http
GET {{baseUrl}}/api/staff/{{staffId}}
Authorization: Bearer {{ownerToken}}
```

Response gồm mã nhân viên, thông tin liên hệ, CCCD, ngày vào làm, vị trí, trạng thái, ghi chú và thời điểm tạo/cập nhật tài khoản.

ID không tồn tại trả lỗi `Khong tim thay nhan vien`.

## 6. FR-STF-04 - Cập nhật nhân viên

```http
PUT {{baseUrl}}/api/staff/{{staffId}}
Authorization: Bearer {{ownerToken}}
Content-Type: application/json
```

```json
{
  "email": "cashier01@example.com",
  "phoneNumber": "0901234567",
  "fullName": "Nguyen Van A",
  "avatarUrl": "https://example.com/avatar.jpg",
  "employeeCode": "NV-001",
  "identityCard": "012345678901",
  "hireDate": "2026-09-17",
  "position": "RECEPTIONIST",
  "notes": "Chuyen sang le tan"
}
```

Kết quả mong đợi: `200 OK`. API không thay đổi mật khẩu và role của nhân viên.

## 7. FR-STF-07/08 - Khóa và mở tài khoản

Khóa:

```http
PATCH {{baseUrl}}/api/staff/{{staffId}}/lock
Authorization: Bearer {{ownerToken}}
```

Response có `status = BLOCKED`. Tài khoản bị khóa không thể đăng nhập.

Mở lại:

```http
PATCH {{baseUrl}}/api/staff/{{staffId}}/unlock
Authorization: Bearer {{ownerToken}}
```

Response có `status = ACTIVE` và nhân viên có thể đăng nhập lại.

## 8. FR-STF-09 - Xem hồ sơ bản thân

```http
GET {{baseUrl}}/api/staff/me
Authorization: Bearer {{staffToken}}
```

Kết quả mong đợi: `200 OK`, hồ sơ ứng với email trong JWT. CUSTOMER hoặc OWNER không có role STAFF nhận `403 Forbidden`.

## 9. FR-CSH-17 - Thu ngân đang trực theo chi nhánh

Chuẩn bị một ca đang mở cho nhân viên có `position = CASHIER`:

```sql
INSERT INTO cashier_shifts (staff_id, venue_id, opening_cash, status)
VALUES (<staffId>, <venueId>, 500000.00, 'OPEN');
```

Gọi API:

```http
GET {{baseUrl}}/api/staff/on-duty?venueId={{venueId}}
Authorization: Bearer {{ownerToken}}
```

Response mẫu:

```json
{
  "success": true,
  "message": "Lay danh sach thu ngan dang truc thanh cong",
  "data": [
    {
      "staffId": 10,
      "employeeCode": "NV-001",
      "fullName": "Nguyen Van Thu Ngan",
      "email": "cashier01@example.com",
      "phoneNumber": "0901234567",
      "position": "CASHIER",
      "shiftId": 5,
      "openedAt": "2026-09-17T08:00:00+07:00",
      "shiftStatus": "OPEN",
      "venueId": 1,
      "venueName": "Co so Quan 1"
    }
  ]
}
```

API không trả nhân viên bị khóa, nhân viên không phải `CASHIER`, ca đã `CLOSED` hoặc ca thuộc venue khác. `venueId` không tồn tại trả lỗi `Khong tim thay co so`.

## 10. Kiểm tra phân quyền và validation

- Gọi API quản trị bằng STAFF/CUSTOMER: mong đợi `403 Forbidden`.
- Gọi `/api/staff/me` bằng OWNER/CUSTOMER: mong đợi `403 Forbidden`.
- Tạo hai nhân viên có mã `nv-001` và `NV-001`: request thứ hai phải bị từ chối.
- Tạo nhân viên với email, số điện thoại hoặc CCCD đã tồn tại: phải bị từ chối.
- `hireDate` ở tương lai, mật khẩu dưới 6 ký tự hoặc số điện thoại sai định dạng: mong đợi lỗi validation.

## 11. Thứ tự test đề xuất

1. Đăng nhập OWNER.
2. Tạo nhân viên CASHIER và lưu `staffId`.
3. Xem danh sách và chi tiết.
4. Cập nhật hồ sơ nhưng giữ `position = CASHIER` để test ca trực.
5. Đăng nhập tài khoản STAFF vừa tạo và gọi `/me`.
6. Tạo ca `OPEN`, gọi `/on-duty` theo venue.
7. Khóa tài khoản và kiểm tra không đăng nhập được.
8. Mở lại tài khoản và kiểm tra đăng nhập được.
9. Thử các trường hợp trùng dữ liệu và sai quyền.

## 12. Checklist

- [ ] OWNER xem, tạo, sửa, khóa và mở lại nhân viên thành công.
- [ ] Tài khoản mới có role STAFF và mật khẩu đã mã hóa.
- [ ] `employee_code` duy nhất không phân biệt hoa/thường.
- [ ] `position` được chuẩn hóa; thu ngân có giá trị `CASHIER`.
- [ ] STAFF chỉ xem đúng hồ sơ của mình qua `/me`.
- [ ] Danh sách ca trực chỉ gồm CASHIER ACTIVE có ca OPEN tại venue được chọn.
- [ ] Permission và validation trả đúng lỗi.
