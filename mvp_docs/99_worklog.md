# MVP Worklog

## 2026-03-15

### Summary

- 문서 기준 합의를 실제 저장소 골격으로 전환
- `Spring Boot + Kotlin` 멀티모듈 루트와 `apps/admin-api` 부트 앱 생성
- bounded context 기준 `modules/*` 초기 구조 생성
- `python/ingestion-worker`, `python/rag-orchestrator`, `tests/*` 진입 구조 생성
- 단일 작업 로그 중심 구조를 해체하고 주제별 문서 구조로 재편
- MVP 목표, 역할 플로우, 화면, 데이터/API, 아키텍처, 권한, 전달 계획을 분리
- OpenRAG는 MVP 본체가 아니라 병렬 PoC 대상으로 유지
- `04_data_api.md`를 요청/응답 예시, 상태값, 인덱스 초안 수준까지 상세화
- `03_screen_spec.md`에 화면별 상태값과 필터 규칙을 추가
- `05_architecture_openrag.md`에 PoC 범위, 체크리스트, 종료 기준을 추가
- `07_delivery_plan.md`를 마일스톤, 트랙별 구현 티켓, 의존성, Sprint 컷 기준으로 상세화
- `08_traceability_matrix.md`를 추가해 화면, API, 핵심 테이블 매핑을 한 장으로 정리
- `09_unresolved_qa_state_machine.md`를 추가해 unresolved/QA 상태 전이와 집계 규칙을 분리
- `10_auth_authz_api.md`를 추가해 관리자 인증, 세션, 역할, 권한 검사 API 계약을 분리
- `08_traceability_matrix.md`에 화면별 선행 권한과 Sprint 1 인증/권한 의존성을 연결
- `11_traceability_test_cases.md`를 추가해 화면, 권한, API, DB 기준 최소 회귀팩과 E2E 시나리오를 고정
- `12_test_strategy.md`를 추가해 traceability 테스트를 unit, api, e2e, data 검증 레벨로 분해
- `13_test_runner_structure.md`를 추가해 테스트 러너 책임, 폴더 구조, fixture/seed 경계, CI 실행 순서를 고정
- `14_test_file_scaffold.md`를 추가해 Sprint 1 테스트 파일명, 최소 책임, helper/fixture 연결, 생성 순서를 고정
- `15_ingestion_browser_review.md`를 추가해 crawling 런타임, Playwright 기본값, Lightpanda/OpenRAG PoC 경계를 정리
- `04_data_api.md`에 `crawl_sources`, `document_versions`, `ingestion_jobs`와 ingestion API 계약을 추가
- `03_screen_spec.md`에 `Crawl Source Management`, `Ingestion Job Monitor` 화면을 추가
- `08_traceability_matrix.md`에 ingestion 화면의 권한, API, DB, trace 키 연결을 추가
- `16_springboot_kotlin_ddd_msa_review.md`를 추가해 Spring Boot, Kotlin, DDD, MSA 방향성과 Python ingestion/RAG 하이브리드 권장안을 정리
- `apps/admin-api`에 `GET /healthz`, `GET /admin/auth/me` 개발용 엔드포인트와 MockMvc 테스트를 추가
- 루트에 `ENV_SETUP.md`를 추가해 Windows 기준 JDK 21, Gradle Wrapper, 첫 검증 절차를 정리
- 사용자 영역 `JDK 21`, `Gradle 9.4.0`, `Gradle Wrapper`를 준비하고 `.\gradlew.bat test` 통과 확인
- `identity-access`, `organization-directory` 모듈에 세션 복원/조직 조회 계약을 추가하고 `admin-api`를 모듈 계약 기반으로 정리
- 작업 폴더를 `git init -b main`으로 초기화하고 로컬 Git 추적 기반을 마련
- `ingestion-ops` 모듈에 crawl source/job 조회 계약을 추가하고 `admin-api`에 범위 기반 조회 엔드포인트를 연결
- `identity-access`에 `AdminSessionRepository` 포트를 추가하고 `X-Admin-Session-Id` 기반 세션 복원 경로를 연결
- OpenSpec 기반 변경 추적 구조와 템플릿을 추가하고, 이후 중요한 변경은 change 단위로 관리하기로 결정
- `add-ingestion-write-contracts` change로 ingestion 쓰기 계약, 상태 전이 규칙, 개발용 쓰기 API와 테스트를 추가
- 전체 개발 진행 추적용 `17_development_wbs_milestones.md`를 추가하고 Milestone/WBS/Mermaid 일정 기준을 고정
- ingestion API 권한 검증을 `role` 분기 대신 `identity-access`의 액션 기반 정책으로 올리는 change를 시작
- `identity-access`에 `AdminAuthorizationPolicy`를 추가하고 ingestion API가 액션 기반 권한 검증을 사용하도록 전환
- `add-auth-session-lifecycle` change로 로그인/로그아웃, 세션 만료/폐기, 명시적 세션 ID 오류 응답, 개발용 자격 증명 검증을 추가
- `separate-repository-ports-identity-org` change로 저장소 포트 인터페이스를 명확히 분리하고 인메모리 구현체 추가
- `identity-access`에 `AdminUserRepository`, `AuditLogRepository` 포트 추가 (AdminSessionRepository는 이미 존재)
- `organization-directory`에 `OrganizationRepository`, `ServiceRepository` 포트 추가
- modules에 ConcurrentHashMap 기반 인메모리 어댑터 구현 (향후 JPA 전환 준비)
- admin-api는 기존 개발용 구현 유지 (Bean 충돌 회피, 점진적 전환 전략)
- `add-ingestion-worker-crawl-flow` change로 Python ingestion-worker 기본 실행 흐름 구현
- admin-api에 crawl source/job 개별 조회 API 추가 (GET /admin/crawl-sources/{id}, /admin/ingestion-jobs/{id})
- Python worker에 AdminApiClient 구현 (httpx 기반, X-Admin-Session-Id 인증)
- Playwright 기반 CrawlExecutor 구현 (async API, URL fetch, 스크린샷 저장)
- IngestionJobRunner 구현 (job lifecycle: queued → running → succeeded/failed)
- CLI 통합 (ingestion-worker run --job-id, 환경 변수 지원)
- pyproject.toml에 playwright, pytest 의존성 추가
- `add-auth-ingestion-test-cases` change로 개별 조회 API 테스트 추가
- AdminApiApplicationTests에 6개 테스트 추가 (crawl source/job 개별 조회, 404, 권한 범위)
- 전체 테스트 19개 → 25개로 확장
- `add-jpa-entities-identity-org` change로 PostgreSQL + JPA 연동 구현
- docker-compose.yml 추가 (PostgreSQL 15-alpine)
- Flyway migration 5개 작성 (admin_users, admin_sessions, audit_logs, organizations, services)
- JPA 엔티티 5개 + Spring Data JPA Repository 5개 구현
- Repository 어댑터 6개 구현 (AdminSession, AdminUser, AuditLog, Organization, Service, DirectoryReader)
- RepositoryConfiguration으로 JPA 구현체 Bean 등록
- admin-api 기존 InMemory 구현 제거, DevelopmentAdminCredentialAuthenticator 분리
- kotlin-spring, kotlin-jpa plugin 추가 (modules)
- 모든 어댑터 `open class`로 설정 (CGLIB proxy 지원)
- 테스트 환경 H2 in-memory 설정, Flyway migration 자동 적용
- AdminUser.lastLoginAt nullable로 수정
- ./gradlew test 통과 (25 tests, H2 기반)
- `add-jpa-entities-ingestion-ops` change로 ingestion-ops 모듈 JPA 연동 완성
- Flyway migration 2개 추가 (V006 crawl_sources, V007 ingestion_jobs + seed)
- ingestion-ops JPA 엔티티 2개 구현 (CrawlSourceEntity, IngestionJobEntity)
- JPA Repository 2개 + 어댑터 4개 구현 (Reader/Writer, scope 필터링)
- IngestionJobWriterAdapter에 상태 머신 통합, source 상태 자동 업데이트
- RepositoryConfiguration에 ingestion-ops Bean 4개 등록
- DevelopmentIngestionStore 제거 (JPA 어댑터로 완전 교체)
- 테스트 수정 (동적 job ID 사용)
- ./gradlew test 통과 (25 tests)
- 전체 DB 영속화 완성 (identity, org, ingestion-ops)
- `add-e2e-auth-ingestion-flow` change로 E2E 통합 테스트 추가
- AdminApiApplicationTests에 4개 E2E 테스트 추가:
  • 전체 인증 플로우 (login → session 복원 → logout → revoked 검증)
  • Ingestion 전체 플로우 (source 생성 → job 실행 → 상태 전이 → source 상태 업데이트)
  • Client admin 권한 제한 (범위 밖 접근, 쓰기 권한 검증)
  • 멀티테넌트 격리 (ops vs client, org별 데이터 분리)
- 테스트 개수: 25개 → 29개 (E2E 4개 추가)
- ./gradlew test 통과 (29 tests)
- `add-qa-review-module` change로 QA Review 모듈 핵심 기능 구현
- qa-review 모듈 도메인 모델 정의 (QAReviewStatus, RootCauseCode, ActionType enum)
- QAReviewStateMachine 구현 (validateReview, validateTransition)
  • confirmed_issue: root_cause + action 필수
  • false_alarm: action_type = no_action 강제
  • resolved: review_comment 필수
  • 금지 전이: false_alarm ↔ resolved
- Flyway V008__create_qa_reviews.sql (FK는 questions 구현 후 추가)
- JPA 엔티티 + Repository (findByQuestionIdOrderByReviewedAtDesc)
- QAReviewReaderAdapter, QAReviewWriterAdapter (상태 머신 통합)
- QAReviewController (POST /admin/qa-reviews, GET /admin/qa-reviews?questionId=)
- 권한 검증 (qa.review.read, qa.review.write)
- AdminApiApplicationTests에 5개 QA review 테스트 추가
- 테스트 개수: 29개 → 34개 (QA review 5개 추가)
- ./gradlew test 통과 (34 tests)

### Current Decision

- 제품 중심은 `Admin/API/DB`다.
- OpenRAG는 `RAG Adapter` 뒤에서만 선택적으로 검토한다.
- 신규 논의 내용은 해당 주제 문서에 먼저 반영한다.
- API 계약은 RAG 구현체와 분리해 제품 스키마 기준으로 고정한다.
- 전달 계획은 `Track -> DB/Backend/Frontend -> 완료 기준` 구조로 관리한다.
- 구현 영향도 판단은 `08_traceability_matrix.md`를 기준으로 본다.
- unresolved/QA 운영 상태는 `answer_status`, `review_status`, `resolution_status` 3층 구조로 관리한다.
- 인증/권한 계약은 화면 정책과 분리해 `세션 -> 역할 -> 액션 -> 조직 범위` 흐름으로 고정한다.
- traceability 기준 문서는 이제 `화면 -> 권한 액션 -> API -> DB` 순서로 본다.
- 테스트 기준은 `traceability -> 상태 전이 -> API 계약` 순서로 연결한다.
- 자동화 전략은 `unit -> api -> e2e -> data verification` 순서로 진행한다.
- 테스트 저장소 구조는 `tests/unit`, `tests/api`, `tests/e2e`, `tests/data` 4축으로 고정한다.
- OpenRAG는 테스트에서도 제품 계약 바깥의 stub 대상이다.
- Sprint 1 테스트 착수 기준은 문서가 아니라 실제 파일명 단위로 관리한다.
- ingestion 기본 브라우저 런타임은 `Playwright`다.
- Lightpanda는 `기본 채택`이 아니라 `선별 URL 대상 병렬 PoC`다.
- `OpenRAG + Lightpanda` 동시 기본 채택은 MVP 기간에 보류한다.
- ingestion 시스템 오브 레코드는 `crawl_sources -> ingestion_jobs -> documents/document_versions` 흐름으로 관리한다.
- ingestion 운영 화면은 `source 관리`와 `job 모니터링`을 분리한다.
- ingestion 추적 키는 `crawl_source_id`, `ingestion_job_id`, `document_id`를 함께 본다.
- 제품 본체 백엔드의 현재 우선안은 `Spring Boot + Kotlin`이다.
- 아키텍처 기본값은 `DDD-lite modular monolith + Python workers`다.
- `full MSA`는 MVP 즉시 채택이 아니라 추후 분리 가능한 경계 설계 대상으로 둔다.
- 저장소 초기 골격은 이미 생성했고, 다음 단계는 각 모듈 계약과 엔드포인트를 채우는 것이다.
- 로컬 부트스트랩은 완료됐고 기본 실행 명령은 `.\gradlew.bat test` 다.
- `admin-api`는 이제 모듈 계약을 통해 세션을 복원하며, 현재 구현체는 개발용 인메모리 adapter 다.
- 개발 환경에서도 `X-Admin-Session-Id` 경로를 우선 사용해 저장소 기반 세션 복원을 먼저 검증한다.
- ingestion 조회도 세션 조직 범위를 따라가며, 전역 역할은 전체 source/job을 조회한다.
- 중요한 변경은 `OpenSpec change -> 구현 -> 검증 -> 한글 커밋` 순서로 관리한다.
- ingestion 쓰기 흐름은 `source 생성 -> 수동 job 생성 -> 상태 전이` 3단계로 먼저 고정한다.
- ingestion job 상태 전이 검증은 `modules/ingestion-ops`의 단일 상태 머신을 기준으로 본다.
- 전체 진행 추적은 `17_development_wbs_milestones.md`의 마일스톤과 Gantt 상태를 기준으로 갱신한다.
- ingestion API 권한 검증은 `grantedActions + organization scope` 조합으로 판정한다.
- ingestion 조회/쓰기 엔드포인트는 이제 역할명이 아니라 액션 코드로 권한을 확인한다.
- 인증 API 기준선은 이제 `login -> me -> logout -> revoked/expired 401` 라이프사이클까지 포함한다.
- 명시적 `X-Admin-Session-Id`가 잘못됐을 때는 디버그 스텁으로 폴백하지 않는다.

### Next Actions

1. `identity-access`와 `organization-directory`를 실제 저장소 포트 기준으로 분리
2. `python/ingestion-worker`에 crawl source 실행 흐름과 job callback 스텁 추가
3. `tests/api`, `tests/e2e`에 auth/ingestion 범위 회귀 케이스를 추가
4. ingestion job 상세 조회와 재실행 API를 추가
5. 다음 구현도 `openspec/changes/<change-id>`를 먼저 생성하고 WBS 상태를 함께 갱신
