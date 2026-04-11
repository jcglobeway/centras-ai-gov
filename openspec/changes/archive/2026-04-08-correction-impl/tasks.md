# Tasks

## Phase 1 — 프론트 타입 불일치 수정

- [x] `correction/page.tsx`: `Feedback` 인터페이스를 백엔드 `FeedbackResponse` 스펙으로 교체
  - `feedbackId` → `id`
  - `feedbackText` → `comment`
  - `PagedResponse<Feedback>` → `{ items: FeedbackResponse[], total: number }` 인라인 타입
- [x] `lowRatingFeedbacks` 필터 및 테이블 렌더링 키/필드명 일괄 수정
- [x] 목업 배지(`목업 데이터`) 제거

## Phase 2 — Flyway 마이그레이션

- [x] `V052__create_answer_corrections.sql` 작성 (V030은 Kotlin 마이그레이션이 사용 중)
  - `answer_corrections` 테이블: `id`, `organization_id`, `service_id`, `question_id`, `question_text`, `original_answer_text`, `corrected_answer_text`, `corrected_by`, `correction_reason`, `created_at`

## Phase 3 — 백엔드 헥사고날 스택 (chat-runtime 모듈)

- [x] `domain/AnswerCorrection.kt` — 도메인 데이터 클래스 + `AnswerCorrectionSummary`
- [x] `domain/CreateCorrectionCommand.kt` — 생성 커맨드
- [x] `domain/CorrectionScope.kt` — 스코프 (organizationIds, globalAccess) (AnswerCorrection.kt에 포함)
- [x] `application/port/in/ManageCorrectionUseCase.kt` — create + list 인터페이스
- [x] `application/port/out/LoadCorrectionPort.kt`
- [x] `application/port/out/RecordCorrectionPort.kt`
- [x] `application/service/ManageCorrectionService.kt` — UseCase 구현체
- [x] `adapter/outbound/persistence/AnswerCorrectionEntity.kt` — JPA 엔티티 + `toSummary()`
- [x] `adapter/outbound/persistence/JpaAnswerCorrectionRepository.kt`
- [x] `adapter/outbound/persistence/LoadCorrectionPortAdapter.kt` (`open class`)
- [x] `adapter/outbound/persistence/RecordCorrectionPortAdapter.kt` (`open class`)
- [x] `RepositoryConfiguration.kt` — LoadCorrectionPortAdapter, RecordCorrectionPortAdapter `@Bean` 등록
- [x] `ServiceConfiguration.kt` — ManageCorrectionService `@Bean` 등록

## Phase 4 — 인바운드 어댑터 + 프론트 연동

- [x] `apps/admin-api/.../CorrectionController.kt`
  - `POST /admin/corrections` — Ground Truth 저장
  - `GET /admin/corrections` — 교정 이력 조회 (스코프 기반 필터)
- [x] `correction/page.tsx`: Ground Truth 폼 `onSubmit` 핸들러 구현 (`POST /api/admin/corrections`)
- [x] `correction/page.tsx`: 교정 이력 섹션 — `GET /api/admin/corrections` 실제 연동, `MOCK_CORRECTION_HISTORY` 제거

## Phase 5 — 테스트

- [x] `CorrectionApiTest.kt` 작성
  - `POST /admin/corrections` — 201 Created 확인
  - `GET /admin/corrections` — 목록 조회 확인
- [x] ArchUnit 통과 확인
- [x] Correction 테스트 3개 통과

## Phase 6 — 마무리

- [ ] 커밋 (한국어 메시지)
