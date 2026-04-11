# Proposal

## Change ID

`reports-page-real-data`

## Summary

- `/ops/reports` 페이지의 하드코딩된 목업 데이터를 실 API 데이터로 교체한다.
- 주간/월간 집계 로직을 프론트엔드에서 구현하여 추이 차트와 기관별 성과 표를 실 데이터 기반으로 렌더링한다.
- 백엔드 변경 없음 — 필요한 엔드포인트 3개가 이미 존재한다.

## Impact

- 영향 파일: `frontend/src/app/ops/reports/page.tsx` (전체 교체)
- 영향 API: `GET /admin/metrics/daily`, `GET /admin/ragas-evaluations/summary`, `GET /admin/metrics/feedback-trend`
- 영향 테스트: 없음 (프론트엔드 UI 변경)

## Done Definition

- 주간/월간 탭 전환 시 실 데이터 기반 차트 및 표가 렌더링됨
- KPI 카드 4개 (총 질의 수, 세션 성공률, Knowledge Gap율, Faithfulness) 실 데이터 표시
- 기관별 성과 표: organizationId 기준 집계 데이터 표시
- RAGAS 요약 ScoreBar 2개 표시
- 피드백 트렌드 30일 BarChart 표시
- "목업 데이터" 배지 제거
