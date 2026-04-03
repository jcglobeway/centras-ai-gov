# Tasks: admin-dashboard-renewal

---

## STEP 1 — 전체 앱 뼈대 생성

- [x] `frontend/tailwind.config.ts` — `darkMode: 'class'` 설정, CSS 변수 참조 토큰 교체, `plex-mono` / `inter` fontFamily 추가
- [x] `frontend/src/app/globals.css` — `:root` (라이트) + `.dark` (다크) CSS 변수 정의
- [x] `frontend/src/app/layout.tsx` — 깜빡임 방지 인라인 스크립트 추가
- [x] `frontend/src/lib/theme.ts` — `useTheme()` hook 신규 (localStorage 'theme' 키, dark/light 토글)
- [x] `frontend/src/app/ops/layout.tsx` — 레이아웃 구조 확인 (Sidebar + Header + main 골격 유지)
- [x] `frontend/src/app/client/layout.tsx` — 동일 확인
- [x] `frontend/src/app/qa/layout.tsx` — 동일 확인

---

## STEP 2 — 공통 레이아웃 & 네비게이션

- [x] `frontend/src/components/layout/Sidebar.tsx`
  - 너비 w-56 → w-60 (240px)
  - `border-r` 제거 (No-Line Rule)
  - 아이콘: 특수문자 → Material Symbols (`<span className="material-symbols-outlined">`)
  - 활성 상태: `bg-accent/10 text-accent` → `bg-bg-elevated text-white border-l-[3px] border-accent`
  - 섹션 헤더 그루핑 (`NavSection` 타입 도입)
  - ops 메뉴 재편 (IA 최종안 기준, STEP 3 화면과 1:1 대응)
  - System Health LED 도트 컴포넌트 (하단 고정)
- [x] `frontend/src/components/layout/Header.tsx`
  - 테마 토글 버튼 추가 (Material Symbols `light_mode` / `dark_mode`)
  - `useTheme()` hook 연결
- [x] `frontend/src/components/charts/KpiCard.tsx`
  - 상단 stripe 제거 → 하단 progress bar 추가
  - label: `font-mono` 10px uppercase
  - status pill: IBM Plex Mono 9px, OPTIMAL / WARNING / CRITICAL
- [x] `frontend/src/components/ui/Card.tsx`
  - `CardTitle` font-size: `text-[12px]` → `text-[13px]`
- [x] `frontend/src/app/layout.tsx` — Material Symbols CSS CDN link 추가

---

## STEP 3 — 화면별 구현 (IA 순서)

### 3-1. 통합 관제 (`/ops`)
- [x] `frontend/src/app/ops/page.tsx`
  - KPI 5개: ANSWER RATE / ERROR RATE / E2E LATENCY / KNOWLEDGE GAP / COST/QUERY
  - 파이프라인 레이턴시 바 (Retrieval / Re-ranking / LLM / 후처리, 초기 하드코딩)
  - 시스템 상태 신호등 (RAG 파이프라인 / 지식베이스 / 응답 품질, metrics 기반 자동 판정)
  - 기관 헬스맵 (기존 유지)
  - 최근 질문 테이블 (기존 유지)

### 3-2. 서비스 통계 (`/ops/statistics`) — 신규
- [x] `frontend/src/app/ops/statistics/page.tsx`
  - DAU / 총 질의 수 KPI (metrics/daily)
  - Knowledge Gap Rate KPI → 미해결 질의 링크
  - 질의 수 7일 추이 차트 (MetricsLineChart)
  - 카테고리 분포 도넛 차트 (목업 데이터, recharts PieChart)

### 3-3. 품질/보안 요약 (`/ops/quality-summary`) — 신규
- [x] `frontend/src/app/ops/quality-summary/page.tsx`
  - Faithfulness / Hallucination Rate KPI (RAGAS evaluations)
  - 사용자 피드백 👍/👎 비율 (feedbacks API)
  - 스파크라인 미니 차트 (7일 추이)

### 3-4. 데이터 업로드 (`/ops/upload`) — 신규 (목업)
- [x] `frontend/src/app/ops/upload/page.tsx`
  - 파일 업로드 드래그앤드롭 UI (HWP/PDF/XLSX/DOCX)
  - 웹 크롤링 설정 (URL 등록, 주기 선택)
  - 업로드 이력 테이블 (실제 ingestion-jobs API 사용)
  - 수동 재인덱싱 버튼 + 스텝 인디케이터

### 3-5. 데이터 전처리 (`/ops/preprocessing`) — 신규 (목업)
- [x] `frontend/src/app/ops/preprocessing/page.tsx`
  - 청킹 알고리즘 선택 (고정 크기 / 문장 / 시맨틱)
  - 토큰 크기·오버랩 수치 설정 UI
  - PII 비식별화 규칙 설정
  - Ingestion Error 로그 테이블 (목업)

### 3-6. 벡터 DB 관리 (`/ops/vector-db`) — 신규 (목업)
- [x] `frontend/src/app/ops/vector-db/page.tsx`
  - 임베딩 모델 선택 드롭다운
  - 인덱스 상태 KPI (Memory 사용률 / QPS / Fallback Rate, 목업)
  - Embedding Drift 모니터링 카드 (목업)
  - 청크 검색 UI (chunk_id 검색, 목업)

### 3-7. 동의어/금칙어 (`/ops/dictionary`) — 신규 (목업)
- [x] `frontend/src/app/ops/dictionary/page.tsx`
  - 동의어 그룹 CRUD 테이블 (목업)
  - 금칙어 목록 + 처리 방식 설정 (목업)
  - 차단 로그 요약 카드 (목업)

### 3-8. 프롬프트 엔지니어링 (`/ops/prompt`) — 신규 (목업)
- [x] `frontend/src/app/ops/prompt/page.tsx`
  - 시스템 프롬프트 편집기 (textarea)
  - Persona / 톤앤매너 선택 (공식/친근/중립)
  - 버전 이력 목록 (목업)
  - 저장 후 "시뮬레이션 룸에서 검증하세요" 안내 배너

### 3-9. RAG 파라미터 튜닝 (`/ops/rag-params`) — 신규 (목업)
- [x] `frontend/src/app/ops/rag-params/page.tsx`
  - 유사도 임계값 슬라이더 (Cosine Similarity)
  - Top-K 설정 (숫자 입력)
  - Reranker 토글
  - 변경 전후 Recall@K 미리보기 (목업)

### 3-10. 모델 서빙 관리 (`/ops/model-serving`) — 신규 (목업)
- [x] `frontend/src/app/ops/model-serving/page.tsx`
  - LLM API 연동 상태 카드 (목업)
  - 인스턴스 리소스 CPU/Memory/GPU 현황 (목업)
  - Rate Limit 히트율 (목업)

### 3-11. 평가 지표 (`/ops/quality`)
- [x] `frontend/src/app/ops/quality/page.tsx`
  - 기존 유지 + 버전 비교 탭 UI (현재 배포 vs 이전 버전, 목업)
  - 이상 시 드릴다운 링크 (→ /ops/simulator)

### 3-12. 미해결 질의 (`/ops/unresolved`) — 신규 (qa/unresolved에서 분리)
- [x] `frontend/src/app/ops/unresolved/page.tsx`
  - 기존 `qa/unresolved/page.tsx` 로직 이전
  - 카테고리 자동 분류 컬럼 추가
  - 담당자 지정 UI (목업)
  - 지식베이스 추가 바로가기 (→ /ops/upload)

### 3-13. 답변 교정 (`/ops/correction`) — 신규 (목업)
- [x] `frontend/src/app/ops/correction/page.tsx`
  - 부정 피드백 답변 목록 테이블 (feedbacks API, rating <= 2)
  - Ground Truth 입력 편집기 (목업)
  - 교정 이력 (교정자·일시·변경 전/후, 목업)

### 3-14. 시뮬레이션 룸 (`/ops/simulator`)
- [x] `frontend/src/app/ops/simulator/page.tsx`
  - A/B 비교 2패널 레이아웃 (Version A / Version B)
  - Retrieved Chunks 영역 (청크 목록 + 유사도 점수, 목업 폴백)
  - 양 패널 별도 sessionId 관리

### 3-15. 레드팀 케이스셋 (`/ops/redteam`) — 신규
- [x] `frontend/src/app/ops/redteam/page.tsx`
  - safety.tsx 레드팀 이력 테이블 이전
  - 케이스 관리 Card (목업)
  - 일괄 실행 버튼 + 통과율 도넛 (목업)
  - 실행 이력 Table

### 3-16. 대화 이력 (`/ops/chat-history`) — 신규 (목업)
- [x] `frontend/src/app/ops/chat-history/page.tsx`
  - 세션 로그 테이블 (기관별 필터, 목업)
  - 세션 상세 버튼 disabled

### 3-17. 보안 감사 로그 (`/ops/audit`) — 신규 (목업)
- [x] `frontend/src/app/ops/audit/page.tsx`
  - PII 감지 이벤트 테이블 (목업)
  - 관리자 접근 이력 (목업)
  - 금칙어 차단 로그 요약 카드

### 3-18. 이상 징후 감지 (`/ops/anomaly`) — 신규 (incidents + safety 통합)
- [x] `frontend/src/app/ops/anomaly/page.tsx`
  - Query Drift / Recall Deviation / Embedding Drift / 반복 질의 KPI
  - 7일 Drift 추이 차트 (MetricsLineChart, metrics/daily)
  - 임계값 초과 알림 테이블 (incidents 로직 이전)
  - 임계값 설정 UI (저장 API 미연동)
  - 안전성 지표 Card (safety 이전)
- [x] `frontend/src/app/ops/incidents/page.tsx` 삭제
- [x] `frontend/src/app/ops/safety/page.tsx` 삭제

### 3-19. 성과 분석 리포트 (`/ops/reports`) — 신규 (목업)
- [x] `frontend/src/app/ops/reports/page.tsx`
  - 품질 지표 추이 (주간/월간, 목업)
  - 고객사별 만족도 요약 (목업)
  - PDF/PPT 다운로드 버튼 (목업)

### 3-20. 비용 분석 (`/ops/cost`)
- [x] `frontend/src/app/ops/cost/page.tsx`
  - Cache Hit Rate KPI 추가 (목업, → RAG 파라미터 링크)

### 3-21. 사용자/권한 관리 (`/ops/users`) — 신규 (목업)
- [x] `frontend/src/app/ops/users/page.tsx`
  - 계정 목록 테이블 (목업)
  - RBAC 설정 테이블 (읽기 전용 토글 표시)

### 3-22. 연동 API 관리 (`/ops/api-keys`) — 신규 (목업)
- [x] `frontend/src/app/ops/api-keys/page.tsx`
  - 외부 연동 키 목록 (목업)
  - API 호출량 카드
  - Webhook 설정 (Slack/PagerDuty, 목업)

---

## STEP 4 — 다크 테마 마무리

- [x] 전체 페이지 CSS 변수 토큰 적용 확인 (하드코딩 색상값 잔재 제거)
- [x] KpiCard progress bar 다크 트랙 색상 확인 (`bg-bg-prominent`)
- [x] Sidebar System Health LED glow 확인 (`box-shadow: 0 0 6px var(--success)`)
- [x] Modal / Dropdown backdrop-blur 다크 동작 확인
- [x] recharts 차트 계열색·그리드선 다크 팔레트 정합성 확인 (CSS 변수로 교체)
- [x] `npm run build` 0 errors 확인

---

## STEP 5 — 라이트 테마 전환 (다크 완성 후)

- [x] `globals.css` `:root` 라이트 변수값 최종 확인 (Digital Architect 기준)
- [x] Header 테마 토글 동작 확인 (dark class 추가/제거)
- [x] 라이트 테마 전체 페이지 시각 검수
  - 배경: `#f8f9ff` (base) / `#ffffff` (surface) / `#eff4ff` (elevated)
  - 텍스트: `#0b1c30` (primary) — 순수 검정 미사용 확인
  - Sidebar: No-Line Rule (border-r 없음, tone shift만)
  - KpiCard: 라이트 배경(`#ffffff`)에서 progress bar 가시성 확인
  - 차트: 라이트 배경에서 계열색 대비 확인 (CSS 변수 기반으로 자동 대응)
- [x] localStorage 테마 유지 확인 (새로고침 후 깜빡임 없음)
- [x] 다크↔라이트 전환 애니메이션 `transition-colors duration-150` 전체 적용 확인
