# Proposal: rag-similarity-threshold

## Problem

현재 `rag-orchestrator`는 pgvector 검색 후 유사 문서가 사실상 없는 경우에도 LLM을 호출해 답변을 생성한다. 이로 인해 두 가지 문제가 발생한다.

1. **오프토픽 질문 미탐지**: "날씨 어때?", "주식 정보 알려줘" 처럼 지식베이스와 무관한 질문에도 LLM이 "정보가 없지만..." 형태의 답변을 생성하고 `answer_status = answered`로 기록된다.

2. **KPI 왜곡**: ANSWER RATE가 실제보다 부풀려지고, Knowledge Gap(`no_answer`) 미탐지로 인해 운영자가 오프토픽 질문 규모와 지식베이스 보완 필요성을 파악할 수 없다.

### 현재 흐름 (문제)

```
hybrid_search() → [유사도 낮은 결과 반환] → generate_answer_with_ollama() → answer_status=answered
```

### 근본 원인

`hybrid_search()`가 RRF 점수를 `[0, 1]` distance로 정규화해 반환하지만, vector search 단계에서 최고 유사도(cosine similarity)가 임계값 미만인지 검사하는 로직이 없다. RRF fusion 이후에는 개별 vector similarity 정보가 손실된다.

## Proposed Solution

`hybrid_search()` 실행 직후, vector search 결과의 **최고 cosine similarity**(`1 - distance`)가 임계값 미만이면 LLM 호출 없이 즉시 `no_answer`를 반환한다.

### 변경 후 흐름

```
vector_search() → [최고 similarity < threshold] → no_answer 즉시 반환 (LLM 호출 없음)
                → [최고 similarity >= threshold] → hybrid_search() → LLM → answered
```

### 임계값 기준

- bge-m3 임베딩 기준 cosine similarity 0.5 이하는 사실상 관련 없는 문서
- 환경변수 `RAG_SIMILARITY_THRESHOLD`로 운영 중 조정 가능 (기본값 `0.5`)

### 구현 위치

| 파일 | 변경 내용 |
|------|-----------|
| `python/rag-orchestrator/src/rag_orchestrator/retrieval.py` | `vector_search()`가 `distance` 외에 `similarity`(`1 - distance`) 포함한 결과 반환, 또는 `get_max_similarity()` 헬퍼 추가 |
| `python/rag-orchestrator/src/rag_orchestrator/app.py` | `/generate` 엔드포인트: `vector_search()` 호출 후 max similarity 검사 → 미달 시 `no_answer` 조기 반환. `/generate/stream` 엔드포인트 동일 처리 |
| `python/rag-orchestrator/.env.example` | `RAG_SIMILARITY_THRESHOLD=0.5` 문서화 |

## Out of Scope

- BM25 점수 기반 임계값 검사 (RRF 이전 vector similarity만 사용)
- Admin API 스키마 변경 (`answer_status` 값 `no_answer`는 기존 enum에 이미 존재)
- Semantic cache 저장 로직 변경 (`no_answer`는 캐시하지 않음)
- Spring Boot 백엔드 변경
- 프론트엔드 변경

## Success Criteria

1. `RAG_SIMILARITY_THRESHOLD=0.5` 환경변수 미설정 시 기본값 `0.5` 적용
2. 오프토픽 질문 → `answer_status=no_answer`, `question_failure_reason_code=A01`, `citation_count=0` 반환
3. 관련 질문 → 기존 동작 유지 (`answered` 또는 `fallback`)
4. `/generate` 와 `/generate/stream` 양쪽 엔드포인트에서 동일하게 동작
5. `no_answer` 경로에서는 LLM API 호출이 발생하지 않음
6. 임계값 미달 시 `rag_search_log`는 `retrieval_status=low_similarity`로 기록
