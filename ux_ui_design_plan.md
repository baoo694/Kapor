# 🎨 Kapor UX/UI Design Specifications

Tài liệu này mô tả chi tiết thiết kế UX/UI cho 7 màn hình chính của ứng dụng Kapor, tập trung vào trải nghiệm người dùng, bố cục, màu sắc, tương tác và hiệu ứng (animations).

## 🌟 Tổng quan Thiết kế (Design System)
- **Màu sắc chủ đạo:** Xanh Teal (Sự tin cậy, công nghệ) kết hợp Xanh Blue.
- **Typography:** Hiện đại, dễ đọc, ưu tiên hiển thị rõ ràng cho cả tiếng Hàn và tiếng Việt/Anh. Cấu trúc rõ ràng, khoảng cách dòng (line-height) đủ lớn.
- **Phong cách:** Glass-morphism (hiệu ứng kính mờ) cho các thẻ (cards) kết hợp với Gradient nhẹ nhàng, giao diện tối giản (Minimalist), khoảng trắng (white-space) rộng rãi.
- **Trạng thái & Phản hồi:** Tương tác vi mô (Micro-interactions) như độ nảy (bounce) khi nhấn, hiệu ứng lướt (slide), haptic feedback (rung nhẹ) cho các tương tác quan trọng.

---

## 📱 Màn hình 1: DevAnalytics (Dashboard)
**Mục tiêu:** Cung cấp cái nhìn tổng quan về tiến độ học tập, khích lệ người dùng thông qua các chỉ số trực quan và điều hướng nhanh đến các tính năng.

### 1. StreakCounter (Đếm chuỗi ngày học)
- **UI:** Nằm ở vị trí nổi bật trên cùng. Sử dụng hiệu ứng Lottie hình ngọn lửa (flame animation) đang cháy sinh động nếu streak > 0.
- **UX:** Khi người dùng chạm (tap), hiển thị modal lịch (calendar) nhỏ gọn. Khi đạt chuỗi (streak) mới, ngọn lửa có hiệu ứng nảy to ra (scale bounce) kèm theo haptic feedback để chúc mừng.

### 2. ProgressChart (Biểu đồ tiến độ)
- **UI:** Biểu đồ vòng cung (Ring chart) sử dụng React Native Skia mượt mà. Bốn vòng màu khác nhau tương ứng với 4 kỹ năng: Nói, Từ vựng, Nghe, Điểm Roleplay.
- **UX:** Có Segmented control (công tắc chuyển) để đổi góc nhìn theo Tuần/Tháng. Khi tải dữ liệu, các vòng cung chạy hiệu ứng vẽ từ 0 đến phần trăm hiện tại (Draw-in animation).

### 3. SmartRecommendation (Gợi ý học tập thông minh)
- **UI:** Thẻ Gradient nổi bật với icon, tiêu đề rõ ràng (VD: "Resume: Frontend Deployment"), phụ đề và nút Call-to-Action (CTA) tương phản cao.
- **UX:** Card này có hiệu ứng float nhẹ nhàng. Tap vào thẻ sẽ mở thẳng tới bài học đang dang dở (Deep link) giúp giảm số lần chạm (clicks) cho người dùng.

### 4. QuickNavGrid (Lưới điều hướng nhanh)
- **UI:** Lưới 2x2 chứa các module chính (DevVocab, TechTalk AI, MemByte, Honorifics). Các thẻ mang phong cách glass-morphism tinh tế, icon lớn, nhãn (label) dễ nhìn.
- **UX:** Hiệu ứng lún/thu nhỏ nhẹ khi nhấn (Press scale effect bằng Reanimated) tạo cảm giác vật lý, xúc giác chân thực.

---

## 📱 Màn hình 2: DevVocab (Từ vựng chuyên ngành)
**Mục tiêu:** Trình bày lộ trình từ vựng theo cấu trúc hình cây (Skill tree) trực quan, giúp người dùng dễ dàng định vị vị trí và kỹ năng của mình.

### 1. TechStackTabBar (Thanh phân loại Domain)
- **UI:** Thanh cuộn ngang (Horizontal scroll) chứa các pill (chip) mượt mà. Tab đang chọn hiển thị nền màu đầy đủ (filled), tab chưa chọn dạng viền (outlined).
- **UX:** Hiệu ứng trượt (slide animation) của thanh gạch dưới (indicator) khi chuyển giữa các domain như Frontend, Backend, DevOps, v.v.

### 2. SkillTreeRoadmap (Lộ trình kỹ năng)
- **UI:** Các thẻ bài học sắp xếp theo chiều dọc, được nối với nhau bằng các đường thẳng đứt nét hoặc liền nét. 
  - Bài học bị khóa (Locked) sẽ có màu xám (grayscale) và biểu tượng ổ khóa. 
  - Bài học đã mở (Unlocked) hiển thị màu sắc tươi sáng cùng vòng tròn tiến độ bao quanh.
- **UX:** Khi hoàn thành một bài học điều kiện, đường nối sẽ sáng lên, ổ khóa mở ra và node mới chuyển sang màu (Unlock animation) tạo cảm giác như đang chơi game (gamification).

### 3. SmartSummarizer (Tổng hợp thông minh bằng AI)
- **UI:** Nút nổi (Floating Action Button - FAB) ở góc dưới cùng bên phải. Khi nhấn sẽ kéo lên một Bottom Sheet.
- **UX:** Trong lúc AI xử lý đoạn văn bản IT người dùng vừa nhập, màn hình hiển thị Skeleton loading. Sau đó hiển thị danh sách Flashcards dạng preview mượt mà kèm nút lưu nhanh vào thẻ MemByte.

---

## 📱 Màn hình 3: Interactive Video Player (Video Tương tác)
**Mục tiêu:** Nền tảng học qua video YouTube với phụ đề kép thông minh và tra từ điển thời gian thực không làm gián đoạn dòng suy nghĩ.

### 1. DualSubtitleOverlay (Phụ đề kép thông minh)
- **UI:** Phụ đề tiếng Hàn nằm ở trên (font chữ lớn, rõ nét), tiếng Việt nằm ở dưới (font chữ nhỏ hơn, mờ hơn một chút để không gây sao nhãng).
- **UX:** Phụ đề chạy đồng bộ chính xác với thời gian video. Các từ vựng chính yếu (tokens) có gạch chân chấm mờ (dotted underline) báo hiệu có thể tương tác (clickable).

### 2. Tương tác Từ Vựng (DictionaryPopup)
- **UX:** Khi tap vào một từ được gạch chân -> Video lập tức tự động tạm dừng. Bottom Sheet nảy lên (Spring slide up) hiển thị nghĩa IT, cách phát âm và ví dụ thực tế. Nút "Thêm vào MemByte" to và nổi bật. Khi đóng sheet, video có thể tự tiếp tục hoặc chờ người dùng bấm Play.

### 3. QuizMarker (Đánh dấu Câu đố)
- **UI:** Các chấm nhỏ đính trên thanh tiến trình video. Màu Vàng (sắp tới), Xanh lá (đã vượt qua), Xanh dương/To hơn (chấm hiện tại).
- **UX:** Khi video chạy đến điểm marker, hình ảnh sẽ blur (mờ) đi, video dừng và modal câu đố xuất hiện trượt từ giữa màn hình ra.

---

## 📱 Màn hình 4: MemByte (Flashcards Thông minh SRS)
**Mục tiêu:** Ôn tập từ vựng bằng thuật toán lặp lại ngắt quãng (Spaced Repetition) với tương tác vật lý sống động.

### 1. FlipCard (Thẻ lật 3D)
- **UI:** Một thẻ lớn chiếm phần lớn màn hình. 
  - Mặt trước: Chữ tiếng Hàn to, rõ tâm điểm + nút phát âm thanh nhỏ ở góc. 
  - Mặt sau: Nghĩa tiếng Việt, ngữ cảnh IT, và khối mã code (CodeContextBlock).
- **UX:** Tap hoặc vuốt (swipe) vào thẻ để lật (Hiệu ứng xoay 3D trục Y cực kỳ mượt mà trong khoảng 400ms).

### 2. CodeContextBlock (Khối mã ngữ cảnh)
- **UI:** Khu vực hiển thị code (Syntax highlighted) với font chữ Monospace chuẩn IT. Từ khóa tiếng Hàn đang học sẽ được highlight nền sáng (background color) ngay bên trong đoạn code để làm nổi bật ngữ cảnh.

### 3. SRSFeedbackLayout (Phản hồi ôn tập)
- **UI:** 4 nút đánh giá trượt lên từ dưới cùng (Slide up) sau khi người dùng lật thẻ: Again (Đỏ), Hard (Cam), Good (Xanh lá), Easy (Xanh dương). Kèm theo thời gian ôn tập tiếp theo hiển thị nhỏ gọn bên dưới mỗi nút.
- **UX:** Tap vào nút sẽ đẩy thẻ cũ bay đi (fly out) và rút thẻ mới lên (draw in) không có độ trễ.

---

## 📱 Màn hình 5: TechTalk AI (Roleplay Giao tiếp IT)
**Mục tiêu:** Luyện tập giao tiếp tiếng Hàn trong ngữ cảnh công việc thực tế (Review code, Báo cáo lỗi, Stand-up meeting) với một AI Persona sống động.

### 1. ScenarioProfileHeader (Hồ sơ tình huống)
- **UI:** Banner phía trên hiển thị Avatar AI (hình tròn), Tên, Chức vụ (VD: Tech Lead) bằng các Badge. Có phần nhiệm vụ (Mission objectives) dạng Checklist có thể thu gọn/mở rộng.

### 2. ChatHistoryThread (Luồng trò chuyện thời gian thực)
- **UI:** Bố cục bong bóng chat (chat bubbles) quen thuộc. AI (trái) dùng bong bóng gradient. User (phải) dùng màu khối đặc, có kèm huy hiệu đánh giá lỗi nhỏ (nếu có).
- **UX:** Hiệu ứng gõ chữ (Typing/Streaming effect) theo thời gian thực (token-by-token) tạo cảm giác AI đang thật sự suy nghĩ và gõ. Tự động phát âm thanh (Auto-play TTS) mượt mà không vấp. Cuộn mượt xuống tin nhắn mới nhất.

### 3. AudioTextInputBar & ScaffoldHint
- **UI:** Thanh nhập liệu bên dưới linh hoạt giữa Gõ phím (Keyboard) và Ghi âm (Mic). Khi giữ mic, xuất hiện hiệu ứng sóng âm (waveform) sinh động.
- **UX:** Gợi ý (ScaffoldHint - Icon Bóng đèn) sẽ tự động phát sáng nhẹ (Pulse glow animation) nếu AI nhận thấy người dùng chần chừ không phản hồi quá 10s. Nhấn vào sẽ hiện Card gợi ý từ khóa và mẫu câu.

---

## 📱 Màn hình 6: AI Pronunciation Lab (Phát âm & Shadowing)
**Mục tiêu:** Chỉnh sửa phát âm chuẩn xác với phân tích sóng âm chuyên sâu, giao diện mang tính chất "phòng thí nghiệm" (Lab).

### 1. Waveform Comparison (So sánh Sóng âm)
- **UI:** Hiển thị khu vực đồ thị. Sóng âm mẫu (Teal/Cyan) và sóng âm người dùng thu (Orange/Amber) đè lên nhau (Semi-transparent overlay).
- **UX:** Thanh chạy (playhead cursor) di chuyển dọc theo biểu đồ khi phát audio. Sự khác biệt nhịp điệu (rhythm) có thể được nhận diện ngay lập tức bằng mắt thường.

### 2. ColorCodedFeedback (Phản hồi Văn bản bằng Màu sắc)
- **UI:** Đoạn văn bản (Transcript) được đánh màu từng chữ: Xanh lá (Phát âm đúng), Cam (Lỗi nhỏ/Ngọng nhẹ), Đỏ (Lỗi nghiêm trọng/Bỏ sót âm).
- **UX:** Tap trực tiếp vào từ bị lỗi màu đỏ/cam sẽ mở ra một tooltip phân tích chi tiết phiên âm IPA và hướng dẫn cách đặt lưỡi/miệng.

### 3. MetricScoreDashboard (Bảng chỉ số đánh giá)
- **UI:** 3 vòng tròn chỉ số (Circular progress): Độ chính xác (Accuracy), Độ trôi chảy (Fluency), Ngữ điệu (Prosody).
- **UX:** Hiệu ứng số đếm tăng dần từ 0 lên số điểm (Count-up animation) cùng lúc vòng tròn chạy (draw stroke). Có mũi tên xanh/đỏ (↑↓) so sánh với lần thử (attempt) trước đó.

---

## 📱 Màn hình 7: Honorifics & Grammar Analyzer (Kính ngữ & Ngữ pháp)
**Mục tiêu:** Công cụ hỗ trợ viết email, tin nhắn Slack chuẩn mực, không bao giờ lo dùng sai cấp độ kính ngữ gây thất lễ trong công ty Hàn Quốc.

### 1. RawTextInput & PolitenessIndicator (Trạng thái Lịch sự)
- **UI:** Vùng nhập liệu rộng thoáng có nút mic để đọc chính tả. Ngay góc trên của vùng nhập là một huy hiệu (Badge) động chỉ báo mức độ lịch sự (PolitenessIndicator).
- **UX:** Huy hiệu thay đổi màu sắc và text realtime khi người dùng đang gõ: 
  - Đỏ (Banmal - Nguy hiểm/Thân mật)
  - Vàng (Heyohaet - Tiêu chuẩn)
  - Xanh lá (Hasipsio - Lịch sự/Doanh nghiệp). 
  - Hiệu ứng chuyển đổi màu sắc (Color cross-fade) mềm mại.

### 2. CorrectionDiffViewer (Chế độ xem so sánh sửa lỗi)
- **UI:** Chế độ xem nội tuyến (Inline diff view). Chữ/trợ từ gốc dùng sai bị gạch ngang màu đỏ, chữ sửa lại hiển thị màu xanh lá bên cạnh.
- **UX:** Tap vào phần sửa lỗi để xem giải thích dạng pop-up (VD: giải thích tại sao "너" phải đổi thành "귀하" trong ngữ cảnh email công việc).

### 3. ApplyTransformButton (Nút Áp dụng/Copy)
- **UI:** Nút bấm nổi bật (Primary Action) để "Áp dụng" văn bản đã sửa hoặc "Sao chép".
- **UX:** Khi sao chép thành công hiện thông báo Toast từ trên rớt xuống và icon copy biến thành hiệu ứng dấu tích (Checkmark animation) rồi từ từ mờ đi.
