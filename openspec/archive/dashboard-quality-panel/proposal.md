# Proposal: dashboard-quality-panel

## Problem

`/ops` 메인 대시보드에서 운영자가 품질 이상과 보안 이슈를 즉시 파악하기 어렵다.
Faithfulness, Hallucination Rate, PII 감지, 사용자 피드백 같은 핵심 지표를 보려면
`/ops/quality-summary`, `/ops/audit` 등 별도 페이지로 이동해야 한다.

## Proposed Solution

`/ops` 대시보드의 이슈 알림 로그 위에 "품질/보안 요약" 인라인 패널을 추가한다.

레이아웃:
- Row 1 (3-col): Faithfulness / Hallucination Rate / Recall@K
- Row 2 (2-col): Session Success Rate / PII 감지 건수
- Row 3 (full-width): 사용자 피드백 (비율 바 + 주간 추이)

데이터 소스:
- Faithfulness + Hallucination Rate: `GET /api/admin/ragas-evaluations?page_size=7`
- PII 감지: `GET /api/admin/metrics/pii-count`
- 피드백: `GET /api/admin/feedbacks?page_size=100` + `GET /api/admin/metrics/feedback-trend?days=7`
- Recall@K / Session Success Rate: 데이터 없음 → empty state 표시

## Out of Scope

- `/ops/quality-summary` 페이지 수정
- 새 API 엔드포인트 추가 (기존 API 사용)
- 새 npm 패키지 설치 (CSS sparkline 사용)
- 수치 하드코딩

## Success Criteria

- `/ops` 대시보드에 품질/보안 요약 패널이 표시된다
- Faithfulness, Hallucination Rate는 스파크라인과 함께 실제 RAGAS 평가 데이터로 표시된다
- Recall@K, Session Success Rate는 "eval-runner 배치 실행 후 표시" empty state를 보여준다
- PII 감지, 피드백은 실제 API 데이터로 표시되며 데이터 없으면 empty state를 보여준다
- TypeScript 컴파일 에러 없음
