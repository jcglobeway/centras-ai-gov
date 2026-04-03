# Tasks: unresolved-qa-assignee

## Backend

- [x] `V038__add_assignee_to_qa_reviews.sql` — `qa_reviews.assignee_id VARCHAR(64)` 추가
- [x] `QAReview.kt` — `QAReviewSummary`, `CreateQAReviewCommand`에 `assigneeId: String?` 추가
- [x] `QAReviewEntity.kt` — `assignee_id` 컬럼 매핑, `toSummary()` / `toEntity()` 반영
- [x] `JpaQAReviewRepository.kt` — `@Modifying @Query` native SQL UPDATE 추가
- [x] `UpdateQAReviewAssigneePort.kt` — 아웃바운드 포트 신규
- [x] `AssignQAReviewUseCase.kt` — 인바운드 포트 신규
- [x] `AssignQAReviewService.kt` — 서비스 구현체 신규
- [x] `UpdateQAReviewAssigneePortAdapter.kt` — JPA 어댑터 신규
- [x] `QAReviewController.kt` — `PATCH /admin/qa-reviews/{id}` 엔드포인트, DTO 추가
- [x] `ServiceConfiguration.kt` — `assignQAReviewUseCase` @Bean 등록
- [x] `RepositoryConfiguration.kt` — `updateQAReviewAssigneePort` @Bean 등록
- [x] `QuestionSummary.kt` — `UnresolvedQuestionSummary`에 `latestReviewId`, `latestReviewAssigneeId` 추가
- [x] `JpaQuestionRepository.kt` — SQL 서브쿼리 2개 추가 (latestReviewId, latestReviewAssigneeId)
- [x] `LoadQuestionPortAdapter.kt` — 새 필드 전달
- [x] `QuestionController.kt` — `UnresolvedQuestionResponse`에 `latestReviewId`, `assigneeId` 추가

## Frontend

- [x] `api.ts` — `qaApi.assignReview()`, `adminUserApi.list()` 추가
- [x] `types.ts` — `UnresolvedQuestion`에 `latestReviewId`, `assigneeId` 추가; `AdminUser` 인터페이스 추가
- [x] `qa/unresolved/page.tsx` — 테이블 7컬럼 재편, FAILURE_CODE_LABEL 맵(A01-A10), 리뷰 모달 컨텍스트 패널, 담당자 지정 모달
