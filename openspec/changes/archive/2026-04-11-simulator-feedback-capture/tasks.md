# Tasks: simulator-feedback-capture

## Phase 0 — OpenSpec 정렬
- [x] proposal.md 작성
- [x] tasks.md 작성
- [x] status.md 작성

## Phase 1 — 데이터 경로 정리
- [x] `frontend/src/app/api/simulator/chat/route.ts` done 패킷에 `question_id` 포함
- [x] `ops/simulator` 메시지 메타데이터 타입에 `questionId` 추가

## Phase 2 — 시뮬레이터 피드백 UI
- [x] 답변 메시지에 `👍 / 👎` 버튼 추가
- [x] 선택 시 코멘트 입력(선택) + 저장 CTA 추가
- [x] 저장 중/완료/실패 상태를 메시지별로 표시

## Phase 3 — 피드백 저장 연동
- [x] `POST /api/admin/feedbacks` 호출 구현
- [x] payload: `organizationId/serviceId/sessionId/questionId/rating/comment/channel`
- [x] 중복 제출 방지 로직 추가

## Phase 4 — 검증
- [x] `/ops/simulator`에서 질의→응답→피드백 저장 E2E 확인
- [x] DB에서 `feedbacks.channel='simulator'` 레코드 확인
- [x] 기존 시뮬레이터 스트리밍 동작 회귀 확인

## Phase 5 — 문서화
- [x] 시뮬레이터 피드백 사용법/제약 문서화
