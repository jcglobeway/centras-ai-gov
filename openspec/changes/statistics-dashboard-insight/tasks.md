# Tasks: statistics-dashboard-insight

## 백엔드

- [x] `MetricsController.kt` — `getQuestionLengthDistribution` 엔드포인트 추가
- [x] `QuestionLengthDistributionResponse` data class 추가 (같은 파일 내)

## 프론트엔드

- [x] `page.tsx` — `QuestionLengthDistributionResponse` 인터페이스 추가
- [x] `page.tsx` — `/api/admin/metrics/question-length-distribution` SWR 훅 추가
- [x] `page.tsx` — 섹션 헤더 컴포넌트 (구분선 포함 `sec-title` 스타일) 정의
- [x] `page.tsx` — 섹션 A: 질문 패턴 분석 (중복 질의 + 질문 길이 분포 2열 그리드)
- [x] `page.tsx` — 섹션 B: RAG 개선 포인트 인사이트 카드 목록
  - [x] `Insight` 인터페이스 및 `insights` 배열 계산 로직
  - [x] fallbackRate > 10 → critical
  - [x] zeroResultRate > 5 → critical
  - [x] avgResponseMs > 2000 → warn
  - [x] 중복 질의 최다 건수 > 5 → warn
  - [x] 단답형 비율 > 20% → info
  - [x] 인사이트 0개일 때 "현재 주요 이슈 없음" info 카드
  - [x] 인사이트 카드 UI (컬러 보더 + 뱃지 + 지표값)
