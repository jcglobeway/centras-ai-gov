# Proposal: dashboard-v2-data-pipeline

## Change ID

`dashboard-v2-data-pipeline`

## Summary

### 목적

v3 PRD(`docs/rag-admin-dashboard-v3.html`) 기준으로 어드민 포털 대시보드를 production-ready 수준으로 고도화하고,
공공 민원 상담 데이터(`data/3.개방데이터/`)를 활용해 실제 DB를 채워 모든 지표가 동작하는지 검증한다.

### 변경 범위

**프론트엔드 (Next.js 15)**

- 신규 공통 컴포넌트: `ProgressBar`, `AlertBanner`, `ScoreTable`
- 기존 컴포넌트 확장: `KpiCard` — `status` prop(ok/warn/critical) 추가로 상단 컬러 스트라이프 표시
- 대시보드 페이지 6개 고도화:
  - `ops/page.tsx` — 5 KPI + 파이프라인 레이턴시 바 + 최근 질문 테이블 + AlertBanner
  - `ops/quality/page.tsx` — RAGAS 스코어카드 섹션 추가
  - `ops/indexing/page.tsx` — 요약 KPI 3개(성공/실패/실행 중) 추가
  - `ops/incidents/page.tsx` — placeholder → 임계값 초과 알림 로그 테이블
  - `client/page.tsx` — 미해결 건수 KpiCard + 응답률 목표 진행 바
  - `qa/page.tsx` — RAGAS 점수 섹션 + 최근 QA 리뷰 5건 테이블

**백엔드 (Spring Boot + Kotlin)**

- `GET /admin/ragas-evaluations` 목록 조회 엔드포인트 신규 추가
  - `ListRagasEvaluationsUseCase` + `LoadRagasEvaluationsPort` (hexagonal)
  - `RagasEvaluationController` GET 핸들러
  - `ServiceConfiguration` / `RepositoryConfiguration` bean 등록

**DB 마이그레이션**

- `V027__seed_public_data_and_llm_metrics.sql` 신규 생성:
  - 기존 seed answers에 LLM 메트릭(model_name, input_tokens, estimated_cost_usd 등) UPDATE
  - 공공 민원 데이터 기반 Q&A 10~20건 INSERT (중앙행정기관/지방행정기관)
  - `ragas_evaluations` seed 데이터 INSERT

### 제외 범위

- 실시간 알림 API (AlertManager, PagerDuty 연동)
- 시맨틱 캐시 (Redis VSEARCH)
- 멀티턴 품질 지표 (Context Retention, Reference Resolution)
- 실제 ingestion 파이프라인 (ZIP → 파싱 → 임베딩 → 인덱싱) — web2rag-poc POC 완료 후 진행
- LLM 비용 집계 전용 API (V027 seed + `/metrics/daily` 재활용으로 대체)

## Impact

### 영향 모듈

- `frontend/src/components/` — 신규 컴포넌트 3개, KpiCard 확장
- `frontend/src/app/ops/`, `client/`, `qa/` — 페이지 6개
- `frontend/src/lib/types.ts` — `RagasEvaluation` 인터페이스 추가
- `apps/admin-api/src/main/kotlin/.../evaluation/` — UseCase, Port, Controller 확장
- `apps/admin-api/src/main/kotlin/.../config/` — ServiceConfiguration, RepositoryConfiguration bean 추가
- `apps/admin-api/src/main/resources/db/migration/` — V027 신규

### 영향 API

- `GET /admin/ragas-evaluations` — 신규
- `GET /admin/questions` — 기존 (프론트에서 최근 5건 조회에 활용)
- `GET /admin/questions/unresolved` — 기존 (client 대시보드 미해결 건수)
- `GET /admin/metrics/daily` — 기존 (ops/client 대시보드)

### 영향 테스트

- `apps/admin-api/src/test/resources/application-test.yml` — `flyway.target: "27"`
- 기존 50개 테스트 전부 유지 (회귀 없음)
- 신규: `GET /admin/ragas-evaluations` 테스트 케이스 추가 권장

## Done Definition

- [ ] `GET /admin/ragas-evaluations` 가 200 + `items[]` 반환
- [ ] Ops 대시보드에서 KPI 5개가 상태 색상 스트라이프와 함께 렌더링됨
- [ ] Ops Quality 페이지에서 RAGAS 스코어카드가 seed 데이터로 표시됨
- [ ] Ops Indexing 페이지 상단에 성공/실패/실행 중 요약 KPI 표시됨
- [ ] Ops Incidents 페이지에 알림 로그 테이블이 표시됨 (더 이상 placeholder 없음)
- [ ] Client 대시보드에 미해결 건수 KpiCard 표시됨
- [ ] QA 대시보드에 RAGAS 점수 + 최근 리뷰 테이블 표시됨
- [ ] V027 마이그레이션이 PostgreSQL 및 H2(테스트) 환경 모두에서 성공
- [ ] `./gradlew :apps:admin-api:test` 전체 통과 (50개 이상)
