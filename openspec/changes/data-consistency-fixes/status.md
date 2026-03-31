# Status

- 상태: `in_progress`
- 시작일: `2026-03-30`
- 마지막 업데이트: `2026-03-30`

## Progress

- [x] P0-A: `api.ts` reviewNote → reviewComment 수정
- [x] P0-B: `/qa/unresolved` QA 리뷰 작성 모달 구현
- [x] P0-C: `/qa/documents` 버전 이력 모달 구현
- [x] P2-B: `/ops/anomaly` EMBEDDING DRIFT KpiCard 제거
- [x] P2-C: `/ops/anomaly` 안전성 지표 섹션 제거
- [x] P2-D: `/ops/quality` 이전 버전(v2.3.9) 비교 탭 제거
- [x] P2-E: `/ops/redteam` KPI 카드 4개 제거
- [x] P3-A: `/client` revisitRate 레이블 수정 (재방문율)
- [ ] P1: 백엔드 집계 로직 (V023, 카테고리, 피드백 추이, 감사로그, 사용자)

## 제거 항목 및 사유

### EMBEDDING DRIFT (`/ops/anomaly`)
- **제거 사유**: pgvector에 임베딩 분포 추적 로직이 없음. 항상 `0.02` 고정값 표시.
  "이상 징후 감지" 화면에서 실측되지 않은 지표가 `status="ok"`로 표시되면 실제 이상 발생 시 감지하지 못한다는 오해를 유발.
- **향후 구현 가이드**: pgvector 임베딩 통계(cosine 분포 평균/분산)를 주기적으로 측정하는 별도 배치 필요.

### 안전성 지표 섹션 (`/ops/anomaly`)
제거 항목: PII 유출, 답변 거부율, OOD 탐지율, Adversarial 방어율, 독성 점수, Safety Score
- **제거 사유**: 해당 지표를 측정하기 위한 ML 분류기(classifier)가 존재하지 않음.
  OOD 탐지율 89.2%, Adversarial 96.7%, Safety Score 96.4 등은 모두 하드코딩된 수치로,
  실제 보안 상태와 무관하게 "정상(green)" 상태로 표시됨 → 보안 담당자가 허위 안도감을 가질 위험.
- **의사결정 기록**: 2026-03-30 사용자 확인. "실측 불가 항목은 제거" 방향으로 합의.
- **향후 구현 가이드**: OOD 탐지 → 별도 의도 분류 모델 필요. PII 감지 → 정규식 필터 또는 NER 모델 연동.

### 이전 버전(v2.3.9) 비교 탭 (`/ops/quality`)
- **제거 사유**: 이전 버전 RAGAS 수치가 현재 평가값에서 임의의 오프셋(-0.03~-0.05)을 뺀 값으로 계산됨.
  실제 v2.3.9 평가 기록이 DB에 존재하지 않아 버전 비교 탭 자체가 허위 데이터.
- **향후 구현 가이드**: ragas_evaluations 테이블에 `model_version` 컬럼 추가 후 버전별 비교 기능 구현 가능.

### KPI 카드 4개 (`/ops/redteam`)
제거 항목: PII 방어율(100%), OOD 탐지율(89.2%), Adversarial 방어율(96.7%), Safety Score(96.4)
- **제거 사유**: 안전성 지표와 동일. 레드팀 실행 로직 자체가 미구현(케이스 수동 관리 수준).
  실행 결과가 집계되지 않는 상태에서 KPI 수치를 표시하는 것은 무의미.
  케이스 목록(MOCK_CASES)과 실행 이력(RED_TEAM_HISTORY)은 목업 배지와 함께 유지.
- **의사결정 기록**: 2026-03-30 사용자 확인. 케이스 목록은 유지, KPI만 제거 합의.

### CACHE HIT RATE (`/ops/cost`) — 유지 결정
- **유지 사유**: 사용자가 향후 구현 의사 확인. Redis가 이미 인프라에 포함되어 있어 구현 가능성 있음.
- **변경 내용**: `status="warn"` 제거 (항상 warn 표시되던 버그 수정), help 텍스트에 "샘플 데이터" 명시.
- **향후 구현 가이드**: rag-orchestrator에 Redis 기반 Exact Match 캐시 추가. 별도 change `cache-hit-rate`로 추적.

## Risks

- V023 집계 로직은 기존 metrics-reporting 모듈의 일별 집계 시점(배치)에 의존 — 실시간 집계 아님
- 감사로그/사용자 목록 API는 구현 전 기존 엔드포인트 존재 여부 확인 필요
