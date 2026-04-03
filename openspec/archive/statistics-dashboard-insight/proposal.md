# Proposal: statistics-dashboard-insight

## Problem

현재 `/ops/statistics` 페이지는 KPI 수치와 기본 차트만 제공한다.
관리자가 지표를 보고 "어떤 조치가 필요한가"를 즉시 판단하기 어렵고,
질문 유형(길이) 분포도 조회할 수 없다.

## Proposed Solution

두 가지를 추가한다.

1. **백엔드 엔드포인트**: `GET /admin/metrics/question-length-distribution`
   - 질문 텍스트 길이 기준으로 ≤5자 / 6~20자 / 21자+ 세 구간 집계
   - 기존 `getCategoryDistribution` 과 동일한 스코프 필터링 패턴 적용

2. **프론트엔드 대시보드 보강**:
   - 섹션 A (질문 패턴 분석): 중복 질의 TOP 10 + 질문 길이 분포 막대 차트 2열 배치
   - 섹션 B (RAG 개선 포인트 도출): 지표값에서 인사이트를 자동 생성해 카드로 표시
     (critical / warn / info 수준별 컬러 보더)

## Out of Scope

- 질문 텍스트 형태소 분석이나 키워드 추출
- 시계열 기반 길이 분포 (날짜별 트렌드)
- 인사이트 서버사이드 생성 (클라이언트 계산으로 충분)

## Success Criteria

- `GET /admin/metrics/question-length-distribution` 응답에 `veryShort / short / long / total` 포함
- `/ops/statistics` 페이지에 질문 길이 분포 막대 차트 표시
- 지표 임계값 초과 시 인사이트 카드 자동 생성
- 모든 인사이트가 0건이면 "현재 주요 이슈 없음" 카드 표시
- 기존 Spring Boot 테스트 50개 전부 통과
