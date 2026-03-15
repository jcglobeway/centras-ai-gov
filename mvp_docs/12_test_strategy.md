# Test Strategy

## 1. Purpose

이 문서는 `11_traceability_test_cases.md` 를 실제 테스트 구현 단위로 분해한 전략 문서다.
목적은 다음과 같다.

- 어떤 시나리오를 어떤 테스트 레벨에서 검증할지 고정한다.
- Sprint 1 최소 자동화 범위를 `unit`, `api`, `e2e`, `data` 로 나눈다.
- OpenRAG 사용 여부와 무관하게 유지돼야 하는 제품 계약 테스트를 분리한다.

## 2. Test Levels

### 2.1 Unit Test

대상:
- 상태 전이 판단 함수
- 권한 액션 검사 함수
- 대시보드 집계 계산 함수
- unresolved 큐 노출 판정 함수
- API 요청 validation 함수

목표:
- 빠르게 실패를 잡는다.
- DB 접근 없이 핵심 정책 로직을 고정한다.

### 2.2 API Test

대상:
- 인증/세션 API
- 질문 조회/상세 API
- QA 저장 API
- 문서 재인덱싱 API
- 대시보드 조회 API

목표:
- 요청/응답 계약, 에러 코드, 권한 미들웨어, 조직 범위를 검증한다.
- 제품 API 계약이 RAG 구현체와 독립적으로 유지되는지 확인한다.

### 2.3 E2E Test

대상:
- 관리자 로그인 후 화면 진입
- unresolved 큐 조회
- 질문 상세 trace 확인
- QA 판정 저장
- 재인덱싱 요청

목표:
- 화면, API, DB, 권한이 연결된 실제 운영 루프를 검증한다.

### 2.4 Data Verification Test

대상:
- KPI 집계 테이블 정합성
- 질문, 답변, QA 이력 연결성
- 감사로그, request_id, trace_id 적재 여부

목표:
- 집계와 추적성이 회귀로 깨지지 않게 한다.

## 3. Mapping From Traceability Cases

### 3.1 Unit First

- TC-04 `Unresolved Queue Visibility`
- TC-05 `QA Review Save And State Transition`
- TC-06 `Invalid QA Transition Rejected`
- TC-09 `Dashboard Metric Consistency`
- TC-10 `Auth Failure UX Contract` 중 에러 분류 함수

### 3.2 API First

- TC-01 `Admin Login And Session Restore`
- TC-03 `Client Scope Isolation`
- TC-05 `QA Review Save And State Transition`
- TC-06 `Invalid QA Transition Rejected`
- TC-08 `Document Reindex Request Authorization`
- TC-10 `Auth Failure UX Contract`

### 3.3 E2E First

- TC-01 `Admin Login And Session Restore`
- TC-04 `Unresolved Queue Visibility`
- TC-07 `Question Detail Trace Reconstruction`
- TC-08 `Document Reindex Request Authorization`
- TC-11 `Chat To QA End-To-End`

### 3.4 Data First

- TC-02 `Ops Dashboard Read Success`
- TC-07 `Question Detail Trace Reconstruction`
- TC-09 `Dashboard Metric Consistency`
- TC-11 `Chat To QA End-To-End`

## 4. Unit Test Design

### 4.1 QA State Machine

검증 포인트:
- `pending -> confirmed_issue` 허용
- `confirmed_issue -> resolved` 허용
- `false_alarm -> resolved` 금지
- `confirmed_issue` 저장 시 `root_cause_code`, `action_type` 필수
- `false_alarm` 저장 시 `action_type = no_action` 강제

### 4.2 Unresolved Visibility Rule

검증 포인트:
- `fallback`, `no_answer`, `error` 는 기본 노출
- `answered` 여도 최신 리뷰가 `confirmed_issue` 이면 노출
- 최신 리뷰가 `resolved`, `false_alarm` 이면 기본 목록 제외

### 4.3 Authorization Rule

검증 포인트:
- 역할별 허용 액션 매핑
- 조직 범위 포함 여부
- `ops_admin` 전기관 범위 허용
- `client_admin` 단일 기관 범위 강제
- `qa_admin` 쓰기 가능한 액션과 읽기 전용 액션 구분

### 4.4 Metric Aggregation Rule

검증 포인트:
- `fallback_rate`
- `open_unresolved_count`
- `reviewed_issue_count`
- 드릴다운 원천 로그와 집계 숫자 일치

## 5. API Test Design

### 5.1 Auth API

대상 API:
- `POST /admin/auth/login`
- `POST /admin/auth/logout`
- `GET /admin/auth/me`

검증 포인트:
- 성공 응답 필드
- 세션 생성/만료 처리
- `401`, `403` 에러 코드 분리
- `audit_logs.action_code` 적재

### 5.2 Questions API

대상 API:
- `GET /admin/questions/unresolved`
- `GET /admin/questions/{id}`

검증 포인트:
- 조직 범위 제한
- 상태 필터
- `trace_id`, `request_id` 포함
- retrieval, citations, qa history 조립

### 5.3 QA Review API

대상 API:
- `POST /admin/qa-reviews`

검증 포인트:
- validation 실패 시 에러 코드
- 금지 전이 차단
- append-only 저장
- 최신 상태 계산 반영

### 5.4 Document Operation API

대상 API:
- `POST /admin/documents/{id}/reindex`

검증 포인트:
- 역할별 요청/실행 권한 차이
- 상태 변경 규칙
- 감사로그 기록

### 5.5 Dashboard API

대상 API:
- `GET /admin/ops/dashboard`
- `GET /admin/client/dashboard`

검증 포인트:
- 범위별 응답 차이
- 집계 필드 정합성
- 필수 KPI 누락 여부

## 6. E2E Test Design

### 6.1 Admin Access Flow

시나리오:
1. 로그인
2. 권한에 맞는 대시보드 진입
3. 권한 없는 화면 직접 접근 차단 확인

### 6.2 Unresolved To QA Flow

시나리오:
1. unresolved 목록 조회
2. 질문 상세 진입
3. retrieval, answer, qa history 확인
4. QA 판정 저장
5. 목록 재조회 후 상태 반영 확인

### 6.3 Document Reindex Flow

시나리오:
1. 문서 상세 진입
2. 재인덱싱 요청
3. 상태 배지 확인
4. 허용되지 않은 역할로 동일 액션 차단 확인

### 6.4 Chat To QA Full Loop

시나리오:
1. 시민 질문 생성
2. fallback 또는 issue 후보 생성
3. admin unresolved 큐 노출
4. QA 판정 저장
5. trace 복원과 상태 이탈 확인

## 7. Data Verification Design

검증 포인트:
- `daily_metrics_org` 수치와 원천 로그 일치
- `question_id` 기준으로 `answers`, `qa_reviews`, retrieval 로그 연결
- 고위험 관리자 액션에 `audit_logs` 존재
- 모든 주요 응답에서 `request_id` 추적 가능

## 8. Sprint 1 Minimum Automation

Sprint 1 자동화 최소 범위:

- Unit
  - QA 상태 전이
  - unresolved 노출 판정
  - 권한 액션 판정
- API
  - 로그인/세션 복원
  - unresolved 목록
  - QA 저장
  - 재인덱싱 권한
- E2E
  - 로그인 -> unresolved -> 질문 상세 -> QA 저장
- Data
  - KPI 집계 정합성 1세트

## 9. Execution Order

1. Unit 테스트로 정책 로직 고정
2. API 테스트로 계약과 권한 검증
3. E2E 테스트로 운영 루프 검증
4. Data 검증으로 집계/추적 정합성 점검

이 순서를 유지해야 실패 원인을 빠르게 좁힐 수 있다.

## 10. OpenRAG Boundary

- OpenRAG 관련 테스트는 retrieval quality 비교용 보조 트랙으로 둔다.
- 본 문서의 핵심 자동화는 제품 API, 제품 DB, 제품 상태 모델 기준으로 유지한다.
- OpenRAG 교체 시에도 `unit`, `api`, `e2e` 계약 테스트는 수정 없이 통과하는 상태를 목표로 한다.
