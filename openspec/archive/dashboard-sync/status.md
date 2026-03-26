# Status

- 상태: `implemented`
- 시작일: `2026-03-24`
- 마지막 업데이트: `2026-03-26`

## Progress

- [x] OpenSpec 문서 작성 (proposal, design, spec, tasks)
- [x] Phase 1: QuestionResponse 확장 (failureReasonCode, questionCategory, isEscalated, answerConfidence)
- [x] Phase 2: 미결질문 answerStatus/latestReviewStatus (UnresolvedRow + findUnresolvedWithStatus + UnresolvedQuestionSummary 체인)
- [x] Phase 3: 프론트 타입 동기화 (Question/UnresolvedQuestion 인터페이스 백엔드 일치)
- [x] Phase 4: 프론트 페이지 필드명 수정 (failureCode→failureReasonCode, ops answerStatus 제거)
- [x] Phase 5: QA Review review_status 필터 (listByStatus 체인 + QAReviewController ?review_status 파라미터)
- [ ] Phase 6: V023 온디맨드 집계 (미구현 — V028 시드 데이터로 대체)
- [x] Phase 7: client/performance 교체 (최근 응대 현황, /admin/questions 사용)

## Verification

- `ChatRuntimeApiTests` PASS (5개) — unresolved 엔드포인트 반환 타입 변경 포함
- `ArchitectureTest` PASS (8개)
- `RagasEvaluationApiTest` PASS (3개)
- pre-existing 실패 10개 (auth/ingestion/E2E/qa)는 이전 커밋부터 존재 — 이번 변경과 무관

## Resolved Risks

- `findUnresolvedWithStatus()` H2 호환: MAX() 서브쿼리 방식으로 구현하여 LIMIT 없이도 안정 동작
- `LoadQuestionPort` 시그니처 변경: LoadQuestionPortAdapter, ListQuestionsService, ListQuestionsUseCase 모두 업데이트 완료
