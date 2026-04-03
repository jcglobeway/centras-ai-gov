# Status

- 상태: `completed`
- 시작일: `2026-04-02`
- 마지막 업데이트: `2026-04-02`

## Progress

- [x] Phase 1 — 사전 확인 (admin-api :8081 ✅, rag-orchestrator :8090 ✅)
- [x] Phase 2 — 평가 데이터 준비 (30건, ground_truth 포함)
- [x] Phase 3 — 질문 투입 (30건, contexts 5개/건 평균)
- [x] Phase 4 — RAGAS 평가 완료 (50건)
- [x] Phase 5 — 대시보드 검증 가능 상태

## 결과

| 지표 | 건수 | 평균 |
|------|------|------|
| Faithfulness | 49/50 | 0.697 |
| Answer Relevancy | 47/50 | 0.540 |
| Context Precision | 50/50 | 0.448 |
| Context Recall | 50/50 | 0.640 |

## 부수 수정

- `query_runner.py`: `fetch_rag_search_logs` → `GET /admin/questions/{id}/context` 사용으로 변경
  (기존 `GET /admin/rag-search-logs`는 stats 집계 API로 question_id 파라미터 미지원)
- `eval-runner/.env`: 세션 토큰 갱신
