# Tasks

## 사전 정리

- [x] 기존 `PatchRagasContextMetrics*` 파일 4개 내용 교체 (클래스명·파일명 포함)
  - `application/port/out/PatchRagasContextMetricsPort.kt` → `PatchRagasEvaluationPort`
  - `application/port/in/PatchRagasContextMetricsUseCase.kt` → `PatchRagasEvaluationUseCase` + `PatchRagasEvaluationCommand`
  - `application/service/PatchRagasContextMetricsService.kt` → `PatchRagasEvaluationService`
  - `adapter/outbound/persistence/PatchRagasContextMetricsPortAdapter.kt` → `PatchRagasEvaluationPortAdapter`
- [x] `RagasEvaluationController.kt` 미완성 import 정리

## Kotlin — port/out

- [x] `PatchRagasEvaluationPort` 인터페이스 작성
  - `patch(questionId: String, metrics: PatchRagasEvaluationCommand): Boolean`
  - 행 없으면 `false` 반환

## Kotlin — port/in

- [x] `PatchRagasEvaluationCommand` data class 작성
  - 6지표 필드 모두 nullable (`Double?`)
- [x] `PatchRagasEvaluationUseCase` 인터페이스 작성
  - `patch(questionId: String, command: PatchRagasEvaluationCommand): Boolean`

## Kotlin — service

- [x] `PatchRagasEvaluationService` 구현체 작성
  - `PatchRagasEvaluationPort` 주입, 행 없으면 예외 처리

## Kotlin — adapter/outbound/persistence

- [x] `PatchRagasEvaluationPortAdapter` 작성 (`open class`, JdbcTemplate 사용)
  - COALESCE SQL: `UPDATE ragas_evaluations SET col = COALESCE(?, col) WHERE question_id = ?`
  - 6지표 필드 각각 COALESCE 적용
  - UPDATE rows = 0 이면 `false` 반환

## Kotlin — adapter/inbound/web

- [x] `RagasEvaluationController` 에 `GET /admin/ragas-evaluations/by-question/{questionId}` 추가
  - 기존 조회 포트 재사용 또는 신규 포트 추가
  - 없으면 404 반환
- [x] `RagasEvaluationController` 에 `PATCH /admin/ragas-evaluations/by-question/{questionId}` 추가
  - `PatchRagasEvaluationUseCase` 호출
  - 없으면 404, 성공 시 200

## Kotlin — Bean 등록

- [x] `RepositoryConfiguration.kt` 에 `PatchRagasEvaluationPortAdapter` @Bean 등록
- [x] `ServiceConfiguration.kt` 에 `PatchRagasEvaluationService` @Bean 등록

## Kotlin — port/out (GET by-question)

- [x] `LoadRagasEvaluationByQuestionPort` 인터페이스 작성 (없으면 신규)
  - 기존 `ListRagasEvaluationsUseCase`(questionId 필터 + pageSize=1) 재사용으로 신규 포트 불필요
- [x] Bean 등록 (기존 bean 재사용)

## Kotlin — 테스트

- [x] `RagasEvaluationApiTest` 에 케이스 추가
  - `GET by-question/{id}` — 존재하는 question_id → 200
  - `GET by-question/{id}` — 없는 question_id → 404
  - `PATCH by-question/{id}` — null 필드 채우기 → 200, 기존 값 불변 확인
  - `PATCH by-question/{id}` — 없는 question_id → 404
- [x] 기존 3개 테스트 통과 확인
- [x] ArchUnit 8개 규칙 위반 없음 확인
- [x] 전체 테스트 실행: 54개 통과 (0 failures)

## Python — eval-runner

- [x] `ragas_batch.py` — `AdminApiClient.get_evaluation(question_id)` 추가
  - `GET /admin/ragas-evaluations/by-question/{question_id}`
  - 404 → None 반환, 200 → dict 반환
- [x] `ragas_batch.py` — `AdminApiClient.patch_evaluation(question_id, metrics)` 추가
  - `PATCH /admin/ragas-evaluations/by-question/{question_id}`
- [x] `ragas_batch.py` — `_compute_missing_metrics(existing, dataset)` 헬퍼 추가
  - `existing` dict에서 None인 필드만 계산 대상으로 선별
  - 계산이 필요 없는 지표는 RAGAS 호출 생략
- [x] `ragas_batch.py` — `evaluate_batch()` 로직 변경
  - 질문별 루프에서 `get_evaluation()` 호출
  - 행 없음: 6지표 전체 계산 → `post_evaluation()` (기존)
  - 행 있음: `_compute_missing_metrics()` → `patch_evaluation()`

## 최종 확인

- [ ] eval-runner 수동 실행 — 동일 question_id 중복 행 미발생 확인
- [ ] realtime-eval이 저장한 값이 batch 후에도 유지됨 확인
- [ ] 커밋: `기능: eval-batch-restore — ragas batch PATCH 전략으로 중복 행 방지`
