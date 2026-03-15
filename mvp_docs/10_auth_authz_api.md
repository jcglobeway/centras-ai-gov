# Auth And Authorization API

## 1. Purpose

이 문서는 관리자 인증, 세션, 역할, 권한 검사 API 계약을 정의한다.
화면 정책은 [06_access_policy.md](/C:/Users/User/Documents/work/mvp_docs/06_access_policy.md)를 기준으로 하고,
이 문서는 그 정책을 API와 서버 검증 규칙으로 연결한다.

## 2. Scope

포함 범위:
- 관리자 로그인/로그아웃
- 세션 조회와 갱신
- 내 권한 조회
- 조직 범위가 반영된 사용자 목록 조회
- 서버 액션 단위 권한 검사 규칙

비포함 범위:
- 시민용 챗봇 사용자 인증
- 외부 SSO 제품 선정
- 운영사 내부 인사 시스템 연동

## 3. Core Principles

- 권한 검사는 화면 단위가 아니라 `action` 단위로 수행한다.
- 모든 관리자 요청은 `user_id`, `role_code`, `organization_scope` 를 복원 가능해야 한다.
- `ops_admin` 만 전기관 범위 접근이 가능하다.
- `client_admin`, `qa_admin` 은 허용된 기관 범위 안에서만 읽기/쓰기 가능하다.
- 감사로그는 인증 이벤트와 고위험 권한 액션을 모두 남긴다.

## 4. Roles And Scope

### `ops_admin`

- 범위: 전체 기관
- 허용 액션:
  - `read`
  - `annotate`
  - `trigger_operation`
  - `review`
  - `admin_config`

### `client_admin`

- 범위: 할당된 자기 기관
- 허용 액션:
  - `read`
  - `annotate`
- 제한 액션:
  - `trigger_operation`: 문서 재수집/재인덱싱 요청 수준만 허용 가능
  - `review`: 직접 QA 판정 저장은 기본 불가
  - `admin_config`: 불가

### `qa_admin`

- 범위: 배정 기관 또는 전체 QA 운영 범위
- 허용 액션:
  - `read`
  - `annotate`
  - `review`
- 제한 액션:
  - `trigger_operation`: 재인덱싱 요청 생성까지만 허용, 실제 실행은 불가
  - `admin_config`: 불가

## 5. Required Tables

최소 신규/확장 테이블:
- `admin_users`
- `admin_user_roles`
- `admin_sessions`
- `audit_logs`

기존 참조 테이블:
- `organizations`
- `services`

### `admin_users`

핵심 필드:
- `id`
- `email`
- `display_name`
- `status`
- `last_login_at`
- `created_at`

상태값:
- `active`
- `invited`
- `locked`
- `disabled`

### `admin_user_roles`

핵심 필드:
- `id`
- `user_id`
- `role_code`
- `organization_id`
- `service_scope_json`
- `assigned_at`
- `revoked_at`

원칙:
- `ops_admin` 은 `organization_id = null` 허용
- `client_admin`, `qa_admin` 은 최소 1개 기관이 필요
- 역할 변경은 기존 이력을 덮어쓰지 않고 신규 row로 남긴다

### `admin_sessions`

핵심 필드:
- `id`
- `user_id`
- `session_token_hash`
- `issued_at`
- `expires_at`
- `last_seen_at`
- `ip_address`
- `user_agent`
- `revoked_at`

원칙:
- 원문 토큰은 저장하지 않는다
- 세션 무효화는 `revoked_at` 으로 처리한다

### `audit_logs`

핵심 필드:
- `id`
- `actor_user_id`
- `actor_role_code`
- `organization_id`
- `action_code`
- `resource_type`
- `resource_id`
- `request_id`
- `trace_id`
- `result_code`
- `created_at`

## 6. Permission Model

서버 내부 권한 판단 입력:
- `role_code`
- `action_code`
- `organization_id`
- `resource_owner_organization_id`

권장 액션 코드:
- `dashboard.read`
- `organization.read`
- `organization.update`
- `document.read`
- `document.reingest.request`
- `document.reindex.request`
- `document.reindex.execute`
- `qa.review.read`
- `qa.review.write`
- `metrics.read`
- `auth.user.read`
- `auth.role.assign`

판단 규칙:
- 액션이 허용되지 않으면 즉시 `403`
- 액션은 허용되지만 기관 범위가 다르면 `403`
- 리소스가 없거나 범위를 숨겨야 하면 `404`

## 7. Auth Flow

### 7.1 Login

1. 사용자가 관리자 로그인 요청
2. 자격 증명 검증 또는 외부 IdP 검증
3. 활성 사용자/역할 여부 확인
4. `admin_sessions` 생성
5. 응답에 세션 토큰과 권한 요약 반환

### 7.2 Session Restore

1. 요청 헤더의 세션 토큰 확인
2. 토큰 해시 조회
3. 세션 만료/폐기 여부 확인
4. 역할과 기관 범위 복원
5. 컨트롤러 진입 전 권한 컨텍스트 주입

### 7.3 Logout

1. 현재 세션 `revoked_at` 기록
2. 감사로그 저장
3. 클라이언트 토큰 폐기

## 8. API Contracts

### `POST /admin/auth/login`

목적:
- 관리자 세션 생성

요청 예시:

```json
{
  "email": "qa.manager@gov-platform.kr",
  "password": "********"
}
```

응답 예시:

```json
{
  "user": {
    "id": "usr_001",
    "email": "qa.manager@gov-platform.kr",
    "display_name": "QA Manager",
    "status": "active"
  },
  "session": {
    "token": "sess_xxx",
    "expires_at": "2026-03-15T18:00:00Z"
  },
  "authorization": {
    "primary_role": "qa_admin",
    "organization_scope": [
      "org_seoul_120"
    ],
    "actions": [
      "dashboard.read",
      "document.read",
      "qa.review.read",
      "qa.review.write",
      "metrics.read"
    ]
  }
}
```

실패 규칙:
- 자격 증명 오류: `401 AUTH_INVALID_CREDENTIALS`
- 비활성 사용자: `403 AUTH_USER_DISABLED`
- 역할 미할당: `403 AUTH_ROLE_NOT_ASSIGNED`

### `POST /admin/auth/logout`

목적:
- 현재 세션 폐기

요청 예시:

```json
{}
```

응답 예시:

```json
{
  "revoked": true
}
```

### `GET /admin/auth/me`

목적:
- 현재 로그인 사용자와 권한 요약 조회

응답 예시:

```json
{
  "user": {
    "id": "usr_001",
    "email": "qa.manager@gov-platform.kr",
    "display_name": "QA Manager",
    "status": "active",
    "last_login_at": "2026-03-15T08:45:00Z"
  },
  "roles": [
    {
      "role_code": "qa_admin",
      "organization_id": "org_seoul_120"
    }
  ],
  "actions": [
    "dashboard.read",
    "document.read",
    "qa.review.read",
    "qa.review.write",
    "metrics.read"
  ]
}
```

### `GET /admin/users`

목적:
- 관리자 사용자 목록 조회

쿼리:
- `role_code`
- `organization_id`
- `status`

권한:
- `ops_admin` 만 전체 조회 가능
- `client_admin` 은 자기 기관 담당자만 제한 조회 가능

응답 예시:

```json
{
  "items": [
    {
      "id": "usr_002",
      "email": "client.admin@gov-platform.kr",
      "display_name": "Client Admin",
      "status": "active",
      "roles": [
        {
          "role_code": "client_admin",
          "organization_id": "org_seoul_120"
        }
      ]
    }
  ],
  "total": 1
}
```

### `POST /admin/users/{id}/roles`

목적:
- 사용자 역할 부여 또는 확장

요청 예시:

```json
{
  "role_code": "qa_admin",
  "organization_id": "org_seoul_120",
  "service_scope": [
    "svc_civil"
  ]
}
```

응답 예시:

```json
{
  "user_id": "usr_002",
  "assigned_role": {
    "role_code": "qa_admin",
    "organization_id": "org_seoul_120",
    "service_scope": [
      "svc_civil"
    ]
  }
}
```

권한:
- `ops_admin` 만 가능

검증 규칙:
- 동일 활성 역할 중복 부여 금지
- `client_admin`, `qa_admin` 은 `organization_id` 필수
- 역할 부여 시 감사로그 필수

## 9. Middleware Validation Rule

모든 `/admin/*` 요청 공통 검증:
- 세션 토큰 존재 여부 확인
- 세션 만료 및 폐기 여부 확인
- 사용자 상태가 `active` 인지 확인
- 액션 코드 매핑 확인
- 조직 범위 확인

컨트롤러 전 주입 컨텍스트:
- `actor.user_id`
- `actor.primary_role`
- `actor.organization_scope`
- `actor.actions`
- `request_id`
- `trace_id`

## 10. Error Contract

응답 포맷:

```json
{
  "error": {
    "code": "AUTH_FORBIDDEN_SCOPE",
    "message": "You do not have access to this organization.",
    "request_id": "req_123"
  }
}
```

권장 코드:
- `AUTH_UNAUTHORIZED`
- `AUTH_INVALID_CREDENTIALS`
- `AUTH_SESSION_EXPIRED`
- `AUTH_USER_DISABLED`
- `AUTH_ROLE_NOT_ASSIGNED`
- `AUTH_FORBIDDEN_ACTION`
- `AUTH_FORBIDDEN_SCOPE`

## 11. Audit Logging Rule

반드시 기록할 이벤트:
- 로그인 성공
- 로그인 실패
- 로그아웃
- 역할 부여/회수
- 사용자 잠금/비활성화
- 권한 부족으로 거부된 고위험 요청

감사로그 최소 속성:
- `actor_user_id`
- `action_code`
- `target_user_id`
- `organization_id`
- `result_code`
- `request_id`
- `trace_id`

## 12. Screen Mapping

화면 연결 기준:
- Operations Dashboard: `GET /admin/auth/me` 로 범위 복원 후 `GET /admin/ops/dashboard`
- Client Dashboard: `GET /admin/auth/me` 의 `organization_scope` 기준으로 제한 조회
- QA Review: `qa.review.read`, `qa.review.write` 필요
- Document Source Management: 조회는 `document.read`, 실행 요청은 `document.reindex.request` 또는 `document.reindex.execute`

## 13. Sprint 1 Cut

Sprint 1 필수 범위:
- `POST /admin/auth/login`
- `POST /admin/auth/logout`
- `GET /admin/auth/me`
- 세션 기반 권한 미들웨어
- `audit_logs` 적재

Sprint 1 제외:
- 비밀번호 재설정 UI
- 다중 인증
- 세밀한 서비스 단위 위임 관리

## 14. OpenRAG Boundary

OpenRAG는 인증/권한 모델의 일부가 아니다.

즉:
- OpenRAG 엔진 접근 권한을 별도 사용자 개념으로 노출하지 않는다.
- 관리자 권한은 항상 제품 API 게이트웨이에서 판정한다.
- 외부 RAG 엔진 호출 전 권한 검증이 완료돼야 한다.
