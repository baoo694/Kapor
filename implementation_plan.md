# 📋 Implementation Plan — Kapor (AI-Powered Korean IT Communication)

**Phân tích dựa trên:** SRS 7 màn hình, 29 UI components
**Tech Stack:** Expo SDK 53 + Spring Boot 3.4 + MongoDB 7 + VPS

---

## User Review Required

> [!WARNING]
> **AI API Costs:** Nhiều tính năng phụ thuộc Gemini API (roleplay, honorifics, summarizer). Cần thiết lập rate limiting và usage quotas ngay từ đầu để kiểm soát chi phí.

## Decisions Made

| # | Câu hỏi | Quyết định |
|---|---------|------------|
| 1 | Authentication | **Google only** (OAuth2) |
| 2 | Video content | **YouTube embed** (react-native-youtube-iframe) |
| 3 | Offline mode | ⏳ *Chưa quyết định* |
| 4 | UI Language | **Song ngữ Việt/Anh** (2 options, i18n) |
| 5 | Admin panel | **Web riêng biệt** (Next.js, tách hoàn toàn khỏi app) |
| 6 | Monetization | ⏳ *Chưa quyết định* |

## Open Questions

1. **Offline mode:** Mức độ offline support? Chỉ flashcards (MemByte) hay cả lessons (DevVocab)?
2. **Monetization:** Free hoàn toàn hay có premium tier? Ảnh hưởng đến backend authorization logic.

---

# PART 1: FRONTEND (Expo / React Native)

## 1.1 Project Structure

```
kapor-mobile/
├── app/                              # Expo Router (file-based routing)
│   ├── _layout.tsx                   # Root layout (auth check, providers)
│   ├── (auth)/                       # Auth group (unauthenticated)
│   │   ├── _layout.tsx
│   │   ├── login.tsx
│   │   └── onboarding.tsx
│   ├── (tabs)/                       # Main tab navigation
│   │   ├── _layout.tsx               # Tab bar config
│   │   ├── index.tsx                 # Screen 1: DevAnalytics (Dashboard)
│   │   ├── devvocab/                 # Screen 2: DevVocab
│   │   │   ├── index.tsx             # Topic list + skill tree
│   │   │   └── [topicId].tsx         # Individual lesson
│   │   ├── membyte/                  # Screen 4: MemByte
│   │   │   ├── index.tsx             # Deck list
│   │   │   └── review.tsx            # Review session (flip cards)
│   │   └── profile.tsx               # User profile & settings
│   ├── video/                        # Screen 3: Video Player
│   │   └── [videoId].tsx
│   ├── techtalk/                     # Screen 5: TechTalk AI
│   │   ├── index.tsx                 # Scenario selection
│   │   └── [sessionId].tsx           # Active roleplay session
│   ├── pronunciation/                # Screen 6: Pronunciation Lab
│   │   ├── index.tsx                 # Exercise list
│   │   └── [exerciseId].tsx          # Active practice session
│   └── honorifics/                   # Screen 7: Honorifics Analyzer
│       └── index.tsx
│
├── components/
│   ├── ui/                           # Design system primitives
│   │   ├── Button.tsx
│   │   ├── Card.tsx
│   │   ├── Badge.tsx
│   │   ├── ProgressBar.tsx
│   │   ├── Modal.tsx
│   │   ├── BottomSheet.tsx
│   │   └── Typography.tsx
│   ├── dashboard/                    # Screen 1 components
│   │   ├── StreakCounter.tsx          # DA-001
│   │   ├── ProgressChart.tsx         # DA-002
│   │   ├── SmartRecommendation.tsx   # DA-003
│   │   └── QuickNavGrid.tsx          # DA-004
│   ├── devvocab/                     # Screen 2 components
│   │   ├── TechStackTabBar.tsx       # DV-001
│   │   ├── SkillTreeRoadmap.tsx      # DV-002
│   │   ├── LessonProgressBar.tsx     # DV-003
│   │   └── SmartSummarizer.tsx       # DV-004
│   ├── video/                        # Screen 3 components
│   │   ├── MediaEngine.tsx           # VP-001
│   │   ├── DualSubtitleOverlay.tsx   # VP-002
│   │   ├── WordTokenizer.tsx         # VP-003
│   │   ├── DictionaryPopup.tsx       # VP-004
│   │   └── QuizMarker.tsx            # VP-005
│   ├── membyte/                      # Screen 4 components
│   │   ├── FlipCard.tsx              # MB-001
│   │   ├── CodeContextBlock.tsx      # MB-002
│   │   ├── TTSAudioButton.tsx        # MB-003
│   │   └── SRSFeedbackLayout.tsx     # MB-004
│   ├── techtalk/                     # Screen 5 components
│   │   ├── ScenarioProfileHeader.tsx # TT-001
│   │   ├── ChatHistoryThread.tsx     # TT-002
│   │   ├── ScaffoldHintButton.tsx    # TT-003
│   │   └── AudioTextInputBar.tsx     # TT-004
│   ├── pronunciation/                # Screen 6 components
│   │   ├── ReferenceWaveform.tsx     # PS-001
│   │   ├── UserRecordingWaveform.tsx # PS-002
│   │   ├── ColorCodedFeedback.tsx    # PS-003
│   │   └── MetricScoreDashboard.tsx  # PS-004
│   └── honorifics/                   # Screen 7 components
│       ├── RawTextInput.tsx          # HG-001
│       ├── PolitenessIndicator.tsx   # HG-002
│       ├── CorrectionDiffViewer.tsx  # HG-003
│       └── ApplyTransformButton.tsx  # HG-004
│
├── services/                         # API & external service clients
│   ├── api/
│   │   ├── client.ts                 # Axios/Fetch instance + interceptors
│   │   ├── auth.api.ts
│   │   ├── analytics.api.ts
│   │   ├── devvocab.api.ts
│   │   ├── video.api.ts
│   │   ├── membyte.api.ts
│   │   ├── techtalk.api.ts
│   │   ├── pronunciation.api.ts
│   │   └── honorifics.api.ts
│   ├── speech/
│   │   ├── tts.service.ts            # Text-to-Speech wrapper
│   │   └── stt.service.ts            # Speech-to-Text streaming
│   └── storage/
│       └── media.service.ts          # Audio recording, file upload
│
├── stores/                           # Zustand stores
│   ├── authStore.ts
│   ├── dashboardStore.ts
│   ├── reviewStore.ts                # Active SRS review session
│   ├── roleplayStore.ts             # Active TechTalk session
│   └── settingsStore.ts
│
├── hooks/                            # Custom React hooks
│   ├── useAuth.ts
│   ├── useSRS.ts                     # FSRS algorithm hook
│   ├── useAudioRecorder.ts
│   ├── useSubtitleSync.ts
│   └── useStreamingAI.ts            # SSE hook for AI streaming
│
├── lib/
│   ├── fsrs.ts                       # FSRS algorithm implementation
│   ├── constants.ts
│   ├── theme.ts                      # Design tokens
│   ├── types.ts                      # Shared TypeScript types
│   └── i18n/                         # 🌐 Internationalization
│       ├── index.ts                  # i18next config
│       ├── vi.json                   # Vietnamese translations
│       └── en.json                   # English translations
│
├── assets/
│   ├── fonts/
│   ├── images/
│   └── animations/                   # Lottie JSON files
│
└── app.json                          # Expo config
```

## 1.2 Navigation Architecture

```
Root _layout.tsx
├── (auth)                            # Stack: unauthenticated
│   ├── login.tsx
│   └── onboarding.tsx
│
└── (tabs)                            # Tab Navigator: authenticated
    ├── Tab 1: Dashboard (index.tsx)   ← Screen 1: DevAnalytics
    ├── Tab 2: DevVocab               ← Screen 2
    │   └── Stack: index → [topicId]
    ├── Tab 3: MemByte                ← Screen 4
    │   └── Stack: index → review
    ├── Tab 4: Profile
    │
    ├── Modal: Video Player           ← Screen 3 (presented modally)
    ├── Modal: TechTalk AI            ← Screen 5 (presented modally)
    ├── Modal: Pronunciation Lab      ← Screen 6 (presented modally)
    └── Modal: Honorifics Checker     ← Screen 7 (presented modally)
```

> [!NOTE]
> Screens 3, 5, 6, 7 được truy cập qua **Quick Navigation Grid (DA-004)** trên Dashboard hoặc contextual links. Chúng được present dạng modal/full-screen stack thay vì tab riêng.

## 1.3 Screen-by-Screen Component Specification

### Screen 1: DevAnalytics (Dashboard)

#### DA-001: StreakCounter
```typescript
interface StreakCounterProps {
  currentStreak: number;
  longestStreak: number;
  isActiveToday: boolean;
}
// Visual: Lottie flame animation khi streak > 0
// Tap: Mở streak calendar modal
// Animation: Scale bounce khi streak tăng
```

#### DA-002: ProgressChart
```typescript
interface ProgressChartProps {
  period: 'weekly' | 'monthly';
  metrics: {
    speaking: number;      // 0-100
    vocabulary: number;    // 0-100
    listening: number;     // 0-100
    roleplayScore: number; // 0-100
  };
}
// Visual: Ring chart (React Native Skia) cho 4 metrics
// Toggle: Switch weekly/monthly bằng segmented control
// Animation: Draw-in animation khi data load
```

#### DA-003: SmartRecommendation
```typescript
interface SmartRecommendationProps {
  recommendation: {
    type: 'resume_lesson' | 'next_challenge' | 'review_due' | 'new_topic';
    title: string;         // "Resume: Frontend Deployment Vocabulary"
    subtitle: string;      // "15 từ vựng cần ôn tập"
    targetScreen: string;  // Deep link target
    icon: string;
  };
}
// Visual: Gradient card với icon + title + CTA button
// Data: Fetched từ AI recommendation endpoint (mỗi lần mở app)
// Tap: Navigate tới targetScreen
```

#### DA-004: QuickNavGrid
```typescript
// 2x2 grid layout
const modules = [
  { id: 'devvocab', icon: '📚', label: 'DevVocab', route: '/(tabs)/devvocab' },
  { id: 'techtalk', icon: '🤖', label: 'TechTalk AI', route: '/techtalk' },
  { id: 'membyte', icon: '🧠', label: 'MemByte', route: '/(tabs)/membyte' },
  { id: 'honorifics', icon: '🎯', label: 'Honorifics', route: '/honorifics' },
];
// Visual: Glass-morphism cards với icon + label
// Animation: Press scale effect (Reanimated)
```

---

### Screen 2: DevVocab (Topic Selection)

#### DV-001: TechStackTabBar
```typescript
type Domain = 'all' | 'frontend' | 'backend' | 'devops' | 'agile';

interface TechStackTabBarProps {
  activeTab: Domain;
  onTabChange: (tab: Domain) => void;
  topicCounts: Record<Domain, number>;
}
// Visual: Horizontal scrollable segmented control
// Chip style: Filled chip cho active, outlined cho inactive
// Animation: Indicator slide animation
```

#### DV-002: SkillTreeRoadmap
```typescript
interface SkillNode {
  id: string;
  title: string;           // "CSS Grid & Flexbox 용어"
  domain: Domain;
  isLocked: boolean;        // true nếu chưa hoàn thành prerequisites
  prerequisites: string[];  // node IDs
  completionPercent: number;
  lessonCount: number;
}

interface SkillTreeRoadmapProps {
  nodes: SkillNode[];
  onNodePress: (nodeId: string) => void;
}
// Visual: Vertical connected nodes với lines
// Locked: Grayscale + padlock icon overlay
// Unlocked: Color + progress ring
// Animation: Unlock animation khi prerequisite hoàn thành
```

#### DV-003: LessonProgressBar
```typescript
// Embedded trong mỗi SkillNode card
interface LessonProgressBarProps {
  completionPercent: number; // 0-100
  totalLessons: number;
  completedLessons: number;
}
// Visual: Thin gradient progress bar (teal → blue)
// Text: "12/20 bài" bên phải
```

#### DV-004: SmartSummarizer
```typescript
interface SmartSummarizerProps {
  onSubmit: (rawText: string) => Promise<FlashcardDeck>;
}
// Visual: Floating Action Button → mở bottom sheet
// Bottom sheet: TextArea + "Generate Flashcards" button
// Loading: Skeleton cards animation
// Result: Preview generated flashcards → "Save to MemByte" CTA
```

---

### Screen 3: Interactive Video Player

> [!IMPORTANT]
> **YouTube Embed:** Video không self-hosted. Dùng `react-native-youtube-iframe` để embed YouTube videos.
> Subtitles được quản lý riêng ở backend (admin upload), không dùng YouTube captions.

#### VP-001: MediaEngine (YouTube Embed)
```typescript
interface MediaEngineProps {
  youtubeVideoId: string;  // YouTube video ID (e.g., "dQw4w9WgXcQ")
  onTimeUpdate: (currentTime: number) => void;
  onQuizTrigger: (quizId: string) => void;
  playbackSpeed: 0.5 | 0.75 | 1.0 | 1.25 | 1.5;
}
// Implementation: react-native-youtube-iframe
// Wrapper: Polls getCurrentTime() mỗi 250ms → sync subtitles
// Controls: YouTube native controls + custom speed toggle overlay
// Note: YouTube IFrame API hỗ trợ playbackRate, seekTo, getCurrentTime
```

**YouTube → Subtitle Sync Flow:**
```
react-native-youtube-iframe
  ↓ onStateChange (playing/paused)
  ↓ setInterval → playerRef.getCurrentTime()
  ↓ currentTime (seconds)
  ↓
DualSubtitleOverlay (VP-002)
  ↓ binary search currentTime trong subtitle array
  ↓
Active subtitle line rendered with tokenized words
```

#### VP-002: DualSubtitleOverlay
```typescript
interface SubtitleLine {
  start: number;  // seconds
  end: number;
  text: string;
  tokens?: TokenizedWord[];  // Pre-tokenized từ backend
}

interface DualSubtitleOverlayProps {
  koreanSubtitles: SubtitleLine[];
  vietnameseSubtitles: SubtitleLine[];
  currentTime: number;
  onWordTap: (word: TokenizedWord) => void;
}
// Layout: Korean (top, larger font) + Vietnamese (bottom, smaller)
// Sync: Binary search currentTime trong subtitle array
```

#### VP-003: WordTokenizer
```typescript
interface TokenizedWord {
  surface: string;       // "배포가" (surface form)
  stem: string;          // "배포" (dictionary form)
  pos: string;           // "NNG" (part of speech)
  isClickable: boolean;  // particles (조사) = false
}
// Render: Mỗi token = TouchableOpacity
// Visual: Underline dotted cho clickable words
// Tap: Pause video + trigger DictionaryPopup (VP-004)
```

#### VP-004: DictionaryPopup
```typescript
interface DictionaryPopupProps {
  word: TokenizedWord;
  definition: {
    korean: string;
    vietnamese: string;
    pronunciation: string;  // romanized
    itContext: string;       // IT-specific meaning
    examples: string[];
  };
  onAddToMemByte: () => void;
  onClose: () => void;
}
// Visual: Bottom sheet (react-native-bottom-sheet)
// Content: Word + pronunciation + IT definition + example
// CTA: "➕ Thêm vào MemByte" button
// Animation: Spring slide up
```

#### VP-005: QuizMarker
```typescript
interface QuizMarkerProps {
  markers: { timestamp: number; quizId: string }[];
  videoDuration: number;
  currentTime: number;
}
// Visual: Small dots on the seek bar at quiz timestamps
// Color: Yellow dot (upcoming), Green (completed), Blue (current)
// Trigger: Auto-pause + show quiz modal khi currentTime = marker.timestamp
```

---

### Screen 4: MemByte (Intelligent Flashcards)

#### MB-001: FlipCard
```typescript
interface FlipCardProps {
  front: {
    korean: string;        // "오류"
    pronunciation: string; // "oryu"
    audioUri: string;
  };
  back: {
    vietnamese: string;    // "Lỗi"
    itContext: string;     // "Error in try-catch exception handling"
    exampleSentence: string;
    codeSnippet?: string;  // Hiển thị bằng CodeContextBlock
  };
  onFlip: () => void;
}
// Animation: Reanimated 3 — 3D Y-axis rotation (withTiming, 400ms)
// Gesture: Tap to flip
// Front: Large Korean text + pronunciation + audio button
// Back: Vietnamese meaning + context + code block
```

#### MB-002: CodeContextBlock
```typescript
interface CodeContextBlockProps {
  language: string;        // "javascript", "python", etc.
  code: string;
  highlightWord: string;   // Korean term to highlight in code
}
// Visual: Syntax highlighted code block (react-native-syntax-highlighter)
// Highlight: Target Korean word highlighted với background color
// Example: "try { } catch(오류) { console.log(오류) }" — "오류" highlighted
```

#### MB-003: TTSAudioButton
```typescript
interface TTSAudioButtonProps {
  text: string;            // Korean text to speak
  speed: 'slow' | 'normal' | 'fast';
  onPlay: () => void;
}
// Visual: Round icon button với speaker icon
// States: Idle → Playing (animated equalizer bars) → Done
// Implementation: GCP TTS API qua backend proxy
```

#### MB-004: SRSFeedbackLayout
```typescript
type SRSRating = 'again' | 'hard' | 'good' | 'easy';

interface SRSFeedbackLayoutProps {
  onRate: (rating: SRSRating) => void;
  nextReviewTimes: Record<SRSRating, string>; // "< 1m", "6m", "1d", "4d"
}
// Visual: 4 buttons in a row
// Colors: Again (red), Hard (orange), Good (green), Easy (blue)
// Subtitle: Next review time under each button
// Animation: Slide up after card flip
```

---

### Screen 5: TechTalk AI (Roleplay)

#### TT-001: ScenarioProfileHeader
```typescript
interface ScenarioProfileHeaderProps {
  persona: {
    name: string;          // "Kim Min-su"
    role: string;          // "Tech Lead"
    avatarUri: string;
    company: string;       // "Naver"
  };
  mission: {
    title: string;         // "Report a critical production bug"
    difficulty: 'beginner' | 'intermediate' | 'advanced';
    objectives: string[];
  };
}
// Visual: Top banner với avatar circle + name + role badge
// Mission: Collapsible section showing objectives
```

#### TT-002: ChatHistoryThread
```typescript
interface ChatMessage {
  id: string;
  role: 'ai' | 'user';
  content: string;
  audioUri?: string;       // AI messages có auto-generated TTS
  timestamp: Date;
  evaluation?: {           // Inline feedback cho user messages
    grammar: number;
    vocabulary: number;
    politeness: number;
    corrections?: string[];
  };
}

interface ChatHistoryThreadProps {
  messages: ChatMessage[];
  isAITyping: boolean;
  onPlayAudio: (uri: string) => void;
}
// Visual: Alternating chat bubbles (AI left, User right)
// AI messages: Auto-play TTS, gradient bubble
// User messages: Solid bubble + optional evaluation badge
// Streaming: SSE để hiện text từng token (typing effect)
// List: FlashList cho performance
```

#### TT-003: ScaffoldHintButton
```typescript
interface ScaffoldHintButtonProps {
  onRequestHint: () => Promise<ScaffoldHint>;
}

interface ScaffoldHint {
  keywords: string[];         // ["배포", "서버", "장애"]
  sentenceStructure: string;  // "... 에 문제가 발생했습니다"
  politenessTip: string;      // "Dùng -습니다 thay vì -어요 khi nói với Tech Lead"
}
// Visual: Floating lightbulb FAB (bottom-right)
// Tap: Show hint card with keywords + sentence template
// Animation: Pulse glow khi user im lặng > 10s
```

#### TT-004: AudioTextInputBar
```typescript
interface AudioTextInputBarProps {
  mode: 'text' | 'voice';
  onSendText: (text: string) => void;
  onSendAudio: (audioUri: string, transcript: string) => void;
  onModeToggle: () => void;
  isRecording: boolean;
}
// Visual: Input bar ở bottom
// Text mode: TextInput + Send button
// Voice mode: Mic button (hold to record) + waveform animation
// Toggle: Keyboard ↔ Microphone icon button
// STT: Streaming speech-to-text, hiện transcript real-time
```

---

### Screen 6: AI Pronunciation & Shadowing Lab

#### PS-001: ReferenceWaveform
```typescript
interface ReferenceWaveformProps {
  audioUri: string;
  waveformData: number[];  // Amplitude samples
  onPlay: () => void;
  isPlaying: boolean;
}
// Visual: React Native Skia canvas — waveform bars
// Color: Teal/cyan gradient
// Playback: Animated cursor moving along waveform
// Markers: Intonation peak indicators
```

#### PS-002: UserRecordingWaveform
```typescript
interface UserRecordingWaveformProps {
  recordedAudioUri: string;
  waveformData: number[];
  referenceWaveformData: number[];  // For overlay comparison
  isRecording: boolean;
  onStartRecording: () => void;
  onStopRecording: () => void;
}
// Visual: User waveform overlaid (semi-transparent) trên reference
// Color: Orange/amber cho user waveform
// Recording state: Live amplitude bars animation
// Comparison: Visual rhythm alignment feedback
```

#### PS-003: ColorCodedFeedback
```typescript
interface TranscribedWord {
  text: string;
  accuracy: 'correct' | 'minor_error' | 'major_error' | 'omitted';
  phonemeDetail?: string;  // IPA feedback
}

interface ColorCodedFeedbackProps {
  words: TranscribedWord[];
}
// Visual: Text container với color-coded words
// Colors: Green (correct), Orange (minor), Red (major/omitted)
// Tap word: Show phoneme-level detail popover
```

#### PS-004: MetricScoreDashboard
```typescript
interface MetricScoreDashboardProps {
  accuracy: number;    // 0-100
  fluency: number;     // 0-100
  prosody: number;     // 0-100
  previousScores?: {   // For comparison
    accuracy: number;
    fluency: number;
    prosody: number;
  };
}
// Visual: 3 circular progress indicators (React Native SVG)
// Colors: Each metric has distinct color
// Animation: Count-up animation from 0 to score
// Comparison: Small arrow (↑↓) vs previous attempt
```

---

### Screen 7: Honorifics & Grammar Analyzer

#### HG-001: RawTextInput
```typescript
interface RawTextInputProps {
  value: string;
  onChangeText: (text: string) => void;
  onMicPress: () => void;  // Dictation mode
  placeholder: string;
  maxLength: number;
}
// Visual: Multi-line TextArea với mic button
// Features: Character count, clear button
// Dictation: Hold mic → STT → insert text
```

#### HG-002: PolitenessIndicator
```typescript
type PolitenessLevel = 'banmal' | 'heyohaet' | 'hasipsio';

interface PolitenessIndicatorProps {
  level: PolitenessLevel;
  confidence: number;  // 0-1
}
// Visual: Badge with color coding
// Banmal (반말): Red badge — "Casual / ⚠️ Danger"
// Heyohaet-che (해요체): Yellow badge — "Polite / Standard"
// Hasipsio-che (하십시오체): Green badge — "Formal / Corporate"
// Animation: Badge transition khi level thay đổi real-time
```

#### HG-003: CorrectionDiffViewer
```typescript
interface CorrectionDiff {
  original: string;
  corrected: string;
  type: 'particle' | 'honorific' | 'vocabulary' | 'grammar';
  explanation: string;  // "너 → 귀하 (formal 'you' for business context)"
}

interface CorrectionDiffViewerProps {
  diffs: CorrectionDiff[];
}
// Visual: Side-by-side or inline diff view
// Left: Original text with red strikethrough on errors
// Right: Corrected text with green highlights
// Tap diff: Show explanation popover
```

#### HG-004: ApplyTransformButton
```typescript
interface ApplyTransformButtonProps {
  transformedText: string;
  onApply: () => void;
  onCopyToClipboard: () => void;
}
// Visual: Primary action button + clipboard icon
// Tap "Apply": Replace input text với transformed version
// Tap "Copy": Copy to clipboard + toast notification
// Animation: Checkmark animation on success
```

---

## 1.4 State Management Architecture

### Zustand Stores

```typescript
// stores/authStore.ts
interface AuthStore {
  user: User | null;
  token: string | null;
  isLoading: boolean;
  locale: 'vi' | 'en';                // 🌐 UI language
  login: (provider: 'google') => Promise<void>;  // Google only
  logout: () => void;
  refreshToken: () => Promise<void>;
  setLocale: (locale: 'vi' | 'en') => void;
}

// stores/reviewStore.ts — Active MemByte review session
interface ReviewStore {
  currentDeck: FlashcardDeck | null;
  currentCardIndex: number;
  isFlipped: boolean;
  sessionStats: { again: number; hard: number; good: number; easy: number };
  startReview: (deckId: string) => void;
  flipCard: () => void;
  rateCard: (rating: SRSRating) => void;
  nextCard: () => void;
  endSession: () => void;
}

// stores/roleplayStore.ts — Active TechTalk session
interface RoleplayStore {
  session: RoleplaySession | null;
  messages: ChatMessage[];
  isAIResponding: boolean;
  inputMode: 'text' | 'voice';
  startSession: (scenarioId: string) => void;
  sendMessage: (content: string, audioUri?: string) => void;
  requestHint: () => Promise<ScaffoldHint>;
  endSession: () => void;
}
```

### TanStack Query Keys

```typescript
const queryKeys = {
  // Dashboard
  dashboard: ['dashboard'] as const,
  streak: ['dashboard', 'streak'] as const,
  recommendation: ['dashboard', 'recommendation'] as const,
  progress: (period: string) => ['dashboard', 'progress', period] as const,

  // DevVocab
  topics: (domain: string) => ['topics', domain] as const,
  skillTree: (domain: string) => ['skillTree', domain] as const,
  lesson: (topicId: string) => ['lesson', topicId] as const,

  // Videos
  videos: (filters: object) => ['videos', filters] as const,
  videoDetail: (id: string) => ['video', id] as const,
  subtitles: (videoId: string) => ['subtitles', videoId] as const,

  // MemByte
  decks: ['decks'] as const,
  deckCards: (deckId: string) => ['deck', deckId, 'cards'] as const,
  dueCards: ['cards', 'due'] as const,

  // TechTalk
  scenarios: ['scenarios'] as const,
  sessionHistory: ['techtalk', 'history'] as const,

  // Pronunciation
  exercises: ['pronunciation', 'exercises'] as const,

  // Dictionary
  definition: (word: string) => ['dictionary', word] as const,
};
```

## 1.5 API Client Layer

```typescript
// services/api/client.ts
import axios from 'axios';
import { useAuthStore } from '@/stores/authStore';

const apiClient = axios.create({
  baseURL: process.env.EXPO_PUBLIC_API_URL, // https://api.kapor.app
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor: Attach JWT
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Response interceptor: Handle 401 → refresh token
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      await useAuthStore.getState().refreshToken();
      return apiClient.request(error.config);
    }
    return Promise.reject(error);
  }
);

// SSE Client for AI Streaming
export const createSSEConnection = (url: string, onMessage: (data: string) => void) => {
  const eventSource = new EventSource(url, {
    headers: { Authorization: `Bearer ${useAuthStore.getState().token}` },
  });
  eventSource.onmessage = (event) => onMessage(event.data);
  return eventSource;
};
```

---

# PART 2: BACKEND (Spring Boot 3.4 + Java 21)

## 2.1 Project Structure

```
kapor-backend/
├── src/main/java/com/kapor/
│   │
│   ├── KaporApplication.java                 # @SpringBootApplication
│   │
│   ├── config/
│   │   ├── SecurityConfig.java               # Spring Security + JWT filter chain
│   │   ├── MongoConfig.java                  # MongoDB connection + auditing
│   │   ├── RedisConfig.java                  # Redis template + cache manager
│   │   ├── CorsConfig.java                   # CORS policy
│   │   ├── MinioConfig.java                  # MinIO S3 client
│   │   ├── GeminiConfig.java                 # LangChain4j model config
│   │   └── WebFluxConfig.java                # SSE + reactive endpoints
│   │
│   ├── auth/
│   │   ├── controller/
│   │   │   └── AuthController.java           # POST /api/auth/login, /register, /refresh
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   ├── JwtService.java               # JWT generate, validate, refresh
│   │   │   └── OAuth2Service.java            # Google, Apple, Kakao verification
│   │   ├── model/
│   │   │   ├── LoginRequest.java
│   │   │   └── AuthResponse.java             # { accessToken, refreshToken, user }
│   │   └── security/
│   │       ├── JwtAuthFilter.java            # OncePerRequestFilter
│   │       └── CustomUserDetails.java
│   │
│   ├── user/
│   │   ├── controller/UserController.java    # GET/PUT /api/users/me
│   │   ├── service/UserService.java
│   │   ├── model/
│   │   │   ├── User.java                     # @Document
│   │   │   └── UserSettings.java             # Embedded
│   │   └── repository/UserRepository.java    # MongoRepository
│   │
│   ├── analytics/
│   │   ├── controller/AnalyticsController.java
│   │   ├── service/
│   │   │   ├── AnalyticsService.java         # Dashboard data aggregation
│   │   │   ├── StreakService.java             # Streak calculation logic
│   │   │   └── RecommendationService.java    # AI-powered recommendations
│   │   ├── model/
│   │   │   ├── LearningProgress.java         # @Document
│   │   │   ├── DailyActivity.java            # @Document
│   │   │   └── ProgressMetrics.java          # Embedded: speaking, vocab, listening, roleplay
│   │   └── repository/
│   │       ├── LearningProgressRepository.java
│   │       └── DailyActivityRepository.java
│   │
│   ├── devvocab/
│   │   ├── controller/
│   │   │   ├── TopicController.java          # GET /api/topics, /api/topics/{id}
│   │   │   └── LessonController.java         # GET /api/lessons/{id}, POST /api/lessons/{id}/complete
│   │   ├── service/
│   │   │   ├── TopicService.java
│   │   │   ├── LessonService.java
│   │   │   └── SkillTreeService.java         # Prerequisite logic, unlock checking
│   │   ├── model/
│   │   │   ├── Topic.java                    # @Document
│   │   │   ├── Lesson.java                   # @Document
│   │   │   └── SkillNode.java                # Embedded in Topic
│   │   └── repository/
│   │       ├── TopicRepository.java
│   │       └── LessonRepository.java
│   │
│   ├── video/
│   │   ├── controller/VideoController.java   # GET /api/videos, /api/videos/{id}
│   │   ├── service/
│   │   │   ├── VideoService.java
│   │   │   └── SubtitleService.java          # Subtitle sync, tokenized data
│   │   ├── model/
│   │   │   ├── Video.java                    # @Document
│   │   │   ├── SubtitleLine.java             # Embedded
│   │   │   └── QuizMarker.java              # Embedded
│   │   └── repository/VideoRepository.java
│   │
│   ├── membyte/
│   │   ├── controller/
│   │   │   ├── DeckController.java           # CRUD /api/decks
│   │   │   ├── CardController.java           # CRUD /api/cards
│   │   │   └── ReviewController.java         # POST /api/review/session, /api/review/rate
│   │   ├── service/
│   │   │   ├── DeckService.java
│   │   │   ├── CardService.java
│   │   │   └── FSRSService.java              # FSRS algorithm implementation
│   │   ├── model/
│   │   │   ├── Deck.java                     # @Document
│   │   │   ├── Flashcard.java                # @Document
│   │   │   └── FSRSData.java                 # Embedded: difficulty, stability, etc.
│   │   └── repository/
│   │       ├── DeckRepository.java
│   │       └── FlashcardRepository.java
│   │
│   ├── techtalk/
│   │   ├── controller/
│   │   │   ├── ScenarioController.java       # GET /api/scenarios
│   │   │   └── RoleplayController.java       # POST /api/roleplay/start, /send, /hint
│   │   ├── service/
│   │   │   ├── ScenarioService.java
│   │   │   ├── RoleplayService.java          # Session management
│   │   │   └── RoleplayStreamService.java    # SSE streaming AI responses
│   │   ├── model/
│   │   │   ├── Scenario.java                 # @Document
│   │   │   ├── RoleplaySession.java          # @Document
│   │   │   ├── ChatMessage.java              # Embedded
│   │   │   └── Persona.java                  # Embedded
│   │   └── repository/
│   │       ├── ScenarioRepository.java
│   │       └── RoleplaySessionRepository.java
│   │
│   ├── pronunciation/
│   │   ├── controller/PronunciationController.java  # POST /api/pronunciation/evaluate
│   │   ├── service/
│   │   │   ├── PronunciationService.java
│   │   │   └── WaveformService.java          # Audio analysis, waveform extraction
│   │   ├── model/
│   │   │   ├── PronunciationExercise.java    # @Document
│   │   │   ├── PronunciationAttempt.java     # @Document
│   │   │   └── PronunciationScore.java       # Embedded: accuracy, fluency, prosody
│   │   └── repository/
│   │       ├── ExerciseRepository.java
│   │       └── AttemptRepository.java
│   │
│   ├── honorifics/
│   │   ├── controller/HonorificsController.java  # POST /api/honorifics/analyze, /transform
│   │   ├── service/
│   │   │   ├── HonorificsService.java
│   │   │   └── PolitenessClassifier.java     # AI classification logic
│   │   ├── model/
│   │   │   ├── HonorificsAnalysis.java       # Response DTO
│   │   │   └── CorrectionDiff.java           # DTO
│   │   └── repository/                        # (stateless, no persistence needed)
│   │
│   ├── dictionary/
│   │   ├── controller/DictionaryController.java  # GET /api/dictionary/{word}
│   │   ├── service/DictionaryService.java
│   │   ├── model/
│   │   │   └── DictionaryEntry.java          # @Document
│   │   └── repository/DictionaryRepository.java
│   │
│   ├── ai/
│   │   ├── GeminiChatService.java            # LangChain4j Gemini wrapper
│   │   ├── RoleplayAgent.java                # AI Service interface for TechTalk
│   │   ├── HonorificsAgent.java              # AI Service interface for Grammar
│   │   ├── SummarizerAgent.java              # AI Service interface for article→flashcards
│   │   ├── RecommendationAgent.java          # AI Service interface for smart recommendations
│   │   └── prompts/                          # Prompt template files
│   │       ├── roleplay-system.txt
│   │       ├── honorifics-analyze.txt
│   │       ├── honorifics-transform.txt
│   │       ├── summarizer.txt
│   │       ├── recommendation.txt
│   │       └── scaffold-hint.txt
│   │
│   ├── nlp/
│   │   ├── KoreanTokenizerClient.java        # REST client → FastAPI Mecab service
│   │   └── TokenizationResult.java
│   │
│   ├── speech/
│   │   ├── controller/SpeechController.java  # POST /api/speech/tts, /stt
│   │   ├── service/
│   │   │   ├── TTSService.java               # GCP TTS proxy
│   │   │   └── STTService.java               # GCP STT proxy
│   │   └── model/
│   │       ├── TTSRequest.java
│   │       └── STTResult.java
│   │
│   ├── storage/
│   │   ├── controller/StorageController.java # POST /api/storage/upload, GET /api/storage/{key}
│   │   └── service/MinioStorageService.java  # MinIO S3 operations
│   │
│   └── common/
│       ├── dto/
│       │   ├── ApiResponse.java              # Standard { success, data, error } wrapper
│       │   └── PageResponse.java             # Paginated response
│       ├── exception/
│       │   ├── GlobalExceptionHandler.java   # @ControllerAdvice
│       │   ├── ResourceNotFoundException.java
│       │   └── UnauthorizedException.java
│       └── util/
│           └── DateTimeUtil.java
│
├── src/main/resources/
│   ├── application.yml                        # Main config
│   ├── application-dev.yml                    # Dev profile
│   ├── application-prod.yml                   # Production profile
│   └── prompts/                               # AI prompt templates
│
├── src/test/java/com/kapor/                   # Test files mirror main structure
│
├── Dockerfile
├── docker-compose.yml
├── docker-compose.dev.yml
└── pom.xml                                    # Maven dependencies
```

## 2.2 REST API Specification

### Authentication

| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| POST | `/api/auth/register` | Đăng ký tài khoản | `{ email, password, name }` | `AuthResponse` |
| POST | `/api/auth/login` | Đăng nhập email/password | `{ email, password }` | `AuthResponse` |
| POST | `/api/auth/oauth` | OAuth2 login | `{ provider, idToken }` | `AuthResponse` |
| POST | `/api/auth/refresh` | Refresh access token | `{ refreshToken }` | `AuthResponse` |
| POST | `/api/auth/logout` | Logout (blacklist token) | — | `204` |

### User Profile

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/me` | Lấy profile hiện tại |
| PUT | `/api/users/me` | Cập nhật profile |
| PUT | `/api/users/me/settings` | Cập nhật settings (TTS speed, daily goal, theme) |

### Dashboard & Analytics (Screen 1)

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/api/analytics/dashboard` | Tổng hợp dashboard data | `{ streak, progress, recommendation }` |
| GET | `/api/analytics/streak` | Streak details | `{ current, longest, calendar[] }` |
| GET | `/api/analytics/progress?period=weekly` | Progress metrics | `{ speaking, vocabulary, listening, roleplay }` |
| GET | `/api/analytics/recommendation` | AI smart recommendation | `{ type, title, targetScreen }` |
| POST | `/api/analytics/activity` | Log daily activity | `{ activityType, duration, score }` |

### DevVocab (Screen 2)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/topics?domain=frontend` | Danh sách topics theo domain |
| GET | `/api/topics/{topicId}` | Chi tiết topic + skill tree |
| GET | `/api/topics/{topicId}/lessons` | Danh sách lessons trong topic |
| GET | `/api/lessons/{lessonId}` | Chi tiết bài học |
| POST | `/api/lessons/{lessonId}/complete` | Hoàn thành bài học |
| POST | `/api/summarizer/generate` | AI: Paste article → generate flashcards |

### Videos (Screen 3)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/videos?domain=frontend&page=1` | Danh sách videos (paginated) |
| GET | `/api/videos/{videoId}` | Video detail + metadata |
| GET | `/api/videos/{videoId}/subtitles` | Tokenized dual subtitles |
| GET | `/api/videos/{videoId}/quizzes` | Quiz markers data |
| POST | `/api/videos/{videoId}/quiz/{quizId}/answer` | Submit quiz answer |

### MemByte (Screen 4)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/decks` | Danh sách flashcard decks |
| POST | `/api/decks` | Tạo deck mới |
| DELETE | `/api/decks/{deckId}` | Xóa deck |
| GET | `/api/decks/{deckId}/cards` | Cards trong deck |
| POST | `/api/cards` | Thêm card mới |
| PUT | `/api/cards/{cardId}` | Sửa card |
| DELETE | `/api/cards/{cardId}` | Xóa card |
| GET | `/api/review/due?limit=20` | Lấy cards cần ôn (FSRS) |
| POST | `/api/review/rate` | Submit SRS rating | 

**POST `/api/review/rate`** Request:
```json
{
  "cardId": "abc123",
  "rating": "good",
  "responseTimeMs": 3500
}
```
Response:
```json
{
  "nextReview": "2026-06-28T10:00:00Z",
  "newDifficulty": 5.2,
  "newStability": 12.5,
  "sessionStats": { "reviewed": 15, "remaining": 5 }
}
```

### TechTalk AI (Screen 5)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/scenarios` | Danh sách roleplay scenarios |
| GET | `/api/scenarios/{id}` | Scenario detail + persona |
| POST | `/api/roleplay/start` | Bắt đầu session mới |
| POST | `/api/roleplay/{sessionId}/send` | Gửi message (text/audio) |
| GET | `/api/roleplay/{sessionId}/stream` | **SSE** — Stream AI response |
| POST | `/api/roleplay/{sessionId}/hint` | Yêu cầu scaffold hint |
| POST | `/api/roleplay/{sessionId}/end` | Kết thúc session + get evaluation |
| GET | `/api/roleplay/history` | Lịch sử sessions |

**SSE Streaming Flow:**
```
Client: POST /api/roleplay/{sessionId}/send
        { "content": "서버에 오류가 발생했습니다", "audioUri": null }

Server: 202 Accepted + Location header → SSE URL

Client: GET /api/roleplay/{sessionId}/stream (EventSource)

Server sends SSE events:
  event: token
  data: {"text": "네,"}

  event: token
  data: {"text": " 어떤"}

  event: token
  data: {"text": " 오류인지"}

  event: evaluation
  data: {"grammar": 85, "vocabulary": 90, "politeness": 95}

  event: done
  data: {"messageId": "msg_xyz"}
```

### Pronunciation Lab (Screen 6)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/pronunciation/exercises` | Danh sách bài tập |
| GET | `/api/pronunciation/exercises/{id}` | Bài tập detail + reference audio |
| POST | `/api/pronunciation/evaluate` | Upload audio → get scoring |
| GET | `/api/pronunciation/history` | Lịch sử attempts |

**POST `/api/pronunciation/evaluate`** — multipart/form-data:
```
audioFile: [binary WAV/M4A]
exerciseId: "ex_123"
```
Response:
```json
{
  "accuracy": 82,
  "fluency": 75,
  "prosody": 68,
  "transcription": [
    { "text": "서버", "accuracy": "correct" },
    { "text": "배포가", "accuracy": "minor_error", "phonemeDetail": "배 → [pɛ] expected [bɛ]" },
    { "text": "완료되었습니다", "accuracy": "correct" }
  ],
  "referenceWaveform": [0.12, 0.45, ...],
  "userWaveform": [0.10, 0.38, ...]
}
```

### Honorifics Analyzer (Screen 7)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/honorifics/analyze` | Phân tích text input |
| POST | `/api/honorifics/transform` | Chuyển đổi sang formal Korean |

**POST `/api/honorifics/analyze`** Request:
```json
{
  "text": "나 오늘 서버 배포 했어. 너 확인해봐.",
  "targetLevel": "hasipsio"
}
```
Response:
```json
{
  "currentLevel": "banmal",
  "confidence": 0.95,
  "corrections": [
    {
      "original": "나",
      "corrected": "저",
      "type": "pronoun",
      "explanation": "나 (casual 'I') → 저 (humble 'I' for formal context)"
    },
    {
      "original": "했어",
      "corrected": "하였습니다",
      "type": "verb_ending",
      "explanation": "했어 (casual past) → 하였습니다 (formal past)"
    },
    {
      "original": "너",
      "corrected": "귀하",
      "type": "pronoun",
      "explanation": "너 (casual 'you') → 귀하 (formal 'you' for business)"
    },
    {
      "original": "확인해봐",
      "corrected": "확인해 주시기 바랍니다",
      "type": "verb_ending",
      "explanation": "해봐 (casual imperative) → 해 주시기 바랍니다 (formal request)"
    }
  ]
}
```

### Dictionary & NLP

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/dictionary/{word}` | Tra từ điển IT tiếng Hàn |
| POST | `/api/nlp/tokenize` | Tách từ tiếng Hàn (proxy → FastAPI) |

### Speech (Proxy to GCP)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/speech/tts` | Text-to-Speech (trả về audio URL) |
| POST | `/api/speech/stt` | Speech-to-Text (upload audio → text) |
| WS | `/ws/stt/stream` | WebSocket streaming STT |

### Storage

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/storage/upload` | Upload file (audio/image) → MinIO |
| GET | `/api/storage/{key}` | Get presigned URL for file |

---

## 2.3 AI Service Layer (LangChain4j)

### LangChain4j Integration

```java
// config/GeminiConfig.java
@Configuration
public class GeminiConfig {

    @Bean
    public ChatLanguageModel geminiModel(
            @Value("${gemini.api-key}") String apiKey) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-pro")
                .temperature(0.7)
                .maxOutputTokens(2048)
                .build();
    }

    @Bean
    public StreamingChatLanguageModel geminiStreamingModel(
            @Value("${gemini.api-key}") String apiKey) {
        return GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-pro")
                .temperature(0.7)
                .build();
    }
}
```

### AI Agent Definitions

```java
// ai/RoleplayAgent.java
@AiService
public interface RoleplayAgent {

    @SystemMessage(fromResource = "prompts/roleplay-system.txt")
    @UserMessage("{{userMessage}}")
    TokenStream chat(
        @MemoryId String sessionId,
        @V("userMessage") String userMessage
    );
}

// ai/HonorificsAgent.java
@AiService
public interface HonorificsAgent {

    @SystemMessage(fromResource = "prompts/honorifics-analyze.txt")
    HonorificsAnalysis analyze(@UserMessage String text);

    @SystemMessage(fromResource = "prompts/honorifics-transform.txt")
    String transform(
        @UserMessage String text,
        @V("targetLevel") String targetLevel
    );
}

// ai/SummarizerAgent.java
@AiService
public interface SummarizerAgent {

    @SystemMessage(fromResource = "prompts/summarizer.txt")
    FlashcardGenerationResult summarize(@UserMessage String articleText);
}

// ai/RecommendationAgent.java
@AiService
public interface RecommendationAgent {

    @SystemMessage(fromResource = "prompts/recommendation.txt")
    Recommendation recommend(
        @UserMessage String userProgressJson
    );
}
```

### Prompt Template Examples

```
# prompts/roleplay-system.txt
You are {{personaName}}, a {{personaRole}} at {{company}} in South Korea.
You only speak Korean. You are in a workplace meeting scenario.

Current mission: {{missionTitle}}
Difficulty: {{difficulty}}

Rules:
1. Stay in character at all times
2. Use {{politenessLevel}} speech level
3. Use IT/tech vocabulary relevant to the scenario
4. If the user makes grammar mistakes, continue naturally but note them internally
5. Provide subtle corrections by modeling correct usage in your responses
6. Keep responses concise (2-4 sentences)
7. Gradually increase complexity as the conversation progresses
```

```
# prompts/honorifics-analyze.txt
You are a Korean business language expert. Analyze the following text for:
1. Current politeness level (banmal/heyohaet-che/hasipsio-che)
2. Grammar errors specific to formal Korean writing
3. Inappropriate casual vocabulary for workplace
4. Wrong particle usage

Return a JSON with corrections array. Each correction must have:
- original: the problematic text
- corrected: the formal replacement
- type: particle | honorific | vocabulary | grammar | verb_ending | pronoun
- explanation: brief explanation in Vietnamese
```

---

## 2.4 FastAPI NLP Microservice

```
kapor-nlp/
├── main.py                  # FastAPI app
├── routers/
│   ├── tokenizer.py         # /tokenize endpoint
│   └── health.py            # /health
├── services/
│   ├── mecab_service.py     # Mecab-ko wrapper
│   └── article_parser.py    # HTML/text extraction
├── requirements.txt
└── Dockerfile
```

### API Endpoints

```python
# POST /tokenize
# Request: { "text": "서버 배포가 완료되었습니다" }
# Response:
{
  "tokens": [
    { "surface": "서버", "stem": "서버", "pos": "NNG", "is_content_word": true },
    { "surface": "배포", "stem": "배포", "pos": "NNG", "is_content_word": true },
    { "surface": "가", "stem": "가", "pos": "JKS", "is_content_word": false },
    { "surface": "완료", "stem": "완료", "pos": "NNG", "is_content_word": true },
    { "surface": "되었습니다", "stem": "되다", "pos": "VV+EP+EF", "is_content_word": true }
  ]
}

# POST /parse-article
# Request: { "url": "https://d2.naver.com/..." } or { "text": "..." }
# Response:
{
  "title": "...",
  "content": "...",
  "extracted_terms": [
    { "korean": "배포", "frequency": 12, "pos": "NNG" },
    { "korean": "서버", "frequency": 8, "pos": "NNG" }
  ]
}
```

---

## 2.5 Docker Compose

```yaml
# docker-compose.yml
version: '3.8'

services:
  # ---- Core Application ----
  kapor-api:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      MONGODB_URI: mongodb://mongo:27017/kapor
      REDIS_HOST: redis
      MINIO_ENDPOINT: http://minio:9000
      GEMINI_API_KEY: ${GEMINI_API_KEY}
      GCP_CREDENTIALS: ${GCP_CREDENTIALS}
      JWT_SECRET: ${JWT_SECRET}
      NLP_SERVICE_URL: http://kapor-nlp:8001
    depends_on:
      - mongo
      - redis
      - minio
      - kapor-nlp
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 2G

  # ---- NLP Microservice ----
  kapor-nlp:
    build: ./kapor-nlp
    ports:
      - "8001:8001"
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 512M

  # ---- Database ----
  mongo:
    image: mongo:7
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db
    command: --wiredTigerCacheSizeGB 2
    restart: unless-stopped

  # ---- Cache ----
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes --maxmemory 512mb
    restart: unless-stopped

  # ---- Object Storage ----
  minio:
    image: minio/minio:latest
    ports:
      - "9000:9000"
      - "9001:9001"  # Console
    volumes:
      - minio_data:/data
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    command: server /data --console-address ":9001"
    restart: unless-stopped

  # ---- Monitoring ----
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    volumes:
      - grafana_data:/var/lib/grafana
    restart: unless-stopped

volumes:
  mongo_data:
  redis_data:
  minio_data:
  grafana_data:
```

---

# PART 3: DATABASE (MongoDB 7)

## 3.1 Collections Overview

| Collection | Mô tả | Screen | Quan hệ |
|-----------|-------|--------|---------|
| `users` | Thông tin user, streak, settings | All | Root |
| `daily_activities` | Log hoạt động hàng ngày | S1 (DA-001, DA-002) | → users |
| `learning_progress` | Tiến độ per topic | S1 (DA-002), S2 | → users, topics |
| `topics` | Chủ đề IT (Frontend, Backend, ...) | S2 (DV-001, DV-002) | Contains skill nodes |
| `lessons` | Nội dung bài học | S2 (DV-003) | → topics |
| `videos` | Video + subtitles + quiz | S3 (VP-001→005) | Standalone |
| `dictionary` | Từ điển IT Hàn-Việt | S3 (VP-004), S4 | Standalone |
| `decks` | Nhóm flashcards | S4 (MB-001) | → users |
| `flashcards` | Flashcard + FSRS data | S4 (MB-001→004) | → users, decks |
| `scenarios` | Kịch bản roleplay | S5 (TT-001) | Standalone |
| `roleplay_sessions` | Phiên hội thoại AI | S5 (TT-001→004) | → users, scenarios |
| `pronunciation_exercises` | Bài tập phát âm | S6 (PS-001) | Standalone |
| `pronunciation_attempts` | Kết quả phát âm | S6 (PS-003, PS-004) | → users, exercises |

## 3.2 Detailed Collection Schemas

### users

```javascript
{
  _id: ObjectId("..."),
  email: "user@example.com",
  passwordHash: "$2a$10$...",        // null if OAuth
  provider: "google",                 // google | apple | kakao | email
  providerId: "google_uid_123",

  profile: {
    displayName: "Nguyễn Văn A",
    avatarUrl: "/storage/avatars/user123.jpg",
    nativeLanguage: "vi",             // vi | en
    koreanLevel: "beginner",          // beginner | intermediate | advanced
    joinedAt: ISODate("2026-01-15")
  },

  streak: {
    current: 15,
    longest: 30,
    lastActiveDate: ISODate("2026-06-26"),
    freezesRemaining: 2               // streak protect
  },

  settings: {
    ttsSpeed: 1.0,                    // 0.5 - 2.0
    dailyGoalMinutes: 15,
    dailyGoalCards: 20,
    theme: "dark",                    // dark | light | system
    notificationsEnabled: true,
    reminderTime: "09:00"
  },

  stats: {
    totalStudyMinutes: 1250,
    totalCardsReviewed: 3400,
    totalRoleplaySessions: 45,
    totalVideosWatched: 23
  },

  refreshToken: "rt_abc...",
  refreshTokenExpiry: ISODate("2026-07-26"),

  createdAt: ISODate("2026-01-15"),
  updatedAt: ISODate("2026-06-26")
}
```

**Indexes:**
```javascript
db.users.createIndex({ email: 1 }, { unique: true });
db.users.createIndex({ provider: 1, providerId: 1 }, { unique: true });
db.users.createIndex({ "streak.lastActiveDate": 1 });
```

---

### daily_activities

```javascript
{
  _id: ObjectId("..."),
  userId: ObjectId("user_123"),
  date: ISODate("2026-06-26T00:00:00Z"),  // Truncated to day

  activities: [
    {
      type: "vocabulary_review",       // vocabulary_review | lesson | video | roleplay | pronunciation | honorifics
      moduleId: "deck_abc",
      durationMinutes: 12,
      score: 85,
      itemsCompleted: 20,
      timestamp: ISODate("2026-06-26T08:30:00Z")
    },
    {
      type: "roleplay",
      moduleId: "session_xyz",
      durationMinutes: 8,
      score: 78,
      itemsCompleted: 1,
      timestamp: ISODate("2026-06-26T09:15:00Z")
    }
  ],

  summary: {
    totalMinutes: 20,
    totalItems: 21,
    speaking: 78,                      // Aggregated scores
    vocabulary: 85,
    listening: 0,
    roleplayScore: 78
  }
}
```

**Indexes:**
```javascript
db.daily_activities.createIndex({ userId: 1, date: -1 }, { unique: true });
// TTL index: auto-delete after 1 year
db.daily_activities.createIndex({ date: 1 }, { expireAfterSeconds: 31536000 });
```

---

### learning_progress

```javascript
{
  _id: ObjectId("..."),
  userId: ObjectId("user_123"),
  topicId: ObjectId("topic_frontend_css"),
  domain: "frontend",

  completionPercent: 65,
  completedLessons: [
    ObjectId("lesson_1"), ObjectId("lesson_2"), ObjectId("lesson_3")
  ],
  totalLessons: 5,

  scores: {
    vocabulary: 85,
    quizAverage: 78,
    lastQuizScore: 82
  },

  lastAccessedAt: ISODate("2026-06-25"),
  startedAt: ISODate("2026-06-01"),
  completedAt: null                    // null if not completed
}
```

**Indexes:**
```javascript
db.learning_progress.createIndex({ userId: 1, domain: 1 });
db.learning_progress.createIndex({ userId: 1, topicId: 1 }, { unique: true });
db.learning_progress.createIndex({ userId: 1, lastAccessedAt: -1 });
```

---

### topics

```javascript
{
  _id: ObjectId("topic_frontend_css"),
  title: "CSS Grid & Flexbox 용어",
  titleVi: "Thuật ngữ CSS Grid & Flexbox",
  domain: "frontend",                 // frontend | backend | devops | agile
  order: 3,                           // Sort order in skill tree
  difficulty: "intermediate",

  description: "Master CSS layout terminology in Korean...",
  iconUrl: "/storage/icons/css-grid.png",
  estimatedMinutes: 45,

  prerequisites: [
    ObjectId("topic_frontend_html"),
    ObjectId("topic_frontend_css_basics")
  ],

  skillNode: {
    x: 200,                           // Position for visual skill tree
    y: 300,
    connections: [                     // Lines to draw
      ObjectId("topic_frontend_html")
    ]
  },

  lessons: [
    ObjectId("lesson_1"),
    ObjectId("lesson_2"),
    ObjectId("lesson_3")
  ],
  lessonCount: 3,

  tags: ["CSS", "layout", "frontend"],
  createdAt: ISODate("2026-01-01"),
  updatedAt: ISODate("2026-06-01")
}
```

**Indexes:**
```javascript
db.topics.createIndex({ domain: 1, order: 1 });
db.topics.createIndex({ tags: 1 });
```

---

### lessons

```javascript
{
  _id: ObjectId("lesson_1"),
  topicId: ObjectId("topic_frontend_css"),
  title: "Flexbox 방향 속성",
  titleVi: "Thuộc tính hướng Flexbox",
  order: 1,

  content: {
    introduction: "Flexbox에서 방향을 설정하는 속성들을 배워봅시다...",
    introductionVi: "Hãy học các thuộc tính thiết lập hướng trong Flexbox...",

    vocabulary: [
      {
        korean: "방향",
        vietnamese: "Hướng",
        pronunciation: "banghyang",
        itContext: "CSS flex-direction property",
        exampleSentence: "flex-direction으로 방향을 설정합니다.",
        codeSnippet: ".container { flex-direction: row; /* 방향: 행 */ }"
      },
      {
        korean: "정렬",
        vietnamese: "Căn chỉnh",
        pronunciation: "jeongnyeol",
        itContext: "CSS alignment (justify-content, align-items)",
        exampleSentence: "아이템을 정렬하는 방법을 알아봅시다.",
        codeSnippet: ".container { justify-content: center; /* 정렬: 가운데 */ }"
      }
    ],

    exercises: [
      {
        type: "multiple_choice",
        question: "'정렬'은 무슨 뜻입니까?",
        options: ["Alignment", "Direction", "Spacing", "Wrapping"],
        correctAnswer: 0,
        explanation: "정렬 = căn chỉnh/alignment"
      }
    ]
  },

  estimatedMinutes: 15,
  createdAt: ISODate("2026-01-01")
}
```

**Indexes:**
```javascript
db.lessons.createIndex({ topicId: 1, order: 1 });
```

---

### videos

```javascript
{
  _id: ObjectId("video_deview_2025"),
  title: "Naver DEVIEW 2025 - 서버리스 아키텍처",
  titleVi: "Naver DEVIEW 2025 - Kiến trúc Serverless",
  domain: "backend",
  difficulty: "advanced",

  youtubeVideoId: "dQw4w9WgXcQ",     // YouTube video ID (extracted from URL)
  youtubeUrl: "https://youtube.com/watch?v=dQw4w9WgXcQ",
  thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",  // Auto-generated
  durationSeconds: 1200,

  subtitles: {
    korean: [
      {
        start: 0.0,
        end: 3.5,
        text: "서버 배포가 완료되었습니다",
        tokens: [
          { surface: "서버", stem: "서버", pos: "NNG", isContentWord: true },
          { surface: "배포", stem: "배포", pos: "NNG", isContentWord: true },
          { surface: "가", stem: "가", pos: "JKS", isContentWord: false },
          { surface: "완료", stem: "완료", pos: "NNG", isContentWord: true },
          { surface: "되었습니다", stem: "되다", pos: "VV", isContentWord: true }
        ]
      },
      {
        start: 3.5,
        end: 7.2,
        text: "오늘은 서버리스 아키텍처에 대해 설명하겠습니다",
        tokens: [/* ... */]
      }
    ],
    vietnamese: [
      { start: 0.0, end: 3.5, text: "Việc triển khai server đã hoàn tất" },
      { start: 3.5, end: 7.2, text: "Hôm nay tôi sẽ giải thích về kiến trúc Serverless" }
    ]
  },

  quizMarkers: [
    {
      timestamp: 45.0,
      question: "'배포'는 어떤 뜻입니까?",
      questionVi: "'배포' nghĩa là gì?",
      options: ["Deployment", "Development", "Testing", "Monitoring"],
      correctAnswer: 0
    },
    {
      timestamp: 120.0,
      question: "'서버리스'의 정의를 설명하세요",
      questionVi: "Hãy giải thích định nghĩa của 'Serverless'",
      type: "open_ended"
    }
  ],

  tags: ["serverless", "architecture", "naver", "deview"],
  viewCount: 156,
  createdAt: ISODate("2026-03-15")
}
```

**Indexes:**
```javascript
db.videos.createIndex({ domain: 1, difficulty: 1 });
db.videos.createIndex({ tags: 1 });
db.videos.createIndex({ createdAt: -1 });
```

---

### dictionary

```javascript
{
  _id: ObjectId("..."),
  korean: "배포",
  stem: "배포",
  pronunciation: "baepo",
  hanja: "配布",

  meanings: [
    {
      vietnamese: "Triển khai, phát hành",
      english: "Deployment, Distribution",
      context: "general"
    },
    {
      vietnamese: "Deploy (triển khai phần mềm)",
      english: "Software Deployment",
      context: "IT",
      domain: ["devops", "backend", "frontend"]
    }
  ],

  examples: [
    {
      korean: "새 버전을 배포했습니다.",
      vietnamese: "Đã triển khai phiên bản mới.",
      context: "IT workplace"
    },
    {
      korean: "자동 배포 파이프라인을 구축했습니다.",
      vietnamese: "Đã xây dựng pipeline triển khai tự động.",
      context: "DevOps"
    }
  ],

  relatedWords: ["배포하다", "재배포", "배포 환경"],
  frequency: "high",                // how common in IT context

  // Vector embedding for RAG search
  embedding: [0.12, -0.34, 0.56, ...],  // 768-dim vector

  createdAt: ISODate("2026-01-01")
}
```

**Indexes:**
```javascript
db.dictionary.createIndex({ korean: 1 }, { unique: true });
db.dictionary.createIndex({ stem: 1 });
db.dictionary.createIndex({ "meanings.domain": 1 });
// Vector search index (MongoDB Atlas Search or self-hosted equivalent)
// Created via Atlas UI or mongosh:
db.dictionary.createSearchIndex({
  name: "vector_index",
  type: "vectorSearch",
  definition: {
    fields: [{
      type: "vector",
      numDimensions: 768,
      path: "embedding",
      similarity: "cosine"
    }]
  }
});
```

---

### decks

```javascript
{
  _id: ObjectId("deck_abc"),
  userId: ObjectId("user_123"),
  name: "Frontend Deployment 용어",
  description: "Từ vựng triển khai Frontend",
  domain: "frontend",
  source: "manual",                   // manual | ai_generated | video_import
  sourceId: null,                     // video ID or article URL if auto-generated

  cardCount: 25,
  dueCount: 5,                       // Cards due for review (cached, updated periodically)
  newCount: 3,                       // Unreviewed cards

  coverColor: "#4A9EFF",             // Gradient color for deck card UI
  iconEmoji: "🚀",

  createdAt: ISODate("2026-06-01"),
  updatedAt: ISODate("2026-06-26")
}
```

**Indexes:**
```javascript
db.decks.createIndex({ userId: 1 });
db.decks.createIndex({ userId: 1, domain: 1 });
```

---

### flashcards

```javascript
{
  _id: ObjectId("card_xyz"),
  userId: ObjectId("user_123"),
  deckId: ObjectId("deck_abc"),

  // Card content
  front: {
    korean: "오류",
    pronunciation: "oryu",
    audioUrl: "/storage/audio/tts/오류.mp3"
  },
  back: {
    vietnamese: "Lỗi (Error)",
    itContext: "Exception/Error in programming - used in try-catch blocks",
    exampleSentence: "오류가 발생하면 로그를 확인하세요.",
    exampleSentenceVi: "Nếu xảy ra lỗi, hãy kiểm tra log.",
    codeSnippet: {
      language: "javascript",
      code: "try {\n  await deploy();\n} catch (오류) {\n  console.error('배포 오류:', 오류.message);\n}",
      highlightWord: "오류"
    }
  },

  // FSRS Algorithm Data
  fsrs: {
    state: "review",                  // new | learning | review | relearning
    difficulty: 5.2,                  // 1-10
    stability: 12.5,                  // days
    retrievability: 0.87,             // 0-1 (probability of recall)
    reps: 5,                          // successful reviews
    lapses: 1,                        // times forgotten
    lastReview: ISODate("2026-06-24T10:30:00Z"),
    nextReview: ISODate("2026-06-28T10:30:00Z"),
    lastRating: "good",
    elapsedDays: 2,
    scheduledDays: 4
  },

  tags: ["error-handling", "try-catch"],
  createdAt: ISODate("2026-06-01"),
  updatedAt: ISODate("2026-06-24")
}
```

**Indexes:**
```javascript
db.flashcards.createIndex({ userId: 1, deckId: 1 });
// Critical: FSRS due cards query
db.flashcards.createIndex({ userId: 1, "fsrs.nextReview": 1, "fsrs.state": 1 });
db.flashcards.createIndex({ "front.korean": 1 });
db.flashcards.createIndex({ tags: 1 });
```

---

### scenarios

```javascript
{
  _id: ObjectId("scenario_bug_report"),
  title: "Report a Critical Production Bug",
  titleVi: "Báo cáo lỗi nghiêm trọng trên Production",
  domain: "backend",
  difficulty: "intermediate",

  persona: {
    name: "김민수",
    nameRomanized: "Kim Min-su",
    role: "Tech Lead",
    company: "Naver",
    avatarUrl: "/storage/avatars/persona_kim.png",
    speechStyle: "hasipsio",           // Expected politeness level
    personality: "strict but fair, values clear communication"
  },

  mission: {
    title: "서버 장애 보고 (Server Incident Report)",
    objectives: [
      "Greet the Tech Lead formally",
      "Describe the bug clearly using Korean IT terminology",
      "Explain the impact on users",
      "Propose a fix or mitigation plan"
    ],
    objectivesVi: [
      "Chào Tech Lead bằng kính ngữ",
      "Mô tả bug rõ ràng bằng thuật ngữ IT tiếng Hàn",
      "Giải thích ảnh hưởng đến người dùng",
      "Đề xuất cách sửa hoặc giảm thiểu"
    ],
    contextPrompt: "The production server experienced a memory leak causing 30% of API requests to fail. The user needs to report this to the Tech Lead.",
    requiredVocabulary: ["장애", "서버", "메모리 누수", "API", "오류율", "모니터링"]
  },

  evaluationCriteria: {
    grammarWeight: 0.3,
    vocabularyWeight: 0.3,
    politenessWeight: 0.25,
    taskCompletionWeight: 0.15
  },

  order: 5,
  isActive: true,
  createdAt: ISODate("2026-01-01")
}
```

**Indexes:**
```javascript
db.scenarios.createIndex({ domain: 1, difficulty: 1 });
db.scenarios.createIndex({ isActive: 1, order: 1 });
```

---

### roleplay_sessions

```javascript
{
  _id: ObjectId("session_xyz"),
  userId: ObjectId("user_123"),
  scenarioId: ObjectId("scenario_bug_report"),

  status: "completed",                // active | completed | abandoned

  messages: [
    {
      _id: ObjectId("msg_1"),
      role: "ai",
      content: "안녕하세요. 무슨 일이십니까?",
      audioUrl: "/storage/audio/roleplay/msg_1.mp3",
      timestamp: ISODate("2026-06-26T08:00:00Z")
    },
    {
      _id: ObjectId("msg_2"),
      role: "user",
      content: "김민수 팀장님, 서버에 장애가 발생했습니다.",
      audioUrl: "/storage/audio/roleplay/msg_2_user.m4a",
      evaluation: {
        grammar: 90,
        vocabulary: 85,
        politeness: 95,
        corrections: [
          {
            original: "장애가 발생했습니다",
            suggestion: "장애가 발생하였습니다",
            type: "formality",
            note: "하였습니다 is slightly more formal than 했습니다"
          }
        ]
      },
      timestamp: ISODate("2026-06-26T08:00:30Z")
    }
  ],

  hintsUsed: 1,

  finalEvaluation: {
    overallScore: 82,
    grammar: 85,
    vocabulary: 80,
    politeness: 90,
    taskCompletion: 75,
    feedback: "전반적으로 잘하셨습니다. 기술 용어 사용이 적절했습니다.",
    feedbackVi: "Nhìn chung bạn làm tốt. Sử dụng thuật ngữ kỹ thuật phù hợp.",
    improvementAreas: ["Use more specific error descriptions", "Include timeline of events"]
  },

  startedAt: ISODate("2026-06-26T08:00:00Z"),
  endedAt: ISODate("2026-06-26T08:12:00Z"),
  durationMinutes: 12
}
```

**Indexes:**
```javascript
db.roleplay_sessions.createIndex({ userId: 1, status: 1 });
db.roleplay_sessions.createIndex({ userId: 1, startedAt: -1 });
db.roleplay_sessions.createIndex({ scenarioId: 1 });
```

---

### pronunciation_exercises

```javascript
{
  _id: ObjectId("ex_server_deploy"),
  title: "서버 배포 관련 문장",
  titleVi: "Câu liên quan đến triển khai server",
  domain: "devops",
  difficulty: "intermediate",

  sentences: [
    {
      text: "서버 배포가 완료되었습니다",
      audioUrl: "/storage/audio/reference/서버배포.mp3",
      waveformData: [0.12, 0.45, 0.78, ...],  // Pre-computed
      durationMs: 2500,
      phonemes: ["seo", "beo", "bae", "po", "ga", "wan", "ryo", "doe", "eot", "seum", "ni", "da"]
    }
  ],

  order: 3,
  createdAt: ISODate("2026-01-01")
}
```

---

### pronunciation_attempts

```javascript
{
  _id: ObjectId("attempt_abc"),
  userId: ObjectId("user_123"),
  exerciseId: ObjectId("ex_server_deploy"),
  sentenceIndex: 0,

  audioUrl: "/storage/audio/user/attempt_abc.m4a",
  userWaveformData: [0.10, 0.38, 0.65, ...],

  scores: {
    accuracy: 82,
    fluency: 75,
    prosody: 68,
    overall: 75
  },

  transcription: [
    { text: "서버", accuracy: "correct" },
    { text: "배포가", accuracy: "minor_error", phonemeDetail: "배 → [pɛ] expected [bɛ]" },
    { text: "완료되었습니다", accuracy: "correct" }
  ],

  attemptedAt: ISODate("2026-06-26T09:00:00Z")
}
```

**Indexes:**
```javascript
db.pronunciation_attempts.createIndex({ userId: 1, exerciseId: 1 });
db.pronunciation_attempts.createIndex({ userId: 1, attemptedAt: -1 });
```

---

## 3.3 Aggregation Pipelines (Analytics)

### Weekly Progress Aggregation (DA-002)

```javascript
// GET /api/analytics/progress?period=weekly
db.daily_activities.aggregate([
  {
    $match: {
      userId: ObjectId("user_123"),
      date: {
        $gte: ISODate("2026-06-20"),    // Last 7 days
        $lte: ISODate("2026-06-26")
      }
    }
  },
  {
    $group: {
      _id: null,
      avgSpeaking: { $avg: "$summary.speaking" },
      avgVocabulary: { $avg: "$summary.vocabulary" },
      avgListening: { $avg: "$summary.listening" },
      avgRoleplay: { $avg: "$summary.roleplayScore" },
      totalMinutes: { $sum: "$summary.totalMinutes" },
      activeDays: { $sum: 1 }
    }
  }
]);
```

### Streak Calculation

```javascript
// Streak is calculated by checking consecutive days backward from today
db.daily_activities.aggregate([
  { $match: { userId: ObjectId("user_123") } },
  { $sort: { date: -1 } },
  { $limit: 365 },
  {
    $group: {
      _id: null,
      dates: { $push: "$date" }
    }
  }
]);
// Then calculate consecutive days in application logic (StreakService.java)
```

### Due Cards Query (FSRS)

```javascript
// GET /api/review/due?limit=20
db.flashcards.find({
  userId: ObjectId("user_123"),
  $or: [
    { "fsrs.state": "new" },
    {
      "fsrs.nextReview": { $lte: ISODate("2026-06-26T23:59:59Z") },
      "fsrs.state": { $in: ["learning", "review", "relearning"] }
    }
  ]
})
.sort({ "fsrs.nextReview": 1 })
.limit(20);
```

---

## 3.4 Redis Cache Strategy

| Key Pattern | TTL | Data | Purpose |
|------------|-----|------|---------|
| `user:{id}:dashboard` | 5 min | Dashboard aggregate | Avoid recalculating on every app open |
| `user:{id}:streak` | 1 hour | Streak data | Streak rarely changes intraday |
| `user:{id}:recommendation` | 30 min | AI recommendation | Expensive AI call, cache result |
| `tts:{hash}` | 24 hours | Audio URL | Cache TTS-generated audio |
| `tokenize:{hash}` | 7 days | Tokenization result | Korean text tokenization rarely changes |
| `dict:{word}` | 7 days | Dictionary entry | Frequently looked up words |
| `jwt:blacklist:{jti}` | token TTL | — | Revoked JWT tokens |
| `rate:ai:{userId}` | 1 min | Counter | Rate limit AI calls (10/min) |

---

# PART 4: DEVELOPMENT PHASES

## Phase 1: Foundation (Week 1-4)

| Task | Frontend | Backend | Database |
|------|----------|---------|----------|
| Project setup | Expo init, navigation, theme, i18n | Spring Boot init, Docker Compose | MongoDB setup, seed data |
| Authentication | Google login screen | JWT + OAuth2 (Google only) | users collection |
| Dashboard (S1) | DA-001→004 components | Analytics APIs | daily_activities, learning_progress |
| DevVocab (S2) | DV-001→003 components | Topics & Lessons APIs | topics, lessons collections |
| i18n setup | expo-localization + i18next, vi/en | — | — |

## Phase 2: Core Learning (Week 5-8)

| Task | Frontend | Backend | Database |
|------|----------|---------|----------|
| MemByte (S4) | MB-001→004 (flip cards, SRS) | Cards CRUD, FSRS algorithm | decks, flashcards |
| Video Player (S3) | VP-001→005 (YouTube embed + subtitles) | Video metadata APIs, NLP tokenization | videos, dictionary |
| Smart Summarizer (DV-004) | Bottom sheet UI | AI article→flashcards | LangChain4j integration |

## Phase 3: AI Features (Week 9-12)

| Task | Frontend | Backend | Database |
|------|----------|---------|----------|
| TechTalk AI (S5) | TT-001→004 (chat, voice) | Roleplay SSE streaming | scenarios, roleplay_sessions |
| Honorifics (S7) | HG-001→004 (diff viewer) | AI analysis & transform | Stateless (AI only) |
| Speech integration | TTS/STT service layer | GCP Speech proxy | — |

## Phase 4: Advanced, Admin Panel & Polish (Week 13-18)

| Task | Frontend | Backend | Database |
|------|----------|---------|----------|
| Pronunciation Lab (S6) | PS-001→004 (waveforms) | Audio evaluation API | pronunciation_* collections |
| AI Recommendation | Smart recommendation card | Recommendation AI agent | Aggregation pipelines |
| **Admin Panel** | **Next.js 15 web app** | **Admin APIs + RBAC** | **admins collection** |
| Polish | Animations, error states, loading | Rate limiting, monitoring | Index optimization |
| Testing | Component tests, E2E (Maestro) | Integration tests, load testing | — |

---

# PART 5: ADMIN PANEL (Separate Web Application)

> [!IMPORTANT]
> Admin Panel là **web app riêng biệt**, triển khai trên subdomain `admin.kapor.app`, tách hoàn toàn khỏi mobile app. Chia sẻ cùng Spring Boot backend APIs nhưng sử dụng admin-specific endpoints với RBAC (Role-Based Access Control).

## 5.1 Tech Stack — Admin Panel

| Công nghệ | Vai trò | Lý do |
|-----------|---------|-------|
| **Next.js 15** (App Router) | Framework | SSR, file-based routing, React Server Components |
| **TypeScript** | Ngôn ngữ | Type safety |
| **Tailwind CSS v4** | Styling | Rapid admin UI development |
| **shadcn/ui** | Component Library | High-quality, accessible components (tables, forms, dialogs) |
| **TanStack Table** | Data Tables | Advanced filtering, sorting, pagination cho content management |
| **Recharts** | Charts | Learning analytics visualization |
| **React Hook Form + Zod** | Form Handling | Type-safe forms cho content CRUD |
| **NextAuth.js v5** | Admin Auth | Google OAuth + role checking (chỉ admin mới vào được) |

## 5.2 Project Structure

```
kapor-admin/
├── app/
│   ├── layout.tsx                    # Root layout (sidebar + header)
│   ├── page.tsx                      # Dashboard overview
│   ├── login/
│   │   └── page.tsx                  # Admin login (Google OAuth)
│   │
│   ├── users/                        # User Management
│   │   ├── page.tsx                  # User list table
│   │   └── [userId]/
│   │       └── page.tsx              # User detail + activity log
│   │
│   ├── content/                      # Content Management
│   │   ├── topics/
│   │   │   ├── page.tsx              # Topics CRUD table
│   │   │   ├── new/page.tsx          # Create topic
│   │   │   └── [topicId]/
│   │   │       └── page.tsx          # Edit topic + manage lessons
│   │   │
│   │   ├── lessons/
│   │   │   ├── page.tsx              # Lessons CRUD table
│   │   │   ├── new/page.tsx          # Create lesson
│   │   │   └── [lessonId]/
│   │   │       └── page.tsx          # Edit lesson (vocabulary, exercises)
│   │   │
│   │   ├── videos/
│   │   │   ├── page.tsx              # Videos CRUD table
│   │   │   ├── new/page.tsx          # Add video (YouTube URL + subtitles)
│   │   │   └── [videoId]/
│   │   │       └── page.tsx          # Edit video, upload/edit subtitles
│   │   │
│   │   ├── scenarios/
│   │   │   ├── page.tsx              # Roleplay scenarios CRUD
│   │   │   ├── new/page.tsx          # Create scenario + persona
│   │   │   └── [scenarioId]/
│   │   │       └── page.tsx          # Edit scenario
│   │   │
│   │   ├── dictionary/
│   │   │   ├── page.tsx              # Dictionary entries table
│   │   │   ├── new/page.tsx          # Add dictionary entry
│   │   │   ├── import/page.tsx       # Bulk import (CSV/JSON)
│   │   │   └── [entryId]/
│   │   │       └── page.tsx          # Edit entry
│   │   │
│   │   └── pronunciation/
│   │       ├── page.tsx              # Pronunciation exercises CRUD
│   │       ├── new/page.tsx          # Create exercise + upload reference audio
│   │       └── [exerciseId]/
│   │           └── page.tsx          # Edit exercise
│   │
│   ├── analytics/                    # Analytics Dashboard
│   │   ├── page.tsx                  # Overview (DAU, MAU, retention)
│   │   ├── users/page.tsx            # User engagement analytics
│   │   ├── content/page.tsx          # Content performance
│   │   └── ai/page.tsx               # AI usage & costs
│   │
│   └── settings/                     # System Settings
│       ├── page.tsx                  # General settings
│       ├── admins/page.tsx           # Admin user management
│       └── ai/page.tsx               # AI prompt management
│
├── components/
│   ├── layout/
│   │   ├── Sidebar.tsx               # Navigation sidebar
│   │   ├── Header.tsx                # Top bar + admin profile
│   │   └── Breadcrumbs.tsx
│   ├── tables/
│   │   ├── DataTable.tsx             # Reusable TanStack Table wrapper
│   │   ├── columns/                  # Column definitions per entity
│   │   │   ├── userColumns.tsx
│   │   │   ├── topicColumns.tsx
│   │   │   ├── videoColumns.tsx
│   │   │   └── ...
│   │   └── TableToolbar.tsx          # Search, filters, bulk actions
│   ├── forms/
│   │   ├── TopicForm.tsx
│   │   ├── LessonForm.tsx
│   │   ├── VideoForm.tsx             # YouTube URL input + subtitle editor
│   │   ├── ScenarioForm.tsx
│   │   ├── DictionaryEntryForm.tsx
│   │   └── PronunciationExerciseForm.tsx
│   ├── editors/
│   │   ├── SubtitleEditor.tsx        # Timeline-based subtitle editor
│   │   ├── VocabularyEditor.tsx      # Add/edit vocabulary with code snippets
│   │   ├── QuizEditor.tsx            # Create quiz questions
│   │   └── PromptEditor.tsx          # Edit AI prompt templates
│   └── charts/
│       ├── UserGrowthChart.tsx
│       ├── EngagementChart.tsx
│       ├── ContentPerformanceChart.tsx
│       └── AIUsageCostChart.tsx
│
├── lib/
│   ├── api.ts                        # Admin API client
│   ├── auth.ts                       # NextAuth config
│   └── utils.ts
│
├── next.config.ts
├── tailwind.config.ts
└── package.json
```

## 5.3 Admin Panel Pages — Detail Specification

### 5.3.1 Dashboard Overview (`/`)

```
┌─────────────────────────────────────────────────────────────┐
│  📊 Admin Dashboard                                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐          │
│  │ Total   │ │ DAU     │ │ MAU     │ │ Content │          │
│  │ Users   │ │ Today   │ │ This    │ │ Items   │          │
│  │ 1,234   │ │ 156     │ │ Month   │ │ 523     │          │
│  │ +12%    │ │ +5%     │ │ 890     │ │         │          │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘          │
│                                                             │
│  ┌──────────────────────┐  ┌──────────────────────┐        │
│  │ User Growth          │  │ AI API Usage & Cost  │        │
│  │ (Line Chart 30 days) │  │ (Bar Chart)          │        │
│  └──────────────────────┘  └──────────────────────┘        │
│                                                             │
│  ┌──────────────────────┐  ┌──────────────────────┐        │
│  │ Popular Content      │  │ Recent Activity      │        │
│  │ (Top 10 lessons)     │  │ (Activity feed)      │        │
│  └──────────────────────┘  └──────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

### 5.3.2 Content Management — Videos (`/content/videos`)

**Video List Page:**
- Data table: Title, YouTube ID, Domain, Difficulty, Subtitle status, Quiz count, View count
- Filters: Domain, Difficulty, Has subtitles (yes/no)
- Bulk actions: Delete, Change domain

**Add/Edit Video Page (`/content/videos/new`):**
```
┌─────────────────────────────────────────────────────────────┐
│  🎬 Add New Video                                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  YouTube URL: [https://youtube.com/watch?v=________]        │
│  → Auto-fetch: title, thumbnail, duration                   │
│                                                             │
│  Title (VI): [_____________________________]                │
│  Domain:     [Frontend ▾]    Difficulty: [Intermediate ▾]  │
│  Tags:       [serverless] [architecture] [+]                │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  📝 Subtitle Editor                                  │    │
│  │                                                     │    │
│  │  [YouTube Player Preview]                           │    │
│  │                                                     │    │
│  │  Timeline: ═══●════════════════════════════         │    │
│  │                                                     │    │
│  │  00:00 - 00:03  KR: 서버 배포가 완료되었습니다     │    │
│  │                 VI: Triển khai server đã hoàn tất    │    │
│  │  00:03 - 00:07  KR: [_________________________]     │    │
│  │                 VI: [_________________________]     │    │
│  │                                                     │    │
│  │  [+ Add Line]  [Import SRT]  [Auto-tokenize All]   │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  📋 Quiz Markers                                     │    │
│  │  Timestamp: [00:45]  Type: [Multiple Choice ▾]      │    │
│  │  Question (KR): [__________________________]        │    │
│  │  Question (VI): [__________________________]        │    │
│  │  Options: [A] [B] [C] [D]  Correct: [A]            │    │
│  │  [+ Add Quiz Marker]                                │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  [Cancel]                              [Save as Draft] [Publish] │
└─────────────────────────────────────────────────────────────┘
```

### 5.3.3 Content Management — Lessons (`/content/lessons`)

**Lesson Editor Features:**
- Rich text editor cho introduction (Korean + Vietnamese)
- Vocabulary table editor: Korean term, Vietnamese, pronunciation, IT context, code snippet
- Code snippet editor với syntax highlighting (Monaco Editor)
- Exercise builder: Multiple choice, fill-in-blank, matching
- Preview mode: Xem lesson trông như thế nào trên mobile

### 5.3.4 Content Management — Roleplay Scenarios (`/content/scenarios`)

**Scenario Editor Features:**
- Persona builder: Name, role, company, avatar upload, speech style, personality
- Mission editor: Title, objectives (KR + VI), context prompt, required vocabulary
- Evaluation criteria sliders: Grammar/Vocabulary/Politeness/TaskCompletion weights
- Prompt template editor: System prompt cho LangChain4j roleplay agent
- Test mode: Chat trực tiếp với AI persona để test scenario

### 5.3.5 Subtitle Editor (Key Feature)

```typescript
interface SubtitleEditorProps {
  youtubeVideoId: string;
  subtitles: {
    korean: SubtitleLine[];
    vietnamese: SubtitleLine[];
  };
  onSave: (subtitles: SubtitleData) => void;
}

// Features:
// 1. YouTube player preview (sync playback with subtitle timeline)
// 2. Add/edit/delete subtitle lines with timestamp
// 3. Import from SRT/VTT files
// 4. "Auto-tokenize" button → calls NLP API to tokenize all Korean lines
// 5. Manual token correction (edit tokenization results)
// 6. Keyboard shortcuts: Space (play/pause), Enter (add line at current time)
```

### 5.3.6 Analytics Dashboard (`/analytics`)

**Metrics tracked:**

| Category | Metrics |
|----------|--------|
| **Users** | DAU, MAU, retention rate, churn rate, new registrations, avg session duration |
| **Engagement** | Lessons completed/day, cards reviewed/day, roleplay sessions/day, videos watched/day |
| **Content** | Most popular topics, lesson completion rates, video watch-through rates, most searched dictionary words |
| **AI Usage** | API calls/day (Gemini, GCP Speech), cost/day, avg response time, error rates |
| **Learning** | Avg SRS retention rate, avg pronunciation score improvement, avg politeness score |

## 5.4 Backend — Admin API Endpoints

> [!NOTE]
> Admin APIs share the same Spring Boot backend, protected by `ROLE_ADMIN` authorization. All admin endpoints are prefixed with `/api/admin/`.

### Admin Authentication & Authorization

```java
// Spring Security — Admin role check
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/**").authenticated()
            );
        return http.build();
    }
}
```

### Admin API Spec

| Method | Endpoint | Description |
|--------|----------|-------------|
| **Dashboard** | | |
| GET | `/api/admin/dashboard/stats` | KPI overview (users, DAU, MAU, content count) |
| GET | `/api/admin/dashboard/growth?period=30d` | User growth time series |
| GET | `/api/admin/dashboard/ai-usage?period=30d` | AI API usage & cost |
| **User Management** | | |
| GET | `/api/admin/users?page=1&search=...` | Paginated user list |
| GET | `/api/admin/users/{id}` | User detail + activity log |
| PUT | `/api/admin/users/{id}/role` | Change user role (user/admin) |
| DELETE | `/api/admin/users/{id}` | Deactivate user |
| **Topics** | | |
| GET | `/api/admin/topics` | All topics (with stats) |
| POST | `/api/admin/topics` | Create topic |
| PUT | `/api/admin/topics/{id}` | Update topic |
| DELETE | `/api/admin/topics/{id}` | Delete topic |
| PUT | `/api/admin/topics/reorder` | Reorder skill tree nodes |
| **Lessons** | | |
| GET | `/api/admin/lessons?topicId=...` | Lessons by topic |
| POST | `/api/admin/lessons` | Create lesson |
| PUT | `/api/admin/lessons/{id}` | Update lesson (vocab, exercises) |
| DELETE | `/api/admin/lessons/{id}` | Delete lesson |
| **Videos** | | |
| GET | `/api/admin/videos` | All videos (with stats) |
| POST | `/api/admin/videos` | Add video (YouTube URL + metadata) |
| PUT | `/api/admin/videos/{id}` | Update video metadata |
| PUT | `/api/admin/videos/{id}/subtitles` | Upload/update subtitles |
| POST | `/api/admin/videos/{id}/tokenize` | Trigger NLP tokenization for all subtitle lines |
| DELETE | `/api/admin/videos/{id}` | Delete video |
| **Scenarios** | | |
| GET | `/api/admin/scenarios` | All roleplay scenarios |
| POST | `/api/admin/scenarios` | Create scenario |
| PUT | `/api/admin/scenarios/{id}` | Update scenario |
| DELETE | `/api/admin/scenarios/{id}` | Delete scenario |
| POST | `/api/admin/scenarios/{id}/test` | Test chat with scenario AI |
| **Dictionary** | | |
| GET | `/api/admin/dictionary?search=...&domain=...` | Search dictionary |
| POST | `/api/admin/dictionary` | Add entry |
| PUT | `/api/admin/dictionary/{id}` | Update entry |
| DELETE | `/api/admin/dictionary/{id}` | Delete entry |
| POST | `/api/admin/dictionary/import` | Bulk import (CSV/JSON) |
| **Pronunciation** | | |
| GET | `/api/admin/pronunciation` | All exercises |
| POST | `/api/admin/pronunciation` | Create exercise |
| PUT | `/api/admin/pronunciation/{id}` | Update exercise |
| POST | `/api/admin/pronunciation/{id}/audio` | Upload reference audio |
| DELETE | `/api/admin/pronunciation/{id}` | Delete exercise |
| **Settings** | | |
| GET | `/api/admin/settings/prompts` | Get all AI prompt templates |
| PUT | `/api/admin/settings/prompts/{name}` | Update prompt template |
| GET | `/api/admin/admins` | List admin users |
| POST | `/api/admin/admins/invite` | Invite new admin (email) |

## 5.5 MongoDB — Admin Collection

```javascript
// Collection: admins
{
  _id: ObjectId("..."),
  email: "admin@kapor.app",
  provider: "google",
  providerId: "google_uid_admin_001",
  displayName: "Admin User",
  avatarUrl: "...",
  role: "super_admin",          // super_admin | content_admin | analytics_viewer
  permissions: [
    "users.read", "users.write",
    "content.read", "content.write",
    "analytics.read",
    "settings.read", "settings.write",
    "admins.manage"
  ],
  lastLoginAt: ISODate("2026-06-26"),
  createdAt: ISODate("2026-01-01")
}
```

**Admin Roles:**

| Role | Permissions |
|------|------------|
| `super_admin` | Full access: users, content, analytics, settings, admin management |
| `content_admin` | Content CRUD (topics, lessons, videos, scenarios, dictionary, pronunciation). No user management or settings |
| `analytics_viewer` | Read-only access to analytics dashboard. No content editing |

## 5.6 Admin Panel Deployment

```yaml
# docker-compose.yml — thêm vào existing compose
  kapor-admin:
    build: ./kapor-admin
    ports:
      - "3001:3000"
    environment:
      NEXT_PUBLIC_API_URL: http://kapor-api:8080
      NEXTAUTH_URL: https://admin.kapor.app
      NEXTAUTH_SECRET: ${NEXTAUTH_SECRET}
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
    depends_on:
      - kapor-api
    restart: unless-stopped
```

**Nginx config:**
```nginx
# admin.kapor.app → kapor-admin container
server {
    server_name admin.kapor.app;
    location / {
        proxy_pass http://localhost:3001;
    }
}

# api.kapor.app → kapor-api container
server {
    server_name api.kapor.app;
    location / {
        proxy_pass http://localhost:8080;
    }
}
```

---

# PART 6: I18N (Internationalization)

## 6.1 Setup

```typescript
// lib/i18n/index.ts
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import * as Localization from 'expo-localization';
import vi from './vi.json';
import en from './en.json';

i18n.use(initReactI18next).init({
  resources: { vi: { translation: vi }, en: { translation: en } },
  lng: Localization.getLocales()[0]?.languageCode || 'vi',
  fallbackLng: 'vi',
  interpolation: { escapeValue: false },
});

export default i18n;
```

## 6.2 Translation File Structure

```json
// lib/i18n/vi.json
{
  "common": {
    "loading": "Đang tải...",
    "error": "Đã có lỗi xảy ra",
    "retry": "Thử lại",
    "save": "Lưu",
    "cancel": "Hủy",
    "delete": "Xóa",
    "search": "Tìm kiếm"
  },
  "auth": {
    "loginWith": "Đăng nhập bằng Google",
    "logout": "Đăng xuất",
    "welcome": "Chào mừng đến Kapor!"
  },
  "dashboard": {
    "title": "Tổng quan",
    "streak": "Chuỗi ngày học",
    "streakDays": "{{count}} ngày liên tiếp",
    "progress": "Tiến độ tuần này",
    "recommendation": "Gợi ý cho bạn",
    "quickNav": "Truy cập nhanh"
  },
  "devvocab": {
    "title": "Từ vựng IT",
    "allTopics": "Tất cả",
    "locked": "Chưa mở khóa",
    "lessons": "{{completed}}/{{total}} bài",
    "summarizer": "Tạo flashcard từ bài viết"
  },
  "video": {
    "title": "Video IT Hàn Quốc",
    "tapWord": "Chạm vào từ để tra nghĩa",
    "addToMembyte": "Thêm vào MemByte",
    "quizTime": "Câu hỏi kiểm tra!"
  },
  "membyte": {
    "title": "Thẻ ghi nhớ",
    "dueToday": "Cần ôn hôm nay",
    "newCards": "Thẻ mới",
    "flipToSee": "Chạm để lật thẻ",
    "again": "Lại",
    "hard": "Khó",
    "good": "Tốt",
    "easy": "Dễ"
  },
  "techtalk": {
    "title": "Hội thoại AI",
    "mission": "Nhiệm vụ",
    "hint": "Gợi ý",
    "typeMessage": "Nhập tin nhắn...",
    "holdToRecord": "Giữ để ghi âm"
  },
  "pronunciation": {
    "title": "Phòng luyện phát âm",
    "reference": "Mẫu phát âm",
    "yourAttempt": "Bản ghi của bạn",
    "accuracy": "Độ chính xác",
    "fluency": "Độ trôi chảy",
    "prosody": "Ngữ điệu"
  },
  "honorifics": {
    "title": "Kiểm tra kính ngữ",
    "inputPlaceholder": "Nhập văn bản tiếng Hàn cần kiểm tra...",
    "banmal": "Thân mật (⚠️ Nguy hiểm)",
    "heyohaet": "Lịch sự (Tiêu chuẩn)",
    "hasipsio": "Trang trọng (Doanh nghiệp)",
    "applyTransform": "Áp dụng chuyển đổi",
    "copyToClipboard": "Sao chép"
  },
  "settings": {
    "title": "Cài đặt",
    "language": "Ngôn ngữ",
    "ttsSpeed": "Tốc độ phát âm",
    "dailyGoal": "Mục tiêu hàng ngày",
    "theme": "Giao diện",
    "notifications": "Thông báo"
  }
}
```

```json
// lib/i18n/en.json (tương tự, bằng tiếng Anh)
{
  "common": {
    "loading": "Loading...",
    "error": "Something went wrong",
    "retry": "Retry"
  },
  "dashboard": {
    "title": "Overview",
    "streak": "Learning Streak",
    "streakDays": "{{count}} consecutive days"
  }
  // ... (tương tự cấu trúc vi.json)
}
```

## 6.3 Language Switcher Component

```typescript
// components/ui/LanguageSwitcher.tsx
interface LanguageSwitcherProps {
  // In Settings screen
}
// Visual: Segmented control [🇻🇳 Tiếng Việt | 🇬🇧 English]
// Persist: Lưu vào MMKV + update i18n.changeLanguage()
// Sync: Update user settings trên backend
```

> [!NOTE]
> **Nội dung học (lessons, vocabulary) luôn bằng cả Hàn + Việt** — không bị ảnh hưởng bởi language setting.
> Language setting chỉ thay đổi **UI labels** (buttons, titles, navigation, placeholders).

---

# Verification Plan

### Automated Tests
```bash
# Backend
./mvnw test                          # Unit + integration tests
./mvnw verify -P integration-test     # Full integration test suite

# Mobile Frontend
npx expo run:ios --configuration Debug
npm test                              # Jest component tests

# Admin Panel
cd kapor-admin && npm test            # Jest + React Testing Library
cd kapor-admin && npx playwright test  # E2E tests

# E2E Mobile
maestro test flows/                   # Maestro E2E test suite
```

### Manual Verification
- Kiểm tra tất cả 7 screens trên iOS Simulator + Android Emulator
- Test **cả 2 ngôn ngữ** (Việt/Anh) trên mọi screen
- Test YouTube video embed + subtitle sync trên cả iOS/Android
- Test AI roleplay conversations với các scenarios khác nhau
- Test pronunciation scoring với native Korean speakers
- Test Admin Panel: CRUD content, subtitle editor, analytics
- Load test: 100 concurrent users trên VPS staging
- Verify offline mode: airplane mode → MemByte review → online sync
