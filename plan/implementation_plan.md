# Migration Plan: React to Flutter Screens

Kế hoạch này vạch ra lộ trình chi tiết để chuyển đổi 12 màn hình còn thiếu từ ứng dụng React (`App.tsx`) sang dự án Flutter mới. Quá trình chuyển đổi được chia thành 5 giai đoạn (Phases) dựa trên logic luồng người dùng và mức độ liên kết tính năng.

## User Review Required
> [!IMPORTANT]
> - Vui lòng xem xét lộ trình các Phase bên dưới. Bạn có muốn điều chỉnh thứ tự ưu tiên (ví dụ đẩy TechTalk lên trước) hay không?
> - Các màn hình này sẽ sử dụng hệ thống Design System / Theme hiện tại trong `app_theme.dart`. Bạn có muốn bổ sung thêm các màu/icon chuyên biệt từ React App sang Theme của Flutter trước khi bắt đầu không?

## Proposed Migration Phases

### Phase 1: Authentication & Onboarding (Khởi đầu)
Đây là các màn hình đầu tiên người dùng tiếp xúc, thiết lập trạng thái ban đầu của ứng dụng.
* **Các màn hình:**
  * `LoginScreen` -> `lib/features/auth/login_screen.dart`
  * `OnboardingScreen` -> `lib/features/onboarding/onboarding_screen.dart`
* **Công việc phụ trợ:**
  * Bổ sung route `/login` và `/onboarding` vào `app_router.dart`.
  * Cập nhật `initialLocation` trong `GoRouter` để kiểm tra trạng thái login.

---

### Phase 2: Core Feature Sub-screens (Chi tiết tính năng chính)
Hoàn thiện luồng sâu của các tính năng đã có màn hình ngoài (Dashboard/DevVocab/MemByte).
* **Các màn hình:**
  * `DevVocabLessonScreen` -> `lib/features/devvocab/devvocab_lesson_screen.dart`
  * `MemByteReviewScreen` -> `lib/features/membyte/membyte_review_screen.dart`
* **Công việc phụ trợ:**
  * Bổ sung các nested routes vào `app_router.dart` (ví dụ: `/devvocab/lesson`, `/membyte/review`).
  * Xử lý truyền tham số/argument giữa màn hình danh sách và màn hình chi tiết.

---

### Phase 3: TechTalk Module (Hệ thống đàm thoại)
Nhóm tính năng TechTalk có sự liên kết chặt chẽ với nhau thành một luồng (chọn -> thực hành -> kết quả).
* **Các màn hình:**
  * `TechTalkSelectScreen` -> `lib/features/techtalk/techtalk_select_screen.dart`
  * `TechTalkScreen` -> `lib/features/techtalk/techtalk_screen.dart`
  * `TechTalkResultScreen` -> `lib/features/techtalk/techtalk_result_screen.dart`
* **Công việc phụ trợ:**
  * Xây dựng UI giả lập các đoạn hội thoại chat.
  * Cấu hình các route liên hoàn `/techtalk/select`, `/techtalk/chat`, `/techtalk/result`.

---

### Phase 4: Additional Learning Modules (Các tính năng học tập khác)
Các module học tập độc lập còn lại.
* **Các màn hình:**
  * `HonorificsScreen` -> `lib/features/honorifics/honorifics_screen.dart`
  * `VideoScreen` -> `lib/features/video/video_screen.dart`
  * `PronunciationListScreen` -> `lib/features/pronunciation/pronunciation_list_screen.dart`
  * `PronunciationScreen` -> `lib/features/pronunciation/pronunciation_screen.dart`
* **Công việc phụ trợ:**
  * Cần bổ sung các plugin phụ trợ như `video_player` (cho VideoScreen) nếu chưa có.

---

### Phase 5: Administration (Quản trị)
Màn hình panel dành cho admin.
* **Các màn hình:**
  * `AdminPanel` -> `lib/features/admin/admin_panel_screen.dart`

## Verification Plan

Sau mỗi Phase, chúng ta sẽ thực hiện các bước kiểm tra sau:
### Automated Tests
- Kiểm thử các widget đơn giản bằng `flutter test` (nếu cần thiết).
- Chạy `flutter analyze` để đảm bảo code sạch sẽ và tuân thủ lint.

### Manual Verification
- Chạy `flutter run` trên máy ảo hoặc thiết bị thật.
- Điều hướng thủ công (Hot Reload) qua các routes mới để đảm bảo UI không bị vỡ và hoạt động tương đương với React App.
- Kiểm tra tính tương thích Responsive (mobile màn hình bé vs to).
