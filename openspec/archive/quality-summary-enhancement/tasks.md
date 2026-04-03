# Tasks: quality-summary-enhancement

## Implementation Tasks

### T1. API 호출 수정
- [x] useSWR page_size=1 -> page_size=2 로 변경
- [x] prev(이전 평가) 데이터 추출 로직 추가

### T2. 섹션 구분 헤더 컴포넌트
- [x] SectionDivider 인라인 컴포넌트 작성 (title prop)
- [x] RAGAS 평가 섹션 헤더 삽입
- [x] 사용자 피드백 섹션 헤더 삽입

### T3. 알림 배너 컴포넌트
- [x] AlertBanner 인라인 컴포넌트 작성 (level: warning|critical, message: string)
- [x] Hallucination Rate > 3% Warning, > 5% Critical 조건 추가
- [x] Faithfulness < 0.85 Warning, < 0.80 Critical 조건 추가
- [x] KPI 카드 블록 위에 배너 렌더링 (복수 배너 지원)

### T4. RAGAS 레이더 차트
- [x] Recharts RadarChart import 추가
- [x] radarData 배열 구성 (4개 지표 * 100 스케일)
- [x] RadarChart 컴포넌트 렌더링 (ResponsiveContainer 260px, 4축)
- [x] 데이터 없을 때 empty state 메시지 표시
- [x] RAGAS 평가 섹션 안에 스코어카드 위에 배치

### T5. RAGAS 스코어카드 delta 컬럼
- [x] DeltaScoreTable 인라인 컴포넌트 작성 (rows prop)
- [x] delta = current - prev 계산 (prev 없으면 null)
- [x] delta > 0 초록, < 0 빨강, null 회색 표시
- [x] 기존 ScoreTable 컴포넌트를 DeltaScoreTable로 교체

### T6. 피드백 세그먼트 바
- [x] FeedbackSegmentBar 인라인 컴포넌트 작성 (positive, negative prop)
- [x] 기존 3박스 grid 제거
- [x] 세그먼트 바 + 긍정/부정 건수 레이블 렌더링

### T7. PII 카드 lastDetectedAt 표시
- [x] lastDetectedAt 포맷팅 (ko-KR locale, null 시 감지 없음)
- [x] KpiCard 아래 lastDetectedAt 텍스트 추가

### T11. /ops/quality 와 중복 섹션 제거 (역할 분리)
- [x] RAG 검색 통계 패널 제거 — `/ops/quality`에 날짜 필터 포함 더 풍부하게 존재
- [x] Faithfulness 시계열 차트 제거 — `/ops/quality`에 RAGAS 추세 차트로 존재
- [x] 관련 타입(`RagSearchLogStats`), SWR 훅, 컴포넌트(`QualityTrendChart`, `RagSearchStatsPanel`) 모두 제거
- [x] `page_size=10` → `page_size=2` 원복 (Δ 계산에 2건이면 충분)
- [x] TypeScript 컴파일 에러 없음 확인

#### 역할 정의 (확정)
- `/ops/quality` — RAG 파이프라인 품질 모니터링 (날짜 필터, 추세, 검색 통계)
- `/ops/quality-summary` — 품질·보안 요약 (PII, 사용자 만족도, 알림 배너, 레이더, Δ 스코어카드)

## Verification

- [x] TypeScript 컴파일 에러 없음
- [ ] 데이터 없을 때 각 섹션 empty state 정상 표시 (수동 확인 필요)
- [ ] 데이터 있을 때 알림 배너, 레이더 차트, Δ 스코어카드, 피드백 세그먼트 바 정상 표시 (수동 확인 필요)
