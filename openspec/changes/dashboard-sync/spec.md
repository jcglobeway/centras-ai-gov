# Spec

## API 계약 변경

### GET /admin/questions

**추가 응답 필드**:
```json
{
  "items": [{
    "questionId": "question_abc123",
    "organizationId": "org_seoul_120",
    "serviceId": "svc_welfare",
    "chatSessionId": "session_xyz",
    "questionText": "...",
    "questionIntentLabel": null,
    "channel": "web",
    "questionCategory": "복지",
    "failureReasonCode": "A01",
    "isEscalated": false,
    "answerConfidence": 0.82,
    "createdAt": "2026-03-20T10:00:00Z"
  }]
}
```

### GET /admin/questions/unresolved

**응답 타입 변경** (`UnresolvedQuestionResponse`):
```json
{
  "items": [{
    "questionId": "question_abc123",
    "organizationId": "org_seoul_120",
    "questionText": "...",
    "failureReasonCode": "A04",
    "questionCategory": null,
    "isEscalated": false,
    "answerStatus": "fallback",
    "latestReviewStatus": "confirmed_issue",
    "createdAt": "2026-03-20T10:00:00Z"
  }]
}
```

### GET /admin/qa-reviews

**추가 파라미터**: `?review_status={pending|confirmed_issue|resolved|false_alarm}`

기존 `?questionId=` 파라미터는 유지.

### GET /admin/metrics/daily

**추가 응답 필드** (V023):
```json
{
  "items": [{
    "autoResolutionRate": 0.68,
    "escalationRate": 0.21,
    "revisitRate": 0.08,
    "afterHoursRate": 0.34,
    "lowSatisfactionCount": 3,
    "unansweredCount": 7,
    "knowledgeGapCount": 5
  }]
}
```

## 프론트엔드 타입 명세

### Question (types.ts)

```typescript
export interface Question {
  questionId: string;
  organizationId: string;
  serviceId: string;
  chatSessionId: string;
  questionText: string;
  questionIntentLabel: string | null;
  channel: string;
  questionCategory: string | null;
  failureReasonCode: string | null;
  isEscalated: boolean;
  answerConfidence: number | null;
  createdAt: string;
}

export interface UnresolvedQuestion extends Question {
  answerStatus: string;
  latestReviewStatus: string | null;
}
```

### DailyMetric (types.ts) - V023 추가 필드

```typescript
export interface DailyMetric {
  // 기존 필드 유지...
  autoResolutionRate: number | null;
  escalationRate: number | null;
  revisitRate: number | null;
  afterHoursRate: number | null;
  lowSatisfactionCount: number;
  unansweredCount: number;
  knowledgeGapCount: number;
}
```

## 제약사항

- `UnresolvedRow` Spring Data Projection은 H2 native query와 호환 필요
- `UnresolvedQuestionSummary`는 domain 레이어 — JPA/Spring 의존성 금지
- ArchUnit Rule 4 (Controller → persistence 직접 접근 금지) 준수
- 기존 `findUnresolvedQuestions()` 메서드 삭제 가능 (사용처 없음 확인 후)
- V023 온디맨드 집계는 현재 데이터량 기준 API 호출 시 실행 (스케줄러 불필요)
