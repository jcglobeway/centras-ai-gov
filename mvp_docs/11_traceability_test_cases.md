# Traceability Test Cases

## 1. Purpose

이 문서는 `08_traceability_matrix.md` 를 실제 검증 시나리오로 전개한 테스트 기준 문서다.
목적은 다음과 같다.

- 화면, 권한, API, DB 연결이 실제 동작으로 성립하는지 검증한다.
- Sprint 1 구현 범위를 최소 E2E 시나리오로 고정한다.
- OpenRAG 사용 여부와 무관하게 제품 계약이 유지되는지 확인한다.

## 2. Scope

포함 범위:
- 관리자 인증과 세션 복원
- 주요 MVP 화면의 조회/저장 흐름
- QA 상태 전이와 문서 재인덱싱 요청
- 대시보드 집계와 원천 로그 정합성

비포함 범위:
- 시각 디자인 검수
- 외부 SSO 상세 연동
- OpenRAG 내부 알고리즘 성능 비교 자체

## 3. Test Axes

모든 테스트는 아래 축을 함께 본다.

- `Screen`: 사용자가 접근하는 화면 또는 사용자 플로우
- `Auth`: 역할, 액션, 조직 범위 검증
- `API`: 요청/응답 계약과 에러 코드
- `DB`: 읽기/쓰기 테이블과 상태 반영
- `Audit`: 감사로그 및 추적 키 기록 여부

## 4. Preconditions

- 최소 시드 데이터 존재:
  - `organizations`
  - `services`
  - `admin_users`
  - `admin_user_roles`
  - `documents`
  - `questions`
  - `answers`
- 역할별 테스트 계정 존재:
  - `ops_admin`
  - `client_admin`
  - `qa_admin`
- 질문 상세 복원에 필요한 `question_id`, `request_id`, `trace_id` 가 적재돼 있어야 한다.

## 5. Core E2E Cases

### TC-01 Admin Login And Session Restore

목적:
- 로그인 후 세션과 권한 컨텍스트가 정상 복원되는지 확인

사전조건:
- `admin_users.status = active`
- 대상 사용자의 활성 역할 존재

절차:
1. `POST /admin/auth/login` 호출
2. 응답 토큰으로 `GET /admin/auth/me` 호출

기대결과:
- 로그인 응답에 `user`, `session`, `authorization` 포함
- `GET /admin/auth/me` 에서 `actions`, `organization_scope` 복원
- `admin_sessions` row 생성
- `audit_logs.action_code = auth.login` 기록

### TC-02 Ops Dashboard Read Success

목적:
- 운영사 관리자가 전체 기관 KPI를 조회할 수 있는지 확인

사전조건:
- `ops_admin` 계정 로그인 완료
- `daily_metrics_org`, `rag_search_logs` 데이터 존재

절차:
1. `GET /admin/ops/dashboard`
2. `GET /admin/rag/search-logs`

기대결과:
- 응답에 전체 기관 범위 데이터 포함
- KPI 값이 `daily_metrics_org` 집계와 일치
- `zero_result_rate`, `fallback_rate` 가 상세 로그와 모순되지 않음

### TC-03 Client Scope Isolation

목적:
- 고객사 관리자가 자기 기관 데이터만 조회 가능한지 확인

사전조건:
- `client_admin` 에 하나의 `organization_id` 할당
- 다른 기관 데이터도 DB에 존재

절차:
1. `GET /admin/client/dashboard`
2. 타 기관 `organization_id` 로 `GET /admin/questions/unresolved` 호출

기대결과:
- 대시보드 응답은 자기 기관 데이터만 반환
- 타 기관 조회는 `403 AUTH_FORBIDDEN_SCOPE` 또는 범위 은닉이 필요한 경우 `404`
- 감사로그에 범위 위반 시도 기록 가능

### TC-04 Unresolved Queue Visibility

목적:
- 미해결 질문 큐가 상태 규칙대로 노출되는지 확인

사전조건:
- `answers.answer_status` 가 각각 `fallback`, `no_answer`, `error`, `answered` 인 데이터 준비
- 일부 `answered` 건에는 `confirmed_issue` 리뷰 존재

절차:
1. `GET /admin/questions/unresolved`
2. 상태 필터별 재조회

기대결과:
- `fallback`, `no_answer`, `error` 는 기본 노출
- `answered` 여도 최신 리뷰가 `confirmed_issue` 이면 노출
- 최신 `resolved`, `false_alarm` 은 기본 목록에서 제외

### TC-05 QA Review Save And State Transition

목적:
- QA 판정 저장 시 상태 전이와 검증 규칙이 맞는지 확인

사전조건:
- 대상 질문의 최신 상태가 `pending`
- `qa_admin` 로그인 완료

절차:
1. `POST /admin/qa-reviews` with `review_status = confirmed_issue`
2. 동일 질문에 `review_status = resolved` 저장

기대결과:
- 1차 저장 시 `root_cause_code`, `action_type` 필수 검증 수행
- `qa_reviews` append-only 저장
- 최신 상태가 `confirmed_issue -> resolved` 로 전이
- `queue_visibility` 값이 함께 계산

### TC-06 Invalid QA Transition Rejected

목적:
- 금지된 QA 전이가 API에서 차단되는지 확인

사전조건:
- 최신 리뷰가 `false_alarm`

절차:
1. `POST /admin/qa-reviews` with `review_status = resolved`

기대결과:
- 요청 거절
- 에러 코드가 상태 전이 위반을 설명
- `qa_reviews` 신규 row 미생성

### TC-07 Question Detail Trace Reconstruction

목적:
- 질문 상세 화면이 trace 기준으로 answer, retrieval, review 를 재구성하는지 확인

사전조건:
- 동일 `question_id` 에 대해 `answers`, `rag_search_logs`, `rag_retrieved_documents`, `qa_reviews` 존재

절차:
1. `GET /admin/questions/{id}`

기대결과:
- 응답에서 retrieval 로그, citations, QA 이력이 함께 조회
- `trace_id` 기준으로 서로 다른 리소스가 연결 가능
- 화면 표시 데이터와 DB 레코드 간 키 불일치가 없음

### TC-08 Document Reindex Request Authorization

목적:
- 재인덱싱 요청과 실행 권한이 역할별로 다르게 적용되는지 확인

사전조건:
- `client_admin`, `qa_admin`, `ops_admin` 계정 준비
- 대상 문서 존재

절차:
1. `client_admin` 으로 `POST /admin/documents/{id}/reindex`
2. `qa_admin` 으로 동일 요청
3. `ops_admin` 으로 실행 요청

기대결과:
- 요청/실행 권한이 액션 코드 기준으로 구분
- 허용 역할만 성공
- `documents.index_status` 와 작업 이력이 규칙대로 변경
- 고위험 액션은 `audit_logs` 기록

### TC-09 Dashboard Metric Consistency

목적:
- 대시보드 KPI 와 원천 데이터 간 정합성 확인

사전조건:
- 동일 기간의 `questions`, `answers`, `qa_reviews`, `daily_metrics_org` 데이터 준비

절차:
1. `GET /admin/client/dashboard`
2. 동일 기간 원천 테이블 직접 검증

기대결과:
- `question_count`, `fallback_rate`, `open_unresolved_count` 정합
- 집계 기준은 `daily_metrics_org` 우선, 상세 드릴다운은 원천 로그 기준

### TC-10 Auth Failure UX Contract

목적:
- 인증 실패와 권한 실패가 API 계약대로 분리되는지 확인

사전조건:
- 만료 세션 토큰 준비
- 액션 불가 계정 준비

절차:
1. 만료 토큰으로 `GET /admin/auth/me`
2. 권한 없는 계정으로 `POST /admin/qa-reviews`

기대결과:
- 만료 토큰은 `401 AUTH_SESSION_EXPIRED`
- 권한 부족은 `403 AUTH_FORBIDDEN_ACTION`
- 화면은 세션 만료와 권한 부족을 서로 다른 UX 분기로 처리 가능

### TC-11 Chat To QA End-To-End

목적:
- 시민 질문부터 QA 처리까지 핵심 운영 루프가 성립하는지 확인

사전조건:
- 검색 실패 또는 fallback 이 발생하는 질문 준비

절차:
1. `POST /chat/questions`
2. 생성된 `question_id` 로 `GET /admin/questions/unresolved`
3. `GET /admin/questions/{id}`
4. `POST /admin/qa-reviews`

기대결과:
- `question_id` 가 전체 단계에서 공통 추적 키로 유지
- 질문, 답변, retrieval, QA 기록이 모두 연결
- unresolved 큐 진입과 이탈이 상태 규칙과 일치

## 6. Negative Cases

- 비활성 사용자 로그인 시 `403 AUTH_USER_DISABLED`
- 역할 미할당 사용자 로그인 시 `403 AUTH_ROLE_NOT_ASSIGNED`
- 다른 기관 문서 상세 접근 시 범위 차단
- `confirmed_issue` 저장 시 `root_cause_code` 누락 요청 거절
- `false_alarm` 저장 시 `action_type != no_action` 요청 거절

## 7. Audit And Observability Checks

- 모든 관리자 API 응답은 `request_id` 추적 가능해야 한다.
- 질문 상세 복원 시 `trace_id` 로 retrieval 로그를 역추적할 수 있어야 한다.
- 로그인, 로그아웃, 권한 실패, 재인덱싱 실행은 `audit_logs` 에 반드시 남아야 한다.

## 8. Sprint 1 Minimum Regression Pack

Sprint 1 최소 회귀팩:
- TC-01
- TC-03
- TC-04
- TC-05
- TC-07
- TC-08
- TC-11

이유:
- 인증, 범위 제한, unresolved 큐, QA 저장, 질문 상세, 문서 운영, 전체 운영 루프를 한 세트로 커버한다.

## 9. OpenRAG Boundary

- OpenRAG 사용 시에도 TC-11 의 판정 기준은 제품 API와 제품 DB다.
- OpenRAG 교체 또는 비활성화 후에도 TC-04, TC-07, TC-09 는 동일하게 통과해야 한다.
- 즉 테스트 기준은 외부 엔진 기능이 아니라 `정규화된 retrieval 결과` 와 `제품 상태 모델` 에 둔다.
