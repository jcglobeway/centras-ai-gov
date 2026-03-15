# Test Runner And Folder Structure

## 1. Purpose

이 문서는 `12_test_strategy.md`를 실제 저장소 구조와 실행 단위로 연결하기 위한 기준 문서다.
목적은 다음과 같다.

- 테스트 유형별 디렉터리 경계를 고정한다.
- 어떤 러너가 어떤 책임을 가지는지 정한다.
- fixture, seed, mock, contract data를 어디에 둘지 정한다.
- CI에서 어떤 순서로 테스트를 돌릴지 정한다.

## 2. Recommended Top-Level Structure

```text
tests/
  unit/
    domain/
    auth/
    metrics/
    qa/
    validation/
  api/
    auth/
    dashboard/
    questions/
    qa_reviews/
    documents/
  e2e/
    auth/
    unresolved/
    qa/
    documents/
    dashboard/
  data/
    metrics/
    traceability/
    audit/
  fixtures/
    organizations/
    users/
    questions/
    documents/
    rag/
  seeds/
    base/
    regression/
  helpers/
    api/
    db/
    auth/
    time/
  contracts/
    api/
    events/
```

## 3. Runner Responsibility

### 3.1 Unit Runner

책임:
- 순수 함수 테스트
- 상태 전이 규칙 테스트
- 권한 판정 테스트
- validation 테스트

원칙:
- 외부 네트워크 금지
- DB 접근 금지
- fixture는 최소 JSON 또는 in-memory object 사용

### 3.2 API Runner

책임:
- HTTP 요청/응답 계약 검증
- 인증, 권한, 조직 범위 검증
- persistence 결과 검증

원칙:
- 테스트용 DB 사용
- API 서버를 테스트 모드로 띄운 뒤 실행
- 외부 RAG는 mock adapter 또는 stub provider 사용

### 3.3 E2E Runner

책임:
- 로그인부터 화면 액션까지 실제 사용자 플로우 검증
- unresolved 목록, 질문 상세, QA 저장, 재인덱싱 요청 검증

원칙:
- 브라우저 기반 실행
- seed data 명시
- flaky 요소를 줄이기 위해 비동기 작업은 polling 규칙 표준화

### 3.4 Data Verification Runner

책임:
- 집계 테이블 정합성
- trace_id, request_id, audit_log 연결성
- QA 최신 상태 계산 정합성

원칙:
- 사전 seed 후 검증 쿼리 실행
- API 응답과 DB 결과를 함께 비교

## 4. Folder Ownership

### 4.1 `tests/unit`

대상:
- `09_unresolved_qa_state_machine.md`의 상태 전이 규칙
- `06_access_policy.md`의 권한 액션 판정
- `04_data_api.md`의 요청 validation 및 응답 조립 보조 함수

파일 예시:
- `tests/unit/qa/state-machine.spec.ts`
- `tests/unit/auth/permission-map.spec.ts`
- `tests/unit/metrics/dashboard-metrics.spec.ts`

### 4.2 `tests/api`

대상:
- `10_auth_authz_api.md`
- `04_data_api.md`

파일 예시:
- `tests/api/auth/login.spec.ts`
- `tests/api/questions/unresolved.spec.ts`
- `tests/api/qa_reviews/create.spec.ts`
- `tests/api/documents/reindex.spec.ts`

### 4.3 `tests/e2e`

대상:
- `03_screen_spec.md`
- `08_traceability_matrix.md`
- `11_traceability_test_cases.md`

파일 예시:
- `tests/e2e/auth/admin-login.spec.ts`
- `tests/e2e/unresolved/review-flow.spec.ts`
- `tests/e2e/documents/reindex-authorization.spec.ts`

### 4.4 `tests/data`

대상:
- `04_data_api.md`의 KPI 집계 기준
- `08_traceability_matrix.md`의 trace 복원 기준
- `11_traceability_test_cases.md`의 audit 검증 항목

파일 예시:
- `tests/data/metrics/daily-metrics-consistency.spec.ts`
- `tests/data/traceability/question-detail-trace.spec.ts`
- `tests/data/audit/qa-review-audit.spec.ts`

## 5. Fixture And Seed Rule

### 5.1 Fixtures

용도:
- 테스트 입력 재사용
- 조직, 사용자, 질문, 문서, retrieval 결과 샘플 관리

원칙:
- fixture는 읽기 전용 샘플로 취급
- 한 fixture가 여러 테스트 의미를 동시에 가지지 않게 분리
- 파일명에 기관 범위와 상태를 드러낸다

예시:
- `tests/fixtures/users/ops-admin-a.json`
- `tests/fixtures/questions/org-a-fallback.json`
- `tests/fixtures/rag/retrieval-hit-top3.json`

### 5.2 Seeds

용도:
- API/E2E/Data 테스트용 DB 초기 상태 구성

원칙:
- `base` 는 모든 테스트가 공유하는 최소 조직/사용자/권한 데이터
- `regression` 은 특정 결함 재현용 seed
- seed 실행 후 생성되는 식별자는 deterministic 해야 한다

## 6. Mock And Stub Boundary

원칙:
- 제품 계약 테스트에서는 OpenRAG 자체를 직접 검증하지 않는다.
- `RAG Adapter` 앞 계약만 검증한다.
- 외부 모델, 벡터 DB, 크롤러는 통합 테스트 대상에서 제외하고 stub 처리한다.

권장 분리:
- unit: adapter interface mock
- api: stub retrieval response
- e2e: 고정 응답 fixture 사용
- data: ingestion 결과를 seed 데이터로 대체

## 7. Naming Convention

원칙:
- 파일명은 `기능-행동.spec.ts` 형식 사용
- 테스트 제목은 `역할/조건/기대결과` 순서로 작성
- traceability 케이스와 연결이 필요한 경우 TC id를 제목 또는 주석에 포함

예시:
- `client-scope-isolation.spec.ts`
- `qa-review-invalid-transition.spec.ts`
- `TC-05 qa admin saves confirmed issue and state is updated`

## 8. Execution Order

기본 순서:
1. `unit`
2. `api`
3. `data`
4. `e2e`

이유:
- 정책 로직 실패를 가장 먼저 잡는다.
- API 계약과 데이터 정합성을 브라우저 테스트 전에 고정한다.
- 가장 느리고 flaky한 E2E는 마지막에 실행한다.

## 9. CI Pipeline Recommendation

### 9.1 Pull Request Minimum

- unit
- api
- 선택적 data smoke

### 9.2 Merge To Develop Minimum

- unit
- api
- data
- 핵심 e2e smoke

### 9.3 Release Candidate

- full unit
- full api
- full data
- full e2e
- 수동 점검 체크리스트 병행

## 10. Sprint 1 Minimum Test Layout

필수 파일:
- `tests/unit/qa/state-machine.spec.ts`
- `tests/unit/auth/permission-map.spec.ts`
- `tests/api/auth/login.spec.ts`
- `tests/api/questions/unresolved.spec.ts`
- `tests/api/qa_reviews/create.spec.ts`
- `tests/data/traceability/question-detail-trace.spec.ts`
- `tests/e2e/unresolved/review-flow.spec.ts`

목표:
- 인증
- 조직 격리
- unresolved 노출
- QA 상태 전이
- trace 복원

## 11. Out Of Scope For MVP

- OpenRAG 내부 알고리즘 정확도 벤치마크 자동화
- 대규모 부하 테스트
- 시각 회귀 테스트 전면 도입
- 멀티브라우저 전체 매트릭스

## 12. Next Link

다음 단계에서는 이 문서를 기준으로 실제 저장소용 `테스트 태스크 체크리스트` 또는 `테스트 파일 스캐폴드` 문서를 추가할 수 있다.
