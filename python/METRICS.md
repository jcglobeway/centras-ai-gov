물ㅇ# RAG 플랫폼 메트릭 수집 방식 총람

`python/` 내 모든 서비스가 생성·수집하는 지표를 수집 방식별로 정리한다.

---

## 수집 방식 정의

| 방식 | 설명 | 트리거 |
|------|------|--------|
| **On-demand** | 질의 발생 시 자동 생성. 별도 실행 불필요 | 시민 질의 |
| **Batch** | 수동 CLI로 실행. 과거 데이터를 소급 처리 | CLI 명령 |
| **Scheduled** | 내부 스케줄러가 주기적으로 집계. cron 외부화 가능 | Spring `@Scheduled` |
| **On-request** | API 호출 시 즉시 집계. 저장된 원본 데이터를 집계만 수행 | 프론트엔드 API 호출 |

---

## On-demand — 질의마다 자동 생성

### rag-orchestrator (질의 1건당 동기 실행)

| 지표 | 설명 | 단위 | 저장 테이블 |
|------|------|------|-------------|
| `response_time_ms` | 전체 응답 소요 시간 | ms | `answers` |
| `latency_ms` | Retrieval 단계 소요 시간 (임베딩 + pgvector 검색 + BM25 + RRF) | ms | `rag_search_logs` |
| `llm_ms` | LLM 생성 단계 소요 시간 | ms | `rag_search_logs` |
| `postprocess_ms` | 후처리 소요 시간 (confidence 계산 등) | ms | `rag_search_logs` |
| `input_tokens` | LLM 입력 토큰 수 (`prompt_eval_count`) | 개 | `answers` |
| `output_tokens` | LLM 출력 토큰 수 (`eval_count`) | 개 | `answers` |
| `confidence_score` | `1 − avg(cosine distance)` — 검색 결과 평균 신뢰도 | 0–1 | `answers` |
| `citation_count` | 최종 반환된 청크 수 (top-N after rerank) | 개 | `answers` |
| `retrieval_status` | `success` / `zero_result` | enum | `rag_search_logs` |
| `answer_status` | `answered` / `fallback` / `no_answer` / `error` | enum | `answers` |
| `is_escalated` | confidence < 0.4 시 에스컬레이션 플래그 | bool | `questions` |
| `question_failure_reason_code` | A04(검색 실패) / A05(신뢰도 미달) 등 | enum | `questions` |

**청크별 검색 점수** (`rag_retrieved_documents`):

| 점수 | 설명 |
|------|------|
| `distance` | RRF 정규화 점수 → `1 − (rrf_score / max_rrf)` |
| `rank` | 최종 반환 순위 |

> **검색 파이프라인 내부 중간값** (저장 안 됨, 최종 `distance`로 합산):
> - `vector_search`: pgvector cosine `<=>` 거리
> - `bm25_search`: BM25Okapi 점수
> - `rrf_fusion`: `Σ 1/(60 + rank)`

---

### realtime-eval (질의 완료 후 비동기 실행)

Redis `ragas:eval:queue` BRPOP → `_compute_metrics()` 호출

| 지표 | 설명 | ground_truth 필요 | 저장 테이블 |
|------|------|:-----------------:|-------------|
| `faithfulness` | 답변의 주장이 검색 청크에 근거하는 비율 | X | `ragas_evaluations` |
| `answer_relevancy` | 답변이 질문에 얼마나 관련 있는지 (임베딩 유사도) | X | `ragas_evaluations` |
| `citation_coverage` | 검색 청크 중 LLM이 실제 활용한 비율 | X | `ragas_evaluations` |
| `citation_correctness` | 활용된 청크가 답변 내용을 올바르게 지지하는 비율 | X | `ragas_evaluations` |
| `context_precision` | 실시간 모드에서는 항상 `null` | O → 없음 | `ragas_evaluations` |
| `context_recall` | 실시간 모드에서는 항상 `null` | O → 없음 | `ragas_evaluations` |

> **실패 시**: `ragas:eval:dlq`에 push (Dead Letter Queue)

---

## Batch — 수동/스케줄 실행

### eval-runner (ragas_batch)

```bash
eval-runner --date YYYY-MM-DD [--organization-id ORG_ID]
eval-runner --from-eval-results   # eval_results.json 기준
```

**저장 전략 (eval-batch-restore):**

```
질문별 루프:
  1. GET /admin/ragas-evaluations/by-question/{questionId}
  2. 행 없음 → 6지표 전체 계산 → POST (신규 생성)
  3. 행 있음 → null 필드만 선택 계산 → PATCH (COALESCE — 기존 값 보존)
```

> realtime-eval이 먼저 저장한 값은 절대 덮어쓰지 않는다. 중복 행 발생 없음.

| 지표 | ground_truth 필요 | 계산 방식 | 저장 테이블 |
|------|:-----------------:|-----------|-------------|
| `faithfulness` | X | RAGAS 라이브러리 (LLM judge) | `ragas_evaluations` |
| `answer_relevancy` | X | RAGAS 라이브러리 (임베딩 유사도) | `ragas_evaluations` |
| `context_precision` | **O** | RAGAS 라이브러리 (LLM judge) | `ragas_evaluations` |
| `context_recall` | **O** | RAGAS 라이브러리 (LLM judge) | `ragas_evaluations` |
| `citation_coverage` | X | 커스텀 LLM judge (청크별 활용 여부) | `ragas_evaluations` |
| `citation_correctness` | X | 커스텀 LLM judge (청크별 지지 여부) | `ragas_evaluations` |

> ground_truth는 `eval_results.json` (prepare-eval-data + query-runner 출력)에서 공급.
> 없으면 context_precision / context_recall은 `null`로 저장.

---

### cluster-questions

```bash
cluster-questions --org-id org_acc --days 7
```

3단계로 실행되며 각각 별도 테이블에 저장.

**Phase 1: 키워드 빈도 통계**

| 지표 | 설명 | 저장 테이블 |
|------|------|-------------|
| `keyword` | LLM이 추출한 핵심 명사 | `question_keyword_stats` |
| `count` | 해당 기간 내 출현 빈도 | `question_keyword_stats` |

**Phase 2: 유사 질문 클러스터링**

| 지표 | 설명 | 저장 테이블 |
|------|------|-------------|
| `avg_similarity` | 클러스터 내 질문 간 평균 코사인 유사도 | `question_similarity_groups` |
| `question_count` | 클러스터 크기 | `question_similarity_groups` |
| `representative_text` | 클러스터 대표 질문 (가장 짧은 질문) | `question_similarity_groups` |

> 파이프라인: bge-m3 임베딩 → 코사인 유사도 행렬 → 임계값(0.75) 필터 → LLM 의도 동일 여부 검증 → Union-Find 클러스터 구성

**Phase 3: 질문 유형 분류**

| 지표 | 설명 | 저장 테이블 |
|------|------|-------------|
| `type_label` | LLM이 도출한 기관 특화 유형 (예: "예약/접수", "운영시간") | `question_type_stats` |
| `count` | 해당 유형으로 분류된 질문 수 | `question_type_stats` |

---

### ingestion-worker

```bash
ingestion-worker run --job-id <id>
```

품질 지표보다는 **파이프라인 상태 추적** 위주.

| 항목 | 설명 | 저장 위치 |
|------|------|-----------|
| `job_stage` | `FETCH → EXTRACT → CHUNK → EMBED → INDEX → COMPLETE` 전이 | `ingestion_jobs` |
| `job_status` | `running / succeeded / failed / cancelled` | `ingestion_jobs` |
| `error_code` | `CRAWL_ERROR / EMPTY_CONTENT / WORKER_ERROR` | `ingestion_jobs` |
| `token_count` | 청크당 단어 수 (공백 분리 추정) | `document_chunks` |
| embedded 성공률 | `embedded_count / total_chunks` — 로그 출력만, DB 저장 안 됨 | — |

---

## On-request — API 호출 시 즉시 집계

Admin API (Kotlin)가 담당한다.

### RAGAS 평가 요약

Python이 `ragas_evaluations` 테이블에 저장한 원본 데이터를 `AVG()`로 즉시 집계해 반환한다.

```
GET /admin/ragas-evaluations/summary
  ?organization_id=org_acc&from_date=2026-03-28&to_date=2026-04-03
```

| 집계 지표 | 설명 |
|-----------|------|
| `avg_faithfulness` | 기간 내 평균 |
| `avg_answer_relevancy` | 기간 내 평균 |
| `avg_context_precision` | 기간 내 평균 (null 제외) |
| `avg_context_recall` | 기간 내 평균 (null 제외) |
| `avg_citation_coverage` | 기간 내 평균 |
| `avg_citation_correctness` | 기간 내 평균 |
| `count` | 집계 대상 건수 |

응답에는 **current** (요청 기간)와 **previous** (동일 길이 직전 기간) 두 세트가 포함되어 프론트엔드에서 Δ 비교에 사용한다.

### 운영 KPI 집계 트리거

`MetricsAggregationScheduler`가 `questions` + `answers` 테이블을 집계해 `daily_metrics_org`에 저장한다.

**스케줄**: 기본값 매일 00:05 (`0 5 0 * * *`). `application.yml`의 `metrics.aggregation.cron` 으로 외부화되어 있으며 환경별 덮어쓰기 가능.

> 현재 운영 설정: `"0 */30 * * * *"` (30분마다) — 데이터 최신성 확보용

**수동 트리거 API** (운영/검증용):

```
POST /admin/metrics/trigger-aggregation
  ?date=2026-04-01          # 생략 시 어제 날짜
```

- 권한: `ops_admin` / `super_admin` 전용
- 효과: 지정 날짜의 `daily_metrics_org` 레코드를 즉시 재집계 (upsert)
- 응답: `{ "triggered": true, "date": "2026-04-01" }`

---

## 전체 지표 맵

```
시민 질의
  │
  ├─ [On-demand] rag-orchestrator
  │     → rag_search_logs    : latency_ms / llm_ms / postprocess_ms / retrieval_status
  │     → rag_retrieved_docs : rank / distance (RRF 정규화)
  │     → answers            : response_time_ms / confidence_score / tokens / answer_status
  │     → questions          : is_escalated / failure_reason_code
  │
  └─ [On-demand] realtime-eval (Redis 비동기)
        → ragas_evaluations  : faithfulness / answer_relevancy / citation_coverage / citation_correctness

[Batch] eval-runner
  → ragas_evaluations  : 위 4개 + context_precision / context_recall (ground_truth 있을 때)

[Batch] cluster-questions
  → question_keyword_stats    : 키워드 빈도
  → question_similarity_groups: 유사 질문 클러스터 / avg_similarity
  → question_type_stats       : 질문 유형 분류 카운트

[On-request] GET /admin/ragas-evaluations/summary
  → ragas_evaluations 테이블 AVG() 즉시 집계
  → current / previous 두 기간 비교 반환

[Scheduled] MetricsAggregationScheduler
  → questions + answers → daily_metrics_org (upsert)
  → 기본 스케줄: 매일 00:05 (metrics.aggregation.cron 외부화, 현재 운영: 30분마다)

[On-request] POST /admin/metrics/trigger-aggregation
  → MetricsAggregationScheduler 수동 트리거 (ops_admin/super_admin 전용)
  → 지정 날짜의 daily_metrics_org 즉시 재집계
```

---

## 목표 기준값 (품질 지표)

| 지표 | 목표 | 수집 방식 |
|------|------|-----------|
| Faithfulness | ≥ 0.85 | On-demand + Batch |
| Answer Relevancy | ≥ 0.80 | On-demand + Batch |
| Context Precision | ≥ 0.75 | Batch only |
| Context Recall | ≥ 0.70 | Batch only |
| Citation Coverage | ≥ 0.80 | On-demand + Batch |
| Citation Correctness | ≥ 0.85 | On-demand + Batch |
| Confidence Score | ≥ 0.40 | On-demand |
