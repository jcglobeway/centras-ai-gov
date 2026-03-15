# Screen Access And Action Policy

이 문서는 화면/액션 기준 권한 정책을 정의한다.
인증 흐름, 세션 토큰, 사용자/역할 API 계약은 `10_auth_authz_api.md`에서 별도로 관리한다.

## 1. MVP Roles

- `ops_admin`
- `client_admin`
- `qa_admin`

## 2. Access Policy By Screen

### Operations Dashboard

- `ops_admin`: 조회, 이슈 확인, 재처리 실행 가능
- `client_admin`: 접근 불가
- `qa_admin`: 읽기 전용 또는 접근 불가

### Organization Management

- `ops_admin`: 전체 조회 및 수정 가능
- `client_admin`: 자기 기관 읽기 일부 허용 가능
- `qa_admin`: 접근 불가

### Document Source Management

- `ops_admin`: 전체 조회, 재수집, 재인덱싱 가능
- `client_admin`: 자기 기관 문서 조회, 수정 요청 또는 제한적 등록
- `qa_admin`: 문서 상태 읽기 가능

### Client Dashboard

- `ops_admin`: 전체 기관 조회 가능
- `client_admin`: 자기 기관만 조회 가능
- `qa_admin`: 필요 시 읽기 전용

### Question Analysis / Unresolved Questions

- `ops_admin`: 전체 조회 가능
- `client_admin`: 자기 기관만 조회 및 개선 요청 가능
- `qa_admin`: 전체 또는 배정 범위 조회 가능

### QA Review

- `ops_admin`: 상태 조회 가능
- `client_admin`: 결과 열람만 제한 허용 가능
- `qa_admin`: 검수 저장, 원인 분류, 후속 액션 실행 가능

## 3. Action Classes

- `read`
- `annotate`
- `trigger_operation`
- `review`
- `admin_config`

API는 화면 단위가 아니라 액션 단위 권한 검사를 적용해야 한다.

## 4. Audit Logging Rules

다음 액션은 반드시 감사로그를 남긴다.

- 문서 등록, 수정, 비활성화
- 재수집, 재인덱싱 실행
- QA 검수 결과 저장
- 권한 변경
- 운영 메모 또는 상태 변경

## 5. Policy Boundary

이 문서의 범위:
- 화면 접근 가능 여부
- 액션 클래스별 권한 원칙
- 감사로그 필요 액션

이 문서의 비범위:
- 로그인 방식과 세션 만료 정책
- 사용자/역할 조회 API
- 권한 검사 미들웨어 응답 포맷

세부 구현은 아래 문서로 연결한다.
- [10_auth_authz_api.md](/C:/Users/User/Documents/work/mvp_docs/10_auth_authz_api.md)
