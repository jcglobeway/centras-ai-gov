# Tasks

## 사전 확인
- [ ] `questions`, `chat_sessions` 테이블 현재 컬럼 확인 (V010, V009 마이그레이션)
- [ ] `QuestionEntity`, `ChatSessionEntity` 위치 확인
- [ ] `Question`, `ChatSession` 도메인 모델 구조 확인
- [ ] `QuestionController` 응답 형태 확인

## 구현
- [ ] V021 마이그레이션 작성
  - `questions`: `question_category`, `answer_confidence`, `failure_reason_code`, `is_escalated` 추가
  - `chat_sessions`: `session_end_type`, `total_question_count` 추가
- [ ] `QuestionEntity` 컬럼 필드 추가
- [ ] `ChatSessionEntity` 컬럼 필드 추가
- [ ] `Question` 도메인 data class 필드 추가 (nullable)
- [ ] `ChatSession` 도메인 data class 필드 추가 (nullable)
- [ ] `toSummary()` / `toDomain()` 매퍼 업데이트
- [ ] `application.yml` (test) `spring.flyway.target` → `"21"` 업데이트

## 테스트
- [ ] `./gradlew test` 전체 통과 확인
- [ ] 기존 시드 데이터로 NULL 컬럼 기본값 동작 확인

## 완료
- [ ] `status.md` 업데이트
- [ ] 커밋
