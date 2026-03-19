# Tasks

## 사전 확인
- [ ] `feedbacks` 테이블 현재 컬럼 확인 (V019 마이그레이션)
- [ ] `FeedbackEntity`, `Feedback` 도메인 모델 위치 확인
- [ ] `FeedbackController` 요청/응답 DTO 구조 확인

## 구현
- [ ] V022 마이그레이션 작성
  - `feedbacks`: `feedback_type`, `clicked_link`, `clicked_document`, `target_action_type`, `target_action_completed`, `dwell_time_ms` 추가
- [ ] `FeedbackEntity` 컬럼 필드 추가
- [ ] `Feedback` 도메인 data class 필드 추가 (nullable)
- [ ] `toSummary()` / `toDomain()` 매퍼 업데이트
- [ ] Feedback 생성 API 요청 body DTO 업데이트 (선택 필드)
- [ ] `application.yml` (test) `spring.flyway.target` → `"22"` 업데이트

## 테스트
- [ ] `./gradlew test` 전체 통과 확인
- [ ] 기존 피드백 시드 데이터 NULL 호환성 확인

## 완료
- [ ] `status.md` 업데이트
- [ ] 커밋
