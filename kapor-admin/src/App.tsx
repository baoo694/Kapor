// @ts-nocheck
import React, { useState, useRef, useEffect } from "react";
import {
  api,
  type AdminLessonPayload,
  type AdminTopicPayload,
  type AdminVideoPayload,
  type AdminScenarioPayload,
  type AdminDictionaryPayload,
  type AdminPronunciationPayload,
  type AdminPromptPayload,
  type LessonExercisePayload,
  type LessonVocabularyPayload,
} from "./services/api";
import {
  Flame, BarChart2, BookOpen, Brain, MessageSquare, Mic, Target,
  ChevronLeft, Lock, CheckCircle, Play, Pause, Volume2, Send,
  Lightbulb, Copy, Check, User, ArrowRight, Plus, X,
  Clock, Globe, Bell, Moon, Zap, Radio, Award, TrendingUp, Users, Video,
} from "lucide-react";
import {
  RadarChart, PolarGrid, PolarAngleAxis, Radar, ResponsiveContainer,
  LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid,
  BarChart, Bar, AreaChart, Area,
} from "recharts";

// ─── Types ─────────────────────────────────────────────────────────────────────

type Screen =
  | "login" | "onboarding"
  | "dashboard" | "devvocab" | "devvocab-lesson"
  | "membyte" | "membyte-review"
  | "profile" | "video"
  | "techtalk-select" | "techtalk" | "techtalk-result"
  | "pronunciation-list" | "pronunciation"
  | "honorifics";

type Lang = "vi" | "en";

// ─── Constants ──────────────────────────────────────────────────────────────────

const TEAL = "#2dd4bf";
const AMBER = "#fbbf24";

type TopicFormState = {
  title: string;
  titleVi: string;
  domain: string;
  description: string;
  order: string;
  prerequisiteTopicIds: string;
  tags: string;
  isActive: boolean;
};

const emptyTopicForm = (): TopicFormState => ({
  title: "",
  titleVi: "",
  domain: "frontend",
  description: "",
  order: "0",
  prerequisiteTopicIds: "",
  tags: "",
  isActive: true,
});

const commaSeparatedList = (value: string) => value
  .split(",")
  .map(item => item.trim())
  .filter(Boolean);

type LessonFormState = {
  topicId: string;
  title: string;
  titleVi: string;
  content: string;
  contentVi: string;
  order: string;
  vocabulary: LessonVocabularyPayload[];
  exercises: LessonExercisePayload[];
};

const emptyVocabulary = (): LessonVocabularyPayload => ({
  korean: "", pronunciation: "", vietnamese: "", english: "", context: "", codeSnippet: "", audioUrl: "",
});

const emptyExercise = (): LessonExercisePayload => ({
  type: "multiple_choice", question: "", questionVi: "", options: ["", ""], correctAnswer: "",
});

const emptyLessonForm = (topicId = ""): LessonFormState => ({
  topicId, title: "", titleVi: "", content: "", contentVi: "", order: "0", vocabulary: [], exercises: [],
});

// ─── i18n ───────────────────────────────────────────────────────────────────────

const i18n = {
  vi: {
    greeting: "Xin chào",
    streakDays: "ngày liên tiếp",
    recordLabel: "Kỷ lục",
    days: "ngày",
    progressTitle: "Tiến độ học tập",
    weekly: "Tuần", monthly: "Tháng",
    nextUp: "TIẾP THEO", explore: "KHÁM PHÁ",
    speakingLabel: "Nói", vocabLabel: "Từ vựng",
    listenLabel: "Nghe", roleplayLabel: "Roleplay",
    videoLab: "Video Lab", techTalkAI: "TechTalk AI",
    pronunciationNav: "Phát âm", honorificsNav: "Honorifics",
    tabDashboard: "Dashboard", tabDevVocab: "DevVocab",
    tabMemByte: "MemByte", tabProfile: "Hồ sơ",
    profileTitle: "Hồ sơ", settingsSection: "CÀI ĐẶT",
    notifications: "Thông báo nhắc nhở", ttsSpeed: "Tốc độ TTS",
    uiLanguage: "Ngôn ngữ UI", darkMode: "Giao diện tối",
    studyBtn: "Học",
    startReview: "Bắt đầu phiên ôn tập ngay",
    cardsToReview: "thẻ cần ôn tập",
    endSession: "Kết thúc", selectScenario: "Chọn kịch bản",
    exercisesTitle: "Bài tập phát âm",
    streakTitle: "Chuỗi ngày học",
    currentStreak: "Hiện tại", longestStreak: "Kỷ lục", freezeCount: "Đóng băng",
    closeBtn: "Đóng",
    todayCheck: "Hôm nay",
    yourDecks: "BỘ THẺ CỦA BẠN",
    sessionResult: "Kết quả phiên học",
    tryAgain: "Thử lại", goHome: "Về trang chủ",
    details: "CHI TIẾT", feedback: "NHẬN XÉT", improvements: "CẢI THIỆN",
  },
  en: {
    greeting: "Hello",
    streakDays: "day streak",
    recordLabel: "Record",
    days: "days",
    progressTitle: "Learning Progress",
    weekly: "Week", monthly: "Month",
    nextUp: "NEXT UP", explore: "EXPLORE",
    speakingLabel: "Speaking", vocabLabel: "Vocab",
    listenLabel: "Listening", roleplayLabel: "Roleplay",
    videoLab: "Video Lab", techTalkAI: "TechTalk AI",
    pronunciationNav: "Pronunciation", honorificsNav: "Honorifics",
    tabDashboard: "Dashboard", tabDevVocab: "DevVocab",
    tabMemByte: "MemByte", tabProfile: "Profile",
    profileTitle: "Profile", settingsSection: "SETTINGS",
    notifications: "Reminder Notifications", ttsSpeed: "TTS Speed",
    uiLanguage: "UI Language", darkMode: "Dark Mode",
    studyBtn: "Study",
    startReview: "Start review session now",
    cardsToReview: "cards to review",
    endSession: "End", selectScenario: "Select Scenario",
    exercisesTitle: "Pronunciation Exercises",
    streakTitle: "Learning Streak",
    currentStreak: "Current", longestStreak: "Record", freezeCount: "Freezes",
    closeBtn: "Close",
    todayCheck: "Today",
    yourDecks: "YOUR DECKS",
    sessionResult: "Session Results",
    tryAgain: "Try Again", goHome: "Home",
    details: "DETAILS", feedback: "FEEDBACK", improvements: "IMPROVEMENTS",
  },
} as const;

type I18nKey = keyof typeof i18n.vi;
function makeT(lang: Lang) {
  return (key: I18nKey): string => i18n[lang][key];
}

// ─── Mock Data ──────────────────────────────────────────────────────────────────

const radarData = [
  { metric: "Speaking", value: 72 },
  { metric: "Vocab", value: 85 },
  { metric: "Listening", value: 60 },
  { metric: "Roleplay", value: 78 },
];

const skillNodes = [
  { id: "html", title: "HTML & DOM 용어", titleVi: "Thuật ngữ HTML & DOM", domain: "frontend", locked: false, pct: 100, lessons: 5 },
  { id: "css", title: "CSS Grid & Flexbox 용어", titleVi: "Thuật ngữ CSS Grid & Flexbox", domain: "frontend", locked: false, pct: 65, lessons: 5 },
  { id: "deploy", title: "배포 & CI/CD 용어", titleVi: "Thuật ngữ Deployment & CI/CD", domain: "devops", locked: false, pct: 30, lessons: 6 },
  { id: "api", title: "REST API 설계 용어", titleVi: "Thuật ngữ thiết kế REST API", domain: "backend", locked: true, pct: 0, lessons: 4 },
  { id: "docker", title: "Docker & 컨테이너", titleVi: "Docker & Container", domain: "devops", locked: true, pct: 0, lessons: 5 },
];

const lessonVocab = [
  { korean: "방향", pronunciation: "banghyang", vietnamese: "Hướng", itContext: "CSS flex-direction property", code: ".box { flex-direction: row; }" },
  { korean: "정렬", pronunciation: "jeongnyeol", vietnamese: "Căn chỉnh", itContext: "CSS alignment", code: ".box { justify-content: center; }" },
  { korean: "배치", pronunciation: "baechi", vietnamese: "Bố cục", itContext: "CSS layout & positioning", code: ".box { display: grid; }" },
];

const flashDecks = [
  { id: "d1", name: "Frontend Deployment 용어", nameVi: "Từ vựng Deployment Frontend", domain: "frontend", cards: 25, due: 5, newCards: 3, color: TEAL, emoji: "🚀" },
  { id: "d2", name: "API & 서버 용어", nameVi: "Từ vựng API & Server", domain: "backend", cards: 18, due: 2, newCards: 1, color: "#a78bfa", emoji: "⚡" },
  { id: "d3", name: "DevOps 핵심 용어", nameVi: "Thuật ngữ cốt lõi DevOps", domain: "devops", cards: 30, due: 8, newCards: 5, color: "#fb923c", emoji: "🔧" },
];

const flashcards = [
  { korean: "오류", pronunciation: "oryu", vietnamese: "Lỗi (Error)", itContext: "Exception/Error in programming", example: "오류가 발생하면 로그를 확인하세요.", code: "try {\n  deploy();\n} catch (오류) {\n  console.log(오류);\n}" },
  { korean: "배포", pronunciation: "baepo", vietnamese: "Triển khai (Deploy)", itContext: "Software deployment", example: "새 버전을 배포했습니다.", code: "git push origin main\n# 배포 완료" },
  { korean: "서버", pronunciation: "seobeo", vietnamese: "Máy chủ (Server)", itContext: "Backend server", example: "서버가 다운되었습니다.", code: "const 서버 = express();\n서버.listen(3000);" },
];

const initMessages = [
  { id: "1", role: "ai" as const, content: "안녕하세요! 저는 김민수 팀장입니다. 오늘 긴급 장애 보고 연습을 시작할까요?", eval: null },
  { id: "2", role: "user" as const, content: "네, 안녕하세요. 서버에 오류가 발생했습니다.", eval: { grammar: 88, vocabulary: 75, politeness: 92 } },
  { id: "3", role: "ai" as const, content: "어떤 종류의 오류인지 구체적으로 설명해 주시겠어요?", eval: null },
];

const pronunciationWords = [
  { text: "서버", accuracy: "correct" as const },
  { text: "배포가", accuracy: "minor_error" as const },
  { text: "완료되었습니다", accuracy: "correct" as const },
];

const corrections = [
  { original: "나", corrected: "저", type: "pronoun", explanation: "나 (casual) → 저 (humble, formal)" },
  { original: "했어", corrected: "하였습니다", type: "verb_ending", explanation: "했어 (casual past) → 하였습니다 (formal past)" },
  { original: "너", corrected: "귀하", type: "pronoun", explanation: "너 (casual 'you') → 귀하 (formal 'you')" },
  { original: "확인해봐", corrected: "확인해 주시기 바랍니다", type: "verb_ending", explanation: "해봐 (casual) → 해 주시기 바랍니다 (formal)" },
];

const videoSubtitles = [
  { id: 0, ko: "서버리스 아키텍처에 대해 설명하겠습니다", vi: "Tôi sẽ giải thích về kiến trúc Serverless", tokens: ["서버리스", "아키텍처에", "대해", "설명하겠습니다"] },
  { id: 1, ko: "배포가 자동으로 처리됩니다", vi: "Việc triển khai được xử lý tự động", tokens: ["배포가", "자동으로", "처리됩니다"] },
  { id: 2, ko: "오류 처리는 중요한 부분입니다", vi: "Xử lý lỗi là phần quan trọng", tokens: ["오류", "처리는", "중요한", "부분입니다"] },
];

const waveHeights = [3, 8, 14, 22, 18, 12, 26, 20, 15, 9, 24, 17, 11, 28, 19, 13, 21, 7, 16, 25, 10, 23, 6, 20, 14];

const scenarios = [
  { id: "s1", name: "긴급 장애 보고", nameVi: "Báo cáo sự cố nghiêm trọng", persona: "김민수", role: "Tech Lead", company: "Naver", difficulty: "intermediate", domain: "backend", mission: "Report a critical production bug", missionVi: "Báo cáo lỗi nghiêm trọng trên production", avatar: "🧑‍💼", color: "#a78bfa" },
  { id: "s2", name: "코드 리뷰 요청", nameVi: "Yêu cầu Code Review", persona: "이지영", role: "Senior Dev", company: "Kakao", difficulty: "beginner", domain: "frontend", mission: "Request a code review for your pull request", missionVi: "Yêu cầu code review cho Pull Request của bạn", avatar: "👩‍💻", color: "#34d399" },
  { id: "s3", name: "스프린트 계획 미팅", nameVi: "Sprint Planning Meeting", persona: "박준호", role: "Scrum Master", company: "Coupang", difficulty: "advanced", domain: "agile", mission: "Participate in a sprint planning meeting", missionVi: "Tham gia cuộc họp lập kế hoạch sprint", avatar: "🧑‍🔧", color: AMBER },
];

type ScenarioType = typeof scenarios[0];

const finalEvaluation = {
  overallScore: 76,
  metrics: [
    { name: "Ngữ pháp", nameEn: "Grammar", score: 82, color: "#34d399" },
    { name: "Từ vựng", nameEn: "Vocabulary", score: 75, color: TEAL },
    { name: "Lịch sự", nameEn: "Politeness", score: 68, color: "#a78bfa" },
    { name: "Hoàn thành", nameEn: "Task Completion", score: 80, color: AMBER },
  ],
  feedbackVi: "Bạn sử dụng tốt cấu trúc câu formal. Cần cải thiện việc dùng kính ngữ đặc biệt trong báo cáo sự cố.",
  feedbackEn: "Good use of formal sentence structures. Improve special honorific usage in incident reports.",
  improvementsVi: ["Dùng -습니다 nhất quán hơn", "Học từ vựng chuyên ngành backend", "Luyện mẫu câu báo cáo sự cố"],
  improvementsEn: ["Use -습니다 more consistently", "Learn backend technical vocabulary", "Practice incident report templates"],
};

const pronunciationExercises = [
  { id: "p1", phrase: "서버 배포가 완료되었습니다", phraseVi: "Server deployment completed", difficulty: "medium", domain: "devops", attempts: 3, bestScore: 82 },
  { id: "p2", phrase: "코드 리뷰 부탁드립니다", phraseVi: "Please review the code", difficulty: "easy", domain: "agile", attempts: 5, bestScore: 91 },
  { id: "p3", phrase: "긴급 장애가 발생했습니다", phraseVi: "Critical outage occurred", difficulty: "hard", domain: "backend", attempts: 1, bestScore: 65 },
  { id: "p4", phrase: "스프린트 회고를 시작하겠습니다", phraseVi: "Sprint retrospective starting", difficulty: "hard", domain: "agile", attempts: 0, bestScore: 0 },
];

const streakCalendar = [
  "active","none","active","active","none","none",
  "active","active","none","active","active","active",
  "none","none","freeze",
  "active","active","active","active","active",
  "active","active","active","active","active",
  "active","active","active","active","active",
];

const videoQuiz = {
  questionKo: "서버리스 아키텍처의 주요 장점은 무엇인가요?",
  questionVi: "Ưu điểm chính của kiến trúc Serverless là gì?",
  options: [
    { text: "항상 저렴한 비용", textVi: "Chi phí luôn thấp", correct: false },
    { text: "자동 스케일링", textVi: "Tự động mở rộng quy mô", correct: true },
    { text: "더 빠른 개발 속도", textVi: "Tốc độ phát triển nhanh hơn", correct: false },
    { text: "쉬운 디버깅", textVi: "Dễ debug hơn", correct: false },
  ],
};

const generatedCards = [
  { ko: "마이크로서비스", vi: "Microservices", domain: "Architecture" },
  { ko: "컨테이너화", vi: "Containerization", domain: "DevOps" },
  { ko: "API 게이트웨이", vi: "API Gateway", domain: "Backend" },
];


const lessonExercises = [
  { id: "e1", type: "multiple_choice", question: "다음 중 'flex-direction: row'의 의미는?", options: ["세로 방향", "가로 방향", "대각선 방향", "고정 위치"], correct: 1 },
  { id: "e2", type: "fill_blank", question: "아이템을 세로로 정렬하려면 flex-direction: ___을 사용합니다.", answer: "column" },
  { id: "e3", type: "matching", pairs: [["flex-start", "왼쪽/위 정렬"], ["flex-end", "오른쪽/아래 정렬"], ["center", "가운데 정렬"]] as [string, string][] },
];

// ─── Shared UI ──────────────────────────────────────────────────────────────────

function StatusBar() {
  return (
    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "10px 20px 4px", fontFamily: "JetBrains Mono, monospace", fontSize: 11, color: "oklch(0.6 0.03 250)" }}>
      <span>9:41</span>
      <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
        <div style={{ display: "flex", gap: 2, alignItems: "flex-end" }}>
          {[4, 6, 8, 10].map((h, i) => <div key={i} style={{ width: 3, height: h, background: "currentColor", borderRadius: 1 }} />)}
        </div>
        <span>WiFi</span>
        <span>🔋</span>
      </div>
    </div>
  );
}

function ModalHeader({ title, onBack }: { title: string; onBack: () => void }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 10, padding: "10px 16px", borderBottom: "1px solid oklch(0.22 0.03 250)" }}>
      <button onClick={onBack} style={{ width: 32, height: 32, borderRadius: 10, border: "none", background: "oklch(0.17 0.025 250)", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
        <ChevronLeft size={18} color={TEAL} />
      </button>
      <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 15, color: "oklch(0.92 0.01 250)" }}>{title}</span>
    </div>
  );
}

function KCard({ children, style = {} }: { children: React.ReactNode; style?: React.CSSProperties }) {
  return (
    <div style={{ background: "oklch(0.14 0.025 250)", border: "1px solid oklch(0.22 0.03 250)", borderRadius: 14, padding: 14, ...style }}>
      {children}
    </div>
  );
}

function KBadge({ children, color = TEAL }: { children: React.ReactNode; color?: string }) {
  return (
    <span style={{ padding: "2px 8px", borderRadius: 999, fontSize: 10, fontWeight: 600, fontFamily: "JetBrains Mono, monospace", background: `${color}22`, color, border: `1px solid ${color}44` }}>
      {children}
    </span>
  );
}

function CircRing({ value, color, size = 68, label }: { value: number; color: string; size?: number; label: string }) {
  const r = (size - 10) / 2;
  const circ = 2 * Math.PI * r;
  const dash = (value / 100) * circ;
  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 4 }}>
      <div style={{ position: "relative", width: size, height: size }}>
        <svg width={size} height={size} style={{ transform: "rotate(-90deg)" }}>
          <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="oklch(0.22 0.03 250)" strokeWidth={5} />
          <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke={color} strokeWidth={5} strokeDasharray={`${dash} ${circ}`} strokeLinecap="round" />
        </svg>
        <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center" }}>
          <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: size * 0.22, color }}>{value}</span>
        </div>
      </div>
      <span style={{ fontSize: 9, color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>{label}</span>
    </div>
  );
}

// ─── Login ──────────────────────────────────────────────────────────────────────

function LoginScreen({ nav }: { nav: (s: Screen) => void }) {
  return (
    <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "0 28px", gap: 28 }}>
      <div style={{ textAlign: "center" }}>
        <div style={{ width: 76, height: 76, borderRadius: 24, background: `${TEAL}18`, border: `2px solid ${TEAL}44`, display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 14px", fontSize: 38 }}>🎓</div>
        <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 30, color: TEAL, margin: "0 0 4px" }}>KAPOR</h1>
        <p style={{ fontSize: 11, color: "oklch(0.40 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0, letterSpacing: 1 }}>KOREAN IT COMMUNICATION · AI</p>
      </div>
      <div style={{ textAlign: "center" }}>
        <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 20, color: "oklch(0.90 0.01 250)", margin: "0 0 6px" }}>Học IT tiếng Hàn</p>
        <p style={{ fontSize: 12, color: "oklch(0.48 0.03 250)", margin: 0, fontFamily: "Inter, sans-serif" }}>Dành cho lập trình viên Việt Nam</p>
      </div>
      <div style={{ width: "100%", display: "flex", flexDirection: "column", gap: 10 }}>
        <button onClick={() => nav("onboarding")} style={{ width: "100%", padding: "14px 0", borderRadius: 14, border: "none", background: "#fff", color: "#1a1a2e", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 15, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", gap: 10 }}>
          <span style={{ fontFamily: "serif", fontWeight: 700, fontSize: 18, color: "#4285f4" }}>G</span>
          Đăng nhập với Google
        </button>
        <button style={{ width: "100%", padding: "13px 0", borderRadius: 14, border: `1px solid ${TEAL}40`, background: `${TEAL}10`, color: TEAL, fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, cursor: "pointer" }}>
          Đăng ký tài khoản mới
        </button>
      </div>
      <div style={{ display: "flex", gap: 16, flexWrap: "wrap", justifyContent: "center" }}>
        {["IT Flashcards", "AI Roleplay", "발음 Lab", "Honorifics"].map(f => (
          <span key={f} style={{ fontSize: 10, color: "oklch(0.32 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>· {f}</span>
        ))}
      </div>
    </div>
  );
}

// ─── Onboarding ─────────────────────────────────────────────────────────────────

function OnboardingScreen({ nav }: { nav: (s: Screen) => void }) {
  const [step, setStep] = useState(0);
  const [goals, setGoals] = useState<string[]>([]);
  const [dailyGoal, setDailyGoal] = useState("10");

  const steps = ["Mục tiêu", "Kế hoạch"];
  const goalOptions = ["사무적 한국어 (Giao tiếp văn phòng)", "IT 전문 용어 (Thuật ngữ IT)", "인터뷰 준비 (Phỏng vấn)", "직장 생활 (Sinh hoạt công sở)"];
  const dailyOptions = ["5", "10", "15", "30"];
  const canNext = step === 0 ? goals.length > 0 : !!dailyGoal;
  const handleNext = () => step < 1 ? setStep(s => s + 1) : nav("dashboard");

  return (
    <div style={{ flex: 1, display: "flex", flexDirection: "column" }}>
      <div style={{ padding: "16px 20px 0" }}>
        <div style={{ display: "flex", gap: 6, marginBottom: 20 }}>
          {steps.map((_, i) => (
            <div key={i} style={{ flex: 1, height: 3, borderRadius: 2, background: i <= step ? TEAL : "oklch(0.22 0.03 250)", transition: "background 0.3s" }} />
          ))}
        </div>
        <p style={{ fontSize: 10, color: TEAL, fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 2px" }}>{steps[step].toUpperCase()} · {step + 1}/{steps.length}</p>
      </div>
      <div style={{ flex: 1, overflowY: "auto", padding: "8px 20px 16px", scrollbarWidth: "none" }}>
        {step === 0 && (
          <>
            <h2 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 20, color: "oklch(0.92 0.01 250)", margin: "0 0 4px" }}>Bạn muốn học gì?</h2>
            <p style={{ fontSize: 12, color: "oklch(0.50 0.03 250)", margin: "0 0 16px" }}>Chọn ít nhất một mục tiêu học tập</p>
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {goalOptions.map(g => {
                const sel = goals.includes(g);
                return (
                  <button key={g} onClick={() => setGoals(p => p.includes(g) ? p.filter(x => x !== g) : [...p, g])} style={{ padding: "13px 16px", borderRadius: 12, border: `1px solid ${sel ? TEAL : "oklch(0.25 0.03 250)"}`, background: sel ? `${TEAL}15` : "oklch(0.14 0.025 250)", display: "flex", alignItems: "center", justifyContent: "space-between", cursor: "pointer" }}>
                    <span style={{ fontFamily: "Inter, sans-serif", fontSize: 13, color: sel ? TEAL : "oklch(0.80 0.01 250)", textAlign: "left" }}>{g}</span>
                    {sel && <Check size={15} color={TEAL} />}
                  </button>
                );
              })}
            </div>
          </>
        )}
        {step === 1 && (
          <>
            <h2 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 20, color: "oklch(0.92 0.01 250)", margin: "0 0 4px" }}>Mục tiêu hàng ngày?</h2>
            <p style={{ fontSize: 12, color: "oklch(0.50 0.03 250)", margin: "0 0 16px" }}>Học bao nhiêu phút mỗi ngày?</p>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
              {dailyOptions.map(d => {
                const sel = dailyGoal === d;
                return (
                  <button key={d} onClick={() => setDailyGoal(d)} style={{ padding: "22px 0", borderRadius: 12, border: `1px solid ${sel ? TEAL : "oklch(0.25 0.03 250)"}`, background: sel ? `${TEAL}15` : "oklch(0.14 0.025 250)", cursor: "pointer", textAlign: "center" }}>
                    <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 26, color: sel ? TEAL : "oklch(0.55 0.02 250)", margin: 0 }}>{d}m</p>
                    <p style={{ fontSize: 10, color: "oklch(0.40 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: "4px 0 0" }}>/ngày</p>
                  </button>
                );
              })}
            </div>
          </>
        )}
      </div>
      <div style={{ padding: "10px 20px 20px", display: "flex", gap: 10 }}>
        <button onClick={() => nav("dashboard")} style={{ padding: "12px 0", borderRadius: 12, border: "1px solid oklch(0.25 0.03 250)", background: "none", color: "oklch(0.48 0.03 250)", fontFamily: "Outfit, sans-serif", fontSize: 13, cursor: "pointer", flex: 1 }}>
          Bỏ qua
        </button>
        <button onClick={handleNext} disabled={!canNext} style={{ padding: "12px 0", borderRadius: 12, border: "none", background: canNext ? TEAL : "oklch(0.22 0.03 250)", color: canNext ? "#000" : "oklch(0.38 0.03 250)", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, cursor: canNext ? "pointer" : "default", flex: 2 }}>
          {step < 2 ? "Tiếp theo →" : "Bắt đầu học! 🚀"}
        </button>
      </div>
    </div>
  );
}

// ─── Dashboard ──────────────────────────────────────────────────────────────────

function DashboardScreen({ nav, lang }: { nav: (s: Screen) => void; lang: Lang }) {
  const t = makeT(lang);
  const [period, setPeriod] = useState<"weekly" | "monthly">("weekly");
  const [showStreak, setShowStreak] = useState(false);

  const quickNav = [
    { id: "video" as Screen, icon: Play, label: t("videoLab"), sub: "Phim kỹ thuật", color: "#fb923c" },
    { id: "techtalk-select" as Screen, icon: MessageSquare, label: t("techTalkAI"), sub: "Roleplay IT", color: "#a78bfa" },
    { id: "pronunciation-list" as Screen, icon: Mic, label: t("pronunciationNav"), sub: "Luyện giọng", color: "#34d399" },
    { id: "honorifics" as Screen, icon: Target, label: t("honorificsNav"), sub: "Ngữ pháp tôn kính", color: AMBER },
  ];

  return (
    <div style={{ flex: 1, overflowY: "auto", scrollbarWidth: "none", position: "relative" }}>
      <div style={{ padding: "12px 20px 8px" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
          <div>
            <p style={{ fontSize: 11, color: "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>{t("greeting")} 👋</p>
            <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 22, color: "oklch(0.95 0.01 250)", margin: "2px 0 0" }}>Nguyễn Văn A</h1>
          </div>
          <button onClick={() => nav("profile")} style={{ width: 36, height: 36, borderRadius: "50%", background: `${TEAL}20`, border: `1px solid ${TEAL}44`, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <User size={15} color={TEAL} />
          </button>
        </div>
      </div>

      {/* Streak card — tappable */}
      <div style={{ padding: "0 16px 12px" }}>
        <button onClick={() => setShowStreak(true)} style={{ width: "100%", border: "none", background: "none", padding: 0, cursor: "pointer", textAlign: "left" }}>
          <KCard style={{ display: "flex", alignItems: "center", gap: 14 }}>
            <div style={{ position: "relative" }}>
              <div style={{ width: 48, height: 48, borderRadius: 14, background: `${AMBER}22`, display: "flex", alignItems: "center", justifyContent: "center" }}>
                <Flame size={22} color={AMBER} />
              </div>
              <div style={{ position: "absolute", top: -4, right: -4, width: 16, height: 16, borderRadius: "50%", background: TEAL, display: "flex", alignItems: "center", justifyContent: "center" }}>
                <Check size={9} color="#000" />
              </div>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ display: "flex", alignItems: "baseline", gap: 6 }}>
                <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 32, color: AMBER }}>15</span>
                <span style={{ fontSize: 13, color: "oklch(0.55 0.03 250)" }}>{t("streakDays")}</span>
              </div>
              <p style={{ fontSize: 10, color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>{t("recordLabel")}: 30 {t("days")}</p>
            </div>
            <KBadge color={TEAL}>{t("todayCheck")} ✓</KBadge>
          </KCard>
        </button>
      </div>

      {/* Progress */}
      <div style={{ padding: "0 16px 12px" }}>
        <KCard>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
            <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 13, color: "oklch(0.85 0.01 250)" }}>{t("progressTitle")}</span>
            <div style={{ display: "flex", gap: 4 }}>
              {(["weekly", "monthly"] as const).map(p => (
                <button key={p} onClick={() => setPeriod(p)} style={{ padding: "2px 8px", borderRadius: 6, border: "none", cursor: "pointer", fontFamily: "JetBrains Mono, monospace", fontSize: 10, background: period === p ? TEAL : "oklch(0.20 0.03 250)", color: period === p ? "#000" : "oklch(0.50 0.03 250)" }}>
                  {p === "weekly" ? t("weekly") : t("monthly")}
                </button>
              ))}
            </div>
          </div>
          <ResponsiveContainer width="100%" height={150}>
            <RadarChart id="radar-dashboard" data={radarData} outerRadius={50}>
              <PolarGrid key="pg" stroke="oklch(0.22 0.03 250)" />
              <PolarAngleAxis key="pa" dataKey="metric" tick={{ fill: "oklch(0.50 0.03 250)", fontSize: 10, fontFamily: "JetBrains Mono, monospace" }} />
              <Radar key="r" dataKey="value" stroke={TEAL} fill={TEAL} fillOpacity={0.2} strokeWidth={2} />
            </RadarChart>
          </ResponsiveContainer>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 6, marginTop: 4 }}>
            {([["Nói", 72, "#a78bfa"], ["Từ vựng", 85, TEAL], ["Nghe", 60, "#fb923c"], ["Roleplay", 78, "#34d399"]] as [string, number, string][]).map(([lbl, val, col]) => (
              <div key={lbl} style={{ textAlign: "center" }}>
                <div style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 15, color: col }}>{val}</div>
                <div style={{ fontSize: 9, color: "oklch(0.40 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>{lbl}</div>
              </div>
            ))}
          </div>
        </KCard>
      </div>

      {/* Recommendation */}
      <div style={{ padding: "0 16px 12px" }}>
        <div style={{ borderRadius: 14, padding: 14, background: `${TEAL}12`, border: `1px solid ${TEAL}30` }}>
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <div style={{ width: 36, height: 36, borderRadius: 10, background: `${TEAL}28`, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              <Zap size={17} color={TEAL} />
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <p style={{ fontSize: 9, color: TEAL, fontFamily: "JetBrains Mono, monospace", margin: "0 0 2px", letterSpacing: 1 }}>{t("nextUp")}</p>
              <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 13, color: "oklch(0.92 0.01 250)", margin: 0 }}>CSS Grid & Flexbox 용어</p>
              <p style={{ fontSize: 11, color: "oklch(0.50 0.03 250)", margin: "2px 0 0" }}>15 từ vựng cần ôn tập hôm nay</p>
            </div>
            <button onClick={() => nav("devvocab")} style={{ padding: "6px 12px", borderRadius: 8, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 12, cursor: "pointer", display: "flex", alignItems: "center", gap: 4, flexShrink: 0 }}>
              {t("studyBtn")} <ArrowRight size={12} />
            </button>
          </div>
        </div>
      </div>

      {/* Quick nav */}
      <div style={{ padding: "0 16px 20px" }}>
        <p style={{ fontSize: 10, color: "oklch(0.40 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, marginBottom: 10 }}>{t("explore")}</p>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
          {quickNav.map(({ id, icon: Icon, label, sub, color }) => (
            <button key={id} onClick={() => nav(id)} style={{ borderRadius: 14, padding: 14, background: `${color}10`, border: `1px solid ${color}28`, cursor: "pointer", textAlign: "left" }}>
              <div style={{ width: 32, height: 32, borderRadius: 9, background: `${color}22`, display: "flex", alignItems: "center", justifyContent: "center", marginBottom: 8 }}>
                <Icon size={15} color={color} />
              </div>
              <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 13, color: "oklch(0.90 0.01 250)", margin: "0 0 2px" }}>{label}</p>
              <p style={{ fontSize: 10, color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>{sub}</p>
            </button>
          ))}
        </div>
      </div>

      {/* Streak Modal */}
      {showStreak && (
        <div style={{ position: "absolute", inset: 0, background: "rgba(0,0,0,0.72)", zIndex: 100, display: "flex", alignItems: "flex-end" }}>
          <div style={{ width: "100%", borderRadius: "20px 20px 0 0", background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.22 0.03 250)", borderBottom: "none", padding: "20px 20px 28px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
              <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 16, color: "oklch(0.92 0.01 250)" }}>{t("streakTitle")}</span>
              <button onClick={() => setShowStreak(false)} style={{ width: 28, height: 28, borderRadius: "50%", background: "oklch(0.20 0.03 250)", border: "none", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <X size={13} color="oklch(0.55 0.03 250)" />
              </button>
            </div>
            <div style={{ display: "flex", gap: 10, marginBottom: 18 }}>
              {([["currentStreak", "15", AMBER], ["longestStreak", "30", "#34d399"], ["freezeCount", "2", "#60a5fa"]] as [I18nKey, string, string][]).map(([key, val, col]) => (
                <div key={key} style={{ flex: 1, borderRadius: 10, padding: "10px 6px", background: `${col}10`, border: `1px solid ${col}28`, textAlign: "center" }}>
                  <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 22, color: col, margin: 0 }}>{val}</p>
                  <p style={{ fontSize: 9, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: "2px 0 0" }}>{t(key)}</p>
                </div>
              ))}
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(7, 1fr)", gap: 4, marginBottom: 18 }}>
              {streakCalendar.map((status, i) => (
                <div key={i} style={{ aspectRatio: "1", borderRadius: 5, background: status === "active" ? `${TEAL}25` : status === "freeze" ? `${AMBER}25` : "oklch(0.18 0.03 250)", border: i === 29 ? `1.5px solid ${TEAL}` : `1px solid ${status === "active" ? `${TEAL}44` : status === "freeze" ? `${AMBER}44` : "oklch(0.22 0.03 250)"}`, display: "flex", alignItems: "center", justifyContent: "center" }}>
                  {status === "active" && <div style={{ width: 5, height: 5, borderRadius: "50%", background: TEAL }} />}
                  {status === "freeze" && <div style={{ width: 5, height: 5, borderRadius: "50%", background: AMBER }} />}
                </div>
              ))}
            </div>
            <button onClick={() => setShowStreak(false)} style={{ width: "100%", padding: "11px 0", borderRadius: 12, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, cursor: "pointer" }}>
              {t("closeBtn")}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

// ─── DevVocab ───────────────────────────────────────────────────────────────────

function DevVocabScreen({ nav, lang }: { nav: (s: Screen) => void; lang: Lang }) {
  const [domain, setDomain] = useState("all");
  const [showSummarizer, setShowSummarizer] = useState(false);
  const [summPhase, setSummPhase] = useState<"input" | "loading" | "results">("input");
  const [summText, setSummText] = useState("");

  useEffect(() => {
    if (summPhase !== "loading") return;
    const id = setTimeout(() => setSummPhase("results"), 2200);
    return () => clearTimeout(id);
  }, [summPhase]);

  const domains = ["all", "frontend", "backend", "devops", "agile"];
  const filtered = domain === "all" ? skillNodes : skillNodes.filter(n => n.domain === domain);
  const domainColor = (d: string) => d === "frontend" ? TEAL : d === "devops" ? "#fb923c" : d === "agile" ? "#34d399" : "#a78bfa";

  return (
    <div style={{ flex: 1, overflowY: "auto", scrollbarWidth: "none", display: "flex", flexDirection: "column", position: "relative" }}>
      <div style={{ padding: "14px 16px 8px" }}>
        <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 20, color: "oklch(0.95 0.01 250)", margin: "0 0 2px" }}>DevVocab</h1>
        <p style={{ fontSize: 10, color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>Từ vựng IT chuyên ngành tiếng Hàn</p>
      </div>
      <div style={{ padding: "0 16px 12px", overflowX: "auto", scrollbarWidth: "none" }}>
        <div style={{ display: "flex", gap: 6, width: "max-content" }}>
          {domains.map(d => (
            <button key={d} onClick={() => setDomain(d)} style={{ padding: "5px 12px", borderRadius: 999, border: `1px solid ${domain === d ? TEAL : "oklch(0.25 0.03 250)"}`, background: domain === d ? TEAL : "oklch(0.16 0.025 250)", color: domain === d ? "#000" : "oklch(0.55 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 10, fontWeight: 600, cursor: "pointer", whiteSpace: "nowrap" }}>
              {d === "all" ? "Tất cả" : d.charAt(0).toUpperCase() + d.slice(1)}
            </button>
          ))}
        </div>
      </div>
      <div style={{ padding: "0 16px", flex: 1 }}>
        {filtered.map((node, i) => {
          const col = node.pct === 100 ? "#34d399" : TEAL;
          return (
            <div key={node.id} style={{ marginBottom: 8 }}>
              <button onClick={() => !node.locked && nav("devvocab-lesson")} style={{ width: "100%", borderRadius: 14, border: `1px solid ${node.pct === 100 ? "#34d39940" : node.locked ? "oklch(0.20 0.02 250)" : `${TEAL}30`}`, background: node.locked ? "oklch(0.12 0.02 250)" : "oklch(0.14 0.025 250)", padding: 14, textAlign: "left", cursor: node.locked ? "default" : "pointer", opacity: node.locked ? 0.6 : 1 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <div style={{ width: 40, height: 40, borderRadius: 12, background: node.locked ? "oklch(0.18 0.02 250)" : `${col}18`, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                    {node.locked ? <Lock size={15} color="oklch(0.38 0.03 250)" /> : node.pct === 100 ? <CheckCircle size={15} color="#34d399" /> : <BookOpen size={15} color={TEAL} />}
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 13, color: node.locked ? "oklch(0.38 0.03 250)" : "oklch(0.90 0.01 250)", margin: "0 0 2px" }}>{node.title}</p>
                    <p style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", margin: "0 0 6px", fontFamily: "JetBrains Mono, monospace" }}>{node.titleVi}</p>
                    {!node.locked && (
                      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                        <div style={{ flex: 1, height: 4, borderRadius: 2, background: "oklch(0.22 0.03 250)" }}>
                          <div style={{ height: "100%", borderRadius: 2, width: `${node.pct}%`, background: col }} />
                        </div>
                        <span style={{ fontSize: 9, color: col, fontFamily: "JetBrains Mono, monospace", flexShrink: 0 }}>{node.pct}%</span>
                      </div>
                    )}
                  </div>
                  <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 4 }}>
                    <KBadge color={domainColor(node.domain)}>{node.domain}</KBadge>
                    <span style={{ fontSize: 9, color: "oklch(0.40 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>{node.lessons} bài</span>
                  </div>
                </div>
              </button>
            </div>
          );
        })}
      </div>

      {/* FAB */}
      <div style={{ position: "sticky", bottom: 12, display: "flex", justifyContent: "flex-end", padding: "0 20px 8px" }}>
        <button onClick={() => { setShowSummarizer(true); setSummPhase("input"); setSummText(""); }} style={{ width: 48, height: 48, borderRadius: "50%", background: TEAL, border: "none", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", boxShadow: `0 4px 20px ${TEAL}44` }}>
          <Plus size={20} color="#000" />
        </button>
      </div>

      {/* SmartSummarizer Sheet */}
      {showSummarizer && (
        <div onClick={() => setShowSummarizer(false)} style={{ position: "absolute", inset: 0, background: "rgba(0,0,0,0.65)", zIndex: 50, display: "flex", alignItems: "flex-end" }}>
          <div onClick={e => e.stopPropagation()} style={{ width: "100%", borderRadius: "20px 20px 0 0", background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.22 0.03 250)", borderBottom: "none", padding: "20px 18px 28px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
              <div>
                <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 15, color: "oklch(0.90 0.01 250)" }}>SmartSummarizer</span>
                <span style={{ marginLeft: 8 }}><KBadge color={TEAL}>AI</KBadge></span>
              </div>
              <button onClick={() => setShowSummarizer(false)} style={{ width: 28, height: 28, borderRadius: "50%", background: "oklch(0.20 0.03 250)", border: "none", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <X size={13} color="oklch(0.55 0.03 250)" />
              </button>
            </div>
            {summPhase === "input" && (
              <>
                <textarea value={summText} onChange={e => setSummText(e.target.value)} placeholder={lang === "vi" ? "Dán URL bài viết IT tiếng Hàn hoặc nội dung văn bản..." : "Paste Korean IT article URL or text content..."} rows={4} style={{ width: "100%", borderRadius: 10, padding: "10px 12px", background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.25 0.03 250)", color: "oklch(0.85 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none", resize: "none", boxSizing: "border-box", marginBottom: 12 }} />
                <button onClick={() => setSummPhase("loading")} style={{ width: "100%", padding: "11px 0", borderRadius: 12, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }}>
                  <Zap size={15} /> {lang === "vi" ? "Tạo Flashcard AI" : "Generate Flashcards"}
                </button>
              </>
            )}
            {summPhase === "loading" && (
              <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                {[0, 1, 2].map(i => (
                  <div key={i} style={{ height: 58, borderRadius: 10, background: "oklch(0.16 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", animation: `kpulse 1.4s ease-in-out ${i * 0.18}s infinite` }} />
                ))}
                <p style={{ textAlign: "center", fontSize: 11, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: "4px 0 0" }}>
                  {lang === "vi" ? "AI đang phân tích văn bản..." : "AI is analyzing text..."}
                </p>
              </div>
            )}
            {summPhase === "results" && (
              <>
                <p style={{ fontSize: 9, color: TEAL, fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 10px" }}>3 FLASHCARD ĐÃ TẠO</p>
                <div style={{ display: "flex", flexDirection: "column", gap: 8, marginBottom: 12 }}>
                  {generatedCards.map((card, i) => (
                    <div key={i} style={{ borderRadius: 10, padding: "10px 14px", background: "oklch(0.16 0.025 250)", border: `1px solid ${TEAL}25`, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <div>
                        <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 16, color: TEAL }}>{card.ko}</span>
                        <span style={{ fontSize: 11, color: "oklch(0.55 0.03 250)", marginLeft: 8 }}>= {card.vi}</span>
                      </div>
                      <KBadge color={TEAL}>{card.domain}</KBadge>
                    </div>
                  ))}
                </div>
                <button onClick={() => setShowSummarizer(false)} style={{ width: "100%", padding: "11px 0", borderRadius: 12, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }}>
                  <CheckCircle size={15} /> {lang === "vi" ? "Lưu vào MemByte" : "Save to MemByte"}
                </button>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function DevVocabLessonScreen({ nav }: { nav: (s: Screen) => void }) {
  return (
    <div style={{ flex: 1, overflowY: "auto", scrollbarWidth: "none" }}>
      <ModalHeader title="CSS Grid & Flexbox 용어" onBack={() => nav("devvocab")} />
      <div style={{ padding: "14px 16px" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 16 }}>
          <div style={{ flex: 1, height: 5, borderRadius: 3, background: "oklch(0.22 0.03 250)" }}>
            <div style={{ width: "40%", height: "100%", borderRadius: 3, background: TEAL }} />
          </div>
          <span style={{ fontSize: 10, color: TEAL, fontFamily: "JetBrains Mono, monospace" }}>2 / 5 bài</span>
        </div>
        {lessonVocab.map((v, i) => (
          <KCard key={i} style={{ marginBottom: 12 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 8 }}>
              <div>
                <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 30, color: TEAL }}>{v.korean}</span>
                <span style={{ fontSize: 12, color: "oklch(0.45 0.03 250)", marginLeft: 8, fontFamily: "JetBrains Mono, monospace" }}>/{v.pronunciation}/</span>
              </div>
              <button style={{ width: 32, height: 32, borderRadius: 10, background: `${TEAL}18`, border: "none", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <Volume2 size={14} color={TEAL} />
              </button>
            </div>
            <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 16, color: "oklch(0.88 0.01 250)", margin: "0 0 3px" }}>{v.vietnamese}</p>
            <p style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", margin: "0 0 10px" }}>{v.itContext}</p>
            <div style={{ borderRadius: 10, padding: "10px 12px", background: "oklch(0.09 0.02 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
              <pre style={{ fontFamily: "JetBrains Mono, monospace", fontSize: 10, color: TEAL, margin: 0, whiteSpace: "pre-wrap" }}>{v.code}</pre>
            </div>
          </KCard>
        ))}
        <button style={{ width: "100%", padding: "12px 0", borderRadius: 12, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }}>
          <CheckCircle size={16} /> Hoàn thành bài học
        </button>
      </div>
    </div>
  );
}

// ─── MemByte ────────────────────────────────────────────────────────────────────

function MemByteScreen({ nav, lang }: { nav: (s: Screen) => void; lang: Lang }) {
  const t = makeT(lang);
  const totalDue = flashDecks.reduce((a, d) => a + d.due, 0);
  return (
    <div style={{ flex: 1, overflowY: "auto", scrollbarWidth: "none" }}>
      <div style={{ padding: "14px 16px 10px" }}>
        <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 20, color: "oklch(0.95 0.01 250)", margin: "0 0 2px" }}>MemByte</h1>
        <p style={{ fontSize: 10, color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>Flashcard thông minh · Thuật toán FSRS</p>
      </div>
      {totalDue > 0 && (
        <div style={{ padding: "0 16px 12px" }}>
          <button onClick={() => nav("membyte-review")} style={{ width: "100%", borderRadius: 14, padding: 14, background: `${TEAL}12`, border: `1px solid ${TEAL}40`, cursor: "pointer", display: "flex", alignItems: "center", gap: 12 }}>
            <div style={{ width: 40, height: 40, borderRadius: "50%", background: `${TEAL}28`, display: "flex", alignItems: "center", justifyContent: "center" }}>
              <Brain size={18} color={TEAL} />
            </div>
            <div style={{ flex: 1, textAlign: "left" }}>
              <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 15, color: "oklch(0.92 0.01 250)", margin: "0 0 2px" }}>{totalDue} {t("cardsToReview")}</p>
              <p style={{ fontSize: 11, color: "oklch(0.50 0.03 250)", margin: 0 }}>{t("startReview")}</p>
            </div>
            <ArrowRight size={18} color={TEAL} />
          </button>
        </div>
      )}
      <div style={{ padding: "0 16px 20px" }}>
        <p style={{ fontSize: 10, color: "oklch(0.40 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, marginBottom: 10 }}>{t("yourDecks")}</p>
        {flashDecks.map(deck => (
          <button key={deck.id} onClick={() => nav("membyte-review")} style={{ width: "100%", borderRadius: 14, border: `1px solid ${deck.color}28`, background: "oklch(0.14 0.025 250)", padding: 14, textAlign: "left", cursor: "pointer", marginBottom: 8, display: "block" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <div style={{ width: 40, height: 40, borderRadius: 12, background: `${deck.color}18`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 20, flexShrink: 0 }}>{deck.emoji}</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 13, color: "oklch(0.90 0.01 250)", margin: "0 0 2px" }}>{deck.name}</p>
                <p style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", margin: 0, fontFamily: "JetBrains Mono, monospace" }}>{deck.nameVi}</p>
              </div>
              <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 4 }}>
                {deck.due > 0 && <KBadge color="#f87171">{deck.due} due</KBadge>}
                {deck.newCards > 0 && <KBadge color="#60a5fa">{deck.newCards} new</KBadge>}
              </div>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 10 }}>
              <div style={{ flex: 1, height: 3, borderRadius: 2, background: "oklch(0.22 0.03 250)" }}>
                <div style={{ height: "100%", borderRadius: 2, width: `${((deck.cards - deck.due) / deck.cards) * 100}%`, background: deck.color }} />
              </div>
              <span style={{ fontSize: 9, color: "oklch(0.40 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>{deck.cards} thẻ</span>
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}

function MemByteReviewScreen({ nav }: { nav: (s: Screen) => void }) {
  const [idx, setIdx] = useState(0);
  const [flipped, setFlipped] = useState(false);
  const card = flashcards[idx % flashcards.length];
  const rate = () => { setFlipped(false); setTimeout(() => setIdx(i => i + 1), 200); };
  const ratings = [
    { label: "Again", color: "#f87171", time: "< 1m" },
    { label: "Hard", color: "#fb923c", time: "6m" },
    { label: "Good", color: "#34d399", time: "1d" },
    { label: "Easy", color: "#60a5fa", time: "4d" },
  ];
  return (
    <div style={{ flex: 1, display: "flex", flexDirection: "column" }}>
      <ModalHeader title={`Ôn tập · ${15 - (idx % 15)} còn lại`} onBack={() => nav("membyte")} />
      <div style={{ padding: "8px 16px 4px" }}>
        <div style={{ height: 4, borderRadius: 2, background: "oklch(0.22 0.03 250)" }}>
          <div style={{ height: "100%", borderRadius: 2, width: `${(idx % 15) / 15 * 100}%`, background: TEAL, transition: "width 0.3s" }} />
        </div>
      </div>
      <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "12px 16px", gap: 16 }}>
        <div onClick={() => setFlipped(f => !f)} style={{ width: "100%", height: 230, perspective: 1000, cursor: "pointer" }}>
          <div style={{ position: "relative", width: "100%", height: "100%", transformStyle: "preserve-3d", transform: flipped ? "rotateY(180deg)" : "rotateY(0deg)", transition: "transform 0.4s ease" }}>
            <div style={{ position: "absolute", inset: 0, backfaceVisibility: "hidden" }}>
              <div style={{ width: "100%", height: "100%", borderRadius: 18, border: `1px solid ${TEAL}30`, background: "oklch(0.16 0.03 250)", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 10 }}>
                <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 52, color: TEAL }}>{card.korean}</span>
                <span style={{ fontFamily: "JetBrains Mono, monospace", fontSize: 14, color: "oklch(0.50 0.03 250)" }}>/{card.pronunciation}/</span>
                <button onClick={e => e.stopPropagation()} style={{ width: 38, height: 38, borderRadius: "50%", background: `${TEAL}18`, border: "none", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
                  <Volume2 size={16} color={TEAL} />
                </button>
                <span style={{ fontSize: 10, color: "oklch(0.35 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>Nhấn để lật thẻ</span>
              </div>
            </div>
            <div style={{ position: "absolute", inset: 0, backfaceVisibility: "hidden", transform: "rotateY(180deg)" }}>
              <div style={{ width: "100%", height: "100%", borderRadius: 18, border: `1px solid ${TEAL}30`, background: "oklch(0.16 0.03 250)", display: "flex", flexDirection: "column", justifyContent: "center", padding: "18px 20px", gap: 8 }}>
                <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 24, color: "oklch(0.92 0.01 250)" }}>{card.vietnamese}</span>
                <p style={{ fontSize: 11, color: "oklch(0.50 0.03 250)", margin: 0 }}>{card.itContext}</p>
                <p style={{ fontSize: 11, color: "oklch(0.58 0.03 250)", fontStyle: "italic", margin: 0 }}>{card.example}</p>
                <div style={{ borderRadius: 8, padding: "8px 10px", background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
                  <pre style={{ fontFamily: "JetBrains Mono, monospace", fontSize: 9, color: TEAL, margin: 0, whiteSpace: "pre-wrap" }}>{card.code}</pre>
                </div>
              </div>
            </div>
          </div>
        </div>
        {flipped ? (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 8, width: "100%" }}>
            {ratings.map(r => (
              <button key={r.label} onClick={rate} style={{ borderRadius: 12, padding: "10px 4px", border: `1px solid ${r.color}40`, background: `${r.color}15`, cursor: "pointer", display: "flex", flexDirection: "column", alignItems: "center", gap: 3 }}>
                <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 12, color: r.color }}>{r.label}</span>
                <span style={{ fontSize: 9, color: "oklch(0.40 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>{r.time}</span>
              </button>
            ))}
          </div>
        ) : (
          <button onClick={() => setFlipped(true)} style={{ width: "100%", padding: "12px 0", borderRadius: 12, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, cursor: "pointer" }}>
            Xem đáp án
          </button>
        )}
      </div>
    </div>
  );
}

// ─── TechTalk Select ────────────────────────────────────────────────────────────

function TechTalkSelectScreen({ nav, lang, onSelect }: { nav: (s: Screen) => void; lang: Lang; onSelect: (s: ScenarioType) => void }) {
  const t = makeT(lang);
  return (
    <div style={{ flex: 1, display: "flex", flexDirection: "column", overflow: "hidden" }}>
      <ModalHeader title={t("selectScenario")} onBack={() => nav("dashboard")} />
      <div style={{ flex: 1, overflowY: "auto", padding: "14px 16px", scrollbarWidth: "none" }}>
        <p style={{ fontSize: 10, color: "oklch(0.40 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, marginBottom: 14 }}>CHỌN TÌNH HUỐNG LUYỆN TẬP</p>
        {scenarios.map(sc => (
          <button key={sc.id} onClick={() => { onSelect(sc); nav("techtalk"); }} style={{ width: "100%", borderRadius: 14, border: `1px solid ${sc.color}30`, background: "oklch(0.14 0.025 250)", padding: 14, textAlign: "left", cursor: "pointer", marginBottom: 10, display: "block" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 10 }}>
              <div style={{ width: 44, height: 44, borderRadius: 14, background: `${sc.color}18`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 22, flexShrink: 0 }}>{sc.avatar}</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, color: "oklch(0.90 0.01 250)", margin: "0 0 2px" }}>{sc.name}</p>
                <p style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", margin: 0 }}>{sc.nameVi}</p>
              </div>
              <div style={{ display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 4 }}>
                <KBadge color={sc.color}>{sc.difficulty}</KBadge>
                <KBadge color="oklch(0.55 0.03 250)">{sc.domain}</KBadge>
              </div>
            </div>
            <div style={{ padding: "8px 10px", borderRadius: 8, background: "oklch(0.10 0.02 250)", marginBottom: 10 }}>
              <p style={{ fontSize: 11, color: "oklch(0.55 0.03 250)", margin: 0 }}>🎯 {lang === "vi" ? sc.missionVi : sc.mission}</p>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <span style={{ fontSize: 14 }}>{sc.avatar}</span>
              <span style={{ fontSize: 11, color: "oklch(0.55 0.03 250)", fontFamily: "Inter, sans-serif" }}>{sc.persona} · {sc.role} @ {sc.company}</span>
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}

// ─── TechTalk ───────────────────────────────────────────────────────────────────

function TechTalkScreen({ nav, lang, scenario }: { nav: (s: Screen) => void; lang: Lang; scenario: ScenarioType }) {
  const t = makeT(lang);
  const [messages, setMessages] = useState(initMessages);
  const [input, setInput] = useState("");
  const [showHint, setShowHint] = useState(false);
  const [voiceMode, setVoiceMode] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  const send = () => {
    if (!input.trim()) return;
    const userMsg = { id: Date.now().toString(), role: "user" as const, content: input, eval: { grammar: 82, vocabulary: 78, politeness: 88 } };
    setMessages(m => [...m, userMsg]);
    setInput("");
    setTimeout(() => {
      setMessages(m => [...m, { id: (Date.now() + 1).toString(), role: "ai" as const, content: "알겠습니다. 장애 발생 시간과 영향 범위를 구체적으로 보고해 주세요.", eval: null }]);
      bottomRef.current?.scrollIntoView({ behavior: "smooth" });
    }, 1000);
  };

  return (
    <div style={{ flex: 1, display: "flex", flexDirection: "column", overflow: "hidden" }}>
      <div style={{ display: "flex", alignItems: "center", gap: 10, padding: "10px 16px", borderBottom: "1px solid oklch(0.22 0.03 250)" }}>
        <button onClick={() => nav("techtalk-select")} style={{ width: 32, height: 32, borderRadius: 10, border: "none", background: "oklch(0.17 0.025 250)", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
          <ChevronLeft size={18} color={TEAL} />
        </button>
        <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 15, color: "oklch(0.92 0.01 250)", flex: 1 }}>TechTalk AI</span>
        <button onClick={() => nav("techtalk-result")} style={{ padding: "5px 12px", borderRadius: 8, border: "1px solid #f8717140", background: "#f8717112", color: "#f87171", fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 12, cursor: "pointer" }}>
          {t("endSession")}
        </button>
      </div>
      <div style={{ padding: "10px 14px", borderBottom: "1px solid oklch(0.20 0.03 250)", background: "oklch(0.12 0.025 250)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <div style={{ width: 40, height: 40, borderRadius: "50%", background: `${scenario.color}18`, border: `2px solid ${scenario.color}44`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 18 }}>{scenario.avatar}</div>
          <div style={{ flex: 1 }}>
            <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 13, color: "oklch(0.90 0.01 250)", margin: "0 0 3px" }}>{scenario.persona}</p>
            <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
              <KBadge color={scenario.color}>{scenario.role}</KBadge>
              <span style={{ fontSize: 10, color: "oklch(0.40 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>{scenario.company}</span>
            </div>
          </div>
          <KBadge color={AMBER}>{scenario.difficulty}</KBadge>
        </div>
        <div style={{ marginTop: 8, padding: "6px 10px", borderRadius: 8, background: "oklch(0.15 0.025 250)" }}>
          <p style={{ fontSize: 10, color: "oklch(0.48 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>🎯 {lang === "vi" ? scenario.missionVi : scenario.mission}</p>
        </div>
      </div>
      <div style={{ flex: 1, overflowY: "auto", padding: "12px 14px", display: "flex", flexDirection: "column", gap: 10, scrollbarWidth: "none" }}>
        {messages.map(msg => (
          <div key={msg.id} style={{ display: "flex", justifyContent: msg.role === "user" ? "flex-end" : "flex-start" }}>
            <div style={{ maxWidth: "78%" }}>
              <div style={{ padding: "10px 14px", background: msg.role === "user" ? TEAL : "oklch(0.17 0.025 250)", color: msg.role === "user" ? "#000" : "oklch(0.88 0.01 250)", borderRadius: msg.role === "user" ? "16px 16px 4px 16px" : "16px 16px 16px 4px", fontFamily: "Inter, sans-serif", fontSize: 13, lineHeight: 1.5 }}>
                {msg.content}
              </div>
              {msg.eval && (
                <div style={{ display: "flex", gap: 8, marginTop: 4, justifyContent: "flex-end" }}>
                  {([["G", msg.eval.grammar, "#34d399"], ["V", msg.eval.vocabulary, "#60a5fa"], ["P", msg.eval.politeness, "#a78bfa"]] as [string, number, string][]).map(([k, v, c]) => (
                    <span key={k} style={{ fontSize: 9, fontFamily: "JetBrains Mono, monospace", color: c }}>{k}:{v}</span>
                  ))}
                </div>
              )}
            </div>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>
      {showHint && (
        <div style={{ margin: "0 12px 8px", borderRadius: 12, padding: "10px 12px", background: `${AMBER}12`, border: `1px solid ${AMBER}30` }}>
          <p style={{ fontSize: 10, color: AMBER, fontFamily: "JetBrains Mono, monospace", margin: "0 0 5px", letterSpacing: 1 }}>💡 GỢI Ý</p>
          <p style={{ fontSize: 12, color: "oklch(0.85 0.01 250)", margin: "0 0 3px" }}>Từ khóa: <strong style={{ color: TEAL }}>배포, 서버, 장애</strong></p>
          <p style={{ fontSize: 11, color: "oklch(0.55 0.03 250)", margin: 0 }}>Mẫu câu: "...에 문제가 발생했습니다"</p>
        </div>
      )}
      <div style={{ padding: "10px 12px", borderTop: "1px solid oklch(0.20 0.03 250)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <button onClick={() => setShowHint(h => !h)} style={{ width: 34, height: 34, borderRadius: "50%", background: `${AMBER}18`, border: "none", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
            <Lightbulb size={14} color={AMBER} />
          </button>
          {voiceMode ? (
            <div style={{ flex: 1, height: 38, borderRadius: 12, background: "oklch(0.16 0.025 250)", border: "1px solid #f8717130", display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
              <Radio size={13} color="#f87171" />
              <span style={{ fontSize: 11, color: "#f87171", fontFamily: "JetBrains Mono, monospace" }}>Đang ghi âm...</span>
            </div>
          ) : (
            <input value={input} onChange={e => setInput(e.target.value)} onKeyDown={e => e.key === "Enter" && send()} placeholder="Nhập câu tiếng Hàn..." style={{ flex: 1, height: 38, borderRadius: 12, padding: "0 12px", background: "oklch(0.16 0.025 250)", border: "1px solid oklch(0.25 0.03 250)", color: "oklch(0.88 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none" }} />
          )}
          <button onClick={() => setVoiceMode(v => !v)} style={{ width: 34, height: 34, borderRadius: "50%", background: voiceMode ? "#f8717118" : "oklch(0.20 0.03 250)", border: "none", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
            <Mic size={14} color={voiceMode ? "#f87171" : "oklch(0.55 0.03 250)"} />
          </button>
          <button onClick={send} style={{ width: 34, height: 34, borderRadius: "50%", background: TEAL, border: "none", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
            <Send size={14} color="#000" />
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── TechTalk Result ────────────────────────────────────────────────────────────

function TechTalkResultScreen({ nav, lang }: { nav: (s: Screen) => void; lang: Lang }) {
  const t = makeT(lang);
  const improvements = lang === "vi" ? finalEvaluation.improvementsVi : finalEvaluation.improvementsEn;
  return (
    <div style={{ flex: 1, display: "flex", flexDirection: "column", overflow: "hidden" }}>
      <div style={{ padding: "12px 16px", borderBottom: "1px solid oklch(0.22 0.03 250)", display: "flex", alignItems: "center", gap: 10 }}>
        <div style={{ width: 32, height: 32, borderRadius: 10, background: `${TEAL}18`, display: "flex", alignItems: "center", justifyContent: "center" }}>
          <Award size={16} color={TEAL} />
        </div>
        <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 15, color: "oklch(0.92 0.01 250)" }}>{t("sessionResult")}</span>
      </div>
      <div style={{ flex: 1, overflowY: "auto", padding: "16px", scrollbarWidth: "none" }}>
        <div style={{ display: "flex", justifyContent: "center", marginBottom: 20 }}>
          <CircRing value={finalEvaluation.overallScore} color={TEAL} size={90} label="OVERALL" />
        </div>
        <KCard style={{ marginBottom: 12 }}>
          <p style={{ fontSize: 9, color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 12px" }}>{t("details")}</p>
          {finalEvaluation.metrics.map(m => (
            <div key={m.name} style={{ marginBottom: 10 }}>
              <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
                <span style={{ fontSize: 12, color: "oklch(0.75 0.02 250)" }}>{lang === "vi" ? m.name : m.nameEn}</span>
                <span style={{ fontSize: 12, color: m.color, fontFamily: "JetBrains Mono, monospace", fontWeight: 700 }}>{m.score}</span>
              </div>
              <div style={{ height: 5, borderRadius: 3, background: "oklch(0.22 0.03 250)" }}>
                <div style={{ height: "100%", borderRadius: 3, width: `${m.score}%`, background: m.color }} />
              </div>
            </div>
          ))}
        </KCard>
        <KCard style={{ marginBottom: 12, background: `${TEAL}0a`, border: `1px solid ${TEAL}25` }}>
          <p style={{ fontSize: 9, color: TEAL, fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 6px" }}>{t("feedback")}</p>
          <p style={{ fontSize: 12, color: "oklch(0.80 0.01 250)", margin: 0, lineHeight: 1.7 }}>{lang === "vi" ? finalEvaluation.feedbackVi : finalEvaluation.feedbackEn}</p>
        </KCard>
        <KCard style={{ marginBottom: 20 }}>
          <p style={{ fontSize: 9, color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 10px" }}>{t("improvements")}</p>
          {improvements.map((item, i) => (
            <div key={i} style={{ display: "flex", alignItems: "flex-start", gap: 10, marginBottom: i < improvements.length - 1 ? 8 : 0 }}>
              <div style={{ width: 18, height: 18, borderRadius: "50%", background: `${AMBER}18`, border: `1px solid ${AMBER}40`, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0, marginTop: 1 }}>
                <span style={{ fontSize: 9, color: AMBER, fontFamily: "JetBrains Mono, monospace", fontWeight: 700 }}>{i + 1}</span>
              </div>
              <span style={{ fontSize: 12, color: "oklch(0.72 0.02 250)", lineHeight: 1.5 }}>{item}</span>
            </div>
          ))}
        </KCard>
        <div style={{ display: "flex", gap: 10 }}>
          <button onClick={() => nav("techtalk-select")} style={{ flex: 1, padding: "12px 0", borderRadius: 12, border: `1px solid ${TEAL}40`, background: `${TEAL}12`, color: TEAL, fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer" }}>
            {t("tryAgain")}
          </button>
          <button onClick={() => nav("dashboard")} style={{ flex: 1, padding: "12px 0", borderRadius: 12, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer" }}>
            {t("goHome")}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Honorifics ─────────────────────────────────────────────────────────────────

function HonorificsScreen({ nav }: { nav: (s: Screen) => void }) {
  const [text, setText] = useState("나 오늘 서버 배포 했어. 너 확인해봐.");
  const [analyzed, setAnalyzed] = useState(false);
  const [copied, setCopied] = useState(false);
  const [activeIdx, setActiveIdx] = useState<number | null>(null);
  const formalText = "저 오늘 서버 배포 하였습니다. 귀하 확인해 주시기 바랍니다.";
  return (
    <div style={{ flex: 1, display: "flex", flexDirection: "column", overflow: "hidden" }}>
      <ModalHeader title="Honorifics Analyzer" onBack={() => nav("dashboard")} />
      <div style={{ flex: 1, overflowY: "auto", padding: "14px 16px", scrollbarWidth: "none" }}>
        <div style={{ marginBottom: 12 }}>
          <textarea value={text} onChange={e => { setText(e.target.value); setAnalyzed(false); }} rows={3} style={{ width: "100%", borderRadius: 12, padding: "10px 12px", background: "oklch(0.14 0.025 250)", border: "1px solid oklch(0.25 0.03 250)", color: "oklch(0.88 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, lineHeight: 1.6, outline: "none", resize: "none", boxSizing: "border-box", marginBottom: 10 }} />
          <button onClick={() => setAnalyzed(true)} style={{ width: "100%", padding: "11px 0", borderRadius: 12, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }}>
            <Zap size={16} /> 분석하기 (Phân tích)
          </button>
        </div>
        {analyzed && (
          <>
            <KCard style={{ marginBottom: 12 }}>
              <p style={{ fontSize: 9, color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 8px" }}>MỨC ĐỘ LỊCH SỰ</p>
              <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 12 }}>
                <div style={{ padding: "4px 12px", borderRadius: 999, background: "#f8717120", border: "1px solid #f8717150" }}>
                  <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, color: "#f87171" }}>반말 (Casual)</span>
                </div>
                <ArrowRight size={14} color="oklch(0.38 0.03 250)" />
                <div style={{ padding: "4px 12px", borderRadius: 999, background: "#34d39920", border: "1px solid #34d39950" }}>
                  <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 12, color: "#34d399" }}>하십시오체</span>
                </div>
              </div>
            </KCard>
            <div style={{ marginBottom: 12 }}>
              <p style={{ fontSize: 9, color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, marginBottom: 8 }}>SỬA ĐỔI ({corrections.length})</p>
              {corrections.map((c, i) => (
                <button key={i} onClick={() => setActiveIdx(activeIdx === i ? null : i)} style={{ width: "100%", borderRadius: 12, border: `1px solid ${activeIdx === i ? `${TEAL}44` : "oklch(0.22 0.03 250)"}`, background: "oklch(0.14 0.025 250)", padding: "10px 12px", textAlign: "left", cursor: "pointer", marginBottom: 6, display: "block" }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                      <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, color: "#f87171", textDecoration: "line-through" }}>{c.original}</span>
                      <ArrowRight size={12} color="oklch(0.38 0.03 250)" />
                      <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, color: "#34d399" }}>{c.corrected}</span>
                    </div>
                    <KBadge color={TEAL}>{c.type}</KBadge>
                  </div>
                  {activeIdx === i && <p style={{ fontSize: 11, color: "oklch(0.60 0.03 250)", margin: "8px 0 0" }}>{c.explanation}</p>}
                </button>
              ))}
            </div>
            <KCard>
              <p style={{ fontSize: 9, color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 8px" }}>KẾT QUẢ CHÍNH THỨC</p>
              <p style={{ fontFamily: "Inter, sans-serif", fontSize: 14, color: "#34d399", lineHeight: 1.7, margin: "0 0 12px" }}>{formalText}</p>
              <div style={{ display: "flex", gap: 8 }}>
                <button onClick={() => setText(formalText)} style={{ flex: 1, padding: "9px 0", borderRadius: 10, background: `${TEAL}18`, border: `1px solid ${TEAL}40`, color: TEAL, fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 12, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
                  <Check size={13} /> Áp dụng
                </button>
                <button onClick={() => { setCopied(true); setTimeout(() => setCopied(false), 2000); }} style={{ flex: 1, padding: "9px 0", borderRadius: 10, background: "oklch(0.18 0.03 250)", border: "1px solid oklch(0.25 0.03 250)", color: "oklch(0.62 0.03 250)", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 12, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
                  {copied ? <><Check size={13} />Đã sao chép!</> : <><Copy size={13} />Sao chép</>}
                </button>
              </div>
            </KCard>
          </>
        )}
      </div>
    </div>
  );
}

// ─── Video ──────────────────────────────────────────────────────────────────────

function VideoScreen({ nav }: { nav: (s: Screen) => void }) {
  const [playing, setPlaying] = useState(false);
  const [subIdx, setSubIdx] = useState(1);
  const [showDict, setShowDict] = useState(false);
  const [selectedWord, setSelectedWord] = useState("배포");
  const [speed, setSpeed] = useState("1×");
  const [showQuiz, setShowQuiz] = useState(false);
  const [quizSelected, setQuizSelected] = useState<number | null>(null);
  const sub = videoSubtitles[((subIdx % videoSubtitles.length) + videoSubtitles.length) % videoSubtitles.length];

  return (
    <div style={{ flex: 1, display: "flex", flexDirection: "column", overflow: "hidden", position: "relative" }}>
      <ModalHeader title="Video Lab" onBack={() => nav("dashboard")} />
      <div style={{ position: "relative", background: "#050d1a", aspectRatio: "16/9", flexShrink: 0 }}>
        <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center" }}>
          <div style={{ textAlign: "center" }}>
            <div style={{ width: 48, height: 48, borderRadius: "50%", background: `${TEAL}18`, border: `2px solid ${TEAL}44`, display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 8px" }}>
              <Play size={20} color={TEAL} style={{ marginLeft: 2 }} />
            </div>
            <p style={{ fontSize: 10, color: "oklch(0.40 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>DEVIEW 2025 · Serverless Architecture</p>
          </div>
        </div>
        <div style={{ position: "absolute", bottom: 8, left: 0, right: 0, textAlign: "center", padding: "0 12px" }}>
          <div style={{ display: "inline-block", background: "rgba(0,0,0,0.75)", borderRadius: 8, padding: "6px 12px" }}>
            <div style={{ display: "flex", flexWrap: "wrap", justifyContent: "center", gap: 4, marginBottom: 3 }}>
              {sub.tokens.map((tok, i) => (
                <button key={i} onClick={() => { setSelectedWord(tok); setShowDict(true); }} style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "#fff", background: "none", border: "none", cursor: "pointer", textDecoration: "underline dotted rgba(255,255,255,0.5)", textUnderlineOffset: 3, padding: 0 }}>
                  {tok}
                </button>
              ))}
            </div>
            <p style={{ fontSize: 10, color: "rgba(255,255,255,0.6)", margin: 0, fontFamily: "Inter, sans-serif" }}>{sub.vi}</p>
          </div>
        </div>
        <button onClick={() => setPlaying(p => !p)} style={{ position: "absolute", inset: 0, background: "none", border: "none", cursor: "pointer" }} />
      </div>
      <div style={{ padding: "10px 14px", borderBottom: "1px solid oklch(0.20 0.03 250)", flexShrink: 0 }}>
        <div style={{ position: "relative", height: 6, borderRadius: 3, background: "oklch(0.20 0.03 250)", marginBottom: 10, cursor: "pointer" }}>
          <div style={{ position: "absolute", height: "100%", borderRadius: 3, width: "32%", background: TEAL }} />
          {[0.375, 0.6].map((pos, i) => (
            <div key={i} onClick={() => { setShowQuiz(true); setQuizSelected(null); }} style={{ position: "absolute", top: "50%", transform: "translateY(-50%)", left: `${pos * 100}%`, width: 12, height: 12, borderRadius: "50%", background: pos < 0.32 ? "#34d399" : AMBER, border: "2px solid #000", cursor: "pointer", zIndex: 2 }} />
          ))}
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <button onClick={() => setPlaying(p => !p)} style={{ width: 32, height: 32, borderRadius: "50%", background: TEAL, border: "none", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
            {playing ? <Pause size={13} color="#000" /> : <Play size={13} color="#000" style={{ marginLeft: 1 }} />}
          </button>
          <span style={{ fontSize: 10, color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", flex: 1 }}>6:24 / 20:00</span>
          <div style={{ display: "flex", gap: 4 }}>
            {["0.75×", "1×", "1.25×"].map(s => (
              <button key={s} onClick={() => setSpeed(s)} style={{ padding: "2px 7px", borderRadius: 5, border: "none", cursor: "pointer", fontFamily: "JetBrains Mono, monospace", fontSize: 9, background: speed === s ? TEAL : "oklch(0.20 0.03 250)", color: speed === s ? "#000" : "oklch(0.50 0.03 250)" }}>{s}</button>
            ))}
          </div>
        </div>
        <div style={{ display: "flex", gap: 6, marginTop: 8 }}>
          <button onClick={() => setSubIdx(i => i - 1)} style={{ flex: 1, padding: "6px 0", borderRadius: 8, background: "oklch(0.18 0.03 250)", border: "none", cursor: "pointer", fontSize: 11, color: "oklch(0.55 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>← Trước</button>
          <button onClick={() => setSubIdx(i => i + 1)} style={{ flex: 1, padding: "6px 0", borderRadius: 8, background: "oklch(0.18 0.03 250)", border: "none", cursor: "pointer", fontSize: 11, color: "oklch(0.55 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>Tiếp →</button>
        </div>
      </div>
      <div style={{ flex: 1, overflowY: "auto", padding: "12px 14px", scrollbarWidth: "none" }}>
        <p style={{ fontSize: 9, color: "oklch(0.40 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, marginBottom: 10 }}>TỪ VỰNG ĐOẠN NÀY</p>
        <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
          {["서버리스", "아키텍처", "배포", "자동화", "오류 처리", "함수"].map(w => (
            <button key={w} onClick={() => { setSelectedWord(w); setShowDict(true); }} style={{ padding: "5px 12px", borderRadius: 999, background: `${TEAL}12`, border: `1px solid ${TEAL}30`, color: TEAL, fontSize: 13, fontFamily: "Outfit, sans-serif", cursor: "pointer" }}>
              {w}
            </button>
          ))}
        </div>
      </div>
      {showDict && (
        <div onClick={() => setShowDict(false)} style={{ position: "absolute", inset: 0, background: "rgba(0,0,0,0.6)", display: "flex", alignItems: "flex-end", zIndex: 50 }}>
          <div onClick={e => e.stopPropagation()} style={{ width: "100%", borderRadius: "18px 18px 0 0", padding: "20px 18px", background: "oklch(0.14 0.025 250)", border: "1px solid oklch(0.22 0.03 250)", borderBottom: "none" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 10 }}>
              <div>
                <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 30, color: TEAL }}>{selectedWord}</span>
                <span style={{ fontSize: 12, color: "oklch(0.45 0.03 250)", marginLeft: 8, fontFamily: "JetBrains Mono, monospace" }}>bae-po</span>
              </div>
              <button onClick={() => setShowDict(false)} style={{ width: 28, height: 28, borderRadius: "50%", background: "oklch(0.20 0.03 250)", border: "none", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <X size={13} color="oklch(0.55 0.03 250)" />
              </button>
            </div>
            <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 17, color: "oklch(0.90 0.01 250)", margin: "0 0 4px" }}>Triển khai (Deploy)</p>
            <p style={{ fontSize: 12, color: "oklch(0.48 0.03 250)", margin: "0 0 5px" }}>Software deployment — pushing code to production</p>
            <p style={{ fontSize: 12, color: "oklch(0.58 0.03 250)", fontStyle: "italic", margin: "0 0 14px" }}>새 버전을 배포했습니다.</p>
            <button style={{ width: "100%", padding: "11px 0", borderRadius: 12, background: TEAL, border: "none", color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }}>
              <Plus size={16} /> Thêm vào MemByte
            </button>
          </div>
        </div>
      )}
      {showQuiz && (
        <div style={{ position: "absolute", inset: 0, background: "rgba(0,0,0,0.8)", zIndex: 60, display: "flex", alignItems: "center", padding: "0 20px" }}>
          <div style={{ width: "100%", borderRadius: 18, padding: "20px 18px", background: "oklch(0.14 0.025 250)", border: "1px solid oklch(0.22 0.03 250)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 14 }}>
              <div style={{ width: 28, height: 28, borderRadius: 8, background: `${AMBER}20`, display: "flex", alignItems: "center", justifyContent: "center" }}>
                <Brain size={14} color={AMBER} />
              </div>
              <span style={{ fontSize: 10, color: AMBER, fontFamily: "JetBrains Mono, monospace", letterSpacing: 1 }}>QUIZ · 6:24</span>
            </div>
            <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 15, color: "oklch(0.92 0.01 250)", margin: "0 0 4px", lineHeight: 1.4 }}>{videoQuiz.questionKo}</p>
            <p style={{ fontSize: 11, color: "oklch(0.50 0.03 250)", margin: "0 0 16px" }}>{videoQuiz.questionVi}</p>
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {videoQuiz.options.map((opt, i) => {
                const isSelected = quizSelected === i;
                const answered = quizSelected !== null;
                const isCorrect = answered && opt.correct;
                const isWrong = isSelected && !opt.correct;
                return (
                  <button key={i} onClick={() => !answered && setQuizSelected(i)} style={{ padding: "10px 14px", borderRadius: 10, border: `1px solid ${isCorrect ? "#34d39950" : isWrong ? "#f8717150" : "oklch(0.25 0.03 250)"}`, background: isCorrect ? "#34d39914" : isWrong ? "#f8717114" : "oklch(0.16 0.025 250)", cursor: !answered ? "pointer" : "default", textAlign: "left", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <span style={{ fontFamily: "Inter, sans-serif", fontSize: 13, color: isCorrect ? "#34d399" : isWrong ? "#f87171" : "oklch(0.80 0.01 250)" }}>{opt.text}</span>
                    {answered && opt.correct && <Check size={14} color="#34d399" />}
                    {isWrong && <X size={14} color="#f87171" />}
                  </button>
                );
              })}
            </div>
            {quizSelected !== null && (
              <button onClick={() => { setShowQuiz(false); setQuizSelected(null); }} style={{ width: "100%", marginTop: 14, padding: "11px 0", borderRadius: 12, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, cursor: "pointer" }}>
                Tiếp tục →
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Pronunciation List ─────────────────────────────────────────────────────────

function PronunciationListScreen({ nav, lang }: { nav: (s: Screen) => void; lang: Lang }) {
  const t = makeT(lang);
  const diffColor = (d: string) => d === "easy" ? "#34d399" : d === "medium" ? AMBER : "#f87171";
  const domColor = (d: string) => d === "frontend" ? TEAL : d === "devops" ? "#fb923c" : d === "agile" ? "#34d399" : "#a78bfa";
  return (
    <div style={{ flex: 1, display: "flex", flexDirection: "column", overflow: "hidden" }}>
      <ModalHeader title={t("exercisesTitle")} onBack={() => nav("dashboard")} />
      <div style={{ flex: 1, overflowY: "auto", padding: "14px 16px", scrollbarWidth: "none" }}>
        {pronunciationExercises.map(ex => (
          <button key={ex.id} onClick={() => nav("pronunciation")} style={{ width: "100%", borderRadius: 14, border: "1px solid oklch(0.22 0.03 250)", background: "oklch(0.14 0.025 250)", padding: 14, textAlign: "left", cursor: "pointer", marginBottom: 8, display: "block" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 8 }}>
              <div style={{ flex: 1, minWidth: 0, marginRight: 10 }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 15, color: "oklch(0.90 0.01 250)", margin: "0 0 2px" }}>{ex.phrase}</p>
                <p style={{ fontSize: 10, color: "oklch(0.48 0.03 250)", margin: 0 }}>{ex.phraseVi}</p>
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: 4, alignItems: "flex-end" }}>
                <KBadge color={diffColor(ex.difficulty)}>{ex.difficulty}</KBadge>
                <KBadge color={domColor(ex.domain)}>{ex.domain}</KBadge>
              </div>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <span style={{ fontSize: 10, color: "oklch(0.40 0.03 250)", fontFamily: "JetBrains Mono, monospace", flexShrink: 0 }}>{ex.attempts} {lang === "vi" ? "lần" : "tries"}</span>
              {ex.bestScore > 0 ? (
                <>
                  <div style={{ flex: 1, height: 3, borderRadius: 2, background: "oklch(0.22 0.03 250)" }}>
                    <div style={{ height: "100%", borderRadius: 2, width: `${ex.bestScore}%`, background: TEAL }} />
                  </div>
                  <span style={{ fontSize: 10, color: TEAL, fontFamily: "JetBrains Mono, monospace", flexShrink: 0 }}>{lang === "vi" ? "Tốt nhất" : "Best"}: {ex.bestScore}</span>
                </>
              ) : (
                <span style={{ flex: 1, fontSize: 10, color: "oklch(0.35 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>{lang === "vi" ? "Chưa thử" : "Not attempted"}</span>
              )}
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}

// ─── Pronunciation ──────────────────────────────────────────────────────────────

function PronunciationScreen({ nav }: { nav: (s: Screen) => void }) {
  const [recording, setRecording] = useState(false);
  const [phase, setPhase] = useState<"listen" | "record" | "result">("listen");
  const [tick, setTick] = useState(0);

  useEffect(() => {
    if (!recording) return;
    const id = setInterval(() => setTick(t => t + 1), 100);
    return () => clearInterval(id);
  }, [recording]);

  const handleRecord = () => {
    if (recording) { setRecording(false); setTimeout(() => setPhase("result"), 600); }
    else { setRecording(true); setPhase("record"); }
  };

  const colorMap = { correct: "#34d399", minor_error: AMBER, major_error: "#f87171", omitted: "#f87171" };

  return (
    <div style={{ flex: 1, display: "flex", flexDirection: "column", overflow: "hidden" }}>
      <ModalHeader title="Pronunciation Lab" onBack={() => nav("pronunciation-list")} />
      <div style={{ flex: 1, overflowY: "auto", padding: "14px 16px", scrollbarWidth: "none" }}>
        <KCard style={{ marginBottom: 12 }}>
          <p style={{ fontSize: 9, color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 8px" }}>CÂU MẪU</p>
          <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 20, color: "oklch(0.92 0.01 250)", margin: "0 0 4px" }}>서버 배포가 완료되었습니다</p>
          <p style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", margin: "0 0 12px" }}>Việc triển khai server đã hoàn tất</p>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <button style={{ width: 34, height: 34, borderRadius: "50%", background: `${TEAL}18`, border: "none", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              <Play size={14} color={TEAL} style={{ marginLeft: 1 }} />
            </button>
            <div style={{ flex: 1, display: "flex", alignItems: "flex-end", gap: 2, height: 30 }}>
              {waveHeights.map((h, i) => <div key={i} style={{ flex: 1, height: h, borderRadius: 2, background: TEAL, opacity: 0.65 }} />)}
            </div>
          </div>
        </KCard>
        <KCard style={{ marginBottom: 12 }}>
          <p style={{ fontSize: 9, color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 10px" }}>BẢN GHI ÂM CỦA BẠN</p>
          <div style={{ display: "flex", alignItems: "flex-end", gap: 2, height: 48, marginBottom: 14 }}>
            {waveHeights.map((h, i) => {
              const liveH = recording ? Math.abs(Math.sin((tick + i) * 0.35)) * 32 + 5 : (phase !== "listen" ? h * 0.76 : 4);
              return <div key={i} style={{ flex: 1, height: liveH, borderRadius: 2, background: recording ? "#f87171" : (phase !== "listen" ? AMBER : "oklch(0.28 0.03 250)"), transition: "height 0.08s" }} />;
            })}
          </div>
          <button onClick={handleRecord} style={{ width: "100%", padding: "11px 0", borderRadius: 12, border: `1px solid ${recording ? "#f87171" : TEAL}40`, background: recording ? "#f8717112" : `${TEAL}12`, color: recording ? "#f87171" : TEAL, fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }}>
            <Mic size={15} /> {recording ? "Dừng ghi âm" : phase === "result" ? "Ghi âm lại" : "Bắt đầu ghi âm"}
          </button>
        </KCard>
        {phase === "result" && (
          <>
            <div style={{ marginBottom: 12 }}>
              <p style={{ fontSize: 9, color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, marginBottom: 10 }}>ĐÁNH GIÁ TỪNG TỪ</p>
              <div style={{ display: "flex", flexWrap: "wrap", gap: 10 }}>
                {pronunciationWords.map((w, i) => (
                  <span key={i} style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 18, color: colorMap[w.accuracy], borderBottom: `2px solid ${colorMap[w.accuracy]}55`, paddingBottom: 2 }}>{w.text}</span>
                ))}
              </div>
            </div>
            <KCard style={{ marginBottom: 12 }}>
              <p style={{ fontSize: 9, color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 14px" }}>ĐIỂM SỐ</p>
              <div style={{ display: "flex", justifyContent: "space-around" }}>
                <CircRing value={82} color={TEAL} label="ACCURACY" />
                <CircRing value={75} color="#a78bfa" label="FLUENCY" />
                <CircRing value={68} color={AMBER} label="PROSODY" />
              </div>
            </KCard>
            <button style={{ width: "100%", padding: "12px 0", borderRadius: 12, background: TEAL, border: "none", color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, cursor: "pointer" }}>
              Bài tiếp theo →
            </button>
          </>
        )}
      </div>
    </div>
  );
}

// ─── Profile ────────────────────────────────────────────────────────────────────

function ProfileScreen({ lang, setLang }: { lang: Lang; setLang: (l: Lang) => void }) {
  const t = makeT(lang);
  const [notif, setNotif] = useState(true);
  const [ttsSpeed, setTtsSpeed] = useState("1.0×");

  const stats = [
    { label: lang === "vi" ? "Phút học" : "Study mins", value: "1,250", icon: Clock, color: TEAL },
    { label: lang === "vi" ? "Thẻ đã ôn" : "Cards reviewed", value: "3,400", icon: Brain, color: "#a78bfa" },
    { label: "Roleplay", value: "45", icon: MessageSquare, color: "#34d399" },
    { label: "Video", value: "23", icon: Play, color: "#fb923c" },
  ];

  return (
    <div style={{ flex: 1, overflowY: "auto", scrollbarWidth: "none" }}>
      <div style={{ padding: "14px 16px 10px" }}>
        <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 20, color: "oklch(0.95 0.01 250)", margin: 0 }}>{t("profileTitle")}</h1>
      </div>
      <div style={{ padding: "0 16px 12px" }}>
        <KCard style={{ display: "flex", alignItems: "center", gap: 14 }}>
          <div style={{ width: 54, height: 54, borderRadius: "50%", background: `${TEAL}18`, border: `2px solid ${TEAL}40`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 24, flexShrink: 0 }}>👨‍💻</div>
          <div>
            <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 16, color: "oklch(0.92 0.01 250)", margin: "0 0 3px" }}>Nguyễn Văn A</p>
            <p style={{ fontSize: 10, color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: "0 0 6px" }}>nguyen.van.a@gmail.com</p>
            <div style={{ display: "flex", gap: 6 }}>
              <KBadge color={TEAL}>Beginner KO</KBadge>
              <KBadge color="#a78bfa">🇻🇳 Việt</KBadge>
            </div>
          </div>
        </KCard>
      </div>
      <div style={{ padding: "0 16px 12px" }}>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
          {stats.map(({ label, value, icon: Icon, color }) => (
            <KCard key={label} style={{ display: "flex", alignItems: "center", gap: 10, padding: 12 }}>
              <div style={{ width: 34, height: 34, borderRadius: 10, background: `${color}18`, display: "flex", alignItems: "center", justifyContent: "center" }}>
                <Icon size={14} color={color} />
              </div>
              <div>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 17, color, margin: 0 }}>{value}</p>
                <p style={{ fontSize: 9, color: "oklch(0.40 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>{label}</p>
              </div>
            </KCard>
          ))}
        </div>
      </div>
      <div style={{ padding: "0 16px 24px" }}>
        <p style={{ fontSize: 9, color: "oklch(0.40 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, marginBottom: 8 }}>{t("settingsSection")}</p>
        <KCard style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 8 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <Bell size={15} color="oklch(0.50 0.03 250)" />
            <span style={{ fontSize: 13, color: "oklch(0.82 0.01 250)", fontFamily: "Inter, sans-serif" }}>{t("notifications")}</span>
          </div>
          <button onClick={() => setNotif(n => !n)} style={{ width: 42, height: 24, borderRadius: 12, background: notif ? TEAL : "oklch(0.28 0.03 250)", border: "none", cursor: "pointer", position: "relative", transition: "background 0.2s" }}>
            <div style={{ position: "absolute", top: 3, left: notif ? "calc(100% - 21px)" : 3, width: 18, height: 18, borderRadius: "50%", background: "#fff", transition: "left 0.2s" }} />
          </button>
        </KCard>
        <KCard style={{ marginBottom: 8 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 10 }}>
            <Volume2 size={15} color="oklch(0.50 0.03 250)" />
            <span style={{ fontSize: 13, color: "oklch(0.82 0.01 250)", fontFamily: "Inter, sans-serif" }}>{t("ttsSpeed")}</span>
          </div>
          <div style={{ display: "flex", gap: 6 }}>
            {["0.75×", "1.0×", "1.25×", "1.5×"].map(s => (
              <button key={s} onClick={() => setTtsSpeed(s)} style={{ flex: 1, padding: "5px 0", borderRadius: 8, border: "none", cursor: "pointer", fontFamily: "JetBrains Mono, monospace", fontSize: 10, background: ttsSpeed === s ? TEAL : "oklch(0.20 0.03 250)", color: ttsSpeed === s ? "#000" : "oklch(0.50 0.03 250)" }}>{s}</button>
            ))}
          </div>
        </KCard>
        <KCard style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 8 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <Globe size={15} color="oklch(0.50 0.03 250)" />
            <span style={{ fontSize: 13, color: "oklch(0.82 0.01 250)", fontFamily: "Inter, sans-serif" }}>{t("uiLanguage")}</span>
          </div>
          <button onClick={() => setLang(lang === "vi" ? "en" : "vi")} style={{ padding: "4px 12px", borderRadius: 8, border: `1px solid ${TEAL}40`, background: `${TEAL}12`, color: TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 12, cursor: "pointer" }}>
            {lang === "vi" ? "Tiếng Việt" : "English"}
          </button>
        </KCard>
        <KCard style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <Moon size={15} color="oklch(0.50 0.03 250)" />
            <span style={{ fontSize: 13, color: "oklch(0.82 0.01 250)", fontFamily: "Inter, sans-serif" }}>{t("darkMode")}</span>
          </div>
          <div style={{ width: 42, height: 24, borderRadius: 12, background: TEAL, display: "flex", alignItems: "center", justifyContent: "flex-end", paddingRight: 3 }}>
            <div style={{ width: 18, height: 18, borderRadius: "50%", background: "#000" }} />
          </div>
        </KCard>
      </div>
    </div>
  );
}

// ─── Tab Bar ────────────────────────────────────────────────────────────────────

const TAB_SCREENS: Screen[] = ["dashboard", "devvocab", "membyte", "profile"];

function TabBar({ active, nav, lang }: { active: Screen; nav: (s: Screen) => void; lang: Lang }) {
  const t = makeT(lang);
  const activeTab =
    active === "dashboard" ? "dashboard" :
    (active === "devvocab" || active === "devvocab-lesson") ? "devvocab" :
    (active === "membyte" || active === "membyte-review") ? "membyte" :
    active === "profile" ? "profile" : null;

  if (!activeTab) return null;

  const tabs = [
    { id: "dashboard" as Screen, icon: BarChart2, label: t("tabDashboard") },
    { id: "devvocab" as Screen, icon: BookOpen, label: t("tabDevVocab") },
    { id: "membyte" as Screen, icon: Brain, label: t("tabMemByte") },
    { id: "profile" as Screen, icon: User, label: t("tabProfile") },
  ];

  return (
    <div style={{ display: "flex", background: "oklch(0.12 0.025 250)", borderTop: "1px solid oklch(0.20 0.03 250)" }}>
      {tabs.map(({ id, icon: Icon, label }) => {
        const isActive = activeTab === id;
        return (
          <button key={id} onClick={() => nav(id)} style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", padding: "10px 0 8px", border: "none", background: "none", cursor: "pointer", gap: 3 }}>
            <Icon size={20} color={isActive ? TEAL : "oklch(0.38 0.03 250)"} />
            <span style={{ fontSize: 9, fontFamily: "JetBrains Mono, monospace", color: isActive ? TEAL : "oklch(0.38 0.03 250)" }}>{label}</span>
            {isActive && <div style={{ width: 4, height: 4, borderRadius: 2, background: TEAL }} />}
          </button>
        );
      })}
    </div>
  );
}

// ─── Admin Panel ────────────────────────────────────────────────────────────────

type AdminSection =
  | "dashboard" | "users" | "users-detail"
  | "content-topics" | "content-lessons" | "content-videos" | "content-scenarios" | "content-dictionary" | "content-dictionary-import" | "content-pronunciation"
  | "analytics" | "analytics-users" | "analytics-content" | "analytics-ai"
  | "settings-prompts" | "settings-admins";

function AdminPanel({ lang }: { lang: Lang }) {
  const [section, setSection] = useState<AdminSection>("dashboard");
  const [stats, setStats] = useState<any>(null);
  const [users, setUsers] = useState<any[]>([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [usersTotalPages, setUsersTotalPages] = useState(1);
  const [usersPage, setUsersPage] = useState(1);
  const [topics, setTopics] = useState<AdminTopicPayload[]>([]);
  const [topicsLoading, setTopicsLoading] = useState(false);
  const [topicsError, setTopicsError] = useState("");
  const [lessons, setLessons] = useState<AdminLessonPayload[]>([]);
  const [lessonsLoading, setLessonsLoading] = useState(false);
  const [lessonsError, setLessonsError] = useState("");
  const [lessonFilterTopicId, setLessonFilterTopicId] = useState("");
  const [videos, setVideos] = useState<AdminVideoPayload[]>([]);
  const [adminScenariosData, setAdminScenariosData] = useState<AdminScenarioPayload[]>([]);
  const [dictionaryEntries, setDictionaryEntries] = useState<AdminDictionaryPayload[]>([]);
  const [pronunciationExerciseData, setPronunciationExerciseData] = useState<AdminPronunciationPayload[]>([]);
  const [promptTemplates, setPromptTemplates] = useState<AdminPromptPayload[]>([]);
  const [adminUsersData, setAdminUsersData] = useState<any[]>([]);
  const [adminDataError, setAdminDataError] = useState("");

  const loadAdminData = async () => {
    try {
      const [dashboard, videoRows, scenarioRows, dictionaryRows, pronunciationRows, promptRows, admins] = await Promise.all([
        api.getDashboardStats(), api.getVideos(), api.getScenarios(), api.getDictionary(),
        api.getPronunciationExercises(), api.getPrompts(), api.getAdmins(),
      ]);
      setStats(dashboard); setVideos(videoRows); setAdminScenariosData(scenarioRows);
      setDictionaryEntries(dictionaryRows); setPronunciationExerciseData(pronunciationRows);
      setPromptTemplates(promptRows);
      setSelectedPrompt(current => current ?? promptRows[0] ?? null);
      setPromptContent(current => current || promptRows[0]?.content || "");
      setAdminUsersData(admins.map((user: any) => ({
        id: user.id, name: user.displayName || user.email, email: user.email,
        avatar: user.avatarUrl || "⚙️", role: "admin", lastLogin: "—",
      })));
    } catch (error) {
      console.error("Failed to load admin data:", error);
      setAdminDataError(error instanceof Error ? error.message : "Unable to load admin data.");
    }
  };

  useEffect(() => { void loadAdminData(); }, []);

  const fetchUsers = async (page = 1, search = '') => {
    setUsersLoading(true);
    try {
      const data = await api.getUsers(page, search);
      // Spring Data Page object: { content: [...], totalPages, totalElements, ... }
      if (data && data.content) {
        setUsers(data.content.map((u: any) => ({
          id: u.id,
          name: u.displayName || u.email?.split('@')[0] || 'Unknown',
          email: u.email,
          streak: u.streak?.current || 0,
          joined: u.roles ? new Date().toISOString().slice(0, 7) : '—',
          role: u.roles?.includes('ROLE_ADMIN') ? 'admin' : u.roles?.includes('ROLE_PREMIUM') ? 'premium' : 'student',
          avatar: u.roles?.includes('ROLE_ADMIN') ? '⚙️' : '👨‍💻',
          stats: u.stats,
        })));
        setUsersTotalPages(data.totalPages || 1);
      } else if (Array.isArray(data)) {
        setUsers(data.map((u: any) => ({
          id: u.id,
          name: u.displayName || u.email?.split('@')[0] || 'Unknown',
          email: u.email,
          streak: u.streak?.current || 0,
          joined: '—',
          role: u.roles?.includes('ROLE_ADMIN') ? 'admin' : u.roles?.includes('ROLE_PREMIUM') ? 'premium' : 'student',
          avatar: u.roles?.includes('ROLE_ADMIN') ? '⚙️' : '👨‍💻',
          stats: u.stats,
        })));
      }
    } catch (e) {
      console.error('Failed to fetch users:', e);
    } finally {
      setUsersLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers(usersPage, userSearch);
  }, [usersPage]);

  const dUserGrowth = stats?.userGrowthData ?? [];
  const dDau = stats?.dauData ?? [];
  const dLesson = stats?.lessonCompletionData ?? [];
  const dAiUsage = stats?.aiUsageData ?? [];
  const dNewReg = stats?.newRegData ?? [];
  const dRetention = stats?.retentionData ?? [];
  const dAiDaily = stats?.aiDailyData ?? [];
  const dUsers = stats?.users ?? 0;
  const dContent = stats?.contentCount ?? 0;
  const dDauCount = stats?.dau ?? 0;
  const videoRows = videos.map(video => ({ ...video, subtitles: !video.koreanSubtitles?.length ? "없음" : video.koreanSubtitles.length === video.vietnameseSubtitles?.length ? "완료" : "진행중", quizzes: video.quizMarkers?.length ?? 0 }));
  const scenarioRows = adminScenariosData.map(scenario => ({ ...scenario, personaName: scenario.persona?.name ?? "—", personaRole: scenario.persona?.role ?? "—", company: scenario.persona?.company ?? "—", requiredVocab: scenario.requiredVocabulary?.length ?? 0 }));
  const popularContentData = [...lessons.map(lesson => ({ title: lesson.title, domain: lesson.domain ?? "—", type: "Lesson", completions: 0 })), ...videoRows.map(video => ({ title: video.title, domain: video.domain, type: "Video", completions: 0 })), ...scenarioRows.map(scenario => ({ title: scenario.title, domain: scenario.domain, type: "Scenario", completions: 0 }))].slice(0, 5);
  const contentPerformanceRows = popularContentData.map(item => ({ ...item, rate: 0 }));
  const [showSubEditor, setShowSubEditor] = useState(false);
  const [showAddVideo, setShowAddVideo] = useState(false);
  const [editSubs, setEditSubs] = useState<{ id: string; start: string; end: string; ko: string; vi: string }[]>([]);
  const [selectedVideo, setSelectedVideo] = useState<AdminVideoPayload | null>(null);
  const [contentOpen, setContentOpen] = useState(true);
  const [analyticsOpen, setAnalyticsOpen] = useState(true);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [showAddTopic, setShowAddTopic] = useState(false);
  const [editingTopicId, setEditingTopicId] = useState<string | null>(null);
  const [topicForm, setTopicForm] = useState<TopicFormState>(emptyTopicForm);
  const [topicFormError, setTopicFormError] = useState("");
  const [topicSaving, setTopicSaving] = useState(false);
  const [showAddLesson, setShowAddLesson] = useState(false);
  const [lessonForm, setLessonForm] = useState<LessonFormState>(emptyLessonForm);
  const [lessonFormError, setLessonFormError] = useState("");
  const [lessonSaving, setLessonSaving] = useState(false);
  const [showAddScenario, setShowAddScenario] = useState(false);
  const [showAddDict, setShowAddDict] = useState(false);
  const [showAddPron, setShowAddPron] = useState(false);
  const [scenarioForm, setScenarioForm] = useState<AdminScenarioPayload>({ title: "", titleVi: "", domain: "backend", difficulty: "beginner", missionVi: "", persona: {}, objectives: [], requiredVocabulary: [], active: true });
  const [dictionaryForm, setDictionaryForm] = useState<AdminDictionaryPayload>({ korean: "", pronunciation: "", vietnamese: "", domain: "backend", hanja: "", frequency: "medium" });
  const [pronunciationForm, setPronunciationForm] = useState<AdminPronunciationPayload>({ title: "", titleVi: "", domain: "backend", difficulty: "beginner", order: 0, sentences: [{ text: "", translationVi: "", audioUrl: "" }] });
  const [videoForm, setVideoForm] = useState<AdminVideoPayload>({ title: "", titleVi: "", youtubeUrl: "", domain: "backend", difficulty: "beginner", durationSeconds: 0, koreanSubtitles: [], vietnameseSubtitles: [], quizMarkers: [] });
  const [videoSaving, setVideoSaving] = useState(false);
  const [videoFormError, setVideoFormError] = useState("");
  const [testScenario, setTestScenario] = useState<(typeof scenarioRows)[number] | null>(null);
  const [testMessages, setTestMessages] = useState<{ role: "ai" | "user"; text: string }[]>([]);
  const [testInput, setTestInput] = useState("");
  const [selectedPrompt, setSelectedPrompt] = useState<AdminPromptPayload | null>(null);
  const [promptContent, setPromptContent] = useState("");
  const [dictSearch, setDictSearch] = useState("");
  const [inviteEmail, setInviteEmail] = useState("");
  const [userSearch, setUserSearch] = useState("");
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [userRoleEdits, setUserRoleEdits] = useState<Record<string, string>>({});
  const [deactivatedUsers, setDeactivatedUsers] = useState<Set<string>>(new Set());
  const [showCreateUser, setShowCreateUser] = useState(false);
  const [createUserForm, setCreateUserForm] = useState({ name: '', email: '', password: '', role: 'ROLE_USER' });
  const [createUserLoading, setCreateUserLoading] = useState(false);
  const [createUserError, setCreateUserError] = useState('');
  const [createUserSuccess, setCreateUserSuccess] = useState('');
  const [videoFilterDomain, setVideoFilterDomain] = useState("all");
  const [videoFilterDiff, setVideoFilterDiff] = useState("all");
  const [videoFilterSubs, setVideoFilterSubs] = useState("all");
  const [videoBulkSelected, setVideoBulkSelected] = useState<Set<string>>(new Set());
  const [analyticsTab, setAnalyticsTab] = useState<"overview" | "users" | "content" | "ai">("overview");
  const [lessonEditorTab, setLessonEditorTab] = useState<"vocab" | "exercises" | "preview">("vocab");
  const [editingLessonId, setEditingLessonId] = useState<string | null>(null);
  const [dictImportStep, setDictImportStep] = useState<"upload" | "preview">("upload");
  const [dictImportRows, setDictImportRows] = useState<AdminDictionaryPayload[]>([]);
  const [dictImportError, setDictImportError] = useState("");
  const dictFileInputRef = useRef<HTMLInputElement>(null);

  const readDictionaryFile = async (file?: File) => {
    if (!file) return;
    try {
      const text = await file.text();
      const rows: AdminDictionaryPayload[] = file.name.toLowerCase().endsWith(".json")
        ? JSON.parse(text)
        : (() => {
            const [header, ...values] = text.trim().split(/\r?\n/).filter(Boolean).map(line => line.split(",").map(value => value.trim()));
            const index = (name: string) => header.indexOf(name);
            return values.map(value => ({ korean: value[index("korean")] ?? "", pronunciation: value[index("pronunciation")] ?? "", vietnamese: value[index("vietnamese")] ?? "", domain: value[index("domain")] ?? "", hanja: value[index("hanja")] ?? "", frequency: value[index("frequency")] ?? "medium" }));
          })();
      if (!Array.isArray(rows) || !rows.length || rows.some(row => !row.korean || !row.vietnamese)) throw new Error("File must contain korean and vietnamese for every entry.");
      setDictImportRows(rows.slice(0, 10000)); setDictImportError(""); setDictImportStep("preview");
    } catch (error) { setDictImportError(error instanceof Error ? error.message : "Unable to read import file."); }
  };

  const fetchTopics = async () => {
    setTopicsLoading(true);
    setTopicsError("");
    try {
      setTopics(await api.getAdminTopics());
    } catch (error) {
      console.error("Failed to fetch topics:", error);
      setTopicsError(error instanceof Error ? error.message : "Unable to load topics.");
    } finally {
      setTopicsLoading(false);
    }
  };

  const fetchLessons = async (topicId = lessonFilterTopicId) => {
    setLessonsLoading(true);
    setLessonsError("");
    try {
      setLessons(await api.getAdminLessons(topicId || undefined));
    } catch (error) {
      console.error("Failed to fetch lessons:", error);
      setLessonsError(error instanceof Error ? error.message : "Unable to load lessons.");
    } finally {
      setLessonsLoading(false);
    }
  };

  useEffect(() => {
    if (section === "content-topics") {
      void fetchTopics();
    }
  }, [section]);

  useEffect(() => {
    if (section === "content-lessons") {
      void fetchTopics();
      void fetchLessons();
    }
  }, [section, lessonFilterTopicId]);

  const openNewTopic = () => {
    setEditingTopicId(null);
    setTopicForm(emptyTopicForm());
    setTopicFormError("");
    setShowAddTopic(true);
  };

  const openEditTopic = (topic: AdminTopicPayload) => {
    setEditingTopicId(topic.id ?? null);
    setTopicForm({
      title: topic.title ?? "",
      titleVi: topic.titleVi ?? "",
      domain: topic.domain ?? "frontend",
      description: topic.description ?? "",
      order: String(topic.order ?? 0),
      prerequisiteTopicIds: (topic.prerequisiteTopicIds ?? []).join(", "),
      tags: (topic.tags ?? []).join(", "),
      isActive: topic.isActive !== false,
    });
    setTopicFormError("");
    setShowAddTopic(true);
  };

  const saveTopic = async () => {
    if (!topicForm.title.trim() || !topicForm.titleVi.trim() || !topicForm.domain.trim()) {
      setTopicFormError("Korean title, Vietnamese title, and domain are required.");
      return;
    }

    const order = Number(topicForm.order);
    if (!Number.isInteger(order) || order < 0) {
      setTopicFormError("Order must be a whole number that is zero or greater.");
      return;
    }

    const payload: AdminTopicPayload = {
      title: topicForm.title.trim(),
      titleVi: topicForm.titleVi.trim(),
      domain: topicForm.domain.trim(),
      description: topicForm.description.trim(),
      order,
      prerequisiteTopicIds: commaSeparatedList(topicForm.prerequisiteTopicIds),
      tags: commaSeparatedList(topicForm.tags),
      isActive: topicForm.isActive,
    };

    setTopicSaving(true);
    setTopicFormError("");
    try {
      if (editingTopicId) {
        await api.updateAdminTopic(editingTopicId, payload);
      } else {
        await api.createAdminTopic(payload);
      }
      setShowAddTopic(false);
      setEditingTopicId(null);
      await fetchTopics();
    } catch (error) {
      console.error("Failed to save topic:", error);
      setTopicFormError(error instanceof Error ? error.message : "Unable to save topic.");
    } finally {
      setTopicSaving(false);
    }
  };

  const deleteTopic = async (topic: AdminTopicPayload) => {
    if (!topic.id || !confirm(`Delete topic \"${topic.title}\"? This cannot be undone.`)) {
      return;
    }

    try {
      await api.deleteAdminTopic(topic.id);
      await fetchTopics();
    } catch (error) {
      console.error("Failed to delete topic:", error);
      setTopicsError(error instanceof Error ? error.message : "Unable to delete topic.");
    }
  };

  const openNewLesson = () => {
    setEditingLessonId(null);
    setLessonForm(emptyLessonForm(lessonFilterTopicId || topics[0]?.id || ""));
    setLessonFormError("");
    setShowAddLesson(true);
  };

  const openEditLesson = (lesson: AdminLessonPayload) => {
    setEditingLessonId(lesson.id ?? null);
    setLessonForm({
      topicId: lesson.topicId,
      title: lesson.title ?? "",
      titleVi: lesson.titleVi ?? "",
      content: lesson.content ?? "",
      contentVi: lesson.contentVi ?? "",
      order: String(lesson.order ?? 0),
      vocabulary: (lesson.vocabulary ?? []).map(item => ({ ...item })),
      exercises: (lesson.exercises ?? []).map(item => ({ ...item, options: [...(item.options ?? [])] })),
    });
    setLessonFormError("");
    setShowAddLesson(true);
  };

  const saveLesson = async () => {
    if (!lessonForm.topicId || !lessonForm.title.trim() || !lessonForm.titleVi.trim()) {
      setLessonFormError("Topic, Korean title, and Vietnamese title are required.");
      return;
    }
    const order = Number(lessonForm.order);
    if (!Number.isInteger(order) || order < 0) {
      setLessonFormError("Order must be a whole number that is zero or greater.");
      return;
    }

    const payload: AdminLessonPayload = {
      topicId: lessonForm.topicId,
      title: lessonForm.title.trim(),
      titleVi: lessonForm.titleVi.trim(),
      content: lessonForm.content.trim(),
      contentVi: lessonForm.contentVi.trim(),
      order,
      vocabulary: lessonForm.vocabulary,
      exercises: lessonForm.exercises.map(exercise => ({
        ...exercise,
        options: exercise.type === "multiple_choice" ? (exercise.options ?? []) : [],
      })),
    };

    setLessonSaving(true);
    setLessonFormError("");
    try {
      if (editingLessonId) {
        await api.updateAdminLesson(editingLessonId, payload);
      } else {
        await api.createAdminLesson(payload);
      }
      setShowAddLesson(false);
      setEditingLessonId(null);
      await fetchLessons();
    } catch (error) {
      console.error("Failed to save lesson:", error);
      setLessonFormError(error instanceof Error ? error.message : "Unable to save lesson.");
    } finally {
      setLessonSaving(false);
    }
  };

  const deleteLesson = async (lesson: AdminLessonPayload) => {
    if (!lesson.id || !confirm(`Delete lesson \"${lesson.title}\"? This cannot be undone.`)) {
      return;
    }
    try {
      await api.deleteAdminLesson(lesson.id);
      if (editingLessonId === lesson.id) {
        setShowAddLesson(false);
        setEditingLessonId(null);
      }
      await fetchLessons();
    } catch (error) {
      console.error("Failed to delete lesson:", error);
      setLessonsError(error instanceof Error ? error.message : "Unable to delete lesson.");
    }
  };

  const navTo = (s: AdminSection) => {
    setSection(s); setShowSubEditor(false); setShowAddVideo(false); setShowAddTopic(false);
    setShowAddLesson(false); setShowAddScenario(false); setShowAddDict(false); setShowAddPron(false);
    setTestScenario(null); setSelectedUserId(null); setVideoBulkSelected(new Set());
    setDictImportStep("upload"); setEditingLessonId(null); setLessonEditorTab("vocab");
    setEditingTopicId(null); setTopicFormError("");
    if (s === "analytics") setAnalyticsTab("overview");
  };

  const ttStyle = { background: "oklch(0.16 0.025 250)", border: "1px solid oklch(0.22 0.03 250)", borderRadius: 8, color: TEAL, fontSize: 12 };

  const domainColor = (d: string) => d === "frontend" ? TEAL : d === "devops" ? "#fb923c" : d === "agile" ? "#34d399" : "#a78bfa";
  const diffColor = (d: string) => d === "beginner" ? "#34d399" : d === "intermediate" ? AMBER : "#f87171";

  const AdminTable = ({ headers, children }: { headers: string[]; children: React.ReactNode }) => (
    <div style={{ borderRadius: 14, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", overflow: "hidden" }}>
      <table style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
          <tr style={{ borderBottom: "1px solid oklch(0.18 0.03 250)" }}>
            {headers.map(h => (
              <th key={h} style={{ padding: "12px 16px", textAlign: "left", fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, fontWeight: 600 }}>{h.toUpperCase()}</th>
            ))}
          </tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  );

  const AdminTd = ({ children, mono = false }: { children: React.ReactNode; mono?: boolean }) => (
    <td style={{ padding: "12px 16px", fontSize: 13, color: mono ? "oklch(0.55 0.03 250)" : "oklch(0.85 0.01 250)", fontFamily: mono ? "JetBrains Mono, monospace" : "Inter, sans-serif" }}>{children}</td>
  );

  const TR = ({ children }: { children: React.ReactNode }) => (
    <tr style={{ borderBottom: "1px solid oklch(0.15 0.025 250)" }}>{children}</tr>
  );

  const AddForm = ({ title, fields, onSave, onCancel }: { title: string; fields: { label: string; placeholder: string; type?: string }[]; onSave: () => void; onCancel: () => void }) => (
    <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: `1px solid ${TEAL}30`, marginBottom: 20 }}>
      <p style={{ fontSize: 11, color: TEAL, fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 14px" }}>{title}</p>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 14 }}>
        {fields.map(f => (
          <div key={f.label}>
            <p style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: "0 0 5px" }}>{f.label}</p>
            {f.type === "select" ? (
              <select style={{ width: "100%", padding: "10px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none" }}>
                <option>frontend</option><option>backend</option><option>devops</option><option>agile</option>
              </select>
            ) : (
              <input placeholder={f.placeholder} style={{ width: "100%", padding: "10px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none", boxSizing: "border-box" }} />
            )}
          </div>
        ))}
      </div>
      <div style={{ display: "flex", gap: 10 }}>
        <button onClick={onSave} style={{ padding: "9px 20px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer" }}>Save</button>
        <button onClick={onCancel} style={{ padding: "9px 20px", borderRadius: 10, border: "1px solid oklch(0.25 0.03 250)", background: "none", color: "oklch(0.55 0.03 250)", fontFamily: "Outfit, sans-serif", fontSize: 13, cursor: "pointer" }}>Cancel</button>
      </div>
    </div>
  );

  const kpiCards = [
    { label: "Total Users", value: dUsers.toLocaleString(), Icon: Users, color: TEAL, sub: "+12% this month" },
    { label: "DAU", value: dDauCount.toLocaleString(), Icon: TrendingUp, color: "#34d399", sub: "+8% vs last week" },
    { label: "MAU", value: (stats?.mau ?? 0).toLocaleString(), Icon: BarChart2, color: "#a78bfa", sub: "active users in the last 30 days" },
    { label: "Content", value: dContent.toLocaleString(), Icon: BookOpen, color: AMBER, sub: "managed lessons" },
  ];

  const SideItem = ({ id, icon: Icon, label, indent = false }: { id: AdminSection; icon: React.ComponentType<{ size: number; color: string }>; label: string; indent?: boolean }) => (
    <button onClick={() => navTo(id)} style={{ width: "100%", padding: indent ? "9px 20px 9px 36px" : "11px 20px", border: "none", background: section === id ? `${TEAL}10` : "none", cursor: "pointer", display: "flex", alignItems: "center", gap: 10, borderLeft: `2px solid ${section === id ? TEAL : "transparent"}` }}>
      <Icon size={14} color={section === id ? TEAL : "oklch(0.42 0.03 250)"} />
      <span style={{ fontSize: indent ? 12 : 13, color: section === id ? TEAL : "oklch(0.52 0.03 250)", fontWeight: section === id ? 600 : 400 }}>{label}</span>
    </button>
  );

  const SideGroup = ({ label, open, onToggle }: { label: string; open: boolean; onToggle: () => void }) => (
    <button onClick={onToggle} style={{ width: "100%", padding: "8px 20px", border: "none", background: "none", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
      <span style={{ fontSize: 9, color: "oklch(0.35 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1.5, fontWeight: 700 }}>{label}</span>
      <span style={{ fontSize: 10, color: "oklch(0.35 0.03 250)" }}>{open ? "▾" : "▸"}</span>
    </button>
  );

  return (
    <div style={{ display: "flex", width: "100vw", height: "100vh", background: "oklch(0.08 0.02 250)" }}>
      <div style={{ width: 228, background: "oklch(0.11 0.025 250)", borderRight: "1px solid oklch(0.18 0.03 250)", display: "flex", flexDirection: "column", flexShrink: 0, overflowY: "auto" }}>
        <div style={{ padding: "24px 20px 20px", borderBottom: "1px solid oklch(0.16 0.03 250)" }}>
          <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 20, color: TEAL }}>KAPOR</span>
          <p style={{ fontSize: 10, color: "oklch(0.38 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: "2px 0 0", letterSpacing: 1 }}>ADMIN PANEL</p>
        </div>
        <div style={{ padding: "12px 0", flex: 1 }}>
          <SideItem id="dashboard" icon={BarChart2} label="Dashboard" />
          <SideItem id="users" icon={Users} label="Users" />
          <div style={{ margin: "8px 0 2px" }}>
            <SideGroup label="CONTENT" open={contentOpen} onToggle={() => setContentOpen(o => !o)} />
            {contentOpen && (
              <>
                <SideItem id="content-topics" icon={Target} label="Topics" indent />
                <SideItem id="content-lessons" icon={BookOpen} label="Lessons" indent />
                <SideItem id="content-videos" icon={Video} label="Videos" indent />
                <SideItem id="content-scenarios" icon={MessageSquare} label="Scenarios" indent />
                <SideItem id="content-dictionary" icon={Globe} label="Dictionary" indent />
                <SideItem id="content-dictionary-import" icon={Plus} label="↳ Bulk Import" indent />
                <SideItem id="content-pronunciation" icon={Mic} label="Pronunciation" indent />
              </>
            )}
          </div>
          <div style={{ margin: "8px 0 2px" }}>
            <SideGroup label="ANALYTICS" open={analyticsOpen} onToggle={() => setAnalyticsOpen(o => !o)} />
            {analyticsOpen && (
              <>
                <SideItem id="analytics" icon={TrendingUp} label="Overview" indent />
                <SideItem id="analytics-users" icon={Users} label="Users" indent />
                <SideItem id="analytics-content" icon={BookOpen} label="Content" indent />
                <SideItem id="analytics-ai" icon={Zap} label="AI Usage" indent />
              </>
            )}
          </div>
          <div style={{ margin: "8px 0 2px" }}>
            <SideGroup label="SETTINGS" open={settingsOpen} onToggle={() => setSettingsOpen(o => !o)} />
            {settingsOpen && (
              <>
                <SideItem id="settings-prompts" icon={Zap} label="AI Prompts" indent />
                <SideItem id="settings-admins" icon={User} label="Admin Users" indent />
              </>
            )}
          </div>
        </div>
        <div style={{ padding: "16px 20px", borderTop: "1px solid oklch(0.16 0.03 250)" }}>
          <p style={{ fontSize: 11, color: "oklch(0.55 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: "0 0 2px" }}>admin@kapor.app</p>
          <p style={{ fontSize: 9, color: "oklch(0.32 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>Super Admin</p>
        </div>
      </div>

      <div style={{ flex: 1, overflow: "auto", padding: "28px 32px" }}>
        {section === "dashboard" && (
          <div>
            <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: "0 0 24px" }}>Dashboard</h1>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 16, marginBottom: 24 }}>
              {kpiCards.map(({ label, value, Icon, color, sub }) => (
                <div key={label} style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 12 }}>
                    <div style={{ width: 36, height: 36, borderRadius: 10, background: `${color}18`, display: "flex", alignItems: "center", justifyContent: "center" }}>
                      <Icon size={16} color={color} />
                    </div>
                  </div>
                  <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 28, color, margin: "0 0 2px" }}>{value}</p>
                  <p style={{ fontSize: 12, color: "oklch(0.50 0.03 250)", margin: "0 0 4px" }}>{label}</p>
                  <p style={{ fontSize: 10, color: "oklch(0.38 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>{sub}</p>
                </div>
              ))}
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "2fr 1fr", gap: 16, marginBottom: 16 }}>
              <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 16px" }}>User Growth (2025)</p>
                <ResponsiveContainer width="100%" height={200}>
                  <LineChart id="line-user-growth" data={dUserGrowth}>
                    <CartesianGrid key="cg" stroke="oklch(0.17 0.03 250)" strokeDasharray="3 3" />
                    <XAxis key="x" dataKey="month" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                    <YAxis key="y" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                    <Tooltip key="tt" contentStyle={ttStyle} />
                    <Line key="l" type="monotone" dataKey="users" stroke={TEAL} strokeWidth={2} dot={false} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
              <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 16px" }}>DAU This Week</p>
                <ResponsiveContainer width="100%" height={200}>
                  <BarChart id="bar-dau-week" data={dDau}>
                    <XAxis key="x" dataKey="day" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                    <Tooltip key="tt" contentStyle={{ ...ttStyle, color: "#a78bfa" }} />
                    <Bar key="b" dataKey="dau" fill="#a78bfa" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
              <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 14px" }}>Popular Content (Top 5)</p>
                {popularContentData.map((item, i) => (
                  <div key={i} style={{ display: "flex", alignItems: "center", gap: 12, padding: "8px 0", borderBottom: i < 4 ? "1px solid oklch(0.17 0.03 250)" : "none" }}>
                    <span style={{ fontFamily: "JetBrains Mono, monospace", fontSize: 11, color: "oklch(0.38 0.03 250)", width: 16, flexShrink: 0 }}>{i + 1}</span>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <p style={{ fontSize: 12, color: "oklch(0.82 0.01 250)", margin: 0, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{item.title}</p>
                      <div style={{ display: "flex", gap: 6, marginTop: 2 }}>
                        <KBadge color={domainColor(item.domain)}>{item.domain}</KBadge>
                        <span style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>{item.type}</span>
                      </div>
                    </div>
                    <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, color: TEAL, flexShrink: 0 }}>{item.completions}</span>
                  </div>
                ))}
              </div>
              <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 14px" }}>Recent Activity</p>
                {users.slice(0, 5).map((act, i) => (
                  <div key={i} style={{ display: "flex", alignItems: "center", gap: 10, padding: "8px 0", borderBottom: i < 4 ? "1px solid oklch(0.17 0.03 250)" : "none" }}>
                    <div style={{ width: 28, height: 28, borderRadius: "50%", background: `${TEAL}12`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 14, flexShrink: 0 }}>{act.avatar}</div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <p style={{ fontSize: 12, color: "oklch(0.82 0.01 250)", margin: 0 }}><span style={{ color: TEAL }}>{act.name}</span> · registered account</p>
                      <p style={{ fontSize: 10, color: "oklch(0.45 0.03 250)", margin: 0, fontFamily: "JetBrains Mono, monospace", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{act.email}</p>
                    </div>
                    <span style={{ fontSize: 10, color: "oklch(0.38 0.03 250)", fontFamily: "JetBrains Mono, monospace", flexShrink: 0 }}>{act.joined}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {section === "users" && (
          <div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
              <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: 0 }}>Users</h1>
              <button onClick={() => { setShowCreateUser(v => !v); setCreateUserError(''); setCreateUserSuccess(''); }} style={{ padding: "10px 18px", borderRadius: 10, border: "none", background: showCreateUser ? '#f87171' : TEAL, color: showCreateUser ? '#fff' : '#000', fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer", display: "flex", alignItems: "center", gap: 6, transition: 'all 0.2s' }}>
                {showCreateUser ? <><X size={15} /> Cancel</> : <><Plus size={15} /> Create User</>}
              </button>
            </div>

            {/* Create User Form */}
            {showCreateUser && (
              <div style={{ borderRadius: 14, padding: 24, background: "oklch(0.13 0.025 250)", border: `1px solid ${TEAL}33`, marginBottom: 20 }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 15, color: TEAL, margin: "0 0 16px", letterSpacing: 1 }}>NEW USER</p>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 12 }}>
                  <div>
                    <label style={{ display: "block", fontFamily: "JetBrains Mono, monospace", fontSize: 10, color: "oklch(0.50 0.03 250)", marginBottom: 6, letterSpacing: 1 }}>DISPLAY NAME</label>
                    <input value={createUserForm.name} onChange={e => setCreateUserForm(f => ({ ...f, name: e.target.value }))} placeholder="Nguyễn Văn A" style={{ width: "100%", padding: "10px 14px", borderRadius: 8, border: "1px solid oklch(0.22 0.03 250)", background: "oklch(0.10 0.02 250)", color: "oklch(0.90 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none", boxSizing: "border-box" }} />
                  </div>
                  <div>
                    <label style={{ display: "block", fontFamily: "JetBrains Mono, monospace", fontSize: 10, color: "oklch(0.50 0.03 250)", marginBottom: 6, letterSpacing: 1 }}>EMAIL</label>
                    <input type="email" value={createUserForm.email} onChange={e => setCreateUserForm(f => ({ ...f, email: e.target.value }))} placeholder="user@example.com" style={{ width: "100%", padding: "10px 14px", borderRadius: 8, border: "1px solid oklch(0.22 0.03 250)", background: "oklch(0.10 0.02 250)", color: "oklch(0.90 0.01 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 13, outline: "none", boxSizing: "border-box" }} />
                  </div>
                  <div>
                    <label style={{ display: "block", fontFamily: "JetBrains Mono, monospace", fontSize: 10, color: "oklch(0.50 0.03 250)", marginBottom: 6, letterSpacing: 1 }}>PASSWORD</label>
                    <input type="password" value={createUserForm.password} onChange={e => setCreateUserForm(f => ({ ...f, password: e.target.value }))} placeholder="••••••••" style={{ width: "100%", padding: "10px 14px", borderRadius: 8, border: "1px solid oklch(0.22 0.03 250)", background: "oklch(0.10 0.02 250)", color: "oklch(0.90 0.01 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 13, outline: "none", boxSizing: "border-box" }} />
                  </div>
                  <div>
                    <label style={{ display: "block", fontFamily: "JetBrains Mono, monospace", fontSize: 10, color: "oklch(0.50 0.03 250)", marginBottom: 6, letterSpacing: 1 }}>ROLE</label>
                    <select value={createUserForm.role} onChange={e => setCreateUserForm(f => ({ ...f, role: e.target.value }))} style={{ width: "100%", padding: "10px 14px", borderRadius: 8, border: "1px solid oklch(0.22 0.03 250)", background: "oklch(0.10 0.02 250)", color: "oklch(0.90 0.01 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 13, outline: "none", boxSizing: "border-box", cursor: "pointer" }}>
                      <option value="ROLE_USER">Student</option>
                      <option value="ROLE_PREMIUM">Premium</option>
                      <option value="ROLE_ADMIN">Admin</option>
                    </select>
                  </div>
                </div>
                {createUserError && <p style={{ color: "#f87171", fontSize: 12, fontFamily: "JetBrains Mono, monospace", margin: "0 0 10px" }}>❌ {createUserError}</p>}
                {createUserSuccess && <p style={{ color: "#34d399", fontSize: 12, fontFamily: "JetBrains Mono, monospace", margin: "0 0 10px" }}>✅ {createUserSuccess}</p>}
                <button
                  disabled={createUserLoading || !createUserForm.name || !createUserForm.email || !createUserForm.password}
                  onClick={async () => {
                    setCreateUserLoading(true);
                    setCreateUserError('');
                    setCreateUserSuccess('');
                    try {
                      await api.createUser(createUserForm);
                      setCreateUserSuccess(`User "${createUserForm.name}" created successfully!`);
                      setCreateUserForm({ name: '', email: '', password: '', role: 'ROLE_USER' });
                      fetchUsers(usersPage, userSearch);
                      setTimeout(() => { setCreateUserSuccess(''); setShowCreateUser(false); }, 2000);
                    } catch (e: any) {
                      setCreateUserError(e.message || 'Failed to create user');
                    } finally {
                      setCreateUserLoading(false);
                    }
                  }}
                  style={{ padding: "10px 24px", borderRadius: 8, border: "none", background: (!createUserForm.name || !createUserForm.email || !createUserForm.password) ? 'oklch(0.25 0.03 250)' : TEAL, color: (!createUserForm.name || !createUserForm.email || !createUserForm.password) ? 'oklch(0.45 0.03 250)' : '#000', fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: (!createUserForm.name || !createUserForm.email || !createUserForm.password) ? 'not-allowed' : 'pointer', opacity: createUserLoading ? 0.7 : 1 }}
                >
                  {createUserLoading ? 'Creating...' : 'Create User'}
                </button>
              </div>
            )}

            <div style={{ display: "flex", gap: 10, marginBottom: 16 }}>
              <input
                value={userSearch} onChange={e => setUserSearch(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter') { setUsersPage(1); fetchUsers(1, userSearch); } }}
                placeholder="Search by name or email…"
                style={{ flex: 1, padding: "10px 14px", borderRadius: 10, background: "oklch(0.12 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.85 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none" }}
              />
              <button onClick={() => { setUsersPage(1); fetchUsers(1, userSearch); }} style={{ padding: "10px 16px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer" }}>Search</button>
              <span style={{ padding: "10px 14px", borderRadius: 10, background: "oklch(0.12 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 12, display: "flex", alignItems: "center" }}>
                {users.length} results
              </span>
            </div>
            {usersLoading ? (
              <div style={{ textAlign: "center", padding: 40, color: "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 13 }}>Loading users...</div>
            ) : users.length === 0 ? (
              <div style={{ textAlign: "center", padding: 40, color: "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 13 }}>No users found</div>
            ) : (
            <div style={{ borderRadius: 14, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", overflow: "hidden" }}>
              <table style={{ width: "100%", borderCollapse: "collapse" }}>
                <thead>
                  <tr style={{ borderBottom: "1px solid oklch(0.18 0.03 250)" }}>
                    {["User", "Email", "Streak", "Joined", "Role", "Actions"].map(h => (
                      <th key={h} style={{ padding: "12px 16px", textAlign: "left", fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, fontWeight: 600 }}>{h.toUpperCase()}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {users
                    .map(u => {
                      const isDeactivated = deactivatedUsers.has(u.id);
                      const currentRole = userRoleEdits[u.id] ?? u.role;
                      return (
                        <tr key={u.id} style={{ borderBottom: "1px solid oklch(0.15 0.025 250)", opacity: isDeactivated ? 0.45 : 1 }}>
                          <td style={{ padding: "12px 16px" }}>
                            <button onClick={() => { setSelectedUserId(u.id); setSection("users-detail"); }} style={{ display: "flex", alignItems: "center", gap: 10, background: "none", border: "none", cursor: "pointer", padding: 0 }}>
                              <div style={{ width: 32, height: 32, borderRadius: "50%", background: `${TEAL}12`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 16 }}>{u.avatar}</div>
                              <span style={{ fontSize: 13, color: TEAL, fontWeight: 500, textDecoration: "underline", textDecorationColor: `${TEAL}44` }}>{u.name}</span>
                            </button>
                          </td>
                          <td style={{ padding: "12px 16px", fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: "oklch(0.50 0.03 250)" }}>{u.email}</td>
                          <td style={{ padding: "12px 16px" }}>
                            <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
                              <Flame size={12} color={AMBER} />
                              <span style={{ fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: AMBER }}>{u.streak}</span>
                            </div>
                          </td>
                          <td style={{ padding: "12px 16px", fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: "oklch(0.42 0.03 250)" }}>{u.joined}</td>
                          <td style={{ padding: "12px 16px" }}>
                            <select
                              value={currentRole}
                              onChange={e => setUserRoleEdits(prev => ({ ...prev, [u.id]: e.target.value }))}
                              style={{ padding: "4px 8px", borderRadius: 6, background: "oklch(0.10 0.02 250)", border: `1px solid ${currentRole === "admin" ? `${AMBER}44` : currentRole === "premium" ? `${TEAL}44` : "oklch(0.25 0.03 250)"}`, color: currentRole === "admin" ? AMBER : currentRole === "premium" ? TEAL : "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 11, outline: "none", cursor: "pointer" }}
                            >
                              <option value="student">student</option>
                              <option value="premium">premium</option>
                              <option value="admin">admin</option>
                            </select>
                          </td>
                          <td style={{ padding: "12px 16px" }}>
                            <div style={{ display: "flex", gap: 6 }}>
                              {userRoleEdits[u.id] && (
                                <button onClick={async () => {
                                  try {
                                    const roleMap: Record<string, string> = { admin: 'ROLE_ADMIN', premium: 'ROLE_PREMIUM', student: 'ROLE_USER' };
                                    await api.updateUserRole(u.id, roleMap[userRoleEdits[u.id]] || 'ROLE_USER');
                                    setUserRoleEdits(prev => { const n = { ...prev }; delete n[u.id]; return n; });
                                    fetchUsers(usersPage, userSearch);
                                  } catch (e) { console.error('Failed to update role:', e); }
                                }} style={{ padding: "4px 10px", borderRadius: 6, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Save</button>
                              )}
                              <button onClick={async () => {
                                if (confirm('Are you sure you want to delete this user?')) {
                                  try {
                                    await api.deleteUser(u.id);
                                    fetchUsers(usersPage, userSearch);
                                  } catch (e) { console.error('Failed to delete user:', e); }
                                }
                              }} style={{ padding: "4px 10px", borderRadius: 6, border: "1px solid #f8717144", background: "#f8717110", color: "#f87171", fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>
                                Delete
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })
                  }
                </tbody>
              </table>
            </div>
            )}
            {/* Pagination */}
            {usersTotalPages > 1 && (
              <div style={{ display: "flex", justifyContent: "center", gap: 8, marginTop: 16 }}>
                <button disabled={usersPage <= 1} onClick={() => setUsersPage(p => p - 1)} style={{ padding: "6px 14px", borderRadius: 8, border: "1px solid oklch(0.22 0.03 250)", background: "oklch(0.12 0.02 250)", color: usersPage <= 1 ? "oklch(0.30 0.03 250)" : TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 12, cursor: usersPage <= 1 ? "not-allowed" : "pointer" }}>← Prev</button>
                <span style={{ padding: "6px 14px", fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: "oklch(0.55 0.03 250)", display: "flex", alignItems: "center" }}>Page {usersPage} / {usersTotalPages}</span>
                <button disabled={usersPage >= usersTotalPages} onClick={() => setUsersPage(p => p + 1)} style={{ padding: "6px 14px", borderRadius: 8, border: "1px solid oklch(0.22 0.03 250)", background: "oklch(0.12 0.02 250)", color: usersPage >= usersTotalPages ? "oklch(0.30 0.03 250)" : TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 12, cursor: usersPage >= usersTotalPages ? "not-allowed" : "pointer" }}>Next →</button>
              </div>
            )}
          </div>
        )}

        {section === "users-detail" && (() => {
          const u = users.find(u => u.id === selectedUserId) ?? users[0];
          if (!u) return <div style={{ color: 'oklch(0.50 0.03 250)', fontFamily: 'JetBrains Mono, monospace', textAlign: 'center', padding: 40 }}>User not found</div>;
          const currentRole = userRoleEdits[u.id] ?? u.role;
          const typeColor = (t: string) => t === "Lesson" ? TEAL : t === "TechTalk" ? "#a78bfa" : t === "Pronunciation" ? "#34d399" : t === "MemByte" ? AMBER : "oklch(0.50 0.03 250)";
          return (
            <div>
              <button onClick={() => setSection("users")} style={{ display: "flex", alignItems: "center", gap: 6, background: "none", border: "none", cursor: "pointer", color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 12, marginBottom: 20, padding: 0 }}>
                <ChevronLeft size={14} /> Back to Users
              </button>
              <div style={{ borderRadius: 14, padding: 24, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", marginBottom: 20, display: "flex", alignItems: "center", gap: 20 }}>
                <div style={{ width: 64, height: 64, borderRadius: "50%", background: `${TEAL}15`, border: `2px solid ${TEAL}40`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 32 }}>{u.avatar}</div>
                <div style={{ flex: 1 }}>
                  <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 20, color: "oklch(0.92 0.01 250)", margin: "0 0 4px" }}>{u.name}</p>
                  <p style={{ fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: "oklch(0.50 0.03 250)", margin: "0 0 8px" }}>{u.email}</p>
                  <div style={{ display: "flex", gap: 8 }}>
                    <KBadge color={currentRole === "admin" ? AMBER : currentRole === "premium" ? TEAL : "oklch(0.50 0.03 250)"}>{currentRole}</KBadge>
                    <span style={{ display: "flex", alignItems: "center", gap: 3, fontSize: 12, color: AMBER, fontFamily: "JetBrains Mono, monospace" }}><Flame size={12} color={AMBER} />{u.streak} day streak</span>
                  </div>
                </div>
                <div style={{ display: "flex", gap: 10 }}>
                  <div style={{ textAlign: "center" }}>
                    <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 22, color: TEAL, margin: "0 0 2px" }}>{u.stats?.totalStudyMinutes?.toLocaleString() ?? '0'}</p>
                    <p style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>MINUTES</p>
                  </div>
                  <div style={{ width: 1, background: "oklch(0.20 0.03 250)" }} />
                  <div style={{ textAlign: "center" }}>
                    <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 22, color: "#a78bfa", margin: "0 0 2px" }}>{u.stats?.totalCardsReviewed?.toLocaleString() ?? '0'}</p>
                    <p style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>CARDS</p>
                  </div>
                  <div style={{ width: 1, background: "oklch(0.20 0.03 250)" }} />
                  <div style={{ textAlign: "center" }}>
                    <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 22, color: "#34d399", margin: "0 0 2px" }}>{u.stats?.totalVideosWatched ?? '0'}</p>
                    <p style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>VIDEOS</p>
                  </div>
                </div>
              </div>
              <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: 16, alignItems: "start", marginBottom: 20 }}>
                <div style={{ borderRadius: 14, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", overflow: "hidden" }}>
                  <div style={{ padding: "14px 16px", borderBottom: "1px solid oklch(0.18 0.03 250)" }}>
                    <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: 0 }}>Activity Log</p>
                  </div>
                  <table style={{ width: "100%", borderCollapse: "collapse" }}>
                    <thead>
                      <tr style={{ borderBottom: "1px solid oklch(0.18 0.03 250)" }}>
                        {["Date", "Type", "Activity", "Duration", "Score"].map(h => (
                          <th key={h} style={{ padding: "10px 16px", textAlign: "left", fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, fontWeight: 600 }}>{h.toUpperCase()}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {userActivityLog.map((a, i) => (
                        <tr key={i} style={{ borderBottom: "1px solid oklch(0.15 0.025 250)" }}>
                          <td style={{ padding: "10px 16px", fontFamily: "JetBrains Mono, monospace", fontSize: 11, color: "oklch(0.42 0.03 250)" }}>{a.date}</td>
                          <td style={{ padding: "10px 16px" }}><KBadge color={typeColor(a.type)}>{a.type}</KBadge></td>
                          <td style={{ padding: "10px 16px", fontSize: 13, color: "oklch(0.82 0.01 250)" }}>{a.action}</td>
                          <td style={{ padding: "10px 16px", fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: "oklch(0.55 0.03 250)" }}>{a.duration}</td>
                          <td style={{ padding: "10px 16px", fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: a.score ? (a.score >= 90 ? "#34d399" : a.score >= 75 ? TEAL : AMBER) : "oklch(0.35 0.03 250)" }}>
                            {a.score ? `${a.score}%` : "—"}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", minWidth: 200 }}>
                  <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 13, color: "oklch(0.82 0.01 250)", margin: "0 0 14px" }}>Change Role</p>
                  <select
                    value={currentRole}
                    onChange={e => setUserRoleEdits(prev => ({ ...prev, [u.id]: e.target.value }))}
                    style={{ width: "100%", padding: "9px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 12, outline: "none", marginBottom: 10 }}
                  >
                    <option value="student">student</option>
                    <option value="premium">premium</option>
                    <option value="admin">admin</option>
                  </select>
                  <button onClick={async () => {
                    try {
                      const roleMap: Record<string, string> = { admin: 'ROLE_ADMIN', premium: 'ROLE_PREMIUM', student: 'ROLE_USER' };
                      await api.updateUserRole(u.id, roleMap[currentRole] || 'ROLE_USER');
                      setUserRoleEdits(prev => { const n = { ...prev }; delete n[u.id]; return n; });
                      fetchUsers(usersPage, userSearch);
                    } catch (e) { console.error('Failed to update role:', e); }
                  }} style={{ width: "100%", padding: "9px", borderRadius: 8, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer", marginBottom: 8 }}>Save Role</button>
                  <button onClick={async () => {
                    if (confirm('Are you sure you want to delete this user?')) {
                      try {
                        await api.deleteUser(u.id);
                        setSection('users');
                        fetchUsers(usersPage, userSearch);
                      } catch (e) { console.error('Failed to delete user:', e); }
                    }
                  }} style={{ width: "100%", padding: "9px", borderRadius: 8, border: "1px solid #f8717144", background: "#f8717110", color: "#f87171", fontFamily: "Outfit, sans-serif", fontSize: 13, cursor: "pointer" }}>
                    Delete User
                  </button>
                </div>
              </div>
            </div>
          );
        })()}

        {section === "content-topics" && (
          <div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 24 }}>
              <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: 0 }}>Topics</h1>
              <button onClick={openNewTopic} style={{ padding: "8px 16px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer", display: "flex", alignItems: "center", gap: 6 }}>
                <Plus size={15} /> New Topic
              </button>
            </div>
            {showAddTopic && (
              <form onSubmit={event => { event.preventDefault(); void saveTopic(); }} style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: `1px solid ${TEAL}30`, marginBottom: 20 }}>
                <p style={{ fontSize: 11, color: TEAL, fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 14px" }}>{editingTopicId ? "EDIT TOPIC" : "NEW TOPIC"}</p>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 12 }}>
                  <label style={{ display: "grid", gap: 5 }}>
                    <span style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>Title (Korean)</span>
                    <input required value={topicForm.title} onChange={event => setTopicForm(current => ({ ...current, title: event.target.value }))} placeholder="CSS Grid & Flexbox 용어" style={{ width: "100%", padding: "10px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none", boxSizing: "border-box" }} />
                  </label>
                  <label style={{ display: "grid", gap: 5 }}>
                    <span style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>Title (Vietnamese)</span>
                    <input required value={topicForm.titleVi} onChange={event => setTopicForm(current => ({ ...current, titleVi: event.target.value }))} placeholder="Thuật ngữ CSS Grid & Flexbox" style={{ width: "100%", padding: "10px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none", boxSizing: "border-box" }} />
                  </label>
                  <label style={{ display: "grid", gap: 5 }}>
                    <span style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>Domain</span>
                    <select value={topicForm.domain} onChange={event => setTopicForm(current => ({ ...current, domain: event.target.value }))} style={{ width: "100%", padding: "10px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none" }}>
                      <option value="frontend">frontend</option><option value="backend">backend</option><option value="devops">devops</option><option value="agile">agile</option>
                    </select>
                  </label>
                  <label style={{ display: "grid", gap: 5 }}>
                    <span style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>Order</span>
                    <input required type="number" min="0" step="1" value={topicForm.order} onChange={event => setTopicForm(current => ({ ...current, order: event.target.value }))} style={{ width: "100%", padding: "10px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none", boxSizing: "border-box" }} />
                  </label>
                  <label style={{ display: "grid", gap: 5, gridColumn: "1 / -1" }}>
                    <span style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>Description</span>
                    <textarea value={topicForm.description} onChange={event => setTopicForm(current => ({ ...current, description: event.target.value }))} rows={3} placeholder="Describe the learning topic" style={{ width: "100%", padding: "10px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none", boxSizing: "border-box", resize: "vertical" }} />
                  </label>
                  <label style={{ display: "grid", gap: 5 }}>
                    <span style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>Prerequisite topic IDs</span>
                    <input value={topicForm.prerequisiteTopicIds} onChange={event => setTopicForm(current => ({ ...current, prerequisiteTopicIds: event.target.value }))} placeholder="id-1, id-2" style={{ width: "100%", padding: "10px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none", boxSizing: "border-box" }} />
                  </label>
                  <label style={{ display: "grid", gap: 5 }}>
                    <span style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>Tags</span>
                    <input value={topicForm.tags} onChange={event => setTopicForm(current => ({ ...current, tags: event.target.value }))} placeholder="CSS, layout" style={{ width: "100%", padding: "10px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none", boxSizing: "border-box" }} />
                  </label>
                </div>
                <label style={{ display: "inline-flex", alignItems: "center", gap: 8, marginBottom: 14, color: "oklch(0.72 0.01 250)", fontSize: 12, cursor: "pointer" }}>
                  <input type="checkbox" checked={topicForm.isActive} onChange={event => setTopicForm(current => ({ ...current, isActive: event.target.checked }))} /> Active and visible to learners
                </label>
                {topicFormError && <p role="alert" style={{ margin: "0 0 12px", color: "#f87171", fontSize: 12 }}>{topicFormError}</p>}
                <div style={{ display: "flex", gap: 10 }}>
                  <button type="submit" disabled={topicSaving} style={{ padding: "9px 20px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: topicSaving ? "wait" : "pointer", opacity: topicSaving ? 0.65 : 1 }}>{topicSaving ? "Saving..." : "Save"}</button>
                  <button type="button" onClick={() => { setShowAddTopic(false); setEditingTopicId(null); setTopicFormError(""); }} disabled={topicSaving} style={{ padding: "9px 20px", borderRadius: 10, border: "1px solid oklch(0.25 0.03 250)", background: "none", color: "oklch(0.55 0.03 250)", fontFamily: "Outfit, sans-serif", fontSize: 13, cursor: "pointer" }}>Cancel</button>
                </div>
              </form>
            )}
            {topicsError && <p role="alert" style={{ margin: "0 0 12px", color: "#f87171", fontSize: 12 }}>{topicsError}</p>}
            <AdminTable headers={["#", "Title", "Domain", "Status", "Tags", ""]}>
              {topicsLoading && <TR><td colSpan={6} style={{ padding: "24px 16px", textAlign: "center", color: "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 12 }}>Loading topics...</td></TR>}
              {!topicsLoading && topics.length === 0 && <TR><td colSpan={6} style={{ padding: "24px 16px", textAlign: "center", color: "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 12 }}>No topics found.</td></TR>}
              {!topicsLoading && topics.map(t => (
                <TR key={t.id}>
                  <AdminTd mono>{t.order}</AdminTd>
                  <td style={{ padding: "12px 16px" }}>
                    <p style={{ fontSize: 13, color: "oklch(0.88 0.01 250)", margin: "0 0 2px" }}>{t.title}</p>
                    <p style={{ fontSize: 11, color: "oklch(0.45 0.03 250)", margin: 0, fontFamily: "JetBrains Mono, monospace" }}>{t.titleVi}</p>
                    {t.description && <p style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", margin: "5px 0 0" }}>{t.description}</p>}
                  </td>
                  <td style={{ padding: "12px 16px" }}><KBadge color={domainColor(t.domain)}>{t.domain}</KBadge></td>
                  <td style={{ padding: "12px 16px" }}><KBadge color={t.isActive ? "#34d399" : "#f87171"}>{t.isActive ? "active" : "inactive"}</KBadge></td>
                  <AdminTd mono>{(t.tags ?? []).join(", ") || "—"}</AdminTd>
                  <td style={{ padding: "12px 16px", display: "flex", gap: 6 }}>
                    <button onClick={() => openEditTopic(t)} style={{ padding: "5px 10px", borderRadius: 7, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Edit</button>
                    <button onClick={() => void deleteTopic(t)} style={{ padding: "5px 10px", borderRadius: 7, border: "1px solid #f8717144", background: "#f8717110", color: "#f87171", fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Delete</button>
                  </td>
                </TR>
              ))}
            </AdminTable>
          </div>
        )}

        {section === "content-lessons" && (
          <div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
              <div>
                <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: 0 }}>Lessons</h1>
                <p style={{ margin: "4px 0 0", fontSize: 12, color: "oklch(0.46 0.03 250)" }}>Manage lessons linked to real Topics.</p>
              </div>
              <button onClick={openNewLesson} style={{ padding: "8px 16px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer", display: "flex", alignItems: "center", gap: 6 }}><Plus size={15} /> New Lesson</button>
            </div>

            <div style={{ display: "flex", gap: 10, marginBottom: 16, alignItems: "center" }}>
              <label style={{ fontSize: 11, color: "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>TOPIC FILTER</label>
              <select value={lessonFilterTopicId} onChange={event => setLessonFilterTopicId(event.target.value)} style={{ minWidth: 250, padding: "8px 10px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)" }}>
                <option value="">All topics</option>
                {topics.map(topic => <option key={topic.id} value={topic.id}>{topic.title} · {topic.domain}</option>)}
              </select>
            </div>

            {showAddLesson && (
              <form onSubmit={event => { event.preventDefault(); void saveLesson(); }} style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: `1px solid ${TEAL}35`, marginBottom: 20 }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
                  <p style={{ fontSize: 11, color: TEAL, fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: 0 }}>{editingLessonId ? "EDIT LESSON" : "NEW LESSON"}</p>
                  <button type="button" onClick={() => { setShowAddLesson(false); setEditingLessonId(null); setLessonFormError(""); }} aria-label="Close lesson editor" style={{ border: "none", background: "none", color: "oklch(0.52 0.03 250)", cursor: "pointer" }}><X size={17} /></button>
                </div>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(2, minmax(0, 1fr))", gap: 12 }}>
                  {[["Topic", "topicId"], ["Title (Korean)", "title"], ["Title (Vietnamese)", "titleVi"], ["Order", "order"]].map(([label, field]) => (
                    <label key={field} style={{ display: "grid", gap: 5 }}>
                      <span style={{ fontSize: 11, color: "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>{label}</span>
                      {field === "topicId" ? (
                        <select required value={lessonForm.topicId} onChange={event => setLessonForm(current => ({ ...current, topicId: event.target.value }))} style={{ padding: "10px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)" }}>
                          <option value="">Select a Topic</option>
                          {topics.map(topic => <option key={topic.id} value={topic.id}>{topic.title} · {topic.titleVi} · {topic.domain}</option>)}
                        </select>
                      ) : (
                        <input required={field !== "order"} type={field === "order" ? "number" : "text"} min={field === "order" ? 0 : undefined} value={(lessonForm as any)[field]} onChange={event => setLessonForm(current => ({ ...current, [field]: event.target.value }))} style={{ padding: "10px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)", boxSizing: "border-box" }} />
                      )}
                    </label>
                  ))}
                  {[["Content (Korean)", "content"], ["Content (Vietnamese)", "contentVi"]].map(([label, field]) => (
                    <label key={field} style={{ display: "grid", gap: 5 }}><span style={{ fontSize: 11, color: "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>{label}</span><textarea value={(lessonForm as any)[field]} onChange={event => setLessonForm(current => ({ ...current, [field]: event.target.value }))} rows={4} style={{ padding: "10px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)", resize: "vertical" }} /></label>
                  ))}
                </div>

                <div style={{ marginTop: 20 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}><p style={{ margin: 0, color: "oklch(0.82 0.01 250)", fontWeight: 700 }}>Vocabulary</p><button type="button" onClick={() => setLessonForm(current => ({ ...current, vocabulary: [...current.vocabulary, emptyVocabulary()] }))} style={{ padding: "6px 10px", borderRadius: 7, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, cursor: "pointer" }}>+ Add term</button></div>
                  {lessonForm.vocabulary.map((item, index) => (
                    <div key={item.id ?? index} style={{ display: "grid", gridTemplateColumns: "repeat(4, minmax(0, 1fr)) auto", gap: 8, padding: 10, marginBottom: 8, borderRadius: 10, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.18 0.03 250)" }}>
                      {["korean", "pronunciation", "vietnamese", "english", "context", "codeSnippet", "audioUrl"].map(field => <input key={field} placeholder={field} value={(item as any)[field] ?? ""} onChange={event => setLessonForm(current => ({ ...current, vocabulary: current.vocabulary.map((value, i) => i === index ? { ...value, [field]: event.target.value } : value) }))} style={{ padding: "8px", borderRadius: 6, minWidth: 0, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)" }} />)}
                      <button type="button" onClick={() => setLessonForm(current => ({ ...current, vocabulary: current.vocabulary.filter((_, i) => i !== index) }))} aria-label="Remove vocabulary term" style={{ border: "none", background: "none", color: "#f87171", cursor: "pointer" }}><X size={16} /></button>
                    </div>
                  ))}
                </div>

                <div style={{ marginTop: 20 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}><p style={{ margin: 0, color: "oklch(0.82 0.01 250)", fontWeight: 700 }}>Exercises</p><div style={{ display: "flex", gap: 6 }}><button type="button" onClick={() => setLessonForm(current => ({ ...current, exercises: [...current.exercises, emptyExercise()] }))} style={{ padding: "6px 10px", borderRadius: 7, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, cursor: "pointer" }}>+ Multiple choice</button><button type="button" onClick={() => setLessonForm(current => ({ ...current, exercises: [...current.exercises, { ...emptyExercise(), type: "fill_blank", options: [] }] }))} style={{ padding: "6px 10px", borderRadius: 7, border: `1px solid ${AMBER}44`, background: `${AMBER}10`, color: AMBER, cursor: "pointer" }}>+ Fill blank</button></div></div>
                  {lessonForm.exercises.map((exercise, index) => (
                    <div key={exercise.id ?? index} style={{ padding: 12, marginBottom: 8, borderRadius: 10, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.18 0.03 250)" }}>
                      <div style={{ display: "flex", gap: 8, marginBottom: 8 }}><select value={exercise.type} onChange={event => setLessonForm(current => ({ ...current, exercises: current.exercises.map((value, i) => i === index ? { ...value, type: event.target.value as LessonExercisePayload["type"], options: event.target.value === "multiple_choice" ? (value.options?.length ? value.options : ["", ""]) : [] } : value) }))} style={{ padding: "7px", borderRadius: 6, background: "oklch(0.13 0.025 250)", color: "oklch(0.80 0.01 250)" }}><option value="multiple_choice">Multiple choice</option><option value="fill_blank">Fill blank</option></select><button type="button" onClick={() => setLessonForm(current => ({ ...current, exercises: current.exercises.filter((_, i) => i !== index) }))} style={{ marginLeft: "auto", border: "none", background: "none", color: "#f87171", cursor: "pointer" }}>Delete</button></div>
                      {["question", "questionVi", "correctAnswer"].map(field => <input key={field} placeholder={field === "correctAnswer" ? "Correct answer" : field === "questionVi" ? "Question (Vietnamese)" : "Question (Korean)"} value={(exercise as any)[field] ?? ""} onChange={event => setLessonForm(current => ({ ...current, exercises: current.exercises.map((value, i) => i === index ? { ...value, [field]: event.target.value } : value) }))} style={{ width: "100%", boxSizing: "border-box", padding: "8px", marginBottom: 7, borderRadius: 6, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)" }} />)}
                      {exercise.type === "multiple_choice" && <input placeholder="Options, separated by commas" value={(exercise.options ?? []).join(", ")} onChange={event => setLessonForm(current => ({ ...current, exercises: current.exercises.map((value, i) => i === index ? { ...value, options: event.target.value.split(",").map(option => option.trim()) } : value) }))} style={{ width: "100%", boxSizing: "border-box", padding: "8px", borderRadius: 6, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)" }} />}
                    </div>
                  ))}
                </div>
                {lessonFormError && <p role="alert" style={{ margin: "14px 0 0", color: "#f87171", fontSize: 12 }}>{lessonFormError}</p>}
                <div style={{ display: "flex", gap: 10, marginTop: 16 }}><button type="submit" disabled={lessonSaving} style={{ padding: "9px 20px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontWeight: 700, cursor: lessonSaving ? "wait" : "pointer", opacity: lessonSaving ? .65 : 1 }}>{lessonSaving ? "Saving..." : "Save Lesson"}</button><button type="button" onClick={() => { setShowAddLesson(false); setEditingLessonId(null); }} disabled={lessonSaving} style={{ padding: "9px 20px", borderRadius: 10, border: "1px solid oklch(0.25 0.03 250)", background: "none", color: "oklch(0.55 0.03 250)", cursor: "pointer" }}>Cancel</button></div>
              </form>
            )}

            {lessonsError && <div role="alert" style={{ marginBottom: 12, color: "#f87171", fontSize: 12 }}>Could not load lessons: {lessonsError} <button onClick={() => void fetchLessons()} style={{ marginLeft: 8, color: TEAL, background: "none", border: "none", cursor: "pointer" }}>Retry</button></div>}
            <AdminTable headers={["Order", "Title", "Topic", "Domain", "Vocabulary", "Exercises", ""]}>
              {lessonsLoading && <TR><td colSpan={7} style={{ padding: "24px 16px", textAlign: "center", color: "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 12 }}>Loading lessons...</td></TR>}
              {!lessonsLoading && !lessonsError && lessons.length === 0 && <TR><td colSpan={7} style={{ padding: "24px 16px", textAlign: "center", color: "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 12 }}>No lessons found. Create one to get started.</td></TR>}
              {!lessonsLoading && lessons.map(lesson => <TR key={lesson.id}><AdminTd mono>{lesson.order}</AdminTd><td style={{ padding: "12px 16px" }}><p style={{ margin: "0 0 2px", color: "oklch(0.88 0.01 250)" }}>{lesson.title}</p><p style={{ margin: 0, fontSize: 11, color: "oklch(0.45 0.03 250)" }}>{lesson.titleVi}</p></td><td style={{ padding: "12px 16px" }}><p style={{ margin: "0 0 2px", fontSize: 12, color: "oklch(0.76 0.01 250)" }}>{lesson.topicTitle ?? topics.find(topic => topic.id === lesson.topicId)?.title ?? "Unknown topic"}</p><p style={{ margin: 0, fontSize: 10, color: "oklch(0.45 0.03 250)" }}>{lesson.topicTitleVi}</p></td><td style={{ padding: "12px 16px" }}><KBadge color={domainColor(lesson.domain ?? "")}>{lesson.domain ?? "—"}</KBadge></td><AdminTd mono>{lesson.vocabulary?.length ?? 0}</AdminTd><AdminTd mono>{lesson.exercises?.length ?? 0}</AdminTd><td style={{ padding: "12px 16px", display: "flex", gap: 6 }}><button onClick={() => openEditLesson(lesson)} style={{ padding: "5px 10px", borderRadius: 7, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, cursor: "pointer" }}>Edit</button><button onClick={() => void deleteLesson(lesson)} style={{ padding: "5px 10px", borderRadius: 7, border: "1px solid #f8717144", background: "#f8717110", color: "#f87171", cursor: "pointer" }}>Delete</button></td></TR>)}
            </AdminTable>
          </div>
        )}

        {section === "content-scenarios" && !testScenario && (
          <div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 24 }}>
              <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: 0 }}>Roleplay Scenarios</h1>
              <button onClick={() => setShowAddScenario(v => !v)} style={{ padding: "8px 16px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer", display: "flex", alignItems: "center", gap: 6 }}>
                <Plus size={15} /> New Scenario
              </button>
            </div>
            {showAddScenario && (
              <form onSubmit={async event => { event.preventDefault(); const saved = await api.createScenario(scenarioForm); setAdminScenariosData(rows => [...rows, saved]); setShowAddScenario(false); }} style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: `1px solid ${TEAL}30`, marginBottom: 20 }}>
                <p style={{ fontSize: 11, color: TEAL, fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 14px" }}>NEW SCENARIO</p>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
                  {[["title", "Title (Korean)"], ["titleVi", "Title (Vietnamese)"], ["domain", "Domain"], ["difficulty", "Difficulty"], ["missionVi", "Mission (Vietnamese)"], ["personaName", "Persona name"], ["personaRole", "Persona role"], ["company", "Company"]].map(([field, label]) => <input key={field} required={field === "title"} value={field.startsWith("persona") || field === "company" ? (field === "personaName" ? scenarioForm.persona?.name ?? "" : field === "personaRole" ? scenarioForm.persona?.role ?? "" : scenarioForm.persona?.company ?? "") : (scenarioForm as any)[field] ?? ""} placeholder={label} onChange={event => { const value = event.target.value; if (field === "personaName" || field === "personaRole" || field === "company") setScenarioForm(current => ({ ...current, persona: { ...current.persona, [field === "personaName" ? "name" : field === "personaRole" ? "role" : "company"]: value } })); else setScenarioForm(current => ({ ...current, [field]: value })); }} style={{ padding: "10px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "white" }} />)}
                </div>
                <div style={{ display: "flex", gap: 10, marginTop: 14 }}><button type="submit" style={{ padding: "9px 20px", border: "none", borderRadius: 9, background: TEAL }}>Save</button><button type="button" onClick={() => setShowAddScenario(false)} style={{ padding: "9px 20px", borderRadius: 9 }}>Cancel</button></div>
              </form>
            )}
            <AdminTable headers={["Title", "Persona", "Domain", "Difficulty", "Vocab", "Active", ""]}>
              {scenarioRows.map(s => (
                <TR key={s.id}>
                  <td style={{ padding: "12px 16px" }}>
                    <p style={{ fontSize: 13, color: "oklch(0.88 0.01 250)", margin: "0 0 2px" }}>{s.title}</p>
                    <p style={{ fontSize: 11, color: "oklch(0.45 0.03 250)", margin: 0, fontFamily: "JetBrains Mono, monospace" }}>{s.titleVi}</p>
                  </td>
                  <td style={{ padding: "12px 16px" }}>
                    <p style={{ fontSize: 12, color: "oklch(0.82 0.01 250)", margin: 0 }}>{s.personaName}</p>
                    <p style={{ fontSize: 10, color: "oklch(0.45 0.03 250)", margin: 0, fontFamily: "JetBrains Mono, monospace" }}>{s.personaRole} @ {s.company}</p>
                  </td>
                  <td style={{ padding: "12px 16px" }}><KBadge color={domainColor(s.domain)}>{s.domain}</KBadge></td>
                  <td style={{ padding: "12px 16px" }}><KBadge color={diffColor(s.difficulty)}>{s.difficulty}</KBadge></td>
                  <AdminTd mono>{s.requiredVocab} terms</AdminTd>
                  <td style={{ padding: "12px 16px" }}>
                    <span style={{ padding: "2px 8px", borderRadius: 999, fontSize: 10, fontFamily: "JetBrains Mono, monospace", background: s.active ? `${TEAL}20` : "oklch(0.20 0.03 250)", color: s.active ? TEAL : "oklch(0.50 0.03 250)", border: `1px solid ${s.active ? `${TEAL}44` : "oklch(0.25 0.03 250)"}` }}>
                      {s.active ? "active" : "draft"}
                    </span>
                  </td>
                  <td style={{ padding: "12px 16px", display: "flex", gap: 6 }}>
                    <button onClick={() => { setTestScenario(s); setTestMessages([{ role: "ai", text: `안녕하세요. 저는 ${s.personaName}입니다. 오늘 ${s.title} 연습을 시작할까요?` }]); setTestInput(""); }} style={{ padding: "5px 10px", borderRadius: 7, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Test</button>
                    <button style={{ padding: "5px 10px", borderRadius: 7, border: "1px solid oklch(0.25 0.03 250)", background: "none", color: "oklch(0.55 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Edit</button>
                  </td>
                </TR>
              ))}
            </AdminTable>
          </div>
        )}

        {section === "content-scenarios" && testScenario && (
          <div style={{ maxWidth: 640 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 20 }}>
              <button onClick={() => setTestScenario(null)} style={{ width: 32, height: 32, borderRadius: 10, background: "oklch(0.16 0.025 250)", border: "none", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <ChevronLeft size={16} color={TEAL} />
              </button>
              <div>
                <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 20, color: "oklch(0.92 0.01 250)", margin: 0 }}>Test: {testScenario.title}</h1>
                <p style={{ fontSize: 11, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>Persona: {testScenario.personaName} ({testScenario.personaRole} @ {testScenario.company})</p>
              </div>
            </div>
            <div style={{ borderRadius: 14, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", padding: 20, minHeight: 340, display: "flex", flexDirection: "column", gap: 12, marginBottom: 12 }}>
              {testMessages.map((msg, i) => (
                <div key={i} style={{ display: "flex", justifyContent: msg.role === "ai" ? "flex-start" : "flex-end" }}>
                  <div style={{ maxWidth: "72%", borderRadius: 12, padding: "10px 14px", background: msg.role === "ai" ? `${TEAL}15` : "oklch(0.20 0.03 250)", border: `1px solid ${msg.role === "ai" ? `${TEAL}30` : "oklch(0.28 0.03 250)"}` }}>
                    {msg.role === "ai" && <p style={{ fontSize: 9, color: TEAL, fontFamily: "JetBrains Mono, monospace", margin: "0 0 4px" }}>{testScenario.personaName}</p>}
                    <p style={{ fontSize: 13, color: "oklch(0.88 0.01 250)", margin: 0 }}>{msg.text}</p>
                  </div>
                </div>
              ))}
            </div>
            <div style={{ display: "flex", gap: 10 }}>
              <input value={testInput} onChange={e => setTestInput(e.target.value)} placeholder="Type a message to test the scenario..." onKeyDown={e => {
                if (e.key === "Enter" && testInput.trim()) {
                  const userMsg = testInput.trim();
                  setTestMessages(m => [...m, { role: "user", text: userMsg }, { role: "ai", text: "네, 이해했습니다. 구체적으로 어떤 오류가 발생했나요?" }]);
                  setTestInput("");
                }
              }} style={{ flex: 1, padding: "10px 14px", borderRadius: 10, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.85 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none" }} />
              <button onClick={() => { if (!testInput.trim()) return; const t = testInput.trim(); setTestMessages(m => [...m, { role: "user", text: t }, { role: "ai", text: "네, 이해했습니다. 구체적으로 어떤 오류가 발생했나요?" }]); setTestInput(""); }} style={{ padding: "10px 18px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer", display: "flex", alignItems: "center", gap: 6 }}>
                <Send size={14} /> Send
              </button>
            </div>
          </div>
        )}

        {section === "content-dictionary" && (
          <div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
              <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: 0 }}>Dictionary</h1>
              <div style={{ display: "flex", gap: 8 }}>
                <button onClick={() => setShowAddDict(v => !v)} style={{ padding: "8px 16px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer", display: "flex", alignItems: "center", gap: 6 }}>
                  <Plus size={15} /> Add Entry
                </button>
                <button style={{ padding: "8px 14px", borderRadius: 10, border: `1px solid ${TEAL}40`, background: `${TEAL}10`, color: TEAL, fontFamily: "Outfit, sans-serif", fontSize: 13, cursor: "pointer" }}>Import CSV</button>
              </div>
            </div>
            <div style={{ display: "flex", gap: 10, marginBottom: 16 }}>
              <input value={dictSearch} onChange={e => setDictSearch(e.target.value)} placeholder="Search Korean terms..." style={{ flex: 1, padding: "10px 14px", borderRadius: 10, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.85 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none" }} />
            </div>
            {showAddDict && (
              <form onSubmit={async event => { event.preventDefault(); const saved = await api.createDictionary(dictionaryForm); setDictionaryEntries(rows => [...rows, saved]); setShowAddDict(false); }} style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: `1px solid ${TEAL}30`, marginBottom: 20 }}>
                <p style={{ fontSize: 11, color: TEAL, fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 14px" }}>NEW DICTIONARY ENTRY</p>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 10 }}>{[["korean", "Korean"], ["pronunciation", "Pronunciation"], ["vietnamese", "Vietnamese"], ["domain", "Domain"], ["hanja", "Hanja"], ["frequency", "Frequency"]].map(([field, label]) => <input key={field} required={field === "korean" || field === "vietnamese"} value={(dictionaryForm as any)[field] ?? ""} placeholder={label} onChange={event => setDictionaryForm(current => ({ ...current, [field]: event.target.value }))} style={{ padding: "10px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "white" }} />)}</div>
                <div style={{ display: "flex", gap: 10, marginTop: 14 }}><button type="submit" style={{ padding: "9px 20px", border: "none", borderRadius: 9, background: TEAL }}>Save</button><button type="button" onClick={() => setShowAddDict(false)} style={{ padding: "9px 20px", borderRadius: 9 }}>Cancel</button></div>
              </form>
            )}
            <AdminTable headers={["Korean", "Pronunciation", "Vietnamese", "Domain", "Frequency", ""]}>
              {dictionaryEntries.filter(d => d.korean.includes(dictSearch) || d.vietnamese.toLowerCase().includes(dictSearch.toLowerCase())).map(d => (
                <TR key={d.id}>
                  <td style={{ padding: "12px 16px" }}><span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 18, color: TEAL }}>{d.korean}</span></td>
                  <AdminTd mono>/{d.pronunciation}/</AdminTd>
                  <AdminTd>{d.vietnamese}</AdminTd>
                  <td style={{ padding: "12px 16px" }}><KBadge color={domainColor(d.domain)}>{d.domain}</KBadge></td>
                  <td style={{ padding: "12px 16px" }}>
                    <span style={{ padding: "2px 8px", borderRadius: 999, fontSize: 10, fontFamily: "JetBrains Mono, monospace", background: d.frequency === "high" ? `${TEAL}20` : "oklch(0.20 0.03 250)", color: d.frequency === "high" ? TEAL : "oklch(0.50 0.03 250)", border: `1px solid ${d.frequency === "high" ? `${TEAL}44` : "oklch(0.25 0.03 250)"}` }}>{d.frequency}</span>
                  </td>
                  <td style={{ padding: "12px 16px", display: "flex", gap: 6 }}>
                    <button style={{ padding: "5px 10px", borderRadius: 7, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Edit</button>
                    <button style={{ padding: "5px 10px", borderRadius: 7, border: "1px solid #f8717144", background: "#f8717110", color: "#f87171", fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Delete</button>
                  </td>
                </TR>
              ))}
            </AdminTable>
            <p style={{ fontSize: 11, color: "oklch(0.38 0.03 250)", fontFamily: "JetBrains Mono, monospace", marginTop: 12 }}>{dictionaryEntries.length} entries total</p>
          </div>
        )}

        {section === "content-pronunciation" && (
          <div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 24 }}>
              <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: 0 }}>Pronunciation Exercises</h1>
              <button onClick={() => setShowAddPron(v => !v)} style={{ padding: "8px 16px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer", display: "flex", alignItems: "center", gap: 6 }}>
                <Plus size={15} /> New Exercise
              </button>
            </div>
            {showAddPron && (
              <form onSubmit={async event => { event.preventDefault(); const saved = await api.createPronunciationExercise(pronunciationForm); setPronunciationExerciseData(rows => [...rows, saved]); setShowAddPron(false); }} style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: `1px solid ${TEAL}30`, marginBottom: 20 }}>
                <p style={{ fontSize: 11, color: TEAL, fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 14px" }}>NEW PRONUNCIATION EXERCISE</p>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>{[["title", "Title (Korean)"], ["titleVi", "Title (Vietnamese)"], ["domain", "Domain"], ["difficulty", "Difficulty"], ["text", "Sentence (Korean)"], ["translationVi", "Sentence (Vietnamese)"], ["audioUrl", "Reference audio URL"]].map(([field, label]) => <input key={field} required={field === "title" || field === "text"} value={field === "text" || field === "translationVi" || field === "audioUrl" ? (pronunciationForm.sentences[0] as any)[field] ?? "" : (pronunciationForm as any)[field] ?? ""} placeholder={label} onChange={event => { const value = event.target.value; if (field === "text" || field === "translationVi" || field === "audioUrl") setPronunciationForm(current => ({ ...current, sentences: [{ ...current.sentences[0], [field]: value }] })); else setPronunciationForm(current => ({ ...current, [field]: value })); }} style={{ padding: "10px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "white" }} />)}</div>
                <div style={{ display: "flex", gap: 10, marginTop: 14 }}><button type="submit" style={{ padding: "9px 20px", border: "none", borderRadius: 9, background: TEAL }}>Save</button><button type="button" onClick={() => setShowAddPron(false)} style={{ padding: "9px 20px", borderRadius: 9 }}>Cancel</button></div>
              </form>
            )}
            <AdminTable headers={["Title", "Domain", "Difficulty", "Sentences", "Reference Audio", ""]}>
              {pronunciationExerciseData.map(p => (
                <TR key={p.id}>
                  <td style={{ padding: "12px 16px" }}>
                    <p style={{ fontSize: 13, color: "oklch(0.88 0.01 250)", margin: "0 0 2px" }}>{p.title}</p>
                    <p style={{ fontSize: 11, color: "oklch(0.45 0.03 250)", margin: 0, fontFamily: "JetBrains Mono, monospace" }}>{p.titleVi}</p>
                  </td>
                  <td style={{ padding: "12px 16px" }}><KBadge color={domainColor(p.domain)}>{p.domain}</KBadge></td>
                  <td style={{ padding: "12px 16px" }}><KBadge color={diffColor(p.difficulty)}>{p.difficulty}</KBadge></td>
                  <AdminTd mono>{p.sentences?.length ?? 0} sentences</AdminTd>
                  <td style={{ padding: "12px 16px" }}>
                    <span style={{ fontSize: 12, fontFamily: "JetBrains Mono, monospace", color: p.sentences?.every(sentence => Boolean(sentence.audioUrl)) ? "#34d399" : "#f87171" }}>{p.sentences?.every(sentence => Boolean(sentence.audioUrl)) ? "✓ uploaded" : "✗ missing"}</span>
                  </td>
                  <td style={{ padding: "12px 16px", display: "flex", gap: 6 }}>
                    <button style={{ padding: "5px 10px", borderRadius: 7, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Edit</button>
                    <button style={{ padding: "5px 10px", borderRadius: 7, border: "1px solid #60a5fa44", background: "#60a5fa10", color: "#60a5fa", fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Upload Audio</button>
                  </td>
                </TR>
              ))}
            </AdminTable>
          </div>
        )}

        {section === "content-dictionary-import" && (
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 24 }}>
              <button onClick={() => setSection("content-dictionary")} style={{ display: "flex", alignItems: "center", gap: 6, background: "none", border: "none", cursor: "pointer", color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 12, padding: 0 }}>
                <ChevronLeft size={14} /> Dictionary
              </button>
              <span style={{ color: "oklch(0.30 0.03 250)" }}>/</span>
              <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: 0 }}>Bulk Import</h1>
            </div>
            {dictImportStep === "upload" && (
              <div>
                <div style={{ borderRadius: 14, padding: 40, background: "oklch(0.13 0.025 250)", border: `2px dashed ${TEAL}44`, textAlign: "center", marginBottom: 20 }}>
                  <div style={{ width: 48, height: 48, borderRadius: 12, background: `${TEAL}15`, border: `1px solid ${TEAL}30`, display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 16px" }}>
                    <Plus size={22} color={TEAL} />
                  </div>
                  <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 16, color: "oklch(0.82 0.01 250)", margin: "0 0 6px" }}>Drop CSV or JSON file here</p>
                  <p style={{ fontSize: 12, color: "oklch(0.42 0.03 250)", margin: "0 0 20px" }}>Supports .csv and .json formats. Max 10,000 entries per upload.</p>
                  <div style={{ display: "flex", gap: 10, justifyContent: "center" }}>
                    <input ref={dictFileInputRef} type="file" accept=".csv,.json,application/json,text/csv" onChange={event => void readDictionaryFile(event.target.files?.[0])} style={{ display: "none" }} />
                    <button onClick={() => dictFileInputRef.current?.click()} style={{ padding: "9px 20px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer" }}>Browse Files</button>
                  </div>
                  {dictImportError && <p style={{ color: "#f87171", fontSize: 12 }}>{dictImportError}</p>}
                </div>
                <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", marginBottom: 20 }}>
                  <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 12px" }}>Expected CSV Format</p>
                  <code style={{ display: "block", fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: TEAL, background: "oklch(0.09 0.02 250)", padding: 14, borderRadius: 8, lineHeight: 1.8 }}>
                    korean,pronunciation,vietnamese,domain,hanja,frequency<br />
                    빌드,bild-eu,Build / Biên dịch,devops,,high<br />
                    테스트,te-seu-teu,Kiểm thử,backend,,medium
                  </code>
                </div>
              </div>
            )}
            {dictImportStep === "preview" && (
              <div>
                <div style={{ borderRadius: 14, padding: 16, background: `${TEAL}10`, border: `1px solid ${TEAL}30`, marginBottom: 20, display: "flex", alignItems: "center", gap: 12 }}>
                  <CheckCircle size={18} color={TEAL} />
                  <p style={{ fontSize: 13, color: TEAL, fontFamily: "Inter, sans-serif", margin: 0 }}>
                    <strong>{dictImportRows.length} entries detected</strong> — Review below before importing.
                  </p>
                </div>
                <div style={{ borderRadius: 14, padding: 16, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", marginBottom: 16 }}>
                  <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 12px" }}>Column Mapping</p>
                  <div style={{ display: "grid", gridTemplateColumns: "repeat(3,1fr)", gap: 10 }}>
                    {[["Korean", "korean"], ["Pronunciation", "pronunciation"], ["Vietnamese", "vietnamese"], ["Domain", "domain"], ["Hanja", "hanja"], ["Frequency", "frequency"]].map(([label, val]) => (
                      <div key={label}>
                        <p style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: "0 0 4px" }}>{label.toUpperCase()}</p>
                        <select defaultValue={val} style={{ width: "100%", padding: "8px 10px", borderRadius: 7, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.75 0.01 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 11, outline: "none" }}>
                          <option value={val}>{val}</option>
                          <option value="—">— skip —</option>
                        </select>
                      </div>
                    ))}
                  </div>
                </div>
                <div style={{ borderRadius: 14, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", overflow: "hidden", marginBottom: 20 }}>
                  <div style={{ padding: "12px 16px", borderBottom: "1px solid oklch(0.18 0.03 250)" }}>
                    <p style={{ fontSize: 13, color: "oklch(0.82 0.01 250)", fontFamily: "Outfit, sans-serif", fontWeight: 600, margin: 0 }}>Preview (first 5 rows)</p>
                  </div>
                  <table style={{ width: "100%", borderCollapse: "collapse" }}>
                    <thead>
                      <tr style={{ borderBottom: "1px solid oklch(0.18 0.03 250)" }}>
                        {["Korean", "Pronunciation", "Vietnamese", "Domain"].map(h => (
                          <th key={h} style={{ padding: "10px 16px", textAlign: "left", fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, fontWeight: 600 }}>{h.toUpperCase()}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {dictImportRows.slice(0, 5).map((entry, i) => (
                        <tr key={i} style={{ borderBottom: "1px solid oklch(0.15 0.025 250)" }}>
                          <td style={{ padding: "10px 16px" }}><span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 15, color: TEAL }}>{entry.korean}</span></td>
                          <td style={{ padding: "10px 16px", fontFamily: "JetBrains Mono, monospace", fontSize: 11, color: "oklch(0.50 0.03 250)" }}>/{entry.pronunciation}/</td>
                          <td style={{ padding: "10px 16px", fontSize: 12, color: "oklch(0.75 0.01 250)" }}>{entry.vietnamese}</td>
                          <td style={{ padding: "10px 16px" }}><KBadge color={domainColor(entry.domain)}>{entry.domain}</KBadge></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <div style={{ display: "flex", gap: 10 }}>
                  <button onClick={async () => { const saved = await api.importDictionary(dictImportRows); setDictionaryEntries(current => [...current, ...saved]); setDictImportStep("upload"); setDictImportRows([]); setSection("content-dictionary"); }} style={{ padding: "10px 24px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, cursor: "pointer" }}>Import {dictImportRows.length} Entries</button>
                  <button onClick={() => setDictImportStep("upload")} style={{ padding: "10px 24px", borderRadius: 10, border: "1px solid oklch(0.25 0.03 250)", background: "none", color: "oklch(0.55 0.03 250)", fontFamily: "Outfit, sans-serif", fontSize: 14, cursor: "pointer" }}>Back</button>
                </div>
              </div>
            )}
          </div>
        )}

        {section === "settings-prompts" && (
          <div>
            <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: "0 0 24px" }}>AI Prompt Templates</h1>
            <div style={{ display: "grid", gridTemplateColumns: "260px 1fr", gap: 20 }}>
              <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                {promptTemplates.map(pt => (
                  <button key={pt.id} onClick={() => { setSelectedPrompt(pt); setPromptContent(pt.content); }} style={{ padding: "12px 14px", borderRadius: 10, border: `1px solid ${selectedPrompt?.id === pt.id ? `${TEAL}50` : "oklch(0.20 0.03 250)"}`, background: selectedPrompt?.id === pt.id ? `${TEAL}10` : "oklch(0.13 0.025 250)", textAlign: "left", cursor: "pointer" }}>
                    <p style={{ fontSize: 13, fontWeight: 600, color: selectedPrompt?.id === pt.id ? TEAL : "oklch(0.82 0.01 250)", margin: "0 0 3px", fontFamily: "Outfit, sans-serif" }}>{pt.name}</p>
                    <p style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", margin: 0, fontFamily: "JetBrains Mono, monospace" }}>{pt.description}</p>
                  </button>
                ))}
              </div>
              <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", display: "flex", flexDirection: "column" }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
                  <div>
                    <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 16, color: "oklch(0.92 0.01 250)", margin: "0 0 2px" }}>{selectedPrompt?.name ?? "No prompt selected"}</p>
                    <p style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>{selectedPrompt?.id ? `${selectedPrompt.id}.txt` : "Create a prompt in the database to edit it."}</p>
                  </div>
                  <KBadge color={TEAL}>Gemini 2.5 Pro</KBadge>
                </div>
                <textarea value={promptContent} onChange={e => setPromptContent(e.target.value)} rows={14} style={{ flex: 1, borderRadius: 10, padding: "12px 14px", background: "oklch(0.09 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.82 0.01 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 12, outline: "none", resize: "vertical", lineHeight: 1.7 }} />
                <div style={{ display: "flex", gap: 10, marginTop: 14 }}>
                  <button disabled={!selectedPrompt?.id} onClick={async () => { if (!selectedPrompt?.id) return; const saved = await api.updatePrompt(selectedPrompt.id, { ...selectedPrompt, content: promptContent }); setPromptTemplates(rows => rows.map(row => row.id === saved.id ? saved : row)); setSelectedPrompt(saved); }} style={{ padding: "9px 20px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer" }}>Save Changes</button>
                  <button onClick={() => setPromptContent(selectedPrompt?.content ?? "")} style={{ padding: "9px 16px", borderRadius: 10, border: "1px solid oklch(0.25 0.03 250)", background: "none", color: "oklch(0.55 0.03 250)", fontFamily: "Outfit, sans-serif", fontSize: 13, cursor: "pointer" }}>Reset</button>
                  <div style={{ marginLeft: "auto", padding: "9px 0" }}>
                    <span style={{ fontSize: 10, color: "oklch(0.38 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>{promptContent.length} chars · {promptContent.split("\n").length} lines</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {section === "settings-admins" && (
          <div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 24 }}>
              <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: 0 }}>Admin Users</h1>
            </div>
            <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: `1px solid ${TEAL}30`, marginBottom: 20 }}>
              <p style={{ fontSize: 11, color: TEAL, fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 12px" }}>INVITE NEW ADMIN</p>
              <div style={{ display: "flex", gap: 10 }}>
                <input value={inviteEmail} onChange={e => setInviteEmail(e.target.value)} placeholder="Email address..." style={{ flex: 1, padding: "10px 14px", borderRadius: 10, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.85 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none" }} />
                <select style={{ padding: "10px 14px", borderRadius: 10, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.75 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none" }}>
                  <option>content_admin</option>
                  <option>analytics_viewer</option>
                  <option>super_admin</option>
                </select>
                <button onClick={async () => { if (!inviteEmail.trim()) return; await api.grantAdmin(inviteEmail); setInviteEmail(""); await loadAdminData(); }} style={{ padding: "10px 18px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer" }}>Grant admin</button>
              </div>
            </div>
            <AdminTable headers={["Admin", "Email", "Role", "Last Login", ""]}>
              {adminUsersData.map(a => (
                <TR key={a.id}>
                  <td style={{ padding: "12px 16px", display: "flex", alignItems: "center", gap: 10 }}>
                    <div style={{ width: 32, height: 32, borderRadius: "50%", background: `${TEAL}12`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 16, flexShrink: 0 }}>{a.avatar}</div>
                    <span style={{ fontSize: 13, color: "oklch(0.88 0.01 250)", fontWeight: 500 }}>{a.name}</span>
                  </td>
                  <AdminTd mono>{a.email}</AdminTd>
                  <td style={{ padding: "12px 16px" }}>
                    <span style={{ padding: "2px 8px", borderRadius: 999, fontSize: 10, fontFamily: "JetBrains Mono, monospace", background: a.role === "super_admin" ? `${AMBER}20` : a.role === "content_admin" ? `${TEAL}20` : "oklch(0.20 0.03 250)", color: a.role === "super_admin" ? AMBER : a.role === "content_admin" ? TEAL : "oklch(0.50 0.03 250)", border: `1px solid ${a.role === "super_admin" ? `${AMBER}44` : a.role === "content_admin" ? `${TEAL}44` : "oklch(0.25 0.03 250)"}` }}>{a.role}</span>
                  </td>
                  <AdminTd mono>{a.lastLogin}</AdminTd>
                  <td style={{ padding: "12px 16px" }}>
                    <button onClick={async () => { if (confirm(`Revoke admin access for ${a.email}?`)) { await api.revokeAdmin(a.id); await loadAdminData(); } }} style={{ padding: "5px 10px", borderRadius: 7, border: "1px solid #f8717144", background: "#f8717110", color: "#f87171", fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Remove</button>
                  </td>
                </TR>
              ))}
            </AdminTable>
            <div style={{ marginTop: 20, borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
              <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 12px" }}>Role Permissions</p>
              {[
                { role: "super_admin", color: AMBER, perms: "Full access: users, content, analytics, settings, admin management" },
                { role: "content_admin", color: TEAL, perms: "Content CRUD (topics, lessons, videos, scenarios, dictionary, pronunciation). No user management." },
                { role: "analytics_viewer", color: "#60a5fa", perms: "Read-only access to analytics dashboard. No content editing." },
              ].map(r => (
                <div key={r.role} style={{ display: "flex", gap: 12, marginBottom: 10, padding: "10px 14px", borderRadius: 10, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.16 0.03 250)" }}>
                  <span style={{ padding: "1px 8px", borderRadius: 999, fontSize: 10, fontFamily: "JetBrains Mono, monospace", background: `${r.color}20`, color: r.color, border: `1px solid ${r.color}44`, height: "fit-content", flexShrink: 0 }}>{r.role}</span>
                  <p style={{ fontSize: 12, color: "oklch(0.62 0.03 250)", margin: 0, lineHeight: 1.5 }}>{r.perms}</p>
                </div>
              ))}
            </div>
          </div>
        )}

        {section === "content-videos" && !showSubEditor && (
          <div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
              <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: 0 }}>Videos</h1>
              <button onClick={() => { setShowAddVideo(v => !v); setVideoFormError(""); }} style={{ padding: "8px 16px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer", display: "flex", alignItems: "center", gap: 6 }}>
                <Plus size={15} /> Add Video
              </button>
            </div>
            <div style={{ display: "flex", gap: 8, marginBottom: 16, flexWrap: "wrap" }}>
              <span style={{ fontSize: 11, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", alignSelf: "center" }}>DOMAIN:</span>
              {["all", "frontend", "backend", "devops"].map(d => (
                <button key={d} onClick={() => setVideoFilterDomain(d)} style={{ padding: "5px 12px", borderRadius: 20, border: `1px solid ${videoFilterDomain === d ? TEAL : "oklch(0.25 0.03 250)"}`, background: videoFilterDomain === d ? `${TEAL}18` : "none", color: videoFilterDomain === d ? TEAL : "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>{d}</button>
              ))}
              <span style={{ fontSize: 11, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", alignSelf: "center", marginLeft: 8 }}>SUBS:</span>
              {[["all", "All"], ["완료", "Done"], ["진행중", "In Progress"], ["없음", "None"]].map(([v, label]) => (
                <button key={v} onClick={() => setVideoFilterSubs(v)} style={{ padding: "5px 12px", borderRadius: 20, border: `1px solid ${videoFilterSubs === v ? TEAL : "oklch(0.25 0.03 250)"}`, background: videoFilterSubs === v ? `${TEAL}18` : "none", color: videoFilterSubs === v ? TEAL : "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>{label}</button>
              ))}
            </div>
            {videoBulkSelected.size > 0 && (
              <div style={{ borderRadius: 10, padding: "10px 14px", background: `${TEAL}10`, border: `1px solid ${TEAL}30`, marginBottom: 12, display: "flex", alignItems: "center", gap: 12 }}>
                <span style={{ fontSize: 12, color: TEAL, fontFamily: "JetBrains Mono, monospace" }}>{videoBulkSelected.size} selected</span>
                <button onClick={() => setVideoBulkSelected(new Set())} style={{ padding: "4px 10px", borderRadius: 6, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Change Domain</button>
                <button onClick={() => setVideoBulkSelected(new Set())} style={{ padding: "4px 10px", borderRadius: 6, border: "1px solid #f8717144", background: "#f8717110", color: "#f87171", fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Delete Selected</button>
                <button onClick={() => setVideoBulkSelected(new Set())} style={{ marginLeft: "auto", background: "none", border: "none", cursor: "pointer", color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 11 }}>Clear</button>
              </div>
            )}
            {showAddVideo && (
              <form onSubmit={async event => {
                event.preventDefault();
                if (!videoForm.youtubeUrl?.trim() || !videoForm.title.trim() || !videoForm.titleVi?.trim()) {
                  setVideoFormError("YouTube URL, Korean title, and Vietnamese title are required.");
                  return;
                }
                try {
                  setVideoSaving(true); setVideoFormError("");
                  const saved = await api.createVideo({ ...videoForm, youtubeUrl: videoForm.youtubeUrl.trim(), title: videoForm.title.trim(), titleVi: videoForm.titleVi.trim() });
                  setVideos(rows => [...rows, saved]);
                  setShowAddVideo(false);
                  setVideoForm({ title: "", titleVi: "", youtubeUrl: "", domain: "backend", difficulty: "beginner", durationSeconds: 0, koreanSubtitles: [], vietnameseSubtitles: [], quizMarkers: [] });
                } catch (error) {
                  setVideoFormError(error instanceof Error ? error.message : "Unable to save video.");
                } finally { setVideoSaving(false); }
              }} style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: `1px solid ${TEAL}30`, marginBottom: 16 }}>
                <p style={{ fontSize: 11, color: TEAL, fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 14px" }}>ADD NEW VIDEO</p>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 14 }}>
                  {[["YouTube URL", "youtubeUrl", "https://youtube.com/watch?v=..."], ["Title (Korean)", "title", "강의 제목"], ["Title (Vietnamese)", "titleVi", "Tiêu đề bài học"], ["Thumbnail URL (optional)", "thumbnailUrl", "https://..."], ["Duration (seconds, optional)", "durationSeconds", "0"]].map(([label, field, placeholder]) => (
                    <label key={field} style={{ display: "grid", gap: 5 }}>
                      <span style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>{label}</span>
                      <input required={field === "youtubeUrl" || field === "title" || field === "titleVi"} type={field === "durationSeconds" ? "number" : "text"} min={field === "durationSeconds" ? 0 : undefined} value={(videoForm as any)[field] ?? ""} placeholder={placeholder} onChange={event => setVideoForm(current => ({ ...current, [field]: field === "durationSeconds" ? Number(event.target.value) : event.target.value }))} style={{ width: "100%", padding: "10px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none", boxSizing: "border-box" }} />
                    </label>
                  ))}
                  <label style={{ display: "grid", gap: 5 }}><span style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>Domain</span><select value={videoForm.domain} onChange={event => setVideoForm(current => ({ ...current, domain: event.target.value }))} style={{ padding: "10px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)" }}><option value="frontend">frontend</option><option value="backend">backend</option><option value="devops">devops</option><option value="agile">agile</option></select></label>
                  <label style={{ display: "grid", gap: 5 }}><span style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>Difficulty</span><select value={videoForm.difficulty} onChange={event => setVideoForm(current => ({ ...current, difficulty: event.target.value }))} style={{ padding: "10px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)" }}><option value="beginner">beginner</option><option value="intermediate">intermediate</option><option value="advanced">advanced</option></select></label>
                </div>
                {videoFormError && <p role="alert" style={{ color: "#f87171", fontSize: 12, margin: "0 0 12px" }}>{videoFormError}</p>}
                <div style={{ display: "flex", gap: 10 }}>
                  <button type="submit" disabled={videoSaving} style={{ padding: "9px 20px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: videoSaving ? "wait" : "pointer", opacity: videoSaving ? .7 : 1 }}>{videoSaving ? "Saving..." : "Save Video"}</button>
                  <button type="button" onClick={() => { setShowAddVideo(false); setVideoFormError(""); }} disabled={videoSaving} style={{ padding: "9px 20px", borderRadius: 10, border: "1px solid oklch(0.25 0.03 250)", background: "none", color: "oklch(0.55 0.03 250)", fontFamily: "Outfit, sans-serif", fontSize: 13, cursor: "pointer" }}>Cancel</button>
                </div>
              </form>
            )}
            <div style={{ borderRadius: 14, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", overflow: "hidden" }}>
              <table style={{ width: "100%", borderCollapse: "collapse" }}>
                <thead>
                  <tr style={{ borderBottom: "1px solid oklch(0.18 0.03 250)" }}>
                    <th style={{ padding: "12px 12px 12px 16px", width: 32 }}>
                      <input type="checkbox" onChange={e => setVideoBulkSelected(e.target.checked ? new Set(videoRows.map(v => v.id ?? "")) : new Set())} style={{ accentColor: TEAL }} />
                    </th>
                    {["Title", "Domain", "Difficulty", "Subtitles", "Quizzes", ""].map((h, i) => (
                      <th key={i} style={{ padding: "12px 16px", textAlign: "left", fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, fontWeight: 600 }}>{h.toUpperCase()}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {videoRows
                    .filter(v => (videoFilterDomain === "all" || v.domain === videoFilterDomain) && (videoFilterSubs === "all" || v.subtitles === videoFilterSubs))
                    .map(v => {
                      const subColor = v.subtitles === "완료" ? "#34d399" : v.subtitles === "진행중" ? AMBER : "#f87171";
                      const isSelected = videoBulkSelected.has(v.id);
                      return (
                        <tr key={v.id} style={{ borderBottom: "1px solid oklch(0.15 0.025 250)", background: isSelected ? `${TEAL}08` : "none" }}>
                          <td style={{ padding: "12px 12px 12px 16px" }}>
                            <input type="checkbox" checked={isSelected} onChange={e => setVideoBulkSelected(prev => { const n = new Set(prev); e.target.checked ? n.add(v.id) : n.delete(v.id); return n; })} style={{ accentColor: TEAL }} />
                          </td>
                          <td style={{ padding: "12px 16px", fontSize: 13, color: "oklch(0.85 0.01 250)" }}>{v.title}</td>
                          <td style={{ padding: "12px 16px" }}><KBadge color={domainColor(v.domain)}>{v.domain}</KBadge></td>
                          <td style={{ padding: "12px 16px" }}><KBadge color={diffColor(v.difficulty)}>{v.difficulty}</KBadge></td>
                          <td style={{ padding: "12px 16px", fontSize: 12, color: subColor, fontFamily: "JetBrains Mono, monospace" }}>{v.subtitles}</td>
                          <td style={{ padding: "12px 16px", fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: "oklch(0.55 0.03 250)" }}>{v.quizzes}</td>
                          <td style={{ padding: "12px 16px" }}>
                            <button onClick={() => { setSelectedVideo(v); setEditSubs((v.koreanSubtitles ?? []).map((line, index) => ({ id: String(index), start: String(line.start), end: String(line.end), ko: line.text, vi: v.vietnameseSubtitles?.[index]?.text ?? "" }))); setShowSubEditor(true); }} style={{ padding: "5px 12px", borderRadius: 7, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Edit Subs</button>
                          </td>
                        </tr>
                      );
                    })
                  }
                </tbody>
              </table>
              {videoRows.filter(v => (videoFilterDomain === "all" || v.domain === videoFilterDomain) && (videoFilterSubs === "all" || v.subtitles === videoFilterSubs)).length === 0 && (
                <p style={{ textAlign: "center", padding: "24px", fontSize: 13, color: "oklch(0.38 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>No videos match the current filters.</p>
              )}
            </div>
          </div>
        )}

        {section === "content-videos" && showSubEditor && (
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 24 }}>
              <button onClick={() => setShowSubEditor(false)} style={{ width: 32, height: 32, borderRadius: 10, background: "oklch(0.16 0.025 250)", border: "none", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <ChevronLeft size={16} color={TEAL} />
              </button>
              <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 22, color: "oklch(0.92 0.01 250)", margin: 0 }}>Subtitle Editor</h1>
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1.6fr", gap: 20 }}>
              <div>
                <div style={{ borderRadius: 14, overflow: "hidden", background: "#050d1a", aspectRatio: "16/9", display: "flex", alignItems: "center", justifyContent: "center", marginBottom: 12 }}>
                  <div style={{ textAlign: "center" }}>
                    <Play size={32} color={TEAL} />
                    <p style={{ fontSize: 11, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: "8px 0 0" }}>DEVIEW 2025</p>
                  </div>
                </div>
                <div style={{ display: "flex", gap: 8 }}>
                  <button style={{ flex: 1, padding: "8px 0", borderRadius: 8, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 12, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
                    <Zap size={13} /> Auto-tokenize
                  </button>
                  <button style={{ flex: 1, padding: "8px 0", borderRadius: 8, border: `1px solid ${TEAL}40`, background: `${TEAL}10`, color: TEAL, fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 12, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
                    <Plus size={13} /> Add Line
                  </button>
                </div>
              </div>
              <div style={{ borderRadius: 14, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", overflow: "hidden" }}>
                <div style={{ padding: "12px 16px", borderBottom: "1px solid oklch(0.18 0.03 250)", display: "grid", gridTemplateColumns: "72px 1fr 1fr", gap: 8 }}>
                  {["Time", "Korean (KR)", "Vietnamese (VI)"].map(h => (
                    <span key={h} style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>{h}</span>
                  ))}
                </div>
                {editSubs.map((row, i) => (
                  <div key={row.id} style={{ padding: "10px 16px", borderBottom: "1px solid oklch(0.14 0.025 250)", display: "grid", gridTemplateColumns: "72px 1fr 1fr", gap: 8, alignItems: "start" }}>
                    <div>
                      <p style={{ fontSize: 10, color: TEAL, fontFamily: "JetBrains Mono, monospace", margin: 0 }}>{row.start}</p>
                      <p style={{ fontSize: 10, color: "oklch(0.36 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>→ {row.end}</p>
                    </div>
                    <textarea value={row.ko} onChange={e => { const next = [...editSubs]; next[i] = { ...next[i], ko: e.target.value }; setEditSubs(next); }} rows={2} style={{ width: "100%", borderRadius: 6, padding: "6px 8px", background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.20 0.03 250)", color: "oklch(0.82 0.01 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 11, outline: "none", resize: "none", boxSizing: "border-box" }} />
                    <textarea value={row.vi} onChange={e => { const next = [...editSubs]; next[i] = { ...next[i], vi: e.target.value }; setEditSubs(next); }} rows={2} style={{ width: "100%", borderRadius: 6, padding: "6px 8px", background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.20 0.03 250)", color: "oklch(0.75 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 11, outline: "none", resize: "none", boxSizing: "border-box" }} />
                  </div>
                ))}
                <div style={{ padding: "12px 16px" }}>
                  <button onClick={async () => { if (!selectedVideo?.id) return; const koreanSubtitles = editSubs.map(row => ({ start: Number(row.start), end: Number(row.end), text: row.ko, tokens: [] })); const vietnameseSubtitles = editSubs.map(row => ({ start: Number(row.start), end: Number(row.end), text: row.vi, tokens: [] })); const saved = await api.updateVideo(selectedVideo.id, { ...selectedVideo, koreanSubtitles, vietnameseSubtitles }); setVideos(rows => rows.map(row => row.id === saved.id ? saved : row)); setShowSubEditor(false); }} style={{ padding: "9px 20px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer" }}>Save Changes</button>
                </div>
              </div>
            </div>
          </div>
        )}

        {section === "analytics" && (
          <div>
            <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: "0 0 24px" }}>Analytics — Overview</h1>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 12, marginBottom: 20 }}>
              {[
                { label: "DAU", value: String(dDauCount), sub: "today", color: TEAL },
                { label: "MAU", value: String(stats?.mau ?? 0), sub: "last 30 days", color: "#a78bfa" },
                { label: "Avg Session", value: `${stats?.averageSessionMinutes ?? 0}m`, sub: "from recorded activity", color: "#34d399" },
                { label: "Churn Rate", value: `${(stats?.churnRate ?? 0).toFixed(1)}%`, sub: "inactive in last 30 days", color: AMBER },
              ].map(k => (
                <div key={k.label} style={{ borderRadius: 12, padding: "16px 18px", background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
                  <p style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 6px" }}>{k.label.toUpperCase()}</p>
                  <p style={{ fontSize: 26, fontFamily: "Outfit, sans-serif", fontWeight: 800, color: k.color, margin: "0 0 4px" }}>{k.value}</p>
                  <p style={{ fontSize: 11, color: "oklch(0.42 0.03 250)", margin: 0 }}>{k.sub}</p>
                </div>
              ))}
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginBottom: 16 }}>
              <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 16px" }}>DAU Trend</p>
                <ResponsiveContainer width="100%" height={180}>
                  <LineChart id="line-analytics-dau" data={dDau}>
                    <CartesianGrid key="cg" stroke="oklch(0.17 0.03 250)" strokeDasharray="3 3" />
                    <XAxis key="x" dataKey="day" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                    <YAxis key="y" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                    <Tooltip key="tt" contentStyle={{ ...ttStyle, color: "#a78bfa" }} />
                    <Line key="l" type="monotone" dataKey="dau" stroke="#a78bfa" strokeWidth={2} dot={false} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
              <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 16px" }}>Lessons by Domain</p>
                <ResponsiveContainer width="100%" height={180}>
                  <BarChart id="bar-analytics-lessons" data={dLesson}>
                    <XAxis key="x" dataKey="domain" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                    <Tooltip key="tt" contentStyle={ttStyle} />
                    <Bar key="b" dataKey="completions" fill={TEAL} radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
            <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
              <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 16px" }}>AI API Cost (USD/month)</p>
              <ResponsiveContainer width="100%" height={180}>
                <AreaChart id="area-analytics-ai" data={dAiUsage}>
                  <CartesianGrid key="cg" stroke="oklch(0.17 0.03 250)" strokeDasharray="3 3" />
                  <XAxis key="x" dataKey="month" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                  <YAxis key="y" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                  <Tooltip key="tt" contentStyle={{ ...ttStyle, color: AMBER }} />
                  <Area key="a" type="monotone" dataKey="cost" stroke={AMBER} fill={`${AMBER}18`} strokeWidth={2} />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}

        {section === "analytics-users" && (
          <div>
            <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: "0 0 24px" }}>Analytics — User Engagement</h1>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 12, marginBottom: 20 }}>
              {[
                { label: "New Users", value: String(dNewReg.reduce((total, point) => total + point.count, 0)), sub: "last 7 days", color: TEAL },
                { label: "D1 Retention", value: `${dRetention.at(0)?.d1 ?? 0}%`, sub: "measured cohort", color: "#34d399" },
                { label: "D7 Retention", value: `${dRetention.at(0)?.d7 ?? 0}%`, sub: "measured cohort", color: "#a78bfa" },
                { label: "D30 Retention", value: `${dRetention.at(0)?.d30 ?? 0}%`, sub: "measured cohort", color: AMBER },
              ].map(k => (
                <div key={k.label} style={{ borderRadius: 12, padding: "16px 18px", background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
                  <p style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 6px" }}>{k.label.toUpperCase()}</p>
                  <p style={{ fontSize: 26, fontFamily: "Outfit, sans-serif", fontWeight: 800, color: k.color, margin: "0 0 4px" }}>{k.value}</p>
                  <p style={{ fontSize: 11, color: "oklch(0.42 0.03 250)", margin: 0 }}>{k.sub}</p>
                </div>
              ))}
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginBottom: 16 }}>
              <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 16px" }}>New Registrations / Day</p>
                <ResponsiveContainer width="100%" height={180}>
                  <BarChart id="bar-new-reg" data={dNewReg}>
                    <XAxis key="x" dataKey="day" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                    <YAxis key="y" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                    <Tooltip key="tt" contentStyle={ttStyle} />
                    <Bar key="b" dataKey="count" fill={TEAL} radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
              <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 16px" }}>Retention Cohort (D1 / D7 / D30)</p>
                <ResponsiveContainer width="100%" height={180}>
                  <BarChart id="bar-retention" data={dRetention}>
                    <XAxis key="x" dataKey="week" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                    <YAxis key="y" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                    <Tooltip key="tt" contentStyle={ttStyle} />
                    <Bar key="d1" dataKey="d1" fill="#34d399" radius={[2, 2, 0, 0]} />
                    <Bar key="d7" dataKey="d7" fill="#a78bfa" radius={[2, 2, 0, 0]} />
                    <Bar key="d30" dataKey="d30" fill={AMBER} radius={[2, 2, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
                <div style={{ display: "flex", gap: 16, marginTop: 10 }}>
                  {[["D1", "#34d399"], ["D7", "#a78bfa"], ["D30", AMBER]].map(([label, color]) => (
                    <div key={label} style={{ display: "flex", alignItems: "center", gap: 5 }}>
                      <div style={{ width: 8, height: 8, borderRadius: 2, background: color }} />
                      <span style={{ fontSize: 11, color: "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>{label}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {section === "analytics-content" && (
          <div>
            <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: "0 0 24px" }}>Analytics — Content Performance</h1>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginBottom: 20 }}>
              <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 16px" }}>Completions by Domain</p>
                <ResponsiveContainer width="100%" height={180}>
                  <BarChart id="bar-content-domain" data={dLesson}>
                    <XAxis key="x" dataKey="domain" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                    <YAxis key="y" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                    <Tooltip key="tt" contentStyle={ttStyle} />
                    <Bar key="b" dataKey="completions" fill={TEAL} radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
              <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 8px" }}>Most Searched Words</p>
                {[...dictionaryEntries].sort((a, b) => (b.searchCount ?? 0) - (a.searchCount ?? 0)).slice(0, 5).map((item, i) => (
                  <div key={item.word} style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 8 }}>
                    <span style={{ width: 20, fontFamily: "JetBrains Mono, monospace", fontSize: 10, color: "oklch(0.35 0.03 250)" }}>#{i + 1}</span>
                    <span style={{ flex: 1, fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, color: TEAL }}>{item.word}</span>
                    <div style={{ height: 6, width: `${Math.min(100, item.searchCount ?? 0)}px`, background: `${TEAL}44`, borderRadius: 3 }} />
                    <span style={{ fontFamily: "JetBrains Mono, monospace", fontSize: 11, color: "oklch(0.50 0.03 250)", minWidth: 30 }}>{item.searchCount ?? 0}</span>
                  </div>
                ))}
              </div>
            </div>
            <div style={{ borderRadius: 14, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", overflow: "hidden" }}>
              <div style={{ padding: "14px 16px", borderBottom: "1px solid oklch(0.18 0.03 250)" }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: 0 }}>Top Content by Completions</p>
              </div>
              <table style={{ width: "100%", borderCollapse: "collapse" }}>
                <thead>
                  <tr style={{ borderBottom: "1px solid oklch(0.18 0.03 250)" }}>
                    {["#", "Title", "Type", "Domain", "Completions", "Completion Rate"].map(h => (
                      <th key={h} style={{ padding: "10px 16px", textAlign: "left", fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, fontWeight: 600 }}>{h.toUpperCase()}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {contentPerformanceRows.map((item, i) => (
                    <tr key={item.title} style={{ borderBottom: "1px solid oklch(0.15 0.025 250)" }}>
                      <td style={{ padding: "10px 16px", fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: "oklch(0.42 0.03 250)" }}>#{i + 1}</td>
                      <td style={{ padding: "10px 16px", fontSize: 13, color: "oklch(0.88 0.01 250)" }}>{item.title}</td>
                      <td style={{ padding: "10px 16px" }}><KBadge color={item.type === "Lesson" ? TEAL : item.type === "Video" ? "#a78bfa" : item.type === "Scenario" ? AMBER : "#34d399"}>{item.type}</KBadge></td>
                      <td style={{ padding: "10px 16px" }}><KBadge color={domainColor(item.domain)}>{item.domain}</KBadge></td>
                      <td style={{ padding: "10px 16px", fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: TEAL }}>{item.completions}</td>
                      <td style={{ padding: "10px 16px" }}>
                        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                          <div style={{ height: 6, borderRadius: 3, background: "oklch(0.18 0.03 250)", flex: 1, maxWidth: 80, overflow: "hidden" }}>
                            <div style={{ height: "100%", borderRadius: 3, background: item.rate > 75 ? "#34d399" : item.rate > 60 ? TEAL : AMBER, width: `${item.rate}%` }} />
                          </div>
                          <span style={{ fontFamily: "JetBrains Mono, monospace", fontSize: 11, color: "oklch(0.55 0.03 250)", minWidth: 32 }}>{item.rate}%</span>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {section === "analytics-ai" && (
          <div>
            <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: "0 0 24px" }}>Analytics — AI Usage & Costs</h1>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 12, marginBottom: 20 }}>
              {[
                { label: "Total Calls", value: String(stats?.totalAiCalls ?? 0), sub: "recorded usage", color: TEAL },
                { label: "Total Cost", value: `$${(stats?.totalAiCost ?? 0).toFixed(2)}`, sub: "recorded usage", color: AMBER },
                { label: "Avg Response", value: "—", sub: "not collected yet", color: "#34d399" },
                { label: "Error Rate", value: (stats?.totalAiCalls ?? 0) ? `${(((stats?.aiErrorCount ?? 0) / stats.totalAiCalls) * 100).toFixed(2)}%` : "0%", sub: "recorded usage", color: "#f87171" },
              ].map(k => (
                <div key={k.label} style={{ borderRadius: 12, padding: "16px 18px", background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
                  <p style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 6px" }}>{k.label.toUpperCase()}</p>
                  <p style={{ fontSize: 26, fontFamily: "Outfit, sans-serif", fontWeight: 800, color: k.color, margin: "0 0 4px" }}>{k.value}</p>
                  <p style={{ fontSize: 11, color: "oklch(0.42 0.03 250)", margin: 0 }}>{k.sub}</p>
                </div>
              ))}
            </div>
            <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", marginBottom: 16 }}>
              <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 16px" }}>API Calls by Service — This Week</p>
              <ResponsiveContainer width="100%" height={200}>
                <BarChart id="bar-ai-services" data={dAiDaily}>
                  <CartesianGrid key="cg" stroke="oklch(0.17 0.03 250)" strokeDasharray="3 3" />
                  <XAxis key="x" dataKey="day" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                  <YAxis key="y" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                  <Tooltip key="tt" contentStyle={ttStyle} />
                  <Bar key="gemini" dataKey="gemini" fill={TEAL} radius={[2, 2, 0, 0]} />
                  <Bar key="tts" dataKey="tts" fill="#a78bfa" radius={[2, 2, 0, 0]} />
                  <Bar key="stt" dataKey="stt" fill={AMBER} radius={[2, 2, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
              <div style={{ display: "flex", gap: 16, marginTop: 10 }}>
                {[["Gemini", TEAL], ["GCP TTS", "#a78bfa"], ["GCP STT", AMBER]].map(([label, color]) => (
                  <div key={label} style={{ display: "flex", alignItems: "center", gap: 5 }}>
                    <div style={{ width: 8, height: 8, borderRadius: 2, background: color }} />
                    <span style={{ fontSize: 11, color: "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>{label}</span>
                  </div>
                ))}
              </div>
            </div>
            <div style={{ borderRadius: 14, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", overflow: "hidden" }}>
              <div style={{ padding: "14px 16px", borderBottom: "1px solid oklch(0.18 0.03 250)" }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: 0 }}>Service Breakdown</p>
              </div>
              <table style={{ width: "100%", borderCollapse: "collapse" }}>
                <thead>
                  <tr style={{ borderBottom: "1px solid oklch(0.18 0.03 250)" }}>
                    {["Service", "Calls (7d)", "Cost (7d)", "Avg Response", "Error Rate"].map(h => (
                      <th key={h} style={{ padding: "10px 16px", textAlign: "left", fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, fontWeight: 600 }}>{h.toUpperCase()}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {[{ service: "Gemini", calls: stats?.totalAiCalls ?? 0, cost: stats?.totalAiCost ?? 0, avgMs: 0, errors: stats?.aiErrorCount ?? 0 }].map(svc => (
                    <tr key={svc.service} style={{ borderBottom: "1px solid oklch(0.15 0.025 250)" }}>
                      <td style={{ padding: "12px 16px", fontSize: 13, color: "oklch(0.88 0.01 250)", fontWeight: 500 }}>{svc.service}</td>
                      <td style={{ padding: "12px 16px", fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: TEAL }}>{svc.calls.toLocaleString()}</td>
                      <td style={{ padding: "12px 16px", fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: AMBER }}>${svc.cost.toFixed(2)}</td>
                      <td style={{ padding: "12px 16px", fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: "oklch(0.55 0.03 250)" }}>{svc.avgMs}ms</td>
                      <td style={{ padding: "12px 16px" }}>
                        <span style={{ fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: svc.errors > 10 ? "#f87171" : "#34d399" }}>{((svc.errors / svc.calls) * 100).toFixed(2)}%</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Admin Login ────────────────────────────────────────────────────────────────
function AdminLoginScreen({ onLogin }: { onLogin: () => void }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const res = await fetch((import.meta.env.VITE_API_URL || 'http://192.168.0.73:8080') + "/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password })
      });
      const data = await res.json();
      if (res.ok && data.success) {
        // Check if user has ADMIN role
        const userRoles = data.data.user?.roles || [];
        if (!userRoles.includes("ROLE_ADMIN")) {
          setError("Access denied. Admin privileges required.");
          return;
        }
        localStorage.setItem("kapor_admin_token", data.data.accessToken);
        if (data.data.refreshToken) {
          localStorage.setItem("kapor_admin_refresh_token", data.data.refreshToken);
        }
        onLogin();
      } else {
        setError(data.message || "Invalid credentials");
      }
    } catch (err) {
      setError("Failed to connect to server");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ height: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "#0a0a0f", color: "white" }}>
      <div style={{ background: "oklch(0.14 0.025 250)", border: "1px solid oklch(0.22 0.03 250)", padding: 40, borderRadius: 16, width: 360 }}>
        <div style={{ textAlign: "center", marginBottom: 30 }}>
          <div style={{ width: 60, height: 60, borderRadius: 16, background: `${TEAL}18`, border: `2px solid ${TEAL}44`, display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 12px", fontSize: 28 }}>⚙️</div>
          <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: TEAL, margin: "0 0 4px" }}>KAPOR ADMIN</h1>
          <p style={{ fontSize: 12, color: "oklch(0.50 0.03 250)", margin: 0 }}>System Management Dashboard</p>
        </div>
        <form onSubmit={handleLogin} style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          <input 
            type="email" 
            placeholder="Admin Email" 
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            style={{ padding: "12px 16px", borderRadius: 8, border: "1px solid oklch(0.22 0.03 250)", background: "oklch(0.12 0.02 250)", color: "white", outline: "none", fontFamily: "JetBrains Mono, monospace", fontSize: 13 }}
            required
          />
          <input 
            type="password" 
            placeholder="Password" 
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            style={{ padding: "12px 16px", borderRadius: 8, border: "1px solid oklch(0.22 0.03 250)", background: "oklch(0.12 0.02 250)", color: "white", outline: "none", fontFamily: "JetBrains Mono, monospace", fontSize: 13 }}
            required
          />
          {error && <p style={{ color: "#f87171", fontSize: 12, margin: 0 }}>{error}</p>}
          <button 
            type="submit" 
            disabled={loading}
            style={{ padding: "14px", borderRadius: 8, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 15, cursor: loading ? "not-allowed" : "pointer", opacity: loading ? 0.7 : 1, marginTop: 8 }}
          >
            {loading ? "Authenticating..." : "Login"}
          </button>
        </form>
      </div>
    </div>
  );
}

// ─── Root ────────────────────────────────────────────────────────────────────────

export default function App() {
  const [lang] = useState<Lang>("vi");
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(!!localStorage.getItem("kapor_admin_token"));

  return (
    <div style={{ position: "relative" }}>
      <style>{`@keyframes kpulse { 0%,100%{opacity:0.35} 50%{opacity:1} }`}</style>
      {isAuthenticated ? (
        <AdminPanel lang={lang} />
      ) : (
        <AdminLoginScreen onLogin={() => setIsAuthenticated(true)} />
      )}
    </div>
  );
}
