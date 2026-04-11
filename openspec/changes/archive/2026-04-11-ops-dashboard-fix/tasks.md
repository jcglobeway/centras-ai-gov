# Tasks: ops-dashboard-fix

## Implementation Tasks

### /ops/page.tsx — 필터 파라미터 누락 수정
- [x] `/api/admin/metrics/llm` SWR 호출에 `organization_id`, `from_date`, `to_date` 추가
- [x] `/api/admin/questions/unresolved` SWR 호출에 `organization_id` 추가
- [x] `/api/admin/metrics/pipeline-latency` SWR 호출에 `organization_id`, `from_date`, `to_date` 추가
- [x] `/api/admin/metrics/pii-count` SWR 호출에 `organization_id` 추가
- [x] `/api/admin/metrics/feedback-trend` SWR 호출에 `organization_id` 추가
- [x] `FeedbackItem` 인터페이스 필드명 `satisfactionScore` → `rating` 수정

### /api/simulator/chat/route.ts — question 선생성 후 RAG 호출
- [x] RAG 호출 전 `POST /admin/questions` 로 question 생성
- [x] 반환된 `questionId` 를 RAG 오케스트레이터 페이로드에 사용

### /ops/simulator/page.tsx — 시뮬레이터 개선
- [x] 질문 전송 요청에 `adminSessionId` 포함
- [x] "지표 집계" 버튼 추가 (`POST /admin/metrics/trigger-aggregation` 온디맨드 호출)

## Testing Tasks

- [x] 국립아시아문화전당 + 1주일 기간 필터 적용 후 KPI 카드 데이터 표시 확인
- [x] 시뮬레이터 질문 전송 후 `rag_search_logs` 레코드 적재 확인 (FK 위반 없음)
- [x] "지표 집계" 버튼 클릭 후 대시보드 반영 확인