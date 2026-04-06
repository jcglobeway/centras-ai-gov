# Google Stitch 프롬프트 가이드 — 통합 어드민 IA

> IA 최종안 전체 화면(22개) 완전 반영 버전  
> 화면 하나씩 순서대로 적용. 한 번에 여러 화면 붙여넣지 않도록 주의.

---

## 활용 순서

```
1단계  → STEP 1   전체 앱 뼈대 생성
2단계  → STEP 2   공통 레이아웃 & 네비게이션
3단계  → STEP 3   화면별 프롬프트 (3-1 ~ 3-22) 순서대로 적용
4단계  → STEP 4   다크 테마 마무리
5단계  → STEP 5   라이트 테마로 전환 (다크 완성 후 덮어씌우기)
```

---

## STEP 1. 전체 앱 뼈대

```
A professional B2B SaaS admin dashboard for managing an AI-powered RAG chatbot system.
Clean and technical feel — similar to Datadog or Linear. Dark mode by default.
Left sidebar navigation with 7 main menus:
  1. 대시보드 (Dashboard) — 3 sub-pages
  2. 지식베이스 관리 (Knowledge Base) — 4 sub-pages
  3. 챗봇 엔진 설정 (Chatbot Settings) — 3 sub-pages
  4. 품질 관리 (Quality Management) — 5 sub-pages
  5. 서비스 모니터링 (Service Monitoring) — 3 sub-pages
  6. 통계 및 보고서 (Reports) — 2 sub-pages
  7. 시스템 및 권한 (System & Permissions) — 2 sub-pages
Role-based access: Admin, Operator, QA, Customer — each role sees different menus and content.
```

---

## STEP 2. 공통 레이아웃 & 네비게이션

```
Fixed left sidebar, 240px wide, dark navy (#0f172a).
Sidebar shows main menu items. Clicking a main menu expands its sub-menu list inline.
Active sub-menu item: left 3px blue (#2563eb) border + slightly lighter background.

Top header bar, full width, 56px tall:
  Left: current page breadcrumb (e.g. "대시보드 / 통합 관제")
  Center: global filter bar — date range picker + customer selector dropdown
  Right: role badge showing current user role + user avatar

Alert banner area: directly below the header, full width.
  Amber background for Warning alerts, red background for Critical alerts.
  Unresolved alerts stay pinned across all pages until dismissed.
  Each alert is clickable and navigates to the relevant detail page.

Main content area: 12-column grid, 16px gutters, 24px page padding.
IBM Plex Mono for all numeric values, metric labels, badges, status values.
Inter or Pretendard for all UI text, headings, body copy.
```

---

## STEP 3. 화면별 프롬프트

---

### 3-1. 대시보드 — 통합 관제 (1-1)

```
Page: "통합 관제" under 대시보드.
Visible to Admin and Operator. QA sees limited view. Customer has no access.
Purpose: instant system health check. Read-only — no editable elements.

Top row: 5 KPI cards side by side.
  Cards: E2E Latency P95 / Error Rate / Answer Rate / Index Freshness / Cost per Query
  Each card: metric name (monospace, small) / large numeric value /
  colored status badge (green=정상, amber=경고, red=위험) / delta vs yesterday below.
  Each card is clickable → navigates to 5-3 이상 징후 감지.

Below KPI row: horizontal pipeline latency breakdown bar.
  4 stages: Retrieval · Reranking · LLM · 후처리
  Stacked horizontal bar with ms value and % per stage. Not clickable.

Below pipeline bar: system status indicator row.
  Label "시스템 전체 상태" + status badge (정상 / 경고 / 위험).
  Determined by worst status among all sub-metrics.

Below status: active alert log table.
  Columns: 발생 시각 / 지표명 / 현재값 / 임계값 / 심각도 / 상태
  Each row clickable → navigates to the relevant metric detail page.
  Unresolved rows have amber or red left border.
```

---

### 3-2. 대시보드 — 서비스 통계 (1-2)

```
Page: "서비스 통계" under 대시보드.
Admin, Operator, QA: full view.
Customer: DAU, 세션 성공률, 카테고리 분포 only. Knowledge Gap Rate section hidden.

Left column (60% width):
  DAU / 총 질의 수 — line chart, last 30 days.
  Below: hourly usage heatmap (24h × 7 days grid, color intensity = query volume).

Right column (40% width):
  Donut chart: query category distribution with legend.
  Below: Session Success Rate card — large % value, monthly trend sparkline,
  target shown as dashed reference line.

Bottom full-width row: [hidden for Customer]
  Knowledge Gap Rate card — current %, weekly trend line,
  count of unhandled queries as a clickable link → navigates to 4-2 미해결 질의.
```

---

### 3-3. 대시보드 — 품질/보안 요약 (1-3)

```
Page: "품질/보안 요약" under 대시보드.
Admin, Operator, QA: full view.
Customer: only sees Faithfulness and Hallucination Rate numeric values.
Sparklines, PII card, and feedback card are hidden for Customer.

Three summary cards in a row:
  Card 1 — Faithfulness: current score + 7-day sparkline.
    Top-right arrow link → 4-1 평가 지표.
  Card 2 — Hallucination Rate: current % + 7-day sparkline.
    Top-right arrow link → 4-1 평가 지표.
  Card 3 — PII 감지 건수: this month's count + last detected timestamp. [hidden for Customer]
    Top-right arrow link → 5-2 보안 감사 로그.

Below cards: user feedback card, full width. [hidden for Customer]
  Horizontal bar: 👍 / 👎 ratio + weekly trend line.
  Top-right arrow link → 4-3 답변 교정.
```

---

### 3-4. 지식베이스 — 데이터 업로드 (2-1)

```
Page: "데이터 업로드" under 지식베이스 관리.
Visible to Admin, Operator, QA. Hidden from Customer.

Top: 3-step progress indicator — Upload → Pre-process → Index (always visible).

Upload section:
  Large drag-and-drop zone: accepts HWP, PDF, XLSX, DOCX.
  Supported format badges shown inside the zone.
  Below: URL input for web crawling + crawl frequency radio (일간 / 주간)
  + excluded path text input.

Top right: "수동 재인덱싱" primary button.
  Triggers immediate re-index with a progress modal.
  On completion: Slack notification sent, status shown in modal.

Upload history table:
  Columns: 파일명 / 형식 / 크기 / 상태 / 업로드 일시
  Status badges: 처리중 (blue) / 완료 (green) / 실패 (red)
  Clicking a 실패 row → inline error detail panel below the row with:
    오류 내용 + "재처리" button + link "전처리 오류 로그 보기" → 2-2 데이터 전처리.
```

---

### 3-5. 지식베이스 — 데이터 전처리 (2-2)

```
Page: "데이터 전처리" under 지식베이스 관리.
Visible to Admin, Operator, QA. Hidden from Customer.

Two-panel layout:

Left panel — Chunking settings:
  Algorithm radio: 고정 크기 / 문장 단위 / 시맨틱
  Token size slider: 128–2048.
  Overlap ratio slider: 0–30%.
  On any change: amber banner "청킹 설정이 변경되었습니다.
  저장 시 전체 재인덱싱이 필요합니다 — 예상 소요: 약 23분".
  Save button at bottom.

Right panel — PII de-identification rules:
  Checklist: 주민번호 / 전화번호 / 계좌번호 / 이메일 / 이름 / 주소
  Masking method dropdown per entity: 마스킹(***) / 대체(토큰) / 삭제
  Save button. On save: modal "기존 인덱스에 소급 적용하시겠습니까?" (Yes / No).

Below both panels — Ingestion Error log:
  Table: 문서명 / 오류 유형 / 발생 일시 / 재처리 버튼

Bottom — Duplicate detection results:
  Table: Chunk ID A / Chunk ID B / 유사도 점수 / 작업
  작업: 삭제 (confirmation modal) / 병합 (confirmation modal)
```

---

### 3-6. 지식베이스 — 벡터 DB 관리 (2-3)

```
Page: "벡터 DB 관리" under 지식베이스 관리.
Visible to Admin and Operator only. QA and Customer cannot access.

Top — Embedding model:
  Dropdown to select model version + current dimension count (read-only).
  On change: amber banner "임베딩 모델 변경 시 전체 재인덱싱이 필요합니다".
  Confirm button to apply.

Middle — Index status (3 cards):
  Memory 사용률 (%) with progress bar /
  QPS with sparkline /
  Fallback Rate (%) with trend. All read-only.

Embedding Drift panel:
  Line chart: weekly distribution shift distance.
  Editable threshold input field.
  If drift > threshold: red warning badge + link "이상 징후 감지에서 확인하기" → 5-3 이상 징후 감지.

Bottom — Chunk search:
  Search bar (chunk_id or keyword).
  Results table: Chunk ID / 문서명 / 생성일 / 미리보기 / 작업(삭제, 갱신)
  삭제: confirmation modal. 갱신: text editor modal.
```

---

### 3-7. 지식베이스 — 동의어/금칙어 사전 (2-4)

```
Page: "동의어/금칙어 사전" under 지식베이스 관리.
Visible to Admin, Operator, QA.

Two tabs: "동의어 사전" | "금칙어 목록"

Tab 1 — 동의어 사전:
  Table: 대표어 / 동의어 목록 / 등록일 / 작업(편집, 삭제)
  "그룹 추가" button → modal: 대표어 + comma-separated 동의어.
  "CSV 업로드" button: bulk import.

Tab 2 — 금칙어 목록:
  Table: 금칙어 / 처리 방식(dropdown) / 등록일 / 작업(편집, 삭제)
  처리 방식: 차단 / 치환 / 경고
  "금칙어 추가" button → modal.
  Below table: block log.
  Table: 발생 일시 / 질의 패턴(PII 마스킹됨) / 처리 방식 / 처리 결과
```

---

### 3-8. 챗봇 엔진 — 프롬프트 엔지니어링 (3-1)

```
Page: "프롬프트 엔지니어링" under 챗봇 엔진 설정.
Visible to Admin, Operator, QA. Hidden from Customer.
Changes are NOT immediate. Workflow: 저장 → 검증 → 배포.

Top: 3-step workflow bar — 편집 중 → 시뮬레이션 검증 → 배포 완료. Active step highlighted.

Main — system prompt editor:
  Large text editor for system prompt (Persona).
  Tone selector below: 공식체 / 친근체 / 중립체 (radio buttons).
  "저장" button. On save:
    Step bar advances to step 2.
    Blue info banner: "변경사항이 저장되었습니다. 배포 전 시뮬레이션 룸에서 검증하세요."
    + button "시뮬레이션 룸으로 이동" → 4-4 시뮬레이션 룸.

Right sidebar — version history:
  List of last 10 versions: version number / 저장 일시 / 저장한 사람
  Each version has "롤백" button → confirmation modal → resets editor to that version.
```

---

### 3-9. 챗봇 엔진 — RAG 파라미터 튜닝 (3-2)

```
Page: "RAG 파라미터 튜닝" under 챗봇 엔진 설정.
Visible to Admin, Operator, QA. Hidden from Customer.
Same save → verify → deploy workflow as 3-8.

Top: same 3-step workflow bar.

Settings:
  Cosine Similarity threshold slider: 0.50–0.95, step 0.01. Value shown numerically.
  Top-K number input: range 1–20. Label "참조 문서 개수 (Top-K)".
  Reranker toggle: on/off. When on: Reranker 모델 dropdown appears.

Before/after comparison panel:
  Two columns: "변경 전" | "변경 후"
  Each shows simulated Recall@K value. Updates in real-time as sliders change.
  "상세 시뮬레이션 실행" button → 4-4 시뮬레이션 룸 with settings pre-loaded.

"저장" button. On save:
  Blue info banner: "파라미터가 저장되었습니다. 배포 후 4-1 평가 지표에서 효과를 확인하세요."
  + link → 4-1 평가 지표.
```

---

### 3-10. 챗봇 엔진 — 모델 서빙 관리 (3-3)

```
Page: "모델 서빙 관리" under 챗봇 엔진 설정.
Visible to Admin and Operator only. QA and Customer cannot access.

Top — LLM API status:
  Status dot (green=connected, red=disconnected) + last response time.
  Model version dropdown.
  "연결 테스트" button → live health check, result shown inline.

Middle — Instance resources (3 cards):
  CPU 사용률 (%) / Memory 사용률 (%) / GPU 사용률 (%) — all with real-time bars.

Bottom — Rate limit monitoring (2 cards):
  LLM API Rate Limit 히트율 (%) with 24h sparkline.
  Embedding API Rate Limit 히트율 (%) with 24h sparkline.
  Both show target threshold and current status badge.
```

---

### 3-11. 품질 관리 — 평가 지표 (4-1)

```
Page: "평가 지표" under 품질 관리.
Admin, Operator, QA: full access.
Customer: current numeric values only. Version comparison and drill-down hidden.

Top right: two version dropdowns for comparison. Default: current vs previous version.

6 metric cards in 3×2 grid:
  Faithfulness / Hallucination Rate / Recall@K /
  Answer Relevance / Citation Correctness / Session Success Rate
  Each card:
    Metric name / current value (large, monospace) / target label "목표: X.XX" /
    status badge (green/amber/red) /
    version comparison mini bar (current=blue, previous=gray)
  [Customer: version bar hidden. Only current value + status badge shown.]

Clicking a warning/critical card → right-side drawer:
  Metric description / sample cases (session ID, query, response snippet) /
  "시뮬레이션 룸에서 재현하기" button → 4-4 시뮬레이션 룸.
  [Customer: drawer does not open.]
```

---

### 3-12. 품질 관리 — 미해결 질의 (4-2)

```
Page: "미해결 질의" under 품질 관리.
Admin, Operator, QA: full access (edit status, assign owner).
Customer: read-only — no status change, no assignee.

Top filter bar: 카테고리 multi-select / 상태(전체/미처리/인입/완료) / 기간

Table (20 rows per page):
  Columns: 질의 내용(truncated) / 카테고리(badge) / 발생 건수 /
           첫 발생일 / 담당자 / 처리 상태 / 작업
  Rows with "미처리" status: subtle red left border.
  담당자 and 처리 상태: dropdowns for Admin/Operator/QA. Plain text for Customer.
  작업: "지식베이스 추가" button → 2-1 데이터 업로드.

Pagination at bottom.
```

---

### 3-13. 품질 관리 — 답변 교정 (4-3)

```
Page: "답변 교정" under 품질 관리.
Visible to Admin, Operator, QA. Hidden from Customer.

Left panel (40%) — feedback list:
  List of 👎 sessions: session ID / query snippet / response snippet / received date.
  Clicking loads it in the right panel.

Right panel (60%) — correction editor:
  Original query (read-only) + original RAG response (read-only, with source chunks).
  Ground Truth text editor below.
  "Synthetic QA 데이터셋에 추가" button.
  "저장" button.

Bottom — correction history table:
  질의 / 수정일 / 교정자 / 변경 전(요약) / 변경 후(요약)
  Each row expandable → full before/after content.
```

---

### 3-14. 품질 관리 — 시뮬레이션 룸 (4-4)

```
Page: "시뮬레이션 룸" under 품질 관리.
Visible to Admin, Operator, QA. Hidden from Customer.

Top bar: Version A dropdown (left) | Version B dropdown (right).
"결과 저장" button top right → saves session + generates share link.

Split layout: Version A panel | Version B panel (equal width, with divider).

Each panel:
  Version label at top.
  Query input text area + "실행" button.
  RAG response output (read-only, monospace).
  Retrieved chunk list below the response:
    Each chunk: Chunk ID / 문서명 / 유사도 점수 / 청크 내용 snippet.
    Sorted by similarity score descending.
    Dark terminal-like background for the chunk list area.

Panels update independently.
```

---

### 3-15. 품질 관리 — 레드팀 케이스셋 (4-5)

```
Page: "레드팀 케이스셋" under 품질 관리.
Admin: full access.
QA: full access including case editing.
Operator: run and view history only — cannot add/edit/delete cases.
Customer: no access.

Top bar:
  Category tabs: 전체 / PII 유도 / 도메인 외 / 프롬프트 인젝션 / 유해 콘텐츠
  Right: "케이스 추가" (secondary) | "전체 실행" (primary blue)
  CI/CD status: GitHub Actions dot + last run timestamp.

If pass rate < 95%: red full-width banner:
  "배포가 블록되었습니다 — 통과율 XX.X% (목표: 95% 이상)"

Table:
  Case ID / 카테고리(badge) / 프롬프트(truncated) / 기대 동작 / 최근 결과(badge) / 작업
  작업: 편집 / 삭제 (visible to QA and Admin only).
  "전체 실행" → progress modal → result summary modal.

Execution history section below table:
  버전 / 실행일 / 케이스 수 / 통과율(progress bar) / 실패 건수
  Each row expandable → failed case list with category + failure reason.
```

---

### 3-16. 서비스 모니터링 — 대화 이력 조회 (5-1)

```
Page: "대화 이력 조회" under 서비스 모니터링.
Admin, Operator: all customers' sessions, full detail.
QA: all sessions but chunk source (문서명) hidden in detail.
Customer: own company's sessions only. 유사도 점수 hidden in detail.

Top filter bar: 기간 / 고객사(hidden for Customer) / 세션 성공여부 / 카테고리

Session list table:
  Columns: 세션 ID / 고객사 / 시작 시각 / 턴 수 / 세션 성공여부(badge) / 카테고리
  Clicking a row → inline expanded detail panel below.

Detail panel:
  All turns in order: 질의 / 응답 pairs.
  Per turn: retrieved chunks (Chunk ID / 문서명 / 유사도 점수 / 내용 snippet).
  [QA: 문서명 hidden] [Customer: 유사도 점수 hidden]
  Response time shown per turn.
  "이 세션 공유" button (Admin/Operator only) → generates shareable link.
```

---

### 3-17. 서비스 모니터링 — 보안 감사 로그 (5-2)

```
Page: "보안 감사 로그" under 서비스 모니터링.
Visible to Admin and Operator only. QA and Customer cannot access.

Three tabs:

Tab 1 — PII 감지 이벤트:
  Table: 감지 일시 / 감지 유형(badge) / 마스킹 처리 여부(badge) / 세션 ID / 처리 결과
  Original content never shown.
  Each row: "PII 규칙 수정" link → 2-2 데이터 전처리 PII section.

Tab 2 — 관리자 접근 이력:
  Table: 접근 일시 / 사용자 / 역할 / 접근 메뉴 / IP 주소 / 결과(성공/실패)
  Filter by user and date range.

Tab 3 — 금칙어 차단 로그:
  Table: 발생 일시 / 질의 패턴(PII 마스킹됨) / 적용 금칙어 / 처리 방식 / 결과
  Read-only. CSV export button.
```

---

### 3-18. 서비스 모니터링 — 이상 징후 감지 (5-3)

```
Page: "이상 징후 감지" under 서비스 모니터링.
Visible to Admin, Operator, QA. Hidden from Customer.
This is the landing page when KPI cards on 1-1 대시보드 are clicked.

Four chart panels in 2×2 grid:
  Panel 1 — Query Drift:
    Line chart, 30 days. Y-axis: cosine distance from 7-day rolling baseline.
    Dashed red threshold line at configured value.
  Panel 2 — Recall Baseline Deviation:
    Line chart: delta (%) from 7-day rolling mean. Reference lines at +5% and -5%.
  Panel 3 — Embedding Drift:
    Weekly bar chart. Bars colored by severity (green/amber/red).
  Panel 4 — 비정상 반복 질의:
    Table: 질의 패턴 / 발생 건수 / 발생 간격 / 출처 IP(마스킹) / 의심 수준(badge)

Below charts — threshold settings panel:
  Table: 지표명 / 경고 임계값(editable input) / 위험 임계값(editable input) / 알림 연동 상태(dot)
  "저장" button.
  Slack/PagerDuty status dots.
  "알림 채널 설정" link → 7-2 연동 API 관리.
```

---

### 3-19. 통계 — 성과 분석 리포트 (6-1)

```
Page: "성과 분석 리포트" under 통계 및 보고서.
Admin, Operator: all customers' data.
Customer: own company only — cross-customer comparison hidden.
QA: no access.

Top: report type radio (주간 / 월간) + customer selector (hidden for Customer)
+ "리포트 생성" primary button → generates PDF/PPT and downloads.

Quality metrics trend:
  Multi-line chart: Faithfulness and Session Success Rate over selected period.
  Period markers on x-axis for notable events (model updates etc).

Customer satisfaction summary:
  Horizontal bar: 👍/👎 ratio. Delta vs previous period shown.

Summary table:
  Columns: 지표 / 이번 기간 / 지난 기간 / 변화량 / 상태
  Rows: all 6 quality metrics (same as 4-1 평가 지표).
```

---

### 3-20. 통계 — 비용 분석 (6-2)

```
Page: "비용 분석" under 통계 및 보고서.
Visible to Admin and Operator only. QA and Customer cannot access.

Top row: 4 KPI cards.
  Cost per Query / 일 총 비용(어제) / Cache Hit Rate / 평균 토큰 수(입력+출력)
  Each: value / delta vs previous period / status badge.

Stacked bar chart — daily total cost, last 30 days:
  Layers: LLM API (green) / Embedding (blue) / Reranker (purple) / Infrastructure (amber)
  Hover tooltip shows breakdown.

Two side-by-side charts:
  Left: Token Usage — dual line (입력 토큰 vs 출력 토큰).
  Right: Cache Hit Rate trend line.
    If Cache Hit Rate < 15%: amber tip card below:
    "캐시 적중률이 낮습니다. RAG 파라미터 튜닝을 검토하세요."
    + link → 3-2 RAG 파라미터 튜닝.

Customer cost breakdown table:
  Columns: 고객사명 / LLM API 비용 / 임베딩 비용 / Reranker 비용 / 합계 / 예산 대비(%)
  Sortable. CSV export button.
```

---

### 3-21. 시스템 — 사용자/권한 관리 (7-1)

```
Page: "사용자/권한 관리" under 시스템 및 권한.
Visible to Admin only.

Split layout:

Left panel (35%) — user list:
  Search bar at top.
  Table: 이름 / 역할(badge) / 소속 고객사 / 마지막 로그인 / 작업(편집, 비활성화)
  "계정 생성" button → opens right panel in create mode.
  Clicking a user row → loads detail in right panel.

Right panel (65%) — user detail / creation form:
  Name, email, role selector (Admin / Operator / QA / Customer).
  If role = Customer: company assignment dropdown appears.

  RBAC permission matrix (Customer role only):
    Grid: 22 menu items (rows) × permission O / △ / X (columns).
    Clickable cells to toggle.
    "기본값으로 초기화" button.

  Data retention period (below RBAC matrix):
    Label: "대화 이력 보존 기간"
    Dropdown: 30일 / 60일 / 90일 / 180일 / 1년

  "저장" and "취소" buttons at bottom.
```

---

### 3-22. 시스템 — 연동 API 관리 (7-2)

```
Page: "연동 API 관리" under 시스템 및 권한.
Visible to Admin and Operator. QA and Customer cannot access.

Section 1 — External integration keys:
  Table: 서비스명 / API 키(masked, last 4 chars) / 발급일 / 마지막 사용 / 상태(badge) / 작업
  상태: 활성 / 만료 / 비활성
  작업: "재발급" (confirmation modal) + "폐기" (confirmation modal)
  "새 API 키 발급" button → modal: service name input + expiry date picker.

Section 2 — Webhook & alert channel settings:
  Slack:
    Webhook URL input + "연결 테스트" button.
    Warning channel input (#channel-name).
    Critical channel input (#channel-name).
    Connection status dot.

  PagerDuty:
    Integration key input + "연결 테스트" button.
    Escalation policy selector.
    Connection status dot.

  Note at bottom: "알림 임계값 설정은 5-3 이상 징후 감지에서 관리합니다."
  Link → 5-3 이상 징후 감지.

  "저장" button.
```

---

## STEP 4. 다크 테마 마무리

```
Finalize the overall dark theme:
Page background: #0f172a
Card backgrounds: #1e293b
Borders: #334155
Primary accent: #2563eb
Status: green #10b981, amber #f59e0b, red #ef4444, gray #64748b
IBM Plex Mono for all numbers, metrics, badges.
Inter or Pretendard for all UI text.
Buttons: 6px border radius.
Input fields: 1px #334155 border, 4px radius.
Sidebar active: 3px left border #2563eb + background #1e293b.
Spacing: 16px base. Card padding: 20px. Section gaps: 24px.
Shadows: max 0 1px 3px rgba(0,0,0,0.3). No gradients.
```

---

## STEP 5. 라이트 테마로 전환

> 다크 모드로 완성된 결과물에 라이트 테마만 덮어씌울 때 사용.  
> 레이아웃·컴포넌트·차트 구조는 그대로 유지. 색상만 교체.

### 5-1. 전체 색상 전환

```
Switch the entire app to light mode. Keep all layouts and components unchanged.

Page background: #f1f5f9
Card background: #ffffff
Sidebar background: #ffffff
All borders: #e2e8f0
Primary text: #0f172a
Secondary text: #475569
Muted / label text: #94a3b8
Primary accent: #2563eb (unchanged)
Status: green #059669, amber #d97706, red #dc2626
Card shadow: 0 1px 3px rgba(0,0,0,0.08)
Input border: #cbd5e1
Sidebar active: 3px left border #2563eb + background #eff6ff
```

### 5-2. 컴포넌트별 세부 조정

```
Alert banners:
  Warning — background #fffbeb, border #fde68a, text #92400e
  Critical — background #fef2f2, border #fecaca, text #991b1b

Tables:
  Header background: #f8fafc. Row hover: #f1f5f9. Borders: #e2e8f0.

Charts:
  Gridlines: #e2e8f0. Axis labels: #94a3b8.
  Tooltip: white background, #e2e8f0 border.
  Pipeline latency track: #e2e8f0.
  All data series colors: unchanged.

Simulation Room chunk output area:
  Background: #f8fafc. Border: #e2e8f0. Code text: #1e293b.

Step indicator bars: active step #2563eb, inactive #e2e8f0.
```

### 5-3. 최종 확인

```
Review all 22 screens in light mode and verify:
- No white text on white background anywhere
- All status badges readable (sufficient contrast on white)
- Chart lines and bars clearly visible on light background
- Sidebar and main content area are visually distinct
- KPI values bold and high-contrast on white cards
- Alert banners clearly distinguishable from regular content
```
