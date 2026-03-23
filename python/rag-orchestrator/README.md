# rag-orchestrator

공공기관 RAG 챗봇의 답변 생성과 품질 평가를 담당하는 Python FastAPI 서비스.

- **포트**: `8090`
- **역할**: pgvector 검색 → Ollama LLM 답변 합성 → Admin API에 검색 로그 기록 → RAGAS 평가

---

## 아키텍처

```
시민 질문 (Admin API → POST /admin/questions)
       │
       ▼
SpringAiAnswerService (admin-api)
       │  RAG_ORCHESTRATOR_ENABLED=true 시
       ▼
POST http://localhost:8090/generate
       │
       ├─ 1. Ollama bge-m3로 query embedding 생성
       ├─ 2. pgvector cosine similarity search (document_chunks)
       ├─ 3. 검색 결과 → Admin API POST /admin/rag-search-logs 기록
       └─ 4. Ollama LLM (qwen2.5:7b)으로 답변 합성
              │
              ▼
       GenerateAnswerResponse → admin-api → answers 테이블 저장
```

Ollama가 없거나 DB 연결이 실패하면 `fallback` 상태로 대체 응답을 반환하며, 서비스 전체가 중단되지 않는다.

---

## 사전 요구사항

| 항목 | 버전 |
|------|------|
| Python | 3.12+ |
| uv | 최신 (`pip install uv` 또는 [공식 문서](https://docs.astral.sh/uv/)) |
| Ollama | 로컬 또는 Tailscale 원격 서버 |
| PostgreSQL + pgvector | 15+ (docker-compose 제공) |

### Ollama 모델 (원격 서버에 이미 설치된 경우 생략)

```bash
ollama pull bge-m3       # 임베딩 (필수, 1.2GB)
ollama pull qwen2.5:7b   # LLM 답변 생성 (4.7GB)
```

> 개발 환경에서는 Tailscale로 원격 Ollama 서버(`http://jcg-office.tailedf4dc.ts.net:11434`)에 연결해 사용한다. 연결 확인:
> ```bash
> curl http://jcg-office.tailedf4dc.ts.net:11434/api/tags
> ```

---

## 설치 및 실행

`uv`를 사용한다. `pip install uv`로 설치하거나 [uv 공식 문서](https://docs.astral.sh/uv/)를 참고.

```bash
cd python/rag-orchestrator

# 의존성 설치 (가상환경 자동 생성)
uv sync

# RAGAS 평가 기능 포함 시
uv sync --extra eval

# 실행
uv run rag-orchestrator
# → http://localhost:8090 에서 실행
```

### 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `OLLAMA_URL` | `http://localhost:11434` | Ollama 서버 주소 |
| `OLLAMA_MODEL` | `qwen2.5:7b` | 답변 생성에 사용할 LLM 모델 |
| `DATABASE_URL` | — | PostgreSQL 연결 문자열 (pgvector 검색용) |
| `ADMIN_API_BASE_URL` | `http://localhost:8081` | 검색 로그를 기록할 Admin API 주소 |

```bash
# 예시 — Ollama를 Tailscale 원격 서버에서 사용하는 경우
DATABASE_URL=postgresql://ragops_user:ragops_pass@localhost:5432/ragops_dev \
OLLAMA_URL=http://jcg-office.tailedf4dc.ts.net:11434 \
OLLAMA_MODEL=qwen2.5:7b \
uv run rag-orchestrator
```

---

## API

### `GET /healthz`

서비스 상태 확인.

```bash
curl http://localhost:8090/healthz
# {"status": "ok", "service": "rag-orchestrator"}
```

---

### `POST /generate`

질문에 대한 RAG 답변을 생성한다.

**요청**
```json
{
  "question_id": "question_abc123",
  "question_text": "긴급복지지원 신청 방법을 알려주세요",
  "organization_id": "org_seoul_120",
  "service_id": "svc_welfare"
}
```

**응답**
```json
{
  "question_id": "question_abc123",
  "answer_text": "긴급복지지원은 위기상황에 처한 저소득층을 지원하는 제도입니다...",
  "answer_status": "answered",
  "citation_count": 3,
  "response_time_ms": 1842,
  "fallback_reason_code": null
}
```

| `answer_status` | 의미 |
|-----------------|------|
| `answered` | 정상 답변 생성 (pgvector 검색 성공 + LLM 합성 성공) |
| `fallback` | Ollama 미실행 또는 검색 실패 시 대체 안내 응답 |

**파이프라인 흐름**
1. Ollama `bge-m3`로 질문 임베딩 생성
2. `document_chunks` 테이블에서 cosine similarity 검색 (top-3)
3. 검색 결과를 Admin API `POST /admin/rag-search-logs`에 비동기 기록
4. Ollama LLM으로 컨텍스트 기반 답변 합성

---

### `POST /evaluate`

RAGAS 지표를 계산한다. `pip install -e ".[eval]"` 필요.

**요청**
```json
{
  "samples": [
    {
      "question_id": "q_001",
      "question_text": "긴급복지지원 신청 방법은?",
      "answer_text": "주민센터에 방문하시거나...",
      "contexts": ["긴급복지지원 제도는..."],
      "ground_truth": "주민센터 또는 복지로에서 신청 가능합니다."
    }
  ],
  "judge_provider": "ollama",
  "judge_model": "qwen2.5:7b"
}
```

**응답**
```json
{
  "results": [
    {
      "question_id": "q_001",
      "faithfulness": 0.92,
      "answer_relevancy": 0.87,
      "context_precision": null,
      "context_recall": null
    }
  ],
  "evaluated_count": 1
}
```

> ragas 패키지나 judge LLM이 없으면 지표값이 `null`로 반환된다.

---

## Admin API와의 연동

Admin API가 rag-orchestrator를 호출하려면 다음 환경변수를 설정해야 한다.

```bash
# admin-api 실행 시
RAG_ORCHESTRATOR_ENABLED=true ./gradlew :apps:admin-api:bootRun
```

`RAG_ORCHESTRATOR_ENABLED=false`(기본값)이면 Admin API는 rag-orchestrator를 호출하지 않고 stub 응답을 저장한다.

---

## E2E 파이프라인 실행 순서

```bash
# 1. 인프라 기동
docker-compose up -d                        # PostgreSQL

# 2. Ollama 기동 및 모델 준비
ollama serve &
ollama pull bge-m3 && ollama pull qwen2.5:7b

# 3. rag-orchestrator 기동
cd python/rag-orchestrator
DATABASE_URL=postgresql://ragops_user:ragops_pass@localhost:5432/ragops_dev \
OLLAMA_URL=http://jcg-office.tailedf4dc.ts.net:11434 \
uv run rag-orchestrator &

# 4. Admin API 기동 (RAG 연동 활성화)
cd ../../
RAG_ORCHESTRATOR_ENABLED=true \
JAVA_HOME=/path/to/jdk ./gradlew :apps:admin-api:bootRun &

# 5. 데이터 리셋 (선택)
docker exec ragops-postgres psql -U ragops_user -d ragops_dev \
  -f /dev/stdin < scripts/reset_data.sql

# 6. 실제 질의 투입
SESSION=$(curl -s -X POST http://localhost:8081/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ops@jcg.com","password":"pass1234"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['session']['token'])")

cd python/eval-runner
ADMIN_API_BASE_URL=http://localhost:8081 \
ADMIN_API_SESSION_TOKEN=$SESSION \
python3 -m eval_runner.query_runner --limit 100

# 7. RAGAS 평가
python3 -m eval_runner.ragas_batch
```

---

## Ollama 모델 선택 가이드

| 모델 | 크기 | 한국어 | 속도 | 권장 용도 |
|------|------|--------|------|-----------|
| `qwen2.5:7b` | 5GB | ★★★ | 보통 | 프로덕션·평가 |
| `llama3.2` | 2GB | ★★☆ | 빠름 | 개발·테스트 |
| `llama3.2:1b` | 1GB | ★☆☆ | 매우 빠름 | 빠른 E2E 확인 |

> 임베딩 모델 `bge-m3`는 한국어 검색 정확도가 가장 높아 변경하지 않는 것을 권장한다.

---

## 문제 해결

**Ollama 응답 없음**
```bash
ollama list                               # 설치된 모델 확인
ollama serve                              # 수동 기동
curl http://localhost:11434/api/tags      # 정상 확인
```

**pgvector 검색 결과 없음 (zero_result)**
- `document_chunks` 테이블에 `embedding_vector`가 채워져 있는지 확인
- `ingestion-worker`로 문서를 임베딩·인덱싱해야 한다

**답변이 항상 fallback**
- `RAG_ORCHESTRATOR_ENABLED=true` 설정 확인
- `DATABASE_URL` 환경변수 설정 확인
- Ollama 실행 여부 확인 (`curl http://localhost:11434/api/tags`)
