# Design

## 발견된 갭 분석

### CRITICAL (UI 기능 파손)

| 갭 | 영향 페이지 | 원인 |
|----|------------|------|
| `answerStatus` 미반환 | ops/page.tsx, qa/page.tsx | QuestionResponse가 answers 테이블 JOIN 없음 |
| `failureReasonCode` 미반환 | client/failure/page.tsx (A01~A10 0건), qa/page.tsx | QuestionSummary에 있으나 QuestionResponse에 미포함 |
| `latestReviewStatus` 미반환 | qa/unresolved/page.tsx (모두 "없음") | /admin/questions/unresolved가 리뷰 상태 미반환 |

### MODERATE

| 갭 | 영향 페이지 | 원인 |
|----|------------|------|
| `confirmedCount` 부정확 | qa/page.tsx KPI | page_size=5 결과만 filter → 최대 5 |
| `client/performance` 내용 불일치 | client/performance | 제목 "민원응대 성과"인데 미결질문 목록 표시 |
| V023 메트릭 null | client/page.tsx | escalation_rate, revisit_rate 등 계산 로직 없음 |

### MINOR (필드명 불일치)

| 프론트 | 백엔드 |
|--------|--------|
| `sessionId` | `chatSessionId` |
| `categoryL1`/`categoryL2` | `questionCategory` |
| `failureCode` | `failureReasonCode` |
| `wasTransferred` | `isEscalated` |

---

## Phase 1 — QuestionResponse 확장

**파일**: `apps/admin-api/src/main/kotlin/.../chatruntime/adapter/inbound/web/QuestionController.kt`

`QuestionSummary`에 이미 있는 필드를 `QuestionResponse`에 추가:
```kotlin
data class QuestionResponse(
    // 기존 8개 필드...
    val failureReasonCode: String?,    // FailureReasonCode?.code
    val questionCategory: String?,
    val isEscalated: Boolean,
    val answerConfidence: BigDecimal?,
)
```

`toResponse()` 매핑에 4개 필드 추가.

---

## Phase 2 — 미결질문 answerStatus + latestReviewStatus 추가

**현재**: `findUnresolvedQuestions()` native SQL이 `List<QuestionEntity>` 반환 → answerStatus/latestReviewStatus 없음

**접근법**: Spring Data Projection 인터페이스로 native query 확장

### Step 2-1: `UnresolvedRow` projection + 새 쿼리

**파일**: `modules/chat-runtime/src/main/kotlin/.../adapter/outbound/persistence/JpaQuestionRepository.kt`

```kotlin
interface UnresolvedRow {
    val id: String
    val answerStatus: String?
    val latestReviewStatus: String?
}

@Query(value = """
    SELECT DISTINCT q.id,
           a.answer_status AS answerStatus,
           (SELECT qr2.review_status FROM qa_reviews qr2
            WHERE qr2.question_id = q.id
            ORDER BY qr2.reviewed_at DESC LIMIT 1) AS latestReviewStatus
    FROM questions q
    LEFT JOIN answers a ON q.id = a.question_id
    LEFT JOIN qa_reviews qr ON q.id = qr.question_id
    WHERE a.answer_status IN ('fallback', 'no_answer', 'error')
       OR (qr.review_status = 'confirmed_issue'
           AND qr.reviewed_at = (
               SELECT MAX(qr2.reviewed_at) FROM qa_reviews qr2
               WHERE qr2.question_id = q.id))
    ORDER BY q.id
""", nativeQuery = true)
fun findUnresolvedWithStatus(): List<UnresolvedRow>
```

### Step 2-2: `UnresolvedQuestionSummary` 도메인 추가

**파일**: `modules/chat-runtime/src/main/kotlin/.../domain/QuestionSummary.kt`

```kotlin
data class UnresolvedQuestionSummary(
    val question: QuestionSummary,
    val answerStatus: String?,
    val latestReviewStatus: String?,
)
```

### Step 2-3 ~ 2-5: 포트/서비스/어댑터/컨트롤러 업데이트

- `LoadQuestionPort.listUnresolvedQuestions()` → `List<UnresolvedQuestionSummary>` 반환
- `ListQuestionsUseCase.listUnresolved()` 반환 타입 변경
- `ListQuestionsService` 구현 교체 (findUnresolvedWithStatus 사용)
- `UnresolvedQuestionResponse` DTO 신규:

```kotlin
data class UnresolvedQuestionResponse(
    val questionId: String,
    val organizationId: String,
    val questionText: String,
    val failureReasonCode: String?,
    val questionCategory: String?,
    val isEscalated: Boolean,
    val answerStatus: String?,
    val latestReviewStatus: String?,
    val createdAt: Instant,
)
```

---

## Phase 3 — 프론트엔드 타입 동기화

**파일**: `frontend/src/lib/types.ts`

```typescript
export interface Question {
  questionId: string;
  organizationId: string;
  serviceId: string;
  chatSessionId: string;           // sessionId 제거
  questionText: string;
  questionIntentLabel: string | null;
  channel: string;
  questionCategory: string | null; // categoryL1/L2 제거
  failureReasonCode: string | null; // failureCode 제거
  isEscalated: boolean;             // wasTransferred 제거
  answerConfidence: number | null;
  createdAt: string;
}

export interface UnresolvedQuestion extends Question {
  answerStatus: string;
  latestReviewStatus: string | null;
}
```

---

## Phase 4 — 프론트엔드 페이지 필드명 수정

| 파일 | 수정 내용 |
|------|----------|
| `client/failure/page.tsx` | `q.failureCode` → `q.failureReasonCode` |
| `qa/page.tsx` | `q.failureCode` → `q.failureReasonCode` |
| `qa/unresolved/page.tsx` | `UnresolvedQuestion` 타입 사용 (자동 해결) |
| `ops/page.tsx` | answerStatus 참조 제거 (Question 타입에 없음) |

---

## Phase 5 — QA Review review_status 필터 + confirmedCount 수정

### Step 5-1~3: 백엔드

`JpaQAReviewRepository.findByReviewStatus(status: String)` 추가 (Spring Data 자동 구현)

`LoadQAReviewPort` + `ListQAReviewsUseCase` + `ListQAReviewsService` 에 `listByStatus()` 추가

`QAReviewController.listQAReviews()` 에 `?review_status=` 파라미터 추가

### Step 5-4: 프론트엔드

```typescript
// qa/page.tsx
const { data: confirmedData } = useSWR<PagedResponse<QAReview>>(
  `/api/admin/qa-reviews?review_status=confirmed_issue&page_size=1`,
  fetcher
);
const confirmedCount = confirmedData?.total ?? null;
```

---

## Phase 6 — V023 온디맨드 집계

DB에 V023 컬럼 존재하나 계산 로직 없음. API 호출 시 raw tables에서 집계해 null 대체.

### 계산 공식

| 지표 | 공식 |
|------|------|
| `escalation_rate` | COUNT(is_escalated=true) / COUNT(*) |
| `auto_resolution_rate` | COUNT(answer_status='answered' AND is_escalated=false) / COUNT(*) |
| `after_hours_rate` | COUNT(HOUR(created_at) < 9 OR >= 18) / COUNT(*) |
| `revisit_rate` | COUNT(sessions where total_question_count > 1) / COUNT(sessions) |
| `unanswered_count` | COUNT(answer_status IN ('no_answer','fallback','error')) |
| `low_satisfaction_count` | COUNT(feedbacks.satisfaction_score <= 2) |
| `knowledge_gap_count` | COUNT(failure_reason_code IN ('A01','A02','A04')) |

### 구현

**파일**: `modules/metrics-reporting/src/main/kotlin/.../adapter/outbound/persistence/LoadDailyMetricsPortAdapter.kt`

`DailyMetricsSummary`의 V023 필드가 null이면 `aggregateFromRawData()` native SQL로 보완.

`DailyMetricsSummary` + `DailyMetricsResponse` 에 7개 필드 추가.

---

## Phase 7 — client/performance/page.tsx 교체

**현재**: 미결질문 목록 (`/admin/questions/unresolved`) — 제목과 불일치
**변경**: 전체 질문 목록 (`/admin/questions`) + answerStatus Badge 표시, 제목 "최근 응대 현황"으로 변경

---

## 수정 파일 목록

### 백엔드 (Kotlin)
| 파일 | 변경 |
|------|------|
| `modules/chat-runtime/.../domain/QuestionSummary.kt` | UnresolvedQuestionSummary 추가 |
| `modules/chat-runtime/.../port/out/LoadQuestionPort.kt` | listUnresolvedQuestions 반환 타입 변경 |
| `modules/chat-runtime/.../port/in/ListQuestionsUseCase.kt` | listUnresolved 반환 타입 변경 |
| `modules/chat-runtime/.../service/ListQuestionsService.kt` | listUnresolved 구현 교체 |
| `modules/chat-runtime/.../persistence/JpaQuestionRepository.kt` | UnresolvedRow projection + findUnresolvedWithStatus() |
| `modules/chat-runtime/.../persistence/LoadQuestionPortAdapter.kt` | listUnresolvedQuestions 구현 교체 |
| `apps/admin-api/.../chatruntime/.../QuestionController.kt` | QuestionResponse 필드 추가, UnresolvedQuestionResponse 신규 |
| `modules/qa-review/.../persistence/JpaQAReviewRepository.kt` | findByReviewStatus() 추가 |
| `modules/qa-review/.../port/out/LoadQAReviewPort.kt` | listByStatus() 추가 |
| `modules/qa-review/.../port/in/ListQAReviewsUseCase.kt` | listByStatus() 추가 |
| `modules/qa-review/.../service/ListQAReviewsService.kt` | listByStatus() 구현 |
| `apps/admin-api/.../qareview/.../QAReviewController.kt` | review_status 파라미터 추가 |
| `modules/metrics-reporting/.../domain/DailyMetricsSummary.kt` | V023 7개 필드 추가 |
| `modules/metrics-reporting/.../persistence/LoadDailyMetricsPortAdapter.kt` | aggregateFromRawData() 추가 |
| `apps/admin-api/.../metrics/.../MetricsController.kt` | DailyMetricsResponse V023 필드 추가 |

### 프론트엔드 (TypeScript)
| 파일 | 변경 |
|------|------|
| `frontend/src/lib/types.ts` | Question/UnresolvedQuestion/DailyMetric 타입 동기화 |
| `frontend/src/app/ops/page.tsx` | answerStatus 참조 제거 |
| `frontend/src/app/client/page.tsx` | V023 필드 사용 (autoResolutionRate 등) |
| `frontend/src/app/client/failure/page.tsx` | failureCode → failureReasonCode |
| `frontend/src/app/client/performance/page.tsx` | 최근 응대 현황으로 교체 |
| `frontend/src/app/qa/page.tsx` | failureCode 수정, confirmedCount total 기반 |
| `frontend/src/app/qa/unresolved/page.tsx` | UnresolvedQuestion 타입 사용 |
