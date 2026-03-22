# Proposal

## Change ID
`introduce-spring-ai`

## Summary
- Python rag-orchestrator /generate를 Spring AI (Ollama + PgVector) 로 대체
- SSE 스트리밍: QuestionStreamController (SseEmitter) + SpringAiAnswerService
- rag-orchestrator는 /evaluate (RAGAS) 전용으로 경량화
- eval-runner: 오프라인 RAGAS 배치 평가 패키지 신규 추가

## Impact
- 영향 모듈: admin-api (chatruntime, 신규 evaluation 패키지)
- 영향 API: GET /admin/questions/stream (신규), POST /admin/ragas-evaluations (신규)
- 영향 테스트: 기존 44개 유지 + RagasEvaluationApiTest 신규

## Done Definition
- 기존 44개 테스트 100% 통과
- POST /admin/ragas-evaluations 저장 확인
- GET /admin/questions/stream SSE 토큰 스트리밍 동작
- ragas_evaluations 테이블 마이그레이션 적용
