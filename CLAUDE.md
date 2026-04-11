# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`public-rag-ops-platform` is a multi-tenant RAG chatbot **operations platform** for Korean public institutions. It manages the full loop: citizen chat → answer generation → QA review → document ingestion → KPI reporting.

The product core is a **DDD-lite modular monolith** (Spring Boot + Kotlin). Python services handle only ingestion and RAG orchestration as external workers.

**Product PRD**: `docs/platform-prd.md` — 제품 요구사항 정의서 (단일 진실 출처)
**Implementation Gap**: `docs/implementation-gap.md` — PRD vs. 현재 구현 상태 비교표

---

## Build and Run Commands

### Kotlin / Spring Boot (Gradle)

```bash
# Build all modules
./gradlew build

# Run admin-api (Spring Boot)
./gradlew :apps:admin-api:bootRun

# Run all tests
JAVA_HOME=/Users/parkseokje/Library/Java/JavaVirtualMachines/openjdk-25.0.2/Contents/Home ./gradlew test

# Run tests for a specific subproject
./gradlew :apps:admin-api:test

# Run a single test class
./gradlew :apps:admin-api:test --tests "com.publicplatform.ragops.adminapi.SomeTest"

# Compile only (no tests)
./gradlew compileKotlin
```

### Frontend (Next.js)

```bash
cd frontend
npm install
npm run dev   # http://localhost:3000
```

### Python Services

```bash
# Ingestion worker (Typer CLI)
cd python/ingestion-worker
pip install -e .
ingestion-worker run --job-id <id>

# RAG orchestrator (FastAPI on port 8090)
cd python/rag-orchestrator
pip install -e .
rag-orchestrator

# Eval runner
cd python/eval-runner
pip install -e .
```

### Infrastructure

```bash
docker-compose up -d   # PostgreSQL + Redis
```

---

## Architecture

### System Boundary

| Layer | Technology | Role |
|---|---|---|
| Admin API | Spring Boot + Kotlin | System of record, all operational state |
| Frontend | Next.js 15 (App Router) | 3-portal admin UI (ops / client / qa) |
| Product DB | PostgreSQL | Single source of truth |
| Cache / Queue | Redis | Triggers, session cache |
| Ingestion Worker | Python (Typer CLI) | Crawl → parse → chunk → embed → index |
| RAG Orchestrator | Python (FastAPI, port 8090) | pgvector retrieval + answer synthesis |
| Vector Index | pgvector (`document_chunks`) | Embedding retrieval |

**Spring Boot is the system of record.** Python services are workers/adapters only.

### Gradle Module Structure

```
apps/
  admin-api/          # Spring Boot application; aggregates all modules

modules/
  shared-kernel/      # DomainEvent interface and shared primitives
  identity-access/    # Auth, sessions, admin users, roles
  organization-directory/  # Multi-tenant org and service registry
  chat-runtime/       # Chat sessions, questions, answers, feedbacks
  document-registry/  # Document metadata, versions, chunks
  ingestion-ops/      # Crawl sources, ingestion jobs
  qa-review/          # QA review workflow and state machine
  metrics-reporting/  # KPI aggregation and daily snapshots

python/
  ingestion-worker/   # Full pipeline: FETCH → EXTRACT → CHUNK → EMBED → INDEX
  rag-orchestrator/   # FastAPI: pgvector retrieval + answer synthesis + /evaluate
  eval-runner/        # RAGAS evaluation runner
  common/             # Shared Python utilities

frontend/
  src/app/
    ops/              # 운영사 포털 (ops_admin / super_admin)
    client/           # 고객사 포털 (client_org_admin / client_viewer)
    qa/               # 품질관리 포털 (qa_manager / knowledge_editor)
```

All modules use Java 25 (`kotlin { jvmToolchain(25) }`).

### Implementation Status

- **7 modules** fully implemented with JPA + PostgreSQL
- **Flyway migrations**: V001–V029 (V018 = Kotlin pgvector migration, V029 = question_embedding 컬럼)
- **50 integration tests** (100% passing) + 8 ArchUnit rules
- **Frontend**: Next.js 15, 3-portal structure, "Control Tower" dark theme
- **Hexagonal architecture**: canonical package structure 완성

### Bounded Context Responsibilities

| Context | Tables | 역할 |
|---------|--------|------|
| identity-access | `admin_users`, `admin_user_roles`, `admin_sessions`, `audit_logs` | 로그인, 세션, 역할 |
| organization-directory | `organizations`, `services` | 멀티테넌트 스코프 기준 |
| chat-runtime | `chat_sessions`, `questions`, `answers`, `feedbacks`, `rag_search_logs` | 시민 인터랙션 전체 |
| document-registry | `documents`, `document_versions`, `document_chunks` | 인덱싱 상태, 벡터 검색 |
| ingestion-ops | `crawl_sources`, `ingestion_jobs` | 수집 정책, 잡 생명주기 |
| qa-review | `qa_reviews` | 검수 상태 머신 |
| metrics-reporting | `daily_metrics_org`, `ragas_evaluations` | KPI 스냅샷, RAGAS 평가 |

---

## Database Schema (Flyway Migrations)

### Core Tables (V001–V015)

| Migration | 내용 |
|-----------|------|
| V001 | `admin_users`, `admin_user_roles` (6 seed users) |
| V002 | `admin_sessions` |
| V003 | `audit_logs` |
| V004 | `organizations` |
| V005 | `services` |
| V006 | `crawl_sources` |
| V007 | `ingestion_jobs` |
| V008 | `qa_reviews` |
| V009 | `chat_sessions` |
| V010 | `questions` |
| V011 | `answers` |
| V012 | QA reviews ↔ questions FK |
| V013 | `documents` |
| V014 | `document_versions` |
| V015 | `daily_metrics_org` |

### Extended Tables (V016–V027)

| Migration | 내용 |
|-----------|------|
| V016 | `document_chunks` (embedding_vector vector(1024)/PostgreSQL) |
| V017 | `rag_search_logs`, `rag_retrieved_documents` |
| V018 | Kotlin migration — pgvector extension + ALTER document_chunks |
| V019 | `feedbacks` (citizen satisfaction ratings) |
| V020 | 역할 6개로 확장 (super_admin, ops_admin, qa_manager, client_org_admin, client_viewer, knowledge_editor) |
| V021 | `questions` 확장: `question_category`, `failure_reason_code` (A01~A10), `is_escalated`, `answer_confidence` |
| V022 | `feedbacks` 확장: 세션 종료 유형·암묵적 신호 |
| V023 | `daily_metrics_org` 확장: 고객사 KPI 10종 (auto_resolution_rate, escalation_rate 등) |
| V024 | 데모 데이터 시드 |
| V025 | `ragas_evaluations` |
| V026 | `answers` 확장: LLM 메트릭 (model_name, tokens, cost, finish_reason) |
| V027 | 공공 문서 + LLM 메트릭 시드 데이터 |

**Test environment**: Testcontainers (PostgreSQL), `spring.flyway.target: "46"`
**Production**: PostgreSQL 15+ via `docker-compose.yml`

---

## Hexagonal Architecture Guidelines

### 패키지 구조 (Canonical)

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
|--------|------|
| **domain** | 순수 비즈니스 모델. `@Entity` 등 JPA/Spring 어노테이션 금지 |
| **application.port.in** | UseCase 인터페이스. Controller가 이를 호출 |
| **application.port.out** | 아웃바운드 포트 인터페이스. Adapter가 구현 |
| **application.service** | UseCase 구현체. Port 인터페이스를 통해서만 인프라 접근 |
| **adapter.outbound.persistence** | JPA Entity + Spring Data Repository + PortAdapter 구현체 |
| **adapter.inbound.web** | HTTP Controller. UseCase 인터페이스에만 의존 |

### 의존성 규칙

1. **안쪽 방향만**: `adapter` → `application.port` → `domain`. 역방향 불가.
2. **domain 순수성**: domain 레이어는 `jakarta.persistence.*`, `org.springframework.*` 의존 금지.
3. **service 격리**: `application.service`는 `adapter.outbound.persistence` 직접 참조 금지.
4. **controller 격리**: Controller는 JPA Entity·Adapter 직접 참조 금지. UseCase 인터페이스만 호출.
5. **엔티티-도메인 분리**: `*Entity` ↔ domain model 변환은 반드시 `toSummary()` / `toEntity()` 매퍼 사용.

### 명명 규칙

- **Port 인터페이스**: 행위 중심 — `LoadQuestionPort`, `RecordQuestionPort`
- **Port Adapter**: Port 이름 + `Adapter` suffix — `LoadQuestionPortAdapter`
- **파일명 = 클래스명**: Kotlin 파일명은 반드시 최상위 클래스명과 일치
- **JPA Repository**: `Jpa{Aggregate}Repository`
- **UseCase 인터페이스**: `{동사}{대상}UseCase`
- **Service 구현체**: `{동사}{대상}Service`

### 코드 생성 규칙 (신규 도메인 추가 시)

- `domain`, `application`, `adapter` 세 레이어를 **동시에** 생성할 것.
- 새 Bean은 `@Component` 대신 `RepositoryConfiguration` / `ServiceConfiguration`에 `@Bean`으로 명시 등록.
- Adapter 클래스는 반드시 `open class` (Spring CGLIB 프록시 요건).
- admin-api에 JPA 엔티티 추가 시 `kotlin("plugin.jpa")` 필수 (no-arg constructor 생성).

### ArchUnit 보호 규칙 (`ArchitectureTest.kt`, 8개)

| Rule | 내용 |
|------|------|
| 1 | `domain` → JPA/Spring Data 의존 금지 |
| 2 | `application.port.out` → `adapter.outbound.persistence` 역방향 금지 |
| 3 | `application.port.in` → `adapter.outbound.persistence` 역방향 금지 |
| 4 | `*Controller` → `adapter.outbound.persistence` 직접 접근 금지 |
| 5 | `Jpa*Repository` → `RepositoryConfiguration`/`ServiceConfiguration`에서만 접근 |
| 6 | 모듈 간 순환 의존성 금지 |
| 7 | `application.service` → `adapter.outbound.persistence` 직접 접근 금지 |
| 8 | `domain` → `application` 역방향 의존 금지 |

---

## Key Design Rules

### Permission Model

- Permission checks are **action-based**, not screen-based.
- Every admin request must restore `user_id`, `role_code`, and `organization_scope` from the session.
- **6개 역할**: `super_admin` (전체), `ops_admin` (운영), `qa_manager` (검수), `client_org_admin` (기관 관리), `client_viewer` (기관 조회), `knowledge_editor` (문서 편집)
- Unauthorized action → `403`; resource hidden by scope → `404`.
- High-risk actions must write to `audit_logs`.

### API Contracts

- All admin endpoints are under `/admin/*`.
- All list endpoints share the filter pattern: `organization_id`, `service_id`, `from`, `to`, `page`, `page_size`.
- All responses include `request_id` and `generated_at`.
- Error shape: `{ "error": { "code": "...", "message": "...", "request_id": "..." } }`.

### State Machines

**Ingestion Job**: `pending → queued → running → success | failed | cancelled`

**QA Review**:
- `pending → confirmed_issue → resolved` (valid)
- `pending → false_alarm` (valid, forces `action_type = no_action`)
- `false_alarm → resolved` (prohibited)
- `confirmed_issue` requires `root_cause_code` and `action_type`.

### Unresolved Queue Visibility Rule

Show if `answer_status` ∈ {`fallback`, `no_answer`, `error`} **OR** latest `qa_review.review_status = confirmed_issue`.
Exclude if latest review is `resolved` or `false_alarm`.

Implementation: native SQL in `JpaQuestionRepository.findUnresolvedQuestions()` (avoids circular dependency).

### Failure Reason Code (A01–A10)

`questions.failure_reason_code` 컬럼 + `FailureReasonCode` enum (chat-runtime/domain):

| 코드 | 원인 | 조치 주체 |
|------|------|-----------|
| A01 | 관련 문서 없음 | 고객사 |
| A02 | 문서 최신 아님 | 고객사 |
| A03 | 파싱 실패 | 운영사 |
| A04 | 검색 실패 | 운영사 |
| A05 | 재랭킹 실패 | 운영사 |
| A06 | 생성 답변 왜곡 (환각) | 운영사 |
| A07 | 질문 의도 분류 실패 | 운영사 |
| A08 | 정책상 답변 제한 | 협의 |
| A09 | 질문 표현 모호함 | 고객사 |
| A10 | 채널 UI/입력 문제 | 운영사 |

---

## Important Implementation Patterns

### JPA Entity Mapping

**Enum storage**: Store as lowercase strings in DB, convert to Enum in code.
```kotlin
@Column(name = "answer_status", nullable = false)
val answerStatus: String  // "answered", "fallback", etc.
```

**Adapter classes**: Must be `open class` for Spring CGLIB proxy support.

**Circular dependencies**: Use native queries when needed (e.g., chat-runtime querying qa_reviews without importing qa-review module).

### Repository Bean Registration

- Adapters are NOT `@Component` — registered via explicit `@Bean` in `RepositoryConfiguration`.
- Spring Data JPA repositories ARE `@Repository`.

### Scope-based Filtering

```kotlin
override fun listX(scope: XScope): List<XSummary> {
    val all = jpaRepository.findAll().map { it.toSummary() }
    return if (scope.globalAccess) all
           else all.filter { it.organizationId in scope.organizationIds }
}
```

Scope types: `IngestionScope`, `ChatScope`, `DocumentScope`, `MetricsScope`.

### Dynamic ID Generation

```kotlin
val id = "prefix_${UUID.randomUUID().toString().substring(0, 8)}"
```

Prefixes: `crawl_src_`, `ing_job_`, `question_`, `answer_`, `qa_rev_`, etc.

---

## Testing Strategy

**50 integration tests + 8 ArchUnit rules (100% passing)**

| Suite | Tests | 내용 |
|-------|-------|------|
| Auth/session | 9 | 로그인, 로그아웃, 세션 복원, 만료 |
| Ingestion | 16 | Source CRUD, job 전이, 스코프 검증 |
| QA Review | 5 | 상태 머신, 유효성, 권한 |
| Chat Runtime | 5 | 질문 생성, 미응답 큐, 문서, 메트릭 |
| E2E | 4 | 전체 플로우, 멀티테넌트 격리 |
| RAGAS Evaluation | 3 | `POST /admin/ragas-evaluations` |
| ArchUnit | 8 | 의존성 방향, 레이어 격리, 순환 탐지 |

**Test pattern**:
- `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`
- `@DirtiesContext(AFTER_CLASS)` for test isolation
- Testcontainers (PostgreSQL), `flyway.target: "46"`
- Helper: `loginAndReturnSessionId()`, `createQuestionAndReturnId()`

---

## OpenSpec Workflow

모든 주요 변경은 OpenSpec change로 추적한다.

```
openspec/
  changes/      # 진행 중인 변경
  archive/      # 완료된 변경
  templates/    # 템플릿
```

**Process**:
1. `openspec/changes/<change-id>/` 생성, 템플릿 복사
2. `proposal.md` (범위·영향), `tasks.md` (체크리스트), `status.md` (진행 현황) 작성
3. `tasks.md` 체크박스 업데이트하며 구현
4. 테스트 통과 후 `status.md` 업데이트
5. 단일 커밋, 한국어 커밋 메시지
6. `openspec/archive/<change-id>/` 로 이동

---

## Reference Documents

| 파일 | 내용 |
|------|------|
| `docs/platform-prd.md` | **제품 PRD** — 단일 진실 출처 |
| `docs/implementation-gap.md` | PRD vs. 현재 구현 상태 비교표 |
| `docs/rag-pipeline-1pager.md` | RAG 품질 지표·자동화 평가 기술 참고 |
| `mvp_docs/04_data_api.md` | 전체 테이블 스키마, 상태 코드, API 계약 |
| `mvp_docs/09_unresolved_qa_state_machine.md` | 미응답 큐 가시성 규칙, QA 리뷰 상태 전이 |
| `mvp_docs/10_auth_authz_api.md` | Auth API, 세션 생명주기, 감사 규칙 |
| `openspec/archive/` | 완료된 변경사항 이력 |