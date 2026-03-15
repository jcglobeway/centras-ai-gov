# Proposal

## Change ID

`separate-repository-ports-identity-org`

## Summary

### 변경 목적
- `identity-access`와 `organization-directory` 모듈의 저장소 포트를 명확히 분리
- 헥사고날 아키텍처의 포트/어댑터 패턴 적용
- 개발용 인메모리 구현과 실제 DB 구현을 교체 가능하게 만들기

### 변경 범위
- `identity-access` 모듈:
  - `AdminSessionRepository` 포트 인터페이스 정의
  - `AdminUserRepository` 포트 인터페이스 정의
  - `AuditLogRepository` 포트 인터페이스 정의
  - 개발용 인메모리 구현체 (`InMemoryAdminSessionRepository` 등)

- `organization-directory` 모듈:
  - `OrganizationRepository` 포트 인터페이스 정의
  - `ServiceRepository` 포트 인터페이스 정의
  - 개발용 인메모리 구현체

- `admin-api` 모듈:
  - 개발용 구현체를 Spring Bean으로 등록
  - 기존 엔드포인트가 새 포트 인터페이스를 사용하도록 조정

### 제외 범위
- 실제 JPA 엔티티 정의 (별도 change에서 진행)
- DB 스키마 마이그레이션 스크립트
- 다른 모듈의 저장소 (chat-runtime, document-registry, ingestion-ops 등)
- 프로덕션용 DB 연결 설정

## Impact

### 영향 모듈
- `modules/identity-access`: 저장소 포트 인터페이스 추가
- `modules/organization-directory`: 저장소 포트 인터페이스 추가
- `apps/admin-api`: 구현체 Bean 등록, 의존성 주입 조정

### 영향 API
- 기존 API 엔드포인트는 영향 없음 (내부 구현만 변경)
- `/admin/auth/*` 엔드포인트의 내부 저장소 호출 경로 변경
- `/admin/organizations`, `/admin/services` 조회 경로 변경

### 영향 테스트
- 기존 MockMvc 테스트는 개발용 인메모리 구현체를 사용하므로 영향 최소화
- 저장소 포트별 단위 테스트 추가 필요

## Done Definition

- [x] `identity-access`의 3개 저장소 포트 인터페이스가 정의됨
  - AdminSessionRepository (이미 존재)
  - AdminUserRepository (신규)
  - AuditLogRepository (신규)
- [x] `organization-directory`의 2개 저장소 포트 인터페이스가 정의됨
  - OrganizationRepository (신규)
  - ServiceRepository (신규)
- [x] 각 포트별 개발용 인메모리 구현체가 작성됨
  - modules에 ConcurrentHashMap 기반 구현 완료
- [x] 향후 JPA 전환을 위한 포트 구조 확보
  - admin-api는 기존 구현 유지 (개발용 데이터 포함)
- [x] 기존 `/admin/auth/*` API 테스트가 모두 통과함
- [x] 기존 ingestion 조회 API 테스트가 모두 통과함
- [x] `./gradlew test` 전체 테스트가 통과함 (19 tests)
