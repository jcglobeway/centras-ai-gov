
# RAG 파이프라인 — 핵심 지표 & AI 트래킹 전략

> Retrieval-Augmented Generation / 품질 모니터링 전략 · AI/ML Optimized · v1.0

---

## 파이프라인 흐름 & 트래킹 포인트

```
[01 Query 분석]  →  [02 Retrieval]  →  [03 Context 평가]  →  [04 Generation]  →  [05 피드백]
 의도 분류           Recall@K           Relevance              Faithfulness        명시적 👍/👎
 복잡도 측정         Precision@K        Similarity             Hallucination       암묵적 행동
 쿼리 임베딩         MRR / NDCG         LLM Judge              Token / TTFT        재질문률
```

---

## 목표 KPI (Target)

| 지표 | 목표값 | 설명 |
|------|--------|------|
| **Faithfulness** | `> 0.90` | 컨텍스트 기반 답변율 |
| **Recall@K** | `> 0.85` | 관련 문서 검색율 |
| **E2E Latency** | `< 3s` | P95 기준 전체 응답 |
| **Hallucination Rate** | `< 5%` | 근거 없는 생성 비율 |

---

## 핵심 지표

### Retrieval 품질

| 지표 | 목표값 |
|------|--------|
| Recall@K | `> 0.85` |
| Precision@K | `> 0.70` |
| MRR (Mean Reciprocal Rank) | `> 0.75` |
| NDCG@K | `> 0.80` |
| Context Relevance | `> 0.75` |

### Generation 품질

| 지표 | 목표값 |
|------|--------|
| Faithfulness | `> 0.90` |
| Answer Relevance | `> 0.85` |
| Answer Correctness | `> 0.80` |
| Hallucination Rate | `< 0.05` |
| TTFT (Time to First Token) | `< 800ms` |

---

## AI/ML 트래킹 방법론

### 01 · LLM-as-Judge `핵심 전략`

Claude/GPT를 평가자로 활용해 Faithfulness·Relevance·Completeness를 자동 채점. Human 레이블링 없이 대규모 평가 가능.

```python
evaluation_prompt = """
질문: {question}
검색된 컨텍스트: {context}
생성된 답변: {answer}

평가 항목 (각 0-1점):
1. Faithfulness: 답변이 컨텍스트에만 근거하는가?
2. Relevance: 답변이 질문에 적절한가?
3. Completeness: 답변이 충분히 완전한가?

JSON 형태로만 출력: {"faithfulness": 0.9, "relevance": 0.8, "completeness": 0.7}
"""
```

---

### 02 · RAGAS 프레임워크 `자동화 평가`

faithfulness, answer_relevancy, context_recall, context_precision을 한 번에 자동 산출. DataFrame 결과 즉시 활용.

```python
from ragas import evaluate
from ragas.metrics import (
    faithfulness, answer_relevancy,
    context_recall, context_precision
)

result = evaluate(
    dataset,
    metrics=[faithfulness, answer_relevancy,
             context_recall, context_precision]
)
```

---

### 03 · Synthetic QA 생성 `Ground Truth`

LLM으로 문서 기반 Q&A 쌍 자동 생성. 수동 레이블링 없이 난이도별 Ground Truth 확보 및 회귀 테스트 구성.

```python
synthetic_prompt = """
다음 문서를 기반으로 테스트 질문-답변 쌍 5개를 생성하세요.
다양한 난이도(쉬움/중간/어려움)로 만들어주세요.
문서: {document}

JSON 형태로만 출력:
[{"question": "...", "answer": "...", "difficulty": "easy"}]
"""
```

---

### 04 · Embedding 모니터링 `벡터 품질`

쿼리-청크 간 cosine similarity 분포 추적. threshold 미달 비율 알림으로 검색 품질 저하 조기 감지.

```python
from sklearn.metrics.pairwise import cosine_similarity

def track_retrieval_quality(query_emb, retrieved_embs, threshold=0.75):
    similarities = cosine_similarity([query_emb], retrieved_embs)[0]
    return {
        "mean_similarity": similarities.mean(),
        "below_threshold_ratio": (similarities < threshold).mean(),
        "top1_similarity": similarities.max()
    }
```

---

### 05 · 온라인 피드백 루프 `Production`

👍/👎 명시적 + 복사·재질문·세션 종료 등 암묵적 신호 수집. 지속적 프롬프트 및 파인튜닝 개선에 활용.

```python
feedback_signals = {
    "explicit": {
        "thumbs_up": +1,
        "thumbs_down": -1,
        "copy_response": +0.5,
    },
    "implicit": {
        "follow_up_question": -0.3,   # 재질문 = 불만족 신호
        "session_end_after": +0.5,    # 답변 후 종료 = 만족 신호
        "response_time_read": +0.2
    }
}
```

---

### 06 · A/B 테스트 `실험 설계`

청크 사이즈·임베딩 모델·프롬프트 변형 등을 실험군/대조군으로 분리. 지표 개선 여부를 통계적으로 검증.

---

## 툴스택

| 목적 | 도구 |
|------|------|
| 실험 트래킹 | LangSmith, Langfuse, MLflow |
| 자동 평가 | RAGAS, DeepEval, TruLens |
| 모니터링 | Grafana + Prometheus, Datadog |
| A/B 테스트 | LaunchDarkly, Statsig, 직접 구현 |
| 로그 분석 | ELK Stack, OpenSearch, Datadog |

---

## 적용 로드맵

| 시점 | 액션 |
|------|------|
| **Day 1** | Latency + 기본 로깅 세팅 |
| **Week 1** | RAGAS 자동 평가 파이프라인 구축 |
| **Week 2** | LLM Judge 정성 평가 파이프라인 구축 |
| **Month 1** | Synthetic QA로 회귀 테스트 자동화 |
| **지속** | 사용자 피드백 → 프롬프트/파인튜닝 개선 루프 |

---

> **핵심 조합**: LLM-as-Judge + RAGAS → Human 개입 없이 지속적 품질 모니터링 자동화
