# Tasks

## 사전 확인

- [x] 요구 범위 재확인 (proposal.md 검토)
- [x] 관련 코드 확인: `DailyMetrics.kt`, `MetricsAggregationScheduler.kt`, `SaveMetricsPortAdapter.kt`
- [x] 관련 코드 확인: `MetricsLineChart` 컴포넌트 구현 방식 (Y축 scale 처리 위치 결정)

## C1 — resolvedRate 항상 null (백엔드)

- [x] `SaveDailyMetricsCommand`에 `resolvedRate: BigDecimal?` 필드 추가
- [x] `MetricsAggregationScheduler`에서 `rate2(answered, totalQuestions)`로 resolvedRate 계산 후 커맨드에 전달
- [x] `SaveMetricsPortAdapter`에서 `resolvedRate` 엔티티 저장 로직 추가

## W1 — revisitRate 지표명 오류 (프론트엔드)

- [x] `frontend/src/app/client/page.tsx`: revisitRate KPI 카드 label "재문의율" → "피드백 완료율" 수정
- [x] `frontend/src/app/client/page.tsx`: revisitRate help 텍스트를 "피드백을 남긴 세션 비율"로 수정

## W2 — 파이프라인 레이턴시 하드코딩 (프론트엔드)

- [x] `frontend/src/app/ops/page.tsx`: "파이프라인 레이턴시 (P95)" 섹션 전체 제거

## W3 — 문서 건강도 미구현 카드 (프론트엔드)

- [x] `frontend/src/app/ops/cost/page.tsx`: "문서 건강도" 섹션 전체 제거

## W4 — client 추세 차트 Y축 스케일 불일치 (프론트엔드)

- [x] `MetricsLineChart` 구현 확인: formatter/scale prop 지원 여부 파악
- [x] `frontend/src/app/client/page.tsx`: autoResolutionRate, escalationRate 데이터를 ×100 변환 후 MetricsLineChart에 전달 (또는 내부 scale 옵션 적용)

## W5 — answerStatus=error "Fallback" 오표시 (프론트엔드)

- [x] `frontend/src/app/qa/page.tsx`: 미응답 질문 목록 Badge 조건에 `"error"` 케이스 추가 → "오류" badge 표시

## M4 — RAGAS context_recall 미표시 (프론트엔드)

- [x] `frontend/src/app/qa/page.tsx`: ragasRows에 `context_recall` 행 추가 (target: 0.75)
- [x] `frontend/src/app/ops/quality/page.tsx`: ragasRows에 `context_recall` 행 추가 (target: 0.75)

## 검증

- [x] 백엔드 통합 테스트 50개 + ArchUnit 8개 전부 통과 확인
- [ ] `MetricsAggregationScheduler.aggregate()` 수동 실행 후 `/admin/metrics/daily` 응답에서 `resolvedRate` null 아님 확인
- [ ] `npm run dev` 후 ops 대시보드 파이프라인 레이턴시 섹션 제거 확인
- [ ] `npm run dev` 후 ops/cost 페이지 문서 건강도 섹션 제거 확인
- [ ] `npm run dev` 후 client 대시보드 "피드백 완료율" label 및 추세 차트 Y축 스케일 확인
- [ ] `npm run dev` 후 qa 페이지 "오류" badge 및 context_recall 행 확인
- [ ] `npm run dev` 후 ops/quality 페이지 context_recall 행 확인

## 마무리

- [ ] 커밋 (한국어 커밋 메시지)
- [ ] status.md 업데이트
