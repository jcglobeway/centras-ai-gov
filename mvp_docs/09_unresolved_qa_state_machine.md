# Unresolved And QA State Machine

## 1. Purpose

이 문서는 `Unresolved Questions`, `QA Review` 화면에서 사용하는 상태값과 전이 규칙을 고정한다.
목적은 다음과 같다.

- 화면 필터와 백엔드 상태 판단 기준을 통일한다.
- QA 처리 중복과 누락을 줄인다.
- KPI 집계 시 `대기`, `확정`, `해결` 정의를 일관되게 유지한다.

## 2. Scope

적용 범위:
- [03_screen_spec.md](/C:/Users/User/Documents/work/mvp_docs/03_screen_spec.md)의 `Unresolved Questions`
- [03_screen_spec.md](/C:/Users/User/Documents/work/mvp_docs/03_screen_spec.md)의 `QA Review`
- [04_data_api.md](/C:/Users/User/Documents/work/mvp_docs/04_data_api.md)의 `GET /admin/questions/unresolved`
- [04_data_api.md](/C:/Users/User/Documents/work/mvp_docs/04_data_api.md)의 `POST /admin/qa-reviews`

비적용 범위:
- 시민용 채팅 세션 상태
- 문서 ingestion 작업 상태
- 운영사 incident 워크플로우 전체

## 3. Core Entities

핵심 판단 단위:
- `answers.answer_status`
- 최신 QA 판정으로 계산한 `review_status`
- 후속 조치 결과를 포함한 `resolution_status`

운영 원칙:
- 질문 1건에 QA 리뷰는 여러 번 누적될 수 있다.
- 목록 화면의 상태는 최신 리뷰 1건을 기준으로 계산한다.
- 원본 리뷰 이력은 수정하지 않고 append-only로 유지한다.

## 4. Base States

### `answer_status`

질문 응답 직후 시스템이 기록하는 상태:
- `answered`
- `fallback`
- `no_answer`
- `error`

분류 기준:
- `answered`: 근거와 함께 정상 응답
- `fallback`: 근거 부족 또는 정책상 제한으로 일반 안내 응답
- `no_answer`: 검색 결과가 없어 답변 생성 불가
- `error`: 런타임 장애 또는 타임아웃으로 응답 실패

### `review_status`

QA 또는 운영자가 최신 판정으로 부여하는 상태:
- `pending`
- `confirmed_issue`
- `false_alarm`
- `resolved`

분류 기준:
- `pending`: 아직 검수 전이거나 추가 확인 필요
- `confirmed_issue`: 실제 품질 문제로 확정
- `false_alarm`: 실제 문제 없음
- `resolved`: 후속 조치 후 해결 완료

### `resolution_status`

실무 운영 추적용 계산 상태:
- `open`
- `in_progress`
- `done`
- `closed_no_action`

분류 기준:
- `open`: 아직 소유자 지정 또는 조치 시작 전
- `in_progress`: FAQ 작성, 문서 수정, 재인덱싱, 운영 이슈 처리 중
- `done`: 조치가 완료되고 QA가 해결 확인
- `closed_no_action`: 이슈 아님 또는 조치 불필요로 종료

## 5. Derived Exposure Rule

`Unresolved Questions` 화면 노출 규칙:
- `answer_status in (fallback, no_answer, error)` 이면 기본 노출 대상이다.
- `answer_status = answered` 여도 QA가 `confirmed_issue`로 판정하면 노출 대상이다.
- 최신 `review_status = resolved` 또는 `false_alarm` 이면 기본 목록에서는 숨기고, 필터로만 조회한다.

`QA Review` 대기함 규칙:
- 최신 `review_status = pending`
- 또는 최신 `review_status = confirmed_issue` 이면서 `resolution_status != done`

## 6. Transition Rules

### 6.1 Initial Creation

초기 생성 규칙:
- `answers.answer_status = answered` 이고 품질 경고가 없으면 QA 상태를 자동 생성하지 않는다.
- `answers.answer_status in (fallback, no_answer, error)` 이면 내부적으로 `review_status = pending` 대상이 된다.
- retrieval 실패, citation 누락, 정책 차단 등 품질 플래그가 있으면 `answered` 응답도 `pending` 후보로 승격할 수 있다.

### 6.2 Review Transition

허용 전이:
- `pending -> confirmed_issue`
- `pending -> false_alarm`
- `confirmed_issue -> resolved`
- `confirmed_issue -> pending`
- `resolved -> pending`

금지 전이:
- `false_alarm -> resolved`
- `resolved -> false_alarm`

설명:
- `confirmed_issue -> pending` 은 후속 조치 후 재검토가 필요한 경우 허용한다.
- `resolved -> pending` 은 회귀 이슈나 잘못된 해결 판정이 발견된 경우만 허용한다.

### 6.3 Resolution Transition

허용 전이:
- `open -> in_progress`
- `in_progress -> done`
- `open -> closed_no_action`
- `in_progress -> open`
- `done -> in_progress`

설명:
- `done -> in_progress` 는 조치 실패 또는 추가 보완 필요 시 허용한다.
- `closed_no_action` 은 재오픈하려면 새 QA 리뷰를 추가해 `pending` 부터 다시 시작한다.

## 7. Action Mapping Rule

`confirmed_issue` 저장 시 `action_type` 은 아래 중 하나여야 한다.
- `faq_create`
- `document_fix_request`
- `reindex_request`
- `ops_issue`
- `no_action`

매핑 규칙:
- `faq_create`: 답변 가능한데 정형 응답이나 가이드가 부족한 경우
- `document_fix_request`: 원문 문서 최신성, 누락, 표현 수정이 필요한 경우
- `reindex_request`: 문서는 존재하나 chunk 또는 인덱스 반영 문제가 의심되는 경우
- `ops_issue`: 장애, 정책 설정, 권한, 외부 연계 이슈인 경우
- `no_action`: 실제 문제는 맞지만 현재 액션 없이 모니터링만 하는 경우

추가 제약:
- `review_status = false_alarm` 이면 `action_type = no_action` 이어야 한다.
- `review_status = resolved` 는 직전 리뷰 또는 연결된 작업이 존재해야 한다.

## 8. Root Cause Guidance

권장 매핑:
- `missing_document` -> `document_fix_request`
- `stale_document` -> `document_fix_request`
- `bad_chunking` -> `reindex_request`
- `retrieval_failure` -> `reindex_request` 또는 `ops_issue`
- `generation_error` -> `ops_issue`
- `policy_block` -> `ops_issue`
- `unclear_question` -> `faq_create` 또는 `no_action`

예외:
- 실제 운영 상황에 따라 QA는 권장 액션과 다른 액션을 선택할 수 있다.
- 예외 선택 시 `review_comment` 를 필수로 남긴다.

## 9. API Validation Rule

`POST /admin/qa-reviews` 검증 규칙:
- `review_status = confirmed_issue` 이면 `root_cause_code` 필수
- `review_status = confirmed_issue` 이면 `action_type` 필수
- `review_status = false_alarm` 이면 `action_type = no_action`
- `review_status = resolved` 이면 `review_comment` 필수
- 동일 질문에 대해 `resolved` 저장 시 직전 최신 상태가 `confirmed_issue` 또는 `pending` 이어야 한다.

권장 응답 필드:
- `latest_review_status`
- `latest_resolution_status`
- `queue_visibility`

## 10. KPI Counting Rule

집계 기준:
- `pending_count`: 최신 `review_status = pending`
- `confirmed_issue_count`: 최신 `review_status = confirmed_issue`
- `resolved_count`: 기간 내 `resolved` 처리 완료 건
- `false_alarm_count`: 기간 내 `false_alarm` 확정 건
- `open_unresolved_count`: 최신 `resolution_status in (open, in_progress)`

주의:
- QA 리뷰 이력 건수와 질문 건수는 분리해서 집계한다.
- 같은 질문의 재검토는 리뷰 처리량에는 포함되지만 고유 질문 수에는 1건으로 본다.

## 11. UI Badge Rule

`Unresolved Questions` 배지:
- `pending` -> `검수대기`
- `confirmed_issue` + `open` -> `이슈확정`
- `confirmed_issue` + `in_progress` -> `조치중`
- `resolved` + `done` -> `해결완료`
- `false_alarm` + `closed_no_action` -> `오탐종결`

색상 가이드:
- `검수대기`: neutral
- `이슈확정`: danger
- `조치중`: warning
- `해결완료`: success
- `오탐종결`: muted

## 12. OpenRAG Boundary

OpenRAG 사용 여부와 관계없이 상태 전이의 기준 레코드는 제품 DB다.

즉:
- retrieval 로그는 OpenRAG 결과를 받아도 `rag_search_logs` 로 정규화한다.
- QA 상태는 OpenRAG 내부 상태를 직접 참조하지 않는다.
- 외부 엔진 교체 시에도 `review_status`, `resolution_status` 규칙은 유지한다.
