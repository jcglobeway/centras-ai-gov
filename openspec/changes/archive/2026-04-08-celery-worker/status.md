# Status: celery-worker

- 상태: `implemented`
- 시작일: `2026-04-07`
- 마지막 업데이트: `2026-04-07`

## Progress

- [x] proposal.md 작성 완료
- [x] tasks.md 작성 완료
- [x] Phase 1 — 의존성 및 Celery 앱 초기화
- [x] Phase 2 — AdminApiClient 확장
- [x] Phase 3 — Celery 태스크 구현
- [x] Phase 4 — CLI 진입점 추가
- [ ] Phase 5 — 통합 검증 (런타임 환경 필요)

## Verification

- `python -c "from ingestion_worker.tasks import process_job; print('OK')"` → OK (import 검증 완료)
- `python -m ingestion_worker.app worker --help` → OK
- `env REDIS_URL=redis://localhost:6379/0 ADMIN_API_SESSION_TOKEN=test python -m ingestion_worker.app worker --concurrency 1 --loglevel warning` → OK
  - Celery worker banner 출력 확인
  - Beat `poll_queued_jobs` 실행 확인
  - Admin API(`http://localhost:8081`)가 내려가 있으면 `Connection refused` 경고를 남김

## Risks

- `poll_queued_jobs` 와 `process_job` 사이 경쟁 조건: 동일 job이 두 번 투입될 수 있음.
  Celery inspect active 로 이미 실행 중인 job_id를 필터링해 방지한다.
  단, inspect 응답 지연 시 중복 투입 가능성이 낮게 존재한다.
  Admin API의 job status 전이 (`queued → running`) 가 멱등하므로 실질적 영향은 제한적이다.
- Beat + Worker를 단일 프로세스로 실행할 경우 Beat 장애가 워커 전체에 영향을 줄 수 있음.
  프로덕션 환경에서는 Beat와 Worker를 분리 실행하는 것을 권장한다.
- 로컬 검증은 Redis 브로커(`redis://localhost:6379/0`)와 Admin API(`http://localhost:8081`)가 함께 떠 있어야 완결된다.
