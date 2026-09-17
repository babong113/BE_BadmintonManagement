# Huong Dan Test API Bookings Bang Postman

Guide nay dung de test cac API booking trong Postman.

Base URL mac dinh:

```text
http://localhost:7000
```

## 1. Chay Backend

Mo terminal tai thu muc project:

```text
D:\BackEnd\DangNhap_API
```

Chay lenh:

```powershell
.\mvnw.cmd spring-boot:run
```

## 2. Tao Postman Environment

Tao environment `DangNhap API Local` voi cac bien:

| Variable | Initial value | Current value |
| --- | --- | --- |
| `baseUrl` | `http://localhost:7000` | `http://localhost:7000` |
| `accessToken` | de trong | de trong |
| `bookingId` | de trong | de trong |
| `bookingDetailId` | de trong | de trong |
| `courtId` | `1` | `1` |
| `newCourtId` | `2` | `2` |
| `venueId` | `1` | `1` |
| `customerId` | de trong | de trong |

## 3. Login Lay Token

Tao request:

```text
Name: Auth - Login
Method: POST
URL: {{baseUrl}}/api/auth/login
```

Headers:

| Key | Value |
| --- | --- |
| `Content-Type` | `application/json` |

Body:

```json
{
  "email": "customer@example.com",
  "password": "123456"
}
```

Sau khi login, copy `data.accessToken` vao bien:

```text
accessToken
```

Tat ca request booking can header:

| Key | Value |
| --- | --- |
| `Authorization` | `Bearer {{accessToken}}` |

Permission can nho:

| Nhom API | Permission |
| --- | --- |
| Tim san trong | `booking:create` hoac `booking:read` |
| Tao booking | `booking:create` |
| Xem booking ca nhan | Dang nhap |
| Xem lich dat san | `booking:read` |
| Xem chi tiet booking | Chinh chu booking hoac `booking:read` |
| Xac nhan/cap nhat/doi san/doi gio/huy detail/hoan tat | `booking:update` |
| Huy booking | `booking:cancel` |

## 4. FR-BKG-01/02/03 - Chon Ngay, Chon Gio, Kiem Tra San Trong

Tao request:

```text
Name: Booking - Available Courts
Method: GET
URL: {{baseUrl}}/api/bookings/available-courts
```

Params:

| Key | Value |
| --- | --- |
| `venueId` | `{{venueId}}` |
| `bookingDate` | `2026-09-15` |
| `startTime` | `18:00:00` |
| `endTime` | `19:00:00` |

Response mong doi:

```json
{
  "success": true,
  "message": "Tim san trong thanh cong",
  "data": [
    {
      "courtId": 1,
      "venueId": 1,
      "venueName": "Sai Gon Central Badminton",
      "courtCode": "C01",
      "courtName": "Court 1",
      "pricePerHour": 120000.00
    }
  ]
}
```

Copy `data[0].courtId` vao bien:

```text
courtId
```

Quy tac trung lich: chi booking `CONFIRMED` va booking `PENDING` duoi 15 phut duoc tinh la dang giu san. Booking `CANCELLED`, `EXPIRED` va `PENDING` da qua 15 phut khong chan ket qua san trong.

## 5. FR-BKG-04 - Khach Hang Dat Mot San

Login bang account `CUSTOMER`.

Tao request:

```text
Name: Booking - Create One Court
Method: POST
URL: {{baseUrl}}/api/bookings
```

Headers:

| Key | Value |
| --- | --- |
| `Authorization` | `Bearer {{accessToken}}` |
| `Content-Type` | `application/json` |

Body:

```json
{
  "venueId": {{venueId}},
  "bookingDate": "2026-09-15",
  "note": "Dat mot san tu Postman",
  "details": [
    {
      "courtId": 1,
      "startTime": "18:00:00",
      "endTime": "19:00:00"
    }
  ]
}
```

Response mong doi:

```json
{
  "success": true,
  "message": "Tao booking thanh cong",
  "data": {
    "id": 1,
    "bookingCode": "BKG20260914-123456",
    "venueId": 1,
    "venueName": "Sai Gon Central Badminton",
    "customerId": 1,
    "bookingDate": "2026-09-15",
    "totalAmount": 120000.00,
    "status": "PENDING",
    "note": "Dat mot san tu Postman",
    "details": [
      {
        "id": 1,
        "courtId": 1,
        "startTime": "18:00:00",
        "endTime": "19:00:00",
        "unitPrice": 120000.00,
        "subtotal": 120000.00,
        "status": "ACTIVE"
      }
    ]
  }
}
```

Copy:

```text
data.id -> bookingId
data.details[0].id -> bookingDetailId
```

Postman tab `Tests` co the dung:

```javascript
const json = pm.response.json();
if (json.data) {
  pm.environment.set("bookingId", json.data.id);
  if (json.data.details && json.data.details.length > 0) {
    pm.environment.set("bookingDetailId", json.data.details[0].id);
  }
}
```

## 6. FR-BKG-05 - Dat Nhieu San

Tao request:

```text
Name: Booking - Create Multiple Courts
Method: POST
URL: {{baseUrl}}/api/bookings
```

Body:

```json
{
  "venueId": {{venueId}},
  "bookingDate": "2026-09-16",
  "note": "Dat nhieu san tu Postman",
  "details": [
    {
      "courtId": 1,
      "startTime": "18:00:00",
      "endTime": "19:00:00"
    },
    {
      "courtId": 2,
      "startTime": "18:00:00",
      "endTime": "19:00:00"
    }
  ]
}
```

Response mong doi: booking co nhieu item trong `data.details`, moi detail co `subtotal`, booking co `totalAmount`.

## 7. FR-BKG-06 - Nhan Vien Tao Booking Cho Khach Co Tai Khoan

Login bang account `STAFF` hoac `OWNER`.

Tao request:

```text
Name: Booking - Staff Create For Customer
Method: POST
URL: {{baseUrl}}/api/bookings
```

Body:

```json
{
  "venueId": {{venueId}},
  "customerId": 1,
  "bookingDate": "2026-09-17",
  "note": "Nhan vien tao booking cho khach co tai khoan",
  "details": [
    {
      "courtId": 1,
      "startTime": "19:00:00",
      "endTime": "20:30:00"
    }
  ]
}
```

Response mong doi:

```json
{
  "success": true,
  "message": "Tao booking thanh cong",
  "data": {
    "customerId": 1,
    "guestName": null,
    "guestPhone": null,
    "status": "PENDING"
  }
}
```

## 8. FR-BKG-07 - Nhan Vien Tao Booking Khach Vang Lai

Login bang account `STAFF` hoac `OWNER`.

Tao request:

```text
Name: Booking - Staff Create For Guest
Method: POST
URL: {{baseUrl}}/api/bookings
```

Body:

```json
{
  "venueId": {{venueId}},
  "guestName": "Khach Vang Lai",
  "guestPhone": "0909999999",
  "bookingDate": "2026-09-18",
  "note": "Nhan vien tao booking cho khach vang lai",
  "details": [
    {
      "courtId": 1,
      "startTime": "20:00:00",
      "endTime": "21:00:00"
    }
  ]
}
```

Response mong doi:

```json
{
  "success": true,
  "message": "Tao booking thanh cong",
  "data": {
    "customerId": null,
    "guestName": "Khach Vang Lai",
    "guestPhone": "0909999999",
    "status": "PENDING"
  }
}
```

## 9. FR-BKG-11 - Xac Nhan Booking

Can permission `booking:update`. Tai khoan xac nhan phai la nhan vien dang co ca thu ngan `OPEN` tai dung venue cua booking. Booking chi duoc xac nhan trong 15 phut ke tu `createdAt`.

Tao request:

```text
Name: Booking - Confirm
Method: PATCH
URL: {{baseUrl}}/api/bookings/{{bookingId}}/confirm
```

Response mong doi:

```json
{
  "success": true,
  "message": "Xac nhan booking thanh cong",
  "data": {
    "id": 1,
    "status": "CONFIRMED"
  }
}
```

Neu qua 15 phut, response khong thanh cong voi message:

```text
Booking da het thoi gian giu san 15 phut
```

Neu nhan vien khong co ca `OPEN` tai dung venue, response khong thanh cong voi message:

```text
Nhan vien phai co ca OPEN tai dung co so cua booking
```

## 10. FR-BKG-12 - Xem Booking Ca Nhan

Login bang account customer.

Tao request:

```text
Name: Booking - My Bookings
Method: GET
URL: {{baseUrl}}/api/bookings/me
```

Response mong doi:

```json
{
  "success": true,
  "message": "Lay booking ca nhan thanh cong",
  "data": [
    {
      "id": 1,
      "bookingCode": "BKG20260914-123456",
      "customerId": 1,
      "status": "PENDING"
    }
  ]
}
```

## 11. FR-BKG-13 - Xem Lich Dat San

Can permission `booking:read`.

Tao request:

```text
Name: Booking - Schedule
Method: GET
URL: {{baseUrl}}/api/bookings/schedule
```

Params:

| Key | Value |
| --- | --- |
| `bookingDate` | `2026-09-15` |
| `courtId` | de trong hoac `{{courtId}}` |
| `venueId` | de trong hoac `{{venueId}}` |

Response mong doi:

```json
{
  "success": true,
  "message": "Lay lich dat san thanh cong",
  "data": [
    {
      "id": 1,
      "bookingDate": "2026-09-15",
      "status": "CONFIRMED",
      "details": [
        {
          "courtId": 1,
          "startTime": "18:00:00",
          "endTime": "19:00:00"
        }
      ]
    }
  ]
}
```

## 12. FR-BKG-14 - Xem Chi Tiet Booking

Tao request:

```text
Name: Booking - Get Detail
Method: GET
URL: {{baseUrl}}/api/bookings/{{bookingId}}
```

Customer chi xem duoc booking cua minh. Staff/Owner co `booking:read` xem duoc booking cua khach.

Response mong doi:

```json
{
  "success": true,
  "message": "Lay chi tiet booking thanh cong",
  "data": {
    "id": 1,
    "bookingCode": "BKG20260914-123456",
    "customerId": 1,
    "bookingDate": "2026-09-15",
    "totalAmount": 120000.00,
    "status": "CONFIRMED",
    "details": [
      {
        "id": 1,
        "courtId": 1,
        "courtCode": "C01",
        "startTime": "18:00:00",
        "endTime": "19:00:00",
        "unitPrice": 120000.00,
        "subtotal": 120000.00,
        "status": "ACTIVE"
      }
    ]
  }
}
```

## 13. FR-BKG-15/24 - Cap Nhat Booking Va Ghi Chu

Can permission `booking:update`.

Tao request:

```text
Name: Booking - Update
Method: PUT
URL: {{baseUrl}}/api/bookings/{{bookingId}}
```

Body:

```json
{
  "guestName": null,
  "guestPhone": null,
  "note": "Cap nhat ghi chu booking"
}
```

Response mong doi:

```json
{
  "success": true,
  "message": "Cap nhat booking thanh cong",
  "data": {
    "id": 1,
    "note": "Cap nhat ghi chu booking"
  }
}
```

## 14. FR-BKG-16 - Doi San

Can permission `booking:update`.

Tao request:

```text
Name: Booking - Change Court
Method: PATCH
URL: {{baseUrl}}/api/bookings/details/{{bookingDetailId}}/court
```

Body:

```json
{
  "courtId": 2
}
```

`newCourtId` bat buoc phai thuoc cung `venueId` cua booking. Neu khac co so, API tra loi validation va khong thay doi booking detail.

Response mong doi:

```json
{
  "success": true,
  "message": "Doi san thanh cong",
  "data": {
    "id": 1,
    "details": [
      {
        "id": 1,
        "courtId": 2,
        "unitPrice": 150000.00,
        "subtotal": 150000.00
      }
    ]
  }
}
```

## 15. FR-BKG-17 - Doi Gio Dat San

Can permission `booking:update`.

Tao request:

```text
Name: Booking - Change Time
Method: PATCH
URL: {{baseUrl}}/api/bookings/details/{{bookingDetailId}}/time
```

Body:

```json
{
  "startTime": "19:00:00",
  "endTime": "20:30:00"
}
```

Response mong doi:

```json
{
  "success": true,
  "message": "Doi gio dat san thanh cong",
  "data": {
    "id": 1,
    "details": [
      {
        "id": 1,
        "startTime": "19:00:00",
        "endTime": "20:30:00",
        "subtotal": 180000.00
      }
    ]
  }
}
```

## 16. FR-BKG-18 - Huy Booking

Can permission `booking:cancel`.

Tao request:

```text
Name: Booking - Cancel
Method: PATCH
URL: {{baseUrl}}/api/bookings/{{bookingId}}/cancel
```

Response mong doi:

```json
{
  "success": true,
  "message": "Huy booking thanh cong",
  "data": {
    "id": 1,
    "status": "CANCELLED",
    "details": [
      {
        "status": "CANCELLED"
      }
    ]
  }
}
```

## 17. FR-BKG-19 - Huy Mot San Trong Booking

Can permission `booking:update`.

Tao request:

```text
Name: Booking - Cancel Detail
Method: PATCH
URL: {{baseUrl}}/api/bookings/details/{{bookingDetailId}}/cancel
```

Response mong doi:

```json
{
  "success": true,
  "message": "Huy san trong booking thanh cong",
  "data": {
    "id": 1,
    "details": [
      {
        "id": 1,
        "status": "CANCELLED"
      }
    ]
  }
}
```

## 18. FR-BKG-20 - Hoan Tat Booking

Can permission `booking:update`.

Tao request:

```text
Name: Booking - Complete
Method: PATCH
URL: {{baseUrl}}/api/bookings/{{bookingId}}/complete
```

Response mong doi:

```json
{
  "success": true,
  "message": "Hoan tat booking thanh cong",
  "data": {
    "id": 1,
    "status": "COMPLETED",
    "details": [
      {
        "status": "COMPLETED"
      }
    ]
  }
}
```

## 19. FR-BKG-25 - Xem Va Cap Nhat Co So Cua Booking

Venue da duoc xac dinh ngay khi tao booking qua truong bat buoc `venueId`. De xem venue cua booking:

```text
Name: Booking - Get Venue
Method: GET
URL: {{baseUrl}}/api/bookings/{{bookingId}}/venue
```

Customer chi xem duoc booking cua minh; staff/owner co `booking:read` xem duoc booking cua khach.

Response mong doi:

```json
{
  "success": true,
  "message": "Lay co so cua booking thanh cong",
  "data": {
    "bookingId": 1,
    "bookingCode": "BKG20260914-123456",
    "venueId": 1,
    "venueName": "Sai Gon Central Badminton"
  }
}
```

De cap nhat venue:

```text
Name: Booking - Change Venue
Method: PATCH
URL: {{baseUrl}}/api/bookings/{{bookingId}}/venue
```

Body:

```json
{
  "venueId": 1
}
```

Customer chi cap nhat duoc booking cua minh; staff/owner can `booking:update`. Booking da `CANCELLED`/`COMPLETED`/`EXPIRED` khong duoc cap nhat.

## 20. FR-BKG-26 - Kiem Tra Tat Ca San Cung Co So

He thong kiem tra quy tac nay tai ba noi:

1. Khi `POST /api/bookings`, moi `courtId` phai thuoc `venueId` trong request.
2. Khi doi san, san moi phai thuoc venue cua booking.
3. Khi cap nhat venue, tat ca san hien tai phai thuoc venue moi.

Test am: tao booking voi `venueId = 1` nhung chon mot `courtId` thuoc venue 2. Response phai khong thanh cong va co message:

```text
Tat ca san trong booking phai thuoc co so da chon
```

Database cung co trigger chan INSERT/UPDATE `booking_details` sai venue, ke ca khi du lieu khong di qua API.

## 21. BR-21/24/25/26 - Giu San 15 Phut Va Giai Phong Lich

Khi tao booking, booking co status `PENDING` va giu lich trong 15 phut. He thong chay tac vu het han moi 60 giay; cac API xem/kiem tra/cap nhat booking cung dong bo trang thai het han truoc khi xu ly.

Kich ban test nhanh:

1. Tao booking `PENDING` tai mot court va khung gio chua co lich.
2. Goi lai `GET /api/bookings/available-courts` cung court/ngay/gio: court khong con trong danh sach.
3. Xac nhan trong 15 phut bang nhan vien co ca `OPEN` tai dung venue: booking thanh `CONFIRMED` va tiep tuc chiem lich.
4. Tao mot booking `PENDING` khac de test het han. Co the doi 16 phut, hoac chi trong moi truong test cap nhat thoi gian:

```sql
UPDATE bookings
SET created_at = CURRENT_TIMESTAMP - INTERVAL '16 minutes'
WHERE id = <booking_id> AND status = 'PENDING';
```

5. Goi API chi tiet, lich, booking ca nhan hoac kiem tra san trong. Booking cu phai thanh `EXPIRED`.
6. Goi lai API san trong: court duoc mo lai.
7. Thu xac nhan booking `EXPIRED`: he thong phai tu choi.

Co the thay doi tan suat job bang bien cau hinh `booking.expiration-check-ms`; mac dinh la `60000` ms.

## 22. Checklist Test Nhanh

Test voi customer:

```text
1. Auth - Login bang CUSTOMER
2. Booking - Available Courts
3. Booking - Create One Court
4. Luu bookingId va bookingDetailId
5. Booking - My Bookings
6. Booking - Get Detail
7. Booking - Get Venue
8. Booking - Cancel
```

Test voi staff/owner:

```text
1. Auth - Login bang STAFF hoac OWNER
2. Booking - Available Courts
3. Booking - Staff Create For Customer
4. Booking - Staff Create For Guest
5. Booking - Confirm
6. Booking - Schedule
7. Booking - Update
8. Booking - Change Court
9. Booking - Get Venue
10. Booking - Change Venue
11. Booking - Change Time
12. Booking - Cancel Detail
13. Booking - Complete
```

## 23. Bang Tong Hop Endpoint

| Chuc nang | Method | URL | Permission |
| --- | --- | --- | --- |
| Kiem tra san trong | `GET` | `{{baseUrl}}/api/bookings/available-courts` | `booking:create` hoac `booking:read` |
| Tao booking | `POST` | `{{baseUrl}}/api/bookings` | `booking:create` |
| Xac nhan booking trong 15 phut | `PATCH` | `{{baseUrl}}/api/bookings/{{bookingId}}/confirm` | `booking:update` + ca `OPEN` dung venue |
| Booking ca nhan | `GET` | `{{baseUrl}}/api/bookings/me` | Dang nhap |
| Lich dat san | `GET` | `{{baseUrl}}/api/bookings/schedule` | `booking:read` |
| Chi tiet booking | `GET` | `{{baseUrl}}/api/bookings/{{bookingId}}` | Chinh chu hoac `booking:read` |
| Xem co so booking | `GET` | `{{baseUrl}}/api/bookings/{{bookingId}}/venue` | Chinh chu hoac `booking:read` |
| Cap nhat co so booking | `PATCH` | `{{baseUrl}}/api/bookings/{{bookingId}}/venue` | Chinh chu co `booking:create` hoac `booking:update` |
| Cap nhat booking | `PUT` | `{{baseUrl}}/api/bookings/{{bookingId}}` | `booking:update` |
| Doi san | `PATCH` | `{{baseUrl}}/api/bookings/details/{{bookingDetailId}}/court` | `booking:update` |
| Doi gio | `PATCH` | `{{baseUrl}}/api/bookings/details/{{bookingDetailId}}/time` | `booking:update` |
| Huy booking | `PATCH` | `{{baseUrl}}/api/bookings/{{bookingId}}/cancel` | `booking:cancel` |
| Huy mot san trong booking | `PATCH` | `{{baseUrl}}/api/bookings/details/{{bookingDetailId}}/cancel` | `booking:update` |
| Hoan tat booking | `PATCH` | `{{baseUrl}}/api/bookings/{{bookingId}}/complete` | `booking:update` |
