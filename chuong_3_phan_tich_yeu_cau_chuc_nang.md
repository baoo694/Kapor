## 3.3. Phân tích yêu cầu chức năng

Quá trình phân tích yêu cầu chức năng được thực hiện dựa trên các luồng nghiệp vụ đã được triển khai cho hai nhóm tác nhân là Người học và Quản trị viên. Bảng 3.2 tổng hợp các yêu cầu chức năng (Functional Requirements - FR) của hệ thống Kapor. Mỗi yêu cầu được định danh bằng một mã FR duy nhất nhằm phục vụ việc truy vết và đặc tả chi tiết Use Case ở các phần sau.

**Bảng 3.2. Danh sách yêu cầu chức năng tổng hợp của hệ thống**

| ID | Tên chức năng | Mô tả | Ưu tiên |
| :--- | :--- | :--- | :--- |
| **Xác thực & Quản lý tài khoản** | | | |
| FR-01 | Đăng ký, đăng nhập và khôi phục tài khoản | Cho phép người dùng đăng ký, đăng nhập bằng email và mật khẩu; cấp cặp access token/refresh token JWT, làm mới phiên đăng nhập và đặt lại mật khẩu bằng mã OTP gửi qua email. | Cao |
| **Phân hệ DevAnalytics (Thống kê học tập)** | | | |
| FR-02 | Xem bảng điều khiển tiến độ | Hiển thị tiến độ học tập theo tuần hoặc tháng đối với các kỹ năng Nói, Từ vựng, Nghe và Roleplay. | Cao |
| FR-03 | Theo dõi chuỗi ngày học (streak) | Ghi nhận chuỗi ngày học hiện tại, chuỗi dài nhất và trạng thái hoạt động trong ngày; tự động sử dụng lượt đóng băng khi người học bỏ lỡ ngày học trong phạm vi cho phép. | Trung bình |
| FR-04 | Nhận gợi ý học tập tiếp theo | Đề xuất hoạt động cần thực hiện dựa trên thẻ MemByte đến hạn và tiến độ các chủ đề đang học. | Trung bình |
| **Phân hệ DevVocab (Từ vựng chuyên ngành)** | | | |
| FR-05 | Lọc chủ đề theo lĩnh vực | Cho phép người học lọc danh sách chủ đề theo các nhóm nội dung đã có như Frontend, Backend, DevOps và Agile. | Cao |
| FR-06 | Xem lộ trình và trạng thái mở khóa | Hiển thị các chủ đề, số bài đã hoàn thành, tỷ lệ tiến độ và trạng thái khóa dựa trên quan hệ tiên quyết giữa các chủ đề. | Cao |
| FR-07 | Xem và học bài từ vựng | Hiển thị nội dung bài học, danh sách từ vựng, nghĩa, ví dụ, phát âm và tiến độ học của từng bài. | Cao |
| FR-08 | Thực hiện hoạt động trong bài học | Cho phép học bằng flashcard, chế độ Study, Quiz và Matching; lưu kết quả từng hoạt động để cập nhật tiến độ bài học. | Cao |
| **Phân hệ Video Player (Học qua video)** | | | |
| FR-09 | Xem video kèm phụ đề song ngữ | Phát video YouTube và đồng bộ phụ đề tiếng Hàn - tiếng Việt đã được lưu trong hệ thống theo thời gian phát. | Cao |
| FR-10 | Tra cứu từ vựng trên phụ đề | Cho phép chọn các token có thể tương tác trên phụ đề để xem dạng từ, từ loại, cách đọc, nghĩa và ví dụ liên quan. | Cao |
| FR-11 | Làm câu hỏi và lưu từ khi xem video | Hiển thị câu hỏi tại các mốc thời gian của video, ghi nhận đáp án và cho phép lưu token đã chọn vào MemByte. | Trung bình |
| **Phân hệ MemByte (Ôn tập Flashcard)** | | | |
| FR-12 | Ôn tập thẻ nhớ theo FSRS | Lập lịch ôn tập từ vựng bằng FSRS dựa trên đánh giá Again, Hard, Good hoặc Easy của người học sau mỗi thẻ. | Cao |
| FR-13 | Xem nội dung mặt sau của thẻ | Mặt sau của thẻ hiển thị nghĩa, định nghĩa tiếng Anh, ví dụ tiếng Hàn và ghi chú ngữ pháp khi thẻ có dữ liệu tương ứng. | Trung bình |
| FR-14 | Nghe phát âm từ vựng | Phát âm tiếng Hàn của nội dung thẻ bằng dịch vụ tổng hợp giọng nói. | Trung bình |
| **Phân hệ TechTalk AI (Hội thoại nhập vai)** | | | |
| FR-15 | Chọn kịch bản Roleplay | Duyệt và chọn tình huống giao tiếp công sở cùng nhân vật, nhiệm vụ, mục tiêu và độ khó tương ứng. | Cao |
| FR-16 | Hội thoại bằng văn bản | Gửi tin nhắn cho nhân vật AI và nhận phản hồi được phát trực tuyến trong phiên hội thoại. | Cao |
| FR-17 | Hội thoại bằng giọng nói và nhận gợi ý | Thu âm câu nói, chuyển giọng nói thành văn bản để gửi vào phiên; hỗ trợ yêu cầu gợi ý và nghe phản hồi của nhân vật bằng TTS. | Cao |
| FR-18 | Xem kết quả và lịch sử hội thoại | Khi kết thúc phiên, hiển thị điểm ngữ pháp, từ vựng, sự lịch sự và mức độ hoàn thành nhiệm vụ; cho phép xem lại lịch sử các phiên. | Cao |
| **Phân hệ Pronunciation Lab (Luyện phát âm)** | | | |
| FR-19 | Ghi âm đọc câu đối chiếu | Cho phép nghe câu mẫu, thu âm giọng đọc tiếng Hàn bằng microphone và gửi bản ghi để đánh giá. | Cao |
| FR-20 | Xem kết quả đánh giá phát âm | Hiển thị điểm tổng thể, độ chính xác, độ trôi chảy, độ đầy đủ và phản hồi theo từ/âm vị từ Azure; kết hợp transcript, timeline của WhisperX và giải thích tiếng Việt do Gemini tạo. | Cao |
| FR-21 | Xem lịch sử và nghe lại bản thu | Lưu lịch sử các lần luyện tập và cho phép phát lại file ghi âm của từng lần đánh giá còn được lưu trữ. | Trung bình |
| **Phân hệ Honorifics Analyzer (Kính ngữ)** | | | |
| FR-22 | Phân tích và chuyển đổi kính ngữ | Phân tích câu tiếng Hàn bằng bộ quy tắc và Gemini khi cần, xác định mức độ trang trọng và tạo câu đã được chuẩn hóa. | Cao |
| FR-23 | Xem chi tiết chỉnh sửa | Hiển thị từng phần được sửa, nội dung trước và sau khi sửa, loại lỗi và giải thích tương ứng. | Trung bình |
| FR-24 | Áp dụng hoặc sao chép câu chuẩn hóa | Cho phép đưa câu đã chuẩn hóa trở lại ô nhập liệu hoặc sao chép câu vào clipboard. | Trung bình |
| **Phân hệ Admin Panel (Quản trị hệ thống)** | | | |
| FR-25 | Quản trị học liệu | Thực hiện CRUD chủ đề, bài học và video; xem, tạo hoặc nhập dữ liệu từ điển; xem và tạo bài luyện phát âm thông qua cổng quản trị. | Cao |
| FR-26 | Biên tập phụ đề song ngữ | Chỉnh sửa thời gian và nội dung phụ đề, nhập tệp SRT, đồng thời sử dụng Gemini để hỗ trợ dịch và phân tích token. | Trung bình |
| FR-27 | Quản lý kịch bản Roleplay và prompt | Tạo, sửa, xóa kịch bản, Persona, nhiệm vụ, mục tiêu, trọng số đánh giá và cấu hình prompt phục vụ TechTalk AI. | Cao |
| FR-28 | Quản lý người dùng và quyền quản trị | Tìm kiếm, tạo, cập nhật vai trò, xóa người dùng; cấp hoặc thu hồi quyền quản trị. | Cao |
| FR-29 | Xem thống kê người dùng và học tập | Hiển thị tổng số người dùng, số học liệu, DAU, MAU, thời lượng học trung bình, tăng trưởng người dùng, đăng ký mới và số lượt hoàn thành bài học đã được ghi nhận. | Trung bình |

### 3.3.1. Phân hệ Bảng điều khiển và Phân tích học tập (DevAnalytics)

- **[FR-02] Tổng quan tiến độ (Dashboard):** Hệ thống tổng hợp tiến độ Nói, Từ vựng, Nghe và Roleplay từ dữ liệu hoạt động của người dùng. Người học có thể chuyển giữa phạm vi bảy ngày và ba mươi ngày để theo dõi kết quả theo tuần hoặc tháng.
- **[FR-03] Quản lý chuỗi ngày học (Streak System):** Sau khi một hoạt động học được ghi nhận, hệ thống cập nhật chuỗi hiện tại, chuỗi dài nhất và trạng thái học trong ngày. Khi phát hiện số ngày bị bỏ lỡ không vượt quá số lượt đóng băng còn lại, hệ thống tự động trừ lượt đóng băng để duy trì chuỗi; ngược lại, chuỗi được tính lại từ hoạt động mới.
- **[FR-04] Gợi ý học tập:** Dashboard ưu tiên đề xuất ôn các thẻ MemByte đã đến hạn. Nếu không có thẻ đến hạn, hệ thống đề xuất tiếp tục chủ đề chưa hoàn thành và đã được mở khóa hoặc chọn một chủ đề đang hoạt động để người học bắt đầu.

### 3.3.2. Phân hệ Học từ vựng CNTT theo lộ trình (DevVocab)

- **[FR-05] Lọc nội dung theo lĩnh vực:** Giao diện DevVocab cung cấp các bộ lọc All, Frontend, Backend, DevOps và Agile để thu hẹp danh sách chủ đề đang hiển thị. Bộ lọc được áp dụng trực tiếp khi người học duyệt nội dung DevVocab.
- **[FR-06] Lộ trình và điều kiện tiên quyết:** Mỗi chủ đề hiển thị số bài hoàn thành, tổng số bài và tỷ lệ tiến độ. Dịch vụ lộ trình kiểm tra các chủ đề tiên quyết; chủ đề chỉ được mở khi người học đã hoàn thành các điều kiện liên quan.
- **[FR-07] Học bài từ vựng:** Người học mở một bài để xem các thuật ngữ, cách đọc, nghĩa, định nghĩa và ví dụ. Trạng thái học của từng từ và tiến độ chung của bài được lấy theo tài khoản hiện tại.
- **[FR-08] Hoạt động và tiến độ bài học:** Mỗi bài hỗ trợ Flashcards, Study, Quiz và Matching. Kết quả Study, số lần làm Quiz, điểm Quiz tốt nhất, kết quả Matching và trạng thái flashcard được lưu ở backend; Quiz đạt từ 80 điểm được ghi nhận là đã vượt qua.

### 3.3.3. Phân hệ Video học tập tương tác song ngữ (Interactive Video Player)

- **[FR-09] Xem video kèm phụ đề song ngữ:** Ứng dụng nhúng video YouTube, theo dõi vị trí phát và hiển thị dòng phụ đề tiếng Hàn cùng bản dịch tiếng Việt tương ứng. Người học có thể bật, tắt và di chuyển theo các dòng phụ đề đã được quản trị viên lưu trong hệ thống.
- **[FR-10] Tra cứu ngữ cảnh trực tiếp (In-context Dictionary):** Các token đã được đánh dấu có thể tương tác trong dữ liệu phụ đề cho phép người học mở bảng chi tiết gồm dạng hiển thị, dạng từ, từ loại, cách đọc, nghĩa Việt/Anh, định nghĩa, ví dụ và ghi chú ngữ pháp nếu có.
- **[FR-11] Tương tác trong video:** Khi video đến mốc đã cấu hình, hệ thống hiển thị câu hỏi và ghi nhận đáp án của người học. Từ bảng chi tiết token, người học có thể lưu từ cùng ngữ cảnh video vào bộ thẻ MemByte để ôn tập sau.

### 3.3.4. Phân hệ Ôn tập từ vựng thông minh (MemByte)

- **[FR-12] Ôn tập thẻ nhớ theo thuật toán FSRS:** MemByte tổng hợp các bộ thẻ đã lưu từ bài học và video, xác định thẻ đến hạn rồi hiển thị trong phiên ôn tập. Sau khi lật thẻ, người học chọn Again, Hard, Good hoặc Easy; FSRS cập nhật trạng thái thẻ và thời điểm ôn kế tiếp với mức ghi nhớ mục tiêu 90%.
- **[FR-13] Nội dung mặt sau của thẻ:** Sau khi lật thẻ, người học xem nghĩa tiếng Việt hoặc tiếng Anh, định nghĩa tiếng Anh, ví dụ tiếng Hàn và ghi chú ngữ pháp. Giao diện chỉ hiển thị các trường có dữ liệu trong thẻ đang ôn.
- **[FR-14] Tích hợp âm thanh (Audio Pronunciation):** Người học có thể nghe nội dung tiếng Hàn trên thẻ thông qua API TTS sử dụng Gemini TTS và phát lại tệp âm thanh trên ứng dụng.

### 3.3.5. Phân hệ Hội thoại nhập vai (TechTalk AI)

- **[FR-15] Lựa chọn kịch bản mô phỏng (Scenario Profile):** Danh sách kịch bản cung cấp thông tin về Persona, bối cảnh, nhiệm vụ, mục tiêu và độ khó. Khi chọn kịch bản, người học có thể bắt đầu phiên mới hoặc tiếp tục phiên đang hoạt động của kịch bản đó.
- **[FR-16] Trò chuyện bằng văn bản:** Người học gửi nội dung tiếng Hàn và nhận phản hồi của Persona từ Gemini qua luồng SSE. Tin nhắn, lượt hội thoại và kết quả đánh giá từng lượt được lưu trong phiên.
- **[FR-17] Tương tác bằng giọng nói và gợi ý:** Ứng dụng ghi âm, gửi tệp âm thanh để WhisperX chuyển thành văn bản rồi dùng transcript làm nội dung lượt nói. Trong phiên, người học có thể yêu cầu gợi ý và nghe phản hồi của Persona bằng Gemini TTS.
- **[FR-18] Đánh giá và lịch sử phiên:** Khi kết thúc, hệ thống tổng hợp điểm ngữ pháp, từ vựng, sự lịch sự và mức độ hoàn thành nhiệm vụ cùng nhận xét chi tiết. Người học có thể duyệt lịch sử, xem lại kết quả và tiếp tục phiên chưa kết thúc.

### 3.3.6. Phân hệ Luyện phát âm và Shadowing (Pronunciation Lab)

- **[FR-19] Ghi âm giọng đọc đối chiếu:** Người học chọn bài luyện, nghe câu mẫu bằng TTS, ghi âm qua microphone và gửi bản ghi PCM cho backend. Backend chuẩn hóa bản ghi thành WAV trước khi thực hiện đánh giá.
- **[FR-20] Phân tích và chấm điểm phát âm (Pronunciation Assessment):** Azure Speech Pronunciation Assessment cung cấp điểm tổng thể, Accuracy, Fluency cùng bằng chứng theo từ và âm vị. WhisperX cung cấp transcript, timeline và điểm Đủ từ dựa trên độ phủ từ/cụm trong câu mẫu; Gemini tạo phần giải thích luyện tập bằng tiếng Việt dựa trên kết quả Azure. Điểm phát âm hiển thị cho người học lấy từ Azure, còn Đủ từ lấy từ WhisperX.
- **[FR-21] Lịch sử và phát lại bản thu:** Mỗi lần đánh giá được lưu vào lịch sử cùng kết quả và thông tin tệp âm thanh. Người học có thể mở lại một lần luyện và phát bản ghi còn trong thời hạn lưu trữ để tự đối chiếu.

### 3.3.7. Phân hệ Phân tích Ngữ pháp và Kính ngữ (Honorifics Analyzer)

- **[FR-22] Phân tích và chuyển đổi kính ngữ:** Hệ thống áp dụng bộ quy tắc cho các mẫu kính ngữ đã xác định và sử dụng Gemini làm cơ chế bổ sung cho câu hoàn chỉnh nằm ngoài các quy tắc đó. Kết quả gồm mức độ trang trọng hiện tại, độ tin cậy, nguồn phân tích và câu đã được chuẩn hóa.
- **[FR-23] Chi tiết chỉnh sửa:** Kết quả phân tích liệt kê đoạn gốc, đoạn đã sửa, loại điều chỉnh và giải thích cho từng thay đổi để người học nhận biết cách chuyển đổi sang cách diễn đạt phù hợp.
- **[FR-24] Áp dụng hoặc sao chép:** Nút Áp dụng đưa toàn bộ câu chuẩn hóa trở lại ô nhập để người học tiếp tục chỉnh sửa; nút Sao chép lưu câu vào clipboard để sử dụng ở nơi khác.

### 3.3.8. Phân hệ Quản trị hệ thống (Admin Panel)

- **[FR-01] Xác thực và phân quyền (Authorization):** Người dùng đăng ký, đăng nhập bằng email/mật khẩu và nhận access token cùng refresh token JWT. Luồng quên mật khẩu gửi OTP qua email để đặt lại mật khẩu. Cổng Admin sử dụng cùng cơ chế đăng nhập nhưng các API `/api/admin/**` chỉ chấp nhận tài khoản có quyền `ROLE_ADMIN`.
- **[FR-25] Quản trị học liệu (Content Management):** Quản trị viên thực hiện CRUD đối với chủ đề, bài học và video. Đối với từ điển, giao diện hỗ trợ xem, tìm kiếm, tạo từng mục và nhập danh sách; đối với bài luyện phát âm, giao diện hỗ trợ xem danh sách và tạo bài mới.
- **[FR-26] Quản lý phụ đề (Subtitle Editor):** Giao diện quản trị cho phép tạo và chỉnh sửa thời điểm bắt đầu, kết thúc, nội dung tiếng Hàn và bản dịch tiếng Việt; hỗ trợ nhập SRT, xem trước video YouTube và dùng Gemini để dịch hoặc phân tích token trước khi lưu.
- **[FR-27] Quản lý kịch bản và prompt:** Scenario Builder cho phép cấu hình Persona, bối cảnh, nhiệm vụ, mục tiêu, từ vựng yêu cầu, trọng số đánh giá và prompt ghi đè. Phân hệ prompt hỗ trợ tạo, sửa, sao chép, phát hành và xóa các phiên bản prompt dùng cho AI.
- **[FR-28] Quản lý người dùng và quản trị viên:** Admin có thể tìm kiếm, phân trang, tạo, thay đổi vai trò hoặc xóa tài khoản người dùng; danh sách quản trị viên hỗ trợ cấp và thu hồi quyền `ROLE_ADMIN`.
- **[FR-29] Thống kê người dùng và hoạt động học:** Dashboard quản trị hiển thị tổng số người dùng, tổng số bài học, DAU, MAU và thời lượng học trung bình từ dữ liệu hoạt động. Các biểu đồ sử dụng dữ liệu tăng trưởng người dùng theo tháng, DAU và đăng ký mới trong bảy ngày gần nhất, cùng tổng số lượt hoàn thành bài học đã ghi nhận.
