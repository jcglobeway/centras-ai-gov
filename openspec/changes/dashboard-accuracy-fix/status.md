# Status

- 상태: `in_progress`
- 시작일: `2026-03-27`
- 마지막 업데이트: `2026-03-27`

## Progress

- proposal.md, tasks.md, status.md 초안 작성 완료
- 구현 완료:
  - C1: `SaveDailyMetricsCommand`에 `resolvedRate` 필드 추가, 스케줄러에서 계산·전달, 어댑터에서 저장
  - W1: `client/page.tsx` revisitRate 카드 label → "피드백 완료율", help 텍스트 수정
  - W2: `ops/page.tsx` 파이프라인 레이턴시 섹션 및 관련 상수(PIPELINE_STEPS, PIPELINE_TOTAL_MS) 제거, ProgressBar import 제거
  - W3: `ops/cost/page.tsx` 문서 건강도 섹션 제거
  - W4: `client/page.tsx` MetricsLineChart 호출 전 autoResolutionRate·escalationRate ×100 변환
  - W5: `qa/page.tsx` answerStatus="error" → "오류" badge 처리
  - M4: `qa/page.tsx`, `ops/quality/page.tsx` ragasRows에 Context Recall (target: 0.75) 추가

## Verification

- 백엔드 빌드 및 테스트: BUILD SUCCESSFUL (전체 통과)
- 프론트엔드 런타임 검증 미완료 (npm run dev 확인 필요)

## Risks

- **W4 Y축 스케일**: MetricsLineChart는 scale prop을 지원하지 않으므로 호출부에서 데이터 변환 방식으로 처리.
- **C1 resolvedRate**: DailyMetricsEntity의 resolvedRate 컬럼 매핑 이미 존재 확인. SaveMetricsPortAdapter의 `resolvedRate = null` 하드코딩을 `command.resolvedRate`로 교체.
