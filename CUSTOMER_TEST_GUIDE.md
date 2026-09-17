# Huong Dan Test API Customers Bang Postman

Guide nay dung de test cac API customer/user profile trong Postman.

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
| `customerId` | de trong | de trong |

Sau do chon environment `DangNhap API Local` o goc phai tren Postman.

## 3. Login Lay Access Token

Tat ca API customer can dang nhap.

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
  "email": "customer@example.com",
  "password": "123456"
}
```

Neu test chuc nang tra cuu/lock/unlock, login bang account `STAFF` hoac `OWNER`.

Sau khi bam `Send`, response thanh cong se co dang:

```json
{
  "success": true,
  "message": "Dang nhap thanh cong",
  "data": {
    "accessToken": "...",
    "userId": "1",
    "email": "customer@example.com",
    "roles": [
      "CUSTOMER"
    ],
    "permissions": [
      "court:read"
    ]
  }
}
```

Copy `data.accessToken` vao environment variable:

```text
accessToken
```

Tat ca request ben duoi can header:

| Key | Value |
| --- | --- |
| `Authorization` | `Bearer {{accessToken}}` |

## 4. FR-USER-01 - Xem Ho So Ca Nhan

Tao request:

```text
Name: Customer - Get My Profile
Method: GET
URL: {{baseUrl}}/api/customers/me
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
  "message": "Lay ho so ca nhan thanh cong",
  "data": {
    "id": 1,
    "email": "customer@example.com",
    "phoneNumber": "0900000001",
    "fullName": "Nguyen Van A",
    "avatarUrl": null,
    "status": "ACTIVE",
    "roles": [
      "CUSTOMER"
    ],
    "permissions": [
      "court:read"
    ]
  }
}
```

Co the copy `data.id` vao environment variable:

```text
customerId
```

## 5. FR-USER-02 - Cap Nhat Ho So

Tao request:

```text
Name: Customer - Update My Profile
Method: PUT
URL: {{baseUrl}}/api/customers/me
```

Tab `Headers`:

| Key | Value |
| --- | --- |
| `Authorization` | `Bearer {{accessToken}}` |
| `Content-Type` | `application/json` |

Tab `Body`: chon `raw` va `JSON`, sau do nhap:

```json
{
  "fullName": "Nguyen Van A Updated",
  "phoneNumber": "0900000001",
  "avatarUrl": "https://example.com/avatar.png"
}
```

Bam `Send`.

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Cap nhat ho so thanh cong",
  "data": {
    "id": 1,
    "email": "customer@example.com",
    "phoneNumber": "0900000001",
    "fullName": "Nguyen Van A Updated",
    "avatarUrl": "https://example.com/avatar.png",
    "status": "ACTIVE"
  }
}
```

Luu y:

- `email` khong cho cap nhat tai endpoint nay.
- `phoneNumber` khong duoc trung voi user khac.

## 6. FR-USER-03 - Doi Mat Khau

Tao request:

```text
Name: Customer - Change Password
Method: PUT
URL: {{baseUrl}}/api/customers/me/password
```

Tab `Headers`:

| Key | Value |
| --- | --- |
| `Authorization` | `Bearer {{accessToken}}` |
| `Content-Type` | `application/json` |

Tab `Body`: chon `raw` va `JSON`, sau do nhap:

```json
{
  "currentPassword": "123456",
  "newPassword": "1234567",
  "confirmPassword": "1234567"
}
```

Bam `Send`.

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Doi mat khau thanh cong",
  "data": null
}
```

Sau khi doi mat khau thanh cong:

1. Login lai bang mat khau moi.
2. Copy access token moi vao environment.

Neu chi test tam thoi, co the doi nguoc lai mat khau cu:

```json
{
  "currentPassword": "1234567",
  "newPassword": "123456",
  "confirmPassword": "123456"
}
```

## 7. FR-USER-04 - Xem Lich Su Dat San

Tao request:

```text
Name: Customer - My Booking History
Method: GET
URL: {{baseUrl}}/api/customers/me/bookings
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
  "message": "Lay lich su dat san thanh cong",
  "data": [
    {
      "id": 1,
      "bookingCode": "BK001",
      "bookingDate": "2026-09-14",
      "totalAmount": 240000.00,
      "status": "CONFIRMED",
      "note": "Dat san buoi toi",
      "createdAt": "2026-09-14T10:00:00+07:00"
    }
  ]
}
```

Neu user chua co booking:

```json
{
  "success": true,
  "message": "Lay lich su dat san thanh cong",
  "data": []
}
```

## 8. FR-USER-05 - Xem Lich Su Thanh Toan

Tao request:

```text
Name: Customer - My Payment History
Method: GET
URL: {{baseUrl}}/api/customers/me/payments
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
  "message": "Lay lich su thanh toan thanh cong",
  "data": [
    {
      "id": 1,
      "bookingId": 1,
      "bookingCode": "BK001",
      "amount": 240000.00,
      "paymentMethod": "CASH",
      "paymentStatus": "PAID",
      "transactionCode": null,
      "paidAt": "2026-09-14T10:10:00+07:00",
      "createdAt": "2026-09-14T10:05:00+07:00"
    }
  ]
}
```

Neu user chua co payment:

```json
{
  "success": true,
  "message": "Lay lich su thanh toan thanh cong",
  "data": []
}
```

## 9. FR-USER-06 - Xem Lich Su Mua Hang

Tao request:

```text
Name: Customer - My Order History
Method: GET
URL: {{baseUrl}}/api/customers/me/orders
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
  "message": "Lay lich su mua hang thanh cong",
  "data": [
    {
      "id": 1,
      "orderCode": "OD001",
      "totalAmount": 500000.00,
      "status": "COMPLETED",
      "createdAt": "2026-09-14T11:00:00+07:00"
    }
  ]
}
```

Neu user chua co order:

```json
{
  "success": true,
  "message": "Lay lich su mua hang thanh cong",
  "data": []
}
```

## 10. FR-USER-07 - Tra Cuu Khach Hang

Request nay can account co permission:

```text
customer:read
```

Role `STAFF` va `OWNER` co permission nay neu database seed dung SQL moi.

Tao request:

```text
Name: Customer - Search
Method: GET
URL: {{baseUrl}}/api/customers
```

Tab `Params`:

| Key | Value |
| --- | --- |
| `keyword` | `nguyen` |

Tab `Headers`:

| Key | Value |
| --- | --- |
| `Authorization` | `Bearer {{accessToken}}` |

Bam `Send`.

Postman se goi URL tuong duong:

```text
{{baseUrl}}/api/customers?keyword=nguyen
```

Ket qua mong doi:

```json
{
  "success": true,
  "message": "Tra cuu khach hang thanh cong",
  "data": [
    {
      "id": 1,
      "email": "customer@example.com",
      "phoneNumber": "0900000001",
      "fullName": "Nguyen Van A",
      "avatarUrl": null,
      "status": "ACTIVE"
    }
  ]
}
```

Copy `data[0].id` vao environment variable:

```text
customerId
```

Luu y:

- `keyword` phai co it nhat 2 ky tu.
- API chi tra user co role `CUSTOMER`.

## 11. FR-USER-08 - Xem Thong Tin Khach Hang

Request nay can account co permission:

```text
customer:read
```

Tao request:

```text
Name: Customer - Get By Id
Method: GET
URL: {{baseUrl}}/api/customers/{{customerId}}
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
  "message": "Lay thong tin khach hang thanh cong",
  "data": {
    "id": 1,
    "email": "customer@example.com",
    "phoneNumber": "0900000001",
    "fullName": "Nguyen Van A",
    "avatarUrl": null,
    "status": "ACTIVE"
  }
}
```

## 12. FR-USER-09 - Khoa Tai Khoan

Request nay can account role:

```text
OWNER
```

Tao request:

```text
Name: Customer - Lock Account
Method: PATCH
URL: {{baseUrl}}/api/customers/{{customerId}}/lock
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
  "message": "Khoa tai khoan thanh cong",
  "data": {
    "id": 1,
    "email": "customer@example.com",
    "phoneNumber": "0900000001",
    "fullName": "Nguyen Van A",
    "avatarUrl": null,
    "status": "BLOCKED"
  }
}
```

Sau khi bi khoa, user do se khong login/khong dung token moi duoc.

## 13. FR-USER-09 - Mo Khoa Tai Khoan

Request nay can account role:

```text
OWNER
```

Tao request:

```text
Name: Customer - Unlock Account
Method: PATCH
URL: {{baseUrl}}/api/customers/{{customerId}}/unlock
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
  "message": "Mo khoa tai khoan thanh cong",
  "data": {
    "id": 1,
    "email": "customer@example.com",
    "phoneNumber": "0900000001",
    "fullName": "Nguyen Van A",
    "avatarUrl": null,
    "status": "ACTIVE"
  }
}
```

## 14. Test Loi Validation

### Cap nhat profile thieu ho ten

Request:

```text
PUT {{baseUrl}}/api/customers/me
```

Body:

```json
{
  "fullName": "",
  "phoneNumber": "0900000001",
  "avatarUrl": null
}
```

Ket qua mong doi:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": [
    {
      "field": "fullName",
      "message": "Ho ten khong duoc de trong"
    }
  ]
}
```

### Doi mat khau xac nhan khong khop

Request:

```text
PUT {{baseUrl}}/api/customers/me/password
```

Body:

```json
{
  "currentPassword": "123456",
  "newPassword": "1234567",
  "confirmPassword": "abcdef"
}
```

Ket qua mong doi:

```json
{
  "success": false,
  "message": "Xac nhan mat khau khong khop",
  "data": 400
}
```

### Tim kiem keyword qua ngan

Request:

```text
GET {{baseUrl}}/api/customers?keyword=a
```

Ket qua mong doi:

```json
{
  "success": false,
  "message": "Tu khoa tim kiem phai co it nhat 2 ky tu",
  "data": 400
}
```

## 15. Loi Thuong Gap Khi Test Bang Postman

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

- User da login nhung khong co permission can thiet.
- `GET /api/customers?keyword=...` va `GET /api/customers/{id}` can `customer:read`.
- `PATCH /api/customers/{id}/lock` va `/unlock` can role `OWNER`.

Cach xu ly:

- Dung account role `STAFF` hoac `OWNER` cho tra cuu khach hang.
- Dung account role `OWNER` cho khoa/mo khoa tai khoan.
- Kiem tra token trong Postman co phai token moi nhat khong.

### 400 Bad Request

Nguyen nhan:

- Body JSON sai.
- Thieu `fullName`, `phoneNumber`.
- Doi mat khau sai mat khau hien tai.
- Mat khau xac nhan khong khop.
- Keyword tim kiem qua ngan.
- Customer id khong ton tai hoac user do khong co role `CUSTOMER`.

### 404 Not Found

Nguyen nhan:

- URL sai.
- Backend dang chay port khac `7000`.

Kiem tra lai:

```text
{{baseUrl}}
```

## 16. Checklist Test Nhanh Tren Postman

Chay theo thu tu neu test tai khoan CUSTOMER:

```text
1. Auth - Login bang customer
2. Copy data.accessToken vao environment accessToken
3. Customer - Get My Profile
4. Customer - Update My Profile
5. Customer - My Booking History
6. Customer - My Payment History
7. Customer - My Order History
8. Customer - Change Password neu can test doi mat khau
```

Chay theo thu tu neu test tai khoan STAFF/OWNER:

```text
1. Auth - Login bang staff hoac owner
2. Copy data.accessToken vao environment accessToken
3. Customer - Search
4. Copy data[0].id vao environment customerId
5. Customer - Get By Id
6. Neu la OWNER: Customer - Lock Account
7. Neu la OWNER: Customer - Unlock Account
```

## 17. Bang Tong Hop Request

| Chuc nang | Method | URL | Permission |
| --- | --- | --- | --- |
| Xem ho so ca nhan | `GET` | `{{baseUrl}}/api/customers/me` | Dang nhap |
| Cap nhat ho so | `PUT` | `{{baseUrl}}/api/customers/me` | Dang nhap |
| Doi mat khau | `PUT` | `{{baseUrl}}/api/customers/me/password` | Dang nhap |
| Xem lich su dat san | `GET` | `{{baseUrl}}/api/customers/me/bookings` | Dang nhap |
| Xem lich su thanh toan | `GET` | `{{baseUrl}}/api/customers/me/payments` | Dang nhap |
| Xem lich su mua hang | `GET` | `{{baseUrl}}/api/customers/me/orders` | Dang nhap |
| Tra cuu khach hang | `GET` | `{{baseUrl}}/api/customers?keyword=nguyen` | `customer:read` |
| Xem thong tin khach hang | `GET` | `{{baseUrl}}/api/customers/{{customerId}}` | `customer:read` |
| Khoa tai khoan | `PATCH` | `{{baseUrl}}/api/customers/{{customerId}}/lock` | `ROLE_OWNER` |
| Mo khoa tai khoan | `PATCH` | `{{baseUrl}}/api/customers/{{customerId}}/unlock` | `ROLE_OWNER` |
