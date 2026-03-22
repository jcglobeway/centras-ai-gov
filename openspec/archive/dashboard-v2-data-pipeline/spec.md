# Spec: dashboard-v2-data-pipeline

## 1. 프론트엔드 컴포넌트 명세

### KpiCard (확장)

```typescript
// frontend/src/components/charts/KpiCard.tsx
interface KpiCardProps {
  label      : string
  value      : string | number
  sub?       : string
  trend?     : "up" | "down" | "neutral"
  trendValue?: string
  help?      : string
  status?    : "ok" | "warn" | "critical"  // 신규 — 상단 스트라이프 색상
  className? : string
}
```

스트라이프 색상 매핑:
| status | Tailwind class |
|---|---|
| `"ok"` | `bg-success` (`#00c47a`) |
| `"warn"` | `bg-warning` (`#f5a623`) |
| `"critical"` | `bg-error` (`#ff4560`) |
| undefined | 스트라이프 없음 |

---

### ProgressBar (신규)

```typescript
// frontend/src/components/ui/ProgressBar.tsx
interface ProgressBarProps {
  label  : string   // 단계명 ("Retrieval", "LLM 호출", "후처리")
  valueMs: number   // 표시할 ms 값
  maxMs  : number   // 100% 기준 ms (보통 파이프라인 전체 P95)
  color  : string   // tailwind bg-* class ("bg-blue-500" 등)
}
```

렌더링 구조:
```
<div class="flex items-center gap-3">
  <span class="w-24 text-xs text-text-secondary">{label}</span>
  <div class="flex-1 h-5 bg-bg-border rounded overflow-hidden">
    <div style={{ width: `${Math.min(valueMs / maxMs * 100, 100)}%` }}
         class={`h-full ${color} flex items-center px-2 text-xs font-mono font-semibold`}>
      {valueMs}ms
    </div>
  </div>
  <span class="w-16 text-right text-xs font-mono text-text-secondary">{valueMs}ms</span>
</div>
```

---

### AlertBanner (신규)

```typescript
// frontend/src/components/ui/AlertBanner.tsx
interface AlertBannerProps {
  variant  : "warn" | "critical"
  message  : string
  time?    : string      // 발생 시각 (선택)
  onDismiss: () => void
}
```

색상 규칙:
| variant | 배경 | 테두리 | 점 색상 |
|---|---|---|---|
| `"warn"` | `bg-warning/10` | `border-warning/30` | `bg-warning` |
| `"critical"` | `bg-error/10` | `border-error/30` | `bg-error` |

---

### ScoreTable (신규)

```typescript
// frontend/src/components/ui/ScoreTable.tsx
interface ScoreRow {
  label : string          // "Faithfulness"
  value : number | null   // 0.92 (null이면 "-" 표시)
  target: number          // 0.90
}

interface ScoreTableProps {
  rows: ScoreRow[]
}
```

Badge 판단 로직:
```typescript
function getStatus(value: number | null, target: number): BadgeVariant {
  if (value === null) return "neutral"
  if (value >= target) return "success"
  if (value >= target * 0.9) return "warning"
  return "error"
}
```

컬럼: 지표명 | 점수(font-mono) | 목표 | 상태(Badge)

---

### types.ts 추가

```typescript
// frontend/src/lib/types.ts
export interface RagasEvaluation {
  id               : string
  questionId       : string
  faithfulness     : number | null
  answerRelevance  : number | null
  contextPrecision : number | null
  contextRecall    : number | null
  rawScores        : string | null
  evaluatedAt      : string
  createdAt        : string
}
```

---

## 2. 백엔드 API 명세

### GET /admin/ragas-evaluations

**Request**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `question_id` | string | 선택 | 특정 질문의 평가만 조회 |
| `page` | int | 선택 | 기본 1 |
| `page_size` | int | 선택 | 기본 10, 최대 100 |

**Response 200**

```json
{
  "items": [
    {
      "id": "ragas_eval_001",
      "questionId": "q_004",
      "faithfulness": 0.92,
      "answerRelevance": 0.87,
      "contextPrecision": 0.74,
      "contextRecall": null,
      "rawScores": "{\"faithfulness\": 0.92}",
      "evaluatedAt": "2026-03-18T10:00:00Z",
      "createdAt": "2026-03-18T10:00:00Z"
    }
  ],
  "total": 5,
  "page": 1,
  "pageSize": 10,
  "requestId": "req_abc123",
  "generatedAt": "2026-03-21T00:00:00Z"
}
```

**Response 401** — 미인증
**Response 403** — 권한 없음

**접근 권한**: `ops_admin`, `super_admin`, `qa_admin`, `qa_manager`

---

### Kotlin 계층 명세

**`ListRagasEvaluationsQuery`** (in-port command):
```kotlin
data class ListRagasEvaluationsQuery(
    val questionId: String? = null,
    val page: Int = 1,
    val pageSize: Int = 10,
)
```

**`ListRagasEvaluationsUseCase`** (port/in):
```kotlin
interface ListRagasEvaluationsUseCase {
    fun listEvaluations(query: ListRagasEvaluationsQuery): PagedResult<RagasEvaluationSummary>
}
```

**`LoadRagasEvaluationsPort`** (port/out):
```kotlin
interface LoadRagasEvaluationsPort {
    fun loadAll(questionId: String?, page: Int, pageSize: Int): List<RagasEvaluationSummary>
    fun countAll(questionId: String?): Long
}
```

**`PagedResult`** — 기존 `PagedResponse` 서비스 레이어 표현용 data class (필요 시 신규):
```kotlin
data class PagedResult<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
)
```

---

## 3. 데이터 파이프라인 명세

### 3.1 신규 Python 스크립트

#### `python/eval-runner/src/eval_runner/ingestion_prep.py`

```
CLI: python -m eval_runner.ingestion_prep
  --zip-dir         data/3.개방데이터          필수
  --output-sql      V027__*.sql               필수
  --output-questions eval_questions.json      필수
  --test-set-size   100                       선택 (기본 100)
  --stratify-by     category                  선택 (기본 category)
```

출력 1 — `V027__seed_public_documents_and_llm_metrics.sql`:
- `documents` INSERT (TL+VL × 3기관 전량: 지방행정기관 + 중앙행정기관 + 국립아시아문화전당)
- `document_versions` INSERT
- `ingestion_jobs` INSERT (job_status='queued')
- 기존 answers LLM 메트릭 UPDATE

출력 2 — `eval_questions.json`:
```json
[
  {
    "question": "기초생활수급자 신청 방법이 어떻게 되나요?",
    "ground_truth": "주민센터를 방문하시거나 복지로(www.bokjiro.go.kr)에서...",
    "org_id": "org_seoul_120",
    "service_id": "svc_welfare",
    "category": "복지"
  }
]
```

#### `python/ingestion-worker/src/ingestion_worker/embedder.py` (신규)

```
ingestion-worker embed
  --org-id        org_seoul_120   필수
  --session-token <token>         필수
  --model         bge-m3          선택 (기본 bge-m3)
  --chunk-size    600             선택
  --chunk-overlap 100             선택
```

처리 흐름:
1. `GET /admin/documents?organization_id=<org_id>&ingestion_status=pending` 로 대상 문서 조회
2. 각 문서 텍스트 청킹 (chunk_size=600, overlap=100)
3. Ollama bge-m3 임베딩 (1024차원)
4. `document_chunks` 테이블 직접 INSERT (psycopg2)
5. `POST /admin/ingestion-jobs/{id}/status` 로 succeeded 전이

#### `python/eval-runner/src/eval_runner/query_runner.py` (신규)

```
CLI: python -m eval_runner.query_runner
  --input         eval_questions.json   필수
  --output        eval_results.json     선택 (기본값)
  --admin-url     http://localhost:8081 선택
  --session-token <token>               필수
```

출력 — `eval_results.json`:
```json
[
  {
    "questionId":   "question_abc123",
    "question":     "기초생활수급자 신청 방법이 어떻게 되나요?",
    "answer":       "긴급복지지원은 위기상황에 처한...",
    "contexts":     ["청크 텍스트 1", "청크 텍스트 2"],
    "ground_truth": "주민센터를 방문하시거나 복지로에서...",
    "org_id":       "org_seoul_120"
  }
]
```

`contexts`는 Spring AI가 반환하는 RAG 검색 결과 청크 (현재 `citationCount`만 있으므로
백엔드 응답에 `retrievedChunks[]` 필드 추가 필요 — 별도 검토).

#### `python/eval-runner/src/eval_runner/ragas_batch.py` (기존 확장)

`--ground-truth` 플래그 활성화:
- `eval_results.json`의 `ground_truth` 필드 사용 시 Context Recall 계산 포함
- 4개 지표 모두 계산 가능

---

### 3.2 V027 마이그레이션 명세

**파일명**: `V027__seed_public_documents_and_llm_metrics.sql`
**생성 주체**: `ingestion_prep.py` 자동 생성 (수동 작성 아님)

포함 내용:
1. 기존 answered answers LLM 메트릭 UPDATE

| answer_id | input_tokens | output_tokens | total_tokens | estimated_cost_usd |
|---|---|---|---|---|
| ans_004 | 2,840 | 412 | 3,252 | $0.006617 |
| ans_005 | 2,650 | 380 | 3,030 | $0.006163 |
| ans_006 | 2,510 | 340 | 2,850 | $0.005688 |
| ans_007 | 2,720 | 390 | 3,110 | $0.006325 |
| ans_012 | 2,420 | 310 | 2,730 | $0.005400 |
| ans_013 | 2,380 | 325 | 2,705 | $0.005400 |
| ans_014 | 2,510 | 355 | 2,865 | $0.005713 |

단가: gpt-5-latest input $1.25/1M tok, output $7.50/1M tok

2. `documents` 40건 (서울 20 + 부산 20)
3. `document_versions` 40건
4. `ingestion_jobs` 40건 (job_status=`queued`)

**H2 호환**: `ragas_evaluations` V025 생성, `document_chunks` V016 생성 — 모두 H2 호환 확인됨
**테스트 설정**: `application-test.yml` → `flyway.target: "27"`

---

### 3.3 Spring AI RAG 응답에 retrieved contexts 포함 여부

현재 `POST /admin/questions` 응답에 RAG 검색 청크 텍스트가 없음.
RAGAS Context Precision/Recall 계산에 `contexts[]`가 필요하므로 두 가지 옵션:

**Option A** (권장): `rag_search_logs` 테이블에서 `question_id`로 조회
- `query_runner.py`가 질의 후 `GET /admin/rag-search-logs?question_id=<id>` 로 청크 조회
- 기존 `rag_search_logs` / `rag_retrieved_documents` 테이블 활용 (V017)

**Option B**: `POST /admin/questions` 응답에 `retrievedChunks[]` 필드 추가
- 백엔드 응답 스키마 변경 필요 — 이번 change 범위 외로 판단

→ **Option A 채택**: 별도 백엔드 변경 없이 기존 테이블 활용

---

## 4. 페이지별 API 의존성 정리

| 페이지 | API | 신규 여부 |
|---|---|---|
| `ops/page.tsx` | `GET /admin/metrics/daily` | 기존 |
| `ops/page.tsx` | `GET /admin/questions?page_size=5` | 기존 (신규 활용) |
| `ops/quality/page.tsx` | `GET /admin/metrics/daily` | 기존 |
| `ops/quality/page.tsx` | `GET /admin/ragas-evaluations?page_size=1` | **신규** |
| `ops/indexing/page.tsx` | `GET /admin/ingestion-jobs` | 기존 |
| `ops/incidents/page.tsx` | `GET /admin/metrics/daily` | 기존 (임계값 계산용) |
| `client/page.tsx` | `GET /admin/metrics/daily` | 기존 |
| `client/page.tsx` | `GET /admin/questions/unresolved?page_size=1` | 기존 (신규 활용) |
| `qa/page.tsx` | `GET /admin/questions/unresolved` | 기존 |
| `qa/page.tsx` | `GET /admin/qa-reviews?page_size=5` | 기존 (신규 활용) |
| `qa/page.tsx` | `GET /admin/ragas-evaluations?page_size=1` | **신규** |
