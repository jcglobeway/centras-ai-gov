# Proposal

## Change ID

`correction-impl`

## Summary

- **변경 목적**: `/ops/correction` (답변 교정) 페이지를 목업에서 실제 동작하는 화면으로 전환한다. 프론트 타입 불일치 수정, Ground Truth 저장 API 신규 구현, 교정 이력 실제 연동을 포함한다.
- **변경 범위**:
  - Phase 1 — 프론트 타입 불일치 수정: `Feedback` 인터페이스를 백엔드 `FeedbackResponse` 스펙에 맞게 수정 (`feedbackId` → `id`, `feedbackText` → `comment`), `PagedResponse<T>` → `FeedbackListResponse { items, total }` 타입 교체
  - Phase 2 — V030 마이그레이션: `answer_corrections` 테이블 신규 생성 (chat-runtime 모듈 소속)
  - Phase 3 — 백엔드 헥사고날 스택: chat-runtime 모듈 내 `AnswerCorrection` 도메인 + 포트 + 서비스 + 어댑터 전 레이어 구현, `POST /admin/corrections` / `GET /admin/corrections` API
  - Phase 4 — 프론트 연동: Ground Truth 입력 버튼 활성화, 교정 이력 실제 API 연결, 목업 제거
- **제외 범위**:
  - RAGAS 파인튜닝 데이터셋 자동 내보내기 (별도 change로 추적)
  - 교정 승인/반려 워크플로우 (단순 저장만 구현)
  - 교정 데이터 기반 모델 재학습 파이프라인

## Impact

- **영향 모듈**: `modules/chat-runtime`, `apps/admin-api`, `frontend`
- **영향 API**:
  - `GET /admin/feedbacks` — 응답 필드 변경 없음 (프론트 타입만 수정)
  - `POST /admin/corrections` — 신규
  - `GET /admin/corrections` — 신규
- **영향 테스트**:
  - 신규 통합 테스트: `CorrectionApiTest.kt` (POST + GET 각 1건 이상)
  - 기존 ArchUnit 규칙 준수 여부 확인

## Done Definition

- `GET /admin/feedbacks` 결과가 `/ops/correction` Section 1에 타입 오류 없이 렌더링된다
- `POST /admin/corrections` 호출 시 `answer_corrections` 테이블에 레코드가 저장된다
- `GET /admin/corrections` 호출 시 저장된 교정 이력이 반환된다
- Ground Truth 입력 폼의 "데이터셋에 추가" 버튼이 실제 API를 호출하고 성공 피드백을 표시한다
- 교정 이력 섹션이 `MOCK_CORRECTION_HISTORY` 대신 실제 API 데이터를 표시한다
- ArchUnit 8개 규칙이 모두 통과한다
- 신규 통합 테스트가 Testcontainers 환경에서 통과한다
