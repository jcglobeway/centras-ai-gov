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

All modules use Java 21 (`kotlin { jvmToolchain(21) }`).

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
