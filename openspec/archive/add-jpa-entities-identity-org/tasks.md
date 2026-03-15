# Tasks

## 계획 단계
- [x] 요구 범위 재확인
- [x] mvp_docs/10_auth_authz_api.md에서 테이블 스키마 확인
- [x] 현재 build.gradle.kts 확인
- [x] 기존 인메모리 구현 위치 파악

## 인프라 설정
- [x] docker-compose.yml 생성 (PostgreSQL 15+)
- [x] build.gradle.kts에 의존성 추가:
  - spring-boot-starter-data-jpa
  - postgresql driver
  - flyway-core
  - h2 (test scope)
- [x] application.yml에 datasource 설정 추가
- [x] Flyway 설정 추가

## DB 마이그레이션 스크립트
- [x] src/main/resources/db/migration/ 디렉터리 생성
- [x] V001__create_admin_users.sql (seed 데이터 포함)
- [x] V002__create_admin_sessions.sql (seed 데이터 포함, snapshot JSON)
- [x] V003__create_audit_logs.sql
- [x] V004__create_organizations.sql (seed 데이터 포함)
- [x] V005__create_services.sql (seed 데이터 포함)

## identity-access JPA 엔티티
- [x] AdminUserEntity.kt (toModel, toEntity 확장 함수)
- [x] AdminSessionEntity.kt (snapshot JSON 직렬화, toModel, toEntity)
- [x] AuditLogEntity.kt (toModel, toEntity)

## identity-access JPA Repository
- [x] JpaAdminUserRepository.kt (findByEmail)
- [x] JpaAdminSessionRepository.kt (findBySessionTokenHash)
- [x] JpaAuditLogRepository.kt
- [x] AdminUserRepositoryAdapter.kt (포트 구현)
- [x] AdminSessionRepositoryAdapter.kt (포트 구현, @Transactional)
- [x] AuditLogRepositoryAdapter.kt (포트 구현)

## organization-directory JPA 엔티티
- [x] OrganizationEntity.kt (toModel, toSummary, toEntity)
- [x] ServiceEntity.kt (toModel, toEntity)

## organization-directory JPA Repository
- [x] JpaOrganizationRepository.kt
- [x] JpaServiceRepository.kt (findByOrganizationId)
- [x] OrganizationRepositoryAdapter.kt (포트 구현)
- [x] ServiceRepositoryAdapter.kt (포트 구현)
- [x] OrganizationDirectoryReaderAdapter.kt (포트 구현)

## Spring Configuration
- [x] RepositoryConfiguration.kt 생성 (JPA 구현체 Bean 등록)
- [x] AdminApiApplication에 @EnableJpaRepositories, @EntityScan 추가
- [x] admin-api의 기존 InMemory 구현 제거
- [x] DevelopmentAdminCredentialAuthenticator 분리
- [x] modules build.gradle.kts에 JPA, Spring 의존성 추가
- [x] kotlin-spring, kotlin-jpa plugin 추가
- [x] 모든 어댑터를 `open class`로 설정 (CGLIB proxy)

## 테스트 환경 설정
- [x] application-test.yml 생성 (H2 MODE=PostgreSQL)
- [x] @ActiveProfiles("test") 추가
- [x] AdminUser.lastLoginAt nullable로 수정

## 검증
- [x] ./gradlew test 전체 통과 (H2 기반, 25 tests)
- [x] Flyway migration 실행 확인 (5 migrations applied)
- [ ] docker-compose up -d 실행 (사용자 환경)
- [ ] PostgreSQL 접속 확인 (사용자 환경)
- [ ] ./gradlew bootRun 실행 (사용자 환경)

## 마무리
- [ ] 99_worklog.md 갱신
- [ ] status.md 완료 상태로 갱신
- [ ] proposal.md Done Definition 업데이트
- [ ] change를 archive로 이동
- [ ] 커밋 (한글 메시지)
