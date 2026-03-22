# Proposal: dashboard-prd-alignment

## 배경

`docs/platform-prd.md`를 단일 진실 출처로 재정비한 뒤 PRD와 현재 구현 사이의 갭을 분석한 결과, 세 가지 핵심 문제가 발견됐다.

1. **고객사 대시보드 지표 불일치** — PRD는 자동응대율·상담전환율·재문의율 등 비즈니스 성과 지표를 요구하지만 현재 `/client` 대시보드는 응답률·Fallback율 같은 시스템 운영 지표만 표시함. V023 마이그레이션으로 DB 컬럼은 준비됐으나 백엔드 DTO·프론트 타입에서 누락.

2. **실패 원인 코드(A01~A10) 설명 오류** — `/client/failure` 페이지의 설명이 임의 작성된 것으로 PRD 정의(A01=문서없음, A02=문서최신아님 등)와 불일치. 운영사/고객사 조치 주체도 미표시.

3. **툴팁 UX 문제** — `KpiCard`의 help 툴팁이 `bottom-full`로 배치돼 페이지 상단에서 뷰포트 위로 잘림. 설명도 짧고 전문적 용어 위주라 이해하기 어려움.

추가로:
- **Ops 대시보드 기관 헬스맵 부재** — PRD 핵심 화면인 기관별 정상/주의/위험 상태 표시 없음.
- **QA 대시보드 KPI 불일치** — PRD는 미응답/오답의심/저만족 3 KPIs를 요구하지만 현재는 미검수/확인이슈/해결완료/오탐지 4 KPIs.

## 변경 범위

- 백엔드: `MetricsController.kt` DTO에 V023 필드 추가
- 프론트: `types.ts` DailyMetric 타입, 3개 포털 대시보드 페이지, KpiCard 툴팁, MetricsLineChart 레이블
- DB: `V028__seed_demo_metrics_v023.sql` (데모 데이터 V023 컬럼 채우기)
- 스크립트: `scripts/reset_data.sql` (E2E 검증용 데이터 리셋)

## 영향 범위

- 기존 테스트 50개 영향 없음 (flyway.target:"26" 유지, V027·V028은 PostgreSQL 환경에서만 적용)
- 프론트엔드 API 응답 형식 확장 (하위 호환 — 기존 필드 변경 없음)
