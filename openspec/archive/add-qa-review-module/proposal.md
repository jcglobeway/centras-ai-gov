# Proposal

## Change ID

`add-qa-review-module`

## Summary

### 변경 목적
- QA Review 모듈 핵심 기능 구현
- 상태 머신 (pending → confirmed_issue → resolved) 구현
- QA review 저장 및 조회 API 구현
- 현재는 questions/answers 없이 QA 독립 테스트 가능하도록 스텁으로 구현

### 변경 범위
- **qa-review 모듈**:
  - 도메인 모델 (QAReviewSummary, QAReviewRecord)
  - 상태 머신 (QAReviewStateMachine)
  - 검증 규칙 (confirmed_issue → root_cause + action 필수)
  - Reader/Writer 포트

- **Flyway migration**:
  - V008__create_qa_reviews.sql (seed 데이터 포함)

- **JPA 구현**:
  - QAReviewEntity
  - JpaQAReviewRepository
  - QAReviewReaderAdapter, QAReviewWriterAdapter

- **admin-api**:
  - QAReviewController (POST /admin/qa-reviews, GET /admin/qa-reviews)
  - 권한 검증 (qa.review.read, qa.review.write)

- **테스트**:
  - QA review 생성 테스트
  - 상태 전이 검증 테스트
  - 권한 검증 테스트

### 제외 범위
- questions, answers 테이블 구현 (별도 change)
- unresolved queue 조회 (questions 의존)
- rag_search_logs 연동
- resolution_status 계산 로직 (간소화)
- 실제 후속 조치 실행 (action_target_id 연결)

## Impact

### 영향 모듈
- `modules/qa-review`: 도메인 모델, 상태 머신, 포트
- `apps/admin-api`: QA review API 추가

### 영향 API
- 신규: POST /admin/qa-reviews
- 신규: GET /admin/qa-reviews

### 영향 테스트
- 기존 29개 유지
- 신규 4-5개 추가 예상

## Done Definition

- [x] qa-review 모듈 도메인 모델 정의 (enum, 포트, exception)
- [x] QAReviewStateMachine 구현 (상태 전이 규칙, 검증 로직)
- [x] QAReviewReader, QAReviewWriter 포트
- [x] Flyway V008__create_qa_reviews.sql (FK는 questions 구현 후 추가)
- [x] QAReviewEntity, JpaQAReviewRepository (findByQuestionId)
- [x] QAReviewReaderAdapter, QAReviewWriterAdapter (상태 머신 통합)
- [x] QAReviewController (POST, GET, questionId 필터)
- [x] 권한 검증 (qa.review.read, qa.review.write)
- [x] 테스트 5개 추가
- [x] ./gradlew test 전체 통과 (34 tests)
