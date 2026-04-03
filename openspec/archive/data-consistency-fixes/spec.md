# Spec: data-consistency-fixes

## 1. 프론트엔드 타입 수정

### types.ts — QAReview

```typescript
// 변경 전
export type RootCauseCode =
  | "A01" | "A02" | "A03" | "A04" | "A05"
  | "A06" | "A07" | "A08" | "A09" | "A10";

export interface QAReview {
  ...
  reviewNote: string | null;
}

// 변경 후
export type RootCauseCode =
  | "missing_document" | "stale_document" | "bad_chunking"
  | "retrieval_failure" | "generation_error" | "policy_block"
  | "unclear_question";

export type ActionType =
  | "faq_create" | "document_fix_request" | "reindex_request"
  | "ops_issue" | "no_action";

export interface QAReview {
  ...
  actionType: ActionType | null;   // 추가
  reviewComment: string | null;    // reviewNote → reviewComment
}
```

### types.ts — DocumentVersion

```typescript
// 변경 전 (백엔드 응답과 불일치)
export interface DocumentVersion {
  versionId: string;
  documentId: string;
  versionNumber: number;
  changeNote: string | null;
  createdAt: string;
}

// 변경 후 (DocumentVersionResponse 매핑)
export interface DocumentVersion {
  id: string;            // versionId → id
  documentId: string;
  versionLabel: string;  // versionNumber → versionLabel
  contentHash: string | null;
  changeDetected: boolean;
  createdAt: string;
}
```

---

## 2. 프론트엔드 API 수정

### lib/api.ts — qaApi.createReview

```typescript
// 변경 전
createReview: (body: {
  questionId: string;
  reviewStatus: string;
  rootCauseCode?: string;
  actionType?: string;
  reviewNote?: string;      // ← 오류
}) => ...

// 변경 후
createReview: (body: {
  questionId: string;
  reviewStatus: string;
  rootCauseCode?: string;
  actionType?: string;
  reviewComment?: string;   // ← 수정
}) => ...
```

---

## 3. 페이지별 변경 스펙

### 3-1. /qa/unresolved — 리뷰 작성 모달

**파일**: `frontend/src/app/qa/unresolved/page.tsx`

**추가 상태**:
```typescript
const [modalQuestion, setModalQuestion] = useState<UnresolvedQuestion | null>(null);
const [reviewStatus, setReviewStatus]   = useState<string>("confirmed_issue");
const [rootCauseCode, setRootCauseCode] = useState<string>("");
const [actionType, setActionType]       = useState<string>("");
const [reviewComment, setReviewComment] = useState<string>("");
const [submitting, setSubmitting]       = useState(false);
```

**제출 핸들러**:
```typescript
async function handleSubmit() {
  if (!modalQuestion) return;
  setSubmitting(true);
  try {
    await qaApi.createReview({
      questionId:   modalQuestion.questionId,
      reviewStatus,
      rootCauseCode: reviewStatus === "confirmed_issue" ? rootCauseCode : undefined,
      actionType:    reviewStatus === "confirmed_issue" ? actionType
                   : reviewStatus === "false_alarm"    ? "no_action"
                   : undefined,
      reviewComment: reviewComment || undefined,
    });
    setModalQuestion(null);
    mutate();  // SWR revalidate
  } finally {
    setSubmitting(false);
  }
}
```

**유효성 검사**: `reviewStatus === "confirmed_issue"` 이면 `rootCauseCode`와 `actionType` 모두 필수.

**SWR key**: `useSWR` 결과에서 `mutate` 함수 추출하여 저장 후 목록 갱신.

---

### 3-2. /qa/documents — 버전 이력 모달

**파일**: `frontend/src/app/qa/documents/page.tsx`

**추가 상태**:
```typescript
const [versionsDocId, setVersionsDocId] = useState<string | null>(null);
const [versionsTitle, setVersionsTitle] = useState<string>("");
```

**버전 데이터 조회**:
```typescript
const { data: versionsData, isLoading: versionsLoading } =
  useSWR<{ items: DocumentVersion[]; total: number }>(
    versionsDocId ? `/api/admin/documents/${versionsDocId}/versions` : null,
    fetcher
  );
```

**버전 이력 모달 표시**:
- `versionsDocId !== null` 일 때 오버레이 렌더링
- 컬럼: 버전 레이블 | 변경 여부 | 생성일
- `changeDetected: true` → "변경 있음" (warning 색), `false` → "변경 없음"

---

### 3-3. /ops/cost — CACHE HIT RATE 제거

**파일**: `frontend/src/app/ops/cost/page.tsx`

- KPI 카드에서 `CACHE HIT RATE` KpiCard 블록 제거
- 관련 상수(`CACHE_HIT_RATE` 등) 있으면 함께 제거

---

### 3-4. /ops/anomaly — EMBEDDING DRIFT, 안전성 지표 제거

**파일**: `frontend/src/app/ops/anomaly/page.tsx`

**KPI 카드 섹션**: EMBEDDING DRIFT KpiCard 제거 (QUERY DRIFT, RECALL DEVIATION 2개만 유지)

**안전성 지표 섹션 전체 제거**:
- PII 유출 카드
- 답변 거부율 카드
- OOD 탐지율 카드
- Adversarial 방어율 카드
- 독성 점수 카드
- Safety Score 카드
- 해당 섹션 CardHeader/CardTitle 포함

---

### 3-5. /ops/quality — 이전 버전 비교 탭 제거

**파일**: `frontend/src/app/ops/quality/page.tsx`

- 탭 선택 상태(`activeTab` 등) 제거
- "이전 버전(v2.3.9)" 탭 버튼 및 탭 패널 제거
- 탭 UI 구조 제거 → 단일 RAGAS 스코어카드로 평탄화

---

### 3-6. /ops/redteam — KPI 카드 4개 제거

**파일**: `frontend/src/app/ops/redteam/page.tsx`

제거 대상 KpiCard: PII 방어율, OOD 탐지율, Adversarial 방어율, Safety Score
유지: 케이스 목록 테이블, 실행 이력 테이블 (목업 배지 있음)

---

### 3-7. /client — revisitRate 레이블 수정

**파일**: `frontend/src/app/client/page.tsx`

```tsx
// 변경 전
<KpiCard
  label="피드백 완료율"
  help="피드백을 완료한 세션 비율"
  ...
/>

// 변경 후
<KpiCard
  label="재방문율"
  help="동일 민원 건으로 재방문한 세션 비율. 10% 미만 정상."
  ...
/>
```

---

## 4. 백엔드 변경 스펙 (P1)

### 4-1. metrics-reporting — V023 필드 실집계

**대상 파일**: `modules/metrics-reporting/.../MetricsDailyAggregationService.kt` (또는 배치 서비스)

**집계 시점**: 일별 배치 (기존 집계 로직 실행 시 함께)

**SQL 예시**:

```sql
-- autoResolutionRate
SELECT
  COUNT(*) FILTER (WHERE is_escalated = false)::float / NULLIF(COUNT(*), 0)
FROM questions
WHERE organization_id = :orgId
  AND DATE(created_at) = :metricDate;

-- afterHoursRate (18시 이후 또는 주말)
SELECT
  COUNT(*) FILTER (
    WHERE EXTRACT(HOUR FROM created_at AT TIME ZONE 'Asia/Seoul') >= 18
       OR EXTRACT(DOW  FROM created_at AT TIME ZONE 'Asia/Seoul') IN (0, 6)
  )::float / NULLIF(COUNT(*), 0)
FROM questions
WHERE organization_id = :orgId AND DATE(created_at) = :metricDate;

-- lowSatisfactionCount
SELECT COUNT(*)
FROM feedbacks f
JOIN questions q ON f.question_id = q.question_id
WHERE q.organization_id = :orgId
  AND DATE(f.created_at) = :metricDate
  AND f.rating <= 2;
```

### 4-2. 신규 엔드포인트

**파일**: `apps/admin-api/.../adapter/inbound/web/MetricsController.kt`

#### GET /admin/metrics/category-distribution

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `organization_id` | 선택 | 기관 필터 |
| `from_date` | 선택 | 시작일 (YYYY-MM-DD) |
| `to_date` | 선택 | 종료일 (YYYY-MM-DD) |

응답:
```json
{
  "items": [
    { "category": "복지/급여", "count": 342, "percentage": 34.2 }
  ],
  "total": 1000
}
```

#### GET /admin/metrics/feedback-trend

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `organization_id` | 선택 | 기관 필터 |
| `days` | 선택 | 조회 일수 (기본 7) |

응답:
```json
{
  "items": [
    { "date": "2026-03-22", "thumbsUp": 18, "thumbsDown": 4 }
  ]
}
```

#### GET /admin/metrics/duplicate-questions

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `organization_id` | 선택 | 기관 필터 |
| `min_occurrences` | 선택 | 중복 임계 건수 (기본 3) |

응답:
```json
{
  "count": 42,
  "topQuestions": [
    { "questionText": "복지카드 신청 방법", "occurrences": 12 }
  ]
}
```

---

## 5. 백엔드 변경 스펙 (P1-F, P1-G)

### 5-1. GET /admin/audit-logs — 감사 로그 목록

**파일 확인**: `apps/admin-api/.../identity-access/` 또는 신규 `AuditLogController.kt`

응답 구조 (audit_logs 테이블 기반):
```json
{
  "items": [
    {
      "id": "audit_abc123",
      "adminUserId": "user_xyz",
      "actionCode": "qa.review.write",
      "resourceId": "qa_rev_001",
      "createdAt": "2026-03-30T10:00:00Z"
    }
  ],
  "total": 156
}
```

### 5-2. GET /admin/users — 관리자 사용자 목록

**파일 확인**: `apps/admin-api/.../identity-access/` 기존 사용자 관련 컨트롤러

응답 구조:
```json
{
  "items": [
    {
      "id": "user_abc",
      "email": "ops@jcg.com",
      "displayName": "운영 담당자",
      "roleCode": "ops_admin",
      "status": "active",
      "createdAt": "2026-03-01T00:00:00Z"
    }
  ],
  "total": 6
}
```

---

## 6. 검증 시나리오

### P0 검증

| 계정 | 페이지 | 시나리오 |
|------|--------|---------|
| qa@jcg.com | /qa/unresolved | 리뷰 작성 버튼 클릭 → 모달 열림 → `confirmed_issue` 선택 → rootCauseCode/actionType 필드 표시 → 저장 → 201 응답 → 목록 갱신 |
| qa@jcg.com | /qa/unresolved | `false_alarm` 선택 → rootCauseCode/actionType 숨김 → 저장 성공 |
| qa@jcg.com | /qa/documents | 버전 이력 버튼 클릭 → 모달 열림 → 버전 목록 표시 |

### P2 검증

| 페이지 | 확인 항목 |
|--------|---------|
| /ops/cost | CACHE HIT RATE KpiCard 없음 |
| /ops/anomaly | KPI 카드가 QUERY DRIFT, RECALL DEVIATION 2개만 표시됨 |
| /ops/anomaly | 안전성 지표 섹션 없음 |
| /ops/quality | 탭 없음, 단일 RAGAS 스코어카드 |
| /ops/redteam | KPI 카드 없음, 케이스 목록만 표시 |

### P3 검증

| 페이지 | 확인 항목 |
|--------|---------|
| /client | "재방문율" 레이블 표시 |
| /client | help 툴팁에 "재방문한 세션 비율" 텍스트 포함 |

### P1 검증 (배치 실행 후)

| 페이지 | 확인 항목 |
|--------|---------|
| /client | autoResolutionRate, escalationRate 실측값 표시 (시드 데이터 이상의 값) |
| /qa | lowSatisfactionCount 실측값 표시 |
| /ops/statistics | 카테고리 분포 차트에 실데이터 표시 |
| /ops/anomaly | 반복 질의 KPI에 실측 건수 표시 |

---

## 7. 제약 및 주의사항

- `revisitRate` 집계는 `chat_sessions` 테이블 기반으로 "동일 조직, 동일 기간 내 2회 이상 세션"으로 정의. 구체적 기준은 P1-A 구현 시 확정.
- 감사로그 API(P1-F), 사용자 목록 API(P1-G)는 구현 전 기존 엔드포인트 존재 여부를 먼저 확인한다.
- P2 UI 제거는 기존 하드코딩 상수(`MOCK_*`, `SAFETY_*` 등)를 파일에서 함께 제거한다.
- 반복 질의 KPI(`duplicate-questions`)는 P2에서 EMBEDDING DRIFT 제거 후 해당 자리에 배치한다 (P1-E 완료 후).
