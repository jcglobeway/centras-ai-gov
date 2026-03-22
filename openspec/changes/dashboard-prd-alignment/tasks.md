# Tasks: dashboard-prd-alignment

## Phase 1 — 공통 인프라

- [x] `MetricsController.kt` — DailyMetricsResponse에 V023 필드 추가 (autoResolutionRate, escalationRate, revisitRate, afterHoursRate, knowledgeGapCount, unansweredCount, lowSatisfactionCount)
- [x] `frontend/src/lib/types.ts` — DailyMetric 인터페이스에 동일 필드 추가
- [x] `MetricsLineChart.tsx` — autoResolutionRate, escalationRate 등 METRIC_LABELS 추가
- [x] `KpiCard.tsx` — 툴팁 방향 수정 (`bottom-full` → `top-full left-0 mt-1.5`) + 크기/스타일 개선
- [x] `V028__seed_demo_metrics_v023.sql` 생성 — org_seoul_120·org_busan_220 V023 컬럼 데모 값 UPDATE
- [x] `application-test.yml` flyway.target — "26" 유지 (V027·V028은 H2 불필요)
- [x] 백엔드 테스트 50/50 통과 확인

## Phase 2 — Ops 포털

- [x] `ops/page.tsx` — 기관 헬스맵 섹션 추가 (per-org 최신 metrics 집계 → 정상/주의/위험 상태)
- [x] `ops/page.tsx` — KPI help 텍스트 구체적으로 개선 (임계값·조치 방법 포함)

## Phase 3 — Client 포털

- [x] `client/page.tsx` — 비즈니스 KPI 중심 재편 (총 문의 수, 자동응대율, 상담전환율, 평균응답시간, 재문의율, 업무시간외 응대율)
- [x] `client/failure/page.tsx` — A01~A10 설명·조치 주체 PRD 기준 수정

## Phase 4 — QA 포털

- [x] `qa/page.tsx` — PRD 기준 3 KPIs (미응답 질문, 오답 의심, 저만족 응답)
- [x] `qa/page.tsx` — 미응답 질문 목록에 원인코드(A01~A10) 표시

## Phase 5 — E2E 데이터 파이프라인

- [x] `scripts/reset_data.sql` 생성 — questions/answers/sessions/feedbacks/qa_reviews/ragas_evaluations DELETE
- [ ] PostgreSQL 환경에서 `scripts/reset_data.sql` 실행 확인
- [ ] 공공 Q&A ZIP 질의 투입 (`eval_questions.json` → query_runner.py → RAG 파이프라인)
- [ ] RAGAS 평가 실행 (eval-runner → POST /admin/ragas-evaluations)
- [ ] 대시보드 시나리오 검증:
  - [ ] ops@jcg.com → /ops → 기관 헬스맵 표시 확인
  - [ ] ops@jcg.com → /ops/quality → RAGAS 스코어카드 실제 값 확인
  - [ ] client@jcg.com → /client → 자동응대율·상담전환율 표시 확인
  - [ ] client@jcg.com → /client/failure → A01~A10 올바른 설명 확인
  - [ ] qa@jcg.com → /qa → 미응답/오답의심/저만족 KPI 확인
