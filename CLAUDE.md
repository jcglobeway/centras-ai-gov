# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`public-rag-ops-platform` is a multi-tenant RAG chatbot **operations platform** for Korean public institutions. It manages the full loop: citizen chat → answer generation → QA review → document ingestion → KPI reporting.

The product core is a **DDD-lite modular monolith** (Spring Boot + Kotlin). Python services handle only ingestion and RAG orchestration as external workers.

---

## Build and Run Commands

### Kotlin / Spring Boot (Gradle)

```bash
# Build all modules
./gradlew build

# Run admin-api (Spring Boot)
./gradlew :apps:admin-api:bootRun

# Run all tests
./gradlew test

# Run tests for a specific subproject
./gradlew :apps:admin-api:test

# Run a single test class
./gradlew :apps:admin-api:test --tests "com.publicplatform.ragops.adminapi.SomeTest"

# Compile only (no tests)
./gradlew compileKotlin
```

### Python Services

Each Python service uses `hatchling` and targets Python >= 3.12.

```bash
# Ingestion worker (Typer CLI)
cd python/ingestion-worker
pip install -e .
ingestion-worker run

# RAG orchestrator (FastAPI on port 8090)
cd python/rag-orchestrator
pip install -e .
rag-orchestrator
```

---

## Architecture

### Hybrid System Boundary

| Layer | Technology | Role |
|---|---|---|
| Admin API | Spring Boot + Kotlin | System of record, all operational state |
| Product DB | PostgreSQL | Single source of truth |
| Cache / Queue | Redis | Triggers, session cache |
| Ingestion Worker | Python (Typer CLI) | Crawl, parse, chunk, embed, index |
| RAG Orchestrator | Python (FastAPI) | Query rewrite, retrieval, reranking, answer synthesis |
| Search Index | OpenSearch or pgvector | Vector retrieval (outside this repo) |

**Spring Boot is the system of record.** Python services are workers/adapters only. All operational state flows back into the product DB via API callbacks or direct writes.

### Gradle Module Structure

```
apps/
  admin-api/          # Spring Boot application; aggregates all modules

modules/
  shared-kernel/      # DomainEvent interface and shared primitives
  identity-access/    # Auth, sessions, admin users, roles
  organization-directory/  # Multi-tenant org and service registry
  chat-runtime/       # Chat sessions, questions, answers
  document-registry/  # Document metadata, versions, chunks
  ingestion-ops/      # Crawl sources, ingestion jobs
  qa-review/          # QA review workflow and state machine
  metrics-reporting/  # KPI aggregation and daily snapshots

python/
  ingestion-worker/   # CLI worker: crawl → parse → chunk → embed → index
  rag-orchestrator/   # FastAPI: retrieval adapter and answer synthesis
  common/             # Shared Python utilities
```

All modules use Java 25 (`kotlin { jvmToolchain(25) }`).

### Implementation Status

**✅ All 7 modules fully implemented with JPA + PostgreSQL**:
- identity-access, organization-directory, ingestion-ops, qa-review, chat-runtime, document-registry, metrics-reporting
- 19 Flyway migrations (V001-V019; V018 = Kotlin migration for pgvector, no-op on H2)
- 44 integration tests (100% passing) + 8 ArchUnit rules
- Hexagonal architecture (canonical package structure)

### Bounded Context Responsibilities

- **identity-access**: `admin_users`, `admin_user_roles`, `admin_sessions`, `audit_logs`. Handles login, session restore, role assignment.
- **organization-directory**: `organizations`, `services`. Top-level multi-tenant scoping key for all other tables.
- **chat-runtime**: `chat_sessions`, `questions`, `answers`. Primary record for citizen interaction; drives unresolved queue and QA.
- **document-registry**: `documents`, `document_versions`, `document_chunks`. Tracks ingestion/index status and content versioning.
- **ingestion-ops**: `crawl_sources`, `ingestion_jobs`. Controls crawl execution policy and tracks job lifecycle.
- **qa-review**: `qa_reviews`. Append-only review records with state machine (`pending → confirmed_issue → resolved`).
- **metrics-reporting**: `daily_metrics_org`. Pre-aggregated KPI snapshots; never computed on demand from raw logs.
- **shared-kernel**: `DomainEvent` interface only.

---

## Database Schema (Flyway Migrations)

**15 tables across 7 bounded contexts** (V001-V015):

### Identity & Access
- V001: `admin_users`, `admin_user_roles` (3 seed users, 3 roles)
- V002: `admin_sessions` (session snapshot as JSON, 3 seed sessions)
- V003: `audit_logs` (action tracking)

### Organization
- V004: `organizations` (2 seed orgs: Seoul, Busan)
- V005: `services` (2 seed services: welfare, faq)

### Ingestion
- V006: `crawl_sources` (2 seed sources)
- V007: `ingestion_jobs` (2 seed jobs: succeeded, failed)

### QA & Chat
- V008: `qa_reviews` (append-only, state machine)
- V009: `chat_sessions` (2 seed sessions)
- V010: `questions` (3 seed questions)
- V011: `answers` (3 seed: answered, no_answer, fallback)
- V012: QA reviews ↔ questions FK (reviewer FK deferred)

### Documents & Metrics
- V013: `documents` (2 seed docs)
- V014: `document_versions` (2 seed versions)
- V015: `daily_metrics_org` (2 seed metrics)

**Test environment**: H2 in-memory (MODE=PostgreSQL) with automatic Flyway migration.
**Production**: PostgreSQL 15+ via docker-compose.yml.

---

## Hexagonal Architecture Guidelines

### 패키지 구조 (Canonical)

모든 모듈은 아래 canonical 헥사고날 패키지 구조를 따른다:

```
{module}/src/main/kotlin/com/publicplatform/ragops/{context}/
  domain/                        # 순수 비즈니스 모델 (data class, 프레임워크 의존 금지)
  application/
    port/
      in/                        # UseCase 인터페이스 (driving port)
      out/                       # Load*/Record*/Save*/Persist*Port 인터페이스 (driven port)
    service/                     # UseCase 구현체
  adapter/
    outbound/
      persistence/               # *Entity.kt, Jpa*Repository.kt, *PortAdapter.kt

apps/admin-api/.../
  adapter/inbound/web/           # *Controller.kt (UseCase 인터페이스에만 의존)
  config/
    RepositoryConfiguration.kt   # 어댑터 @Bean 명시 등록
    ServiceConfiguration.kt      # 서비스 @Bean 명시 등록
```

### 레이어 책임

| 레이어 | 역할 |
|---|---|
| **domain** | 순수 비즈니스 모델. `@Entity` 등 JPA/Spring 어노테이션 금지 |
| **application.port.in** | UseCase 인터페이스. Controller가 이를 호출 |
| **application.port.out** | 아웃바운드 포트 인터페이스. Adapter가 구현 |
| **application.service** | UseCase 구현체. Port 인터페이스를 통해서만 인프라 접근 |
| **adapter.outbound.persistence** | JPA Entity + Spring Data Repository + PortAdapter 구현체 |
| **adapter.inbound.web** | HTTP Controller. UseCase 인터페이스에만 의존 |

### 의존성 규칙

1. **안쪽 방향만**: `adapter` → `application.port` → `domain`. 역방향 불가.
2. **domain 순수성**: domain 레이어는 `application.*`, `jakarta.persistence.*`, `org.springframework.*` 의존 금지.
3. **service 격리**: `application.service`는 `adapter.outbound.persistence`를 직접 참조 금지. 반드시 port 인터페이스 경유.
4. **controller 격리**: Controller는 JPA Entity·Adapter를 직접 참조 금지. UseCase 인터페이스만 호출.
5. **엔티티-도메인 분리**: `*Entity` ↔ domain model 변환은 반드시 `toSummary()` / `toEntity()` 매퍼 사용.

### 명명 규칙

- **Port 인터페이스**: 행위 중심 — `LoadQuestionPort`, `RecordQuestionPort`, `PersistIngestionJobPort`
- **Port Adapter**: Port 이름 + `Adapter` suffix — `LoadQuestionPortAdapter`, `RecordQuestionPortAdapter`
- **파일명 = 클래스명**: Kotlin 파일명은 반드시 최상위 클래스명과 일치해야 한다.
- **JPA Repository**: `Jpa{Aggregate}Repository` (예: `JpaQuestionRepository`)
- **UseCase 인터페이스**: `{동사}{대상}UseCase` (예: `CreateQuestionUseCase`, `ListQuestionsUseCase`)
- **Service 구현체**: `{동사}{대상}Service` (예: `CreateQuestionService`)

### 코드 생성 규칙 (신규 도메인 추가 시)

- `domain`, `application`, `adapter` 세 레이어를 **동시에** 생성할 것.
- 도메인 모델은 Kotlin `data class`를 우선 사용할 것.
- Port 인터페이스를 먼저 정의하고, 이후 Adapter에서 구현할 것.
- 새 Bean은 `@Component` 대신 `RepositoryConfiguration` / `ServiceConfiguration`에 `@Bean`으로 명시 등록.
- Adapter 클래스는 반드시 `open class` (Spring CGLIB 프록시 요건).
- 모든 Kotlin 파일 최상단에 파일 레벨 KDoc 작성 필수.

### ArchUnit 보호 규칙 (`ArchitectureTest.kt`, 8개)

| Rule | 내용 |
|---|---|
| 1 | `domain` → JPA/Spring Data 의존 금지 |
| 2 | `application.port.out` → `adapter.outbound.persistence` 역방향 금지 |
| 3 | `application.port.in` → `adapter.outbound.persistence` 역방향 금지 |
| 4 | `*Controller` → `adapter.outbound.persistence` 직접 접근 금지 |
| 5 | `Jpa*Repository` → `adapter.outbound.persistence` 또는 `RepositoryConfiguration`/`ServiceConfiguration`에서만 접근 |
| 6 | 모듈 간 순환 의존성 금지 |
| 7 | `application.service` → `adapter.outbound.persistence` 직접 접근 금지 |
| 8 | `domain` → `application` 역방향 의존 금지 |

**Testing**:
- Tests use H2 in-memory with `@ActiveProfiles("test")`
- `@DirtiesContext` for test isolation
- All adapters work with both H2 (test) and PostgreSQL (production)
- All modules include `kotlin("plugin.spring")` and `kotlin("plugin.jpa")`

---

## OpenSpec Workflow

**All significant changes are tracked as OpenSpec changes**:

```
openspec/
  changes/      # In-progress changes
  archive/      # Completed changes
  templates/    # Templates for new changes
```

**Process**:
1. Create change: `mkdir openspec/changes/<change-id>`, copy templates
2. Fill in: `proposal.md` (scope, impact), `tasks.md` (checklist), `status.md` (progress)
3. Implement: Update `tasks.md` checkboxes as you work
4. Verify: Run tests, update `status.md`
5. Commit: Single commit per change, Korean commit message
6. Archive: Move to `openspec/archive/<change-id>/`

**Completed changes**: 10 changes (see `openspec/archive/`)

---

## Python Worker Integration

### Ingestion Worker

**Implemented** (python/ingestion-worker):
- AdminApiClient (httpx, X-Admin-Session-Id auth)
- CrawlExecutor (Playwright async, screenshot stub)
- IngestionJobRunner (job lifecycle: queued → running → succeeded/failed)
- CLI: `ingestion-worker run --job-id <id>`

**Environment**:
```bash
ADMIN_API_BASE_URL=http://localhost:8080
ADMIN_API_SESSION_TOKEN=<session_token>
```

**Job callback flow**:
1. Worker reads job from admin-api: `GET /admin/ingestion-jobs/{id}`
2. Worker reads source: `GET /admin/crawl-sources/{id}`
3. Worker transitions: `POST /admin/ingestion-jobs/{id}/status`
4. Repeat for each stage (fetch → extract → complete)

**Dependencies**: playwright (chromium), httpx, pydantic, typer

### RAG Orchestrator

**Planned** (python/rag-orchestrator):
- FastAPI service (port 8090)
- Query rewrite, retrieval adapter, answer synthesis
- admin-api calls rag-orchestrator to generate answers

---

## Testing Strategy

**Current coverage: 44 API/integration tests + 8 ArchUnit rules (100% passing)**

Test distribution:
- Auth/session: 9 tests (login, logout, session restore, expiry)
- Ingestion: 16 tests (source CRUD, job transition, scope validation)
- QA Review: 5 tests (state machine, validation, permissions)
- Chat Runtime: 5 tests (question creation, unresolved queue, documents, metrics)
- E2E scenarios: 4 tests (full auth flow, full ingestion flow, multi-tenant isolation)
- ArchUnit: 8 rules (dependency direction, layer isolation, cycle detection)

**Test pattern**:
- `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`
- `@DirtiesContext` for test isolation
- H2 in-memory DB (MODE=PostgreSQL)
- Flyway migrations auto-run in tests
- Helper functions: `loginAndReturnSessionId()`, `createQuestionAndReturnId()`

**Run tests**: `./gradlew test` (H2) or `./gradlew :apps:admin-api:test`

---

## Key Design Rules

### Package Structure

Organize by **bounded context** (vertical slice), not by technical layer. Do not separate `controller/`, `service/`, `entity/` horizontally across contexts.

### Domain Model

- Do **not** expose JPA entities directly as API response models.
- Encapsulate state transitions and permission checks inside domain rules, not in controllers or services.
- Prefer soft delete (`status` field or `deleted_at`) over hard delete.

### Permission Model

- Permission checks are **action-based**, not screen-based.
- Every admin request must restore `user_id`, `role_code`, and `organization_scope` from the session.
- Three roles: `ops_admin` (all orgs), `client_admin` (own org only), `qa_admin` (assigned org scope).
- Unauthorized action → `403`; resource hidden by scope → `404`.
- High-risk actions must write to `audit_logs`.

### API Contracts

- All admin endpoints are under `/admin/*`.
- All list endpoints share the filter pattern: `organization_id`, `service_id`, `from`, `to`, `page`, `page_size`.
- All responses include `request_id` and `generated_at`.
- Error shape: `{ "error": { "code": "...", "message": "...", "request_id": "..." } }`.
- API contracts are **independent of RAG backend**. Swapping OpenRAG or the ingestion runtime must not change the API shape.

### Organization Scoping

Every business table must be reachable to `organization_id` (direct column or via FK join). This is enforced as a data constraint, not just a query convention.

### Ingestion Job State Machine

`pending → queued → running → success | failed | cancelled`

Trigger types: `schedule`, `manual`, `qa_request`, `document_event`.
Runner types: `python_worker`, `openrag_flow`, `spring_batch`.

### QA Review State Machine

`pending → confirmed_issue → resolved` (valid)
`pending → false_alarm` (valid, forces `action_type = no_action`)
`false_alarm → resolved` (prohibited)

`confirmed_issue` requires `root_cause_code` and `action_type`.

### Unresolved Queue Visibility Rule

Show in unresolved queue if:
- `answer_status` is `fallback`, `no_answer`, or `error`, **OR**
- `answer_status = answered` but latest `qa_review.review_status = confirmed_issue`

Exclude if latest review is `resolved` or `false_alarm`.

**Implementation**: Native SQL query in `JpaQuestionRepository.findUnresolvedQuestions()` (avoids circular dependency between chat-runtime and qa-review modules).

---

## Important Implementation Patterns

### JPA Entity Mapping

**Enum storage**: Store as lowercase strings in DB, convert to Enum in code.
```kotlin
// Entity
@Column(name = "answer_status", nullable = false)
val answerStatus: String  // "answered", "fallback", etc.

// Conversion
private fun String.toAnswerStatus(): AnswerStatus =
    when (this) {
        "answered" -> AnswerStatus.ANSWERED
        "fallback" -> AnswerStatus.FALLBACK
        else -> AnswerStatus.ERROR
    }
```

**JSON serialization**: Use Jackson for complex objects (e.g., AdminSessionSnapshot in admin_sessions.snapshot_json).

**Adapter classes**: Must be `open class` (not final) for Spring CGLIB proxy support.

**Circular dependencies**: Avoid module dependencies that create cycles. Use native queries if needed (e.g., chat-runtime querying qa_reviews table without importing qa-review module).

### Repository Bean Registration

All repository adapters are registered in `apps/admin-api/config/RepositoryConfiguration`:
- Adapters are NOT annotated with `@Component` (to avoid auto-scanning)
- Explicit `@Bean` methods for each adapter
- Spring Data JPA repositories ARE annotated with `@Repository`

### Scope-based Filtering

All Reader adapters implement organization-based filtering:
```kotlin
override fun listX(scope: XScope): List<XSummary> {
    val all = jpaRepository.findAll().map { it.toSummary() }
    return if (scope.globalAccess) {
        all
    } else {
        all.filter { it.organizationId in scope.organizationIds }
    }
}
```

**Scope types**: `IngestionScope`, `ChatScope`, `DocumentScope`, `MetricsScope` (all have same structure).

### Dynamic ID Generation

Use UUID for new resources:
```kotlin
val id = "prefix_${UUID.randomUUID().toString().substring(0, 8)}"
```

Prefixes: `crawl_src_`, `ing_job_`, `question_`, `answer_`, `qa_rev_`, etc.

### Test Data Setup

Tests that need related resources:
```kotlin
// 1. Create question first
val questionId = createQuestionAndReturnId()

// 2. Then create review
mockMvc.post("/admin/qa-reviews") {
    content = """{"questionId": "$questionId", ...}"""
}
```

**FK constraints**: Questions must exist before QA reviews (V012 adds FK).

---

## Test Layout

```
tests/
  unit/    # Domain policy: state transitions, permission actions, aggregation functions
  api/     # Auth/session APIs, question APIs, QA review API, document ops API, dashboard API
  e2e/     # Full operating loop scenarios (login → unresolved → QA → reindex)
  data/    # KPI aggregation correctness, trace_id/request_id coverage, audit log presence
```

Run order: unit → api → e2e → data. This order isolates failure causes fastest.

The product contract tests (unit, api, e2e) must pass regardless of which RAG backend (self-built vs OpenRAG) is active.

---

## Reference Documents

Design decisions, API contracts, and data schemas are maintained in `mvp_docs/`:

| File | Contents |
|---|---|
| `01_mvp_prd.md` | Product goals, scope, KPI definitions |
| `04_data_api.md` | Full table schemas, status codes, API contracts |
| `05_architecture_openrag.md` | Architecture decision: modular monolith + Python workers |
| `06_access_policy.md` | Screen-level access policy by role |
| `10_auth_authz_api.md` | Auth API contracts, session lifecycle, audit rules |
| `12_test_strategy.md` | Test level mapping and Sprint 1 automation scope |
| `16_springboot_kotlin_ddd_msa_review.md` | Why DDD-lite monolith over full MSA |
| `09_unresolved_qa_state_machine.md` | Unresolved queue visibility and QA review state transitions |
| `99_worklog.md` | Development history and completed changes |

---

## Next Steps

### Remaining Work

**Python services** (not yet implemented):
- RAG Orchestrator (FastAPI): Query rewrite, retrieval, answer synthesis
- Actual parsing in ingestion-worker: HTML/PDF parsing, chunking, embedding

**Integration**:
- Run PostgreSQL: `docker-compose up -d`
- Connect admin-api to PostgreSQL (currently test mode uses H2)
- Test Python worker with real job execution

**Production readiness**:
- Logging improvements (request_id, trace_id)
- Error response unification
- Health check enhancements
- CI/CD pipeline

---

## R&D 기반 V2 변경 계획 (crawler-poc PRD 기반)

**R&D 실험 완료 전에는 파싱 파이프라인·벡터 검색 관련 코드를 추가하지 않는다.**
실험은 `web2rag-poc/experiments/` 에서 진행.

### 현재 진행 중인 openspec changes (완료 우선)
1. `extend-roles-6`
2. `extend-question-model`
3. `extend-feedback-model`
4. `extend-daily-metrics`
5. `standardize-failure-codes`

### 바로 구현 가능 (R&D 불필요)
| Change | 우선순위 | 내용 |
|---|---|---|
| `enhance-security` | HIGH | bcrypt 해싱, rate limiting, CORS |
| `add-docker-build` | HIGH | 멀티스테이지 Dockerfile × 3 서비스 |
| `add-production-logging` | MEDIUM | JSON 로그, request_id/trace_id |
| `unify-error-handling` | MEDIUM | GlobalExceptionHandler, 에러 코드 체계 |

### R&D 완료 후 proposal (의존 실험 완료 시)
| Change | 의존 실험 | 주요 대상 파일 |
|---|---|---|
| `add-document-parsing-pipeline` | Exp-1, 3, 4 | `python/ingestion-worker/src/ingestion_worker/` |
| `add-vector-search-retrieval` | Exp-4, 5 | `python/rag-orchestrator/src/rag_orchestrator/retrieval.py` |

### V1 호환성 유지 사항
- `UnifiedDocument` ↔ V1 MetadataModel 필드 매핑
- 청킹 전략 이름: government / legal / sections / form / faq / casebook / semantic / recursive
- `chunk_size=600`, `chunk_overlap=100` 기본값

---

### Completed Today (10 OpenSpec Changes)

1. `separate-repository-ports-identity-org`: Repository ports and in-memory adapters
2. `add-ingestion-worker-crawl-flow`: Python worker + Playwright crawler
3. `add-auth-ingestion-test-cases`: Individual resource endpoint tests
4. `add-jpa-entities-identity-org`: PostgreSQL + JPA for identity/org
5. `add-jpa-entities-ingestion-ops`: JPA for ingestion-ops
6. `add-e2e-auth-ingestion-flow`: E2E integration tests
7. `add-qa-review-module`: QA review state machine
8. `add-chat-runtime-module`: Questions/answers/unresolved queue
9. `add-document-registry-module`: Document metadata
10. `add-metrics-reporting-module`: KPI dashboard

**Result**: MVP core loop fully operational (question → answer → unresolved → QA → ingestion).
