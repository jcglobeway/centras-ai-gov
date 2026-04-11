# Status

- 상태: `implemented`
- 시작일: `2026-04-02`
- 마지막 업데이트: `2026-04-10`

## Progress

- [x] Phase 1 — export_db_eval_data.py 구현 + ragas_batch.py --from-eval-results 추가
- [x] Phase 2 — 실행 (200건 export, eval-runner 백그라운드 실행 중 / qwen3:4b)
- [x] Phase 3 — 검증 실행 완료
  - DB 확인: `ragas_evaluations` 총 `1065`, `org_acc` 총 `554`
  - `org_acc and evaluated_at >= 2026-04-06`는 `385` rows / `200` distinct questions
  - 최신 평가 시각: `2026-04-06 16:03:16`
  - 질문 기준 현황: `questions(org_acc)=549`, `answers 보유 질문=548`, `평가 보유 질문(distinct)=269`, `미평가 질문=280`
  - 화면 API 확인: `GET /api/admin/ragas-evaluations?organization_id=org_acc&page_size=30` 결과 `items=30`, `total=1065`
  - 결론: `/ops/quality` 데이터 노출은 확인됨. 과거 제안서의 `482`는 당시 스냅샷 기준 수치이며, 실제 실행은 `--limit 200` 배치라 `200` 질문만 평가됨
  - 추가 관찰: 동일 question_id 다중 row(최대 4회) 존재 → 후속으로 `question_id` 기준 dedup/UPSERT 정책 정리 필요

## Follow-up

- 본 change 범위의 구현/실행/검증은 완료
- `ragas_evaluations`의 `question_id` 중복 row 정리는 별도 후속 change로 분리 권장
