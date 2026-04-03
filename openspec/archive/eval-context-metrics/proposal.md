# Proposal: Context Precision/Recall 지표 산출 파이프라인 실행

## 배경

현재 `/ops/quality` 및 `/ops/chat-history` 대시보드에서 RAGAS 4개 지표 중
Faithfulness · Answer Relevancy는 표시되지만,
**Context Precision · Context Recall은 대부분 null**로 비어 있다.

이유: eval-runner가 ground_truth 없이 실행되어 해당 지표가 산출되지 않음.

## 목표

`TL_국립아시아문화전당_질의응답.zip` (이미 `data/` 디렉토리에 존재)을 사용해
ground_truth 기반 RAGAS 평가를 실행, Context Precision · Context Recall을
`ragas_evaluations` 테이블에 채운다.

## 데이터 소스

| 파일 | 경로 | 용도 |
|------|------|------|
| `TL_국립아시아문화전당_질의응답.zip` | `data/3.개방데이터/1.데이터/Training/02.라벨링데이터/` | 질문 + ground_truth |
| `TS_국립아시아문화전당.zip` | `data/3.개방데이터/1.데이터/Training/01.원천데이터/` | 상담 원문 (문서 임베딩용) |

## 실행 단계

### Phase 1 — 사전 확인
- rag-orchestrator (`localhost:8090`) 실행 확인
- admin-api (`localhost:8080`) 실행 확인
- Ollama 접근 가능 확인

### Phase 2 — 평가 데이터 준비
- `prepare-eval-data` 실행: TL_ zip → `eval_questions.json` (ground_truth 포함)

### Phase 3 — 질문 투입
- `query-runner`: `eval_questions.json`의 질문을 RAG 파이프라인에 투입
- 결과: `ragas_evaluations` 이전에 `questions` + `answers` + `rag_search_logs` + `rag_retrieved_documents` 저장

### Phase 4 — RAGAS 평가
- `eval-runner`: retrieved_chunks + ground_truth 비교 → 4개 지표 산출
- 결과: `ragas_evaluations`에 `context_precision` · `context_recall` 포함 저장

### Phase 5 — 대시보드 검증
- `/ops/quality` RAGAS 스코어카드에 4개 지표 표시 확인
- `/ops/chat-history` 상세 패널 RAGAS 섹션에 Context Precision · Recall 표시 확인

## 코드 변경 없음

이 변경은 **기존 구현된 파이프라인을 올바른 순서로 실행**하는 것이며
소스 코드 수정은 없다.
