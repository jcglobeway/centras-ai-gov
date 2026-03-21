# Tasks: dashboard-v2-data-pipeline

## Phase A — 공통 컴포넌트

- [ ] `KpiCard.tsx` — `status?: "ok" | "warn" | "critical"` prop + 상단 2px 컬러 스트라이프
- [ ] `ProgressBar.tsx` 신규 생성 — 파이프라인 단계별 레이턴시 바 (label / valueMs / maxMs / color)
- [ ] `AlertBanner.tsx` 신규 생성 — 임계값 초과 경고 배너 (warn/critical variant, dismissible)
- [ ] `ScoreTable.tsx` 신규 생성 — RAGAS 지표 테이블 (지표명 / 점수 / 목표 / 상태 Badge)

## Phase B — 백엔드 API

- [ ] `ListRagasEvaluationsUseCase.kt` 인터페이스 생성 (`application/port/in/`)
- [ ] `LoadRagasEvaluationsPort.kt` 인터페이스 생성 (`application/port/out/`)
- [ ] `ListRagasEvaluationsService.kt` 구현체 생성 (`application/service/`)
- [ ] `LoadRagasEvaluationsPortAdapter.kt` 구현체 생성 (`adapter/outbound/persistence/`)
- [ ] `JpaRagasEvaluationRepository` — `findAll()` 또는 필터 쿼리 메서드 추가
- [ ] `RagasEvaluationController.kt` — `GET /admin/ragas-evaluations` 핸들러 추가
- [ ] `ServiceConfiguration.kt` — `listRagasEvaluationsUseCase` bean 등록
- [ ] `RepositoryConfiguration.kt` — `loadRagasEvaluationsPort` bean 등록
- [ ] `types.ts` — `RagasEvaluation` 인터페이스 추가

## Phase C — 대시보드 페이지

- [ ] `ops/page.tsx` — 5 KPI(상태 스트라이프) + 파이프라인 ProgressBar × 3 + 최근 질문 테이블 + AlertBanner 조건부
- [ ] `ops/quality/page.tsx` — RAGAS ScoreTable 섹션 추가 (빈 상태 처리 포함)
- [ ] `ops/indexing/page.tsx` — KpiCard 3개(성공/실패/실행 중) 상단 추가
- [ ] `ops/incidents/page.tsx` — placeholder 제거, 알림 로그 테이블 구현
- [ ] `client/page.tsx` — 미해결 건수 KpiCard + 응답률 목표 진행 바
- [ ] `qa/page.tsx` — RAGAS ScoreTable + 최근 QA 리뷰 5건 테이블

## Phase D — 데이터 파이프라인

- [ ] ZIP 파일 샘플 추출 — `TL_지방행정기관_질의응답.zip` JSON 구조 확인
- [ ] `V027__seed_public_data_and_llm_metrics.sql` 생성:
  - [ ] 기존 answered answers에 LLM 메트릭 UPDATE (model_name, tokens, cost, finish_reason)
  - [ ] 공공 민원 Q&A 10~20건 INSERT (sessions → questions → answers 순서)
  - [ ] `ragas_evaluations` seed 5건 INSERT
- [ ] `application-test.yml` — `flyway.target: "27"` 업데이트

## Phase E — 검증

- [ ] `./gradlew :apps:admin-api:test` 전체 통과 확인
- [ ] PostgreSQL 환경에서 V027 마이그레이션 확인 (`docker-compose up -d`)
- [ ] 프론트엔드 로컬 실행 + 시나리오 검증:
  - [ ] ops@jcg.com → /ops → KPI 5개 상태 색상 표시
  - [ ] ops@jcg.com → /ops/quality → RAGAS 스코어카드 표시
  - [ ] ops@jcg.com → /ops/indexing → 요약 KPI 표시
  - [ ] ops@jcg.com → /ops/incidents → 알림 로그 테이블 표시
  - [ ] client@jcg.com → /client → 미해결 건수 표시
  - [ ] qa@jcg.com → /qa → RAGAS 점수 + 최근 리뷰 표시
- [ ] 커밋
