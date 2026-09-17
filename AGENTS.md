# Quy tắc phát triển backend

Các quy tắc dưới đây phải được thực hiện khi phát triển model/module và endpoint mới trong project này.

## 1. Trước khi bắt đầu code

Phải đọc đầy đủ hai file migration PostgreSQL sau:

- `src/main/resources/db/migration/postgres/V1__create_badminton_auth_schema.sql`
- `src/main/resources/db/migration/postgres/V2__alter_table.sql`

Mục đích:

- Hiểu cấu trúc bảng, cột, kiểu dữ liệu và giá trị mặc định.
- Kiểm tra primary key, foreign key, unique constraint, check constraint và index.
- Hiểu quan hệ giữa model đang làm với các bảng hiện có.
- Không tự suy đoán schema hoặc tạo field trái với migration.

Nếu model cần thay đổi database, phải tạo migration mới theo thứ tự phiên bản tiếp theo. Không sửa migration cũ đã được áp dụng chỉ để phù hợp với code mới.

## 2. Trong khi code model/module

- Entity/model phải khớp với schema sau khi áp dụng lần lượt V1, V2 và các migration mới hơn (nếu có).
- Kiểm tra đầy đủ các lớp liên quan: entity/model, repository, DTO/request/response, mapper, service, controller và security/permission.
- Validation trong request và business rule phải nhất quán với constraint trong database.
- Endpoint phải dùng đúng HTTP method, status code, quyền truy cập và cấu trúc response chung của project.
- Không làm thay đổi hành vi của module khác nếu yêu cầu không liên quan.
- Chạy test hoặc lệnh build phù hợp sau khi hoàn thành.

## 3. Sau khi hoàn thành một model/module

Phải tạo hoặc cập nhật một file Markdown ở thư mục gốc để hướng dẫn kiểm tra toàn bộ endpoint của model/module đó.

Quy ước tên file:

```text
<TEN_MODEL>_TEST_GUIDE.md
```

Ví dụ:

- `CUSTOMER_TEST_GUIDE.md`
- `COURT_TEST_GUIDE.md`
- `BOOKING_TEST_GUIDE.md`

File hướng dẫn kiểm tra endpoint tối thiểu phải có:

1. Mục đích và phạm vi API được kiểm tra.
2. Cách chạy backend và base URL.
3. Dữ liệu hoặc tài khoản cần chuẩn bị.
4. Cách đăng nhập, lấy token và quyền/role cần dùng.
5. Danh sách tất cả endpoint của model/module.
6. Với từng endpoint:
   - HTTP method và URL.
   - Path variable và query parameter.
   - Headers cần thiết.
   - Request body mẫu hợp lệ.
   - Response thành công mẫu và status code mong đợi.
   - Các trường hợp lỗi quan trọng cùng status code mong đợi.
7. Thứ tự test đề xuất để tránh phụ thuộc dữ liệu.
8. Checklist kết quả cuối cùng.

Ưu tiên hướng dẫn có thể thực hiện trực tiếp bằng Postman. Các ví dụ phải bám đúng DTO, validation, security và endpoint trong source code tại thời điểm hoàn thành.

## 4. Checklist hoàn thành

Một model/module chỉ được xem là hoàn thành khi:

- [ ] Đã đọc V1 và V2 trước khi code.
- [ ] Code khớp schema và quan hệ database.
- [ ] Đã kiểm tra validation và phân quyền.
- [ ] Build/test liên quan chạy thành công.
- [ ] Đã tạo hoặc cập nhật `<TEN_MODEL>_TEST_GUIDE.md`.
- [ ] Guide bao phủ cả trường hợp thành công và lỗi quan trọng của mọi endpoint.
