# Tasks: dashboard-v2-data-pipeline

## Phase A — 공통 컴포넌트

- [x] `KpiCard.tsx` — `status?: "ok" | "warn" | "critical"` prop + 상단 2px 컬러 스트라이프
- [x] `ProgressBar.tsx` 신규 생성 — 파이프라인 단계별 레이턴시 바 (label / valueMs / maxMs / color)
- [x] `AlertBanner.tsx` 신규 생성 — 임계값 초과 경고 배너 (warn/critical variant, dismissible)
- [x] `ScoreTable.tsx` 신규 생성 — RAGAS 지표 테이블 (지표명 / 점수 / 목표 / 상태 Badge)

## Phase B — 백엔드 API

- [x] `ListRagasEvaluationsUseCase.kt` 인터페이스 생성 (`application/port/in/`)
- [x] `LoadRagasEvaluationsPort.kt` 인터페이스 생성 (`application/port/out/`)
- [x] `ListRagasEvaluationsService.kt` 구현체 생성 (`application/service/`)
- [x] `LoadRagasEvaluationsPortAdapter.kt` 구현체 생성 (`adapter/outbound/persistence/`)
- [x] `JpaRagasEvaluationRepository` — `findAll()` 또는 필터 쿼리 메서드 추가
- [x] `RagasEvaluationController.kt` — `GET /admin/ragas-evaluations` 핸들러 추가
- [x] `ServiceConfiguration.kt` — `listRagasEvaluationsUseCase` bean 등록
- [x] `RepositoryConfiguration.kt` — `loadRagasEvaluationsPort` bean 등록
- [x] `types.ts` — `RagasEvaluation` 인터페이스 추가

## Phase B+ — LLM 메트릭 API (C-fix 추가)

- [x] `GetLlmMetricsUseCase.kt` + `LoadLlmMetricsPort.kt` + `GetLlmMetricsService.kt`
- [x] `LoadLlmMetricsPortAdapter.kt` — answers/questions 인메모리 집계
- [x] `LlmMetricsController.kt` — `GET /admin/metrics/llm`
- [x] `ServiceConfiguration.kt` + `RepositoryConfiguration.kt` bean 등록
- [x] `types.ts` — `LlmMetrics` 인터페이스 추가

## Phase C — 대시보드 페이지

- [x] `ops/page.tsx` — 5 KPI(상태 스트라이프) + 파이프라인 ProgressBar × 3 + 최근 질문 테이블 + AlertBanner 조건부
- [x] `ops/quality/page.tsx` — RAGAS ScoreTable 섹션 추가 (빈 상태 처리 포함)
- [x] `ops/indexing/page.tsx` — KpiCard 3개(성공/실패/실행 중) 상단 추가
- [x] `ops/incidents/page.tsx` — placeholder 제거, 알림 로그 테이블 구현
- [x] `client/page.tsx` — 미해결 건수 KpiCard + 응답률 목표 진행 바
- [x] `qa/page.tsx` — RAGAS ScoreTable + 최근 QA 리뷰 5건 테이블

## Phase C-fix — v3 HTML 기준 재구현 (추가)

- [x] `tailwind.config.ts` — 색상 토큰 v3 정렬 (--green/--red/--blue/--bg 계열)
- [x] `KpiCard.tsx` 오버홀 — label mono uppercase / value 26px bold colored / footer
- [x] `Badge.tsx` — border colored, font-mono 10px
- [x] `Table.tsx` — Th uppercase mono, Td 11px
- [x] `Card.tsx` — tag prop 추가 (ph-tag + ph-title 패턴)
- [x] `ProgressBar.tsx` — v3 치수 정렬 (label 82px / track 22px / num 55px)
- [x] `MetricsLineChart.tsx` — v3 팔레트 적용
- [x] `ops/cost/page.tsx` — 신규 Cost & Health 페이지 (4 KPI + LLM 비용 + Knowledge Gap + Doc Health)
- [x] `ops/safety/page.tsx` — 신규 Safety 페이지 (4 KPI + Safety 지표 그리드 + 레드팀 이력)
- [x] `Sidebar.tsx` — `/ops/cost`, `/ops/safety` 항목 추가

## Phase D — 데이터 파이프라인 (실제 동작)

- [x] ZIP 구조 확인 — `TL_지방행정기관_질의응답.zip` JSON 샘플 파싱
- [x] `ingestion_prep.py` 구현 (`python/eval-runner`):
  - [x] ZIP 6개 전량(TL+VL × 3기관) 파싱 → documents SQL (300건) + `eval_questions.json` 100건 생성
  - [x] 케이스 유형별 균등 샘플링 (사실 조회 30 / 절차 안내 30 / 자격 확인 20 / 비교 10 / 복합 10)
  - [x] 기존 answers LLM 메트릭 UPDATE 구문 포함
- [ ] `embedder.py` 구현 (`python/ingestion-worker`) — Phase 3으로 이월
- [x] `query_runner.py` 구현 (`python/eval-runner`):
  - [x] `POST /admin/questions` 실제 RAG 질의
  - [x] `GET /admin/rag-search-logs?question_id=` 로 contexts 조회
  - [x] `eval_results.json` 생성 (question / answer / contexts / ground_truth)
- [x] `run_pipeline.sh` 원샷 실행 스크립트 작성
- [x] `V027__seed_public_documents_and_llm_metrics.sql` 생성 (PostgreSQL 전용)
- [x] `application-test.yml` — flyway.target: "26" 유지 (V027은 H2 불필요)

## Phase E — 검증

- [x] `./gradlew :apps:admin-api:test` 전체 통과 확인 (50건)
- [ ] PostgreSQL 환경에서 V027 마이그레이션 확인 (`docker-compose up -d`)
- [ ] 프론트엔드 로컬 실행 + 시나리오 검증:
  - [ ] ops@jcg.com → /ops → KPI 5개 상태 색상 표시
  - [ ] ops@jcg.com → /ops/quality → RAGAS 스코어카드 표시
  - [ ] ops@jcg.com → /ops/cost → LLM 비용 + Knowledge Gap 표시
  - [ ] ops@jcg.com → /ops/safety → Safety 지표 + 레드팀 이력 표시
  - [ ] ops@jcg.com → /ops/indexing → 요약 KPI 표시
  - [ ] ops@jcg.com → /ops/incidents → 알림 로그 테이블 표시
  - [ ] client@jcg.com → /client → 미해결 건수 표시
  - [ ] qa@jcg.com → /qa → RAGAS 점수 + 최근 리뷰 표시
- [ ] 커밋
