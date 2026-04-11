# Tasks: redteam-caseset

## Phase 1: DB 마이그레이션

- [x] V050__create_redteam_cases.sql 작성
      - redteam_cases 테이블 (id, organization_id, category, title, query_text, expected_behavior, is_active, created_by, created_at, updated_at)
- [x] V051__create_redteam_batch_runs.sql 작성
      - redteam_batch_runs 테이블 (id, organization_id, triggered_by, status, total_cases, pass_count, fail_count, pass_rate, started_at, completed_at)
      - redteam_case_results 테이블 (id, batch_run_id FK, case_id, query_text, response_text, answer_status, judgment, judgment_detail, executed_at)

## Phase 2: Gradle 모듈 신설 (modules/redteam/)

- [x] `modules/redteam/build.gradle.kts` 생성
      - spring-boot-starter-data-jpa, jakarta.persistence 의존성 추가
- [x] `settings.gradle.kts`에 `include(":modules:redteam")` 추가
- [x] `apps/admin-api/build.gradle.kts`에 `implementation(project(":modules:redteam"))` 추가

## Phase 3: 도메인 레이어 (modules/redteam/domain/)

- [x] `RedteamCase.kt` — 도메인 모델 data class (RedteamCategory, RedteamExpectedBehavior enum 포함)
- [x] `RedteamBatchRun.kt` — 도메인 모델 data class (BatchRunStatus enum 포함)
- [x] `RedteamCaseResult.kt` — 도메인 모델 data class (RedteamJudgment enum 포함)
- [x] `RedteamJudge.kt` — rule-based 판정 object
      - pii_induction: 정규식 패턴 매칭 (주민번호, 전화번호) + answerStatus 체크
      - out_of_domain: answerStatus in {no_answer, fallback, error} 체크
      - prompt_injection: 키워드 매칭 + answerStatus 체크
      - harmful_content: answerStatus 체크 (기본 정책)

## Phase 4: 아웃바운드 포트 (modules/redteam/application/port/out/)

- [x] `LoadRedteamCasePort.kt` — findById, findAllByOrganizationId, findActiveByOrganizationId
- [x] `SaveRedteamCasePort.kt` — save(case), update(case), delete(id)
- [x] `LoadRedteamBatchRunPort.kt` — findById, findAllByOrganizationId(limit=20)
- [x] `SaveRedteamBatchRunPort.kt` — save(run), updateResult(runId, passCount, failCount, passRate, completedAt, status)
- [x] `SaveRedteamCaseResultPort.kt` — save(result)

## Phase 5: 인바운드 포트 (modules/redteam/application/port/in/)

- [x] `ManageRedteamCaseUseCase.kt`
      - createCase(command): RedteamCaseSummary
      - updateCase(id, command): RedteamCaseSummary
      - deleteCase(id)
      - listCases(organizationId): List<RedteamCaseSummary>
- [x] `RunRedteamBatchUseCase.kt`
      - runBatch(organizationId, triggeredBy): RedteamBatchRunSummary
- [x] `ListRedteamBatchRunsUseCase.kt`
      - listRuns(organizationId): List<RedteamBatchRunSummary>
      - getRunDetail(runId): RedteamBatchRunDetail

## Phase 6: 서비스 구현 (modules/redteam/application/service/)

- [x] `ManageRedteamCaseService.kt` — LoadRedteamCasePort + SaveRedteamCasePort 주입
- [x] `RunRedteamBatchService.kt` — 배치 실행 로직
      - LoadRedteamCasePort + SaveRedteamBatchRunPort + SaveRedteamCaseResultPort + RagOrchestrationPort 주입
      - 케이스 0개 시 IllegalStateException
      - 케이스별 RAG 질의 → RedteamJudge.judge() → 결과 저장
- [x] `ListRedteamBatchRunsService.kt` — LoadRedteamBatchRunPort 주입

## Phase 7: 퍼시스턴스 어댑터 (modules/redteam/adapter/outbound/persistence/)

- [x] `RedteamCaseEntity.kt` — @Entity, toSummary(), toEntity() 매퍼
- [x] `RedteamBatchRunEntity.kt` — @Entity, toSummary() 매퍼
- [x] `RedteamCaseResultEntity.kt` — @Entity, toSummary() 매퍼
- [x] `JpaRedteamCaseRepository.kt` — @Repository Spring Data JPA
      - findAllByOrganizationIdOrderByCreatedAtDesc
      - findAllByOrganizationIdAndIsActiveTrueOrderByCreatedAtDesc
- [x] `JpaRedteamBatchRunRepository.kt`
      - findAllByOrganizationIdOrderByStartedAtDesc (Pageable top 20)
- [x] `JpaRedteamCaseResultRepository.kt`
      - findAllByBatchRunIdOrderByJudgmentAsc (fail 먼저)
- [x] `RedteamCasePortAdapter.kt` — open class, LoadRedteamCasePort + SaveRedteamCasePort 구현
- [x] `RedteamBatchRunPortAdapter.kt` — open class, LoadRedteamBatchRunPort + SaveRedteamBatchRunPort 구현
- [x] `RedteamCaseResultPortAdapter.kt` — open class, SaveRedteamCaseResultPort 구현

## Phase 8: 인바운드 컨트롤러 (apps/admin-api/redteam/adapter/inbound/web/)

- [x] `RedteamCaseController.kt`
      - POST /admin/redteam/cases → createCase
      - GET /admin/redteam/cases?organizationId=xxx → listCases
      - PUT /admin/redteam/cases/{id} → updateCase
      - DELETE /admin/redteam/cases/{id} → deleteCase
      - 세션에서 userId 추출, createCase command에 createdBy 설정
- [x] `RedteamBatchRunController.kt`
      - POST /admin/redteam/batch-runs → runBatch
      - GET /admin/redteam/batch-runs?organizationId=xxx → listRuns
      - GET /admin/redteam/batch-runs/{id} → getRunDetail

## Phase 9: Bean 등록

- [x] `RepositoryConfiguration.kt`에 추가
      - redteamCasePortAdapter (JpaRedteamCaseRepository 주입)
      - redteamBatchRunPortAdapter (JpaRedteamBatchRunRepository + JpaRedteamCaseResultRepository 주입)
      - redteamCaseResultPortAdapter (JpaRedteamCaseResultRepository 주입)
- [x] `ServiceConfiguration.kt`에 추가
      - manageRedteamCaseUseCase
      - runRedteamBatchUseCase (ragOrchestrationPort Bean 주입)
      - listRedteamBatchRunsUseCase

## Phase 10: 프론트엔드 연동 (frontend/src/app/ops/redteam/)

- [x] `page.tsx` 목업 데이터 제거, 실제 API 훅으로 교체
      - useCases() — GET /api/admin/redteam/cases?organizationId=...
      - useBatchRuns() — GET /api/admin/redteam/batch-runs?organizationId=...
- [x] 케이스 목록 테이블: 실데이터 렌더링, 수정/삭제 버튼 활성화
- [x] `_components/CaseFormModal.tsx` — 케이스 등록/수정 모달
      - category 셀렉트 (4종 한글 레이블)
      - title, queryText 입력
      - expectedBehavior 라디오 (방어/탐지)
      - 저장 → POST or PUT → 목록 갱신
- [x] 일괄 실행 버튼 활성화
      - 클릭 → POST /api/admin/redteam/batch-runs → 로딩 상태 표시
      - 완료 후 passRate 원형 차트 실데이터 갱신, 이력 목록 갱신
- [x] `_components/BatchRunResultModal.tsx` — 실행 결과 상세 모달
      - GET /api/admin/redteam/batch-runs/{id}
      - 케이스별 판정 결과 테이블 (fail 먼저 정렬)
- [x] 실행 이력 테이블: 실데이터 렌더링 + 상세 보기 버튼

## Testing Tasks

- [x] `RedteamJudge.kt` 단위 테스트 (순수 함수이므로 JUnit5만으로 가능)
      - 각 카테고리별 pass/fail 케이스 최소 2개씩
- [x] `RedteamApiTests.kt` 통합 테스트 작성
      - 케이스 등록 → 조회 → 수정 → 삭제 흐름
      - 일괄 실행 (RAG orchestrator 없이, null 응답 처리 경로)
      - 배치런 이력 조회
- [x] 기존 50개 통합 테스트 회귀 확인
      - `JAVA_HOME=... ./gradlew test` 전체 실행 통과
      - (기존 QAReviewApiTests 1건은 seed data 충돌로 인한 기존 문제)
- [x] ArchUnit 8개 규칙 통과 확인

## 완료 기준 체크

- [x] `/ops/redteam` 페이지에서 "목업 데이터" 배지 미노출
- [x] 케이스 등록 모달에서 저장 후 목록에 반영
- [x] "전체 케이스 실행" 버튼 클릭 시 실제 배치 실행 후 방어율 갱신
- [x] 실행 이력 테이블에 실제 배치런 데이터 표시
