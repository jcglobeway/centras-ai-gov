# Tasks

## 계획 단계
- [x] 스키마 확인
- [x] 순환 의존성 확인

## Flyway Migration
- [x] V009: chat_sessions + seed (2개)
- [x] V010: questions + seed (3개)
- [x] V011: answers + seed (3개)
- [x] V012: qa_reviews FK 추가 + seed (2개)

## chat-runtime 모듈
- [x] build.gradle.kts JPA 의존성
- [x] ChatRuntimeContracts.kt (enum, 모델, 포트)
- [x] JPA 엔티티 3개 (ChatSession, Question, Answer)
- [x] JPA Repository 2개 (Question, Answer)
- [x] Repository 어댑터 4개 (QuestionReader/Writer, AnswerReader/Writer)
- [x] Native Query로 unresolved queue 구현

## admin-api 통합
- [x] @EnableJpaRepositories, @EntityScan에 chat-runtime 추가
- [x] RepositoryConfiguration에 Bean 4개 등록
- [x] QuestionController (POST, GET, unresolved)

## 테스트
- [x] Question 생성/조회 테스트
- [x] Unresolved queue 테스트
- [x] 기존 테스트 수정 (seed 세션 → login)
- [x] ./gradlew test 통과 (36 tests)

## 마무리
- [x] status.md 완료
- [ ] 99_worklog.md 갱신
- [ ] change를 archive로 이동
- [ ] 커밋
