# Tasks

## 계획 단계
- [x] mvp_docs/09_unresolved_qa_state_machine.md 확인
- [x] mvp_docs/04_data_api.md qa_reviews 스키마 확인
- [x] 상태 전이 규칙 파악

## qa-review 모듈 도메인
- [x] QAReviewContracts.kt 생성
  - QAReviewStatus enum (pending, confirmed_issue, false_alarm, resolved)
  - RootCauseCode enum (7개)
  - ActionType enum (5개)
  - QAReviewSummary 모델
  - CreateQAReviewCommand
  - QAReviewReader, QAReviewWriter 포트
  - InvalidQAReviewException
- [x] QAReviewStateMachine.kt 생성
  - validateReview: confirmed_issue → root_cause + action 필수
  - validateReview: false_alarm → action_type = no_action 강제
  - validateReview: resolved → review_comment 필수
  - validateTransition: false_alarm ↔ resolved 금지

## qa-review 환경 설정
- [x] modules/qa-review/build.gradle.kts에 JPA 의존성 추가
  - kotlin-spring, kotlin-jpa plugin
  - JPA, Spring 의존성

## Flyway Migration
- [x] V008__create_qa_reviews.sql
  - 테이블 생성
  - 인덱스 추가 (question_id, review_status, reviewer_id, reviewed_at)
  - seed 데이터 2개 (confirmed_issue, resolved)
  - FK는 questions 구현 후 추가 예정

## qa-review JPA 구현
- [x] QAReviewEntity.kt (toSummary, toEntity, enum 변환)
- [x] JpaQAReviewRepository.kt (findByQuestionIdOrderByReviewedAtDesc)
- [x] QAReviewReaderAdapter.kt (listReviews, listAllReviews)
- [x] QAReviewWriterAdapter.kt (createReview, 상태 머신 통합)
- [x] `open class` 설정

## admin-api 통합
- [x] @EnableJpaRepositories, @EntityScan에 qa-review 추가
- [x] RepositoryConfiguration에 qa-review Bean 2개 등록
- [x] QAReviewController 생성 (POST, GET)
- [x] 권한 검증 (qa.review.read, qa.review.write)
- [x] enum 변환 함수

## 테스트
- [x] QA review 생성 테스트 (confirmed_issue)
- [x] root_cause 필수 검증 테스트
- [x] false_alarm action_type 제약 테스트
- [x] questionId 필터링 조회 테스트
- [x] 권한 검증 테스트 (qa_admin vs client_admin)
- [x] ./gradlew test 전체 통과 (34 tests)

## 마무리
- [ ] 99_worklog.md 갱신
- [ ] status.md 완료 상태로 갱신
- [ ] proposal.md Done Definition 업데이트
- [ ] change를 archive로 이동
- [ ] 커밋 (한글 메시지)
