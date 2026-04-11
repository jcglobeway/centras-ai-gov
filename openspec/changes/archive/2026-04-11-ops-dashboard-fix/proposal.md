# Proposal: ops-dashboard-fix

## Problem

운영사 대시보드(`/ops`) 에서 기관(organization_id)과 기간(from_date, to_date) 필터를 적용해도
KPI 카드 전체가 "-" 로 표시되어 실데이터가 반영되지 않는 문제가 있었다.

또한 시뮬레이터(`/ops/simulator`)에서 질문을 전송할 때 `rag_search_logs_question_id_fkey`
FK 위반이 발생하여 search log가 적재되지 않았고, 이로 인해 집계 데이터가 누락되었다.

### 세부 원인

1. `/ops/page.tsx` 의 여러 SWR 호출이 `organization_id`, `from_date`, `to_date` 파라미터를
   쿼리스트링에 포함하지 않아 백엔드 필터링이 동작하지 않았다.

2. `FeedbackItem` 인터페이스의 만족도 필드명이 `satisfactionScore` 로 정의되어 있었으나
   백엔드 실제 응답 필드명은 `rating` 이어서 값을 읽지 못했다.

3. 시뮬레이터의 RAG 호출 Route Handler(`/api/simulator/chat/route.ts`)가
   `/admin/questions` 로 question을 먼저 생성하지 않고 RAG 오케스트레이터를 직접 호출하여
   존재하지 않는 `question_id` 로 `rag_search_logs` 에 INSERT를 시도해 FK 위반이 발생했다.

4. 시뮬레이터에서 집계를 수동으로 트리거할 수단이 없어 대화 후 대시보드에 즉시 반영되지 않았다.

## Proposed Solution

1. `/ops/page.tsx` — 누락된 필터 파라미터를 각 SWR 호출에 추가한다.
   - `/api/admin/metrics/llm`: `organization_id`, `from_date`, `to_date`
   - `/api/admin/questions/unresolved`: `organization_id`
   - `/api/admin/metrics/pipeline-latency`: `organization_id`, `from_date`, `to_date`
   - `/api/admin/metrics/pii-count`: `organization_id`
   - `/api/admin/metrics/feedback-trend`: `organization_id`

2. `/ops/page.tsx` — `FeedbackItem.satisfactionScore` 필드명을 `rating` 으로 수정한다.

3. `/api/simulator/chat/route.ts` — RAG 오케스트레이터 호출 전에
   `POST /admin/questions` 로 question을 먼저 생성하고 반환된 `questionId` 를
   RAG 호출 페이로드에 사용한다.

4. `/ops/simulator/page.tsx`
   - 질문 전송 시 `adminSessionId` 를 포함하도록 수정한다.
   - "지표 집계" 버튼을 추가하여 `POST /admin/metrics/trigger-aggregation` 을
     온디맨드로 호출할 수 있게 한다.

## Out of Scope

- 백엔드 API 변경
- 다른 포털(client, qa) 필터 파라미터 점검
- 시뮬레이터 UI 전면 개편

## Success Criteria

- 국립아시아문화전당 기관 + 1주일 기간 필터 적용 시 KPI 카드에 실데이터가 표시된다.
- 시뮬레이터에서 질문 전송 후 `rag_search_logs` 에 레코드가 정상 적재된다.
- "지표 집계" 버튼 클릭 시 집계가 즉시 트리거되고 대시보드에 반영된다.
