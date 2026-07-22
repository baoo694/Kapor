# Kế hoạch phát triển — Video Lab, TechTalk AI, Pronunciation Lab và Honorifics

## Mục tiêu và phạm vi

Kế hoạch này biến bốn màn Flutter hiện đang dùng dữ liệu mẫu thành các chức năng hoàn chỉnh, có backend Spring Boot, MongoDB, tích hợp AI/speech, quản trị nội dung và kiểm thử. Lộ trình giả định một lập trình viên Flutter và một lập trình viên backend làm việc song song; kiểm thử được thực hiện trong từng tuần.

**Thứ tự ưu tiên:** Honorifics → Video Lab → TechTalk AI (text rồi voice) → Pronunciation Lab.

## Quy ước kỹ thuật chung

- API dùng JWT hiện có, bọc response trong `ApiResponse` và dùng error code nhất quán.
- Feature Flutter mới tách thành `data/`, `domain/`, `presentation/`; không để dữ liệu mock trong screen production.
- Các dịch vụ Gemini và speech phải có rate limit theo người dùng, quota theo ngày, log chi phí và timeout/retry có kiểm soát.
- Audio người dùng được lưu vào object storage bằng URL ký ngắn hạn; không ghi nội dung audio hoặc văn bản nhạy cảm vào log.
- Mọi request AI phải validate schema trước khi trả dữ liệu cho ứng dụng.
- Các endpoint mới có unit test, controller/integration test và test luồng chính trên Android lẫn iOS.

---

## Tuần 1 — Foundation, API contracts và proof of concept

### Mục tiêu

Chốt các quyết định có rủi ro cao trước khi xây feature lớn: YouTube embed trên mobile, khả năng chấm phát âm tiếng Hàn, cấu trúc API và giới hạn chi phí AI.

### Flutter

- Rà soát các màn mock hiện có: `video`, `techtalk`, `pronunciation`, `honorifics`.
- Tạo cấu trúc feature chuẩn cho từng module: model, DTO, service Dio, provider/controller và widget dùng lại.
- Bổ sung các dependency cần thiết sau POC: YouTube player, audio record/playback, permission microphone, waveform và SSE stream client.
- Bổ sung các UI state chuẩn: loading, empty, retry, timeout, mất mạng và quota exceeded.
- Đổi hướng route theo ID cho dữ liệu thật: `/videos`, `/video/:id`, `/techtalk/:sessionId`, `/pronunciation/:exerciseId`.

### Backend

- Viết OpenAPI hoặc tài liệu contract cho toàn bộ endpoint bốn feature.
- Chuẩn hoá payload lỗi và validation message trong `GlobalExceptionHandler`.
- Thêm Redis rate limit cho Gemini và speech theo `userId`; tạo event log tối thiểu: feature, provider, latency, trạng thái, estimated cost.
- Chuẩn bị object storage local/staging cho audio upload; quy định MIME, dung lượng và thời lượng được phép.

### POC bắt buộc

- Nhúng một video YouTube thật trên Android/iOS, kiểm tra play/pause, seek, tốc độ phát và nhận `currentTime` để đồng bộ phụ đề.
- Đánh giá một câu tiếng Hàn đã biết trước bằng speech provider dự kiến; xác minh chính xác các chỉ số/provider trả về.
- Chốt MVP Pronunciation: chỉ hứa hẹn những metric provider hỗ trợ cho `ko-KR`. Không hiển thị “prosody” như một điểm thật nếu POC không chứng minh được.

### Hoàn tất khi

- API contract được cả Flutter và backend xác nhận.
- YouTube và speech POC chạy trên thiết bị/simulator mục tiêu.
- Có bảng quota, lỗi trả về và quy tắc lưu/xoá audio.

---

## Tuần 2 — Honorifics Analyzer end-to-end

### Mục tiêu

Ra mắt chức năng phân tích và chuyển đổi kính ngữ có thể dùng được cho email/tin nhắn công sở.

### Backend

- Tạo package `honorifics` gồm `HonorificsController`, `HonorificsService`, Gemini agent và DTO.
- Cài đặt `POST /api/honorifics/analyze` nhận `text`, `targetLevel`; trả `currentLevel`, `confidence`, `corrections`, `transformedText`.
- Cài đặt `POST /api/honorifics/transform` cho thao tác chuyển đổi được người dùng xác nhận.
- Chuẩn hoá correction types: `particle`, `honorific`, `vocabulary`, `grammar`, `verb_ending`, `pronoun`.
- Ép AI trả structured JSON, validate trường bắt buộc, retry một lần khi JSON lỗi và trả thông báo an toàn nếu vẫn thất bại.
- Giới hạn MVP: tối đa 1.500 ký tự/request và quota phân tích theo user/ngày.

### Flutter

- Thay dữ liệu correction và formal text hard-code bằng `HonorificsService`.
- Tạo `HonorificAnalysis`, `CorrectionDiff`, `HonorificsProvider`.
- Nút Phân tích gọi API; chỉnh text sẽ reset kết quả cũ. Không gọi AI mỗi lần người dùng gõ.
- Hoàn thiện badge mức lịch sự, diff có thể mở giải thích, Apply thay text và Copy dùng clipboard thật.
- Xử lý đầy đủ loading, error, quota và giữ nguyên input khi request thất bại.

### Kiểm thử

- Unit test mapping DTO, kiểm tra JSON AI sai schema và validation input.
- Integration test hai endpoint với Gemini mock.
- Manual test ít nhất 15 câu: 반말, 해요체, 하십시오체, thuật ngữ IT và câu có nhiều lỗi.

### Hoàn tất khi

- Người dùng nhập văn bản, nhận diff đúng format, áp dụng/copy được bản formal.
- Không mất nội dung nhập khi offline, timeout hoặc vượt quota.

---

## Tuần 3 — Video Lab: dữ liệu, thư viện video và phụ đề kép

### Mục tiêu

Hiển thị video YouTube thật, chọn nội dung từ backend và đồng bộ phụ đề Hàn–Việt.

### Backend

- Mở rộng `Video` với domain, difficulty, YouTube ID/URL, thumbnail, duration, subtitle lines song ngữ và metadata.
- Tạo `SubtitleLine`, `TokenizedWord` và `DictionaryEntry`; token chứa surface, stem, POS và trạng thái click được.
- Cài đặt `GET /api/videos`, `GET /api/videos/{id}`, `GET /api/videos/{id}/subtitles`.
- Tạo `SubtitleTokenizer` để token hoá subtitle tiếng Hàn và liên kết tra từ điển theo stem.
- Seed tối thiểu ba video thuộc Frontend, Backend và DevOps, có subtitle song ngữ đã kiểm duyệt.
- Mở rộng admin video để nhập URL YouTube, metadata, subtitle timeline và gọi tokenize lại.

### Flutter

- Tạo video list/library; điều hướng vào `/video/:id` thay cho một VideoScreen cố định.
- Tích hợp player sau POC; poll thời gian chỉ khi đang play.
- Tìm subtitle active bằng binary search; chỉ rebuild khi subtitle line thay đổi.
- Hiển thị Korean lớn hơn Vietnamese; token content word có underline và có thể tap.
- Thêm skeleton/error/empty state cho library, detail và subtitle.

### Kiểm thử

- Test parser subtitle, tìm line theo timestamp và token mapping.
- Manual test video dài, seek nhanh, pause/resume, đổi tốc độ và app lifecycle.
- Đo sai lệch subtitle trên Android/iOS; mục tiêu không quá khoảng 300 ms với video mẫu.

### Hoàn tất khi

- Chọn được video từ backend, xem được YouTube và phụ đề cập nhật đúng theo playback.

---

## Tuần 4 — Video Lab: từ điển, MemByte, quiz và tiến độ

### Mục tiêu

Hoàn thiện vòng lặp học tương tác cho video.

### Backend

- Cài đặt tra từ theo stem và endpoint `GET /api/dictionary/{word}` hoặc đưa definition vào video detail theo nhu cầu UX.
- Cài đặt `POST /api/membyte/cards/from-video` để tạo flashcard từ dictionary entry và video context.
- Tạo `QuizMarker`, `QuizAttempt`, `VideoProgress` và endpoints lấy marker, nộp đáp án, lưu tiến độ.
- Chỉ ghi progress khi pause, exit, complete hoặc theo interval hợp lý; không ghi theo mỗi tick player.
- Cập nhật analytics video watched/time watched sau khi đạt ngưỡng xem hợp lệ.
- Bổ sung admin editor cho quiz markers và câu hỏi.

### Flutter

- Tap token: pause player, gọi dictionary, hiển thị bottom sheet nghĩa IT, romanization, ví dụ và Add to MemByte.
- Hiển thị marker trên timeline; khi tới mốc thì pause và mở quiz modal.
- Gửi answer, hiển thị kết quả và không trigger lại quiz đã hoàn thành.
- Khôi phục vị trí xem gần nhất sau khi người dùng mở lại video.

### Kiểm thử

- Integration test tạo card từ video, đáp án quiz và quyền sở hữu progress.
- Test marker khi seek qua lại, đóng dictionary sheet và resume playback.

### Hoàn tất khi

- Người dùng tra được từ, thêm card và hoàn thành quiz/tiến độ trong một luồng video thật.

---

## Tuần 5 — TechTalk AI: scenario, session và text streaming

### Mục tiêu

Cho phép roleplay IT bằng text với AI persona, phản hồi streaming và chấm từng message.

### Backend

- Tạo collections `scenarios` và `roleplay_sessions`.
- Scenario gồm persona, company, mission, objectives, difficulty, required vocabulary và evaluation weights.
- Cài đặt endpoints: list/detail scenario, start session, send message, stream response, hint, end session, history.
- Khi gửi message: lưu user message, chạy Gemini theo persona, stream token SSE và lưu response sau event done.
- Trả inline evaluation: grammar, vocabulary, politeness và corrections cho từng user message.
- Lưu `finalEvaluation`: overall, task completion, feedback và improvement areas khi end session.
- Dùng xác thực Bearer cho SSE native; nếu hỗ trợ Flutter Web phải dùng stream ticket ngắn hạn, không đưa JWT dài hạn vào query string.

### Flutter

- Thay danh sách scenario hard-code bằng API, có loading/error state.
- Tạo provider quản lý `sessionId`, messages, stream subscription, typing state, reconnect/cancel.
- Render token streaming trong chat bubble, gắn evaluation đúng message của user và tự cuộn có kiểm soát.
- Hiển thị header persona/mission và Scaffold Hint từ API.

### Kiểm thử

- Test SSE parser theo token/evaluation/done/error.
- Integration test ownership: user không đọc/ghi được session của user khác.
- Manual test double-tap send, mất mạng giữa stream, rời màn hình và mở lại session.

### Hoàn tất khi

- Có ít nhất ba scenario và một roleplay text hoàn chỉnh từ chọn scenario đến kết quả cuối phiên.

---

## Tuần 6 — TechTalk AI: voice, STT, TTS và history

### Mục tiêu

Mở rộng roleplay sang giọng nói mà không làm giảm khả năng kiểm soát nội dung của người học.

### Backend

- Tạo upload audio cho roleplay, kiểm MIME/size/duration và lưu URL ký ngắn hạn.
- Cài đặt STT proxy: nhận audio, trả transcript và confidence; AI chỉ nhận transcript sau khi user xác nhận hoặc chỉnh sửa.
- Tạo TTS cho tin AI nếu provider được chấp thuận; cache audio theo message để không sinh lại khi mở history.
- Ghi activity roleplay vào analytics và cập nhật số session/score của người dùng.

### Flutter

- Thêm microphone permission, hold-to-record, waveform đơn giản, playback và upload progress.
- Hiển thị transcript có thể chỉnh trước khi Send vào roleplay.
- Thêm nút phát audio AI, trạng thái generating/playing/error.
- Hoàn thiện session history và kết quả cuối phiên từ dữ liệu backend.

### Kiểm thử

- Test deny microphone, huỷ recording, audio quá dài và STT lỗi.
- Test message audio được replay từ history nhưng không tạo TTS mới.

### Hoàn tất khi

- Người dùng có thể gửi voice → kiểm transcript → nhận AI stream, xem đánh giá và mở lại lịch sử phiên.

---

## Tuần 7 — Pronunciation Lab: exercise, recording và evaluation pipeline

### Mục tiêu

Có luồng shadowing thật từ bài tập đến upload bản ghi và nhận kết quả chấm.

### Backend

- Tạo `pronunciation_exercises` và `pronunciation_attempts`.
- Exercise chứa câu tiếng Hàn, reference audio, reference waveform, duration và thứ tự bài.
- Cài đặt `GET /api/pronunciation/exercises`, detail, history và `POST /api/pronunciation/evaluate` multipart.
- Tạo provider adapter tách biệt với controller để có thể thay speech vendor mà không đổi API app.
- Sinh waveform server-side, lưu scores/transcription/word feedback; giới hạn file audio và tự xoá file upload lỗi.
- Cập nhật analytics speaking/pronunciation sau mỗi attempt hợp lệ.
- Bổ sung admin CRUD exercise và upload reference audio.

### Flutter

- Thay list và câu hard-code bằng API exercise.
- Tích hợp recorder thật, playback audio mẫu/bản ghi, permission và waveform live.
- Upload multipart có progress; khoá nút Evaluate trong lúc request để tránh tạo attempt trùng.
- Vẽ reference/user waveform, hiển thị trạng thái listen → record → uploading → result.

### Kiểm thử

- Test audio validation, upload failure và retry.
- Test DTO của các loại word feedback: correct, minor error, major error, omitted.
- Test trên Android/iOS thật với nhiều định dạng record của thiết bị.

### Hoàn tất khi

- Hoàn thành được luồng nghe mẫu → ghi âm → upload → nhận evaluation cho một câu tiếng Hàn.

---

## Tuần 8 — Pronunciation feedback, chất lượng, bảo mật và release candidate

### Mục tiêu

Đưa phản hồi phát âm thành trải nghiệm học được; hoàn tất kiểm thử chéo bốn feature.

### Backend

- Chuẩn hoá response metrics: `accuracy`, `fluency`, `completeness`, `overall`; chỉ thêm `prosody` khi provider thật sự hỗ trợ tiếng Hàn.
- Trả word-level và phoneme-level detail khi có; không bịa feedback khi provider không cung cấp dữ liệu.
- Tạo index cho history theo `userId`, `exerciseId`, `attemptedAt`.
- Thêm job dọn audio hết hạn và dashboard quan sát API errors/cost/quota.

### Flutter

- Tô màu transcript, tap từ xem chi tiết phát âm/hướng dẫn; dashboard điểm và so sánh attempt trước.
- Hiển thị rõ metric không khả dụng thay vì điểm 0 gây hiểu nhầm.
- Hoàn thiện accessibility, i18n Việt/Anh, loading skeleton và responsive layout.
- Kiểm tra toàn bộ deeplink từ Dashboard/QuickNav tới bốn module.

### Kiểm thử và phát hành

- E2E: Video → Add MemByte; TechTalk text/voice → result/history; Honorifics analyze/apply/copy; Pronunciation → history.
- Regression test auth, API JWT, role ownership, quota/rate limit, prompt injection và upload validation.
- Manual QA trên iOS Simulator, Android Emulator và tối thiểu một thiết bị thật mỗi nền tảng.
- Native Korean speaker review cho bộ câu phát âm; điều chỉnh ngưỡng hiển thị màu điểm.
- Staging load test, theo dõi error rate/latency/cost trước release.

### Hoàn tất khi

- Bốn feature chạy bằng dữ liệu/API thật, không còn mock production.
- Không có lỗi blocker trong luồng E2E; quota, analytics, quyền truy cập và xử lý lỗi đã được kiểm tra.

---

## Tiêu chí release tổng thể

- Video subtitle đồng bộ, quiz không lặp và từ tra được thêm vào MemByte.
- TechTalk lưu session bền vững, stream không tạo duplicate response, voice có bước xác nhận transcript.
- Honorifics trả structured correction an toàn và không mất input khi có lỗi.
- Pronunciation chỉ hiển thị các điểm/feedback được provider hỗ trợ thực tế cho tiếng Hàn.
- Mọi API AI/speech có quota, rate limit, timeout, quan sát chi phí và bảo vệ dữ liệu audio.
