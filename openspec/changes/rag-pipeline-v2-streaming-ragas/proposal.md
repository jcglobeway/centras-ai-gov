# Proposal

## Change ID

`rag-pipeline-v2-streaming-ragas`

## Summary

### 변경 목적

RAG 파이프라인을 고도화한다.

- **스트리밍**: 답변 생성을 SSE(Server-Sent Events)로 실시간 전달해 체감 응답 속도를 개선한다.
- **RAGAS 비동기 평가**: 답변 품질(Faithfulness, Answer Relevance, Context Precision)을 답변 생성 직후 비동기로 자동 측정하고 DB에 저장한다. 공공기관 서비스 특성상 "검색 정확도 + Faithfulness"가 핵심 KPI이므로 RAGAS를 주 평가 도구로 삼는다.
- **오프라인 배치 평가**: `eval-runner`로 날짜 기준 RAGAS 리포트를 생성한다.

### 변경 범위

1. **DB 마이그레이션** — `ragas_evaluations` 테이블 (V025)
2. **Admin-API** — `RagasEvaluationController` (POST `/admin/ragas-evaluations`) + 헥사고날 레이어 풀셋
3. **Admin-API** — `GET /admin/questions/stream` SSE 프록시 엔드포인트
4. **rag-orchestrator** — `POST /generate/stream` SSE 엔드포인트 + BackgroundTask RAGAS 평가
5. **eval-runner** — 신규 Python 패키지, RAGAS 오프라인 배치 CLI

### 제외 범위

- ES/pgvector 듀얼 백엔드 (R&D 의존: Exp-4, 5 완료 후)
- 문서 파싱 파이프라인 (`add-document-parsing-pipeline`)
- LLM-as-Judge 평가 (RAGAS 점수 안정화 후 별도 change)
- Context Recall 지표 — GT는 별도 POC(`web2rag-poc`) 인제스션 단계에서 생성 예정. POC 완료 후 `eval-runner`에 `--ground-truth gt.csv` 옵션으로 연계

### 선행 조건

- `naming-structure-cleanup` 완료 후 패키지 위치 기준 적용
  - `RagOrchestratorClient` 위치: `chatruntime/adapter/outbound/http/`
  - UseCase/Command 위치: `application/port/in/`

---

## 아키텍처 흐름

```
[스트리밍]
Frontend EventSource
  → admin-api  GET /admin/questions/stream (SseEmitter)
  → rag-orchestrator  POST /generate/stream (SSE, FastAPI)

[RAGAS 비동기 평가]
/generate/stream 완료
  → FastAPI BackgroundTask
    - Faithfulness       (GT 불필요, 현재 활성)
    - Answer Relevance   (GT 불필요, 현재 활성)
    - Context Precision  (GT 불필요, 현재 활성)
    - Context Recall     (GT 필요, 보류)
  → POST /admin/ragas-evaluations
  → ragas_evaluations 테이블 저장

[오프라인 배치]
python/eval-runner/ragas_batch.py --date YYYY-MM-DD
  입력: rag-orchestrator (GT 없음 모드)
  출력: report_{date}.json
```

---

## Impact

### 영향 모듈

| 모듈 | 변경 유형 |
|---|---|
| `apps/admin-api` | 신규 Controller, Bean 등록, build.gradle.kts, application.yml |
| `modules/chat-runtime` (또는 신규 모듈) | RagasEvaluation 헥사고날 레이어 |
| `python/rag-orchestrator` | `/generate/stream` 엔드포인트, RAGAS BackgroundTask, pyproject.toml |
| `python/eval-runner` (신규) | RAGAS 오프라인 배치 CLI |

### 영향 API

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/admin/questions/stream` | SSE 스트리밍 프록시 (신규) |
| `POST` | `/admin/ragas-evaluations` | RAGAS 평가 결과 수신 (신규) |
| `POST` | `/generate/stream` | rag-orchestrator SSE 엔드포인트 (신규) |
| `POST` | `/generate` | 기존 동기 엔드포인트 (유지) |

### 영향 테스트

- 기존 44개 통과 유지 (V025 마이그레이션 H2 호환 확인)
- 신규: `RagasEvaluationApiTest` — POST 수신 + 저장 검증

---

## DB 스키마 (V025)

```sql
CREATE TABLE ragas_evaluations (
    id                VARCHAR(255) PRIMARY KEY,
    question_id       VARCHAR(255) NOT NULL,
    eval_model        VARCHAR(100) NOT NULL,
    faithfulness      DECIMAL(4,3),
    answer_relevance  DECIMAL(4,3),
    context_precision DECIMAL(4,3),
    context_recall    DECIMAL(4,3),
    raw_scores        TEXT,
    evaluated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (question_id) REFERENCES questions(id)
);
CREATE INDEX idx_ragas_eval_question_id  ON ragas_evaluations(question_id);
CREATE INDEX idx_ragas_eval_evaluated_at ON ragas_evaluations(evaluated_at);
```

---

## RAGAS 지표별 활성화 계획

| 지표 | GT 필요 | 현재 활성 | 비고 |
|---|---|---|---|
| Faithfulness      | 아니오 | 활성 | 답변 ⊂ context 검증 |
| Answer Relevance  | 아니오 | 활성 | 질문-답변 연관성 |
| Context Precision | 아니오 | 활성 | 검색 컨텍스트 관련성 |
| Context Recall    | 예     | 보류 | `web2rag-poc` 인제스션 단계 GT 생성 완료 후 연계 |

---

## 환경변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `RAGAS_EVAL_ENABLED`   | `true`         | RAGAS 비동기 평가 활성화 |
| `RAGAS_JUDGE_PROVIDER` | `ollama`       | RAGAS 내부 LLM (`ollama`\|`claude`\|`openai`) |
| `RAGAS_OLLAMA_MODEL`   | `qwen2.5:7b`  | RAGAS용 Ollama 모델 |
| `LLM_JUDGE_ENABLED`    | `false`        | LLM-as-Judge 보완 평가 (안정화 후 활성) |
| `JUDGE_PROVIDER`       | `ollama`       | LLM Judge provider |

---

## Done Definition

- [ ] `ragas_evaluations` 테이블 생성, H2 테스트 환경 호환 확인
- [ ] `POST /admin/ragas-evaluations` 수신 및 저장 동작
- [ ] rag-orchestrator `POST /generate/stream` SSE 스트리밍 동작
- [ ] rag-orchestrator RAGAS BackgroundTask 실행 및 admin-api 콜백 동작
- [ ] `GET /admin/questions/stream` SSE 프록시 동작
- [ ] `eval-runner ragas_batch.py --date YYYY-MM-DD` GT 없음 모드 동작
- [ ] 기존 44개 테스트 전체 통과
- [ ] 신규 `RagasEvaluationApiTest` 통과
