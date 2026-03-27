# Proposal

## Change ID

`dashboard-accuracy-fix`

## Summary

- **변경 목적**: 어드민 대시보드 항목의 데이터 정확성 버그를 수정하고, 미구현 상태로 하드코딩된 항목을 제거한다.
- **변경 범위**: 백엔드 resolvedRate 계산 누락(Critical 1건) + 프론트엔드 라벨 오류·하드코딩·미구현 카드·Y축 스케일 오류·badge 누락(Warning 4건) + RAGAS 지표 누락(Minor 1건), 총 6건.
- **제외 범위**: 새 KPI 지표 추가, DB 스키마 변경, 신규 API 엔드포인트 추가.

### 수정 항목 상세

**C1 — resolvedRate 항상 null**
- `MetricsAggregationScheduler`가 `resolvedRate`를 계산·저장하지 않음.
- `SaveDailyMetricsCommand`에 `resolvedRate: BigDecimal?` 필드 추가.
- 스케줄러에서 `answered / totalQuestions` 비율로 계산 후 커맨드에 전달.
- `SaveMetricsPortAdapter`에서 엔티티에 저장.
- `DailyMetricsResponse`에는 이미 필드가 있으므로 API 계약 변경 없음.

**W1 — revisitRate 지표명 오류**
- 백엔드 쿼리가 "피드백 남긴 세션 수 / 전체 세션 수"를 계산하지만, 프론트엔드에서 "재문의율"로 표시 중.
- `frontend/src/app/client/page.tsx`의 label을 "재문의율" → "피드백 완료율"로, help 텍스트를 실제 의미에 맞게 수정.

**W2 — 파이프라인 레이턴시 하드코딩**
- `frontend/src/app/ops/page.tsx`에 Retrieval=438ms, LLM=1128ms, 후처리=114ms 고정값 사용.
- "파이프라인 레이턴시 (P95)" 섹션 전체 제거. avgResponseTimeMs는 KPI 카드로 이미 표시 중.

**W3 — 문서 건강도 미구현 카드**
- `frontend/src/app/ops/cost/page.tsx`의 "문서 건강도" 섹션 3개 카드(Stale Doc Rate, 중복문서율, Cache Hit Rate) 모두 "—" 하드코딩.
- 섹션 전체 제거.

**W4 — client 추세 차트 Y축 스케일 불일치**
- `autoResolutionRate`, `escalationRate`는 0~1 소수이나 MetricsLineChart에 raw값 전달 → Y축 0~1 표시.
- `frontend/src/app/client/page.tsx`에서 해당 필드를 ×100 변환 후 전달, 또는 MetricsLineChart 내부 scale 옵션 적용 (구현 확인 후 결정).

**W5 — answerStatus=error "Fallback" 오표시**
- `frontend/src/app/qa/page.tsx` 미응답 질문 목록에서 `"error"` 상태도 "Fallback" badge로 표시.
- Badge 조건에 `"error"` 케이스 추가 → "오류" badge 표시.

**M4 — RAGAS context_recall 미표시**
- `frontend/src/app/qa/page.tsx`, `frontend/src/app/ops/quality/page.tsx`의 ragasRows에 `context_recall` 행 없음.
- 두 파일 모두 `context_recall` 행 추가 (target: 0.75).

## Impact

- **영향 모듈 (백엔드)**: `metrics-reporting` (domain, service, adapter)
- **영향 파일 (백엔드)**:
  - `modules/metrics-reporting/.../domain/DailyMetrics.kt`
  - `apps/admin-api/.../metrics/adapter/inbound/scheduler/MetricsAggregationScheduler.kt`
  - `modules/metrics-reporting/.../adapter/outbound/persistence/SaveMetricsPortAdapter.kt`
- **영향 파일 (프론트엔드)**:
  - `frontend/src/app/ops/page.tsx`
  - `frontend/src/app/ops/cost/page.tsx`
  - `frontend/src/app/client/page.tsx`
  - `frontend/src/app/qa/page.tsx`
  - `frontend/src/app/ops/quality/page.tsx`
- **영향 API**: 없음 (API 계약 변경 없음, `DailyMetricsResponse.resolvedRate`는 기존 필드)
- **영향 테스트**: 기존 50개 통과 유지. 별도 신규 테스트는 추가하지 않음.

## Done Definition

- `resolvedRate`가 `/admin/metrics/daily` 응답에서 null이 아닌 정상값으로 반환된다.
- `MetricsAggregationScheduler.aggregate()` 수동 실행 시 `resolvedRate` 저장 확인.
- 기존 통합 테스트 50개 + ArchUnit 8개 전부 통과.
- `npm run dev` 후 ops 대시보드에서 파이프라인 레이턴시 섹션이 사라진다.
- `npm run dev` 후 ops/cost 페이지에서 문서 건강도 섹션이 사라진다.
- `npm run dev` 후 client 대시보드에서 "피드백 완료율" label이 표시되고, 추세 차트 Y축이 0~100% 스케일로 표시된다.
- `npm run dev` 후 qa 페이지에서 `error` 상태 질문에 "오류" badge가 표시된다.
- `npm run dev` 후 qa 및 ops/quality 페이지의 RAGAS 표에 `context_recall` 행이 표시된다.
