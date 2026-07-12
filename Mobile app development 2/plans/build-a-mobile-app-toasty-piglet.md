# Plan: Kapor — Phase 3 Gap Analysis & Remaining Features

## Context

All Phase 1 and Phase 2 features are implemented. The user uploaded `implementation_plan-3.md` (same spec, refined) and asked what remains unbuilt. This plan captures the delta between the spec and the current prototype so the next implementation sprint is clear.

---

## What Is Already Implemented ✅

### Mobile Screens (all 14 routes)
- `login`, `onboarding`
- `dashboard` — streak calendar modal, radar chart, recommendation, quick-nav
- `devvocab`, `devvocab-lesson` — skill tree, domain filter, SmartSummarizer FAB
- `membyte`, `membyte-review` — flip card, 4-rating SRS
- `techtalk-select`, `techtalk`, `techtalk-result` — scenario cards, chat, hint, voice toggle, score ring
- `honorifics` — politeness indicator, diff viewer, apply/copy
- `pronunciation-list`, `pronunciation` — exercise list, waveform, 3 score rings
- `video` — dual subtitles, word tokenizer, dictionary popup, quiz modal
- `profile` — stats, TTS speed, language toggle

### Admin Panel (all 11 AdminSection values)
- `dashboard` — KPI cards, user growth chart, DAU bar chart, popular content, recent activity feed
- `users` — table with 4 mock users
- `content-topics`, `content-lessons`, `content-videos`, `content-scenarios`, `content-dictionary`, `content-pronunciation` — CRUD tables + add forms, subtitle editor (basic grid), scenario test chat
- `analytics` — DAU line chart, lessons bar chart, AI cost area chart
- `settings-prompts`, `settings-admins` — prompt editor, admin invite, role permission matrix

---

## What Is NOT Yet Implemented ❌

### A. Admin Panel — Analytics Sub-pages
The spec describes **4 distinct analytics sections**, but currently there is only one combined page with 3 charts.

**Missing:**
- **User Engagement** — retention rate, churn rate, new registrations per day, avg session duration (new KPI cards + dedicated charts)
- **Content Performance** — top-10 most popular topics, lesson completion rates by domain, video watch-through rates, most searched dictionary words (table + bar chart)
- **AI Usage & Costs** — API calls/day per service (Gemini, GCP TTS, GCP STT), cost/day, avg response time, error rate (grouped bar + cost breakdown table)

### B. Admin Panel — User Detail Page
The spec defines a user drill-down (`/users/[userId]`):
- Full profile header (avatar, level, streak, join date, role badge)
- Learning activity log (date, activity type, duration, score)
- SRS card stats (total reviewed, retention rate, avg ease)
- Role change action (dropdown + Save)
- Deactivate user button

Currently: clicking a user row does nothing (no drill-down exists).

### C. Admin Panel — Lesson Rich Editor
The spec calls for a full lesson authoring tool. Currently only a basic vocabulary table exists.

**Missing:**
- Rich text introduction editor (KR + VI, markdown-style)
- **Exercise builder**: multiple choice, fill-in-blank, and matching exercise types with add/delete/reorder
- **Mobile preview panel**: renders lesson content as it would appear in the phone frame
- Monaco-style code snippet editor with syntax highlighting for vocabulary entries

### D. Admin Panel — Dictionary Bulk Import
The spec includes a dedicated import page for uploading CSV/JSON dictionary data.

**Missing:**
- Upload area (drag-and-drop CSV or JSON file)
- Column mapping step (map CSV columns → Korean, Vietnamese, pronunciation, domain fields)
- Preview table showing first 10 rows before import
- Import button with success/error summary

### E. Admin Panel — Video Table Filters & Bulk Actions
Currently the video table is a plain list. The spec requires:
- Filter bar: Domain (chip), Difficulty (chip), Subtitle status (with/without)
- Bulk select (checkboxes) + bulk actions: Delete selected, Change domain
- Row-level "Edit Subs" opens full subtitle editor; currently it is already partially built but lacks YouTube player preview sync

### F. Admin Panel — User Table Search & Actions
Currently the users table is static. The spec requires:
- Search input (by name/email)
- Role change per row (inline dropdown → Save)
- Deactivate user action (soft delete, shows greyed-out row)

---

## Recommended Implementation Order

Priority order based on spec emphasis and visual impact:

1. **Analytics sub-pages** (A) — expand current analytics section into 4 tabs: Overview / Users / Content / AI. Add new mock data and charts.
2. **User detail page** (B) — add `AdminSection = "users-detail"`, render on row click with activity log table and role change.
3. **User table search + actions** (F) — filter state + inline role dropdown, deactivate toggle.
4. **Lesson rich editor** (C) — add exercise builder (3 types) and mobile preview panel.
5. **Dictionary bulk import** (D) — add `AdminSection = "content-dictionary-import"`, upload UI + preview table.
6. **Video table filters + bulk actions** (E) — filter chip bar + checkbox bulk select.

---

## Files to Modify

- **`src/app/App.tsx`** — all changes land here:
  - Extend `type AdminSection` with new values: `"analytics-users" | "analytics-content" | "analytics-ai" | "users-detail" | "content-dictionary-import"`
  - Add mock data: user activity log, content performance metrics, AI usage breakdown
  - New render branches inside `AdminPanel` for each new section
  - Extend `users` section with search state + inline actions
  - Extend `content-lessons` section with exercise builder and preview panel
  - Extend `content-videos` section with filter bar + bulk select state
  - Extend `analytics` section into 4-tab layout

---

## Verification

After implementation:
- Navigate Admin Panel → Analytics → switch between 4 tabs, verify different charts and KPI cards render without errors
- Click a user row → verify user detail page opens with activity log
- Type in Users search input → verify table filters live
- Open Lessons editor → verify exercise builder renders all 3 exercise types, preview panel mirrors content
- Navigate Dictionary → Import → verify upload UI and preview table render
- Open Videos → verify filter chips narrow the list, checkboxes appear, bulk action dropdown activates when ≥1 selected
