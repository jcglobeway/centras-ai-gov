# Status

- 상태: `in_progress`
- 시작일: `2026-03-24`
- 마지막 업데이트: `2026-03-24`

## Progress

- [x] OpenSpec 문서 작성 (proposal, design, spec, tasks)
- [ ] Phase 1: QuestionResponse 확장
- [ ] Phase 2: 미결질문 answerStatus/latestReviewStatus
- [ ] Phase 3: 프론트 타입 동기화
- [ ] Phase 4: 프론트 페이지 필드명 수정
- [ ] Phase 5: QA Review review_status 필터
- [ ] Phase 6: V023 온디맨드 집계
- [ ] Phase 7: client/performance 교체

## Verification

- 실행 예정:
  ```bash
  JAVA_HOME=.../openjdk-25.0.2/Contents/Home ./gradlew test
  curl -H "X-Admin-Session-Id: $SESSION" http://localhost:8081/admin/questions?page_size=3
  curl -H "X-Admin-Session-Id: $SESSION" http://localhost:8081/admin/questions/unresolved?page_size=3
  curl -H "X-Admin-Session-Id: $SESSION" http://localhost:8081/admin/qa-reviews?review_status=confirmed_issue&page_size=1
  ```

## Risks

- `findUnresolvedWithStatus()` native query H2 호환성 (subquery with LIMIT) — H2 테스트에서 검증 필요
- `LoadQuestionPort` 시그니처 변경으로 인한 모든 구현체 업데이트 필수
