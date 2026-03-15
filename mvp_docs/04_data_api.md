# MVP Data And API

## 1. Data Design Principle

- 운영 로그를 먼저 남기고 화면은 그 로그를 읽는 구조로 간다.
- 챗봇 실행 계층과 무관하게 제품 DB 기준 스키마를 유지한다.
- 기관 단위 분리와 감사 추적이 가능한 키를 모든 핵심 테이블에 포함한다.
- OpenRAG를 붙이더라도 저장 스키마와 API 계약은 바꾸지 않는다.

## 2. Must-Have Tables

### Tenant / Service

- `organizations`
- `services`

### Chat Runtime

- `chat_sessions`
- `chat_messages`
- `questions`
- `answers`

### Knowledge / Ingestion

- `crawl_sources`
- `documents`
- `document_versions`
- `document_chunks`
- `ingestion_jobs`

### Retrieval / Quality

- `rag_search_logs`
- `rag_retrieved_documents`
- `qa_reviews`

### Metrics

- `daily_metrics_org`

## 3. Table Responsibility

### `organizations`

- 기관 기본 정보와 운영 상태를 저장한다.
- 고객사 범위 필터링의 최상위 키로 사용한다.

핵심 필드:
- `id`
- `name`
- `org_code`
- `status`
- `owner_user_id`
- `last_document_sync_at`
- `created_at`

### `services`

- 기관 내 챗봇 또는 서비스 단위를 구분한다.
- 동일 기관 내 민원, 복지, 세금 등 서비스 분리를 허용한다.

핵심 필드:
- `id`
- `organization_id`
- `name`
- `channel_type`
- `status`
- `go_live_at`

### `chat_sessions`

- 시민 대화 세션 단위를 관리한다.
- 세션 길이, 유입 채널, 종료 시각을 집계에 활용한다.

핵심 필드:
- `id`
- `organization_id`
- `service_id`
- `channel`
- `user_key_hash`
- `started_at`
- `ended_at`

### `questions`

- 사용자 질문의 원문과 운영 분류 기준을 저장한다.
- unresolved, QA, KPI의 기준 레코드로 사용한다.

핵심 필드:
- `id`
- `organization_id`
- `service_id`
- `chat_session_id`
- `question_text`
- `question_intent_label`
- `channel`
- `created_at`

### `answers`

- 질문에 대한 최종 응답 결과를 저장한다.
- 품질 집계와 SLA 집계의 기준이 된다.

핵심 필드:
- `id`
- `question_id`
- `answer_text`
- `answer_status`
- `response_time_ms`
- `citation_count`
- `fallback_reason_code`
- `created_at`

### `documents`

- 원문 문서 메타데이터와 ingestion/index 상태를 관리한다.
- 공개 범위, 버전, 최신성 판단의 기준 테이블이다.

핵심 필드:
- `id`
- `organization_id`
- `document_type`
- `title`
- `source_uri`
- `version_label`
- `published_at`
- `ingestion_status`
- `index_status`
- `visibility_scope`
- `last_ingested_at`
- `last_indexed_at`

### `crawl_sources`

- 기관별 수집 원천과 수집 정책을 저장한다.
- crawler worker가 읽는 실행 입력이자 운영 화면의 제어 기준이다.

핵심 필드:
- `id`
- `organization_id`
- `service_id`
- `source_type`
- `source_uri`
- `collection_mode`
- `render_mode`
- `schedule_expr`
- `is_active`
- `last_crawled_at`
- `last_success_at`

### `document_versions`

- 같은 문서의 버전 이력과 변경 감지 결과를 저장한다.
- QA와 최신성 추적에서 "현재 노출 답변이 어떤 버전을 근거로 했는지" 복원하는 기준이다.

핵심 필드:
- `id`
- `document_id`
- `version_label`
- `content_hash`
- `source_etag`
- `source_last_modified_at`
- `change_detected`
- `snapshot_uri`
- `parsed_text_uri`
- `created_at`

### `document_chunks`

- 검색 대상 chunk와 embedding 기준 메타를 저장한다.
- 직접 벡터를 저장하지 않더라도 chunk 추적 키는 유지한다.

핵심 필드:
- `id`
- `document_id`
- `chunk_key`
- `chunk_text`
- `token_count`
- `chunk_order`
- `embedding_ref`

### `ingestion_jobs`

- crawl, parse, chunk, embed, index 단위 실행 이력을 저장한다.
- 운영 화면과 재시도 정책은 이 테이블 기준으로 판단한다.

핵심 필드:
- `id`
- `organization_id`
- `service_id`
- `crawl_source_id`
- `document_id`
- `document_version_id`
- `job_type`
- `job_status`
- `trigger_type`
- `runner_type`
- `attempt_count`
- `error_code`
- `started_at`
- `finished_at`

### `rag_search_logs`

- 검색 실행 단위의 입력, 결과, 엔진 정보를 저장한다.
- zero-result, latency, engine 비교의 기준이 된다.

핵심 필드:
- `id`
- `question_id`
- `query_text`
- `query_rewrite_text`
- `zero_result`
- `top_k`
- `latency_ms`
- `retrieval_engine`
- `retrieval_status`
- `created_at`

### `rag_retrieved_documents`

- 각 검색 실행에서 실제로 노출된 문서/chunk를 기록한다.
- QA 화면에서 근거 추적에 사용한다.

핵심 필드:
- `id`
- `rag_search_log_id`
- `document_id`
- `chunk_id`
- `rank`
- `score`
- `used_in_citation`

### `qa_reviews`

- QA 판정과 후속 액션을 저장한다.
- 같은 질문에 여러 차례 검수가 가능하도록 append-only 성격으로 설계한다.

핵심 필드:
- `id`
- `question_id`
- `review_status`
- `root_cause_code`
- `action_type`
- `action_target_id`
- `review_comment`
- `reviewer_id`
- `reviewed_at`

### `daily_metrics_org`

- 기관/서비스/일자 단위 KPI 스냅샷을 저장한다.
- 대시보드 조회 비용을 줄이기 위한 집계 테이블이다.

핵심 필드:
- `id`
- `metric_date`
- `organization_id`
- `service_id`
- `total_sessions`
- `total_questions`
- `resolved_rate`
- `fallback_rate`
- `zero_result_rate`
- `avg_response_time_ms`

## 4. Required Status And Codes

### `organizations.status`

- `active`
- `inactive`
- `onboarding`
- `suspended`

### `services.status`

- `draft`
- `active`
- `maintenance`
- `ended`

### `answers.answer_status`

- `answered`
- `fallback`
- `no_answer`
- `error`

### `documents.ingestion_status`

- `pending`
- `running`
- `success`
- `failed`
- `stale`

### `documents.index_status`

- `pending`
- `indexed`
- `stale`
- `failed`

### `crawl_sources.source_type`

- `website`
- `sitemap`
- `rss`
- `file_feed`
- `manual_upload`

### `crawl_sources.collection_mode`

- `full`
- `incremental`

### `crawl_sources.render_mode`

- `http_only`
- `browser_playwright`
- `browser_lightpanda`

### `document_versions.change_detected`

- `created`
- `updated`
- `unchanged`
- `deleted`

### `ingestion_jobs.job_type`

- `crawl`
- `parse`
- `chunk`
- `embed`
- `index`
- `reingest`
- `reindex`

### `ingestion_jobs.job_status`

- `pending`
- `queued`
- `running`
- `success`
- `failed`
- `cancelled`

### `ingestion_jobs.trigger_type`

- `schedule`
- `manual`
- `qa_request`
- `document_event`

### `ingestion_jobs.runner_type`

- `python_worker`
- `openrag_flow`
- `spring_batch`

### `rag_search_logs.retrieval_status`

- `success`
- `zero_result`
- `timeout`
- `error`

### `qa_reviews.review_status`

- `pending`
- `confirmed_issue`
- `false_alarm`
- `resolved`

### `qa_reviews.root_cause_code`

- `missing_document`
- `stale_document`
- `bad_chunking`
- `retrieval_failure`
- `generation_error`
- `policy_block`
- `unclear_question`

### `qa_reviews.action_type`

- `faq_create`
- `document_fix_request`
- `reindex_request`
- `ops_issue`
- `no_action`

## 5. Minimum Constraints

- 모든 업무 테이블은 `organization_id` 또는 상위 조인으로 기관 범위를 판별할 수 있어야 한다.
- `questions.chat_session_id`, `answers.question_id`, `rag_search_logs.question_id`, `qa_reviews.question_id`는 필수 FK로 둔다.
- `crawl_sources.organization_id`, `documents.organization_id`, `ingestion_jobs.organization_id`는 동일 조직 범위 검증이 가능해야 한다.
- `documents.source_uri`는 기관 범위 내에서 중복 등록을 막기 위해 `organization_id + source_uri + version_label` 유니크를 권장한다.
- `crawl_sources`는 `organization_id + source_uri + source_type` 유니크를 권장한다.
- `document_versions`는 `document_id + content_hash` 유니크를 권장한다.
- `daily_metrics_org`는 `metric_date + organization_id + service_id` 유니크로 관리한다.
- 삭제는 하드 삭제보다 `status` 또는 `deleted_at` 기반 소프트 삭제를 우선한다.

## 6. Minimum Index Draft

- `questions`: `organization_id, created_at desc`
- `questions`: `service_id, created_at desc`
- `answers`: `question_id`
- `answers`: `answer_status, created_at desc`
- `crawl_sources`: `organization_id, is_active, source_type`
- `documents`: `organization_id, index_status, updated_at desc`
- `documents`: `organization_id, ingestion_status, updated_at desc`
- `document_versions`: `document_id, created_at desc`
- `ingestion_jobs`: `organization_id, job_status, created_at desc`
- `ingestion_jobs`: `crawl_source_id, created_at desc`
- `rag_search_logs`: `question_id`
- `rag_search_logs`: `retrieval_status, created_at desc`
- `qa_reviews`: `question_id, reviewed_at desc`
- `daily_metrics_org`: `organization_id, metric_date desc`

## 7. Event Logging Rule

반드시 남겨야 하는 이벤트:
- 질문 수신
- 답변 생성 완료
- 검색 실행
- 검수 저장
- 문서 재수집
- 재인덱싱 실행
- KPI 일배치 집계
- crawl source 등록/수정
- ingestion job 상태 전이

권장 공통 이벤트 필드:
- `event_id`
- `event_type`
- `organization_id`
- `service_id`
- `actor_type`
- `actor_id`
- `target_type`
- `target_id`
- `occurred_at`
- `trace_id`

추가 ingestion 이벤트 필드:
- `job_id`
- `job_type`
- `runner_type`
- `error_code`
- `retry_count`

## 8. API Contract Rule

- 관리자 API는 `/admin/*` 네임스페이스를 사용한다.
- 모든 목록 API는 `organization_id`, `service_id`, `from`, `to`, `page`, `page_size` 필터 패턴을 최대한 공통화한다.
- 모든 응답에는 최소 `request_id`와 서버 기준 `generated_at`을 포함한다.
- OpenRAG 연동 여부와 무관하게 retrieval 상세는 `rag_search_logs`, `rag_retrieved_documents` 구조로 매핑한다.
- ingestion 구현체가 무엇이든 운영 API는 `crawl_sources`, `document_versions`, `ingestion_jobs` 기준으로 상태를 노출한다.

## 9. API Priority 1

### `POST /chat/questions`

목적:
- 시민 질문을 받아 답변을 생성하고 질문, 답변, 검색 로그를 함께 남긴다.

Request example:

```json
{
  "organization_id": "org_seoul_01",
  "service_id": "svc_welfare",
  "session_id": "sess_20260315_0001",
  "channel": "web",
  "question_text": "청년 월세 지원 신청 자격이 어떻게 되나요?"
}
```

Response example:

```json
{
  "request_id": "req_01JPC7M8X",
  "generated_at": "2026-03-15T10:12:33Z",
  "question_id": "q_1001",
  "answer": {
    "answer_id": "ans_1001",
    "status": "answered",
    "text": "청년 월세 지원은 연령, 소득, 거주 요건을 충족해야 합니다.",
    "response_time_ms": 1820,
    "citations": [
      {
        "document_id": "doc_301",
        "title": "청년 월세 지원 공고",
        "source_uri": "https://example.go.kr/welfare/rent-2026"
      }
    ]
  },
  "retrieval": {
    "search_log_id": "rlog_9001",
    "status": "success",
    "zero_result": false
  }
}
```

오류 규칙:
- 문서 근거가 없으면 `answer.status = fallback` 또는 `no_answer`를 반환한다.
- 내부 장애 시 HTTP 5xx와 함께 `error_code`, `request_id`를 반환한다.

### `GET /admin/ops/dashboard`

목적:
- 운영사 대시보드용 통합 KPI와 이상 징후를 조회한다.

Query example:
- `organization_id`
- `from`
- `to`

Response example:

```json
{
  "request_id": "req_ops_001",
  "generated_at": "2026-03-15T10:20:00Z",
  "summary": {
    "organization_count": 12,
    "total_questions": 58231,
    "avg_response_time_ms": 1740,
    "fallback_rate": 0.083,
    "zero_result_rate": 0.041
  },
  "alerts": [
    {
      "organization_id": "org_seoul_01",
      "type": "index_stale",
      "severity": "high",
      "message": "최근 24시간 동안 재인덱싱 실패 3건"
    }
  ]
}
```

### `GET /admin/client/dashboard`

목적:
- 고객사 관리자용 기관 KPI와 개선 우선순위를 조회한다.

Query example:
- `organization_id` 필수
- `service_id` 선택
- `from`
- `to`

Response example:

```json
{
  "request_id": "req_client_001",
  "generated_at": "2026-03-15T10:21:00Z",
  "kpis": {
    "total_questions": 14220,
    "resolved_rate": 0.71,
    "fallback_rate": 0.11,
    "repeat_question_rate": 0.18,
    "satisfaction_score": 3.9
  },
  "top_issue_topics": [
    {
      "topic": "청년 월세 지원",
      "question_count": 224,
      "fallback_rate": 0.27
    }
  ]
}
```

### `GET /admin/questions/unresolved`

목적:
- 미응답, 오답 의심, QA 대기 질문을 목록으로 조회한다.

Query example:
- `organization_id`
- `service_id`
- `answer_status`
- `review_status`
- `root_cause_code`
- `from`
- `to`
- `page`
- `page_size`

Response example:

```json
{
  "request_id": "req_unresolved_001",
  "generated_at": "2026-03-15T10:22:00Z",
  "items": [
    {
      "question_id": "q_1001",
      "organization_id": "org_seoul_01",
      "service_id": "svc_welfare",
      "question_text": "청년 월세 지원 신청 자격이 어떻게 되나요?",
      "answer_status": "fallback",
      "review_status": "pending",
      "created_at": "2026-03-15T09:55:11Z"
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 134
}
```

### `GET /admin/questions/{id}`

목적:
- 질문 상세, 답변, 검색 로그, 검수 이력을 한 번에 조회한다.

Response example:

```json
{
  "request_id": "req_question_detail_001",
  "generated_at": "2026-03-15T10:23:00Z",
  "question": {
    "question_id": "q_1001",
    "question_text": "청년 월세 지원 신청 자격이 어떻게 되나요?",
    "channel": "web",
    "created_at": "2026-03-15T09:55:11Z"
  },
  "answer": {
    "answer_id": "ans_1001",
    "status": "fallback",
    "text": "정확한 근거 문서를 찾지 못했습니다."
  },
  "retrieval": {
    "search_log_id": "rlog_9001",
    "status": "zero_result",
    "documents": []
  },
  "reviews": []
}
```

### `POST /admin/qa-reviews`

목적:
- QA 판정과 후속 액션을 저장한다.

Request example:

```json
{
  "question_id": "q_1001",
  "review_status": "confirmed_issue",
  "root_cause_code": "missing_document",
  "action_type": "document_fix_request",
  "review_comment": "최신 공고문 미반영으로 판단"
}
```

Response example:

```json
{
  "request_id": "req_review_001",
  "generated_at": "2026-03-15T10:24:00Z",
  "review_id": "qarev_7001",
  "saved": true
}
```

### `GET /admin/documents`

목적:
- 기관별 문서 상태와 최신성 문제를 조회한다.

Query example:
- `organization_id`
- `document_type`
- `ingestion_status`
- `index_status`
- `visibility_scope`
- `page`
- `page_size`

Response example:

```json
{
  "request_id": "req_doc_001",
  "generated_at": "2026-03-15T10:25:00Z",
  "items": [
    {
      "document_id": "doc_301",
      "title": "청년 월세 지원 공고",
      "document_type": "policy_notice",
      "version_label": "2026.03",
      "ingestion_status": "success",
      "index_status": "stale",
      "published_at": "2026-03-02",
      "last_indexed_at": "2026-03-03T01:10:00Z"
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 248
}
```

### `POST /admin/documents/{id}/reindex`

목적:
- 특정 문서의 재인덱싱 작업을 시작한다.

Request example:

```json
{
  "reason": "qa_request",
  "requested_by": "user_qa_01"
}
```

Response example:

```json
{
  "request_id": "req_reindex_001",
  "generated_at": "2026-03-15T10:26:00Z",
  "job": {
    "job_type": "reindex",
    "document_id": "doc_301",
    "accepted": true,
    "status": "pending"
  }
}
```

### `GET /admin/crawl-sources`

목적:
- 기관별 수집 원천, 렌더링 방식, 최근 실행 상태를 조회한다.

Query example:
- `organization_id`
- `service_id`
- `source_type`
- `render_mode`
- `is_active`
- `page`
- `page_size`

Response example:

```json
{
  "request_id": "req_crawl_sources_001",
  "generated_at": "2026-03-15T10:28:00Z",
  "items": [
    {
      "crawl_source_id": "csrc_101",
      "organization_id": "org_seoul_01",
      "service_id": "svc_welfare",
      "source_type": "website",
      "source_uri": "https://example.go.kr/welfare",
      "render_mode": "browser_playwright",
      "collection_mode": "incremental",
      "is_active": true,
      "last_success_at": "2026-03-15T01:10:00Z"
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 14
}
```

### `POST /admin/crawl-sources`

목적:
- 신규 수집 원천을 등록하고 다음 스케줄 또는 수동 실행 대상으로 추가한다.

Request example:

```json
{
  "organization_id": "org_seoul_01",
  "service_id": "svc_welfare",
  "source_type": "website",
  "source_uri": "https://example.go.kr/welfare",
  "collection_mode": "incremental",
  "render_mode": "browser_playwright",
  "schedule_expr": "0 */6 * * *"
}
```

Response example:

```json
{
  "request_id": "req_crawl_source_create_001",
  "generated_at": "2026-03-15T10:29:00Z",
  "crawl_source_id": "csrc_101",
  "saved": true
}
```

### `GET /admin/ingestion-jobs`

목적:
- crawl, parse, embed, index 작업 실행 현황과 실패 원인을 조회한다.

Query example:
- `organization_id`
- `service_id`
- `job_type`
- `job_status`
- `runner_type`
- `from`
- `to`
- `page`
- `page_size`

Response example:

```json
{
  "request_id": "req_ingestion_jobs_001",
  "generated_at": "2026-03-15T10:30:00Z",
  "items": [
    {
      "job_id": "ijob_5001",
      "job_type": "crawl",
      "job_status": "failed",
      "runner_type": "python_worker",
      "crawl_source_id": "csrc_101",
      "document_id": "doc_301",
      "attempt_count": 2,
      "error_code": "CRAWL_TIMEOUT",
      "started_at": "2026-03-15T01:00:00Z",
      "finished_at": "2026-03-15T01:03:20Z"
    }
  ],
  "page": 1,
  "page_size": 20,
  "total": 31
}
```

## 10. API Priority 2

### `GET /admin/organizations`

- 기관 상태, 담당자, 최근 동기화 시각을 조회한다.

### `GET /admin/rag/search-logs`

- retrieval 실패 사례와 zero-result 패턴을 조회한다.

권장 필터:
- `organization_id`
- `service_id`
- `retrieval_status`
- `from`
- `to`

### `POST /admin/documents/{id}/reingest`

- 원본 문서를 다시 수집하고 파싱 작업을 시작한다.

### `POST /admin/crawl-sources/{id}/run`

- 특정 수집 원천에 대해 수동 crawl job을 생성한다.

### `GET /admin/documents/{id}/versions`

- 문서 버전 이력과 변경 감지 결과를 조회한다.

### `GET /admin/ingestion-jobs/{id}`

- 개별 ingestion job의 단계별 실행 결과와 에러 상세를 조회한다.

### `GET /admin/metrics/daily`

- 일자별 KPI 시계열을 조회한다.

## 11. Error Response Shape

예시:

```json
{
  "request_id": "req_error_001",
  "generated_at": "2026-03-15T10:27:00Z",
  "error": {
    "code": "FORBIDDEN_ORG_SCOPE",
    "message": "해당 기관 범위에 접근할 수 없습니다."
  }
}
```

권장 에러 코드:
- `FORBIDDEN_ORG_SCOPE`
- `INVALID_FILTER`
- `RESOURCE_NOT_FOUND`
- `REINDEX_ALREADY_RUNNING`
- `CRAWL_SOURCE_ALREADY_EXISTS`
- `INGESTION_JOB_ALREADY_RUNNING`
- `UNSUPPORTED_RENDER_MODE`
- `RAG_BACKEND_TIMEOUT`
- `INTERNAL_ERROR`

## 12. Boundary Rule

RAG 엔진이 자체 구현이든 OpenRAG 연동이든, API 응답은 항상 제품 스키마 기준으로 정규화한다.
Ingestion 런타임이 Playwright, Lightpanda, OpenRAG flow 중 무엇이든 `crawl_sources`, `document_versions`, `ingestion_jobs`, `documents` 계약은 유지한다.
