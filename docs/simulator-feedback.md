# Simulator Feedback Guide

## 목적

`/ops/simulator`에서 생성된 답변에 대해 운영자가 즉시 품질 피드백을 기록한다.

## 사용 방법

1. `/ops/simulator`에서 기관/서비스를 선택하고 세션을 시작한다.
2. 질문을 전송해 답변을 생성한다.
3. 답변 하단의 `👍` 또는 `👎` 버튼을 누른다.
4. 필요하면 코멘트를 입력하고 `피드백 저장`을 누른다.

## 저장 규칙

- `👍` → 내부 `rating=5`
- `👎` → 내부 `rating=1`
- 코멘트는 선택값
- `channel`은 항상 `simulator`로 저장

## 통계 반영

- 저장된 피드백은 `feedbacks` 테이블에 기록된다.
- Ops 화면의 피드백 통계(`/ops`, `/ops/quality-summary`, `/ops/statistics`, `/ops/reports`) 집계에 포함된다.

## 제약 / 주의사항

- 브라우저 `sessionId`가 만료되면 저장이 실패할 수 있다. 이 경우 재로그인 후 다시 저장한다.
- 질문 생성(`question_id`)이 실패한 경우 피드백 저장이 실패할 수 있다.
- 중복 저장은 동일 답변 기준으로 UI에서 차단된다.
