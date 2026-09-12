# DangNhap API

API dang ky, dang nhap, JWT, refresh token, RBAC va quen mat khau bang Spring Boot + PostgreSQL. Du an nay co the dung lam module xac thuc rieng cho cac he thong khac, vi du du an quan li san/cau long.

## 1. API Nay Dung De Lam Gi

DangNhap API chi nen phu trach cac viec lien quan den tai khoan va phan quyen:

- Dang ky tai khoan.
- Dang nhap va tra ve access token + refresh token.
- Lay thong tin user hien tai qua `/api/auth/me`.
- Lam moi token qua `/api/auth/refresh-token`.
- Dang xuat mot thiet bi hoac tat ca thiet bi.
- Quen mat khau va dat lai mat khau.
- Luu user, roles, permissions va refresh tokens.

Du an quan li cau long cua ban nen phu trach nghiep vu rieng:

- Quan li san cau long.
- Quan li khung gio.
- Quan li dat san.
- Quan li thanh toan.
- Quan li lich choi, lich thue san, khach hang, nhan vien.

Hai phan nay noi voi nhau bang JWT. App quan li cau long goi DangNhap API de dang nhap, sau do gui access token khi goi cac API nghiep vu.

## 2. Cau Truc Can Tuan Thu Khi Tich Hop

Khuyen nghi chia he thong thanh 2 nhom API:

```text
auth-api
  - /api/auth/register
  - /api/auth/login
  - /api/auth/me
  - /api/auth/refresh-token
  - /api/auth/logout
  - /api/auth/logout-all
  - /api/auth/forgot-password
  - /api/auth/reset-password

badminton-management-api
  - /api/courts
  - /api/time-slots
  - /api/bookings
  - /api/payments
  - /api/customers
  - /api/staff
  - /api/admin/...
```

Moi request den API nghiep vu can gui header:

```http
Authorization: Bearer <accessToken>
```

API nghiep vu khong nen tu xu ly password. No chi nen:

1. Doc JWT tu header `Authorization`.
2. Xac thuc chu ky JWT bang cung `JWT_SECRET`.
3. Lay `userId`, `email`, `roles`, `permissions` tu token hoac goi `/api/auth/me`.
4. Cho phep/tu choi request dua tren role/permission.

## 3. Yeu Cau Moi Truong

Can cai:

- Java 21 tro len.
- PostgreSQL.
- Maven wrapper da co san trong project.
- SMTP Gmail neu muon dung quen mat khau.

Khong can H2 hoac MySQL. Project hien chi dung PostgreSQL.

## 4. Cau Hinh Database PostgreSQL

Tao database rieng cho auth:

```sql
CREATE DATABASE dangnhap_auth_api;
```

Neu dang dung database local nhu hien tai:

```text
jdbc:postgresql://localhost:42189/test_auth
```

thi chi can dam bao PostgreSQL dang chay va user/password dung.

## 5. Cau Hinh File `.env`

Tao file `.env` trong thu muc goc project:

```text
D:\BackEnd\DangNhap_API\.env
```

Vi du:

```properties
APP_PROFILE=postgres
SERVER_PORT=7000

DB_URL=jdbc:postgresql://localhost:5432/dangnhap_auth_api
DB_USERNAME=postgres
DB_PASSWORD=123456

JWT_SECRET=replace_with_at_least_32_characters_secret_key
JWT_ACCESS_TOKEN_EXPIRATION=900000
JWT_REFRESH_TOKEN_EXPIRATION=2592000000

CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

AUTH_DEFAULT_ROLE=CUSTOMER
AUTH_ALLOWED_ROLES=CUSTOMER,STAFF,OWNER

FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=false

MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_gmail_app_password
```

Giai thich bien quan trong:

- `APP_PROFILE=postgres`: bat cau hinh PostgreSQL.
- `SERVER_PORT=7000`: cong chay auth API.
- `DB_URL`: JDBC URL cua PostgreSQL, khong them `createDatabaseIfNotExist`.
- `JWT_SECRET`: khoa ky JWT, phai dai it nhat 32 ky tu.
- `JWT_ACCESS_TOKEN_EXPIRATION=900000`: access token song 15 phut.
- `JWT_REFRESH_TOKEN_EXPIRATION=2592000000`: refresh token song 30 ngay.
- `CORS_ALLOWED_ORIGINS`: domain frontend duoc phep goi API.
- `AUTH_DEFAULT_ROLE`: role mac dinh khi user dang ky ma khong gui role.
- `AUTH_ALLOWED_ROLES`: danh sach role duoc phep dang ky qua API.

Voi du an quan li cau long, nen dung roles nhu sau:

```properties
AUTH_DEFAULT_ROLE=CUSTOMER
AUTH_ALLOWED_ROLES=CUSTOMER,STAFF,OWNER
```

Project hien co migration `V1__create_badminton_auth_schema.sql` tao moi toan bo schema auth va seed san 3 actor nay.

## 6. Chay API

Tai thu muc project:

```powershell
cd D:\BackEnd\DangNhap_API
.\mvnw.cmd spring-boot:run
```

API chay tai:

```text
http://localhost:7000
```

Kiem tra nhanh:

```http
GET http://localhost:7000/api/auth/me
```

Neu chua gui token, API se tra loi unauthorized. Nhu vay la security dang hoat dong.

## 7. Chay Test

```powershell
.\mvnw.cmd test
```

Ket qua mong doi:

```text
Tests run: 6, Failures: 0, Errors: 0
BUILD SUCCESS
```

## 8. Cau Truc Database Auth

Flyway migration nam o:

```text
src/main/resources/db/migration/postgres/
```

Migration hien tai:

- `V1__create_badminton_auth_schema.sql`: tao moi toan bo schema auth, refresh token, RBAC va seed 3 actor `CUSTOMER`, `STAFF`, `OWNER`.

Bang chinh:

- `users`: thong tin tai khoan.
- `roles`: danh sach vai tro.
- `permissions`: danh sach quyen chi tiet.
- `user_roles`: gan user voi role.
- `role_permissions`: gan role voi permission.
- `refresh_tokens`: luu refresh token da hash.

Khong luu raw refresh token trong database. API chi luu `token_hash`.

## 9. Actor Cho Du An Quan Li Cau Long

V1 hien tai da seed san 3 actor:

```text
CUSTOMER
STAFF
OWNER
```

Phan role trong V1:

```sql
INSERT INTO roles (name, description) VALUES
    ('CUSTOMER', 'Khach hang dat san cau long'),
    ('STAFF', 'Nhan vien van hanh san cau long'),
    ('OWNER', 'Chu san cau long');
```

Permissions trong V1:

```sql
INSERT INTO permissions (name, description) VALUES
    ('court:read', 'Xem danh sach san'),
    ('court:write', 'Them sua xoa san'),
    ('time-slot:read', 'Xem khung gio san'),
    ('time-slot:write', 'Them sua xoa khung gio san'),
    ('booking:read', 'Xem lich dat san'),
    ('booking:create', 'Tao don dat san'),
    ('booking:update', 'Cap nhat don dat san'),
    ('booking:cancel', 'Huy don dat san'),
    ('payment:read', 'Xem thanh toan'),
    ('payment:write', 'Tao hoac cap nhat thanh toan'),
    ('customer:read', 'Xem thong tin khach hang'),
    ('staff:manage', 'Quan li nhan vien'),
    ('report:read', 'Xem bao cao doanh thu va hoat dong');
```

Permission duoc gan theo logic:

- `CUSTOMER`: xem san, xem khung gio, tao/huy booking, xem thanh toan cua minh.
- `STAFF`: xem san/khung gio, xem/tao/cap nhat/huy booking, xu ly thanh toan, xem thong tin khach.
- `OWNER`: co tat ca permissions.

Neu sau nay muon them actor moi, vi du `COACH`, hay tao migration moi `V2__add_coach_role.sql`. Khong sua `V1` neu database da deploy that.

Vi du them role moi trong V2:

```sql
INSERT INTO roles (name, description)
VALUES ('COACH', 'Huan luyen vien cau long')
ON CONFLICT DO NOTHING;
```

## 10. Flow Dang Ky Cho App Quan Li Cau Long

Frontend goi:

```http
POST /api/auth/register
Content-Type: application/json
```

Body:

```json
{
  "email": "khachhang@example.com",
  "password": "123456",
  "phoneNumber": "0912345678",
  "fullName": "Nguyen Van A",
  "role": "CUSTOMER"
}
```

Quy tac:

- `email` bat buoc dung dinh dang email.
- `password` toi thieu 6 ky tu.
- `phoneNumber` gom 10 den 11 chu so.
- `fullName` khong duoc rong.
- `role` co the bo trong, API se dung `AUTH_DEFAULT_ROLE`.
- Role gui len phai nam trong `AUTH_ALLOWED_ROLES`.

Response thanh cong:

```json
{
  "success": true,
  "message": "Dang ky thanh cong",
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "opaque-refresh-token",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "userId": "1",
    "email": "khachhang@example.com",
    "fullName": "Nguyen Van A",
    "roles": ["CUSTOMER"],
    "permissions": ["court:read", "booking:create", "booking:cancel"]
  }
}
```

Sau khi dang ky thanh cong, frontend nen luu:

- `accessToken`: dung goi API.
- `refreshToken`: dung xin token moi.
- `userId`, `email`, `fullName`, `roles`, `permissions`: dung hien thi UI va an/hien menu.

## 11. Flow Dang Nhap

```http
POST /api/auth/login
Content-Type: application/json
```

Body:

```json
{
  "email": "khachhang@example.com",
  "password": "123456"
}
```

Frontend nhan `accessToken` va `refreshToken`, sau do moi request den API quan li cau long phai gui:

```http
Authorization: Bearer jwt-access-token
```

## 12. Flow Lay User Hien Tai

Dung de reload trang ma van giu trang thai dang nhap.

```http
GET /api/auth/me
Authorization: Bearer jwt-access-token
```

Response:

```json
{
  "success": true,
  "message": "Lay thong tin nguoi dung thanh cong",
  "data": {
    "userId": "1",
    "email": "khachhang@example.com",
    "fullName": "Nguyen Van A",
    "roles": ["CUSTOMER"],
    "permissions": ["court:read", "booking:create"]
  }
}
```

## 13. Flow Refresh Token

Khi API nghiep vu tra ve `401 Unauthorized` vi access token het han, frontend goi:

```http
POST /api/auth/refresh-token
Content-Type: application/json
```

Body:

```json
{
  "refreshToken": "opaque-refresh-token"
}
```

Thanh cong thi API tra ve cap token moi:

- `accessToken` moi.
- `refreshToken` moi.

Quan trong: refresh token duoc rotate. Sau moi lan refresh, phai thay refresh token cu bang refresh token moi.

## 14. Flow Dang Xuat

Dang xuat thiet bi hien tai:

```http
POST /api/auth/logout
Content-Type: application/json
```

Body:

```json
{
  "refreshToken": "opaque-refresh-token"
}
```

Dang xuat tat ca thiet bi:

```http
POST /api/auth/logout-all
Authorization: Bearer jwt-access-token
```

Frontend sau khi logout nen xoa `accessToken`, `refreshToken` va thong tin user.

## 15. Flow Quen Mat Khau

Gui email reset:

```http
POST /api/auth/forgot-password
Content-Type: application/json
```

Body:

```json
{
  "email": "khachhang@example.com"
}
```

Dat lai mat khau:

```http
POST /api/auth/reset-password
Content-Type: application/json
```

Body:

```json
{
  "token": "reset-token-from-email",
  "newPassword": "newPassword123",
  "confirmPassword": "newPassword123"
}
```

## 16. Cach App Quan Li Cau Long Nen Goi API

Tren frontend, nen co mot API client dung chung.

Pseudo code:

```javascript
async function apiFetch(url, options = {}) {
  const accessToken = localStorage.getItem("accessToken");

  const response = await fetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
      ...(options.headers || {})
    }
  });

  if (response.status !== 401) {
    return response;
  }

  const refreshToken = localStorage.getItem("refreshToken");
  const refreshResponse = await fetch("http://localhost:7000/api/auth/refresh-token", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken })
  });

  if (!refreshResponse.ok) {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    window.location.href = "/login";
    return refreshResponse;
  }

  const refreshJson = await refreshResponse.json();
  localStorage.setItem("accessToken", refreshJson.data.accessToken);
  localStorage.setItem("refreshToken", refreshJson.data.refreshToken);

  return fetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${refreshJson.data.accessToken}`,
      ...(options.headers || {})
    }
  });
}
```

## 17. Cach Bao Ve API Nghiep Vu Cau Long

Neu app quan li cau long cung la Spring Boot, no nen cau hinh JWT filter rieng va dung cung `JWT_SECRET`.

Endpoint goi y:

```text
GET    /api/courts              CUSTOMER, STAFF, OWNER
POST   /api/courts              OWNER
PUT    /api/courts/{id}         OWNER
DELETE /api/courts/{id}         OWNER

GET    /api/bookings            STAFF, OWNER
POST   /api/bookings            CUSTOMER, STAFF, OWNER
PATCH  /api/bookings/{id}       STAFF, OWNER
DELETE /api/bookings/{id}       CUSTOMER owner, STAFF, OWNER

GET    /api/payments            CUSTOMER owner, STAFF, OWNER
POST   /api/payments            STAFF, OWNER
```

Trong database nghiep vu, nen luu `auth_user_id` de lien ket voi user ben auth:

```sql
CREATE TABLE bookings (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    auth_user_id BIGINT NOT NULL,
    court_id BIGINT NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Khong nen copy password hay refresh token sang database nghiep vu.

## 18. Quy Tac Migration Bat Buoc

Flyway se bao loi checksum neu sua file migration cu da tung chay. Vi vay:

1. Khong sua `V1__create_badminton_auth_schema.sql` neu DB da migrate tren moi truong can giu data.
2. Moi thay doi schema phai tao file version moi.
3. Ten file theo mau:

```text
V2__add_customer_profile_fields.sql
V3__add_booking_permissions.sql
V4__add_coach_role.sql
```

4. Chay test sau khi them migration:

```powershell
.\mvnw.cmd test
```

Neu gap loi checksum tren DB dev local va ban chac file migration hien tai la dung:

```powershell
.\mvnw.cmd org.flywaydb:flyway-maven-plugin:12.4.0:repair "-Dflyway.url=jdbc:postgresql://localhost:42189/test_auth" "-Dflyway.user=postgres" "-Dflyway.password=12345" "-Dflyway.locations=filesystem:src/main/resources/db/migration/postgres"
```

Chi dung `repair` cho DB dev/local khi hieu ro nguyen nhan.

## 19. Checklist Tich Hop Vao Du An Quan Li Cau Long

Lam theo thu tu:

1. Chay PostgreSQL.
2. Tao database auth.
3. Tao `.env` cho DangNhap API.
4. Cau hinh `AUTH_DEFAULT_ROLE` va `AUTH_ALLOWED_ROLES` theo app cau long.
5. Kiem tra migration `V1__create_badminton_auth_schema.sql` da co san role/permission cau long.
6. Chay `.\mvnw.cmd test`.
7. Chay `.\mvnw.cmd spring-boot:run`.
8. Frontend goi `/api/auth/register` hoac `/api/auth/login`.
9. Luu `accessToken` va `refreshToken`.
10. Moi request den API quan li cau long gui `Authorization: Bearer <accessToken>`.
11. Khi gap `401`, goi `/api/auth/refresh-token`.
12. Khi logout, goi `/api/auth/logout` va xoa token o frontend.
13. Trong DB nghiep vu, chi luu `auth_user_id`, khong luu password/token.
14. Neu them bang/cot moi, tao migration version moi, khong sua migration cu.

## 20. Luu Y Bao Mat

- Access token mac dinh song 15 phut.
- Logout chi revoke refresh token. Access token cu van co the dung den khi het han.
- Refresh token da revoke neu bi dung lai se bi xem la token reuse va API se revoke cac refresh token active cua user.
- Nen dung HTTPS khi deploy that.
- Khong commit file `.env`.
- Khong log raw access token hoac refresh token.
- Nen gioi han `CORS_ALLOWED_ORIGINS` theo domain frontend that, khong dung `*` tren production.

## 21. Len Production

Khi deploy:

```properties
APP_PROFILE=postgres
SERVER_PORT=7000
DB_URL=jdbc:postgresql://<host>:5432/<database>
DB_USERNAME=<username>
DB_PASSWORD=<password>
JWT_SECRET=<strong-secret-at-least-32-characters>
CORS_ALLOWED_ORIGINS=https://your-badminton-app.com
FLYWAY_ENABLED=true
```

Can dam bao:

- PostgreSQL backup dinh ky.
- `JWT_SECRET` khong doi tuy tien, vi doi secret se lam token cu mat hieu luc.
- SMTP Gmail dung app password, khong dung mat khau Gmail chinh.
- Log khong in thong tin nhay cam.
