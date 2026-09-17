# Huong Dan Test API Venues Bang Postman

Guide nay dung de test cac API venue trong Postman.

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
| `createdVenueId` | de trong | de trong |

Sau do chon environment `DangNhap API Local` o goc phai tren Postman.

## 3. Login Lay Access Token

API `GET /api/venues/**` dung de xem du lieu venue la public, khong bat buoc dang nhap.

Ban chi can login khi test cac API ghi du lieu:

```text
POST /api/venues
PUT /api/venues/{id}
PATCH /api/venues/{id}/inactive
```

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

Sau khi bam `Send`, response thanh cong se co dang:

```json
{
  "success": true,
  "message": "Dang nhap thanh cong",
  "data": {
    "accessToken": "..."
  }
}
```

Copy `data.accessToken` vao environment variable:

```text
accessToken
```

Voi cac request them/sua/ngung hoat dong, them header:

| Key | Value |
| --- | --- |
| `Authorization` | `Bearer {{accessToken}}` |

Luu y:

- API xem danh sach, chi tiet, vi tri, gan day, san thuoc co so khong can token.
- API them, sua, ngung hoat dong can permission `venue:write`.
- Role `OWNER` co toan bo permission neu database da seed dung SQL moi.

## 4. FR-VEN-01 - Xem Danh Sach Co So

Tao request:

```text
Name: Venue - Get Active Venues
Method: GET
URL: {{baseUrl}}/api/venues
```

Tab `Headers`:

Khong bat buoc header nao.

Bam `Send`.

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Lay danh sach co so thanh cong",
  "data": [
    {
      "id": 1,
      "name": "Sai Gon Central Badminton",
      "address": "123 Nguyen Thi Minh Khai, Phuong Ben Thanh, Quan 1, TP.HCM",
      "phoneNumber": "0901234567",
      "latitude": 10.7768890,
      "longitude": 106.6953120,
      "openingTime": "05:00:00",
      "closingTime": "23:00:00",
      "status": "ACTIVE"
    }
  ]
}
```

Ghi lai `id` cua venue muon test tiep vao environment variable:

```text
venueId
```

## 5. FR-VEN-02 - Xem Chi Tiet Co So

Tao request:

```text
Name: Venue - Get Detail
Method: GET
URL: {{baseUrl}}/api/venues/{{venueId}}
```

Tab `Headers`:

Khong bat buoc header nao.

Bam `Send`.

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Lay chi tiet co so thanh cong",
  "data": {
    "id": 1,
    "name": "Sai Gon Central Badminton",
    "address": "123 Nguyen Thi Minh Khai, Phuong Ben Thanh, Quan 1, TP.HCM",
    "phoneNumber": "0901234567",
    "latitude": 10.7768890,
    "longitude": 106.6953120,
    "openingTime": "05:00:00",
    "closingTime": "23:00:00",
    "status": "ACTIVE"
  }
}
```

## 6. FR-VEN-03 - Xem Vi Tri Co So

Tao request:

```text
Name: Venue - Get Location
Method: GET
URL: {{baseUrl}}/api/venues/{{venueId}}/location
```

Tab `Headers`:

Khong bat buoc header nao.

Bam `Send`.

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Lay vi tri co so thanh cong",
  "data": {
    "id": 1,
    "name": "Sai Gon Central Badminton",
    "address": "123 Nguyen Thi Minh Khai, Phuong Ben Thanh, Quan 1, TP.HCM",
    "latitude": 10.7768890,
    "longitude": 106.6953120
  }
}
```

## 7. FR-VEN-07 - Tim Co So Gan Khach Hang

Tao request:

```text
Name: Venue - Nearby
Method: GET
URL: {{baseUrl}}/api/venues/nearby
```

Tab `Params`:

| Key | Value |
| --- | --- |
| `latitude` | `10.776889` |
| `longitude` | `106.695312` |
| `limit` | `3` |

Tab `Headers`: khong bat buoc header nao.

Bam `Send`.

Postman se goi URL tuong duong:

```text
{{baseUrl}}/api/venues/nearby?latitude=10.776889&longitude=106.695312&limit=3
```

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Lay danh sach co so gan ban thanh cong",
  "data": [
    {
      "id": 1,
      "name": "Sai Gon Central Badminton",
      "address": "123 Nguyen Thi Minh Khai, Phuong Ben Thanh, Quan 1, TP.HCM",
      "phoneNumber": "0901234567",
      "latitude": 10.7768890,
      "longitude": 106.6953120,
      "openingTime": "05:00:00",
      "closingTime": "23:00:00",
      "distanceKm": 0.0
    }
  ]
}
```

Validate:

| Field | Dieu kien |
| --- | --- |
| `latitude` | tu `-90` den `90` |
| `longitude` | tu `-180` den `180` |
| `limit` | tu `1` den `50` |

## 8. FR-VEN-08 - Xem San Thuoc Co So

Tao request:

```text
Name: Venue - Get Courts By Venue
Method: GET
URL: {{baseUrl}}/api/venues/{{venueId}}/courts
```

Tab `Headers`:

Khong bat buoc header nao.

Bam `Send`.

Ket qua mong doi neu venue co san:

```json
{
  "success": true,
  "message": "Lay danh sach san thuoc co so thanh cong",
  "data": [
    {
      "id": 1,
      "venueId": 1,
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

Ket qua mong doi neu venue chua co san:

```json
{
  "success": true,
  "message": "Lay danh sach san thuoc co so thanh cong",
  "data": []
}
```

## 9. FR-VEN-04 - Them Co So

Request nay can token cua user co permission:

```text
venue:write
```

Tao request:

```text
Name: Venue - Create
Method: POST
URL: {{baseUrl}}/api/venues
```

Tab `Headers`:

| Key | Value |
| --- | --- |
| `Authorization` | `Bearer {{accessToken}}` |
| `Content-Type` | `application/json` |

Tab `Body`: chon `raw` va `JSON`, sau do nhap:

```json
{
  "name": "Go Vap Badminton Center",
  "address": "12 Phan Van Tri, Go Vap, TP.HCM",
  "phoneNumber": "0933445566",
  "latitude": 10.8380000,
  "longitude": 106.6710000,
  "openingTime": "05:30:00",
  "closingTime": "22:30:00"
}
```

Bam `Send`.

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Tao co so thanh cong",
  "data": {
    "id": 4,
    "name": "Go Vap Badminton Center",
    "address": "12 Phan Van Tri, Go Vap, TP.HCM",
    "phoneNumber": "0933445566",
    "latitude": 10.8380000,
    "longitude": 106.6710000,
    "openingTime": "05:30:00",
    "closingTime": "22:30:00",
    "status": "ACTIVE"
  }
}
```

Copy `data.id` vao environment variable:

```text
createdVenueId
```

Co the dung tab `Tests` cua Postman de tu dong luu id:

```javascript
const json = pm.response.json();
if (json.data && json.data.id) {
  pm.environment.set("createdVenueId", json.data.id);
}
```

## 10. FR-VEN-05 - Cap Nhat Co So

Request nay can token cua user co permission:

```text
venue:write
```

Tao request:

```text
Name: Venue - Update
Method: PUT
URL: {{baseUrl}}/api/venues/{{createdVenueId}}
```

Tab `Headers`:

| Key | Value |
| --- | --- |
| `Authorization` | `Bearer {{accessToken}}` |
| `Content-Type` | `application/json` |

Tab `Body`: chon `raw` va `JSON`, sau do nhap:

```json
{
  "name": "Go Vap Badminton Center Updated",
  "address": "99 Quang Trung, Go Vap, TP.HCM",
  "phoneNumber": "0933445566",
  "latitude": 10.8400000,
  "longitude": 106.6720000,
  "openingTime": "06:00:00",
  "closingTime": "23:00:00"
}
```

Bam `Send`.

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Cap nhat co so thanh cong",
  "data": {
    "id": 4,
    "name": "Go Vap Badminton Center Updated",
    "address": "99 Quang Trung, Go Vap, TP.HCM",
    "phoneNumber": "0933445566",
    "latitude": 10.8400000,
    "longitude": 106.6720000,
    "openingTime": "06:00:00",
    "closingTime": "23:00:00",
    "status": "ACTIVE"
  }
}
```

## 11. FR-VEN-06 - Ngung Hoat Dong Co So

Request nay can token cua user co permission:

```text
venue:write
```

Tao request:

```text
Name: Venue - Deactivate
Method: PATCH
URL: {{baseUrl}}/api/venues/{{createdVenueId}}/inactive
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
  "message": "Ngung hoat dong co so thanh cong",
  "data": {
    "id": 4,
    "name": "Go Vap Badminton Center Updated",
    "address": "99 Quang Trung, Go Vap, TP.HCM",
    "phoneNumber": "0933445566",
    "latitude": 10.8400000,
    "longitude": 106.6720000,
    "openingTime": "06:00:00",
    "closingTime": "23:00:00",
    "status": "INACTIVE"
  }
}
```

Sau khi inactive, venue se khong con xuat hien trong danh sach active:

```text
GET {{baseUrl}}/api/venues
```

Neu goi lai:

```text
GET {{baseUrl}}/api/venues/{{createdVenueId}}
```

Co the nhan loi:

```json
{
  "success": false,
  "message": "Khong tim thay co so dang hoat dong",
  "data": 400
}
```

## 12. Test Loi Validation

### Thieu name

Dung request `Venue - Create`, body:

```json
{
  "name": "",
  "address": "12 Phan Van Tri, Go Vap, TP.HCM",
  "phoneNumber": "0933445566",
  "latitude": 10.8380000,
  "longitude": 106.6710000,
  "openingTime": "05:30:00",
  "closingTime": "22:30:00"
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
      "message": "Ten co so khong duoc de trong"
    }
  ]
}
```

### Gio dong cua khong hop le

Body:

```json
{
  "name": "Test Venue",
  "address": "Test Address",
  "phoneNumber": "0900000000",
  "latitude": 10.8380000,
  "longitude": 106.6710000,
  "openingTime": "22:30:00",
  "closingTime": "05:30:00"
}
```

Ket qua mong doi:

```json
{
  "success": false,
  "message": "Gio dong cua phai sau gio mo cua",
  "data": 400
}
```

### Toa do khong hop le

Voi request `Venue - Nearby`, nhap params:

| Key | Value |
| --- | --- |
| `latitude` | `100` |
| `longitude` | `106.695312` |
| `limit` | `3` |

Ket qua mong doi la response loi validation `400`.

## 13. Loi Thuong Gap Khi Test Bang Postman

### 401 Unauthorized

Nguyen nhan:

- Chua login.
- Chua set environment variable `accessToken`.
- Header `Authorization` sai format.
- Token het han.

Format dung:

```text
Bearer {{accessToken}}
```

### 403 Forbidden

Nguyen nhan:

- User da login nhung khong co permission `venue:write`.
- Thuong gap o request `Venue - Create`, `Venue - Update`, `Venue - Deactivate`.

Cach xu ly:

- Dung account role `OWNER`.
- Kiem tra database co permission `venue:write`.
- Kiem tra role cua user co duoc gan permission `venue:write`.

### 400 Bad Request

Nguyen nhan:

- Body JSON sai.
- Thieu `name` hoac `address`.
- `closingTime` khong sau `openingTime`.
- `latitude`, `longitude`, `limit` ngoai khoang hop le.
- Venue id khong ton tai hoac venue da inactive.

### 404 Not Found

Nguyen nhan:

- URL sai.
- Backend dang chay port khac `7000`.

Kiem tra lai:

```text
{{baseUrl}}
```

## 14. Checklist Test Nhanh Tren Postman

Chay theo thu tu:

```text
1. Venue - Get Active Venues
2. Copy id cua venue vao environment venueId
3. Venue - Get Detail
4. Venue - Get Location
5. Venue - Nearby
6. Venue - Get Courts By Venue
7. Auth - Login bang account OWNER
8. Copy data.accessToken vao environment accessToken
9. Venue - Create
10. Copy data.id vao environment createdVenueId
11. Venue - Update
12. Venue - Deactivate
13. Venue - Get Active Venues de kiem tra venue inactive da bien mat
```

## 15. Bang Tong Hop Request

| Chuc nang | Method | URL | Permission |
| --- | --- | --- | --- |
| Xem danh sach co so | `GET` | `{{baseUrl}}/api/venues` | Public |
| Xem chi tiet co so | `GET` | `{{baseUrl}}/api/venues/{{venueId}}` | Public |
| Xem vi tri co so | `GET` | `{{baseUrl}}/api/venues/{{venueId}}/location` | Public |
| Tim co so gan khach hang | `GET` | `{{baseUrl}}/api/venues/nearby` | Public |
| Xem san thuoc co so | `GET` | `{{baseUrl}}/api/venues/{{venueId}}/courts` | Public |
| Them co so | `POST` | `{{baseUrl}}/api/venues` | `venue:write` |
| Cap nhat co so | `PUT` | `{{baseUrl}}/api/venues/{{createdVenueId}}` | `venue:write` |
| Ngung hoat dong co so | `PATCH` | `{{baseUrl}}/api/venues/{{createdVenueId}}/inactive` | `venue:write` |
