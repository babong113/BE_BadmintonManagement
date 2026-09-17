# Huong Dan Test API Courts Bang Postman

Guide nay dung de test cac API court trong Postman.

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

Neu app chay thanh cong, backend se lang nghe o:

```text
http://localhost:7000
```

## 2. Tao Postman Environment

Trong Postman:

1. Bam `Environments`.
2. Bam `+` de tao environment moi.
3. Dat ten: `DangNhap API Local`.
4. Them bien:

| Variable | Initial value | Current value |
| --- | --- | --- |
| `baseUrl` | `http://localhost:7000` | `http://localhost:7000` |
| `accessToken` | de trong | de trong |
| `venueId` | `1` | `1` |
| `courtId` | de trong | de trong |
| `createdCourtId` | de trong | de trong |

Sau do chon environment `DangNhap API Local` o goc phai tren Postman.

## 3. Quyen Test

API doc court la public, khong can token:

```text
GET /api/courts/**
```

API ghi du lieu can login bang account co permission:

```text
court:write
```

Thuong la account role `OWNER` neu database seed dung SQL moi.

## 4. Login Lay Access Token Cho API Ghi Du Lieu

Tao request:

```text
Name: Auth - Login
Method: POST
URL: {{baseUrl}}/api/auth/login
```

Tab `Headers`:

| Key | Value |
| --- | --- |
| `Content-Type` | `application/json` |

Tab `Body`: chon `raw` va `JSON`, sau do nhap:

```json
{
  "email": "owner@example.com",
  "password": "123456"
}
```

Sau khi bam `Send`, copy `data.accessToken` vao environment variable:

```text
accessToken
```

Voi request `POST`, `PUT`, `PATCH`, them header:

| Key | Value |
| --- | --- |
| `Authorization` | `Bearer {{accessToken}}` |

## 5. FR-CRT-01 - Xem Danh Sach San

Tao request:

```text
Name: Court - Get All
Method: GET
URL: {{baseUrl}}/api/courts
```

Khong can header.

Bam `Send`.

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Lay danh sach san thanh cong",
  "data": [
    {
      "id": 1,
      "venueId": 1,
      "venueName": "Sai Gon Central Badminton",
      "courtCode": "C01",
      "name": "Court 1",
      "description": "San cau long trong nha",
      "pricePerHour": 120000.00,
      "status": "AVAILABLE",
      "imageUrl": null
    }
  ]
}
```

Copy `data[0].id` vao environment variable:

```text
courtId
```

## 6. FR-CRT-02 - Xem San Theo Co So

Tao request:

```text
Name: Court - Get By Venue
Method: GET
URL: {{baseUrl}}/api/courts/venue/{{venueId}}
```

Khong can header.

Bam `Send`.

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Lay danh sach san theo co so thanh cong",
  "data": [
    {
      "id": 1,
      "venueId": 1,
      "venueName": "Sai Gon Central Badminton",
      "courtCode": "C01",
      "name": "Court 1",
      "description": "San cau long trong nha",
      "pricePerHour": 120000.00,
      "status": "AVAILABLE",
      "imageUrl": null
    }
  ]
}
```

Neu venue khong ton tai hoac da inactive:

```json
{
  "success": false,
  "message": "Khong tim thay co so dang hoat dong",
  "data": 400
}
```

## 7. FR-CRT-03 - Xem Chi Tiet San

Tao request:

```text
Name: Court - Get Detail
Method: GET
URL: {{baseUrl}}/api/courts/{{courtId}}
```

Khong can header.

Bam `Send`.

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Lay chi tiet san thanh cong",
  "data": {
    "id": 1,
    "venueId": 1,
    "venueName": "Sai Gon Central Badminton",
    "courtCode": "C01",
    "name": "Court 1",
    "description": "San cau long trong nha",
    "pricePerHour": 120000.00,
    "status": "AVAILABLE",
    "imageUrl": null
  }
}
```

## 8. FR-CRT-04 - Kiem Tra Trang Thai San

Tao request:

```text
Name: Court - Get Status
Method: GET
URL: {{baseUrl}}/api/courts/{{courtId}}/status
```

Khong can header.

Bam `Send`.

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Lay trang thai san thanh cong",
  "data": {
    "id": 1,
    "courtCode": "C01",
    "name": "Court 1",
    "status": "AVAILABLE"
  }
}
```

Status hop le:

```text
AVAILABLE
MAINTENANCE
INACTIVE
```

## 9. FR-CRT-10 - Tim Kiem San

Tao request:

```text
Name: Court - Search
Method: GET
URL: {{baseUrl}}/api/courts/search
```

Tab `Params`:

| Key | Value |
| --- | --- |
| `keyword` | `C01` |

Khong can header.

Bam `Send`.

Postman se goi URL tuong duong:

```text
{{baseUrl}}/api/courts/search?keyword=C01
```

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Tim kiem san thanh cong",
  "data": [
    {
      "id": 1,
      "venueId": 1,
      "venueName": "Sai Gon Central Badminton",
      "courtCode": "C01",
      "name": "Court 1",
      "description": "San cau long trong nha",
      "pricePerHour": 120000.00,
      "status": "AVAILABLE",
      "imageUrl": null
    }
  ]
}
```

Luu y:

- `keyword` tim theo ten san, ma san hoac ten co so.
- `keyword` phai co it nhat 2 ky tu.

## 10. FR-CRT-11 - Loc San Theo Muc Gia

Tao request:

```text
Name: Court - Filter By Price
Method: GET
URL: {{baseUrl}}/api/courts/price-range
```

Tab `Params`:

| Key | Value |
| --- | --- |
| `minPrice` | `100000` |
| `maxPrice` | `200000` |

Khong can header.

Bam `Send`.

Postman se goi URL tuong duong:

```text
{{baseUrl}}/api/courts/price-range?minPrice=100000&maxPrice=200000
```

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Loc san theo gia thanh cong",
  "data": [
    {
      "id": 1,
      "venueId": 1,
      "venueName": "Sai Gon Central Badminton",
      "courtCode": "C01",
      "name": "Court 1",
      "description": "San cau long trong nha",
      "pricePerHour": 120000.00,
      "status": "AVAILABLE",
      "imageUrl": null
    }
  ]
}
```

Luu y:

- Co the chi truyen `minPrice`.
- Co the chi truyen `maxPrice`.
- Neu khong truyen ca hai, API tra tat ca san theo khoang gia mac dinh.
- `maxPrice` phai lon hon hoac bang `minPrice`.

## 11. FR-CRT-12 - Kiem Tra San Trong

Tao request:

```text
Name: Court - Check Availability
Method: GET
URL: {{baseUrl}}/api/courts/{{courtId}}/availability
```

Tab `Params`:

| Key | Value |
| --- | --- |
| `bookingDate` | `2026-09-14` |
| `startTime` | `18:00:00` |
| `endTime` | `19:00:00` |

Khong can header.

Bam `Send`.

Postman se goi URL tuong duong:

```text
{{baseUrl}}/api/courts/{{courtId}}/availability?bookingDate=2026-09-14&startTime=18:00:00&endTime=19:00:00
```

Ket qua mong doi neu san trong:

```json
{
  "success": true,
  "message": "Kiem tra san trong thanh cong",
  "data": {
    "courtId": 1,
    "courtCode": "C01",
    "name": "Court 1",
    "status": "AVAILABLE",
    "bookingDate": "2026-09-14",
    "startTime": "18:00:00",
    "endTime": "19:00:00",
    "available": true,
    "reason": "San trong"
  }
}
```

Ket qua mong doi neu san da co booking trung gio:

```json
{
  "success": true,
  "message": "Kiem tra san trong thanh cong",
  "data": {
    "courtId": 1,
    "courtCode": "C01",
    "name": "Court 1",
    "status": "AVAILABLE",
    "bookingDate": "2026-09-14",
    "startTime": "18:00:00",
    "endTime": "19:00:00",
    "available": false,
    "reason": "San da co booking trung thoi gian"
  }
}
```

Ket qua mong doi neu san dang bao tri hoac inactive:

```json
{
  "success": true,
  "message": "Kiem tra san trong thanh cong",
  "data": {
    "courtId": 1,
    "courtCode": "C01",
    "name": "Court 1",
    "status": "MAINTENANCE",
    "bookingDate": "2026-09-14",
    "startTime": "18:00:00",
    "endTime": "19:00:00",
    "available": false,
    "reason": "San khong o trang thai AVAILABLE"
  }
}
```

## 12. FR-CRT-05 - Them San

Request nay can token cua user co permission:

```text
court:write
```

Tao request:

```text
Name: Court - Create
Method: POST
URL: {{baseUrl}}/api/courts
```

Tab `Headers`:

| Key | Value |
| --- | --- |
| `Authorization` | `Bearer {{accessToken}}` |
| `Content-Type` | `application/json` |

Tab `Body`: chon `raw` va `JSON`, sau do nhap:

```json
{
  "venueId": 1,
  "courtCode": "C99",
  "name": "Court Test 99",
  "description": "San test tao tu Postman",
  "pricePerHour": 150000,
  "status": "AVAILABLE",
  "imageUrl": "https://example.com/court-99.jpg"
}
```

Bam `Send`.

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Tao san thanh cong",
  "data": {
    "id": 99,
    "venueId": 1,
    "venueName": "Sai Gon Central Badminton",
    "courtCode": "C99",
    "name": "Court Test 99",
    "description": "San test tao tu Postman",
    "pricePerHour": 150000,
    "status": "AVAILABLE",
    "imageUrl": "https://example.com/court-99.jpg"
  }
}
```

Copy `data.id` vao environment variable:

```text
createdCourtId
```

Co the dung tab `Tests` cua Postman de tu dong luu id:

```javascript
const json = pm.response.json();
if (json.data && json.data.id) {
  pm.environment.set("createdCourtId", json.data.id);
}
```

## 13. FR-CRT-06 - Cap Nhat San

Request nay can token cua user co permission:

```text
court:write
```

Tao request:

```text
Name: Court - Update
Method: PUT
URL: {{baseUrl}}/api/courts/{{createdCourtId}}
```

Tab `Headers`:

| Key | Value |
| --- | --- |
| `Authorization` | `Bearer {{accessToken}}` |
| `Content-Type` | `application/json` |

Tab `Body`: chon `raw` va `JSON`, sau do nhap:

```json
{
  "venueId": 1,
  "courtCode": "C99",
  "name": "Court Test 99 Updated",
  "description": "San test da cap nhat",
  "pricePerHour": 180000,
  "status": "AVAILABLE",
  "imageUrl": "https://example.com/court-99-updated.jpg"
}
```

Bam `Send`.

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Cap nhat san thanh cong",
  "data": {
    "id": 99,
    "venueId": 1,
    "venueName": "Sai Gon Central Badminton",
    "courtCode": "C99",
    "name": "Court Test 99 Updated",
    "description": "San test da cap nhat",
    "pricePerHour": 180000,
    "status": "AVAILABLE",
    "imageUrl": "https://example.com/court-99-updated.jpg"
  }
}
```

## 14. FR-CRT-08 - Dua San Vao Bao Tri

Request nay can token cua user co permission:

```text
court:write
```

Tao request:

```text
Name: Court - Maintenance
Method: PATCH
URL: {{baseUrl}}/api/courts/{{createdCourtId}}/maintenance
```

Tab `Headers`:

| Key | Value |
| --- | --- |
| `Authorization` | `Bearer {{accessToken}}` |

Bam `Send`.

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Dua san vao bao tri thanh cong",
  "data": {
    "id": 99,
    "status": "MAINTENANCE"
  }
}
```

Sau do co the goi:

```text
GET {{baseUrl}}/api/courts/{{createdCourtId}}/status
```

de kiem tra status da la `MAINTENANCE`.

## 15. FR-CRT-09 - Mo Lai San

Request nay can token cua user co permission:

```text
court:write
```

Tao request:

```text
Name: Court - Reopen
Method: PATCH
URL: {{baseUrl}}/api/courts/{{createdCourtId}}/available
```

Tab `Headers`:

| Key | Value |
| --- | --- |
| `Authorization` | `Bearer {{accessToken}}` |

Bam `Send`.

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Mo lai san thanh cong",
  "data": {
    "id": 99,
    "status": "AVAILABLE"
  }
}
```

## 16. FR-CRT-07 - Ngung Hoat Dong San

Request nay can token cua user co permission:

```text
court:write
```

Tao request:

```text
Name: Court - Deactivate
Method: PATCH
URL: {{baseUrl}}/api/courts/{{createdCourtId}}/inactive
```

Tab `Headers`:

| Key | Value |
| --- | --- |
| `Authorization` | `Bearer {{accessToken}}` |

Bam `Send`.

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Ngung hoat dong san thanh cong",
  "data": {
    "id": 99,
    "status": "INACTIVE"
  }
}
```

## 17. Test Loi Validation

### Tao san thieu ten

Request:

```text
POST {{baseUrl}}/api/courts
```

Body:

```json
{
  "venueId": 1,
  "courtCode": "C100",
  "name": "",
  "description": "San loi validation",
  "pricePerHour": 150000,
  "status": "AVAILABLE",
  "imageUrl": null
}
```

Ket qua mong doi:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": [
    {
      "field": "name",
      "message": "Ten san khong duoc de trong"
    }
  ]
}
```

### Tao san trung ma trong cung co so

Dung lai `venueId` va `courtCode` da ton tai.

Ket qua mong doi:

```json
{
  "success": false,
  "message": "Ma san da ton tai trong co so nay",
  "data": 400
}
```

### Kiem tra san trong voi gio sai

Request:

```text
GET {{baseUrl}}/api/courts/{{courtId}}/availability?bookingDate=2026-09-14&startTime=19:00:00&endTime=18:00:00
```

Ket qua mong doi:

```json
{
  "success": false,
  "message": "Gio ket thuc phai sau gio bat dau",
  "data": 400
}
```

### Tim kiem keyword qua ngan

Request:

```text
GET {{baseUrl}}/api/courts/search?keyword=a
```

Ket qua mong doi:

```json
{
  "success": false,
  "message": "Tu khoa tim kiem phai co it nhat 2 ky tu",
  "data": 400
}
```

## 18. Loi Thuong Gap Khi Test Bang Postman

### 401 Unauthorized

Nguyen nhan:

- Goi API `POST`, `PUT`, `PATCH` nhung chua login.
- Chua set environment variable `accessToken`.
- Header `Authorization` sai format.
- Token het han.

Format dung:

```text
Bearer {{accessToken}}
```

### 403 Forbidden

Nguyen nhan:

- User da login nhung khong co permission `court:write`.
- Thuong gap o request `Court - Create`, `Court - Update`, `Court - Deactivate`, `Court - Maintenance`, `Court - Reopen`.

Cach xu ly:

- Dung account role `OWNER`.
- Kiem tra database co permission `court:write`.
- Kiem tra role cua user co duoc gan permission `court:write`.

### 400 Bad Request

Nguyen nhan:

- Body JSON sai.
- Thieu `venueId`, `courtCode`, `name`, `pricePerHour`.
- `pricePerHour` am.
- `courtCode` bi trung trong cung venue.
- Venue khong ton tai hoac venue da inactive.
- `endTime` khong sau `startTime`.
- `maxPrice` nho hon `minPrice`.

### 404 Not Found

Nguyen nhan:

- URL sai.
- Backend dang chay port khac `7000`.

Kiem tra lai:

```text
{{baseUrl}}
```

## 19. Checklist Test Nhanh Tren Postman

Chay theo thu tu:

```text
1. Court - Get All
2. Copy data[0].id vao courtId
3. Court - Get By Venue
4. Court - Get Detail
5. Court - Get Status
6. Court - Search
7. Court - Filter By Price
8. Court - Check Availability
9. Auth - Login bang OWNER
10. Copy data.accessToken vao accessToken
11. Court - Create
12. Copy data.id vao createdCourtId
13. Court - Update
14. Court - Maintenance
15. Court - Reopen
16. Court - Deactivate
```

## 20. Bang Tong Hop Request

| Chuc nang | Method | URL | Permission |
| --- | --- | --- | --- |
| Xem danh sach san | `GET` | `{{baseUrl}}/api/courts` | Public |
| Xem san theo co so | `GET` | `{{baseUrl}}/api/courts/venue/{{venueId}}` | Public |
| Xem chi tiet san | `GET` | `{{baseUrl}}/api/courts/{{courtId}}` | Public |
| Kiem tra trang thai san | `GET` | `{{baseUrl}}/api/courts/{{courtId}}/status` | Public |
| Tim kiem san | `GET` | `{{baseUrl}}/api/courts/search?keyword=C01` | Public |
| Loc san theo muc gia | `GET` | `{{baseUrl}}/api/courts/price-range?minPrice=100000&maxPrice=200000` | Public |
| Kiem tra san trong | `GET` | `{{baseUrl}}/api/courts/{{courtId}}/availability?bookingDate=2026-09-14&startTime=18:00:00&endTime=19:00:00` | Public |
| Them san | `POST` | `{{baseUrl}}/api/courts` | `court:write` |
| Cap nhat san | `PUT` | `{{baseUrl}}/api/courts/{{createdCourtId}}` | `court:write` |
| Ngung hoat dong san | `PATCH` | `{{baseUrl}}/api/courts/{{createdCourtId}}/inactive` | `court:write` |
| Dua san vao bao tri | `PATCH` | `{{baseUrl}}/api/courts/{{createdCourtId}}/maintenance` | `court:write` |
| Mo lai san | `PATCH` | `{{baseUrl}}/api/courts/{{createdCourtId}}/available` | `court:write` |
