# Design: data-consistency-fixes

## 설계 원칙

1. **허위 수치는 즉시 제거한다** — 분류기·인프라가 없는 지표(OOD, Adversarial, CACHE HIT 등)는 하드코딩 수치를 UI에서 제거한다. 🔶 목업 배지가 이미 있는 항목은 현행 유지.
2. **백엔드 API가 있으면 반드시 연동한다** — `alert()` 또는 `disabled` 처리된 버튼 중 API가 이미 존재하는 경우 모달/폼으로 실연동한다.
3. **레이블은 DB 필드명 의미와 일치시킨다** — `revisitRate`(재방문율)를 "피드백 완료율"로 표시하는 오류를 수정한다.
4. **새 백엔드 집계는 배치 기반이다** — `metrics-reporting`의 일별 집계 배치에서 V023 필드를 실집계하도록 추가한다.

---

## 우선순위 분류

| 우선순위 | 기준 | 예시 |
|---------|------|------|
| **P0** | 백엔드 API 이미 있음, 프론트만 연결 | QA 리뷰 모달, 문서 버전 이력 모달 |
| **P1** | 백엔드 집계 로직 신규 구현 필요 | V023 실집계, 카테고리 분포, 감사로그 연동 |
| **P2** | UI 제거 (인프라 없음, 허위 수치) | CACHE HIT RATE, EMBEDDING DRIFT, 안전성 지표 |
| **P3** | 레이블 수정 | revisitRate → "재방문율" |

---

## P0 화면 설계

### QA 리뷰 작성 모달 — `/qa/unresolved`

```
┌──────────────────────────────────────────────┐
│ QA 리뷰 작성                              ✕  │
├──────────────────────────────────────────────┤
│ 질문: "긴급복지지원 신청 방법이 어떻게..."   │
│                                              │
│ 리뷰 상태 *                                  │
│ ┌─────────────────────────────────────────┐  │
│ │ confirmed_issue ▼                       │  │
│ └─────────────────────────────────────────┘  │
│                                              │
│ 근본 원인 (confirmed_issue 시 필수)          │
│ ┌─────────────────────────────────────────┐  │
│ │ missing_document ▼                      │  │
│ └─────────────────────────────────────────┘  │
│                                              │
│ 조치 유형 (confirmed_issue 시 필수)          │
│ ┌─────────────────────────────────────────┐  │
│ │ document_fix_request ▼                  │  │
│ └─────────────────────────────────────────┘  │
│                                              │
│ 검토 의견 (선택)                             │
│ ┌─────────────────────────────────────────┐  │
│ │                                         │  │
│ └─────────────────────────────────────────┘  │
│                                              │
│                     [취소]  [저장]           │
└──────────────────────────────────────────────┘
```

**상태 기계 규칙 (모달 내 조건부 필드)**

| reviewStatus | rootCauseCode | actionType |
|-------------|---------------|------------|
| `pending` | 숨김 | 숨김 |
| `confirmed_issue` | 필수 표시 | 필수 표시 |
| `false_alarm` | 숨김 | 자동: `no_action` |

**rootCauseCode 선택지**

| 값 | 레이블 |
|----|--------|
| `missing_document` | 문서 없음 |
| `stale_document` | 문서 최신 아님 |
| `bad_chunking` | 청킹 오류 |
| `retrieval_failure` | 검색 실패 |
| `generation_error` | 생성 오류 (환각) |
| `policy_block` | 정책상 제한 |
| `unclear_question` | 질문 표현 모호 |

**actionType 선택지**

| 값 | 레이블 |
|----|--------|
| `faq_create` | FAQ 추가 |
| `document_fix_request` | 문서 수정 요청 |
| `reindex_request` | 재인덱싱 요청 |
| `ops_issue` | 운영 이슈 |
| `no_action` | 조치 없음 |

---

### 문서 버전 이력 모달 — `/qa/documents`

```
┌──────────────────────────────────────────────┐
│ 버전 이력: 긴급복지지원_안내.pdf          ✕  │
├──────────────────────────────────────────────┤
│ 버전        변경 여부  생성일                 │
│ ─────────────────────────────────────────    │
│ v20260301   변경 있음  2026-03-01            │
│ v20260215   변경 없음  2026-02-15            │
│ v20260101   초기       2026-01-01            │
└──────────────────────────────────────────────┘
```

---

## P2 제거 대상 UI 설계

### `/ops/cost` — CACHE HIT RATE 제거

변경 전: KpiCard 5개 (COST/QUERY, KNOWLEDGE GAP, AVG INPUT TOKENS, TOKEN EFFICIENCY, **CACHE HIT RATE**)
변경 후: KpiCard 4개 (CACHE HIT RATE 제거)

### `/ops/anomaly` — EMBEDDING DRIFT 및 안전성 지표 제거

**KPI 카드 섹션 변경 전 (4개):**
```
QUERY DRIFT | RECALL DEVIATION | EMBEDDING DRIFT | 반복 질의
```
**변경 후 (2개):**
```
QUERY DRIFT | RECALL DEVIATION
```
(반복 질의는 P1-E에서 집계 API 구현 후 복원)

**안전성 지표 섹션 전체 제거:**
- PII 유출, 답변 거부율, OOD 탐지율, Adversarial 방어율, 독성 점수, Safety Score

### `/ops/quality` — 이전 버전 비교 탭 제거

변경 전: 탭 2개 [현재 배포(v2.4.0)] [이전 버전(v2.3.9)]
변경 후: 단일 뷰 (탭 제거, v2.4.0 기준 표시만 유지)

### `/ops/redteam` — 하드코딩 KPI 카드 4개 제거

제거 대상: PII 방어율(100%), OOD 탐지율(89.2%), Adversarial 방어율(96.7%), Safety Score(96.4)
유지: MOCK_CASES 케이스 목록, RED_TEAM_HISTORY (목업 배지 있음)

---

## P3 레이블 설계

### `/client` — revisitRate 레이블

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| KpiCard label | "피드백 완료율" | "재방문율" |
| KpiCard help | "피드백을 완료한 세션 비율" | "동일 민원 건으로 재방문한 세션 비율. 10% 미만 정상." |

---

## P1 집계 로직 설계

### V023 필드 실집계 — metrics-reporting

| 필드 | 집계 공식 | DB 소스 |
|------|----------|---------|
| `autoResolutionRate` | `is_escalated = false` 질문 수 / 전체 질문 수 | `questions` |
| `escalationRate` | `is_escalated = true` 질문 수 / 전체 질문 수 | `questions` |
| `revisitRate` | 재방문 세션 수 / 전체 세션 수 (동일 조직, 당일 기준) | `chat_sessions` |
| `afterHoursRate` | 18시 이후 또는 주말 질문 수 / 전체 질문 수 | `questions.created_at` |
| `lowSatisfactionCount` | `feedbacks.rating <= 2` 건수 | `feedbacks` |

### 신규 엔드포인트

#### GET /admin/metrics/category-distribution

```json
{
  "items": [
    { "category": "복지/급여",  "count": 342, "percentage": 34.2 },
    { "category": "민원/신청",  "count": 280, "percentage": 28.0 },
    { "category": "교육/취업",  "count": 180, "percentage": 18.0 },
    { "category": "교통/주차",  "count": 120, "percentage": 12.0 },
    { "category": "기타",       "count":  78, "percentage":  7.8 }
  ],
  "total": 1000
}
```

- 집계 소스: `questions.question_category` (null → "기타" 포함)
- 필터: `organization_id`, `from_date`, `to_date`

#### GET /admin/metrics/feedback-trend

```json
{
  "items": [
    { "date": "2026-03-22", "thumbsUp": 18, "thumbsDown": 4 },
    { "date": "2026-03-23", "thumbsUp": 22, "thumbsDown": 3 }
  ]
}
```

- 집계 소스: `feedbacks.created_at` groupBy date, `rating >= 4` → thumbsUp, `rating <= 2` → thumbsDown
- 기간: 최근 7일 고정

#### GET /admin/metrics/duplicate-questions

```json
{
  "count": 42,
  "topQuestions": [
    { "questionText": "복지카드 신청 방법", "occurrences": 12 }
  ]
}
```

- 집계 소스: `questions.question_text` 기준 중복 건수 (threshold: 3회 이상)

---

## 데이터 흐름

```
PostgreSQL
  ├─ questions       → autoResolutionRate, escalationRate, afterHoursRate 집계
  ├─ chat_sessions   → revisitRate 집계
  ├─ feedbacks       → lowSatisfactionCount, feedback-trend 집계
  └─ questions       → category-distribution, duplicate-questions 집계

Spring Boot (MetricsController)
  ├─ GET /admin/metrics/daily             (기존, V023 필드 집계 추가)
  ├─ GET /admin/metrics/category-distribution  (신규)
  ├─ GET /admin/metrics/feedback-trend         (신규)
  └─ GET /admin/metrics/duplicate-questions    (신규)

Next.js
  ├─ /client         → autoResolutionRate × 100 등 표시
  ├─ /qa             → lowSatisfactionCount
  ├─ /ops/statistics → category-distribution 파이 차트
  ├─ /ops/quality-summary → feedback-trend 바 차트
  └─ /ops/anomaly    → duplicate-questions KPI
```