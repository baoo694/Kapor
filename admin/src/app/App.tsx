import React, { useState, useRef, useEffect } from "react";
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

const adminUsers = [
  { id: "u1", name: "Nguyễn Văn A", email: "nguyen.a@gmail.com", level: "Beginner", streak: 15, joined: "2024-01", role: "student", avatar: "👨‍💻" },
  { id: "u2", name: "Trần Thị B", email: "tran.b@outlook.com", level: "Intermediate", streak: 42, joined: "2023-09", role: "student", avatar: "👩‍💻" },
  { id: "u3", name: "Lê Văn C", email: "le.c@company.vn", level: "Advanced", streak: 120, joined: "2023-05", role: "premium", avatar: "🧑‍💼" },
  { id: "u4", name: "Phạm Admin", email: "admin@kapor.app", level: "—", streak: 0, joined: "2022-12", role: "admin", avatar: "⚙️" },
];

const adminVideos = [
  { id: "v1", title: "DEVIEW 2025: Serverless Architecture", domain: "backend", difficulty: "advanced", subtitles: "완료", quizzes: 3 },
  { id: "v2", title: "AWS Korea: Kubernetes on EKS", domain: "devops", difficulty: "advanced", subtitles: "진행중", quizzes: 1 },
  { id: "v3", title: "Kakao FE: React Performance 최적화", domain: "frontend", difficulty: "intermediate", subtitles: "없음", quizzes: 0 },
];

const subtitleRowsInit = [
  { id: 1, start: "0:03", end: "0:07", ko: "서버리스 아키텍처에 대해 설명하겠습니다", vi: "Tôi sẽ giải thích về kiến trúc Serverless" },
  { id: 2, start: "0:08", end: "0:13", ko: "배포가 자동으로 처리됩니다", vi: "Việc triển khai được xử lý tự động" },
  { id: 3, start: "0:14", end: "0:19", ko: "오류 처리는 중요한 부분입니다", vi: "Xử lý lỗi là phần quan trọng" },
];

const userGrowthData = [
  { month: "Jan", users: 120 }, { month: "Feb", users: 180 }, { month: "Mar", users: 240 },
  { month: "Apr", users: 350 }, { month: "May", users: 520 }, { month: "Jun", users: 780 },
  { month: "Jul", users: 890 }, { month: "Aug", users: 1050 }, { month: "Sep", users: 1100 },
  { month: "Oct", users: 1180 }, { month: "Nov", users: 1210 }, { month: "Dec", users: 1234 },
];

const lessonCompletionData = [
  { domain: "Frontend", completions: 450 }, { domain: "Backend", completions: 380 },
  { domain: "DevOps", completions: 290 }, { domain: "Agile", completions: 210 },
];

const aiUsageData = [
  { month: "Oct", cost: 48 }, { month: "Nov", cost: 72 }, { month: "Dec", cost: 95 },
  { month: "Jan", cost: 120 }, { month: "Feb", cost: 145 }, { month: "Mar", cost: 168 },
];

const dauData = [
  { day: "Mon", dau: 89 }, { day: "Tue", dau: 112 }, { day: "Wed", dau: 145 },
  { day: "Thu", dau: 132 }, { day: "Fri", dau: 156 }, { day: "Sat", dau: 98 }, { day: "Sun", dau: 76 },
];

const adminTopics = [
  { id: "t1", title: "HTML & DOM 용어", titleVi: "Thuật ngữ HTML & DOM", domain: "frontend", difficulty: "beginner", lessonCount: 5, order: 1 },
  { id: "t2", title: "CSS Grid & Flexbox 용어", titleVi: "Thuật ngữ CSS Grid & Flexbox", domain: "frontend", difficulty: "intermediate", lessonCount: 5, order: 2 },
  { id: "t3", title: "배포 & CI/CD 용어", titleVi: "Thuật ngữ Deployment & CI/CD", domain: "devops", difficulty: "intermediate", lessonCount: 6, order: 3 },
  { id: "t4", title: "REST API 설계 용어", titleVi: "Thuật ngữ thiết kế REST API", domain: "backend", difficulty: "intermediate", lessonCount: 4, order: 4 },
  { id: "t5", title: "Docker & 컨테이너", titleVi: "Docker & Container", domain: "devops", difficulty: "advanced", lessonCount: 5, order: 5 },
];

const adminLessons = [
  { id: "l1", title: "Flexbox 방향 속성", topic: "CSS Grid & Flexbox", domain: "frontend", order: 1, vocabCount: 8, exerciseCount: 3, estMins: 15 },
  { id: "l2", title: "Grid 레이아웃 기초", topic: "CSS Grid & Flexbox", domain: "frontend", order: 2, vocabCount: 10, exerciseCount: 4, estMins: 20 },
  { id: "l3", title: "CI/CD 파이프라인 용어", topic: "배포 & CI/CD", domain: "devops", order: 1, vocabCount: 12, exerciseCount: 5, estMins: 25 },
  { id: "l4", title: "Docker 기본 명령어", topic: "Docker & 컨테이너", domain: "devops", order: 1, vocabCount: 9, exerciseCount: 3, estMins: 18 },
];

const adminScenarios = [
  { id: "s1", title: "긴급 장애 보고", titleVi: "Báo cáo sự cố nghiêm trọng", persona: "김민수", role: "Tech Lead", company: "Naver", domain: "backend", difficulty: "intermediate", active: true, requiredVocab: 6 },
  { id: "s2", title: "코드 리뷰 요청", titleVi: "Yêu cầu Code Review", persona: "이지영", role: "Senior Dev", company: "Kakao", domain: "frontend", difficulty: "beginner", active: true, requiredVocab: 4 },
  { id: "s3", title: "스프린트 계획 미팅", titleVi: "Sprint Planning Meeting", persona: "박준호", role: "Scrum Master", company: "Coupang", domain: "agile", difficulty: "advanced", active: false, requiredVocab: 8 },
];

const adminDictionary = [
  { id: "d1", korean: "배포", pronunciation: "baepo", vietnamese: "Triển khai", domain: "devops", frequency: "high" },
  { id: "d2", korean: "오류", pronunciation: "oryu", vietnamese: "Lỗi", domain: "backend", frequency: "high" },
  { id: "d3", korean: "서버", pronunciation: "seobeo", vietnamese: "Máy chủ", domain: "backend", frequency: "high" },
  { id: "d4", korean: "프레임워크", pronunciation: "peureim-wokeu", vietnamese: "Framework", domain: "frontend", frequency: "medium" },
  { id: "d5", korean: "컨테이너", pronunciation: "keonteineo", vietnamese: "Container", domain: "devops", frequency: "medium" },
  { id: "d6", korean: "마이크로서비스", pronunciation: "maikeuro-seobi-seu", vietnamese: "Microservices", domain: "backend", frequency: "medium" },
];

const adminPronExercises = [
  { id: "p1", title: "서버 배포 관련 문장", titleVi: "Câu liên quan đến triển khai server", domain: "devops", difficulty: "intermediate", sentenceCount: 3, hasAudio: true },
  { id: "p2", title: "코드 리뷰 표현", titleVi: "Diễn đạt code review", domain: "frontend", difficulty: "beginner", sentenceCount: 5, hasAudio: true },
  { id: "p3", title: "긴급 장애 보고 문장", titleVi: "Câu báo cáo sự cố khẩn cấp", domain: "backend", difficulty: "advanced", sentenceCount: 4, hasAudio: false },
];

const aiPromptTemplates = [
  { id: "roleplay-system", name: "Roleplay System Prompt", desc: "Defines AI persona behavior in TechTalk sessions", content: "You are {{personaName}}, a {{personaRole}} at {{company}} in South Korea.\nYou only speak Korean. You are in a workplace meeting scenario.\n\nCurrent mission: {{missionTitle}}\nDifficulty: {{difficulty}}\n\nRules:\n1. Stay in character at all times\n2. Use {{politenessLevel}} speech level\n3. Use IT/tech vocabulary relevant to the scenario\n4. Keep responses concise (2-4 sentences)" },
  { id: "honorifics-analyze", name: "Honorifics Analyzer", desc: "Analyzes Korean text politeness level and suggests corrections", content: "You are a Korean business language expert. Analyze the following text for:\n1. Current politeness level (banmal/heyohaet-che/hasipsio-che)\n2. Grammar errors specific to formal Korean writing\n3. Inappropriate casual vocabulary\n4. Wrong particle usage\n\nReturn JSON with corrections array." },
  { id: "summarizer", name: "SmartSummarizer", desc: "Extracts IT vocabulary from Korean tech articles", content: "You are an expert in Korean IT vocabulary extraction.\nGiven a Korean technical article, extract the most important IT terms.\nFor each term: korean, vietnamese, pronunciation, IT context, example sentence.\nReturn 5-10 flashcard objects as JSON." },
  { id: "recommendation", name: "Smart Recommendation", desc: "Generates personalized learning recommendations", content: "You are a Korean language learning coach specializing in IT communication.\nBased on the user's progress data, recommend the most impactful next activity.\nConsider: recent mistakes, due cards, lesson completion, streak status.\nReturn a JSON recommendation object." },
  { id: "scaffold-hint", name: "Scaffold Hint Generator", desc: "Provides contextual hints during TechTalk sessions", content: "You are a Korean language coach assisting a learner during a roleplay session.\nBased on the conversation context, provide a helpful hint without giving away the answer.\nInclude: key vocabulary words, a sentence template, and a politeness tip." },
];

const adminAdmins = [
  { id: "a1", email: "admin@kapor.app", name: "Phạm Admin", role: "super_admin", lastLogin: "2026-07-10", avatar: "⚙️" },
  { id: "a2", email: "content@kapor.app", name: "Nguyễn Content", role: "content_admin", lastLogin: "2026-07-09", avatar: "📝" },
  { id: "a3", email: "analyst@kapor.app", name: "Trần Analytics", role: "analytics_viewer", lastLogin: "2026-07-08", avatar: "📊" },
];

const popularContent = [
  { title: "CSS Grid & Flexbox 용어", type: "Lesson", completions: 342, domain: "frontend" },
  { title: "배포 & CI/CD 용어", type: "Lesson", completions: 298, domain: "devops" },
  { title: "DEVIEW 2025: Serverless", type: "Video", completions: 254, domain: "backend" },
  { title: "긴급 장애 보고", type: "Scenario", completions: 187, domain: "backend" },
  { title: "서버 배포 발음 연습", type: "Pronunciation", completions: 143, domain: "devops" },
];

const userActivityLog = [
  { date: "2026-07-10", type: "Lesson", action: "Flexbox 방향 속성 완료", duration: "15m", score: 92 },
  { date: "2026-07-10", type: "MemByte", action: "25 cards reviewed", duration: "12m", score: 88 },
  { date: "2026-07-09", type: "TechTalk", action: "긴급 장애 보고 (김민수)", duration: "20m", score: 76 },
  { date: "2026-07-09", type: "Pronunciation", action: "서버 배포 관련 문장", duration: "8m", score: 82 },
  { date: "2026-07-08", type: "Video", action: "DEVIEW 2025: Serverless", duration: "32m", score: null },
  { date: "2026-07-07", type: "Lesson", action: "CI/CD 파이프라인 용어 완료", duration: "25m", score: 95 },
];

const contentPerformanceData = [
  { title: "CSS Grid & Flexbox 용어", type: "Lesson", completions: 342, rate: 78, domain: "frontend" },
  { title: "배포 & CI/CD 용어", type: "Lesson", completions: 298, rate: 65, domain: "devops" },
  { title: "DEVIEW 2025: Serverless", type: "Video", completions: 254, rate: 71, domain: "backend" },
  { title: "긴급 장애 보고", type: "Scenario", completions: 187, rate: 82, domain: "backend" },
  { title: "서버 배포 발음 연습", type: "Pronunciation", completions: 143, rate: 90, domain: "devops" },
  { title: "REST API 설계 용어", type: "Lesson", completions: 128, rate: 60, domain: "backend" },
  { title: "AWS Korea: Kubernetes", type: "Video", completions: 112, rate: 55, domain: "devops" },
  { title: "코드 리뷰 요청", type: "Scenario", completions: 98, rate: 76, domain: "frontend" },
];

const dictSearchRanking = [
  { word: "배포", count: 234 }, { word: "오류", count: 198 }, { word: "서버", count: 176 },
  { word: "컨테이너", count: 143 }, { word: "프레임워크", count: 128 }, { word: "마이크로서비스", count: 112 },
];

const aiServiceBreakdown = [
  { service: "Gemini 2.5-pro", calls: 4820, cost: 96.4, avgMs: 1240, errors: 12 },
  { service: "GCP TTS", calls: 8930, cost: 44.65, avgMs: 340, errors: 3 },
  { service: "GCP STT", calls: 2340, cost: 27.3, avgMs: 890, errors: 8 },
];

const aiDailyData = [
  { day: "Mon", gemini: 680, tts: 1240, stt: 320 },
  { day: "Tue", gemini: 820, tts: 1480, stt: 410 },
  { day: "Wed", gemini: 1050, tts: 1720, stt: 580 },
  { day: "Thu", gemini: 940, tts: 1380, stt: 490 },
  { day: "Fri", gemini: 1120, tts: 1850, stt: 620 },
  { day: "Sat", gemini: 560, tts: 980, stt: 280 },
  { day: "Sun", gemini: 430, tts: 780, stt: 220 },
];

const retentionData = [
  { week: "W1", d1: 100, d7: 62, d30: 38 },
  { week: "W2", d1: 100, d7: 58, d30: 35 },
  { week: "W3", d1: 100, d7: 65, d30: 42 },
  { week: "W4", d1: 100, d7: 70, d30: 45 },
];

const newRegData = [
  { day: "Mon", count: 18 }, { day: "Tue", count: 24 }, { day: "Wed", count: 31 },
  { day: "Thu", count: 28 }, { day: "Fri", count: 35 }, { day: "Sat", count: 14 }, { day: "Sun", count: 10 },
];

const dictImportPreview = [
  { korean: "빌드", pronunciation: "bild-eu", vietnamese: "Build / Biên dịch", domain: "devops" },
  { korean: "테스트", pronunciation: "te-seu-teu", vietnamese: "Kiểm thử", domain: "backend" },
  { korean: "스타일", pronunciation: "seu-ta-il", vietnamese: "Kiểu dáng / Style", domain: "frontend" },
  { korean: "함수", pronunciation: "ham-su", vietnamese: "Hàm số", domain: "backend" },
  { korean: "변수", pronunciation: "byeon-su", vietnamese: "Biến", domain: "backend" },
];

const lessonExercises = [
  { id: "e1", type: "multiple_choice", question: "다음 중 'flex-direction: row'의 의미는?", options: ["세로 방향", "가로 방향", "대각선 방향", "고정 위치"], correct: 1 },
  { id: "e2", type: "fill_blank", question: "아이템을 세로로 정렬하려면 flex-direction: ___을 사용합니다.", answer: "column" },
  { id: "e3", type: "matching", pairs: [["flex-start", "왼쪽/위 정렬"], ["flex-end", "오른쪽/아래 정렬"], ["center", "가운데 정렬"]] as [string, string][] },
];

const recentActivity = [
  { user: "Nguyễn Văn A", action: "Completed lesson", target: "CSS Grid & Flexbox 용어", time: "2m ago", avatar: "👨‍💻" },
  { user: "Trần Thị B", action: "Reviewed 20 cards", target: "Frontend Deployment deck", time: "5m ago", avatar: "👩‍💻" },
  { user: "Lê Văn C", action: "TechTalk session", target: "긴급 장애 보고", time: "12m ago", avatar: "🧑‍💼" },
  { user: "Phạm Thị D", action: "Registered", target: "New user", time: "18m ago", avatar: "🆕" },
  { user: "Vũ Văn E", action: "Pronunciation attempt", target: "서버 배포 관련 문장", time: "25m ago", avatar: "🎤" },
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
  const [level, setLevel] = useState("");
  const [dailyGoal, setDailyGoal] = useState("10");

  const steps = ["Mục tiêu", "Trình độ", "Kế hoạch"];
  const goalOptions = ["사무적 한국어 (Giao tiếp văn phòng)", "IT 전문 용어 (Thuật ngữ IT)", "인터뷰 준비 (Phỏng vấn)", "직장 생활 (Sinh hoạt công sở)"];
  const levelOptions = [
    { value: "beginner", label: "Sơ cấp", sub: "Mới bắt đầu học tiếng Hàn" },
    { value: "intermediate", label: "Trung cấp", sub: "Biết một số từ vựng & ngữ pháp cơ bản" },
    { value: "advanced", label: "Cao cấp", sub: "Có thể hội thoại tiếng Hàn cơ bản" },
  ];
  const dailyOptions = ["5", "10", "15", "30"];
  const canNext = step === 0 ? goals.length > 0 : step === 1 ? !!level : !!dailyGoal;
  const handleNext = () => step < 2 ? setStep(s => s + 1) : nav("dashboard");

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
            <h2 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 20, color: "oklch(0.92 0.01 250)", margin: "0 0 4px" }}>Trình độ hiện tại?</h2>
            <p style={{ fontSize: 12, color: "oklch(0.50 0.03 250)", margin: "0 0 16px" }}>Chọn trình độ tiếng Hàn của bạn</p>
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {levelOptions.map(opt => {
                const sel = level === opt.value;
                return (
                  <button key={opt.value} onClick={() => setLevel(opt.value)} style={{ padding: "13px 16px", borderRadius: 12, border: `1px solid ${sel ? TEAL : "oklch(0.25 0.03 250)"}`, background: sel ? `${TEAL}15` : "oklch(0.14 0.025 250)", textAlign: "left", cursor: "pointer" }}>
                    <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: sel ? TEAL : "oklch(0.82 0.01 250)", margin: "0 0 2px" }}>{opt.label}</p>
                    <p style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", margin: 0 }}>{opt.sub}</p>
                  </button>
                );
              })}
            </div>
          </>
        )}
        {step === 2 && (
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
  const [showSubEditor, setShowSubEditor] = useState(false);
  const [showAddVideo, setShowAddVideo] = useState(false);
  const [editSubs, setEditSubs] = useState(subtitleRowsInit.map(r => ({ ...r })));
  const [contentOpen, setContentOpen] = useState(true);
  const [analyticsOpen, setAnalyticsOpen] = useState(true);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [showAddTopic, setShowAddTopic] = useState(false);
  const [showAddLesson, setShowAddLesson] = useState(false);
  const [showAddScenario, setShowAddScenario] = useState(false);
  const [showAddDict, setShowAddDict] = useState(false);
  const [showAddPron, setShowAddPron] = useState(false);
  const [testScenario, setTestScenario] = useState<typeof adminScenarios[0] | null>(null);
  const [testMessages, setTestMessages] = useState<{ role: "ai" | "user"; text: string }[]>([]);
  const [testInput, setTestInput] = useState("");
  const [selectedPrompt, setSelectedPrompt] = useState(aiPromptTemplates[0]);
  const [promptContent, setPromptContent] = useState(aiPromptTemplates[0].content);
  const [dictSearch, setDictSearch] = useState("");
  const [inviteEmail, setInviteEmail] = useState("");
  const [userSearch, setUserSearch] = useState("");
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [userRoleEdits, setUserRoleEdits] = useState<Record<string, string>>({});
  const [deactivatedUsers, setDeactivatedUsers] = useState<Set<string>>(new Set());
  const [videoFilterDomain, setVideoFilterDomain] = useState("all");
  const [videoFilterDiff, setVideoFilterDiff] = useState("all");
  const [videoFilterSubs, setVideoFilterSubs] = useState("all");
  const [videoBulkSelected, setVideoBulkSelected] = useState<Set<string>>(new Set());
  const [analyticsTab, setAnalyticsTab] = useState<"overview" | "users" | "content" | "ai">("overview");
  const [lessonEditorTab, setLessonEditorTab] = useState<"vocab" | "exercises" | "preview">("vocab");
  const [editingLessonId, setEditingLessonId] = useState<string | null>(null);
  const [dictImportStep, setDictImportStep] = useState<"upload" | "preview">("upload");

  const navTo = (s: AdminSection) => {
    setSection(s); setShowSubEditor(false); setShowAddVideo(false); setShowAddTopic(false);
    setShowAddLesson(false); setShowAddScenario(false); setShowAddDict(false); setShowAddPron(false);
    setTestScenario(null); setSelectedUserId(null); setVideoBulkSelected(new Set());
    setDictImportStep("upload"); setEditingLessonId(null); setLessonEditorTab("vocab");
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
    { label: "Total Users", value: "1,234", Icon: Users, color: TEAL, sub: "+12% this month" },
    { label: "DAU", value: "156", Icon: TrendingUp, color: "#34d399", sub: "+8% vs last week" },
    { label: "MAU", value: "890", Icon: BarChart2, color: "#a78bfa", sub: "72% retention" },
    { label: "Content", value: "523", Icon: BookOpen, color: AMBER, sub: "48 added this week" },
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
                  <LineChart id="line-user-growth" data={userGrowthData}>
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
                  <BarChart id="bar-dau-week" data={dauData}>
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
                {popularContent.map((item, i) => (
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
                {recentActivity.map((act, i) => (
                  <div key={i} style={{ display: "flex", alignItems: "center", gap: 10, padding: "8px 0", borderBottom: i < 4 ? "1px solid oklch(0.17 0.03 250)" : "none" }}>
                    <div style={{ width: 28, height: 28, borderRadius: "50%", background: `${TEAL}12`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 14, flexShrink: 0 }}>{act.avatar}</div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <p style={{ fontSize: 12, color: "oklch(0.82 0.01 250)", margin: 0 }}><span style={{ color: TEAL }}>{act.user}</span> · {act.action}</p>
                      <p style={{ fontSize: 10, color: "oklch(0.45 0.03 250)", margin: 0, fontFamily: "JetBrains Mono, monospace", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{act.target}</p>
                    </div>
                    <span style={{ fontSize: 10, color: "oklch(0.38 0.03 250)", fontFamily: "JetBrains Mono, monospace", flexShrink: 0 }}>{act.time}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {section === "users" && (
          <div>
            <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: "0 0 20px" }}>Users</h1>
            <div style={{ display: "flex", gap: 10, marginBottom: 16 }}>
              <input
                value={userSearch} onChange={e => setUserSearch(e.target.value)}
                placeholder="Search by name or email…"
                style={{ flex: 1, padding: "10px 14px", borderRadius: 10, background: "oklch(0.12 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.85 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none" }}
              />
              <span style={{ padding: "10px 14px", borderRadius: 10, background: "oklch(0.12 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.45 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 12, display: "flex", alignItems: "center" }}>
                {adminUsers.filter(u => u.name.toLowerCase().includes(userSearch.toLowerCase()) || u.email.toLowerCase().includes(userSearch.toLowerCase())).length} results
              </span>
            </div>
            <div style={{ borderRadius: 14, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", overflow: "hidden" }}>
              <table style={{ width: "100%", borderCollapse: "collapse" }}>
                <thead>
                  <tr style={{ borderBottom: "1px solid oklch(0.18 0.03 250)" }}>
                    {["User", "Email", "Level", "Streak", "Joined", "Role", "Actions"].map(h => (
                      <th key={h} style={{ padding: "12px 16px", textAlign: "left", fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, fontWeight: 600 }}>{h.toUpperCase()}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {adminUsers
                    .filter(u => u.name.toLowerCase().includes(userSearch.toLowerCase()) || u.email.toLowerCase().includes(userSearch.toLowerCase()))
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
                          <td style={{ padding: "12px 16px", fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: TEAL }}>{u.level}</td>
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
                                <button onClick={() => setUserRoleEdits(prev => { const n = { ...prev }; delete n[u.id]; return n; })} style={{ padding: "4px 10px", borderRadius: 6, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Save</button>
                              )}
                              <button onClick={() => setDeactivatedUsers(prev => { const n = new Set(prev); isDeactivated ? n.delete(u.id) : n.add(u.id); return n; })} style={{ padding: "4px 10px", borderRadius: 6, border: `1px solid ${isDeactivated ? "#34d39944" : "#f8717144"}`, background: isDeactivated ? "#34d39910" : "#f8717110", color: isDeactivated ? "#34d399" : "#f87171", fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>
                                {isDeactivated ? "Activate" : "Deactivate"}
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
          </div>
        )}

        {section === "users-detail" && (() => {
          const u = adminUsers.find(u => u.id === selectedUserId) ?? adminUsers[0];
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
                    <KBadge color={TEAL}>{u.level}</KBadge>
                    <KBadge color={currentRole === "admin" ? AMBER : currentRole === "premium" ? TEAL : "oklch(0.50 0.03 250)"}>{currentRole}</KBadge>
                    <span style={{ display: "flex", alignItems: "center", gap: 3, fontSize: 12, color: AMBER, fontFamily: "JetBrains Mono, monospace" }}><Flame size={12} color={AMBER} />{u.streak} day streak</span>
                  </div>
                </div>
                <div style={{ display: "flex", gap: 10 }}>
                  <div style={{ textAlign: "center" }}>
                    <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 22, color: TEAL, margin: "0 0 2px" }}>1,250</p>
                    <p style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>MINUTES</p>
                  </div>
                  <div style={{ width: 1, background: "oklch(0.20 0.03 250)" }} />
                  <div style={{ textAlign: "center" }}>
                    <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 22, color: "#a78bfa", margin: "0 0 2px" }}>3,400</p>
                    <p style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>CARDS</p>
                  </div>
                  <div style={{ width: 1, background: "oklch(0.20 0.03 250)" }} />
                  <div style={{ textAlign: "center" }}>
                    <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 22, color: "#34d399", margin: "0 0 2px" }}>88%</p>
                    <p style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>RETENTION</p>
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
                  <button style={{ width: "100%", padding: "9px", borderRadius: 8, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer", marginBottom: 8 }}>Save Role</button>
                  <button onClick={() => setDeactivatedUsers(prev => { const n = new Set(prev); deactivatedUsers.has(u.id) ? n.delete(u.id) : n.add(u.id); return n; })} style={{ width: "100%", padding: "9px", borderRadius: 8, border: "1px solid #f8717144", background: "#f8717110", color: "#f87171", fontFamily: "Outfit, sans-serif", fontSize: 13, cursor: "pointer" }}>
                    {deactivatedUsers.has(u.id) ? "Reactivate" : "Deactivate User"}
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
              <button onClick={() => setShowAddTopic(v => !v)} style={{ padding: "8px 16px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer", display: "flex", alignItems: "center", gap: 6 }}>
                <Plus size={15} /> New Topic
              </button>
            </div>
            {showAddTopic && (
              <AddForm title="NEW TOPIC" fields={[
                { label: "Title (Korean)", placeholder: "CSS Grid & Flexbox 용어" },
                { label: "Title (Vietnamese)", placeholder: "Thuật ngữ CSS Grid & Flexbox" },
                { label: "Domain", placeholder: "frontend", type: "select" },
                { label: "Difficulty", placeholder: "intermediate" },
              ]} onSave={() => setShowAddTopic(false)} onCancel={() => setShowAddTopic(false)} />
            )}
            <AdminTable headers={["#", "Title KR", "Domain", "Difficulty", "Lessons", ""]}>
              {adminTopics.map(t => (
                <TR key={t.id}>
                  <AdminTd mono>{t.order}</AdminTd>
                  <td style={{ padding: "12px 16px" }}>
                    <p style={{ fontSize: 13, color: "oklch(0.88 0.01 250)", margin: "0 0 2px" }}>{t.title}</p>
                    <p style={{ fontSize: 11, color: "oklch(0.45 0.03 250)", margin: 0, fontFamily: "JetBrains Mono, monospace" }}>{t.titleVi}</p>
                  </td>
                  <td style={{ padding: "12px 16px" }}><KBadge color={domainColor(t.domain)}>{t.domain}</KBadge></td>
                  <td style={{ padding: "12px 16px" }}><KBadge color={diffColor(t.difficulty)}>{t.difficulty}</KBadge></td>
                  <AdminTd mono>{t.lessonCount}</AdminTd>
                  <td style={{ padding: "12px 16px", display: "flex", gap: 6 }}>
                    <button style={{ padding: "5px 10px", borderRadius: 7, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Edit</button>
                    <button style={{ padding: "5px 10px", borderRadius: 7, border: "1px solid #f8717144", background: "#f8717110", color: "#f87171", fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Delete</button>
                  </td>
                </TR>
              ))}
            </AdminTable>
          </div>
        )}

        {section === "content-lessons" && (
          <div>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 24 }}>
              <h1 style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 24, color: "oklch(0.92 0.01 250)", margin: 0 }}>Lessons</h1>
              <button onClick={() => setShowAddLesson(v => !v)} style={{ padding: "8px 16px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer", display: "flex", alignItems: "center", gap: 6 }}>
                <Plus size={15} /> New Lesson
              </button>
            </div>
            {showAddLesson && (
              <AddForm title="NEW LESSON" fields={[
                { label: "Title (Korean)", placeholder: "Flexbox 방향 속성" },
                { label: "Topic", placeholder: "CSS Grid & Flexbox" },
                { label: "Domain", placeholder: "frontend", type: "select" },
                { label: "Estimated minutes", placeholder: "15" },
              ]} onSave={() => setShowAddLesson(false)} onCancel={() => setShowAddLesson(false)} />
            )}
            <AdminTable headers={["Title", "Topic", "Domain", "Vocab", "Exercises", "Est.", ""]}>
              {adminLessons.map(l => (
                <TR key={l.id}>
                  <AdminTd>{l.title}</AdminTd>
                  <AdminTd mono>{l.topic}</AdminTd>
                  <td style={{ padding: "12px 16px" }}><KBadge color={domainColor(l.domain)}>{l.domain}</KBadge></td>
                  <AdminTd mono>{l.vocabCount} terms</AdminTd>
                  <AdminTd mono>{l.exerciseCount}</AdminTd>
                  <AdminTd mono>{l.estMins}m</AdminTd>
                  <td style={{ padding: "12px 16px" }}>
                    <button onClick={() => setEditingLessonId(editingLessonId === l.id ? null : l.id)} style={{ padding: "5px 10px", borderRadius: 7, border: `1px solid ${editingLessonId === l.id ? TEAL : `${TEAL}44`}`, background: editingLessonId === l.id ? TEAL : `${TEAL}10`, color: editingLessonId === l.id ? "#000" : TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>
                      {editingLessonId === l.id ? "Close" : "Edit"}
                    </button>
                  </td>
                </TR>
              ))}
            </AdminTable>
            {editingLessonId && (
              <div style={{ marginTop: 20, borderRadius: 14, background: "oklch(0.13 0.025 250)", border: `1px solid ${TEAL}30`, overflow: "hidden" }}>
                <div style={{ padding: "14px 20px", borderBottom: "1px solid oklch(0.18 0.03 250)", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                  <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: 0 }}>
                    Lesson Editor — {adminLessons.find(l => l.id === editingLessonId)?.title}
                  </p>
                  <button onClick={() => setEditingLessonId(null)} style={{ background: "none", border: "none", cursor: "pointer", color: "oklch(0.42 0.03 250)", display: "flex", alignItems: "center" }}><X size={16} /></button>
                </div>
                <div style={{ display: "flex", borderBottom: "1px solid oklch(0.18 0.03 250)" }}>
                  {(["vocab", "exercises", "preview"] as const).map(tab => (
                    <button key={tab} onClick={() => setLessonEditorTab(tab)} style={{ padding: "10px 20px", border: "none", background: "none", cursor: "pointer", fontFamily: "JetBrains Mono, monospace", fontSize: 11, color: lessonEditorTab === tab ? TEAL : "oklch(0.42 0.03 250)", borderBottom: `2px solid ${lessonEditorTab === tab ? TEAL : "transparent"}`, textTransform: "capitalize", letterSpacing: 0.5 }}>
                      {tab === "vocab" ? "Vocabulary" : tab === "exercises" ? "Exercises" : "Preview"}
                    </button>
                  ))}
                </div>
                <div style={{ padding: 20 }}>
                  {lessonEditorTab === "vocab" && (
                    <>
                      <div style={{ overflowX: "auto" }}>
                        <table style={{ width: "100%", borderCollapse: "collapse", minWidth: 700 }}>
                          <thead>
                            <tr style={{ borderBottom: "1px solid oklch(0.18 0.03 250)" }}>
                              {["Korean", "Vietnamese", "Pronunciation", "IT Context", "Code Snippet"].map(h => (
                                <th key={h} style={{ padding: "8px 12px", textAlign: "left", fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontWeight: 600 }}>{h.toUpperCase()}</th>
                              ))}
                            </tr>
                          </thead>
                          <tbody>
                            {lessonVocab.map((v, i) => (
                              <tr key={i} style={{ borderBottom: "1px solid oklch(0.15 0.025 250)" }}>
                                <td style={{ padding: "8px 12px" }}><span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 16, color: TEAL }}>{v.korean}</span></td>
                                <td style={{ padding: "8px 12px", fontSize: 12, color: "oklch(0.75 0.01 250)" }}>{v.vietnamese}</td>
                                <td style={{ padding: "8px 12px", fontFamily: "JetBrains Mono, monospace", fontSize: 11, color: "oklch(0.50 0.03 250)" }}>/{v.pronunciation}/</td>
                                <td style={{ padding: "8px 12px", fontSize: 11, color: "oklch(0.55 0.03 250)", maxWidth: 200 }}>{v.itContext}</td>
                                <td style={{ padding: "8px 12px" }}>
                                  <code style={{ fontFamily: "JetBrains Mono, monospace", fontSize: 10, color: TEAL, background: "oklch(0.10 0.02 250)", padding: "2px 6px", borderRadius: 4 }}>{v.code}</code>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                      <button style={{ marginTop: 12, padding: "7px 14px", borderRadius: 8, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer", display: "flex", alignItems: "center", gap: 6 }}>
                        <Plus size={12} /> Add Vocabulary Term
                      </button>
                    </>
                  )}
                  {lessonEditorTab === "exercises" && (
                    <div>
                      {lessonExercises.map((ex, i) => (
                        <div key={ex.id} style={{ borderRadius: 12, padding: 16, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.18 0.03 250)", marginBottom: 12 }}>
                          <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 10 }}>
                            <span style={{ fontFamily: "JetBrains Mono, monospace", fontSize: 10, color: "oklch(0.35 0.03 250)" }}>#{i + 1}</span>
                            <KBadge color={ex.type === "multiple_choice" ? TEAL : ex.type === "fill_blank" ? AMBER : "#a78bfa"}>
                              {ex.type === "multiple_choice" ? "Multiple Choice" : ex.type === "fill_blank" ? "Fill in Blank" : "Matching"}
                            </KBadge>
                            <button style={{ marginLeft: "auto", background: "none", border: "none", cursor: "pointer", color: "#f87171", fontSize: 11, fontFamily: "JetBrains Mono, monospace" }}>Delete</button>
                          </div>
                          {ex.type === "multiple_choice" && (
                            <>
                              <p style={{ fontSize: 13, color: "oklch(0.82 0.01 250)", margin: "0 0 10px" }}>{ex.question}</p>
                              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 6 }}>
                                {ex.options?.map((opt, oi) => (
                                  <div key={oi} style={{ padding: "7px 12px", borderRadius: 8, background: oi === ex.correct ? `${TEAL}18` : "oklch(0.13 0.02 250)", border: `1px solid ${oi === ex.correct ? TEAL : "oklch(0.22 0.03 250)"}`, fontSize: 12, color: oi === ex.correct ? TEAL : "oklch(0.65 0.01 250)", display: "flex", alignItems: "center", gap: 6 }}>
                                    {oi === ex.correct && <CheckCircle size={12} color={TEAL} />}
                                    {opt}
                                  </div>
                                ))}
                              </div>
                            </>
                          )}
                          {ex.type === "fill_blank" && (
                            <p style={{ fontSize: 13, color: "oklch(0.82 0.01 250)", margin: 0 }}>
                              {ex.question} <span style={{ fontFamily: "JetBrains Mono, monospace", color: TEAL, background: `${TEAL}18`, padding: "1px 8px", borderRadius: 4 }}>{ex.answer}</span>
                            </p>
                          )}
                          {ex.type === "matching" && (
                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 6 }}>
                              {ex.pairs?.map(([kr, vi], pi) => (
                                <React.Fragment key={pi}>
                                  <div style={{ padding: "7px 12px", borderRadius: 8, background: `${TEAL}12`, border: `1px solid ${TEAL}30`, fontSize: 12, color: TEAL, fontFamily: "JetBrains Mono, monospace" }}>{kr}</div>
                                  <div style={{ padding: "7px 12px", borderRadius: 8, background: "oklch(0.13 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", fontSize: 12, color: "oklch(0.70 0.01 250)" }}>{vi}</div>
                                </React.Fragment>
                              ))}
                            </div>
                          )}
                        </div>
                      ))}
                      <div style={{ display: "flex", gap: 8 }}>
                        {["+ Multiple Choice", "+ Fill in Blank", "+ Matching"].map(label => (
                          <button key={label} style={{ padding: "7px 12px", borderRadius: 8, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>{label}</button>
                        ))}
                      </div>
                    </div>
                  )}
                  {lessonEditorTab === "preview" && (
                    <div style={{ display: "flex", justifyContent: "center" }}>
                      <div style={{ width: 320, borderRadius: 32, background: "oklch(0.08 0.02 250)", border: "8px solid oklch(0.16 0.03 250)", overflow: "hidden", boxShadow: "0 20px 60px #00000060" }}>
                        <div style={{ background: "oklch(0.10 0.02 250)", padding: "14px 16px", borderBottom: "1px solid oklch(0.16 0.025 250)" }}>
                          <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, color: TEAL, margin: 0 }}>Flexbox 방향 속성</p>
                          <div style={{ height: 4, borderRadius: 2, background: "oklch(0.16 0.025 250)", marginTop: 8 }}>
                            <div style={{ height: "100%", width: "40%", borderRadius: 2, background: `linear-gradient(90deg, ${TEAL}, #38bdf8)` }} />
                          </div>
                        </div>
                        <div style={{ padding: 16 }}>
                          {lessonVocab.map((v, i) => (
                            <div key={i} style={{ borderRadius: 12, padding: 12, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.18 0.03 250)", marginBottom: 10 }}>
                              <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
                                <span style={{ fontFamily: "Outfit, sans-serif", fontWeight: 800, fontSize: 18, color: TEAL }}>{v.korean}</span>
                                <Volume2 size={14} color="oklch(0.40 0.03 250)" />
                              </div>
                              <p style={{ fontFamily: "JetBrains Mono, monospace", fontSize: 10, color: "oklch(0.40 0.03 250)", margin: "0 0 4px" }}>/{v.pronunciation}/</p>
                              <p style={{ fontSize: 11, color: "oklch(0.70 0.01 250)", margin: "0 0 6px" }}>{v.vietnamese}</p>
                              <code style={{ fontFamily: "JetBrains Mono, monospace", fontSize: 9, color: TEAL, background: "oklch(0.08 0.02 250)", padding: "2px 6px", borderRadius: 4 }}>{v.code}</code>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}
            {!editingLessonId && (
              <p style={{ marginTop: 14, fontSize: 12, color: "oklch(0.40 0.03 250)", fontFamily: "JetBrains Mono, monospace" }}>Click Edit on a lesson to open the rich editor.</p>
            )}
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
              <AddForm title="NEW SCENARIO" fields={[
                { label: "Title (Korean)", placeholder: "긴급 장애 보고" },
                { label: "Title (Vietnamese)", placeholder: "Báo cáo sự cố" },
                { label: "Domain", placeholder: "backend", type: "select" },
                { label: "Difficulty", placeholder: "intermediate" },
                { label: "Persona Name", placeholder: "김민수" },
                { label: "Persona Role", placeholder: "Tech Lead" },
                { label: "Company", placeholder: "Naver" },
                { label: "Speech Style", placeholder: "hasipsio" },
              ]} onSave={() => setShowAddScenario(false)} onCancel={() => setShowAddScenario(false)} />
            )}
            <AdminTable headers={["Title", "Persona", "Domain", "Difficulty", "Vocab", "Active", ""]}>
              {adminScenarios.map(s => (
                <TR key={s.id}>
                  <td style={{ padding: "12px 16px" }}>
                    <p style={{ fontSize: 13, color: "oklch(0.88 0.01 250)", margin: "0 0 2px" }}>{s.title}</p>
                    <p style={{ fontSize: 11, color: "oklch(0.45 0.03 250)", margin: 0, fontFamily: "JetBrains Mono, monospace" }}>{s.titleVi}</p>
                  </td>
                  <td style={{ padding: "12px 16px" }}>
                    <p style={{ fontSize: 12, color: "oklch(0.82 0.01 250)", margin: 0 }}>{s.persona}</p>
                    <p style={{ fontSize: 10, color: "oklch(0.45 0.03 250)", margin: 0, fontFamily: "JetBrains Mono, monospace" }}>{s.role} @ {s.company}</p>
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
                    <button onClick={() => { setTestScenario(s); setTestMessages([{ role: "ai", text: `안녕하세요. 저는 ${s.persona}입니다. 오늘 ${s.title} 연습을 시작할까요?` }]); setTestInput(""); }} style={{ padding: "5px 10px", borderRadius: 7, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Test</button>
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
                <p style={{ fontSize: 11, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>Persona: {testScenario.persona} ({testScenario.role} @ {testScenario.company})</p>
              </div>
            </div>
            <div style={{ borderRadius: 14, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", padding: 20, minHeight: 340, display: "flex", flexDirection: "column", gap: 12, marginBottom: 12 }}>
              {testMessages.map((msg, i) => (
                <div key={i} style={{ display: "flex", justifyContent: msg.role === "ai" ? "flex-start" : "flex-end" }}>
                  <div style={{ maxWidth: "72%", borderRadius: 12, padding: "10px 14px", background: msg.role === "ai" ? `${TEAL}15` : "oklch(0.20 0.03 250)", border: `1px solid ${msg.role === "ai" ? `${TEAL}30` : "oklch(0.28 0.03 250)"}` }}>
                    {msg.role === "ai" && <p style={{ fontSize: 9, color: TEAL, fontFamily: "JetBrains Mono, monospace", margin: "0 0 4px" }}>{testScenario.persona}</p>}
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
              <AddForm title="NEW DICTIONARY ENTRY" fields={[
                { label: "Korean", placeholder: "배포" },
                { label: "Pronunciation", placeholder: "baepo" },
                { label: "Vietnamese", placeholder: "Triển khai" },
                { label: "Domain", placeholder: "devops", type: "select" },
                { label: "Hanja (optional)", placeholder: "配布" },
                { label: "Frequency", placeholder: "high / medium / low" },
              ]} onSave={() => setShowAddDict(false)} onCancel={() => setShowAddDict(false)} />
            )}
            <AdminTable headers={["Korean", "Pronunciation", "Vietnamese", "Domain", "Frequency", ""]}>
              {adminDictionary.filter(d => d.korean.includes(dictSearch) || d.vietnamese.toLowerCase().includes(dictSearch.toLowerCase())).map(d => (
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
            <p style={{ fontSize: 11, color: "oklch(0.38 0.03 250)", fontFamily: "JetBrains Mono, monospace", marginTop: 12 }}>{adminDictionary.length} entries total · Vector embeddings: 768-dim (cosine similarity)</p>
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
              <AddForm title="NEW PRONUNCIATION EXERCISE" fields={[
                { label: "Title (Korean)", placeholder: "서버 배포 관련 문장" },
                { label: "Title (Vietnamese)", placeholder: "Câu liên quan đến triển khai server" },
                { label: "Domain", placeholder: "devops", type: "select" },
                { label: "Difficulty", placeholder: "intermediate" },
              ]} onSave={() => setShowAddPron(false)} onCancel={() => setShowAddPron(false)} />
            )}
            <AdminTable headers={["Title", "Domain", "Difficulty", "Sentences", "Reference Audio", ""]}>
              {adminPronExercises.map(p => (
                <TR key={p.id}>
                  <td style={{ padding: "12px 16px" }}>
                    <p style={{ fontSize: 13, color: "oklch(0.88 0.01 250)", margin: "0 0 2px" }}>{p.title}</p>
                    <p style={{ fontSize: 11, color: "oklch(0.45 0.03 250)", margin: 0, fontFamily: "JetBrains Mono, monospace" }}>{p.titleVi}</p>
                  </td>
                  <td style={{ padding: "12px 16px" }}><KBadge color={domainColor(p.domain)}>{p.domain}</KBadge></td>
                  <td style={{ padding: "12px 16px" }}><KBadge color={diffColor(p.difficulty)}>{p.difficulty}</KBadge></td>
                  <AdminTd mono>{p.sentenceCount} sentences</AdminTd>
                  <td style={{ padding: "12px 16px" }}>
                    <span style={{ fontSize: 12, fontFamily: "JetBrains Mono, monospace", color: p.hasAudio ? "#34d399" : "#f87171" }}>{p.hasAudio ? "✓ uploaded" : "✗ missing"}</span>
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
                    <button style={{ padding: "9px 20px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer" }}>Browse Files</button>
                    <button style={{ padding: "9px 20px", borderRadius: 10, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, fontFamily: "Outfit, sans-serif", fontSize: 13, cursor: "pointer" }}>Download Template</button>
                  </div>
                </div>
                <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", marginBottom: 20 }}>
                  <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 12px" }}>Expected CSV Format</p>
                  <code style={{ display: "block", fontFamily: "JetBrains Mono, monospace", fontSize: 12, color: TEAL, background: "oklch(0.09 0.02 250)", padding: 14, borderRadius: 8, lineHeight: 1.8 }}>
                    korean,pronunciation,vietnamese,domain,hanja,frequency<br />
                    빌드,bild-eu,Build / Biên dịch,devops,,high<br />
                    테스트,te-seu-teu,Kiểm thử,backend,,medium
                  </code>
                </div>
                <button onClick={() => setDictImportStep("preview")} style={{ padding: "10px 24px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, cursor: "pointer" }}>
                  Use Sample Data →
                </button>
              </div>
            )}
            {dictImportStep === "preview" && (
              <div>
                <div style={{ borderRadius: 14, padding: 16, background: `${TEAL}10`, border: `1px solid ${TEAL}30`, marginBottom: 20, display: "flex", alignItems: "center", gap: 12 }}>
                  <CheckCircle size={18} color={TEAL} />
                  <p style={{ fontSize: 13, color: TEAL, fontFamily: "Inter, sans-serif", margin: 0 }}>
                    <strong>5 entries detected</strong> from sample_dict.csv — Review below before importing.
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
                      {dictImportPreview.map((entry, i) => (
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
                  <button style={{ padding: "10px 24px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, cursor: "pointer" }}>Import 5 Entries</button>
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
                {aiPromptTemplates.map(pt => (
                  <button key={pt.id} onClick={() => { setSelectedPrompt(pt); setPromptContent(pt.content); }} style={{ padding: "12px 14px", borderRadius: 10, border: `1px solid ${selectedPrompt.id === pt.id ? `${TEAL}50` : "oklch(0.20 0.03 250)"}`, background: selectedPrompt.id === pt.id ? `${TEAL}10` : "oklch(0.13 0.025 250)", textAlign: "left", cursor: "pointer" }}>
                    <p style={{ fontSize: 13, fontWeight: 600, color: selectedPrompt.id === pt.id ? TEAL : "oklch(0.82 0.01 250)", margin: "0 0 3px", fontFamily: "Outfit, sans-serif" }}>{pt.name}</p>
                    <p style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", margin: 0, fontFamily: "JetBrains Mono, monospace" }}>{pt.desc}</p>
                  </button>
                ))}
              </div>
              <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", display: "flex", flexDirection: "column" }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
                  <div>
                    <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 16, color: "oklch(0.92 0.01 250)", margin: "0 0 2px" }}>{selectedPrompt.name}</p>
                    <p style={{ fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: 0 }}>{selectedPrompt.id}.txt</p>
                  </div>
                  <KBadge color={TEAL}>Gemini 2.5 Pro</KBadge>
                </div>
                <textarea value={promptContent} onChange={e => setPromptContent(e.target.value)} rows={14} style={{ flex: 1, borderRadius: 10, padding: "12px 14px", background: "oklch(0.09 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.82 0.01 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 12, outline: "none", resize: "vertical", lineHeight: 1.7 }} />
                <div style={{ display: "flex", gap: 10, marginTop: 14 }}>
                  <button style={{ padding: "9px 20px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer" }}>Save Changes</button>
                  <button onClick={() => setPromptContent(selectedPrompt.content)} style={{ padding: "9px 16px", borderRadius: 10, border: "1px solid oklch(0.25 0.03 250)", background: "none", color: "oklch(0.55 0.03 250)", fontFamily: "Outfit, sans-serif", fontSize: 13, cursor: "pointer" }}>Reset</button>
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
                <button onClick={() => setInviteEmail("")} style={{ padding: "10px 18px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer" }}>Invite</button>
              </div>
            </div>
            <AdminTable headers={["Admin", "Email", "Role", "Last Login", ""]}>
              {adminAdmins.map(a => (
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
                    {a.role !== "super_admin" && <button style={{ padding: "5px 10px", borderRadius: 7, border: "1px solid #f8717144", background: "#f8717110", color: "#f87171", fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Remove</button>}
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
              <button onClick={() => setShowAddVideo(v => !v)} style={{ padding: "8px 16px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer", display: "flex", alignItems: "center", gap: 6 }}>
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
              <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: `1px solid ${TEAL}30`, marginBottom: 16 }}>
                <p style={{ fontSize: 11, color: TEAL, fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, margin: "0 0 14px" }}>ADD NEW VIDEO</p>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 14 }}>
                  {[["YouTube URL", "https://youtube.com/watch?v=..."], ["Title (Korean)", "강의 제목"], ["Domain", "backend / frontend / devops"], ["Tags", "serverless, aws, architecture"]].map(([label, placeholder]) => (
                    <div key={label}>
                      <p style={{ fontSize: 11, color: "oklch(0.48 0.03 250)", fontFamily: "JetBrains Mono, monospace", margin: "0 0 5px" }}>{label}</p>
                      <input placeholder={placeholder} style={{ width: "100%", padding: "10px 12px", borderRadius: 8, background: "oklch(0.10 0.02 250)", border: "1px solid oklch(0.22 0.03 250)", color: "oklch(0.80 0.01 250)", fontFamily: "Inter, sans-serif", fontSize: 13, outline: "none", boxSizing: "border-box" }} />
                    </div>
                  ))}
                </div>
                <div style={{ display: "flex", gap: 10 }}>
                  <button style={{ padding: "9px 20px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer" }}>Save Video</button>
                  <button onClick={() => setShowAddVideo(false)} style={{ padding: "9px 20px", borderRadius: 10, border: "1px solid oklch(0.25 0.03 250)", background: "none", color: "oklch(0.55 0.03 250)", fontFamily: "Outfit, sans-serif", fontSize: 13, cursor: "pointer" }}>Cancel</button>
                </div>
              </div>
            )}
            <div style={{ borderRadius: 14, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)", overflow: "hidden" }}>
              <table style={{ width: "100%", borderCollapse: "collapse" }}>
                <thead>
                  <tr style={{ borderBottom: "1px solid oklch(0.18 0.03 250)" }}>
                    <th style={{ padding: "12px 12px 12px 16px", width: 32 }}>
                      <input type="checkbox" onChange={e => setVideoBulkSelected(e.target.checked ? new Set(adminVideos.map(v => v.id)) : new Set())} style={{ accentColor: TEAL }} />
                    </th>
                    {["Title", "Domain", "Difficulty", "Subtitles", "Quizzes", ""].map((h, i) => (
                      <th key={i} style={{ padding: "12px 16px", textAlign: "left", fontSize: 10, color: "oklch(0.42 0.03 250)", fontFamily: "JetBrains Mono, monospace", letterSpacing: 1, fontWeight: 600 }}>{h.toUpperCase()}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {adminVideos
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
                            <button onClick={() => setShowSubEditor(true)} style={{ padding: "5px 12px", borderRadius: 7, border: `1px solid ${TEAL}44`, background: `${TEAL}10`, color: TEAL, fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>Edit Subs</button>
                          </td>
                        </tr>
                      );
                    })
                  }
                </tbody>
              </table>
              {adminVideos.filter(v => (videoFilterDomain === "all" || v.domain === videoFilterDomain) && (videoFilterSubs === "all" || v.subtitles === videoFilterSubs)).length === 0 && (
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
                  <button style={{ padding: "9px 20px", borderRadius: 10, border: "none", background: TEAL, color: "#000", fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 13, cursor: "pointer" }}>Save Changes</button>
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
                { label: "DAU", value: "156", sub: "+8% vs last week", color: TEAL },
                { label: "MAU", value: "890", sub: "72% retention", color: "#a78bfa" },
                { label: "Avg Session", value: "18m", sub: "+3m vs last month", color: "#34d399" },
                { label: "Churn Rate", value: "4.2%", sub: "-0.8% vs last month", color: AMBER },
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
                  <LineChart id="line-analytics-dau" data={dauData}>
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
                  <BarChart id="bar-analytics-lessons" data={lessonCompletionData}>
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
                <AreaChart id="area-analytics-ai" data={aiUsageData}>
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
                { label: "New Users / Day", value: "23", sub: "avg this week", color: TEAL },
                { label: "D1 Retention", value: "100%", sub: "baseline", color: "#34d399" },
                { label: "D7 Retention", value: "64%", sub: "+4% vs prev", color: "#a78bfa" },
                { label: "D30 Retention", value: "40%", sub: "industry avg 35%", color: AMBER },
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
                  <BarChart id="bar-new-reg" data={newRegData}>
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
                  <BarChart id="bar-retention" data={retentionData}>
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
                  <BarChart id="bar-content-domain" data={lessonCompletionData}>
                    <XAxis key="x" dataKey="domain" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                    <YAxis key="y" tick={{ fill: "oklch(0.42 0.03 250)", fontSize: 10 }} axisLine={false} tickLine={false} />
                    <Tooltip key="tt" contentStyle={ttStyle} />
                    <Bar key="b" dataKey="completions" fill={TEAL} radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
              <div style={{ borderRadius: 14, padding: 20, background: "oklch(0.13 0.025 250)", border: "1px solid oklch(0.20 0.03 250)" }}>
                <p style={{ fontFamily: "Outfit, sans-serif", fontWeight: 600, fontSize: 14, color: "oklch(0.82 0.01 250)", margin: "0 0 8px" }}>Most Searched Words</p>
                {dictSearchRanking.map((item, i) => (
                  <div key={item.word} style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 8 }}>
                    <span style={{ width: 20, fontFamily: "JetBrains Mono, monospace", fontSize: 10, color: "oklch(0.35 0.03 250)" }}>#{i + 1}</span>
                    <span style={{ flex: 1, fontFamily: "Outfit, sans-serif", fontWeight: 700, fontSize: 14, color: TEAL }}>{item.word}</span>
                    <div style={{ height: 6, width: `${(item.count / 234) * 100}px`, background: `${TEAL}44`, borderRadius: 3 }} />
                    <span style={{ fontFamily: "JetBrains Mono, monospace", fontSize: 11, color: "oklch(0.50 0.03 250)", minWidth: 30 }}>{item.count}</span>
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
                  {contentPerformanceData.map((item, i) => (
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
                { label: "Total Calls / Day", value: "2,298", sub: "avg this week", color: TEAL },
                { label: "Total Cost / Day", value: "$5.6", sub: "$168/month pace", color: AMBER },
                { label: "Avg Response", value: "840ms", sub: "across all services", color: "#34d399" },
                { label: "Error Rate", value: "0.15%", sub: "23 errors total", color: "#f87171" },
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
                <BarChart id="bar-ai-services" data={aiDailyData}>
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
                  {aiServiceBreakdown.map(svc => (
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

// ─── Root ────────────────────────────────────────────────────────────────────────

export default function App() {
  const [screen, setScreen] = useState<Screen>("login");
  const [lang, setLang] = useState<Lang>("vi");
  const [view, setView] = useState<"mobile" | "admin">("mobile");
  const [selectedScenario, setSelectedScenario] = useState<ScenarioType>(scenarios[0]);

  const nav = (s: Screen) => setScreen(s);
  const isAuth = screen !== "login" && screen !== "onboarding";
  const showTabs = ["dashboard", "devvocab", "devvocab-lesson", "membyte", "membyte-review", "profile"].includes(screen);

  const renderScreen = () => {
    switch (screen) {
      case "login": return <LoginScreen nav={nav} />;
      case "onboarding": return <OnboardingScreen nav={nav} />;
      case "dashboard": return <DashboardScreen nav={nav} lang={lang} />;
      case "devvocab": return <DevVocabScreen nav={nav} lang={lang} />;
      case "devvocab-lesson": return <DevVocabLessonScreen nav={nav} />;
      case "membyte": return <MemByteScreen nav={nav} lang={lang} />;
      case "membyte-review": return <MemByteReviewScreen nav={nav} />;
      case "techtalk-select": return <TechTalkSelectScreen nav={nav} lang={lang} onSelect={sc => setSelectedScenario(sc)} />;
      case "techtalk": return <TechTalkScreen nav={nav} lang={lang} scenario={selectedScenario} />;
      case "techtalk-result": return <TechTalkResultScreen nav={nav} lang={lang} />;
      case "honorifics": return <HonorificsScreen nav={nav} />;
      case "video": return <VideoScreen nav={nav} />;
      case "pronunciation-list": return <PronunciationListScreen nav={nav} lang={lang} />;
      case "pronunciation": return <PronunciationScreen nav={nav} />;
      case "profile": return <ProfileScreen lang={lang} setLang={setLang} />;
    }
  };

  if (view === "admin") {
    return (
      <div style={{ position: "relative" }}>
        <style>{`@keyframes kpulse { 0%,100%{opacity:0.35} 50%{opacity:1} }`}</style>
        <div style={{ position: "fixed", top: 14, right: 20, zIndex: 999, display: "flex", gap: 6, padding: "4px", borderRadius: 10, background: "oklch(0.14 0.025 250)", border: "1px solid oklch(0.22 0.03 250)" }}>
          <button onClick={() => setView("mobile")} style={{ padding: "5px 12px", borderRadius: 7, border: "none", background: "none", color: "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>📱 Mobile</button>
          <button style={{ padding: "5px 12px", borderRadius: 7, border: "none", background: TEAL, color: "#000", fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer", fontWeight: 700 }}>⚙️ Admin</button>
        </div>
        <AdminPanel lang={lang} />
      </div>
    );
  }

  return (
    <div style={{ minHeight: "100vh", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", background: "oklch(0.07 0.02 250)" }}>
      <style>{`@keyframes kpulse { 0%,100%{opacity:0.35} 50%{opacity:1} }`}</style>
      <div style={{ position: "fixed", inset: 0, background: `radial-gradient(ellipse at 25% 50%, ${TEAL}07 0%, transparent 55%)`, pointerEvents: "none" }} />
      {isAuth && (
        <div style={{ marginBottom: 16, display: "flex", gap: 4, padding: "4px", borderRadius: 12, background: "oklch(0.14 0.025 250)", border: "1px solid oklch(0.22 0.03 250)", position: "relative", zIndex: 10 }}>
          <button style={{ padding: "6px 16px", borderRadius: 8, border: "none", background: TEAL, color: "#000", fontFamily: "JetBrains Mono, monospace", fontSize: 11, fontWeight: 700, cursor: "pointer" }}>📱 Mobile</button>
          <button onClick={() => setView("admin")} style={{ padding: "6px 16px", borderRadius: 8, border: "none", background: "none", color: "oklch(0.50 0.03 250)", fontFamily: "JetBrains Mono, monospace", fontSize: 11, cursor: "pointer" }}>⚙️ Admin Panel</button>
        </div>
      )}
      <div style={{ width: 390, height: 844, background: "oklch(0.10 0.02 250)", borderRadius: 44, border: "1px solid oklch(0.22 0.03 250)", boxShadow: `0 0 0 8px oklch(0.08 0.02 250), 0 40px 80px rgba(0,0,0,0.65)`, display: "flex", flexDirection: "column", overflow: "hidden", position: "relative" }}>
        <StatusBar />
        <div style={{ flex: 1, display: "flex", flexDirection: "column", overflow: "hidden", position: "relative" }}>
          {renderScreen()}
        </div>
        {showTabs && <TabBar active={screen} nav={nav} lang={lang} />}
      </div>
      <div style={{ marginTop: 16, padding: "6px 16px", borderRadius: 999, background: "oklch(0.14 0.025 250)", border: "1px solid oklch(0.22 0.03 250)", position: "relative", zIndex: 10 }}>
        <span style={{ fontFamily: "JetBrains Mono, monospace", fontSize: 10, color: "oklch(0.38 0.03 250)" }}>KAPOR — Korean IT Communication · prototype v0.2</span>
      </div>
    </div>
  );
}
