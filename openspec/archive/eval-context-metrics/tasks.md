# Tasks

## Phase 1 — 사전 확인

- [ ] admin-api 실행 확인 (`curl localhost:8080/actuator/health`)
- [ ] rag-orchestrator 실행 확인 (`curl localhost:8090/health`)
- [ ] Ollama 접근 확인

## Phase 2 — 평가 데이터 준비

```bash
cd python/eval-runner
source .venv/bin/activate

prepare-eval-data \
  --zip ../../data/3.개방데이터/1.데이터/Training/02.라벨링데이터/TL_국립아시아문화전당_질의응답.zip \
  --org-id org_acc \
  --limit 30
```

- [ ] `eval_questions.json` 생성 확인
- [ ] 샘플 출력에서 `ground_truth` 필드 확인

## Phase 3 — 질문 투입

```bash
query-runner --limit 30 --delay 2.0
```

- [ ] 질문 30건 투입 완료 (실패 0건)
- [ ] `rag_search_logs` · `rag_retrieved_documents` 저장 확인

## Phase 4 — RAGAS 평가

```bash
eval-runner \
  --date $(date +%Y-%m-%d) \
  --organization-id org_acc
```

- [ ] `ragas_evaluations` 행 생성 확인
- [ ] `context_precision` · `context_recall` 값이 non-null인지 확인

## Phase 5 — 대시보드 검증

- [ ] `/ops/quality` RAGAS 스코어카드 4개 지표 표시 확인
- [ ] `/ops/chat-history` 상세 패널 Context Precision · Recall 표시 확인
