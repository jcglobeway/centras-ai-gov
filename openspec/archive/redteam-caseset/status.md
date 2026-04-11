# Status: redteam-caseset

## 상태: COMPLETED

## 완료 일자

2026-04-07

## 구현 요약

헥사고날 아키텍처 기반으로 `modules/redteam/` Gradle 모듈을 신설하고,
레드팀 케이스셋 CRUD + 일괄 실행 + 결과 조회 기능을 전체 레이어에 걸쳐 구현했다.
프론트엔드 `/ops/redteam` 페이지의 목업 데이터를 실제 API 연동으로 교체했다.

## 구현된 컴포넌트

### DB 마이그레이션
- `V050__create_redteam_cases.sql` — redteam_cases 테이블
- `V051__create_redteam_batch_runs.sql` — redteam_batch_runs, redteam_case_results 테이블

### Gradle 모듈 (modules/redteam/)
- `domain/` — RedteamCase, RedteamBatchRun, RedteamCaseResult, RedteamJudge
- `application/port/in/` — ManageRedteamCaseUseCase, RunRedteamBatchUseCase, ListRedteamBatchRunsUseCase
- `application/port/out/` — LoadRedteamCasePort, SaveRedteamCasePort, LoadRedteamBatchRunPort, SaveRedteamBatchRunPort, SaveRedteamCaseResultPort
- `application/service/` — ManageRedteamCaseService, RunRedteamBatchService, ListRedteamBatchRunsService
- `adapter/outbound/persistence/` — Entity 3종, JpaRepository 3종, PortAdapter 3종

### 컨트롤러 (apps/admin-api/)
- `RedteamCaseController` — POST/GET/PUT/DELETE /admin/redteam/cases
- `RedteamBatchRunController` — POST/GET /admin/redteam/batch-runs, GET /admin/redteam/batch-runs/{id}

### 프론트엔드 (frontend/src/app/ops/redteam/)
- `page.tsx` — 목업 제거, useCases/useBatchRuns API 훅 연결
- `_components/CaseFormModal.tsx` — 케이스 등록/수정 모달
- `_components/BatchRunResultModal.tsx` — 배치런 상세 결과 모달

## 테스트 결과

- RedteamJudge 단위 테스트: 카테고리별 pass/fail 케이스 통과
- RedteamApiTests 통합 테스트: 케이스 CRUD, 배치 실행, 이력 조회 통과
- 기존 통합 테스트 50개 회귀 없음
- ArchUnit 8개 규칙 전체 통과

## 핵심 설계 결정

- 판정 방식: rule-based (LLM 없음) — RegEx + 키워드 매칭으로 응답 분류
- 배치 실행: 동기 방식 (케이스 50개 기준 최대 250초, HTTP 타임아웃 이내)
- case_id FK 미적용: 케이스 삭제 후에도 과거 실행 결과 보존 (query_text 스냅샷)
- RagOrchestrationPort 재사용: ServiceConfiguration에서 기존 Bean 주입 (모듈 간 순환 의존 방지)
