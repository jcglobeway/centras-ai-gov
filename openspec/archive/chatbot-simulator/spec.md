# Spec: chatbot-simulator

## 1. rag-orchestrator: POST /generate/stream

### 요청
```
POST /generate/stream
Content-Type: application/json

{
  "question_id": "sim_q_<timestamp>",
  "question_text": "string",
  "organization_id": "string",
  "service_id": "string"
}
```

### 응답 (StreamingResponse, media_type: application/x-ndjson)

NDJSON 청크 스트림:
```
{"content": "토큰 문자열"}     // LLM 생성 중, 1개 이상
{"done": true, "answer_status": "answered"|"fallback", "citation_count": N, "response_time_ms": N, "confidence_score": F}  // 완료 신호 (마지막)
```

Given Ollama가 미연결 상태일 때:
- content 청크 1개: "[DEV MODE] Ollama not available. Stub answer for: {question_text}"
- done 청크: answer_status="fallback", citation_count=0

Given Ollama가 연결된 상태일 때:
- retrieval 동기 수행 (hybrid_search) → _log_search_result 호출 (best-effort)
- Ollama /api/chat stream=True 호출 → 토큰별 content 청크 emit
- 스트리밍 완료 후 done 청크 emit

---

## 2. Spring Boot: POST /admin/simulator/sessions

### 요청
```
POST /admin/simulator/sessions
X-Admin-Session-Id: <sessionId>
Content-Type: application/json

{
  "organizationId": "string",
  "serviceId": "string"
}
```

### 응답
```
HTTP 201 Created
{ "sessionId": "sim_sess_<8chars>" }
```

동작:
- chat_sessions 테이블에 channel='simulator'로 레코드 생성
- 인증 필요 (AdminRequestSessionResolver)
- 403: 세션 없음 / 401: 세션 만료

---

## 3. Next.js API Route: POST /api/simulator/chat

### 요청 (Vercel AI SDK useChat 형식)
```
POST /api/simulator/chat
Content-Type: application/json

{
  "messages": [{"role": "user", "content": "질문 텍스트"}],
  "organizationId": "string",
  "serviceId": "string",
  "sessionId": "string|null"
}
```

### 응답
- StreamingTextResponse (text/plain; charset=utf-8)
- NDJSON → content 토큰만 추출 → plain text stream 변환

동작:
- rag-orchestrator /generate/stream 호출
- rag-orchestrator 연결 실패 시 "RAG 오케스트레이터에 연결할 수 없습니다." 단일 메시지 반환
- done 청크는 클라이언트로 전달하지 않음 (content 토큰만 전달)

---

## 4. Frontend: /ops/simulator 페이지

컴포넌트 트리:
```
SimulatorPage
  ├── 설정 패널 (sessionId 없을 때)
  │     ├── 기관 선택 (select, GET /api/admin/organizations)
  │     ├── 서비스 선택 (select, GET /api/admin/organizations/:id/services)
  │     └── 대화 시작 버튼 → POST /api/admin/simulator/sessions
  └── 채팅 패널 (sessionId 있을 때)
        ├── 헤더: 세션ID 표시 + 대화 초기화 버튼
        ├── 메시지 목록 (user: 오른쪽, assistant: 왼쪽)
        ├── 로딩 인디케이터 (3-dot bounce)
        └── 입력 폼 (useChat handleSubmit)
```

Given 기관/서비스 미선택 시:
- 대화 시작 버튼 disabled

Given 세션 생성 성공 시:
- 채팅 패널으로 전환, 입력 가능

Given 질문 전송 시:
- useChat이 /api/simulator/chat으로 POST 전송
- 응답 토큰이 스트리밍되어 assistant 메시지 버블에 실시간 표시

Given 대화 초기화 클릭 시:
- sessionId, orgId, serviceId 초기화 → 설정 패널로 복귀

---

## 5. Sidebar 변경

OPS_NAV에 항목 추가:
```
{ href: "/ops/simulator", label: "챗봇 시뮬레이터", icon: "◷" }
```
