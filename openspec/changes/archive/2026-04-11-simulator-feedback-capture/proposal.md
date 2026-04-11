# Proposal: simulator-feedback-capture

## Problem

`/ops/simulator`는 답변 생성/조회는 가능하지만, 해당 답변에 대한 운영자 피드백(좋아요/싫어요, 코멘트)을 즉시 기록할 수 없다.  
현재 피드백 데이터는 별도 흐름에서만 수집되어, 시뮬레이터에서 테스트한 답변 품질 신호가 누락된다.

## Proposed Solution

1. 시뮬레이터 응답 버블에 피드백 UI를 추가한다.
   - `👍` / `👎` 버튼
   - 선택 시 선택적 코멘트 입력 후 저장
2. 저장 시 기존 `POST /admin/feedbacks` API를 호출해 피드백을 기록한다.
   - `organizationId`, `serviceId`, `sessionId`, `questionId`, `rating`, `comment`, `channel="simulator"`
3. 질문 생성 경로에서 생성된 `questionId`를 시뮬레이터 클라이언트가 받을 수 있도록 NDJSON done 패킷에 `question_id`를 포함한다.
4. 저장 완료/실패를 메시지 단위 상태로 표시한다. (중복 전송 방지 포함)

## Out of Scope

- 피드백 수정/삭제
- 별도 피드백 집계 대시보드 개편
- thumbs 외 추가 반응 이모지

## Impact

- 영향 프론트:
  - `frontend/src/app/ops/simulator/page.tsx`
  - `frontend/src/app/api/simulator/chat/route.ts`
- 영향 백엔드:
  - 신규 API 없음 (`/admin/feedbacks` 재사용)
- 영향 테스트:
  - 시뮬레이터 피드백 제출 플로우 UI 테스트(또는 수동 E2E)
  - NDJSON done 패킷 필드(`question_id`) 회귀 확인

## Success Criteria

- 시뮬레이터에서 답변 단위로 `좋아요/싫어요`를 남길 수 있다.
- 피드백 저장 시 `feedbacks` 테이블에 `channel=simulator`로 기록된다.
- 동일 답변에서 중복 제출이 방지되고, 저장 결과가 UI에 표시된다.
