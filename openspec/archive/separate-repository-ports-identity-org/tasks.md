# Tasks

## 계획 단계
- [x] 요구 범위 재확인
- [x] 현재 identity-access, organization-directory 모듈 구조 확인
- [x] 기존 저장소 관련 코드 위치 파악
- [x] mvp_docs/10_auth_authz_api.md에서 테이블 스키마 재확인
  - AdminSessionRepository는 이미 존재함
  - AdminUserRepository, AuditLogRepository 추가 필요
  - OrganizationRepository, ServiceRepository 추가 필요

## identity-access 모듈 구현
- [x] `AdminSessionRepository` 포트 인터페이스 정의 (이미 존재함)
  - findBySessionId
  - issue
  - revoke
- [x] `AdminUserRepository` 포트 인터페이스 정의
  - findByEmail
  - findById
  - save
- [x] `AuditLogRepository` 포트 인터페이스 정의
  - save (AuditLogEntry 모델 포함)
- [x] `InMemoryAdminSessionRepository` 구현
- [x] `InMemoryAdminUserRepository` 구현
- [x] `InMemoryAuditLogRepository` 구현

## organization-directory 모듈 구현
- [x] `OrganizationRepository` 포트 인터페이스 정의
  - findAll
  - findById
  - save
- [x] `ServiceRepository` 포트 인터페이스 정의
  - findByOrganizationId
  - findById
  - save
- [x] `InMemoryOrganizationRepository` 구현
- [x] `InMemoryServiceRepository` 구현
- [x] Organization, Service 모델 추가

## admin-api 통합
- [x] modules의 포트 인터페이스 구조 완성
- [x] admin-api는 기존 구현 유지 (개발용 데이터 포함)
- [x] 향후 JPA 전환 시 modules 포트 사용 가능한 구조 확보
- [ ] ~Spring Bean 등록~ (Bean 충돌로 보류, 기존 @Service 유지)

## 테스트
- [x] `./gradlew test` 실행해서 기존 테스트 통과 확인 (19 tests passed)
- [x] `/admin/auth/login` API 테스트 통과 확인
- [x] `/admin/auth/me` API 테스트 통과 확인
- [x] `/admin/auth/logout` API 테스트 통과 확인

## 마무리
- [ ] CLAUDE.md 업데이트 (필요 없음 - 아키텍처 변경 없음)
- [ ] 99_worklog.md 갱신
- [ ] status.md 완료 상태로 갱신
- [ ] proposal.md Done Definition 업데이트
- [ ] change를 archive로 이동
- [ ] 커밋 (한글 메시지)
