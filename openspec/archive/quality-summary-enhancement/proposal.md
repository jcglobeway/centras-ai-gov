# Proposal: quality-summary-enhancement

## Problem

현재 /ops/quality-summary 페이지는 RAGAS 4개 지표를 단순 테이블로만 표시하며,
다음 항목이 누락되어 있다.

1. 레이더 차트 - 4개 지표의 종합적 시각화 부재
2. 버전 비교 - 최근 2회 평가 간 delta(변화량)를 확인할 방법 없음
3. 알림 배너 - Hallucination/Faithfulness 임계값 초과 시 경고 없음
4. 피드백 세그먼트 바 - 긍정/부정 비율을 직관적으로 보여주는 UI 없음
5. PII 타임스탬프 - 마지막 감지 시각이 카드에 표시되지 않음
6. 섹션 구분 헤더 - RAGAS 평가와 사용자 피드백 섹션이 구분되지 않음

PRD v3 4-3. QA Evaluation 섹션 및 레퍼런스 UI(stitch/screens/v1/03-quality-security-summary)
기준으로 현재 데이터로 구현 가능한 P2 항목을 보완한다.

## Proposed Solution

기존 API 엔드포인트만 활용하여 frontend/src/app/ops/quality-summary/page.tsx를 수정한다.

### 변경 범위

1. RAGAS 레이더 차트
- Recharts RadarChart (이미 설치됨) 사용
- 4축: Faithfulness / Answer Relevance / Context Precision / Context Recall
- 데이터 없으면 빈 상태(empty state) 표시

2. RAGAS 스코어카드 버전 비교
- page_size=1 -> page_size=2 변경으로 최근 2회 평가 조회
- ScoreTable에 delta 컬럼 추가 (현재값 - 이전값, 색상 구분)

3. 알림 배너
- Hallucination Rate: > 5% Critical, > 3% Warning
- Faithfulness: < 0.80 Critical, < 0.85 Warning
- 두 조건 모두 배너로 표시 (복수 가능)

4. 피드백 세그먼트 바
- 기존 3박스 그리드 -> 단일 가로 세그먼트 바로 교체
- 긍정(초록) / 부정(빨강) 비율 시각화 + 건수 표시

5. PII 카드 lastDetectedAt 표시
- 기존 감사 로그 바로가기 링크 위에 마지막 감지 시각 추가
- lastDetectedAt == null 이면 감지 없음 표시

6. 섹션 구분 헤더
- RAGAS 평가 구분선 헤더 추가 (레이더 차트 + 스코어카드 묶음)
- 사용자 피드백 구분선 헤더 추가 (피드백 섹션 묶음)

## Out of Scope

- 새 API 엔드포인트 추가
- 새 npm 패키지 추가 (Recharts 이미 설치됨)
- Retrieval 심층 분석 (MRR, Recall@K) - ground truth 데이터 없음
- 목업/하드코딩 데이터 - 실제 API 데이터만 사용

## Success Criteria

- [ ] 레이더 차트가 4개 RAGAS 지표를 시각화한다 (데이터 없으면 empty state)
- [ ] RAGAS 스코어카드에 delta 컬럼이 표시된다 (2회 이상 평가 시)
- [ ] Hallucination/Faithfulness 임계값 초과 시 알림 배너가 표시된다
- [ ] 피드백 섹션이 세그먼트 바로 교체된다
- [ ] PII 카드에 lastDetectedAt이 표시된다
- [ ] 섹션 구분 헤더가 추가된다
- [ ] TypeScript 컴파일 에러 없음
