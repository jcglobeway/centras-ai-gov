# Status

- 상태: `completed`
- 시작일: `2026-04-01`
- 마지막 업데이트: `2026-04-01`

## Progress

### Phase 1 — 상세 패널 메트릭 보강
- [x] 백엔드 — context_precision/context_recall 추가
- [x] 백엔드 — queryRewriteText/llmMs/postprocessMs 추가
- [x] 프론트엔드 — 상세 패널 메트릭 섹션

### Phase 2 — QA 검수 인라인 등록
- [x] 프론트엔드 — 기존 검수 상태 표시
- [x] 프론트엔드 — QA 검수 폼

## 완료 내역

- 백엔드: `QuestionSummary`, `QuestionContextSummary` 확장 (4개 RAGAS 지표 + latency 세부)
- 백엔드: `GET /admin/questions/{id}/context` 엔드포인트 추가
- 백엔드: `GET /admin/rag-search-logs` 통계 엔드포인트 추가
- 프론트: `chat-history/page.tsx` 전면 개편 (답변·신뢰도·RAGAS·검색 청크·QA 검수 폼)
- 프론트: `quality/page.tsx` RAGAS 추세 차트 + 검색 품질 섹션 추가
- 프론트: `types.ts` RootCauseCode A01~A10으로 정정
