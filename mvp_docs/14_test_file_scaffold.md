# Test File Scaffold

## 1. Purpose

이 문서는 `12_test_strategy.md`, `13_test_runner_structure.md`를 기준으로
실제 저장소에 어떤 테스트 파일을 먼저 만들지 고정하기 위한 스캐폴드 문서다.

목적은 다음과 같다.

- Sprint 1에서 먼저 만들 테스트 파일 목록을 정한다.
- 각 파일의 최소 검증 책임을 정한다.
- fixture, helper, seed 연결 지점을 명확히 한다.
- 구현팀이 테스트 뼈대를 바로 만들 수 있게 파일명을 고정한다.

## 2. Sprint 1 Minimum Test Files

```text
tests/
  unit/
    auth/
      permission-map.spec.ts
      org-scope.spec.ts
    qa/
      qa-state-machine.spec.ts
      unresolved-classifier.spec.ts
    validation/
      question-query-validator.spec.ts
      qa-review-validator.spec.ts
  api/
    auth/
      login.spec.ts
      me.spec.ts
    questions/
      unresolved-list.spec.ts
      question-detail.spec.ts
    qa_reviews/
      create.spec.ts
      transition-guard.spec.ts
    documents/
      reindex-request.spec.ts
  data/
    traceability/
      question-detail-trace.spec.ts
    audit/
      qa-review-audit.spec.ts
    metrics/
      daily-metrics-consistency.spec.ts
  e2e/
    auth/
      admin-login.spec.ts
    unresolved/
      unresolved-queue-filter.spec.ts
    qa/
      qa-review-flow.spec.ts
    documents/
      reindex-authorization.spec.ts
```

## 3. Unit Test Scaffold

### 3.1 `tests/unit/auth/permission-map.spec.ts`

검증 책임:
- 역할별 허용 액션 매핑이 `06_access_policy.md`와 일치하는지 검증
- `ops_admin`, `client_admin`, `qa_admin` 간 허용/금지 액션을 구분

최소 케이스:
- `ops_admin` 은 기관 관리와 재인덱싱 액션 허용
- `client_admin` 은 자기 기관 KPI 조회 허용, 타 기관 관리 금지
- `qa_admin` 은 검수 저장 허용, 운영 설정 수정 금지

### 3.2 `tests/unit/auth/org-scope.spec.ts`

검증 책임:
- 조직 범위 판정 함수가 자신의 조직과 타 조직을 정확히 구분하는지 검증

최소 케이스:
- 동일 organization_id 요청 허용
- 다른 organization_id 요청 거부
- `ops_admin` 에 한해 멀티 조직 조회 허용

### 3.3 `tests/unit/qa/qa-state-machine.spec.ts`

검증 책임:
- `09_unresolved_qa_state_machine.md`의 허용/금지 전이 규칙 검증

최소 케이스:
- `pending_review -> confirmed_issue` 허용
- `resolved -> pending_review` 금지
- 후속 조치 없이 `resolution_status=resolved` 저장 금지

### 3.4 `tests/unit/qa/unresolved-classifier.spec.ts`

검증 책임:
- 질문이 unresolved 큐에 들어가야 하는 조건을 분류

최소 케이스:
- no-answer 응답은 unresolved 포함
- low-confidence + 사용자 재질문은 unresolved 포함
- solved 명시 응답은 unresolved 제외

### 3.5 `tests/unit/validation/question-query-validator.spec.ts`

검증 책임:
- 질문 목록 조회 필터 validation

최소 케이스:
- 허용된 상태값만 필터 허용
- 날짜 범위 역전 요청 거부
- organization_id 없는 `client_admin` 요청 거부

### 3.6 `tests/unit/validation/qa-review-validator.spec.ts`

검증 책임:
- QA 저장 요청의 필수 필드와 상태 조합 검증

최소 케이스:
- `review_result=confirmed_issue` 일 때 comment 필수
- `resolution_status=resolved` 일 때 action_type 필수
- 잘못된 enum 입력 거부

## 4. API Test Scaffold

### 4.1 `tests/api/auth/login.spec.ts`

검증 책임:
- 로그인 성공/실패 계약
- 세션 생성 여부

최소 케이스:
- 정상 계정 로그인 시 200과 세션 쿠키 반환
- 잘못된 비밀번호 시 401 반환
- 비활성 사용자 시 403 반환

### 4.2 `tests/api/auth/me.spec.ts`

검증 책임:
- 현재 로그인 사용자 정보와 액션 목록 반환

최소 케이스:
- 로그인 상태에서 role, organization scope 반환
- 비로그인 상태 401 반환

### 4.3 `tests/api/questions/unresolved-list.spec.ts`

검증 책임:
- unresolved 목록 조회 계약
- 조직 범위와 필터 적용 검증

최소 케이스:
- `client_admin` 은 자기 기관 unresolved만 조회
- `ops_admin` 은 전체 기관 필터 조회 가능
- 잘못된 상태 필터는 400 반환

### 4.4 `tests/api/questions/question-detail.spec.ts`

검증 책임:
- 질문 상세 trace 복원 계약

최소 케이스:
- question, answer, retrieved documents, qa summary 포함
- 타 기관 질문 접근 시 403 반환

### 4.5 `tests/api/qa_reviews/create.spec.ts`

검증 책임:
- QA 검수 저장 계약

최소 케이스:
- 정상 저장 시 review row와 audit row 생성
- 필수 comment 누락 시 400 반환

### 4.6 `tests/api/qa_reviews/transition-guard.spec.ts`

검증 책임:
- 금지 상태 전이 차단

최소 케이스:
- 허용되지 않은 review 상태 변경 409 반환
- 이미 resolved 된 건 재오픈 정책 없으면 거부

### 4.7 `tests/api/documents/reindex-request.spec.ts`

검증 책임:
- 재인덱싱 요청 권한 및 생성 계약

최소 케이스:
- `ops_admin` 재인덱싱 요청 성공
- `qa_admin` 요청 403 반환
- 요청 성공 시 job row와 audit row 생성

## 5. Data Verification Scaffold

### 5.1 `tests/data/traceability/question-detail-trace.spec.ts`

검증 책임:
- 질문 상세 API 응답과 DB trace 연결성 검증

최소 케이스:
- API 응답의 retrieved documents 수와 DB row 수 일치
- trace_id, question_id, answer_id 연결 복원 가능

### 5.2 `tests/data/audit/qa-review-audit.spec.ts`

검증 책임:
- QA 저장 시 감사로그 정합성 검증

최소 케이스:
- actor_id, action_code, target_id 저장
- before/after 상태 차이가 정확히 남는지 검증

### 5.3 `tests/data/metrics/daily-metrics-consistency.spec.ts`

검증 책임:
- daily metrics 집계가 원시 질문/답변 로그와 일치하는지 검증

최소 케이스:
- total questions 합계 일치
- unresolved count 일치
- zero-result rate 계산 일치

## 6. E2E Test Scaffold

### 6.1 `tests/e2e/auth/admin-login.spec.ts`

검증 책임:
- 관리자 로그인 진입과 기본 권한 로딩

최소 케이스:
- 로그인 성공 후 역할별 시작 화면 노출
- 비권한 메뉴 비노출

### 6.2 `tests/e2e/unresolved/unresolved-queue-filter.spec.ts`

검증 책임:
- unresolved 큐 필터, 조직 범위, 상태 배지 동작

최소 케이스:
- `client_admin` 로그인 시 자기 기관 건만 노출
- 상태 필터 적용 시 목록과 카운트 동기화

### 6.3 `tests/e2e/qa/qa-review-flow.spec.ts`

검증 책임:
- unresolved 목록에서 질문 상세 진입 후 QA 저장까지 전체 플로우 검증

최소 케이스:
- 질문 상세에서 근거 문서 확인 가능
- QA 저장 후 목록 상태와 상세 상태 동시 갱신

### 6.4 `tests/e2e/documents/reindex-authorization.spec.ts`

검증 책임:
- 재인덱싱 버튼 노출/비노출과 실행 결과 검증

최소 케이스:
- `ops_admin` 만 버튼 노출
- 실행 후 성공 토스트와 job 상태 반영

## 7. Helper And Fixture Pairing

권장 연결:
- `tests/helpers/auth/session-helper.ts`
로그인, 쿠키 재사용, 역할 전환 helper

- `tests/helpers/api/request-helper.ts`
공통 헤더, trace_id, 조직 헤더 주입 helper

- `tests/helpers/db/assertions.ts`
audit, metrics, trace row 검증 helper

- `tests/fixtures/users/*.json`
역할별 사용자 계정 fixture

- `tests/fixtures/questions/*.json`
resolved, unresolved, zero-result 질문 fixture

- `tests/fixtures/documents/*.json`
문서 상태와 소스 유형 fixture

- `tests/seeds/base/*.sql`
공통 조직, 사용자, 권한, 문서 seed

- `tests/seeds/regression/*.sql`
상태 전이 오류, 집계 오류 재현 seed

## 8. Recommended Creation Order

1. `tests/unit/auth/permission-map.spec.ts`
2. `tests/unit/qa/qa-state-machine.spec.ts`
3. `tests/api/auth/login.spec.ts`
4. `tests/api/questions/unresolved-list.spec.ts`
5. `tests/api/qa_reviews/create.spec.ts`
6. `tests/data/traceability/question-detail-trace.spec.ts`
7. `tests/e2e/qa/qa-review-flow.spec.ts`

## 9. Definition Of Ready

- 각 파일은 대응 문서 링크를 상단 주석으로 가진다.
- fixture 이름만 보고도 조직, 역할, 상태를 알 수 있어야 한다.
- API 테스트는 성공 케이스와 실패 케이스를 최소 1개씩 포함한다.
- E2E 테스트는 단일 사용자 여정으로 읽혀야 한다.
- Data 테스트는 API 응답과 DB 검증을 함께 포함한다.
