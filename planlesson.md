# Kế hoạch triển khai Lesson CRUD cho Admin

## Mục tiêu

Xây dựng chức năng quản lý Lesson trên giao diện Admin: tạo, xem danh sách,
sửa và xóa Lesson. Mỗi Lesson phải gắn với một Topic có thật trong MongoDB;
giao diện không còn dùng dữ liệu mock.

## Mô hình dữ liệu

`Lesson` đã có các trường cần thiết:

- `topicId`: ID Topic cha, bắt buộc.
- `title`, `titleVi`: tiêu đề Hàn và Việt.
- `content`, `contentVi`: nội dung song ngữ.
- `order`: thứ tự Lesson trong Topic.
- `vocabulary`: danh sách từ vựng.
- `exercises`: danh sách bài tập.

Tên Topic và domain không được lưu lặp lại trong Lesson. Chúng được lấy từ
Topic thông qua `topicId` để tránh dữ liệu sai lệch.

```text
Topic
└── Lesson 1
└── Lesson 2
└── Lesson 3
```

## Backend

### 1. Tạo `AdminLessonDto`

Tạo `com.kapor.admin.dto.AdminLessonDto` gồm:

- Dữ liệu lưu: `id`, `topicId`, `title`, `titleVi`, `content`, `contentVi`,
  `order`, `vocabulary`, `exercises`.
- Dữ liệu chỉ dùng để hiển thị: `topicTitle`, `topicTitleVi`, `domain`.

Validation:

- `topicId`, `title`, `titleVi` không được rỗng.
- `order` là số nguyên lớn hơn hoặc bằng 0.

### 2. Tạo `AdminLessonService`

Tạo `com.kapor.admin.service.AdminLessonService` với các chức năng:

- `findAll(topicId?)`: lấy tất cả Lesson, hoặc lọc theo Topic; map `topicId`
  sang tên và domain của Topic.
- `create(dto)`: xác nhận Topic tồn tại trước khi lưu Lesson.
- `update(id, dto)`: tìm Lesson, xác nhận Topic đích nếu `topicId` thay đổi,
  sau đó cập nhật dữ liệu.
- `deleteById(id)`: xóa Lesson và trả lỗi 404 nếu Lesson không tồn tại.

Khi vocabulary hoặc exercise không có `id`, service tạo UUID để editor có định
danh ổn định.

### 3. Tạo `AdminLessonController`

Tạo controller ở `com.kapor.admin.controller.AdminLessonController`.
Các endpoint nằm dưới `/api/admin/**`, nên cấu hình bảo mật hiện có sẽ tự yêu
cầu quyền `ROLE_ADMIN`.

| Method | Endpoint | Mục đích |
| --- | --- | --- |
| GET | `/api/admin/lessons?topicId={id}` | Lấy danh sách Lesson, có thể lọc theo Topic |
| POST | `/api/admin/lessons` | Tạo Lesson |
| PUT | `/api/admin/lessons/{id}` | Cập nhật Lesson |
| DELETE | `/api/admin/lessons/{id}` | Xóa Lesson |

API người học `/api/lessons` tiếp tục chỉ phục vụ đọc Lesson theo `topicId`.

### 4. Toàn vẹn dữ liệu

- Không cho tạo hoặc chuyển Lesson sang Topic không tồn tại.
- Khuyến nghị bổ sung chặn xóa Topic nếu Topic đó vẫn còn Lesson, nhằm tránh
  Lesson mồ côi.

## Frontend Admin

### 1. API client

Thêm các hàm vào `kapor-admin/src/services/api.ts`:

```ts
getAdminLessons(topicId?: string)
createAdminLesson(data)
updateAdminLesson(id: string, data)
deleteAdminLesson(id: string)
```

### 2. Danh sách Lessons

Trong tab **Lessons** của `kapor-admin/src/App.tsx`:

- Gọi `getAdminTopics()` để tạo dropdown Topic và map tên/domain.
- Gọi `getAdminLessons()` để hiển thị dữ liệu thật.
- Thay thế hằng `adminLessons` mock bằng state.
- Hiển thị: thứ tự, tiêu đề Hàn/Việt, Topic, domain của Topic, số vocabulary,
  số exercises và các nút Edit/Delete.
- Bổ sung trạng thái loading, empty state, lỗi và retry.

### 3. Form tạo và sửa Lesson

Form cần gồm:

- Topic: dropdown bắt buộc, lấy từ API Topics; không cho nhập domain thủ công.
- Title Korean, Title Vietnamese.
- Content Korean, Content Vietnamese.
- Order.
- Vocabulary editor: Korean, pronunciation, Vietnamese, English, context,
  code snippet, audio URL.
- Exercise editor.

Nút Save gọi POST khi tạo mới hoặc PUT khi đang sửa, sau đó tải lại danh sách.

### 4. Edit và Delete

- Edit tải Lesson thực từ API, bind đủ dữ liệu cơ bản, vocabulary và exercises.
- Delete hiển thị xác nhận, gọi API rồi tải lại danh sách.

## Giới hạn hiện tại của Exercise

Entity `Lesson.Exercise` hiện có `options` và `correctAnswer`, phù hợp với
`multiple_choice` và `fill_blank`. Mock UI có loại `matching`, nhưng entity
chưa có trường `pairs` để lưu dữ liệu này.

Trong phạm vi triển khai hiện tại, chỉ bật `multiple_choice` và `fill_blank`.
Muốn hỗ trợ Matching cần mở rộng entity, DTO và editor với `pairs` trước.

## Kiểm thử

### Backend

- Admin tạo, sửa, xóa Lesson thành công.
- GET filter đúng theo `topicId`.
- Từ chối `topicId` không tồn tại.
- Từ chối request từ user không có quyền admin.
- Kiểm tra tên Topic/domain được trả về đúng trong DTO.

### Frontend

- Build `kapor-admin` thành công.
- Tạo Topic, tạo Lesson gắn với Topic đó, và xác nhận Topic/domain hiển thị
  đúng trong bảng.
- Sửa Lesson, vocabulary và exercises; kiểm tra dữ liệu sau khi tải lại.
- Xóa Lesson và xác nhận bản ghi không còn trong MongoDB.
