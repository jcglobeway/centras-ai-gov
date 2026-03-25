# Proposal

## Change ID

`enrich-question-fields`

## Summary

- **변경 목적**: E2E RAG 파이프라인 실행 후 `questions` 및 `chat_sessions` 테이블의 핵심 컬럼들이 null/0으로 남아 있어 FAQ 분석·유형별 통계·세션 해결 여부 파악이 불가능한 문제를 해결한다.
- **변경 범위**:
  - `questions.question_category` — `CreateQuestionCommand`에 필드 추가 후 query_runner에서 `consulting_category`를 전달
  - `questions.question_intent_label` — query_runner에서 `task_category`를 전달 (필드는 이미 존재)
  - `questions.answer_confidence` — rag-orchestrator에서 pgvector cosine distance 기반 신뢰도 계산 후 반환, 질문 생성 후 UPDATE
  - `questions.failure_reason_code` — zero-result → A04 룰 기반 자동 분류, 질문 생성 후 UPDATE
  - `questions.is_escalated` — answer_status가 fallback/no_answer이면 true, 질문 생성 후 UPDATE
  - `chat_sessions.total_question_count` — 질문 생성 시 세션 카운터 INCREMENT
  - `chat_sessions.session_end_type` / `ended_at` — 각 답변 결과에 따라 "answered" / "escalated" 자동 분류
- **제외 범위**: 새 Flyway 마이그레이션 없음 (기존 컬럼만 채움). 프론트엔드 변경 없음 (백엔드 데이터만 채움).

## Impact

- **영향 모듈**: `chat-runtime` (domain, port, service, adapter), `admin-api` (config, web adapter, http adapter)
- **영향 Python**: `rag-orchestrator/app.py`, `eval-runner/query_runner.py`
- **영향 API**: `POST /admin/questions` — request body에 `questionCategory` 추가 (optional)
- **영향 테스트**: `ChatRuntimeApiTests` — `CreateQuestionService` 생성자 변경으로 ServiceConfiguration 주입 검증 영향 가능. H2 `@Modifying` 쿼리 동작 확인 필요.

## Done Definition

- `query-runner` 실행 후 questions 테이블에서 `question_category`, `question_intent_label`이 null이 아닌 것을 확인
- `questions.answer_confidence`가 0~1 사이 소수값으로 채워진 것을 확인
- `questions.failure_reason_code`가 zero-result 케이스에 'A04'로 채워진 것을 확인
- `chat_sessions.total_question_count`가 0이 아닌 정수로 채워진 것을 확인
- `chat_sessions.session_end_type`이 'answered' 또는 'escalated'로 채워진 것을 확인
- `JAVA_HOME=.../openjdk-25.0.2/Contents/Home ./gradlew test` 50개 모두 통과
