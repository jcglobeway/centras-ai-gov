# Proposal: chatbot-simulator

## Problem

관리자가 RAG 파이프라인의 실제 동작을 빠르게 검증할 방법이 없다.
현재는 eval-runner를 통한 배치 평가만 가능하며, 인터랙티브하게 질문을 던지고 토큰 단위로 스트리밍되는 응답을 확인하는 수단이 없다.

## Proposed Solution

Ops 포털에 챗봇 시뮬레이터 페이지를 추가한다.
관리자가 기관/서비스를 선택하고 질문을 입력하면, 실제 RAG 파이프라인(pgvector 검색 + Ollama LLM)을 통해 토큰 단위로 스트리밍된 답변을 확인할 수 있다.

전체 파이프라인:
```
[Frontend] useChat → /api/simulator/chat (Next.js API Route)
    → POST /generate/stream (rag-orchestrator, NDJSON 스트리밍)
    → Ollama stream=True 토큰 스트리밍
[Spring Boot] POST /admin/simulator/sessions (세션 생성 전용)
```

## Out of Scope

- 시뮬레이터 대화 내역의 Spring Boot DB 영구 저장 (테스트 목적이므로 저장하지 않음)
- /record 엔드포인트 (CreateQuestionUseCase가 RAG를 재호출하는 사이드 이펙트로 인해 제외)
- 스트리밍 중 citation 정보 실시간 표시 (done 청크에서만 제공)
- 멀티턴 컨텍스트 유지 (각 질문은 독립적으로 RAG 검색)

## Success Criteria

- `/ops/simulator` 페이지에서 기관/서비스 선택 후 대화 시작 가능
- 질문 전송 시 Ollama 토큰이 실시간으로 스트리밍되어 화면에 표시됨
- Ollama 미연결 시 fallback 메시지가 정상 반환됨
- Spring Boot 테스트 50개 전부 통과
