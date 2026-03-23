# Spec: dashboard-prd-alignment

## 1. 백엔드 API 변경

### GET /admin/metrics/daily

**변경 없음** — 기존 엔드포인트 그대로 사용. 응답 DTO에 필드 추가.

**응답 (DailyMetricsResponse)**

```json
{
  "items": [
    {
      "id": "dm_abc123",
      "metricDate": "2026-03-22",
      "organizationId": "org_seoul_120",
      "serviceId": "svc_welfare",
      "totalSessions": 120,
      "totalQuestions": 245,
      "resolvedRate": 92.40,
      "fallbackRate": 5.10,
      "zeroResultRate": 2.80,
      "avgResponseTimeMs": 1230,

      // 신규 (V023) — null 허용
      "autoResolutionRate": 0.6800,
      "escalationRate": 0.2100,
      "revisitRate": 0.0820,
      "afterHoursRate": 0.3400,
      "knowledgeGapCount": 5,
      "unansweredCount": 7,
      "lowSatisfactionCount": 3
    }
  ],
  "total": 14
}
```

> `autoResolutionRate` 등은 DB에 소수(0.68)로 저장. 프론트에서 `× 100`으로 백분율 변환.

**수정 파일**
- `apps/admin-api/.../metrics/adapter/inbound/web/MetricsController.kt`
  - `DailyMetricsResponse` data class에 V023 필드 추가
  - `DailyMetricsSummary.toResponse()` 매핑 추가

---

## 2. 프론트엔드 타입

### types.ts — DailyMetric

```typescript
export interface DailyMetric {
  // 기존 필드 (변경 없음)
  id: string;
  organizationId: string;
  serviceId: string;
  metricDate: string;
  totalSessions: number;
  totalQuestions: number;
  resolvedRate: number | null;
  fallbackRate: number | null;
  zeroResultRate: number | null;
  avgResponseTimeMs: number | null;

  // 신규 (V023)
  autoResolutionRate: number | null;   // DB: 소수 (0.68 = 68%)
  escalationRate: number | null;        // DB: 소수 (0.21 = 21%)
  revisitRate: number | null;           // DB: 소수 (0.08 = 8%)
  afterHoursRate: number | null;        // DB: 소수 (0.34 = 34%)
  knowledgeGapCount: number;            // 정수
  unansweredCount: number;              // 정수
  lowSatisfactionCount: number;         // 정수
}
```

---

## 3. 페이지별 스펙

### 3-1. /ops — 운영사 메인 대시보드

**KPI 섹션** (5개, 변경 없음)
- 응답률 / Fallback율 / 무응답률 / 평균 응답시간 / 전체 질문

**기관 헬스맵 섹션** (신규)
- 데이터 소스: `data.items`에서 per-org 최신 row 추출 (`orgLatestMap`)
- 상태 판정: `resolved < 80% → critical`, `fallback ≥ 15% → critical`, `resolved < 90% → warn`, `fallback ≥ 10% → warn`, 그 외 `ok`
- 표시: 기관 ID | 상태 도트 | 이슈 설명 | 상태 레이블

**help 텍스트 기준**
- 임계값·목표치·조치 방법을 포함한 2~3문장
- 예: "90% 이상이면 정상, 80% 미만이면 문서 품질 점검이 필요합니다."

---

### 3-2. /client — 고객사 메인 대시보드

**KPI Row 1** (3개)

| 레이블 | 필드 | 변환 | ok | warn | critical |
|--------|------|------|-----|------|----------|
| 총 문의 수 | `totalQuestions` | 없음 | — | — | — |
| 자동응대 완료율 | `autoResolutionRate` | ×100 | ≥70% | ≥60% | <60% |
| 상담 전환율 | `escalationRate` | ×100 | <20% | <30% | ≥30% |

**KPI Row 2** (3개)

| 레이블 | 필드 | 변환 | ok | warn | critical |
|--------|------|------|-----|------|----------|
| 평균 응답시간 | `avgResponseTimeMs` | 없음 | <1500ms | <2500ms | ≥2500ms |
| 재문의율 | `revisitRate` | ×100 | <10% | <15% | ≥15% |
| 업무시간 외 응대율 | `afterHoursRate` | ×100 | — | — | — |

**추세 차트**: `autoResolutionRate`, `escalationRate` (14일)

---

### 3-3. /client/failure — 실패/전환 분석

**실패 원인 카드 (A01~A10)**

```typescript
type FailureEntry = {
  label: string;    // "문서 없음"
  desc: string;     // "질문에 해당하는 문서가 지식베이스에 등록되지 않음"
  owner: "고객사" | "운영사" | "협의";
}
```

- 데이터 소스: `GET /admin/questions/unresolved?page_size=100`에서 `failureCode` 집계
- 카드당 표시: 코드 + 건수 Badge + 레이블 + 설명 + 조치 주체 Badge + 발생 비율 바
- 조치 주체 Badge 색: 고객사=info, 협의=warning, 운영사=neutral

---

### 3-4. /qa — QA 검수 대시보드

**KPI 3개**

| 레이블 | 데이터 소스 | ok | warn | critical |
|--------|------------|-----|------|----------|
| 미응답 질문 | `GET /admin/questions/unresolved` total | 0건 | <20건 | ≥20건 |
| 오답 의심 | QA reviews `confirmed_issue` count (5건 기준) | 0건 | <5건 | ≥5건 |
| 저만족 응답 | `daily_metrics.lowSatisfactionCount` | 0건 | <10건 | ≥10건 |

**미응답 질문 목록** (최근 5건)
- 컬럼: 질문 내용 | 원인 코드 (`A01 · 문서 없음`) | 답변 상태 Badge | 생성일

**RAGAS 스코어카드**
- `GET /admin/ragas-evaluations?page_size=1` 최신 1건
- 목표: Faithfulness 0.90 / Answer Relevance 0.85 / Context Precision 0.70

---

## 4. DB 마이그레이션

### V028__seed_demo_metrics_v023.sql (신규)

```sql
UPDATE daily_metrics_org SET
    auto_resolution_rate = 0.6800, escalation_rate = 0.2100,
    explicit_resolution_rate = 0.4200, estimated_resolution_rate = 0.6100,
    revisit_rate = 0.0820, after_hours_rate = 0.3400,
    avg_session_turn_count = 2.30,
    knowledge_gap_count = 5, unanswered_count = 7, low_satisfaction_count = 3
WHERE organization_id = 'org_seoul_120';

-- org_busan_220 동일 구조
```

**적용 환경**: PostgreSQL만 (H2 테스트는 `flyway.target: "26"` 유지)

---

## 5. E2E 검증 시나리오

### 5-1. 데이터 리셋

```sql
-- scripts/reset_data.sql 실행
DELETE FROM ragas_evaluations, feedbacks, qa_reviews,
            rag_retrieved_documents, rag_search_logs,
            answers, questions, chat_sessions;
```

### 5-2. 실제 질의 투입

- 소스: `python/eval-runner/eval_questions.json` (100건, 공공 Q&A ZIP)
- 방법: `query_runner.py` → `POST /admin/questions` (세션 생성 포함)
- RAG 파이프라인 응답: pgvector 검색 + Ollama LLM 생성

### 5-3. RAGAS 평가

- `ragas_batch.py` → `POST /admin/ragas-evaluations`
- 목표: Faithfulness > 0.90, Answer Relevance > 0.85

### 5-4. 대시보드 검증 체크리스트

| 계정 | 페이지 | 확인 항목 |
|------|--------|----------|
| ops@jcg.com | /ops | 기관 헬스맵 — 2개 기관 상태 표시 |
| ops@jcg.com | /ops | KPI help 텍스트 — 툴팁이 아래쪽으로 표시 |
| ops@jcg.com | /ops/quality | RAGAS 스코어카드 실제 값 표시 |
| client@jcg.com | /client | 자동응대율·상담전환율 수치 표시 |
| client@jcg.com | /client/failure | A01~A10 올바른 설명 + 조치 주체 Badge |
| qa@jcg.com | /qa | 미응답/오답의심/저만족 3 KPIs |
| qa@jcg.com | /qa | 미응답 질문 목록 — 원인코드 표시 |

---

## 6. 제약 및 주의사항

- `autoResolutionRate` 등은 소수(0.68) 저장 → 프론트에서 `×100` 변환 필수
- V028은 UPDATE only (INSERT 없음) → H2에서 실행해도 안전하지만, V027 PG 전용 시드와 분리를 위해 `flyway.target: "26"` 유지
- 기관 헬스맵은 현재 API 응답에 포함된 org 데이터 기반 → 기관이 많아질 경우 per-org API 분리 검토 필요 (Phase 4 이후)
- KpiCard 툴팁 `z-50` 유지 — 다른 overlay와의 stacking context 충돌 주의