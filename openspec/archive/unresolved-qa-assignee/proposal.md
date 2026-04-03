# Proposal: unresolved-qa-assignee

## Change ID

`unresolved-qa-assignee`

## Summary

- 미해결 질의 페이지(`/qa/unresolved`)를 단순 목록 뷰어에서 실제 이슈 트래커로 전환
- 담당자 지정 기능 추가 (`qa_reviews.assignee_id`)
- 미응답 근거 데이터(`failureReasonCode`, `questionCategory`, `isEscalated`) UI 가시화
- `PATCH /admin/qa-reviews/{id}` 엔드포인트 신규 구현

## Impact

- 영향 모듈: `modules/qa-review`, `modules/chat-runtime`, `apps/admin-api`
- 영향 API: `GET /admin/questions/unresolved`, `POST /admin/qa-reviews`, `PATCH /admin/qa-reviews/{id}`
- 영향 프론트: `frontend/src/app/qa/unresolved/page.tsx`

## Done Definition

- `qa_reviews.assignee_id` 컬럼 추가 (V038 migration)
- `PATCH /admin/qa-reviews/{id}` 엔드포인트 동작
- `GET /admin/questions/unresolved` 응답에 `assigneeId`, `latestReviewId` 포함
- 프론트 테이블에 근거(A01-A10), 카테고리, 담당자 컬럼 표시
- 담당자 지정 모달 동작
