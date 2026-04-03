# Proposal

## Change ID

`session-based-chat-history`

## Summary

### 변경 목적

현재 `/ops/chat-history` 페이지는 질문 단위 flat list를 보여주고 있어, 한 시민이 나눈 대화의 흐름을 파악하기 어렵다. Langfuse 스타일의 3-레벨 세션 중심 UI로 개편한다.

**목표 UX:**
1. **세션 목록** — chat_sessions 기준 행 목록 (시작 시각, 기관, 채널, 질문 수, 종료 유형)
2. **세션 상세** — 특정 세션의 모든 Q&A를 시간순 대화 스레드로 표시
3. **질문 상세 패널** — 개별 질문 클릭 시 RAGAS, 검색 컨텍스트, QA 검수 등 상세 슬라이드 패널 (기존 DetailPanel 재사용)

### 변경 범위

**백엔드 (chat-runtime 모듈 + admin-api)**
- `GET /admin/chat-sessions` — 세션 목록 (org/date 필터)
- `GET /admin/chat-sessions/{sessionId}/questions` — 세션별 질문 목록 (시간순 오름차순)

**프론트엔드 (ops portal)**
- `/ops/chat-history` → 세션 목록 페이지
- `/ops/chat-history/[sessionId]` → 세션 상세 페이지 (신규)

### 제외 범위

- 세션 생성/삭제 API 변경 없음
- 기존 `GET /admin/questions` API 변경 없음 (하위 호환 유지)
- 기존 ArchUnit 8개 규칙 위반 없음

---

## Impact

### 영향 모듈

| 모듈 | 변경 |
|------|------|
| `modules/chat-runtime` | 신규 파일 6개 추가, 기존 파일 3개 수정 |
| `apps/admin-api` | 신규 Controller 1개, Config 2개 수정 |
| `frontend/src/app/ops/chat-history` | 기존 `page.tsx` 수정, 신규 `[sessionId]/page.tsx` 추가 |
| `frontend/src/lib/types.ts` | `ChatSession` 인터페이스 추가 |

### 영향 API

| 엔드포인트 | 변경 유형 |
|-----------|----------|
| `GET /admin/chat-sessions` | 신규 |
| `GET /admin/chat-sessions/{sessionId}/questions` | 신규 |
| `GET /admin/questions` | 변경 없음 |

### 영향 테스트

- 기존 50개 통합 테스트: 변경 없음 (API 추가만이므로 기존 테스트 영향 없음)
- 신규 테스트 추가 범위 외 (proposal 승인 후 별도 논의)

---

## Done Definition

- [ ] `GET /admin/chat-sessions` 호출 시 기관/날짜 필터된 세션 목록 반환
- [ ] `GET /admin/chat-sessions/{sessionId}/questions` 호출 시 해당 세션의 질문+답변 시간순 반환
- [ ] `/ops/chat-history` — 세션 목록 테이블 표시 (started_at, 기관, 채널, 질문수, 종료유형)
- [ ] `/ops/chat-history/[sessionId]` — 대화 스레드 UI (질문 파란색 버블, 답변 회색 버블)
- [ ] 질문 클릭 시 기존 DetailPanel (슬라이드 패널) 표시
- [ ] 세션 목록 → 세션 상세 간 breadcrumb 네비게이션

---

## 설계 상세

### 백엔드 신규 파일

```
modules/chat-runtime/src/main/kotlin/com/publicplatform/ragops/chatruntime/
  application/port/out/LoadChatSessionPort.kt          # 신규
  application/port/in/ListChatSessionsUseCase.kt       # 신규
  application/service/ListChatSessionsService.kt       # 신규
  adapter/outbound/persistence/LoadChatSessionPortAdapter.kt  # 신규
```

### 백엔드 수정 파일

```
  adapter/outbound/persistence/JpaChatSessionRepository.kt
    + findByOrganizationIdInOrderByStartedAtDesc(...)
    + findAllByOrderByStartedAtDesc()

  adapter/outbound/persistence/JpaQuestionRepository.kt
    + findAllWithAnswersBySessionId(@Param("sessionId") sessionId: String)

  application/port/out/LoadQuestionPort.kt
    + listQuestionsWithAnswersBySession(chatSessionId: String)

  adapter/outbound/persistence/LoadQuestionPortAdapter.kt
    + listQuestionsWithAnswersBySession(...) 구현

  application/port/in/ListQuestionsUseCase.kt
    + listBySession(chatSessionId: String)

  application/service/ListQuestionsService.kt
    + listBySession(...) 구현
```

### 백엔드 신규 Controller

```
apps/admin-api/src/main/kotlin/.../chatruntime/adapter/inbound/web/ChatSessionController.kt
  GET /admin/chat-sessions?organization_id=&from_date=&to_date=
  GET /admin/chat-sessions/{sessionId}/questions
```

### API 응답 스펙

**GET /admin/chat-sessions**
```json
{
  "items": [
    {
      "sessionId": "chat_session_001",
      "organizationId": "org_local_gov",
      "serviceId": "svc_welfare",
      "channel": "web",
      "startedAt": "2026-03-15T09:00:00Z",
      "endedAt": null,
      "sessionEndType": null,
      "totalQuestionCount": 3
    }
  ],
  "total": 1
}
```

**GET /admin/chat-sessions/{sessionId}/questions**
```json
{
  "items": [ /* QuestionResponse 배열, created_at ASC */ ],
  "total": 3
}
```

### 프론트엔드 라우팅

```
/ops/chat-history                    → page.tsx (세션 목록)
/ops/chat-history/[sessionId]        → [sessionId]/page.tsx (세션 상세)
```

### 세션 목록 테이블 컬럼

| 시작 시각 | 기관 | 채널 | 질문 수 | 종료 유형 |

### 세션 상세 UI 구조

```
← 대화 이력  /  세션 ID (breadcrumb)
────────────────────────────────
[세션 메타: 시작/종료, 채널, 기관]
────────────────────────────────
Q: 질문 텍스트                     [오른쪽 파란 버블]
A: 답변 텍스트 (truncate)          [왼쪽 회색 버블]  + 상태 badge
→ 클릭 시 DetailPanel 슬라이드
```
