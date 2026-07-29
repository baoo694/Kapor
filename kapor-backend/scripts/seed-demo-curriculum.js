// Kapor production-demo curriculum seed.
//
// This script creates a complete, bilingual Korean-for-tech curriculum:
//   8 topics × 3 lessons × 8 unique flashcards = 192 flashcards
//   3 contextual exercises per lesson = 72 exercises
//
// It is safe to run repeatedly. Only deterministic Kapor demo records and the
// legacy `dv-seed-*` development catalog are removed; user accounts and any
// content created outside these seed prefixes are never touched.
//
// Run from the repository root against the Mongo container:
//   docker cp scripts/seed-demo-curriculum.js kapor-backend-mongo-1:/tmp/seed-demo-curriculum.js
//   docker exec kapor-backend-mongo-1 mongosh kapor /tmp/seed-demo-curriculum.js

const kaporDb = db.getSiblingDB('kapor');
const PREFIX = 'dv-demo';
const CARDS_PER_LESSON = 8;
const EXERCISES_PER_LESSON = 3;

// The authentication API validates an email address rather than a separate
// username. This account's display name is `admin`; sign in with the email
// below. The bcrypt hash is intentionally stored instead of a raw password.
const DEMO_ADMIN = {
  _id: `${PREFIX}-user-admin`,
  email: 'admin@domday.food',
  passwordHash: '$2y$12$/M5t2FX72jjRo6nKqbsqXulxTt0s0fKRibt82DSoy/6NAMAN3HpnO',
  provider: 'email',
  providerId: 'admin@domday.food',
  profile: {
    displayName: 'admin',
    avatarUrl: '',
    nativeLanguage: 'vi',
    learningGoals: ['Korean for technology', 'Product demonstration'],
    joinedAt: new Date(),
  },
  streak: { current: 0, longest: 0, lastActiveDate: null, freezesRemaining: 2 },
  settings: {
    ttsSpeed: 1.0,
    dailyGoalMinutes: 15,
    dailyGoalCards: 20,
    theme: 'dark',
    notificationsEnabled: true,
    reminderTime: '09:00',
    locale: 'vi',
  },
  stats: {
    totalStudyMinutes: 0,
    totalCardsReviewed: 0,
    totalRoleplaySessions: 0,
    totalVideosWatched: 0,
  },
  refreshToken: null,
  refreshTokenExpiry: null,
  roles: ['ROLE_USER', 'ROLE_ADMIN'],
  createdAt: new Date(),
  updatedAt: new Date(),
};

// Revised-Romanization-style helper. It intentionally keeps Latin technical
// acronyms (API, DNS, CI, …) intact while giving every Korean card a useful
// pronunciation hint without relying on an external service.
const INITIAL = ['g', 'kk', 'n', 'd', 'tt', 'r', 'm', 'b', 'pp', 's', 'ss', '', 'j', 'jj', 'ch', 'k', 't', 'p', 'h'];
const MEDIAL = ['a', 'ae', 'ya', 'yae', 'eo', 'e', 'yeo', 'ye', 'o', 'wa', 'wae', 'oe', 'yo', 'u', 'wo', 'we', 'wi', 'yu', 'eu', 'ui', 'i'];
const FINAL = ['', 'k', 'k', 'ks', 'n', 'nj', 'nh', 't', 'l', 'lk', 'lm', 'lb', 'ls', 'lt', 'lp', 'lh', 'm', 'p', 'ps', 't', 't', 'ng', 't', 't', 'k', 't', 'p', 't'];

function romanize(korean) {
  return [...korean].map((character) => {
    const code = character.charCodeAt(0) - 0xac00;
    if (code < 0 || code > 11171) return character;
    const initial = Math.floor(code / 588);
    const medial = Math.floor((code % 588) / 28);
    const final = code % 28;
    return `${INITIAL[initial]}${MEDIAL[medial]}${FINAL[final]}`;
  }).join('-').replace(/-\s+-/g, ' ');
}

function term(korean, vietnamese, english) {
  return { korean, vietnamese, english };
}

function hasHangul(value) {
  return /[\uAC00-\uD7AF]/.test(value || '');
}

function koreanText(first, second) {
  return hasHangul(first) ? first : second;
}

function vietnameseText(first, second) {
  return hasHangul(first) ? second : first;
}

function lesson(slug, title, titleVi, goalKo, goalVi, scenarioKo, scenarioVi, actionKo, actionVi, snippet, terms) {
  return {
    slug,
    title,
    titleVi,
    goalKo: koreanText(goalKo, goalVi),
    goalVi: vietnameseText(goalKo, goalVi),
    scenarioKo: koreanText(scenarioKo, scenarioVi),
    scenarioVi: vietnameseText(scenarioKo, scenarioVi),
    actionKo: koreanText(actionKo, actionVi),
    actionVi: vietnameseText(actionKo, actionVi),
    snippet,
    terms,
  };
}

const curriculum = [
  {
    id: `${PREFIX}-topic-frontend-foundations`,
    domain: 'frontend',
    title: '프론트엔드 구조와 컴포넌트 설계',
    titleVi: 'Cấu trúc Frontend và thiết kế component',
    description: 'Đọc cấu trúc dự án, tách trách nhiệm component và xây dựng design system nhất quán.',
    order: 10,
    tags: ['frontend', 'react', 'architecture', 'design-system'],
    lessons: [
      lesson(
        'frontend-project-map',
        '프로젝트 구조를 읽는 법',
        'Đọc cấu trúc dự án Frontend',
        '새 프로젝트를 열었을 때 실행 흐름과 핵심 파일의 역할을 설명할 수 있습니다.',
        'Giải thích được luồng chạy và vai trò của các tệp quan trọng khi mở một dự án mới.',
        '새 팀원이 “이 화면은 어디에서 시작하고 어떻게 실행하나요?”라고 묻습니다.',
        'Một thành viên mới hỏi: “Màn hình này bắt đầu ở đâu và chạy như thế nào?”',
        '진입점부터 확인한 뒤 의존성과 실행 명령을 순서대로 안내합니다.',
        'Bắt đầu từ entry point, sau đó hướng dẫn dependency và lệnh chạy theo thứ tự.',
        '// package.json\n"scripts": { "dev": "vite", "build": "vite build" }\n// src/main.tsx → <App />',
        [
          term('저장소', 'Kho mã nguồn', 'Repository'),
          term('디렉터리 구조', 'Cấu trúc thư mục', 'Directory structure'),
          term('진입점', 'Điểm vào', 'Entry point'),
          term('의존성', 'Phụ thuộc', 'Dependency'),
          term('패키지 관리자', 'Trình quản lý gói', 'Package manager'),
          term('설정 파일', 'Tệp cấu hình', 'Configuration file'),
          term('실행 명령', 'Lệnh chạy', 'Run command'),
          term('개발 서버', 'Máy chủ phát triển', 'Development server'),
        ],
      ),
      lesson(
        'frontend-component-boundaries',
        '컴포넌트의 책임을 나누기',
        'Phân chia trách nhiệm component',
        'Một component nên làm gì, nhận dữ liệu ra sao và khi nào cần tách nhỏ được xác định rõ ràng.',
        'Xác định rõ component làm gì, nhận dữ liệu thế nào và khi nào cần tách nhỏ.',
        'Màn hình hồ sơ đang quá dài, khó review và nhiều phần không thể tái sử dụng.',
        'Màn hình hồ sơ quá dài, khó review và nhiều phần không thể tái sử dụng.',
        '화면의 책임을 나눈 뒤 속성과 인터페이스를 통해 하위 컴포넌트를 연결합니다.',
        'Tách trách nhiệm màn hình, rồi kết nối component con qua props và interface.',
        'function ProfileCard({ user }) {\n  return <article><Avatar user={user} /><ProfileMeta user={user} /></article>;\n}',
        [
          term('컴포넌트 책임', 'Trách nhiệm component', 'Component responsibility'),
          term('속성', 'Thuộc tính truyền vào', 'Props'),
          term('하위 컴포넌트', 'Component con', 'Child component'),
          term('재사용성', 'Khả năng tái sử dụng', 'Reusability'),
          term('결합도', 'Độ phụ thuộc giữa thành phần', 'Coupling'),
          term('응집도', 'Độ gắn kết trách nhiệm', 'Cohesion'),
          term('인터페이스', 'Giao diện/khế ước', 'Interface'),
          term('관심사 분리', 'Tách biệt mối quan tâm', 'Separation of concerns'),
        ],
      ),
      lesson(
        'frontend-design-tokens',
        '디자인 토큰으로 일관성 만들기',
        'Tạo tính nhất quán bằng design token',
        '색상, 간격, chữ và component variant를 token으로 quản lý하는 이유를 설명할 수 있습니다.',
        'Giải thích được vì sao cần quản lý màu sắc, khoảng cách, chữ và variant bằng token.',
        'Hai màn hình dùng màu và khoảng cách khác nhau dù cùng một sản phẩm.',
        '두 화면이 같은 제품인데도 색상과 여백이 서로 다르게 적용되어 있습니다.',
        '토큰과 명세를 먼저 합의하고 변형 옵션은 제한된 규칙 안에서 사용합니다.',
        'Thống nhất token và specification trước, sau đó chỉ dùng variant trong quy tắc giới hạn.',
        ':root {\n  --color-brand: #5D72FF;\n  --space-3: 12px;\n  --radius-card: 16px;\n}',
        [
          term('디자인 토큰', 'Token thiết kế', 'Design token'),
          term('색상 팔레트', 'Bảng màu', 'Color palette'),
          term('글꼴 크기', 'Cỡ chữ', 'Font size'),
          term('간격 체계', 'Hệ thống khoảng cách', 'Spacing scale'),
          term('테마', 'Chủ đề giao diện', 'Theme'),
          term('변형 옵션', 'Tùy chọn biến thể', 'Variant'),
          term('일관성', 'Tính nhất quán', 'Consistency'),
          term('컴포넌트 명세', 'Đặc tả component', 'Component specification'),
        ],
      ),
    ],
  },
  {
    id: `${PREFIX}-topic-frontend-experience`,
    domain: 'frontend',
    title: '프론트엔드 경험 품질',
    titleVi: 'Chất lượng trải nghiệm Frontend',
    description: 'Thiết kế trạng thái UI, kiểm tra khả năng truy cập và cải thiện hiệu năng rendering.',
    order: 20,
    tags: ['frontend', 'ux', 'accessibility', 'performance'],
    lessons: [
      lesson(
        'frontend-ui-states',
        '화면 상태를 명확히 표현하기',
        'Thể hiện rõ các trạng thái giao diện',
        'Người học biết thiết kế loading, empty, error và success để người dùng không bị mất phương hướng.',
        '로딩, 빈 화면, 오류, 성공 상태를 구분해 사용자가 다음 행동을 알 수 있게 만듭니다.',
        'Danh sách bài học gọi API chậm; người dùng không biết đang tải hay hệ thống đã lỗi.',
        '학습 목록 API가 느릴 때 사용자가 로딩 중인지 오류인지 구분하지 못합니다.',
        '각 상태의 메시지와 다음 행동을 정의하고 피드백을 화면에 즉시 반영합니다.',
        'Xác định thông điệp và hành động kế tiếp cho từng trạng thái, rồi phản hồi ngay trên UI.',
        'if (isLoading) return <LessonSkeleton />;\nif (error) return <RetryPanel onRetry={loadLessons} />;\nreturn <LessonList items={items} />;',
        [
          term('로딩 상태', 'Trạng thái đang tải', 'Loading state'),
          term('빈 상태', 'Trạng thái trống', 'Empty state'),
          term('오류 상태', 'Trạng thái lỗi', 'Error state'),
          term('성공 상태', 'Trạng thái thành công', 'Success state'),
          term('골격 화면', 'Màn hình skeleton', 'Skeleton screen'),
          term('알림 메시지', 'Thông báo', 'Notification message'),
          term('비활성화', 'Vô hiệu hóa', 'Disabled state'),
          term('사용자 피드백', 'Phản hồi cho người dùng', 'User feedback'),
        ],
      ),
      lesson(
        'frontend-accessibility',
        '모두를 위한 접근성 점검',
        'Kiểm tra accessibility cho mọi người',
        'Người học kiểm tra được luồng bàn phím, nhãn mô tả, độ tương phản và trải nghiệm screen reader.',
        '키보드 흐름, 설명 레이블, 명도 대비, 스크린 리더 경험을 점검할 수 있습니다.',
        'Một nút chỉ có icon, không thể tab đến và screen reader không đọc được ý nghĩa.',
        '아이콘만 있는 버튼에 탭으로 접근할 수 없고 스크린 리더도 의미를 읽지 못합니다.',
        '키보드만으로 흐름을 검증하고 모든 조작 요소에 명확한 이름을 제공합니다.',
        'Kiểm tra luồng chỉ bằng bàn phím và đặt tên rõ ràng cho mọi phần tử thao tác.',
        '<button aria-label="학습 목록 새로고침" onClick={reload}>\n  <RefreshCw aria-hidden="true" />\n</button>',
        [
          term('대체 텍스트', 'Văn bản thay thế', 'Alternative text'),
          term('키보드 탐색', 'Điều hướng bằng bàn phím', 'Keyboard navigation'),
          term('초점 순서', 'Thứ tự focus', 'Focus order'),
          term('명도 대비', 'Độ tương phản sáng tối', 'Contrast ratio'),
          term('스크린 리더', 'Trình đọc màn hình', 'Screen reader'),
          term('접근성 레이블', 'Nhãn accessibility', 'Accessibility label'),
          term('의미론', 'Ngữ nghĩa cấu trúc', 'Semantics'),
          term('보조 기술', 'Công nghệ hỗ trợ', 'Assistive technology'),
        ],
      ),
      lesson(
        'frontend-rendering-performance',
        '렌더링 성능 개선',
        'Cải thiện hiệu năng rendering',
        'Người học nhận biết render thừa, lựa chọn lazy loading và đo được điểm nghẽn trước khi tối ưu.',
        '불필요한 다시 그리기를 찾고 지연 로딩을 선택하며 최적화 전에 병목을 측정합니다.',
        'Trang dashboard giật khi chuyển tab dù dữ liệu không thay đổi.',
        '데이터가 바뀌지 않았는데도 대시보드 탭을 전환할 때 화면이 끊깁니다.',
        '측정 결과를 먼저 공유하고 큰 모듈은 나눈 뒤 필요한 시점에만 불러옵니다.',
        'Chia sẻ số liệu đo trước, tách module lớn rồi chỉ tải khi cần.',
        'const ReportsPanel = lazy(() => import("./ReportsPanel"));\nconst total = useMemo(() => calculate(items), [items]);',
        [
          term('렌더링', 'Kết xuất giao diện', 'Rendering'),
          term('다시 그리기', 'Vẽ lại', 'Re-render'),
          term('지연 로딩', 'Tải trì hoãn', 'Lazy loading'),
          term('메모이제이션', 'Ghi nhớ kết quả tính toán', 'Memoization'),
          term('번들 크기', 'Kích thước bundle', 'Bundle size'),
          term('코드 분할', 'Chia tách mã', 'Code splitting'),
          term('성능 측정', 'Đo hiệu năng', 'Performance measurement'),
          term('병목 구간', 'Điểm nghẽn', 'Bottleneck'),
        ],
      ),
    ],
  },
  {
    id: `${PREFIX}-topic-backend-api`,
    domain: 'backend',
    title: '백엔드 API 설계와 요청 처리',
    titleVi: 'Thiết kế API Backend và xử lý request',
    description: 'Thiết kế API hướng resource, validate input và xây dựng truy vấn danh sách đáng tin cậy.',
    order: 10,
    tags: ['backend', 'api', 'validation', 'rest'],
    lessons: [
      lesson(
        'backend-resource-api',
        '리소스 중심 API 설계',
        'Thiết kế API hướng resource',
        'URI, method và payload được chọn theo resource để client có thể dự đoán API.',
        '클라이언트가 예측할 수 있도록 URI, 메서드, 페이로드를 리소스 중심으로 설계합니다.',
        'Mobile team cần endpoint lấy chi tiết bài học và đánh dấu tiến độ flashcard.',
        '모바일 팀이 수업 상세 조회와 플래시카드 진행 저장 API를 요청합니다.',
        '리소스와 동작을 분리하고 요청 및 응답 형식을 문서로 합의합니다.',
        'Tách resource và hành động, rồi thống nhất request/response bằng tài liệu.',
        'GET /api/lessons/{lessonId}\nPUT /api/lessons/{lessonId}/flashcards/{cardId}\nContent-Type: application/json',
        [
          term('리소스', 'Tài nguyên API', 'Resource'),
          term('엔드포인트', 'Điểm cuối API', 'Endpoint'),
          term('요청 본문', 'Nội dung request', 'Request body'),
          term('응답 형식', 'Định dạng response', 'Response format'),
          term('직렬화', 'Tuần tự hóa dữ liệu', 'Serialization'),
          term('역직렬화', 'Giải tuần tự hóa', 'Deserialization'),
          term('클라이언트', 'Ứng dụng khách', 'Client'),
          term('타임아웃', 'Hết thời gian chờ', 'Timeout'),
        ],
      ),
      lesson(
        'backend-input-validation',
        '요청 DTO와 입력 검증',
        'DTO request và kiểm tra input',
        'Input không tin cậy được kiểm tra tại boundary và lỗi trả về có thể hành động được.',
        '신뢰할 수 없는 입력은 경계 지점에서 검증하고 행동 가능한 오류를 반환합니다.',
        'Admin tạo bài học nhưng gửi title trống và order âm.',
        '관리자가 제목이 비어 있고 순서가 음수인 수업을 생성하려고 합니다.',
        'DTO에 제약을 선언하고 잘못된 필드와 수정 방법을 응답에 명시합니다.',
        'Khai báo constraint trong DTO, nêu rõ field sai và cách sửa trong response.',
        'record LessonRequest(@NotBlank String title, @Min(0) int order) {}\n// 400: { "field": "title", "message": "required" }',
        [
          term('데이터 전송 객체', 'Đối tượng truyền dữ liệu', 'Data transfer object'),
          term('입력 제약', 'Ràng buộc đầu vào', 'Input constraint'),
          term('필드 검증', 'Kiểm tra trường dữ liệu', 'Field validation'),
          term('경계값', 'Giá trị biên', 'Boundary value'),
          term('필수값', 'Giá trị bắt buộc', 'Required value'),
          term('정규식', 'Biểu thức chính quy', 'Regular expression'),
          term('유효성 오류', 'Lỗi validation', 'Validation error'),
          term('정제', 'Làm sạch dữ liệu', 'Sanitization'),
        ],
      ),
      lesson(
        'backend-list-query',
        '목록 조회: 필터·정렬·페이지',
        'Truy vấn danh sách: filter, sort và trang',
        'Danh sách lớn vẫn ổn định khi filter, sort và pagination có contract rõ ràng.',
        '필터, 정렬, 페이지 규칙이 명확하면 큰 목록도 안정적으로 조회할 수 있습니다.',
        'Admin cần tìm bài học theo domain, sắp xếp theo thứ tự và tải từng phần.',
        '관리자가 도메인별 수업을 찾고 순서대로 정렬한 뒤 일부씩 불러와야 합니다.',
        '필터 조건과 정렬 기준을 고정하고 결과 집합의 다음 위치를 명확히 전달합니다.',
        'Cố định điều kiện lọc và tiêu chí sort, rồi truyền rõ vị trí tiếp theo của tập kết quả.',
        'GET /api/lessons?domain=backend&sort=order&pageSize=20&cursor=eyJvcmRlciI6MjB9',
        [
          term('페이지네이션', 'Phân trang', 'Pagination'),
          term('페이지 크기', 'Kích thước trang', 'Page size'),
          term('커서', 'Con trỏ phân trang', 'Cursor'),
          term('필터 조건', 'Điều kiện lọc', 'Filter condition'),
          term('정렬 기준', 'Tiêu chí sắp xếp', 'Sort criterion'),
          term('오프셋', 'Độ dịch vị trí', 'Offset'),
          term('검색어', 'Từ khóa tìm kiếm', 'Search query'),
          term('결과 집합', 'Tập kết quả', 'Result set'),
        ],
      ),
    ],
  },
  {
    id: `${PREFIX}-topic-backend-reliability`,
    domain: 'backend',
    title: '데이터·인증·운영 안정성',
    titleVi: 'Độ tin cậy dữ liệu, xác thực và vận hành',
    description: 'Mô hình dữ liệu, bảo vệ phiên đăng nhập và quan sát backend trong production.',
    order: 20,
    tags: ['backend', 'database', 'security', 'observability'],
    lessons: [
      lesson(
        'backend-domain-model',
        '도메인 모델과 데이터 수명주기',
        'Domain model và vòng đời dữ liệu',
        'Người học phân biệt entity, value object, quan hệ và quyết định normalize đúng lúc.',
        '엔터티, 값 객체, 관계를 구분하고 정규화 시점을 결정할 수 있습니다.',
        'Một lesson có nhiều flashcard nhưng tiến độ học lại thuộc về từng user.',
        '하나의 수업에는 여러 카드가 있지만 학습 진행은 사용자별로 따로 관리됩니다.',
        '변하지 않는 값과 독립된 생명주기를 가진 대상을 나누어 모델링합니다.',
        'Mô hình hóa riêng giá trị bất biến và đối tượng có vòng đời độc lập.',
        'lesson { _id, topicId, vocabulary: [...] }\nflashcard_progress { userId, lessonId, vocabularyId, status }',
        [
          term('도메인 모델', 'Mô hình domain', 'Domain model'),
          term('엔터티', 'Thực thể', 'Entity'),
          term('값 객체', 'Đối tượng giá trị', 'Value object'),
          term('관계', 'Mối quan hệ dữ liệu', 'Relationship'),
          term('정규화', 'Chuẩn hóa dữ liệu', 'Normalization'),
          term('비정규화', 'Phi chuẩn hóa dữ liệu', 'Denormalization'),
          term('데이터 수명주기', 'Vòng đời dữ liệu', 'Data lifecycle'),
          term('무결성', 'Tính toàn vẹn', 'Integrity'),
        ],
      ),
      lesson(
        'backend-auth-session',
        '계정·세션·토큰 보호',
        'Bảo vệ tài khoản, session và token',
        'Luồng đăng nhập được thiết kế để password không bị lưu thô và token có vòng đời rõ ràng.',
        '비밀번호를 평문으로 저장하지 않고 토큰의 수명주기를 명확히 설계합니다.',
        'Một người dùng đăng nhập trên điện thoại mới trong khi refresh token cũ bị lộ.',
        '사용자가 새 휴대폰에서 로그인했는데 이전 갱신 토큰 노출이 의심됩니다.',
        '자격 증명을 안전하게 보관하고 만료 및 회전 정책으로 세션 위험을 줄입니다.',
        'Lưu credential an toàn, giảm rủi ro session bằng expiry và rotation policy.',
        'Authorization: Bearer <access-token>\nPOST /api/auth/refresh\n{ "refreshToken": "…" }',
        [
          term('계정', 'Tài khoản', 'Account'),
          term('자격 증명', 'Thông tin xác thực', 'Credential'),
          term('비밀번호 해시', 'Hash mật khẩu', 'Password hash'),
          term('액세스 토큰', 'Access token', 'Access token'),
          term('갱신 토큰', 'Refresh token', 'Refresh token'),
          term('만료 시간', 'Thời điểm hết hạn', 'Expiration time'),
          term('세션 탈취', 'Chiếm đoạt session', 'Session hijacking'),
          term('로그인 이력', 'Lịch sử đăng nhập', 'Login history'),
        ],
      ),
      lesson(
        'backend-structured-observability',
        '구조화 로그와 상태 점검',
        'Log có cấu trúc và kiểm tra trạng thái',
        'Một request production được theo dõi từ log đến health check mà không cần đọc dữ liệu nhạy cảm.',
        '민감 데이터를 읽지 않고도 로그와 헬스 체크로 프로덕션 요청을 추적할 수 있습니다.',
        'API thỉnh thoảng chậm, nhưng team không biết lỗi ở mobile, backend hay dependency.',
        'API가 가끔 느려지지만 모바일, 백엔드, 의존 서비스 중 어디가 문제인지 알 수 없습니다.',
        '요청 식별자를 연결하고 준비 상태와 생존 상태를 분리해 관찰합니다.',
        'Liên kết request ID, tách readiness và liveness để quan sát chính xác.',
        'logger.info("request_complete", Map.of("requestId", requestId, "status", 200));\nGET /actuator/health',
        [
          term('구조화 로그', 'Log có cấu trúc', 'Structured log'),
          term('로그 레벨', 'Mức log', 'Log level'),
          term('상관관계 ID', 'Correlation ID', 'Correlation ID'),
          term('요청 ID', 'ID request', 'Request ID'),
          term('분산 추적', 'Theo dõi phân tán', 'Distributed tracing'),
          term('헬스 체크', 'Kiểm tra sức khỏe', 'Health check'),
          term('준비 상태', 'Trạng thái sẵn sàng', 'Readiness state'),
          term('생존 상태', 'Trạng thái sống', 'Liveness state'),
        ],
      ),
    ],
  },
  {
    id: `${PREFIX}-topic-devops-containers`,
    domain: 'devops',
    title: '개발 환경과 컨테이너 운영',
    titleVi: 'Môi trường phát triển và vận hành container',
    description: 'Tái tạo môi trường, tạo Docker image và kết nối service an toàn bằng Compose.',
    order: 10,
    tags: ['devops', 'docker', 'compose', 'environment'],
    lessons: [
      lesson(
        'devops-reproducible-environment',
        '개발 환경을 재현하기',
        'Tái tạo môi trường phát triển',
        'Một lỗi local được tái tạo được nhờ runtime, environment variable và port rõ ràng.',
        '런타임, 환경 변수, 포트를 명확히 해 로컬 오류를 재현할 수 있습니다.',
        'Một người chạy được app, người khác lại gặp connection refused ngay sau clone repo.',
        '한 사람은 앱이 실행되지만 다른 사람은 저장소를 받은 직후 연결 오류를 만납니다.',
        '필요한 도구와 구성 값을 문서화하고 같은 절차로 실행 결과를 비교합니다.',
        'Tài liệu hóa công cụ và config cần thiết, rồi so sánh kết quả theo cùng một quy trình.',
        '# .env.local\nAPI_BASE_URL=http://localhost:8080/api\nPORT=5173',
        [
          term('로컬 환경', 'Môi trường local', 'Local environment'),
          term('런타임', 'Môi trường chạy', 'Runtime'),
          term('의존성 설치', 'Cài đặt dependency', 'Dependency installation'),
          term('환경 변수', 'Biến môi trường', 'Environment variable'),
          term('포트 번호', 'Số cổng', 'Port number'),
          term('구성 값', 'Giá trị cấu hình', 'Configuration value'),
          term('재현 절차', 'Quy trình tái tạo', 'Reproduction steps'),
          term('개발 도구', 'Công cụ phát triển', 'Development tool'),
        ],
      ),
      lesson(
        'devops-docker-image',
        '도커 이미지 만들기',
        'Tạo Docker image',
        'A production image is small, repeatable and separates build-time dependency from runtime.',
        '프로덕션 이미지를 작고 반복 가능하게 만들며 빌드 단계와 실행 단계를 분리합니다.',
        'Image backend quá lớn và mỗi build lại tải dependency từ đầu.',
        '백엔드 이미지가 너무 크고 매번 빌드할 때 의존성을 처음부터 다시 받습니다.',
        '베이스 이미지를 고르고 레이어 캐시를 활용한 뒤 다단계 빌드로 실행 이미지를 줄입니다.',
        'Chọn base image, tận dụng layer cache và giảm runtime image bằng multi-stage build.',
        'FROM maven:3.9 AS builder\nRUN mvn dependency:go-offline\nFROM eclipse-temurin:21-jre\nCOPY --from=builder /app/app.jar app.jar',
        [
          term('도커파일', 'Dockerfile', 'Dockerfile'),
          term('베이스 이미지', 'Base image', 'Base image'),
          term('레이어', 'Lớp image', 'Layer'),
          term('빌드 컨텍스트', 'Ngữ cảnh build', 'Build context'),
          term('이미지 태그', 'Nhãn image', 'Image tag'),
          term('레지스트리', 'Registry image', 'Container registry'),
          term('컨테이너화', 'Container hóa', 'Containerization'),
          term('다단계 빌드', 'Build nhiều giai đoạn', 'Multi-stage build'),
        ],
      ),
      lesson(
        'devops-compose-networking',
        '컴포즈 서비스 연결',
        'Kết nối service bằng Compose',
        'Services use internal DNS and only public entry points expose host ports.',
        '서비스는 내부 DNS로 통신하고 외부 진입점만 호스트 포트를 노출합니다.',
        'API, Mongo và Caddy chạy cùng VM nhưng port mapping gây xung đột và kết nối sai host.',
        'API, Mongo, Caddy가 같은 VM에서 실행되지만 포트 매핑 충돌과 잘못된 호스트 연결이 발생합니다.',
        '서비스 이름으로 내부 연결을 만들고 외부 포트는 필요한 프록시에만 할당합니다.',
        'Kết nối nội bộ bằng tên service, chỉ gán port ngoài cho proxy cần thiết.',
        'services:\n  api: { depends_on: [mongo] }\n  mongo: { image: mongo:7 }\n# api uses mongodb://mongo:27017/kapor',
        [
          term('컴포즈 파일', 'Tệp Compose', 'Compose file'),
          term('서비스 정의', 'Định nghĩa service', 'Service definition'),
          term('네트워크 별칭', 'Bí danh mạng', 'Network alias'),
          term('볼륨 마운트', 'Gắn volume', 'Volume mount'),
          term('서비스 의존성', 'Phụ thuộc service', 'Service dependency'),
          term('내부 포트', 'Cổng nội bộ', 'Internal port'),
          term('외부 포트', 'Cổng public', 'Published port'),
          term('컨테이너 네트워크', 'Mạng container', 'Container network'),
        ],
      ),
    ],
  },
  {
    id: `${PREFIX}-topic-devops-delivery`,
    domain: 'devops',
    title: '자동화 배포와 클라우드 진입점',
    titleVi: 'Triển khai tự động và điểm vào cloud',
    description: 'Dùng Git, CI và chiến lược release để đưa thay đổi lên production an toàn.',
    order: 20,
    tags: ['devops', 'ci-cd', 'git', 'release'],
    lessons: [
      lesson(
        'devops-branch-practice',
        '브랜치 전략과 변경 이력',
        'Chiến lược branch và lịch sử thay đổi',
        'A team can trace why a change exists and control which branch can reach production.',
        '팀이 변경 이유를 추적하고 어떤 브랜치가 프로덕션에 갈 수 있는지 제어합니다.',
        'Một hotfix vào main không được review khiến release khó truy vết.',
        '검토 없이 main에 핫픽스가 들어가 릴리스 추적이 어려워졌습니다.',
        '기본 브랜치를 보호하고 작업 브랜치와 명확한 커밋 메시지로 변경 이력을 남깁니다.',
        'Bảo vệ default branch, dùng working branch và commit message rõ ràng để lưu lịch sử.',
        'git checkout -b fix/login-refresh\ngit commit -m "fix(auth): rotate refresh token"\ngit push origin fix/login-refresh',
        [
          term('브랜치 전략', 'Chiến lược branch', 'Branch strategy'),
          term('기본 브랜치', 'Branch mặc định', 'Default branch'),
          term('작업 브랜치', 'Branch làm việc', 'Working branch'),
          term('커밋 메시지', 'Thông điệp commit', 'Commit message'),
          term('코드 소유자', 'Người sở hữu code', 'Code owner'),
          term('보호 규칙', 'Quy tắc bảo vệ', 'Protection rule'),
          term('변경 이력', 'Lịch sử thay đổi', 'Change history'),
          term('지속적 통합', 'Tích hợp liên tục', 'Continuous integration'),
        ],
      ),
      lesson(
        'devops-ci-quality-gates',
        'CI 파이프라인과 품질 게이트',
        'Pipeline CI và cổng chất lượng',
        'Every pull request receives the same build and test evidence before it can merge.',
        '모든 풀 리퀘스트가 병합 전에 동일한 빌드와 테스트 근거를 받습니다.',
        'Một lỗi type chỉ xuất hiện sau khi deploy vì CI không chạy frontend build.',
        'CI가 프론트엔드 빌드를 실행하지 않아 타입 오류가 배포 후에 발견됩니다.',
        '파이프라인 단계를 작은 검증으로 나누고 실패 알림이 담당자에게 바로 가게 합니다.',
        'Chia pipeline thành các kiểm tra nhỏ và gửi thông báo thất bại ngay đến người phụ trách.',
        'jobs:\n  verify:\n    steps: [checkout, setup-node, npm-ci, npm-run-build, npm-test]',
        [
          term('파이프라인', 'Pipeline', 'Pipeline'),
          term('빌드 단계', 'Bước build', 'Build step'),
          term('자동 테스트', 'Kiểm thử tự động', 'Automated test'),
          term('품질 게이트', 'Cổng chất lượng', 'Quality gate'),
          term('워크플로', 'Luồng công việc', 'Workflow'),
          term('러너', 'Máy chạy CI', 'Runner'),
          term('실패 알림', 'Thông báo thất bại', 'Failure notification'),
          term('아티팩트', 'Artifact build', 'Build artifact'),
        ],
      ),
      lesson(
        'devops-release-rollback',
        '릴리스와 롤백의 안전한 흐름',
        'Luồng release và rollback an toàn',
        'A release has a version, evidence, approver and a prepared route back to the prior version.',
        '릴리스에는 버전, 검증 근거, 승인자, 이전 버전으로 돌아갈 경로가 있어야 합니다.',
        'Phiên bản mới lỗi đăng nhập sau khi 10% traffic đã vào production.',
        '새 버전이 트래픽 10%를 받은 뒤 로그인 오류를 일으킵니다.',
        '점진 배포 중 지표를 검증하고 기준을 넘으면 즉시 이전 버전으로 되돌립니다.',
        'Trong progressive rollout, kiểm tra metrics và quay lại phiên bản trước khi vượt ngưỡng.',
        'docker compose pull\ndocker compose up -d\n# if error rate rises: deploy image:previous',
        [
          term('시맨틱 버전', 'Phiên bản ngữ nghĩa', 'Semantic version'),
          term('릴리스 후보', 'Ứng viên phát hành', 'Release candidate'),
          term('변경 로그', 'Nhật ký thay đổi', 'Changelog'),
          term('릴리스 태그', 'Tag phát hành', 'Release tag'),
          term('배포 승인', 'Phê duyệt triển khai', 'Deployment approval'),
          term('배포 창', 'Khoảng thời gian triển khai', 'Deployment window'),
          term('점진 배포', 'Triển khai dần', 'Progressive delivery'),
          term('롤백', 'Hoàn nguyên phiên bản', 'Rollback'),
        ],
      ),
    ],
  },
  {
    id: `${PREFIX}-topic-agile-discovery`,
    domain: 'agile',
    title: '제품 발견과 백로그 설계',
    titleVi: 'Khám phá sản phẩm và thiết kế backlog',
    description: 'Khởi đầu từ vấn đề người dùng, viết user story và tạo acceptance criteria có thể kiểm chứng.',
    order: 10,
    tags: ['agile', 'product', 'discovery', 'backlog'],
    lessons: [
      lesson(
        'agile-user-problem',
        '사용자 문제를 발견하기',
        'Khám phá vấn đề người dùng',
        'Research begins with observable user pain rather than a solution the team already prefers.',
        '리서치는 팀이 이미 선호하는 해결책이 아니라 관찰 가능한 사용자 문제에서 시작합니다.',
        'Team muốn thêm leaderboard nhưng chưa biết người học có đang mất động lực vì điều đó hay không.',
        '팀이 리더보드를 추가하려 하지만 학습자가 정말 그 이유로 동기를 잃는지는 모릅니다.',
        '페르소나와 여정을 검증하고 인터뷰의 관찰을 문제 가설과 분리해 기록합니다.',
        'Xác minh persona và journey, ghi tách quan sát phỏng vấn khỏi giả thuyết vấn đề.',
        'Research note:\n- Observation: users stop after card 3\n- Hypothesis: progress feedback is unclear\n- Next: interview 5 learners',
        [
          term('사용자 문제', 'Vấn đề người dùng', 'User problem'),
          term('페르소나', 'Chân dung người dùng', 'Persona'),
          term('사용자 여정', 'Hành trình người dùng', 'User journey'),
          term('불편 사항', 'Điểm bất tiện', 'Pain point'),
          term('인터뷰', 'Phỏng vấn', 'Interview'),
          term('관찰', 'Quan sát', 'Observation'),
          term('문제 가설', 'Giả thuyết vấn đề', 'Problem hypothesis'),
          term('가치 제안', 'Đề xuất giá trị', 'Value proposition'),
        ],
      ),
      lesson(
        'agile-user-story',
        '사용자 스토리로 범위 나누기',
        'Chia phạm vi bằng user story',
        'A user story states who needs what outcome and remains small enough to deliver and learn from.',
        '사용자 스토리는 누가 어떤 결과를 원하는지 말하고 학습 가능한 작은 단위로 유지합니다.',
        '“Làm dashboard” quá lớn, không biết ai dùng và giá trị đầu tiên là gì.',
        '“대시보드를 만든다”는 너무 커서 누가 쓰고 첫 가치는 무엇인지 알 수 없습니다.',
        '역할, 목표, 기대 효과를 먼저 쓰고 스토리 맵으로 작은 백로그 항목을 만듭니다.',
        'Viết role, goal, outcome trước rồi tạo các backlog item nhỏ bằng story map.',
        'As a learner, I want to see today’s review count so that I can decide whether to continue studying.',
        [
          term('사용자 스토리', 'User story', 'User story'),
          term('역할', 'Vai trò', 'Role'),
          term('목표', 'Mục tiêu', 'Goal'),
          term('기대 효과', 'Kết quả mong đợi', 'Expected outcome'),
          term('스토리 맵', 'Bản đồ user story', 'Story map'),
          term('작은 단위', 'Đơn vị nhỏ', 'Small slice'),
          term('범위', 'Phạm vi', 'Scope'),
          term('백로그 항목', 'Hạng mục backlog', 'Backlog item'),
        ],
      ),
      lesson(
        'agile-acceptance-criteria',
        '수용 기준을 명확히 하기',
        'Làm rõ acceptance criteria',
        'Acceptance criteria make a shared, testable definition of finished work before development begins.',
        '수용 기준은 개발 전에 완료된 작업의 공통되고 테스트 가능한 정의를 만듭니다.',
        'Developer hiểu “hiển thị tiến độ”, QA lại hiểu cần xử lý cả trường hợp chưa có dữ liệu.',
        '개발자는 “진행률 표시”만 이해하지만 QA는 데이터가 없을 때도 처리해야 한다고 봅니다.',
        '예외 경우와 검증 방법을 포함해 관찰 가능한 완료 조건을 함께 작성합니다.',
        'Cùng viết điều kiện hoàn thành quan sát được, gồm cả ngoại lệ và cách xác minh.',
        'Given no reviews today\nWhen the dashboard opens\nThen show “0 cards reviewed” and a start-learning action.',
        [
          term('수용 기준', 'Tiêu chí chấp nhận', 'Acceptance criteria'),
          term('완료 조건', 'Điều kiện hoàn thành', 'Done condition'),
          term('예외 경우', 'Trường hợp ngoại lệ', 'Edge case'),
          term('테스트 가능한 문장', 'Câu có thể kiểm thử', 'Testable statement'),
          term('예시 매핑', 'Ánh xạ bằng ví dụ', 'Example mapping'),
          term('공동 이해', 'Hiểu biết chung', 'Shared understanding'),
          term('명확성', 'Tính rõ ràng', 'Clarity'),
          term('검증 방법', 'Phương pháp xác minh', 'Verification method'),
        ],
      ),
    ],
  },
  {
    id: `${PREFIX}-topic-agile-delivery`,
    domain: 'agile',
    title: '스프린트 실행과 팀 협업',
    titleVi: 'Thực thi sprint và hợp tác đội nhóm',
    description: 'Ưu tiên backlog, điều phối daily và biến review/retro thành cải tiến thực tế.',
    order: 20,
    tags: ['agile', 'sprint', 'planning', 'collaboration'],
    lessons: [
      lesson(
        'agile-prioritization',
        '우선순위와 추정으로 계획하기',
        'Lập kế hoạch bằng ưu tiên và ước lượng',
        'The team balances impact, effort, urgency and risk instead of choosing work by the loudest request.',
        '팀은 가장 큰 목소리의 요청이 아니라 영향도, 노력도, 긴급도, 위험도를 균형 있게 봅니다.',
        'Có ba việc: sửa crash, thêm animation và chuẩn hóa API; sprint chỉ còn dung lượng cho một phần.',
        '크래시 수정, 애니메이션 추가, API 정리 세 작업이 있지만 스프린트 용량은 제한적입니다.',
        '영향과 위험을 공개적으로 비교하고 스토리 포인트로 불확실성을 드러냅니다.',
        'So sánh công khai impact/risk và dùng story point để làm lộ độ không chắc chắn.',
        '| Item | Impact | Effort | Risk |\n| Crash fix | High | 2 | High |\n| Animation | Low | 5 | Low |',
        [
          term('우선순위', 'Mức ưu tiên', 'Priority'),
          term('영향도', 'Mức độ ảnh hưởng', 'Impact'),
          term('노력도', 'Mức độ nỗ lực', 'Effort'),
          term('추정치', 'Ước lượng', 'Estimate'),
          term('스토리 포인트', 'Điểm story', 'Story point'),
          term('긴급도', 'Mức độ khẩn cấp', 'Urgency'),
          term('의존 관계', 'Quan hệ phụ thuộc', 'Dependency relationship'),
          term('위험도', 'Mức độ rủi ro', 'Risk level'),
        ],
      ),
      lesson(
        'agile-daily-alignment',
        '데일리에서 진행 상황 공유',
        'Chia sẻ tiến độ trong daily',
        'Daily stand-up is a short synchronization that reveals blockers and creates a concrete next action.',
        '데일리 스탠드업은 차단 요인을 드러내고 다음 행동을 만드는 짧은 동기화 시간입니다.',
        'Một task API chờ schema từ team khác nhưng thông tin này không xuất hiện trên board.',
        'API 작업이 다른 팀의 스키마를 기다리지만 보드에 이 정보가 나타나지 않습니다.',
        '어제 한 일과 오늘 할 일을 짧게 말한 뒤 장애물에는 지원 요청과 담당자를 연결합니다.',
        'Nói ngắn việc hôm qua/hôm nay, rồi nối blocker với yêu cầu hỗ trợ và người phụ trách.',
        'Daily update:\nYesterday: API contract drafted\nToday: implement endpoint\nBlocker: waiting for schema approval from data team',
        [
          term('스프린트 목표', 'Mục tiêu sprint', 'Sprint goal'),
          term('데일리 스탠드업', 'Daily stand-up', 'Daily stand-up'),
          term('진행 상황', 'Tiến độ', 'Progress status'),
          term('어제 한 일', 'Việc đã làm hôm qua', 'Yesterday’s work'),
          term('오늘 할 일', 'Việc làm hôm nay', 'Today’s plan'),
          term('장애물', 'Trở ngại', 'Blocker'),
          term('동기화', 'Đồng bộ thông tin', 'Synchronization'),
          term('지원 요청', 'Yêu cầu hỗ trợ', 'Support request'),
        ],
      ),
      lesson(
        'agile-review-retro',
        '리뷰와 회고를 개선으로 연결하기',
        'Biến review và retro thành cải tiến',
        'A demo validates outcomes with stakeholders; a retrospective produces owned, measurable experiments.',
        '데모는 이해관계자와 결과를 검증하고 회고는 담당자와 측정 가능한 실험을 만듭니다.',
        'Sprint kết thúc, team nói “giao tiếp cần tốt hơn” nhưng không ai biết phải đổi điều gì.',
        '스프린트가 끝난 뒤 팀은 “소통을 개선하자”고 말하지만 무엇을 바꿀지 모릅니다.',
        '리뷰에서 사용자 피드백을 수집하고 회고에서는 한 가지 행동 항목과 다음 실험을 정합니다.',
        'Thu feedback người dùng ở review; tại retro chọn một action item và thí nghiệm tiếp theo.',
        'Retro action: Before next sprint, add API contract review to definition of ready. Owner: Minh. Measure: contract changes after implementation.',
        [
          term('스프린트 리뷰', 'Sprint review', 'Sprint review'),
          term('데모', 'Trình diễn sản phẩm', 'Demo'),
          term('회고', 'Họp retrospective', 'Retrospective'),
          term('잘한 점', 'Điều làm tốt', 'What went well'),
          term('개선점', 'Điểm cần cải thiện', 'Improvement area'),
          term('행동 항목', 'Hạng mục hành động', 'Action item'),
          term('퍼실리테이터', 'Người điều phối', 'Facilitator'),
          term('다음 실험', 'Thí nghiệm tiếp theo', 'Next experiment'),
        ],
      ),
    ],
  },
];

function buildVocabulary(lessonId, lessonData) {
  return lessonData.terms.map((item, index) => ({
    _id: `${lessonId}-vocab-${String(index + 1).padStart(2, '0')}`,
    korean: item.korean,
    pronunciation: romanize(item.korean),
    vietnamese: item.vietnamese,
    english: item.english,
    context: [
      `현업 문장: ${lessonData.scenarioKo} 이 상황에서는 “${item.korean}”를 확인하고 근거를 팀에 공유합니다.`,
      `Tình huống: ${lessonData.scenarioVi} Trong tình huống này, hãy kiểm tra “${item.vietnamese}” và chia sẻ căn cứ với đội.`,
    ].join('\n'),
    codeSnippet: `${lessonData.snippet}\n\n// 핵심 용어 · ${item.korean} (${item.english})`,
    audioUrl: '',
  }));
}

function buildExercises(lessonId, lessonData, vocabulary) {
  const [first, second, third, fourth, fifth] = vocabulary;
  const shift = lessonId.length % 4;
  const options = [first.korean, second.korean, third.korean, fourth.korean];
  const rotatedOptions = options.slice(shift).concat(options.slice(0, shift));
  const actionOptions = [fifth.korean, vocabulary[5].korean, vocabulary[6].korean, vocabulary[7].korean];
  const rotatedActionOptions = actionOptions.slice((shift + 1) % 4).concat(actionOptions.slice(0, (shift + 1) % 4));

  return [
    {
      _id: `${lessonId}-exercise-01`,
      type: 'multiple_choice',
      question: `${lessonData.scenarioKo} 가장 먼저 확인해야 할 핵심 용어는 무엇입니까?`,
      questionVi: `${lessonData.scenarioVi} Thuật ngữ cốt lõi nào cần xác nhận trước?`,
      options: rotatedOptions,
      correctAnswer: first.korean,
    },
    {
      _id: `${lessonId}-exercise-02`,
      type: 'multiple_choice',
      question: `${lessonData.actionKo} 이 행동을 가장 잘 뒷받침하는 용어를 고르세요.`,
      questionVi: `${lessonData.actionVi} Chọn thuật ngữ hỗ trợ đúng nhất cho hành động này.`,
      options: rotatedActionOptions,
      correctAnswer: fifth.korean,
    },
    {
      _id: `${lessonId}-exercise-03`,
      type: 'fill_blank',
      question: `빈칸 채우기: “${second.english}”에 해당하는 한국어 기술 용어는 ____입니다.`,
      questionVi: `Điền vào chỗ trống: thuật ngữ tiếng Hàn tương ứng với “${second.english}” là ____.`,
      options: [],
      correctAnswer: second.korean,
    },
  ];
}

function markdownContent(lessonData) {
  return [
    '## 학습 목표',
    lessonData.goalKo,
    '',
    '## 현업 장면',
    lessonData.scenarioKo,
    '',
    '## 실습 흐름',
    `1. ${lessonData.actionKo}`,
    '2. 핵심 용어를 사용해 팀원에게 판단 근거를 설명합니다.',
    '3. 짧은 코드·설정·문서 예시로 결과를 검증합니다.',
    '',
    '## 말해 보기',
    `“${lessonData.actionKo}”`,
  ].join('\n');
}

function markdownContentVi(lessonData) {
  return [
    '## Mục tiêu',
    lessonData.goalVi,
    '',
    '## Tình huống thực tế',
    lessonData.scenarioVi,
    '',
    '## Quy trình thực hành',
    `1. ${lessonData.actionVi}`,
    '2. Dùng thuật ngữ trọng tâm để giải thích căn cứ ra quyết định cho đồng đội.',
    '3. Xác minh kết quả bằng ví dụ code, cấu hình hoặc tài liệu ngắn.',
    '',
    '## Câu nên nói',
    `“${lessonData.actionVi}”`,
  ].join('\n');
}

function assert(condition, message) {
  if (!condition) throw new Error(`Demo curriculum validation failed: ${message}`);
}

function assertUnique(values, description) {
  const normalized = values.map((value) => String(value).trim().normalize('NFC').toLowerCase());
  assert(new Set(normalized).size === normalized.length, `${description} must be unique`);
}

const topics = [];
const lessons = [];

curriculum.forEach((topicData) => {
  topics.push({
    _id: topicData.id,
    domain: topicData.domain,
    title: topicData.title,
    titleVi: topicData.titleVi,
    description: topicData.description,
    order: topicData.order,
    // Keep every demo topic reachable on a fresh account.
    prerequisiteTopicIds: [],
    tags: topicData.tags,
    isActive: true,
  });

  topicData.lessons.forEach((lessonData, index) => {
    const lessonId = `${PREFIX}-lesson-${lessonData.slug}`;
    const vocabulary = buildVocabulary(lessonId, lessonData);
    lessons.push({
      _id: lessonId,
      topicId: topicData.id,
      title: lessonData.title,
      titleVi: lessonData.titleVi,
      content: markdownContent(lessonData),
      contentVi: markdownContentVi(lessonData),
      order: index + 1,
      vocabulary,
      exercises: buildExercises(lessonId, lessonData, vocabulary),
    });
  });
});

// Guard against accidental regressions before changing any collection.
assert(topics.length === 8, `expected 8 topics, received ${topics.length}`);
assert(lessons.length === 24, `expected 24 lessons, received ${lessons.length}`);
assert(lessons.every((item) => item.vocabulary.length === CARDS_PER_LESSON), 'every lesson must contain 8 flashcards');
assert(lessons.every((item) => item.exercises.length === EXERCISES_PER_LESSON), 'every lesson must contain 3 exercises');
assert(lessons.every((item) => topics.some((topic) => topic._id === item.topicId)), 'every lesson topicId must exist');
assertUnique(topics.map((item) => item._id), 'topic IDs');
assertUnique(topics.map((item) => item.title), 'topic titles');
assertUnique(lessons.map((item) => item._id), 'lesson IDs');
assertUnique(lessons.map((item) => item.title), 'lesson titles');
assertUnique(lessons.flatMap((item) => item.vocabulary.map((word) => word._id)), 'flashcard IDs');
assertUnique(lessons.flatMap((item) => item.vocabulary.map((word) => word.korean)), 'Korean flashcard headwords');
assertUnique(lessons.flatMap((item) => item.exercises.map((exercise) => exercise._id)), 'exercise IDs');

lessons.forEach((item) => {
  item.vocabulary.forEach((word) => {
    assert(word.korean && word.pronunciation && word.vietnamese && word.english && word.context && word.codeSnippet,
      `incomplete vocabulary record ${word._id}`);
  });
  item.exercises.forEach((exercise) => {
    assert(['multiple_choice', 'fill_blank'].includes(exercise.type), `invalid exercise type ${exercise._id}`);
    assert(exercise.question && exercise.questionVi && exercise.correctAnswer, `incomplete exercise ${exercise._id}`);
    if (exercise.type === 'multiple_choice') {
      assert(exercise.options.includes(exercise.correctAnswer), `choice answer missing from options for ${exercise._id}`);
    } else {
      assert(exercise.options.length === 0, `fill_blank options must be empty for ${exercise._id}`);
    }
  });
});

// Remove only seed records. No user-created topics, lessons, accounts, or
// unrelated content is deleted. Progress for this demo catalog is reset so a
// repeated import begins from a clean demo state.
kaporDb.flashcard_progress.deleteMany({ lessonId: /^dv-(seed|demo)-lesson-/ });
kaporDb.learning_progress.deleteMany({ topicId: /^dv-(seed|demo)-topic-/ });
kaporDb.lessons.deleteMany({ _id: /^dv-(seed|demo)-lesson-/ });
kaporDb.topics.deleteMany({ _id: /^dv-(seed|demo)-topic-/ });

kaporDb.topics.insertMany(topics);
kaporDb.lessons.insertMany(lessons);
// This account has a deterministic ID, so every re-run resets only this demo
// account. It does not alter any other user or administrator.
kaporDb.users.replaceOne({ _id: DEMO_ADMIN._id }, DEMO_ADMIN, { upsert: true });

const cards = lessons.reduce((total, item) => total + item.vocabulary.length, 0);
const exercises = lessons.reduce((total, item) => total + item.exercises.length, 0);
print('✅ Kapor production-demo curriculum seeded successfully');
print(`   Topics: ${topics.length}`);
print(`   Lessons: ${lessons.length}`);
print(`   Unique Korean flashcards: ${cards}`);
print(`   Contextual exercises: ${exercises}`);
print(`   Demo admin email: ${DEMO_ADMIN.email}`);
curriculum.forEach((topicData) => {
  const topicLessons = lessons.filter((item) => item.topicId === topicData.id);
  print(`   ${topicData.domain.padEnd(8)} ${topicLessons.length} lessons · ${topicLessons.length * CARDS_PER_LESSON} cards`);
});
