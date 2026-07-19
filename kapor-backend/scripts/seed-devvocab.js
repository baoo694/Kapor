// DevVocab development seed
// Creates 5 Topics × 10 Lessons × 20 vocabulary flashcards = 1,000 cards.
// Safe to run repeatedly: only documents whose IDs start with `dv-seed-` are replaced.
//
// Usage:
//   mongosh "mongodb://localhost:27017/kapor" scripts/seed-devvocab.js
// Or from the repository root:
//   mongosh "${MONGODB_URI:-mongodb://localhost:27017/kapor}" kapor-backend/scripts/seed-devvocab.js

const kaporDb = db.getSiblingDB('kapor');

const topics = [
  {
    id: 'dv-seed-topic-frontend-foundations',
    domain: 'frontend',
    title: 'Frontend Foundations 용어',
    titleVi: 'Thuật ngữ nền tảng Frontend',
    description: 'Các thuật ngữ tiếng Hàn cần thiết khi xây dựng giao diện web.',
    tags: ['frontend', 'html', 'css', 'javascript'],
  },
  {
    id: 'dv-seed-topic-frontend-deployment',
    domain: 'frontend',
    title: 'Frontend Deployment 용어',
    titleVi: 'Thuật ngữ triển khai Frontend',
    description: 'Từ vựng dùng khi build, deploy và vận hành ứng dụng frontend.',
    tags: ['frontend', 'deployment', 'ci-cd'],
  },
  {
    id: 'dv-seed-topic-backend-api',
    domain: 'backend',
    title: 'Backend API & 서버 용어',
    titleVi: 'Thuật ngữ Backend API và Server',
    description: 'Các thuật ngữ tiếng Hàn cho API, server và dữ liệu backend.',
    tags: ['backend', 'api', 'server', 'database'],
  },
  {
    id: 'dv-seed-topic-devops',
    domain: 'devops',
    title: 'DevOps 핵심 용어',
    titleVi: 'Thuật ngữ cốt lõi DevOps',
    description: 'Từ vựng về container, hạ tầng và quy trình vận hành.',
    tags: ['devops', 'docker', 'kubernetes', 'cloud'],
  },
  {
    id: 'dv-seed-topic-agile',
    domain: 'agile',
    title: 'Agile 협업 용어',
    titleVi: 'Thuật ngữ cộng tác Agile',
    description: 'Ngôn ngữ tiếng Hàn sử dụng trong Sprint và phối hợp nhóm.',
    tags: ['agile', 'scrum', 'teamwork', 'planning'],
  },
];

const vocabularyCatalog = [
  ['구성 요소', 'gu-seong yo-so', 'Thành phần', 'Component'],
  ['배포', 'bae-po', 'Triển khai', 'Deployment'],
  ['서버', 'seo-beo', 'Máy chủ', 'Server'],
  ['오류', 'o-ryu', 'Lỗi', 'Error'],
  ['요청', 'yo-cheong', 'Yêu cầu', 'Request'],
  ['응답', 'eung-dap', 'Phản hồi', 'Response'],
  ['데이터베이스', 'de-i-teo-be-i-seu', 'Cơ sở dữ liệu', 'Database'],
  ['인증', 'in-jeung', 'Xác thực', 'Authentication'],
  ['권한', 'gwon-han', 'Quyền truy cập', 'Authorization'],
  ['성능', 'seong-neung', 'Hiệu năng', 'Performance'],
  ['테스트', 'te-seu-teu', 'Kiểm thử', 'Testing'],
  ['버전 관리', 'beo-jeon gwan-ri', 'Quản lý phiên bản', 'Version control'],
  ['저장소', 'jeo-jang-so', 'Kho lưu trữ', 'Repository'],
  ['분기', 'bun-gi', 'Nhánh mã nguồn', 'Branch'],
  ['병합', 'byeong-hap', 'Gộp mã', 'Merge'],
  ['컨테이너', 'keon-te-i-neo', 'Container', 'Container'],
  ['네트워크', 'ne-teu-wo-keu', 'Mạng', 'Network'],
  ['모니터링', 'mo-ni-teo-ring', 'Giám sát', 'Monitoring'],
  ['문서', 'mun-seo', 'Tài liệu', 'Documentation'],
  ['협업', 'hyeop-eop', 'Cộng tác', 'Collaboration'],
];

function lessonId(topicIndex, lessonIndex) {
  return `dv-seed-lesson-${topicIndex + 1}-${lessonIndex}`;
}

function vocabularyItem(topic, topicIndex, lessonIndex, vocabularyIndex) {
  const [korean, pronunciation, vietnamese, english] = vocabularyCatalog[vocabularyIndex];
  const lessonNumber = lessonIndex;
  return {
    // Spring Data maps an embedded object's Java `id` property from MongoDB `_id`.
    _id: `dv-seed-vocab-${topicIndex + 1}-${lessonNumber}-${vocabularyIndex + 1}`,
    korean,
    pronunciation,
    vietnamese,
    english,
    context: `${topic.titleVi} — bài ${lessonNumber}: ${vietnamese} trong ngữ cảnh công việc IT.`,
    codeSnippet: `// Lesson ${lessonNumber}: ${english}\nconst term = '${english.toLowerCase()}';`,
    audioUrl: '',
  };
}

function buildLesson(topic, topicIndex, lessonIndex) {
  const vocabulary = vocabularyCatalog.map((_, vocabularyIndex) =>
    vocabularyItem(topic, topicIndex, lessonIndex, vocabularyIndex),
  );
  return {
    _id: lessonId(topicIndex, lessonIndex),
    topicId: topic.id,
    title: `${topic.title} ${lessonIndex}`,
    titleVi: `${topic.titleVi} — Bài ${lessonIndex}`,
    content: `${topic.title} ${lessonIndex}의 핵심 IT 용어를 학습합니다.`,
    contentVi: `Học 20 thuật ngữ quan trọng của ${topic.titleVi}, bài ${lessonIndex}.`,
    order: lessonIndex,
    vocabulary,
    exercises: [],
  };
}

const seededTopicIds = topics.map((topic) => topic.id);
const seededLessonIds = [];
const lessons = [];

topics.forEach((topic, topicIndex) => {
  for (let lessonIndex = 1; lessonIndex <= 10; lessonIndex += 1) {
    seededLessonIds.push(lessonId(topicIndex, lessonIndex));
    lessons.push(buildLesson(topic, topicIndex, lessonIndex));
  }
});

// Delete only the deterministic seed records, never all Topics or Lessons.
kaporDb.lessons.deleteMany({ _id: { $in: seededLessonIds } });
kaporDb.topics.deleteMany({ _id: { $in: seededTopicIds } });

kaporDb.topics.insertMany(
  topics.map((topic, index) => ({
    _id: topic.id,
    domain: topic.domain,
    title: topic.title,
    titleVi: topic.titleVi,
    description: topic.description,
    order: index + 1,
    prerequisiteTopicIds: [],
    tags: topic.tags,
    isActive: true,
  })),
);
kaporDb.lessons.insertMany(lessons);

print('✅ DevVocab seed completed');
print(`   Topics: ${topics.length}`);
print(`   Lessons: ${lessons.length}`);
print(`   Vocabulary flashcards: ${lessons.length * vocabularyCatalog.length}`);
