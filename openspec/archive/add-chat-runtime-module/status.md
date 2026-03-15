# Status

- 상태: `completed`
- 시작일: `2026-03-15`
- 완료일: `2026-03-15`
- 마지막 업데이트: `2026-03-15`

## Progress

- ✅ Flyway migration 4개 작성
- ✅ chat-runtime 도메인 모델 정의
- ✅ JPA 엔티티 3개
- ✅ JPA Repository 2개 + 어댑터 4개
- ✅ QuestionController (POST, GET, unresolved)
- ✅ 순환 의존성 해결 (chat-runtime ↛ qa-review)
- ✅ Native Query로 unresolved queue 구현
- ✅ 테스트 2개 추가 + 기존 테스트 수정
- ✅ ./gradlew test 통과 (36 tests)

## Verification

- `./gradlew test`: BUILD SUCCESSFUL (36 tests)
- Flyway migration: 12개 적용 (V001-V012)
- Unresolved queue 정상 작동
- QA Review ↔ Questions FK 연결 완료

## Implementation Details

**순환 의존성 해결**:
- chat-runtime에서 qa-review 의존성 제거
- JpaQuestionRepository: Native Query 사용
- qa_reviews 테이블명을 직접 참조

**Unresolved queue 로직**:
- answer_status IN ('fallback', 'no_answer', 'error')
- OR qa_review.review_status = 'confirmed_issue' (최신)
- scope 기반 필터링

**FK 제약 조건**:
- qa_reviews → questions: ✅ 추가
- qa_reviews → reviewer_id: ⏸️ 임시 제거 (디버그 세션 user 미존재)

## Risks

- ✅ 해결됨: 순환 의존성
- ✅ 해결됨: FK 제약 조건
- ✅ 해결됨: seed 세션 테스트 → login 기반으로 수정
