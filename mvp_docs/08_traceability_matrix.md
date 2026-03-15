# MVP Traceability Matrix

## Purpose

이 문서는 화면, API, 핵심 테이블의 연결 관계를 한 번에 보기 위한 매핑 문서다.
구현 우선순위, 테스트 범위, 변경 영향도를 빠르게 판단하는 기준으로 사용한다.

## Mapping Rule

- 화면은 `03_screen_spec.md` 기준 명칭을 사용한다.
- API는 `04_data_api.md` 기준 계약을 사용한다.
- 권한 액션은 `10_auth_authz_api.md` 기준 계약을 사용한다.
- 테이블은 제품 DB 기준 최소 스키마만 우선 연결한다.
- OpenRAG 연동 여부와 무관하게 데이터 저장 책임은 제품 테이블 기준으로 본다.

## 1. Operations Dashboard

화면 목적:
- 운영사가 기관 전체 상태와 이상 징후를 통합 조회

선행 권한:
- 허용 역할: `ops_admin`
- 필수 액션: `dashboard.read`, `metrics.read`

연결 API:
- `GET /admin/ops/dashboard`
- `GET /admin/rag/search-logs`
- `GET /admin/metrics/daily`

주요 읽기 테이블:
- `daily_metrics_org`
- `organizations`
- `documents`
- `rag_search_logs`

주요 쓰기 테이블:
- 직접 쓰기 없음
- 액션 연계 시 `documents` 상태 변경 또는 별도 작업 큐 생성

핵심 검증 포인트:
- 기간 필터에 따라 KPI 집계가 일관되게 내려오는지
- `zero_result_rate`, `fallback_rate`, `avg_response_time_ms`가 대시보드와 상세 로그에서 맞는지
- stale 문서 기관이 alert로 노출되는지

## 2. Organization Management

화면 목적:
- 기관별 운영 상태와 담당 범위를 관리

선행 권한:
- 허용 역할: `ops_admin`
- 제한 허용 역할: `client_admin` 읽기 일부
- 필수 액션: `organization.read`
- 수정 시 추가 액션: `organization.update`

연결 API:
- `GET /admin/organizations`
- `GET /admin/documents`
- `GET /admin/metrics/daily`

주요 읽기 테이블:
- `organizations`
- `services`
- `documents`
- `daily_metrics_org`

주요 쓰기 테이블:
- `organizations`

핵심 검증 포인트:
- 고객사 사용자는 자기 기관만 조회 가능한지
- 기관 상태값과 최근 동기화 시각이 최신 문서 상태와 맞는지

## 3. Document Source Management

화면 목적:
- 문서 최신성, 수집 상태, 인덱싱 상태를 관리

선행 권한:
- 허용 역할: `ops_admin`, `client_admin`, `qa_admin`
- 필수 액션: `document.read`
- 요청 액션: `document.reingest.request`, `document.reindex.request`
- 실행 액션: `document.reindex.execute`

연결 API:
- `GET /admin/documents`
- `POST /admin/documents/{id}/reingest`
- `POST /admin/documents/{id}/reindex`

주요 읽기 테이블:
- `documents`
- `document_chunks`

주요 쓰기 테이블:
- `documents`
- `document_chunks`

핵심 검증 포인트:
- `ingestion_status`, `index_status` 상태 전이가 규칙대로 반영되는지
- 재수집 후 최신 버전 메타데이터가 갱신되는지
- 재인덱싱 후 chunk 추적 키가 유지되는지

## 4. RAG Operations

화면 목적:
- retrieval 실패, latency, zero-result 패턴을 분석

선행 권한:
- 허용 역할: `ops_admin`
- 제한 허용 역할: `qa_admin` 읽기 전용 가능
- 필수 액션: `dashboard.read`, `metrics.read`

연결 API:
- `GET /admin/rag/search-logs`
- `GET /admin/questions/{id}`

주요 읽기 테이블:
- `rag_search_logs`
- `rag_retrieved_documents`
- `questions`
- `answers`

주요 쓰기 테이블:
- 직접 쓰기 없음

핵심 검증 포인트:
- 검색 실패 사례에서 `retrieval_status`와 문서 목록이 일치하는지
- 질문 상세에서 검색 로그와 답변 결과가 같은 trace로 묶이는지

## 4A. Crawl Source Management

화면 목적:
- 기관별 crawl source와 수집 정책을 등록하고 변경 이력을 관리

선행 권한:
- 허용 역할: `ops_admin`
- 제한 허용 역할: `client_admin` 읽기 일부
- 필수 액션: `crawl_source.read`
- 등록/수정 액션: `crawl_source.write`
- 즉시 실행 액션: `document.reingest.request`

연결 API:
- `GET /admin/crawl-sources`
- `POST /admin/crawl-sources`
- `PATCH /admin/crawl-sources/{id}`
- `POST /admin/crawl-sources/{id}/crawl`

주요 읽기 테이블:
- `crawl_sources`
- `organizations`
- `services`
- `ingestion_jobs`

주요 쓰기 테이블:
- `crawl_sources`
- `ingestion_jobs`

핵심 검증 포인트:
- 고객사 사용자는 자기 기관 source만 조회 가능한지
- `allow_patterns`, `deny_patterns`, `schedule_expr` 저장값이 조회 응답과 일치하는지
- 즉시 crawl 실행 시 source와 job 이력이 연결되는지

## 4B. Ingestion Job Monitor

화면 목적:
- crawl, parse, embed, index 단계별 작업 이력과 실패 원인을 추적

선행 권한:
- 허용 역할: `ops_admin`
- 제한 허용 역할: `qa_admin` 읽기 전용 가능
- 필수 액션: `ingestion.read`
- 재실행 요청 액션: `document.reingest.request`
- 재인덱싱 실행 액션: `document.reindex.execute`

연결 API:
- `GET /admin/ingestion-jobs`
- `GET /admin/ingestion-jobs/{id}`
- `POST /admin/ingestion-jobs/{id}/retry`
- `GET /admin/documents`

주요 읽기 테이블:
- `ingestion_jobs`
- `crawl_sources`
- `documents`
- `document_versions`

주요 쓰기 테이블:
- `ingestion_jobs`
- `documents`
- `document_versions`

핵심 검증 포인트:
- `job_status`, `job_stage`, `failure_code`가 최신 실행 결과와 일치하는지
- 실패 재실행 후 새 job이 이전 job과 parent 관계로 추적되는지
- 문서 버전 갱신과 ingestion 완료 시각이 같은 trace에서 복원되는지

## 5. Client Dashboard

화면 목적:
- 고객사가 기관 KPI와 개선 우선순위를 확인

선행 권한:
- 허용 역할: `ops_admin`, `client_admin`
- 제한 허용 역할: `qa_admin` 읽기 전용 가능
- 필수 액션: `dashboard.read`, `metrics.read`

연결 API:
- `GET /admin/client/dashboard`
- `GET /admin/metrics/daily`
- `GET /admin/questions/unresolved`

주요 읽기 테이블:
- `daily_metrics_org`
- `questions`
- `answers`
- `qa_reviews`

주요 쓰기 테이블:
- 직접 쓰기 없음

핵심 검증 포인트:
- KPI 수치와 unresolved 개수가 같은 기간 조건에서 맞는지
- 기관 범위 제한이 항상 적용되는지

## 6. Question Analysis

화면 목적:
- 반복 질문, 증가 질문, 주제별 품질을 분석

선행 권한:
- 허용 역할: `ops_admin`, `client_admin`, `qa_admin`
- 필수 액션: `dashboard.read`, `metrics.read`
- 후속 메모/개선 지정 시 추가 액션: `annotate`

연결 API:
- `GET /admin/questions/unresolved`
- `GET /admin/metrics/daily`
- `GET /admin/questions/{id}`

주요 읽기 테이블:
- `questions`
- `answers`
- `qa_reviews`

주요 쓰기 테이블:
- 직접 쓰기 없음

핵심 검증 포인트:
- 질문 주제 집계와 상세 질문 목록이 같은 필터 기준을 쓰는지
- 개선 대상 지정 후 QA 후속 액션으로 이어질 수 있는지

## 7. Unresolved Questions

화면 목적:
- 미응답, 오답 의심, 개선 필요 질문을 큐로 관리

선행 권한:
- 허용 역할: `ops_admin`, `client_admin`, `qa_admin`
- 필수 액션: `qa.review.read`
- 후속 메모/개선 요청 시 추가 액션: `annotate`
- QA 저장 시 추가 액션: `qa.review.write`

연결 API:
- `GET /admin/questions/unresolved`
- `GET /admin/questions/{id}`
- `POST /admin/qa-reviews`

주요 읽기 테이블:
- `questions`
- `answers`
- `qa_reviews`

주요 쓰기 테이블:
- `qa_reviews`

핵심 검증 포인트:
- `answer_status`와 최신 `review_status` 조합으로 목록이 정확히 필터링되는지
- QA 저장 후 목록 상태가 즉시 갱신되는지

## 8. QA Review

화면 목적:
- 질문, 답변, 검색 근거, 기존 이력을 함께 보고 판정 저장

선행 권한:
- 허용 역할: `qa_admin`
- 제한 허용 역할: `ops_admin` 읽기 전용
- 필수 액션: `qa.review.read`
- 판정 저장 액션: `qa.review.write`
- 후속 재인덱싱 요청 액션: `document.reindex.request`
- 실제 재인덱싱 실행 액션: `document.reindex.execute`

연결 API:
- `GET /admin/questions/{id}`
- `POST /admin/qa-reviews`
- `POST /admin/documents/{id}/reindex`

주요 읽기 테이블:
- `questions`
- `answers`
- `rag_search_logs`
- `rag_retrieved_documents`
- `qa_reviews`
- `documents`

주요 쓰기 테이블:
- `qa_reviews`
- `documents`

핵심 검증 포인트:
- QA 판정 저장 시 `root_cause_code`, `action_type` 조합이 허용 규칙을 따르는지
- `document_fix_request`, `reindex_request` 같은 후속 액션이 추적 가능한지

## 9. Cross-Cutting Trace Rules

필수 추적 키:
- `organization_id`
- `service_id`
- `question_id`
- `request_id`
- `trace_id`
- `crawl_source_id`
- `ingestion_job_id`

추적 원칙:
- `POST /chat/questions`에서 생성된 `question_id`가 답변, retrieval, QA의 기준 키가 된다.
- 관리자 상세 화면은 항상 `question_id` 기준으로 answer, retrieval, review를 재구성한다.
- 집계 화면은 원천 로그가 아니라 `daily_metrics_org`를 우선 조회한다.
- 재인덱싱, 재수집 액션은 문서 단위 작업 이력과 연결 가능해야 한다.
- crawl source 화면은 `crawl_source_id` 기준으로 최근 job, 최신 document version, 마지막 성공 시각을 재구성할 수 있어야 한다.

권한 추적 원칙:
- 관리자 API는 모두 `actor_user_id`, `actor_role_code`, `organization_scope` 를 복원 가능해야 한다.
- `403` 은 액션 불가, `404` 는 범위 은닉이 필요한 경우로 구분한다.
- 고위험 액션은 `audit_logs.action_code` 와 화면 액션이 1:1 대응돼야 한다.

## 10. Sprint 1 Implementation Cut

우선 연결이 필요한 화면:
- Operations Dashboard
- Client Dashboard
- Unresolved Questions
- QA Review
- Document Source Management

Sprint 1 필수 API:
- `POST /chat/questions`
- `GET /admin/ops/dashboard`
- `GET /admin/client/dashboard`
- `GET /admin/questions/unresolved`
- `GET /admin/questions/{id}`
- `POST /admin/qa-reviews`
- `GET /admin/documents`
- `POST /admin/documents/{id}/reindex`
- `POST /admin/auth/login`
- `POST /admin/auth/logout`
- `GET /admin/auth/me`

Sprint 1 필수 테이블:
- `organizations`
- `services`
- `chat_sessions`
- `questions`
- `answers`
- `documents`
- `rag_search_logs`
- `rag_retrieved_documents`
- `qa_reviews`
- `daily_metrics_org`
- `admin_users`
- `admin_user_roles`
- `admin_sessions`
- `audit_logs`

Sprint 1 필수 권한 액션:
- `dashboard.read`
- `metrics.read`
- `document.read`
- `document.reindex.request`
- `document.reindex.execute`
- `qa.review.read`
- `qa.review.write`

## 11. Screen To Auth Action Summary

- Operations Dashboard -> `dashboard.read`, `metrics.read`
- Organization Management -> `organization.read`, `organization.update`
- Document Source Management -> `document.read`, `document.reingest.request`, `document.reindex.request`, `document.reindex.execute`
- RAG Operations -> `dashboard.read`, `metrics.read`
- Client Dashboard -> `dashboard.read`, `metrics.read`
- Question Analysis -> `dashboard.read`, `metrics.read`, `annotate`
- Unresolved Questions -> `qa.review.read`, `annotate`, `qa.review.write`
- QA Review -> `qa.review.read`, `qa.review.write`, `document.reindex.request`, `document.reindex.execute`

## 12. OpenRAG Boundary In Matrix

- OpenRAG는 `POST /chat/questions` 내부 retrieval 단계의 후보 구현체다.
- OpenRAG가 검색과 chunk 반환을 수행하더라도 제품은 `rag_search_logs`, `rag_retrieved_documents`로 정규화해 저장한다.
- QA와 대시보드 관점에서는 OpenRAG 고유 구조를 직접 참조하지 않는다.
