# eval-runner

RAG 파이프라인 평가 도구 모음. 오프라인 배치 평가, 실시간 이벤트 드리븐 평가, 데이터 준비, 질의 주입, 질문 클러스터링을 포함한다.

---

## 생성 지표

| 지표 | 범위 | 계산 방식 | ground_truth 필요 |
|------|------|-----------|:-----------------:|
| **Faithfulness** | 0–1 | RAGAS — 답변의 주장이 context에 근거하는 비율 | X |
| **Answer Relevancy** | 0–1 | RAGAS — 답변이 질문에 얼마나 관련 있는지 (임베딩 유사도) | X |
| **Context Precision** | 0–1 | RAGAS — 검색된 청크 중 실제로 유용한 청크의 비율 | O |
| **Context Recall** | 0–1 | RAGAS — ground_truth를 커버하는 청크 비율 | O |
| **Citation Coverage** | 0–1 | LLM judge — 검색된 N개 청크 중 답변 생성에 실제 활용된 비율 | X |
| **Citation Correctness** | 0–1 | LLM judge — 활용된 청크가 답변 내용을 올바르게 지지하는 비율 | X |

**목표 기준값**

| 지표 | 목표 |
|------|------|
| Faithfulness | ≥ 0.85 |
| Answer Relevancy | ≥ 0.80 |
| Context Precision | ≥ 0.75 |
| Context Recall | ≥ 0.70 |
| Citation Coverage | ≥ 0.80 |
| Citation Correctness | ≥ 0.85 |

> **Faithfulness vs Citation Correctness**: Faithfulness는 답변 전체가 context에 근거하는지를 보고, Citation Correctness는 검색 청크 단위로 답변 지지 여부를 판단한다. 낮은 Faithfulness는 환각(hallucination) 문제를, 낮은 Citation Correctness는 청크 단위 왜곡 문제를 시사한다.

---

## CLI 명령 목록

### 핵심 평가

#### `eval-runner` — 오프라인 배치 평가

지정한 날짜(또는 eval_results.json 기준)의 질문에 대해 6개 지표를 계산하고 Admin API에 저장한다.

```bash
# 날짜 기준 평가 (DB 직접 조회)
eval-runner --date 2026-04-03

# 특정 기관 필터
eval-runner --date 2026-04-03 --organization-id org_acc

# eval_results.json의 question_id 목록 기준으로 평가 (날짜 무시)
eval-runner --from-eval-results

# 결과 미전송 (구조만 출력)
eval-runner --date 2026-04-03 --dry-run
```

**동작 흐름**:
1. DB 또는 Admin API에서 `questions` + `answers` 조회
2. `eval_results.json`이 있으면 `contexts`·`ground_truth` 보완
3. RAGAS 4지표 + Citation 2지표 계산 (Ollama LLM judge)
4. `POST /admin/ragas-evaluations` 로 결과 전송

---

#### `realtime-eval` — 실시간 이벤트 드리븐 평가

Redis `ragas:eval:queue`를 구독하다가 새 답변이 생성될 때마다 자동으로 평가를 수행한다. 답변 생성 파이프라인과 완전히 분리된 별도 프로세스.

```bash
realtime-eval \
  --redis-url redis://localhost:6379 \
  --admin-api-url http://localhost:8081
```

**동작 흐름**:
```
admin-api (답변 저장)
  → Redis LPUSH ragas:eval:queue {questionId}
  → realtime-eval BRPOP
  → GET /admin/questions/{id}/context (retrieved chunks)
  → _compute_metrics() (6지표 계산)
  → POST /admin/ragas-evaluations
  → 실패 시 ragas:eval:dlq 에 push
```

> ground_truth 없이 실행되므로 Context Precision·Recall은 `null`로 저장된다.

---

### 데이터 준비

#### `prepare-eval-data` — 라벨링 데이터 → 평가 질문 목록 생성

AI Hub `TL_*_질의응답.zip`에서 Q&A 쌍을 추출해 `eval_questions.json`을 생성한다.

```bash
prepare-eval-data \
  --zip /path/to/TL_국립아시아문화전당_질의응답.zip \
  --org-id org_acc \
  --limit 50
```

출력 (`eval_questions.json`):
```json
[
  {
    "question": "전시 관람료가 얼마인가요?",
    "ground_truth": "상설전시는 무료...",
    "task_category": "일반문의",
    "consulting_category": "관람안내",
    "org_id": "org_acc",
    "service_id": "svc_acc_chatbot"
  }
]
```

---

#### `ingest-training-docs` — 원천데이터 pgvector 임베딩 인제스터

AI Hub `TS_*.zip`의 상담 대화 원문을 청크로 분할하고 Ollama `bge-m3`로 임베딩해 `documents`·`document_chunks` 테이블에 직접 삽입한다.

```bash
ingest-training-docs \
  --zip /path/to/TS_국립아시아문화전당.zip \
  --org-id org_acc \
  --limit 200 \
  --chunk-size 800
```

---

#### `citizen-query-gen` — 원천데이터 → 시민 질의 생성

`TS_*.zip`의 고객 발화를 Ollama로 독립적인 민원 질문으로 변환해 `citizen_questions.json`을 생성한다. 질의 데이터가 라벨링 셋 외부에서도 필요할 때 사용한다.

```bash
citizen-query-gen \
  --zip /path/to/TS_국립아시아문화전당.zip \
  --limit 200
```

---

### 질의 주입

#### `query-runner` — 단건 질의 실행 및 eval_results.json 생성

`eval_questions.json`을 읽어 Admin API에 순차 질의를 보내고, 각 질의의 `contexts`(검색된 청크)·`answer`를 수집해 `eval_results.json`을 생성한다. `eval-runner`의 Context Precision·Recall 계산에 필요한 파일이다.

```bash
query-runner \
  --input eval_questions.json \
  --limit 50 \
  --delay 2.0
```

---

#### `bulk-query-runner` — 대량 질의 주입 (멀티턴 세션 지원)

대규모 데이터셋을 Admin API에 투입할 때 사용한다. `source_id` 기준으로 그룹핑해 관련 질문들을 하나의 멀티턴 세션으로 묶고, 투입 완료 후 `created_at`을 최근 30일 범위로 분산시킨다.

```bash
bulk-query-runner \
  --zip /path/to/TL_국립아시아문화전당_질의응답.zip \
  --limit 200 \
  --max-turns 3 \
  --delay 1.0

# 사전 생성된 질문 파일 사용
bulk-query-runner \
  --input-json citizen_questions.json \
  --limit 500
```

---

### 보조 도구

#### `cluster-questions` — 의미 기반 질문 클러스터링

최근 N일간의 질문을 LLM 키워드 추출 + `bge-m3` 임베딩 코사인 유사도 2-stage로 클러스터링해 `question_similarity_groups` 테이블에 저장한다.

```bash
cluster-questions --org-id org_acc --days 7
```

---

#### `export-db-eval-data` — DB → eval_results.json 재구성

`ragas_evaluations`가 없는 질문을 DB에서 읽어 `eval_results.json`을 재구성한다. `bulk-query-runner`로 주입된 질문처럼 `ground_truth` 없이 투입된 경우에 사용한다.

```bash
export-db-eval-data --org-id org_acc --limit 200
```

---

## 실행 순서 (전체 E2E 파이프라인)

```
1. ingest-training-docs --zip TS_*.zip --org-id org_acc
       └─ document_chunks에 pgvector 임베딩 삽입

2. prepare-eval-data --zip TL_*_질의응답.zip --org-id org_acc
       └─ eval_questions.json 생성 (질문 + ground_truth)

3. query-runner --input eval_questions.json --limit 50
       └─ Admin API 질의 → eval_results.json 생성 (contexts + answer)

4. eval-runner --from-eval-results
       └─ eval_results.json 기준 6지표 계산 → ragas_evaluations 저장
```

또는 **실시간 모드** (3, 4 대신):

```
3'. realtime-eval  (별도 프로세스로 상시 실행)
       └─ 질의가 들어올 때마다 자동 평가
```

---

## 환경변수

`.env` 파일에 아래 값을 설정한다 (`.env.example` 참고).

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `ADMIN_API_BASE_URL` | `http://localhost:8081` | Admin API 주소 |
| `ADMIN_API_SESSION_TOKEN` | — | Admin API 세션 토큰 |
| `DATABASE_URL` | — | PostgreSQL 연결 문자열 (DB 직접 조회 시 필요) |
| `REDIS_URL` | `redis://localhost:6379` | Redis 주소 (realtime-eval용) |
| `OLLAMA_URL` | `http://jcg-office.tailedf4dc.ts.net:11434` | Ollama 서버 주소 |
| `RAGAS_OLLAMA_MODEL` | `qwen2.5:7b` | RAGAS·Citation judge LLM 모델 |

---

## 저장 위치

모든 평가 결과는 Admin API를 통해 `ragas_evaluations` 테이블에 저장되며, 아래 엔드포인트로 조회한다.

| 엔드포인트 | 설명 |
|------------|------|
| `GET /admin/ragas-evaluations` | 질문별 평가 결과 목록 |
| `GET /admin/ragas-evaluations/summary` | 기간별 평균 (current / previous 비교) |

프론트엔드에서는 `/ops/quality` (스코어카드)와 `/ops/quality-summary` (레이더 차트) 페이지에 표시된다.
