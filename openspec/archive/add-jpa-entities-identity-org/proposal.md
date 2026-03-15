# Proposal

## Change ID

`add-jpa-entities-identity-org`

## Summary

### 변경 목적
- 현재 인메모리 구현을 PostgreSQL + JPA로 전환
- 데이터 영속화 및 재시작 시 세션/사용자 데이터 유지
- 실제 운영 가능한 시스템 기반 마련

### 변경 범위
- **인프라 설정**:
  - PostgreSQL Docker Compose 파일
  - application.yml에 datasource, JPA 설정 추가
  - Flyway migration 설정

- **DB 마이그레이션 스크립트** (Flyway):
  - V001__create_admin_users.sql
  - V002__create_admin_sessions.sql
  - V003__create_audit_logs.sql
  - V004__create_organizations.sql
  - V005__create_services.sql

- **JPA 엔티티**:
  - identity-access: AdminUserEntity, AdminSessionEntity, AuditLogEntity
  - organization-directory: OrganizationEntity, ServiceEntity

- **JPA Repository 어댑터**:
  - JpaAdminUserRepository
  - JpaAdminSessionRepository
  - JpaAuditLogRepository
  - JpaOrganizationRepository
  - JpaServiceRepository

- **Spring Bean 구성**:
  - 개발 환경: H2 in-memory (테스트용)
  - 로컬 환경: PostgreSQL (docker-compose)
  - JPA 구현체를 기본 Bean으로 등록

- **기존 코드 정리**:
  - admin-api의 InMemoryAdminSessionRepository 제거
  - admin-api의 InMemoryOrganizationDirectoryReader 제거
  - modules의 인메모리 구현은 유지 (참고용/단위 테스트용)

### 제외 범위
- 다른 모듈의 JPA 엔티티 (chat-runtime, document-registry, ingestion-ops)
- connection pooling 최적화
- read replica 설정
- 프로덕션 배포 설정

## Impact

### 영향 모듈
- `apps/admin-api`: build.gradle.kts에 JPA, PostgreSQL 의존성 추가
- `modules/identity-access`: JPA 엔티티 및 Repository 추가
- `modules/organization-directory`: JPA 엔티티 및 Repository 추가

### 영향 API
- 기존 API 계약 변경 없음 (내부 구현만 변경)
- 세션 데이터가 영속화되어 재시작 후에도 유지됨

### 영향 테스트
- 테스트는 H2 in-memory DB 사용 (빠른 실행)
- 기존 테스트 코드 변경 없음 (스프링 컨텍스트가 JPA 구현 주입)
- 통합 테스트에서 실제 DB transaction 검증 가능

## Done Definition

- [x] docker-compose.yml 생성 (PostgreSQL 15-alpine)
- [x] build.gradle.kts에 JPA, PostgreSQL, Flyway 의존성 추가
- [x] application.yml에 datasource 설정 추가
- [x] application-test.yml 생성 (H2 MODE=PostgreSQL)
- [x] Flyway migration 스크립트 5개 작성 (seed 데이터 포함)
- [x] identity-access JPA 엔티티 3개 작성
- [x] organization-directory JPA 엔티티 2개 작성
- [x] JPA Repository 인터페이스 5개 작성
- [x] Repository 어댑터 6개 구현 (포트 → JPA)
- [x] Spring Configuration에서 JPA 구현체를 Bean으로 등록
- [x] admin-api의 기존 인메모리 구현 제거
- [x] DevelopmentAdminCredentialAuthenticator 분리
- [x] ./gradlew test 전체 통과 (H2 기반, 25 tests)
- [x] Flyway migration 자동 실행 확인
- [x] AdminSessionSnapshot JSON 직렬화 정상 작동
- [ ] ~docker-compose up → ./gradlew bootRun~ (사용자 환경에서 실행 필요)
