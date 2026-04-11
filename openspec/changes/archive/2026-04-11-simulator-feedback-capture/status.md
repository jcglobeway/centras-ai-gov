# Status: simulator-feedback-capture

- 상태: `implemented`
- 시작일: `2026-04-09`
- 마지막 업데이트: `2026-04-09`

## Progress

- OpenSpec change 초안 작성 완료
- 기존 feedback API 재사용 가능성 확인 완료
- 구현 완료:
  - simulator chat done 패킷에 `question_id` 보장
  - 답변별 `👍/👎` 피드백 UI 추가
  - 코멘트(선택) + 저장/실패/완료 상태 메시지별 표시
  - `POST /admin/feedbacks` 연동(`channel=simulator`)
  - 운영 가이드 문서 추가: `docs/simulator-feedback.md`

## Verification

- `/admin/feedbacks` 생성 API 입력 스키마 확인
- `/ops/simulator` 세션/채팅 흐름 코드 확인
- `simulator/chat` route에서 `questionId` 생성 흐름 확인
- E2E 확인:
  - 세션 생성: `POST /api/admin/simulator/sessions` → `201`
  - 채팅 응답: `POST /api/simulator/chat` done 이벤트에 `question_id` 포함 확인
  - 피드백 저장: `POST /api/admin/feedbacks` → `201`
  - DB 확인: `feedbacks.channel='simulator'`, `question_id`/`session_id` 저장 확인

## Risks

- 프론트 로컬 세션(`sessionId`) 만료 시 저장 실패 가능 (재로그인 필요)
- 화면 가독성 유지를 위해 피드백 UI를 더 압축할 여지 있음
