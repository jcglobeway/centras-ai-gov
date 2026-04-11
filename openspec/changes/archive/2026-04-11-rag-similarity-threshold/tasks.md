# Tasks: rag-similarity-threshold

## Implementation Tasks

### retrieval.py

- [x] `vector_search()` 반환 dict에 `similarity` 필드 추가 (`1 - distance`)
  - 기존 `distance` 필드는 호환성을 위해 유지
  - 신규 필드: `similarity: float` (0.0 ~ 1.0)

### app.py — /generate 엔드포인트

- [x] `vector_search()` 단독 호출을 `hybrid_search()` 이전에 추가 (max similarity 검사용)
  - 단, 이미 semantic cache miss 경로에서 `_query_embedding`이 있으면 임베딩 재생성 생략
  - `RAG_SIMILARITY_THRESHOLD` 환경변수 읽기 (기본값 `0.5`)
- [x] max similarity < threshold 시 `no_answer` 조기 반환 블록 작성
  - `answer_status = "no_answer"`
  - `citation_count = 0`
  - `question_failure_reason_code = "A01"` (관련 문서 없음)
  - `is_escalated = True`
  - `confidence_score = 0.0`
  - `answer_text = ""` (빈 문자열, LLM 미호출)
  - `fallback_reason_code = None`
- [x] `no_answer` 경로에서 `_log_search_result()` 호출 시 `retrieval_status="low_similarity"` 전달
  - `_log_search_result()` 함수 시그니처에 `retrieval_status` 파라미터 추가 (기본값 유지)
- [x] `no_answer` 결과는 Redis / semantic cache 저장 제외

### app.py — /generate/stream 엔드포인트

- [x] `/generate`와 동일한 similarity 검사 로직 적용
- [x] 임계값 미달 시 스트리밍 fallback 응답 반환
  - `answer_status = "no_answer"`, `citation_count = 0`, `confidence_score = 0.0`
  - content 청크 없이 `done` 이벤트만 emit

### .env.example

- [x] `RAG_SIMILARITY_THRESHOLD=0.5` 항목 및 설명 주석 추가

## Testing Tasks

- [x] 오프토픽 질문 (`날씨 어때?`) → `answer_status=no_answer` 수동 확인 (curl)
- [x] 관련 질문 → `answer_status=answered` 기존 동작 유지 확인
- [x] `RAG_SIMILARITY_THRESHOLD=0.0` 설정 시 임계값 무력화(모든 질문 통과) 확인
- [x] `RAG_SIMILARITY_THRESHOLD=1.0` 설정 시 모든 질문 `no_answer` 반환 확인
- [x] vector search 결과 없을 때 (기존 `A04` 경로) 변경 없이 유지 확인
