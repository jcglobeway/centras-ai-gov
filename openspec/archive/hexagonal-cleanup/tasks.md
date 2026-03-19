# Tasks

## P1: 레거시 포트 제거 (organization-directory)

- [ ] `OrganizationDirectoryPorts.kt`에서 `OrganizationRepository`, `ServiceRepository` 인터페이스 삭제
- [ ] `OrganizationRepositoryAdapter.kt` 파일 삭제
- [ ] `ServiceRepositoryAdapter.kt` 파일 삭제
- [ ] `RepositoryConfiguration.kt`에서 `organizationRepository`, `serviceRepository` Bean 등록 제거

## P2: 날짜 필터링 서비스 레이어로 이동

- [ ] `ListQuestionsUseCase`에 `from`, `to` 날짜 파라미터 추가
- [ ] `ListQuestionsService`에서 날짜 필터링 처리
- [ ] `QuestionController`에서 `filterByDateRange` 제거, UseCase에 날짜 전달

## P3: GetOrganizationsUseCase 스코프 패턴 통일

- [ ] `OrganizationDirectoryDomain.kt`에 `OrganizationScope(organizationIds, globalAccess)` 추가
- [ ] `GetOrganizationsUseCase`를 `listOrganizations(scope: OrganizationScope): List<Organization>`로 단순화
- [ ] `GetOrganizationsService` 구현 업데이트
- [ ] `LoadOrganizationPort` 업데이트
- [ ] `LoadOrganizationPortAdapter` 업데이트
- [ ] `OrganizationController` 업데이트 (scope 직접 구성 → UseCase 위임)

## P4: Auth UseCase 인터페이스 도입

- [ ] `identity-access` 모듈 `application/port/in/`에 `AdminAuthUseCase` 인터페이스 정의 (login, logout)
- [ ] `DevelopmentAdminSessionService`가 `AdminAuthUseCase`를 구현하도록 변경
- [ ] `AuthCommandController`가 `AdminAuthUseCase`를 주입하도록 변경
- [ ] `ServiceConfiguration`에 `DevelopmentAdminSessionService` Bean 명시 등록 (`@Service` 제거)

## P5: Command 객체 위치 이동

- [ ] `chat-runtime`: `Create*Command`, `CreateRagSearchLogCommand` 등을 `application/port/in/` 으로 이동
- [ ] `ingestion-ops`: `CreateCrawlSourceCommand`, `RequestIngestionJobCommand`, `TransitionIngestionJobCommand` 이동
- [ ] `qa-review`: `CreateQAReviewCommand` 이동 (있다면)
- [ ] `document-registry`: `SaveDocumentChunkCommand` 이동 (있다면)
- [ ] `metrics-reporting`: `UpsertDailyMetricsCommand` 이동 (있다면)
- [ ] 각 domain 파일에서 Command import 정리

## 공통

- [ ] `./gradlew compileKotlin` 통과 확인
- [ ] `./gradlew test` 전체 통과 확인
- [ ] 커밋
