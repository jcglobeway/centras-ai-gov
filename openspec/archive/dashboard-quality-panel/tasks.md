# Tasks: dashboard-quality-panel

## Implementation Tasks

- [x] openspec 아티팩트 작성 (proposal.md, tasks.md, status.md)
- [x] `RagasEvaluation` 타입을 `ops/page.tsx` import에 추가
- [x] SWR 훅 추가: ragas-evaluations (최근 7개)
- [x] SWR 훅 추가: pii-count
- [x] SWR 훅 추가: feedbacks (최근 100개)
- [x] SWR 훅 추가: feedback-trend (7일)
- [x] 인라인 컴포넌트 `MiniSparkline` 추가
- [x] 인라인 컴포넌트 `FeedbackBar` 추가
- [x] "품질/보안 요약" 섹션 헤더 (상세 보기 링크 포함) 삽입
- [x] Row 1: Faithfulness 카드 (스파크라인 + 최신값)
- [x] Row 1: Hallucination Rate 카드 (스파크라인 + 최신값)
- [x] Row 1: Recall@K 카드 (empty state)
- [x] Row 2: Session Success Rate 카드 (empty state)
- [x] Row 2: PII 감지 건수 카드
- [x] Row 3: 사용자 피드백 full-width 패널

## Testing Tasks

- [x] TypeScript 컴파일 에러 없음 확인 (`npm run build` 또는 `tsc --noEmit`)
- [x] 데이터 없을 때 empty state 표시 확인 (UI 검토)
