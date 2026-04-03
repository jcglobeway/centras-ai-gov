# Proposal: 벌크 투입 질문 RAGAS 소급 평가

## 배경

`bulk_query_runner`로 투입된 482건의 질문이 RAGAS 평가 없이 DB에만 존재한다.
`bulk_query_runner`는 `eval_results.json`을 생성하지 않아 `eval-runner`와
연결고리가 없었기 때문이다.

현재 상태:
- 전체 질문 (org_acc): 541건
- RAGAS 평가 완료: 59건
- RAGAS 미평가: **482건**

## 목표

DB에 이미 저장된 질문·답변·retrieved_chunks를 읽어
`eval_results.json`을 재구성한 뒤 `eval-runner`로 Faithfulness · Answer Relevancy를 산출한다.

## 지표 범위

| 지표 | 산출 가능 여부 | 이유 |
|------|:-----------:|------|
| Faithfulness | ✅ | retrieved_chunks만 있으면 됨 |
| Answer Relevancy | ✅ | 질문·답변만 있으면 됨 |
| Context Precision | ❌ | ground_truth 없음 (bulk 투입 데이터는 TL_ 레이블 미연결) |
| Context Recall | ❌ | 동일 |

## 구현 방법

### 신규 스크립트: `export_db_eval_data.py`

`python/eval-runner/src/eval_runner/export_db_eval_data.py`

DB에서 직접:
1. `questions` + `answers` JOIN → question / answer
2. `GET /admin/questions/{id}/context` → retrieved_chunks (contexts)
3. `eval_results.json` 형식으로 직렬화

### 실행 순서

```bash
# 1. DB → eval_results.json 재구성 (미평가 질문만)
export-db-eval-data \
  --org-id org_acc \
  --skip-evaluated      # ragas_evaluations 이미 있는 질문 제외
  --limit 200           # 1회 배치 크기

# 2. RAGAS 평가 실행 (ground_truth 없으므로 faith + relevancy만)
eval-runner \
  --date $(date +%Y-%m-%d) \
  --organization-id org_acc
```

## 수정 파일

| 파일 | 변경 내용 |
|------|----------|
| `python/eval-runner/src/eval_runner/export_db_eval_data.py` | 신규 — DB → eval_results.json 추출 |
| `python/eval-runner/pyproject.toml` | CLI entry point 추가 (`export-db-eval-data`) |
