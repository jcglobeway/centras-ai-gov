# Tasks

- [x] 요구 범위 재확인 (기존 RAGAS 4지표 구조 분석)
- [x] DB: V042 마이그레이션 생성 (`citation_coverage`, `citation_correctness` 컬럼 추가)
- [x] Kotlin: `RagasEvaluationSummary` 도메인 필드 2개 추가
- [x] Kotlin: `RecordRagasEvaluationCommand` 필드 2개 추가
- [x] Kotlin: `RagasEvaluationEntity` + 매퍼 확장
- [x] Kotlin: `RagasEvaluationService` 필드 전달
- [x] Kotlin: `RagasEvaluationPeriodSummary` aggregate 필드 2개 추가
- [x] Kotlin: `LoadRagasEvaluationSummaryPortAdapter` SQL AVG 집계 확장
- [x] Kotlin: `RagasEvaluationController` Request/Response/PeriodResponse 확장
- [x] 테스트: `AdminApiApplication` ragconfig 패키지 스캔 추가 (기존 버그 수정)
- [x] eval-runner: `_compute_citation_coverage()` 구현
- [x] eval-runner: `_compute_citation_correctness()` 구현
- [x] eval-runner: `_compute_metrics()` 반환 dict 확장
- [x] Frontend: `types.ts` RagasEvaluation + RagasEvaluationPeriodSummary 확장
- [x] Frontend: `/ops/quality/page.tsx` 스코어카드 + targetRows 확장
- [x] Frontend: `/ops/quality-summary/page.tsx` 레이더 6각형 + deltaRows 확장
- [ ] 커밋
