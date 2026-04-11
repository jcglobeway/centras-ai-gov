# Status: ops-dashboard-fix

## 현재 단계

구현 완료 — 아카이브 대기

## 진행 상황

- [x] proposal.md 작성
- [x] tasks.md 작성
- [x] 구현 완료
- [ ] 아카이브

## 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `frontend/src/app/ops/page.tsx` | SWR 필터 파라미터 5건 추가, `FeedbackItem.rating` 필드명 수정 |
| `frontend/src/app/ops/simulator/page.tsx` | `adminSessionId` 포함, "지표 집계" 버튼 추가 |
| `frontend/src/app/api/simulator/chat/route.ts` | question 선생성 후 RAG 호출로 FK 위반 해소 |

## 메모

- 백엔드 API 변경 없음 (프론트엔드 전용 수정)
- 필터 미적용 원인: SWR key 문자열에 쿼리파라미터가 빠져 있었고, 백엔드는 파라미터 없이 받으면 전체(all-org) 집계를 반환하기 때문에 필터가 동작하지 않은 것처럼 보였음
- FK 위반 원인: rag-orchestrator가 내부적으로 `rag_search_logs` 에 question_id FK로 INSERT 하는데, 해당 question_id가 admin-api DB에 존재하지 않아 제약 위반 발생
- "지표 집계" 버튼은 기존 `POST /admin/metrics/trigger-aggregation` 엔드포인트를 활용 (신규 API 불필요)
