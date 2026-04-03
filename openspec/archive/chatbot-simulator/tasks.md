# Tasks: chatbot-simulator

## P1 — rag-orchestrator: /generate/stream 추가
- [x] app.py에 POST /generate/stream 엔드포인트 추가
- [x] fallback 스트림 (Ollama 미연결 시) 구현
- [x] hybrid_search + _log_search_result 재사용 (기존 /generate와 동일 로직)
- [x] httpx.stream으로 Ollama /api/chat stream=True 호출
- [x] content 청크 / done 청크 NDJSON 포맷으로 emit

## P2 — Spring Boot: CreateChatSessionPort + SimulatorController
- [x] CreateChatSessionPort 인터페이스 생성 (chat-runtime/application/port/out/)
- [x] CreateChatSessionPortAdapter 구현 (chat-runtime/adapter/outbound/persistence/)
- [x] RepositoryConfiguration.kt에 createChatSessionPort 빈 등록
- [x] SimulatorController 생성 (adminapi/chatruntime/adapter/inbound/web/)
  - POST /admin/simulator/sessions 엔드포인트
- [x] Spring Boot 테스트 전체 통과 확인 (BUILD SUCCESSFUL)

## P3 — Next.js API Route
- [x] ai 패키지 설치 (npm install ai)
- [x] @ai-sdk/react 설치 (npm install @ai-sdk/react --legacy-peer-deps)
- [x] frontend/src/app/api/simulator/chat/route.ts 생성
- [x] .env.local에 RAG_ORCHESTRATOR_URL 추가

## P4 — 시뮬레이터 페이지
- [x] frontend/src/app/ops/simulator/page.tsx 생성
- [x] Sidebar.tsx OPS_NAV에 챗봇 시뮬레이터 항목 추가

## P5 — 검증 및 아카이브
- [x] ./gradlew :apps:admin-api:test 전체 통과 (BUILD SUCCESSFUL in 17s)
- [x] npm run build 통과 확인 (22 pages, /ops/simulator + /api/simulator/chat 포함)
- [x] status.md 완료로 업데이트

## P6 — 메타데이터 패널 (Retrieved Chunks + 메트릭 표시)

### P6-A. rag-orchestrator: done 패킷에 retrieved_chunks 추가
- [ ] `python/rag-orchestrator/src/rag_orchestrator/app.py`
  - `token_stream()` done yield에 `retrieved_chunks` 배열 추가
    - 각 항목: `{ filename, preview (chunk_text[:200]), score (1 - distance) }`
  - fallback_stream done 패킷에 `"retrieved_chunks": []` 추가

### P6-B. Next.js API Route: NDJSON 그대로 전달
- [ ] `frontend/src/app/api/simulator/chat/route.ts`
  - TransformStream 제거
  - rag-orchestrator body를 그대로 반환 (`Content-Type: application/x-ndjson`)

### P6-C. 시뮬레이터 페이지: 메타데이터 패널 UI
- [ ] `frontend/src/app/ops/simulator/page.tsx`
  - `Metadata` 인터페이스 추가 (answerStatus, responseTimeMs, citationCount, confidenceScore, retrievedChunks)
  - reader 루프에서 `content` / `done` NDJSON 라인 파싱
  - `done` 패킷 수신 시 `metadata` state 업데이트
  - 우측 패널 (w-72): 메트릭 4종 + 참조 문서 목록 (파일명, 미리보기, 유사도 점수)
  - `resetSession` 시 `setMetadata(null)` 추가
