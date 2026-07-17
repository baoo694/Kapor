# Topics Management CRUD Implementation Plan

Mục tiêu: Xây dựng chức năng quản lý Topics (Thêm, Sửa, Xóa, Xem danh sách) trên giao diện Admin, liên kết với dữ liệu thật từ Backend (MongoDB) và bám sát cấu trúc của entity `Topic` theo thiết kế dự án.

## User Review Required

> [!IMPORTANT]
> - Các endpoints mới cho Admin sẽ được đặt tại `/api/admin/topics`. Bạn xác nhận format này đúng với convention của project không?
> - Ở frontend `App.tsx`, hiện tại bạn đang viết gộp cả trang trong 1 file lớn. Việc thêm logic call API và state management cho CRUD sẽ làm file này dài thêm một chút. Tôi sẽ bổ sung trực tiếp vào `App.tsx` nhưng bạn nên lưu ý refactor tách component trong tương lai.

## Proposed Changes

### Backend (`kapor-backend`)

#### [NEW] `com/kapor/admin/dto/TopicDto.java`
- Tạo DTO chứa các field: `id`, `domain`, `title`, `titleVi`, `description`, `order`, `prerequisiteTopicIds`, `tags`, `isActive`. DTO này dùng chung cho Create/Update.

#### [NEW] `com/kapor/admin/service/AdminTopicService.java`
- Chứa các logic:
  - Lấy tất cả topics (`findAll`) hoặc filter theo domain.
  - Thêm mới một Topic (`save`).
  - Cập nhật Topic theo ID (`update`).
  - Xóa Topic theo ID (`deleteById`).

#### [NEW] `com/kapor/admin/controller/AdminTopicController.java`
- Định nghĩa các REST endpoints:
  - `GET /api/admin/topics`: Trả về danh sách topics.
  - `POST /api/admin/topics`: Nhận dữ liệu `TopicDto`, tạo topic mới.
  - `PUT /api/admin/topics/{id}`: Cập nhật topic.
  - `DELETE /api/admin/topics/{id}`: Xóa topic.
- Controller sẽ yêu cầu quyền admin (dựa trên cấu hình security hiện tại của `/api/admin/**`).

### Frontend (`kapor-admin`)

#### [MODIFY] `src/services/api.ts`
- Bổ sung các function giao tiếp với API backend:
  - `getAdminTopics()`
  - `createAdminTopic(data: any)`
  - `updateAdminTopic(id: string, data: any)`
  - `deleteAdminTopic(id: string)`

#### [MODIFY] `src/App.tsx`
- **State Management**: Thay thế biến hằng `adminTopics` bằng `useState<any[]>([])`. Bổ sung `useEffect` gọi `getAdminTopics()` khi vào màn hình "Topics".
- **Add/Edit Topic**: Tái sử dụng form `showAddTopic`, bổ sung logic map đúng các field: `title`, `titleVi`, `domain`, `description`, `order`.
  - Thay đổi nút save để gọi API `createAdminTopic` hoặc `updateAdminTopic`.
- **List View**: Cập nhật lại table Topics để render từ state, có các nút **Edit** và **Delete**.
  - **Delete**: Khi nhấn xóa sẽ gọi `deleteAdminTopic` và fetch lại danh sách.

## Verification Plan

### Manual Verification
- Truy cập vào trang web kapor-admin (localhost).
- Vào tab "Topics". Đảm bảo không còn dữ liệu mock.
- Nhấn "New Topic", điền dữ liệu giả định và lưu. Kiểm tra xem dữ liệu có hiện lên bảng không và kiểm tra database (MongoDB compass) xem bản ghi được lưu thành công không.
- Nhấn "Edit" trên một topic, sửa tên và lưu. Kiểm tra thay đổi.
- Nhấn "Delete" và xác nhận bản ghi đã mất khỏi danh sách.
