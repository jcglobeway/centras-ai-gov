# Proposal

## Change ID

`data-consistency-fixes`

## Summary

전 포털 메뉴의 Mock 데이터를 실데이터로 교체하고, 미연동 기능 버튼을 실제 API에 연결한다.

**범례**
- ✅ 실데이터 (정상)
- ⚠️ 의미 오류 (데이터는 있으나 레이블·계산 잘못)
- 🔶 Mock 안내 있음 (허용, 안내 표시됨)
- ❌ Mock / 미구현 (수정 필요)

---

## OPS 포털

### 통합 관제 `/ops`

#### KPI 카드

| 항목 | 데이터 소스 | 현재 값 | 상태 | 수정 필요사항 |
|------|------------|---------|------|--------------|
| ANSWER RATE | `DailyMetric.resolvedRate` | 실측% | ✅ | - |
| ERROR RATE | `DailyMetric.fallbackRate` | 실측% | ✅ | - |
| E2E LATENCY | `DailyMetric.avgResponseTimeMs` | 실측ms | ✅ | - |
| KNOWLEDGE GAP | `DailyMetric.zeroResultRate` | 실측% | ✅ | - |
| COST / QUERY | `LlmMetrics.avgCostPerQuery` | 실측$ | ✅ | answers 테이블 집계 (V026) |

#### 시스템 상태 신호등

| 항목 | 데이터 소스 | 상태 | 수정 필요사항 |
|------|------------|------|--------------|
| RAG 파이프라인 | `fallbackRate` 임계값 계산 | ✅ | - |
| 지식베이스 | `zeroResultRate` 임계값 계산 | ✅ | - |
| 응답 품질 | `resolvedRate` 임계값 계산 | ✅ | - |

#### 파이프라인 레이턴시

| 항목 | 데이터 소스 | 현재 값 | 상태 | 수정 필요사항 |
|------|------------|---------|------|--------------|
| Retrieval | 하드코딩 | 142ms | ❌ | rag-orchestrator 단계별 latency 측정 후 API 제공 |
| Re-ranking | 하드코딩 | 38ms | ❌ | 동일 |
| LLM | 하드코딩 | 980ms | ❌ | 동일 |
| 후처리 | 하드코딩 | 24ms | ❌ | 동일 |

#### 기관 헬스맵

| 항목 | 데이터 소스 | 상태 | 수정 필요사항 |
|------|------------|------|--------------|
| 기관별 건강 상태 | DailyMetric 최신값 | ✅ | - |

#### 최근 질문 테이블

| 항목 | 상태 | 수정 필요사항 |
|------|------|--------------|
| 질문 5건 | ✅ | - |

---

### 서비스 통계 `/ops/statistics`

#### KPI 카드

| 항목 | 데이터 소스 | 상태 | 수정 필요사항 |
|------|------------|------|--------------|
| 총 질의 수 | `DailyMetric.totalQuestions` | ✅ | - |
| 세션 성공률 | `DailyMetric.resolvedRate` | ✅ | - |
| Knowledge Gap Rate | `DailyMetric.zeroResultRate` | ✅ | - |

#### 일별 질의 수 추이

| 항목 | 상태 | 수정 필요사항 |
|------|------|--------------|
| 14일 라인 차트 | ✅ | - |

#### 카테고리 분포

| 항목 | 데이터 소스 | 현재 값 | 상태 | 수정 필요사항 |
|------|------------|---------|------|--------------|
| 복지/급여 34% | 하드코딩 | 샘플 | 🔶 | `questions.question_category` 집계 API → `GET /admin/metrics/category-distribution` |
| 민원/신청 28% | 하드코딩 | 샘플 | 🔶 | 동일 |
| 교육/취업 18% | 하드코딩 | 샘플 | 🔶 | 동일 |
| 교통/주차 12% | 하드코딩 | 샘플 | 🔶 | 동일 |
| 기타 8% | 하드코딩 | 샘플 | 🔶 | 동일 |

---
### 품질/보안 요약 `/ops/quality-summary`

#### KPI 카드

| 항목 | 데이터 소스 | 상태 | 수정 필요사항 |
|------|------------|------|--------------|
| FAITHFULNESS | `RagasEvaluation.faithfulness` | ✅ | - |
| HALLUCINATION RATE | `1 - faithfulness` 역산 | ✅ | - |
| USER SATISFACTION | feedbacks rating≥4 / 전체 비율 | ✅ | feedbacks 없으면 N/A |

#### RAGAS 스코어카드

| 항목 | 상태 | 수정 필요사항 |
|------|------|--------------|
| 4개 지표 | ✅ | - |

#### 사용자 피드백 요약

| 항목 | 데이터 소스 | 상태 | 수정 필요사항 |
|------|------------|------|--------------|
| 👍 / 👎 카운트 | feedbacks API | ✅ | feedbacks 없으면 목업값 표시 |
| 최근 7일 추이 바 차트 | 하드코딩 | 🔶 | feedbacks를 날짜별 집계 (`createdAt` 기준 groupBy) |

---

### 품질 모니터링 `/ops/quality`

#### KPI 카드

| 항목 | 데이터 소스 | 상태 | 수정 필요사항 |
|------|------------|------|--------------|
| 응답률 | `DailyMetric.resolvedRate` | ✅ | - |
| Fallback율 | `DailyMetric.fallbackRate` | ✅ | - |
| 무응답율 | `DailyMetric.zeroResultRate` | ✅ | - |
| 평균 응답시간 | `DailyMetric.avgResponseTimeMs` | ✅ | - |

#### RAGAS 스코어카드

| 항목 | 데이터 소스 | 상태 | 수정 필요사항 |
|------|------------|------|--------------|
| 현재 배포(v2.4.0) | `RagasEvaluation` | ✅ | - |
| 이전 버전(v2.3.9) 비교 | currentRagasRows - 0.03~0.05 | ❌ | 실 이력 없음 → 비교 탭 제거 또는 ragas_evaluations 히스토리 활용 |

#### Fallback/무응답 추세

| 항목 | 상태 | 수정 필요사항 |
|------|------|--------------|
| 14일 라인 차트 | ✅ | - |

---

### 미해결 질의 `/ops/unresolved`

| 항목 | 상태 | 수정 필요사항 |
|------|------|--------------|
| 질문 목록 | ✅ | - |
| 담당자 지정 버튼 | ❌ disabled | 담당자 지정 API 없음 → 추후 구현 (현행 유지) |
| 지식베이스 추가 링크 | ✅ `/ops/upload` 링크 | - |

---

### 답변 교정 `/ops/correction`

| 항목 | 데이터 소스 | 상태 | 수정 필요사항 |
|------|------------|------|--------------|
| 부정 피드백 목록 (rating≤2) | feedbacks API | ✅ | - |
| Ground Truth 입력 폼 | 없음 | ❌ | 저장 API 미존재 → 별도 테이블/API 구현 필요 |
| 교정 이력 | 하드코딩 1건 | 🔶 | 교정 이력 저장 구조 없음 → Ground Truth 저장 API 구현 시 함께 |

---

### 이상 징후 감지 `/ops/anomaly`

#### KPI 카드

| 항목 | 데이터 소스 | 현재 값 | 상태 | 수정 필요사항 |
|------|------------|---------|------|--------------|
| QUERY DRIFT | `fallbackRate` 7일 평균 이탈률 계산 | 실산 | ✅ | - |
| RECALL DEVIATION | `zeroResultRate` 7일 평균 이탈률 계산 | 실산 | ✅ | - |
| EMBEDDING DRIFT | 하드코딩 | 0.02 (항상 ok) | ❌ | 임베딩 분포 모니터링 인프라 없음 → UI 제거 또는 "-" 표시 |
| 반복 질의 | 하드코딩 | 0건 (항상 ok) | ❌ | `questions` 중복 집계 API 구현 가능 |

#### 7일 Drift 추이

| 항목 | 상태 |
|------|------|
| fallbackRate / zeroResultRate | ✅ |

#### 임계값 초과 알림

| 항목 | 상태 |
|------|------|
| DailyMetric 기반 동적 알림 | ✅ |

#### 임계값 설정

| 항목 | 상태 | 수정 필요사항 |
|------|------|--------------|
| 입력 폼 (로컬 상태) | ❌ | 설정값 저장/로드 API 없음 → 추후 구현 |

#### 안전성 지표

| 항목 | 현재 값 | 상태 | 수정 필요사항 |
|------|---------|------|--------------|
| PII 유출 | 0 건 | ❌ | 필터링 로그 없음 → UI 제거 |
| 답변 거부율 | `-` | ⚠️ | `no_answer` 비율로 대체 가능 (`zeroResultRate` 활용) |
| OOD 탐지율 | 89.2% | ❌ | 분류기 없음 → UI 제거 |
| Adversarial 방어율 | 96.7% | ❌ | 분류기 없음 → UI 제거 |
| 독성 점수 | 0.02% | ❌ | 분류기 없음 → UI 제거 |
| Safety Score | 96.4 | ❌ | 산정 불가 → UI 제거 |

---

### 비용 & 건강도 `/ops/cost`

#### KPI 카드

| 항목 | 데이터 소스 | 현재 값 | 상태 | 수정 필요사항 |
|------|------------|---------|------|--------------|
| COST / QUERY | `LlmMetrics.avgCostPerQuery` | 실측$ | ✅ | - |
| KNOWLEDGE GAP RATE | `DailyMetric.zeroResultRate` | 실측% | ✅ | - |
| AVG INPUT TOKENS | `LlmMetrics.avgInputTokens` | 실측 | ✅ | - |
| TOKEN EFFICIENCY | avgOutputTokens / avgInputTokens | 실산 | ✅ | - |
| CACHE HIT RATE | 하드코딩 | 23.4% (항상 warn) | ❌ | 캐시 히트 추적 없음 → UI 제거 |

#### LLM 비용 요약

| 항목 | 데이터 소스 | 상태 |
|------|------------|------|
| 총 비용(USD) | `LlmMetrics.totalCostUsd` | ✅ |
| 처리 건수 | `LlmMetrics.answerCount` | ✅ |
| 평균 입력/출력 토큰 | `LlmMetrics.avgInputTokens/avgOutputTokens` | ✅ |

#### 미해결 질문 Top 5

| 항목 | 상태 |
|------|------|
| UnresolvedQuestion API | ✅ |

---

## CLIENT 포털

### 기관 대시보드 `/client`

#### KPI 카드 Row 1

| 항목 | 데이터 소스 | 현재 표시 | 상태 | 수정 필요사항 |
|------|------------|----------|------|--------------|
| 총 문의 수 | `DailyMetric.totalQuestions` | 실측 | ✅ | - |
| 자동응대 완료율 | `DailyMetric.autoResolutionRate` | 시드 데이터 | ❌ | `is_escalated=false` 비율 집계 로직 구현 |
| 상담 전환율 | `DailyMetric.escalationRate` | 시드 데이터 | ❌ | `is_escalated=true` 비율 집계 로직 구현 |

#### KPI 카드 Row 2

| 항목 | 데이터 소스 | 현재 표시 | 상태 | 수정 필요사항 |
|------|------------|----------|------|--------------|
| 평균 응답시간 | `DailyMetric.avgResponseTimeMs` | 실측 | ✅ | - |
| 피드백 완료율 | `DailyMetric.revisitRate` | 시드 데이터 | ❌⚠️ | 레이블 오류("재방문율"이 맞음) + 집계 로직 미구현 |
| 업무시간 외 응대율 | `DailyMetric.afterHoursRate` | 시드 데이터 | ❌ | 18시 이후/주말 질문 비율 집계 로직 구현 |

#### 추세 차트

| 항목 | 데이터 소스 | 상태 | 수정 필요사항 |
|------|------------|------|--------------|
| 자동응대율/상담전환율 14일 | autoResolutionRate, escalationRate | ❌ | 집계 로직 구현 후 정상화 |

---

### 민원응대 성과 `/client/performance`

| 항목 | 상태 |
|------|------|
| 질문 목록 (questionId, 내용, 카테고리, 신뢰도, 생성일) | ✅ |

---

### 실패/전환 분석 `/client/failure`

| 항목 | 상태 |
|------|------|
| failureReasonCode A01~A10 카드별 건수 | ✅ |

---

### 지식 현황 `/client/knowledge`

| 항목 | 상태 |
|------|------|
| 문서 목록 (indexStatus, documentType, lastIndexedAt) | ✅ |

---

## QA 포털

### 검수 대시보드 `/qa`

#### KPI 카드

| 항목 | 데이터 소스 | 상태 | 수정 필요사항 |
|------|------------|------|--------------|
| 미응답 질문 | `UnresolvedQuestion.total` | ✅ | - |
| 오답 의심 | `QAReview[confirmed_issue].total` | ✅ | - |
| 저만족 응답 | `DailyMetric.lowSatisfactionCount` | ❌ | feedbacks rating≤2 집계 로직 구현 |

#### 기타

| 항목 | 상태 |
|------|------|
| 미응답 질문 목록 5건 | ✅ |
| RAGAS 스코어 | ✅ |
| 최근 QA 리뷰 5건 | ✅ |

---

### 미응답/오답 관리 `/qa/unresolved`

| 항목 | 상태 | 수정 필요사항 |
|------|------|--------------|
| 질문 목록 | ✅ | - |
| 리뷰 작성 버튼 | ❌ (alert만 뜸) | 백엔드 `POST /admin/qa-reviews` 있음 → 폼 모달 구현 |

---

### 문서 관리 `/qa/documents`

| 항목 | 상태 | 수정 필요사항 |
|------|------|--------------|
| 문서 목록 | ✅ | - |
| 버전 이력 버튼 | ❌ (alert만 뜸) | 백엔드 `GET /admin/documents/{id}/versions` 있음 → 모달 구현 |

---

### 승인 워크플로우 `/qa/approvals`

| 항목 | 상태 | 수정 필요사항 |
|------|------|--------------|
| 전체 | ❌ 미구현 | Phase 3 대상 — 현행 유지 |

---

## 수정 우선순위 요약

### P1 — 백엔드 API 이미 있음, 프론트만 연결하면 됨

| 항목 | 파일 |
|------|------|
| `/qa/unresolved` 리뷰 작성 모달 | `frontend/src/app/qa/unresolved/page.tsx` |
| `/qa/documents` 버전 이력 모달 | `frontend/src/app/qa/documents/page.tsx` |

### P2 — 백엔드 집계 로직 구현 필요

| 항목 | 백엔드 작업 | 프론트 작업 |
|------|------------|-----------|
| autoResolutionRate | metrics-reporting 집계 | `/client` 정상화 |
| escalationRate | metrics-reporting 집계 | `/client` 정상화 |
| revisitRate (재방문율 레이블 수정 포함) | metrics-reporting 집계 | `/client` 레이블 수정 + 정상화 |
| afterHoursRate | metrics-reporting 집계 | `/client` 정상화 |
| lowSatisfactionCount | metrics-reporting feedbacks 집계 | `/qa` 정상화 |
| 카테고리 분포 | `GET /admin/metrics/category-distribution` 신규 | `/ops/statistics` 차트 교체 |
| feedbacks 7일 추이 | feedbacks 날짜별 집계 | `/ops/quality-summary` 차트 교체 |
| 반복 질의 | questions 중복 집계 | `/ops/anomaly` KPI 연결 |

### P3 — 인프라 구현 또는 UI 제거

| 항목 | 결정 |
|------|------|
| PIPELINE_STAGES latency | rag-orchestrator 단계별 측정 추가 |
| CACHE HIT RATE | UI 제거 |
| EMBEDDING DRIFT | UI 제거 |
| 안전성 지표 (OOD, Adversarial, Safety Score, PII, 독성) | UI 제거 |
| 이전 버전 RAGAS 비교 탭 | UI 제거 |
| 답변 거부율 | zeroResultRate로 대체 표시 |
| 임계값 저장 | 추후 구현 (현행 disabled 유지) |

## Impact

- **프론트엔드**: 8개 페이지 수정
- **백엔드**: metrics-reporting 집계 로직, category-distribution 신규 엔드포인트, rag-orchestrator latency 측정
- **테스트**: 없음 (프론트 전용, 기존 백엔드 API 활용)

## Done Definition

- 모든 ❌ 항목이 ✅ 또는 UI 제거로 해결된다
- ⚠️ 레이블 오류가 수정된다
- 새로 구현된 집계 필드가 시드 데이터 이외 실운영 데이터에서도 정상 집계된다
