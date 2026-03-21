# Design: dashboard-v2-data-pipeline

## 1. 프론트엔드 컴포넌트 설계

### 1.1 KpiCard 확장 (`frontend/src/components/charts/KpiCard.tsx`)

기존 KpiCard에 `status` prop을 추가해 v3 디자인의 상단 컬러 스트라이프를 구현한다.

```
status?: "ok" | "warn" | "critical"
  ok       → 상단 2px green (#00c47a)
  warn     → 상단 2px amber (#f5a623)
  critical → 상단 2px red (#ff4560)
  없음      → 스트라이프 없음 (기존 동작 유지)
```

호출부에서 메트릭 값과 임계값을 비교해 status를 결정한다. KpiCard 자체는 판단하지 않는다.

---

### 1.2 ProgressBar (`frontend/src/components/ui/ProgressBar.tsx`)

파이프라인 단계별 레이턴시 시각화용. v3 HTML의 `.pr` 스타일을 React로 구현.

```
props:
  label   : string      // "Retrieval", "LLM 호출", "후처리"
  valueMs : number      // 실측값 (ms)
  maxMs   : number      // 전체 너비 기준 (P95 합계 등)
  color   : string      // tailwind color class 또는 hex
```

레이아웃: `label(w-24) | 채움 바(flex-1) | valueMs(w-16 text-right)`

---

### 1.3 AlertBanner (`frontend/src/components/ui/AlertBanner.tsx`)

임계값 초과 경고 배너. 페이지 최상단에 조건부 렌더링.

```
props:
  variant  : "warn" | "critical"
  message  : string
  onDismiss: () => void   // ✕ 버튼
```

- `warn`     → amber 배경/테두리
- `critical` → red 배경/테두리
- 상태는 로컬 useState로 dismiss 처리 (서버 저장 없음)

---

### 1.4 ScoreTable (`frontend/src/components/ui/ScoreTable.tsx`)

RAGAS 지표를 표 형태로 표시. `ScoreRow[]`를 받아 렌더링.

```typescript
interface ScoreRow {
  label  : string         // "Faithfulness"
  value  : number | null  // 0.92
  target : number         // 0.90
  unit?  : string         // 기본 없음 (소수점 2자리 표시)
}
```

상태 badge 판단: `value >= target` → success, `value >= target * 0.9` → warning, 그 외 → error. value가 null이면 "-" 표시.

---

## 2. 대시보드 페이지 설계

### 2.1 Ops 메인 (`ops/page.tsx`)

**데이터 출처**: `GET /admin/metrics/daily` (기존), `GET /admin/questions?page_size=5` (신규 활용)

**레이아웃**:
```
[AlertBanner] — fallbackRate > 10% or zeroResultRate > 5% 시 표시

[KpiCard × 5] — 2열(모바일) / 5열(데스크톱)
  Answer Rate   | Fallback Rate | Zero Result Rate | Avg Response Ms | Total Questions

[파이프라인 레이턴시] — Card 안에 ProgressBar × 3
  Retrieval 438ms / LLM 호출 1,128ms / 후처리 114ms
  * answers 테이블에 단계별 breakdown이 없으므로 seed 고정값 사용
  * 추후 OpenTelemetry 연동 시 API로 교체

[최근 질문 테이블] — Card 안에 Table
  컬럼: 내용(truncate) | 답변상태(Badge) | 생성일
  5건, page_size=5
```

**KPI 상태 판단 기준**:
| 지표 | ok | warn | critical |
|---|---|---|---|
| Answer Rate | ≥ 90% | ≥ 80% | < 80% |
| Fallback Rate | < 10% | < 15% | ≥ 15% |
| Zero Result Rate | < 5% | < 8% | ≥ 8% |
| Avg Response Ms | < 1500 | < 2500 | ≥ 2500 |

---

### 2.2 Ops Quality (`ops/quality/page.tsx`)

**데이터 출처**: 기존 `GET /admin/metrics/daily` + 신규 `GET /admin/ragas-evaluations?page_size=1`

**레이아웃**:
```
[기존 KpiCard × 4] — 유지

[RAGAS 스코어카드] — ScoreTable
  Faithfulness      | 점수 | 목표 0.90
  Answer Relevance  | 점수 | 목표 0.85
  Context Precision | 점수 | 목표 0.70
  * ragas_evaluations가 비어있으면 "평가 데이터 없음" 빈 상태

[기존 라인차트] — 유지
```

---

### 2.3 Ops Indexing (`ops/indexing/page.tsx`)

jobs 배열에서 클라이언트 측 집계:
```typescript
const succeeded = jobs.filter(j => j.status === "succeeded").length
const failed    = jobs.filter(j => j.status === "failed").length
const running   = jobs.filter(j => j.status === "running").length
```

테이블 위에 KpiCard 3개 추가. 기존 테이블 유지.

---

### 2.4 Ops Incidents (`ops/incidents/page.tsx`)

실시간 알림 API 없음 → metrics 데이터에서 임계값 초과 항목을 프론트에서 계산해 정적 테이블로 표시.

```typescript
// daily metrics 최신값 기준으로 알림 생성
const alerts = []
if (latest?.fallbackRate > 10)     alerts.push({ metric: "Fallback Rate", ... })
if (latest?.zeroResultRate > 5)    alerts.push({ metric: "Zero Result Rate", ... })
if (latest?.avgResponseTimeMs > 1500) alerts.push({ metric: "Avg Response Time", ... })
```

컬럼: 지표 / 현재값 / 임계값 / 심각도 / 상태

---

### 2.5 Client 메인 (`client/page.tsx`)

`GET /admin/questions/unresolved?page_size=1` → `total` 값을 미해결 건수 KpiCard로 표시.

응답률 목표 진행 바: `resolvedRate / 80 * 100` % 너비의 ProgressBar 재활용.

---

### 2.6 QA 메인 (`qa/page.tsx`)

RAGAS ScoreTable + 최근 QA 리뷰 5건 테이블 추가. `GET /admin/qa-reviews?page_size=5` 활용.

---

## 3. 백엔드 API 설계

### 3.1 GET /admin/ragas-evaluations

**Hexagonal 레이어 추가**:

```
application/port/in/
  ListRagasEvaluationsUseCase.kt
    fun listEvaluations(query: ListRagasEvaluationsQuery): PagedResult<RagasEvaluationSummary>

application/port/out/
  LoadRagasEvaluationsPort.kt
    fun loadAll(pageable: Pageable): List<RagasEvaluationSummary>

application/service/
  ListRagasEvaluationsService.kt  — UseCase 구현

adapter/outbound/persistence/
  LoadRagasEvaluationsPortAdapter.kt (open class)  — Port 구현
  JpaRagasEvaluationRepository  — findAll(Pageable) 추가
```

**응답 형식** (기존 PagedResponse 패턴 유지):
```json
{
  "items": [
    {
      "id": "ragas_001",
      "questionId": "q_004",
      "faithfulness": 0.92,
      "answerRelevance": 0.87,
      "contextPrecision": 0.74,
      "contextRecall": null,
      "evaluatedAt": "2026-03-18T10:00:00Z"
    }
  ],
  "total": 5,
  "page": 1,
  "pageSize": 10,
  "requestId": "...",
  "generatedAt": "..."
}
```

**쿼리 파라미터**: `question_id` (optional), `page` (default 1), `page_size` (default 10)

**ArchUnit 준수**:
- `LoadRagasEvaluationsPortAdapter`: `open class`
- Bean 등록: `RepositoryConfiguration` + `ServiceConfiguration`에 `@Bean` 명시
- Controller → UseCase 인터페이스에만 의존

---

## 4. 데이터 파이프라인 설계 (실제 동작 기준)

목표: SQL seed 가 아니라 실제 데이터가 임베딩되어 pgvector에 저장되고,
Spring AI RAG 파이프라인이 실제 벡터 검색으로 답변을 생성하며,
eval-runner가 실제 RAGAS 점수를 DB에 저장하는 엔드투엔드 파이프라인.

### 4.1 전체 흐름

```
[ZIP 파일]
    ↓ 압축 해제 + JSON 파싱
[ingestion_prep.py]        ← 신규 스크립트 (python/eval-runner)
    ↓ V027 SQL 생성 + eval_questions.json 추출
[V027 Flyway 마이그레이션]  ← documents + document_versions 메타데이터만
    ↓
[ingestion-worker embed]   ← 기존 worker에 embed 서브커맨드 추가
    ↓ Ollama bge-m3 (1024차원)
[document_chunks 테이블]   ← pgvector
    ↓
[query_runner.py]          ← 신규 스크립트 (python/eval-runner)
    ↓ POST /admin/questions (Spring AI RAG)
[questions + answers 테이블] ← LLM 메트릭 자동 저장
    ↓
[ragas_batch.py]           ← 기존 eval-runner (ground_truth 포함)
    ↓ POST /admin/ragas-evaluations
[ragas_evaluations 테이블]
    ↓
[대시보드 실제 지표 표시]
```

### 4.2 스텝별 상세

#### Step 1. 문서 준비 (`python/eval-runner/src/eval_runner/ingestion_prep.py` 신규)

- 대상 ZIP (Training + Validation 전체):
  - `TL_지방행정기관_질의응답.zip` + `VL_지방행정기관_질의응답.zip`
  - `TL_중앙행정기관_질의응답.zip` + `VL_중앙행정기관_질의응답.zip`
  - `TL_국립아시아문화전당_질의응답.zip` + `VL_국립아시아문화전당_질의응답.zip`
- **데이터 전량 추출** — 건수 제한 없이 ZIP 내 모든 Q&A 사용
  - 지방행정기관: 복지/교육/교통/주거/세금/민원/환경 등 전 카테고리
  - 중앙행정기관: 법령/정책/신청/자격/절차 등 전 카테고리
  - 국립아시아문화전당: 시설/프로그램/예약/관람 등
- org 매핑:
  - 지방행정기관 → `org_seoul_120` (svc_welfare)
  - 중앙행정기관 → `org_busan_220` (svc_faq)
  - 국립아시아문화전당 → 두 기관에 분산 또는 별도 서비스로 분류
- ZIP 내 JSON 파싱 → 각 answer 텍스트를 문서 원문으로 사용
- 출력 1: `V027__seed_public_documents_and_llm_metrics.sql`
  - `documents` + `document_versions` INSERT (전량)
  - 기존 answered answers LLM 메트릭 UPDATE
- 출력 2: `eval_questions.json` — 100건 테스트셋 (케이스 유형별 균등 샘플링)

#### Step 2. 문서 임베딩 (`python/ingestion-worker` 확장)

`embed` 서브커맨드 추가:

```python
# ingestion_worker/embedder.py (신규)
import ollama
import psycopg2

def embed_and_store(doc_id: str, text: str, org_id: str, chunk_size: int = 600):
    chunks = chunk_text(text, chunk_size, overlap=100)
    for i, chunk in enumerate(chunks):
        vec = ollama.embeddings(model="bge-m3", prompt=chunk)["embedding"]
        db.execute(
            "INSERT INTO document_chunks (id, document_id, organization_id, "
            "chunk_index, chunk_text, embedding_vector, created_at) "
            "VALUES (%s, %s, %s, %s, %s, %s::vector, NOW())",
            [f"chunk_{doc_id}_{i}", doc_id, org_id, i, chunk, vec]
        )
```

실행:
```bash
ollama pull bge-m3
ingestion-worker embed --org-id org_seoul_120
ingestion-worker embed --org-id org_busan_220
```

#### Step 3. 평가 질의 (`python/eval-runner/src/eval_runner/query_runner.py` 신규)

```python
def run_queries(questions_file, session_token):
    questions = json.load(open(questions_file))
    results = []
    for q in questions:
        resp = httpx.post(
            "http://localhost:8081/admin/questions",
            headers={"X-Admin-Session-Id": session_token},
            json={"organizationId": q["org_id"], "serviceId": q["service_id"],
                  "questionText": q["question"], "channel": "eval"}
        )
        data = resp.json()
        results.append({
            "questionId": data["questionId"],
            "question":   q["question"],
            "answer":     data.get("answerText", ""),
            "contexts":   data.get("citations", []),
            "ground_truth": q["ground_truth"],   # ZIP 원문 정답
        })
    json.dump(results, open("eval_results.json", "w"), ensure_ascii=False)
```

#### Step 4. RAGAS 평가 (`python/eval-runner/src/eval_runner/ragas_batch.py` 기존)

`--ground-truth` 플래그 활성화:
- `eval_results.json`의 `ground_truth` 필드 사용
- 4개 지표 모두 계산: Faithfulness, Answer Relevance, Context Precision, **Context Recall** (GT 있으므로)

```bash
python -m eval_runner.ragas_batch \
  --input eval_results.json \
  --admin-url http://localhost:8081 \
  --session-token "$TOKEN" \
  --judge-provider ollama \
  --ollama-model qwen2.5:7b
```

### 4.3 테스트셋 100건 샘플링 전략

케이스 유형별 균등 샘플링으로 평가 편향 방지:

| 케이스 유형 | 건수 | 예시 질문 |
|---|---|---|
| 사실 조회 | 30건 | "○○ 부서 연락처가 어떻게 되나요?" |
| 절차 안내 | 30건 | "기초생활수급자 신청 방법이 어떻게 되나요?" |
| 자격 확인 | 20건 | "국민기초생활보장 수급 자격이 어떻게 되나요?" |
| 비교 질문 | 10건 | "일반 민원과 긴급 민원의 차이가 무엇인가요?" |
| 복합 질문 | 10건 | "신청 자격이 되는지 확인하고 신청 방법도 알려주세요" |

기관별 배분:
- 지방행정기관 (`org_seoul_120`): 40건
- 중앙행정기관 (`org_busan_220`): 40건
- 국립아시아문화전당: 20건

샘플링 로직:
```python
def stratified_sample(pool: list[dict], n: int, keys: list[str]) -> list[dict]:
    """케이스 유형 + 기관 기준 층화 샘플링"""
    from collections import defaultdict
    groups = defaultdict(list)
    for item in pool:
        key = tuple(item.get(k, "") for k in keys)
        groups[key].append(item)

    result = []
    per_group = max(1, n // len(groups))
    for group in groups.values():
        result.extend(random.sample(group, min(per_group, len(group))))

    # 부족분 랜덤 보충
    remaining = [x for x in pool if x not in result]
    result.extend(random.sample(remaining, max(0, n - len(result))))
    return result[:n]
```

---

### 4.4 V027 마이그레이션 범위 (메타데이터만)

```sql
-- V027__seed_public_documents_and_llm_metrics.sql

-- ── 1. 기존 answered answers LLM 메트릭 UPDATE ──────────────────────────
UPDATE answers SET
  model_name='gpt-5-latest', provider_name='openai',
  input_tokens=2840, output_tokens=412, total_tokens=3252,
  estimated_cost_usd=0.006617, finish_reason='stop'
WHERE id IN ('ans_004','ans_005','ans_006','ans_007','ans_012','ans_013','ans_014');

-- ── 2. 공공 민원 documents (TL+VL × 3기관 전량) ── ingestion_prep.py 생성
INSERT INTO documents (...) VALUES ...;
INSERT INTO document_versions (...) VALUES ...;
INSERT INTO ingestion_jobs (..., job_status='queued') VALUES ...;
```

실제 `questions`, `answers`, `ragas_evaluations` 는 파이프라인 실행으로 생성.

### 4.5 사전 요구사항

| 항목 | 요건 |
|---|---|
| Ollama | `bge-m3`, `qwen2.5:7b` 모델 pull |
| PostgreSQL | docker-compose up, pgvector V018 마이그레이션 완료 |
| Spring AI 백엔드 | `SPRING_AI_ANSWER_ENABLED=true`, `SPRING_AI_ANSWER_PROVIDER=ollama` |
| ingestion-worker | `pip install -e python/ingestion-worker[embed]` |
| eval-runner | `pip install -e python/eval-runner[eval]` |

### 4.6 원샷 실행 스크립트 (`run_pipeline.sh` 신규)

```bash
#!/bin/bash
set -e

# 1. DB + 백엔드 시작
docker-compose up -d
./gradlew :apps:admin-api:bootRun &
sleep 15

# 2. 세션 토큰 획득
TOKEN=$(curl -s -X POST http://localhost:8081/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ops@jcg.com","password":"pass1234"}' | jq -r '.session.token')

# 3. 문서 준비 (V027 SQL + eval_questions.json 생성)
python python/eval-runner/src/eval_runner/ingestion_prep.py \
  --zip-dir data/3.개방데이터 \
  --output-sql apps/admin-api/src/main/resources/db/migration/V027__seed_public_documents_and_llm_metrics.sql \
  --output-questions eval_questions.json

# 4. 재시작 (V027 마이그레이션 적용)
kill %1; sleep 3
./gradlew :apps:admin-api:bootRun &
sleep 15

# 5. 임베딩
ingestion-worker embed --session-token "$TOKEN"

# 6. 평가 질의 실행
python python/eval-runner/src/eval_runner/query_runner.py \
  --input eval_questions.json --session-token "$TOKEN"

# 7. RAGAS 평가
python -m eval_runner.ragas_batch \
  --input eval_results.json --session-token "$TOKEN" \
  --judge-provider ollama --ollama-model qwen2.5:7b

echo "파이프라인 완료. 대시보드에서 지표를 확인하세요."
```
