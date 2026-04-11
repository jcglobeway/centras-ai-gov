# Tasks: celery-worker

## Phase 1 — 의존성 및 Celery 앱 초기화

- [x] `pyproject.toml` 에 `celery[redis]>=5.3.0` 추가
- [x] `celery_app.py` 신규 작성
  - `REDIS_URL` 환경변수로 브로커/백엔드 설정 (기본값: `redis://localhost:6379/0`)
  - `task_serializer = "json"`, `result_expires = 3600` 설정
  - Beat 스케줄: `poll_queued_jobs` 5초 간격 등록

## Phase 2 — AdminApiClient 확장

- [x] `admin_api_client.py` 에 `list_queued_jobs(page_size: int = 10) -> list[IngestionJob]` 추가
  - `GET /admin/ingestion-jobs?status=queued&page_size={page_size}` 호출
  - 응답 JSON의 `items` 필드를 `list[IngestionJob]` 으로 파싱해 반환

## Phase 3 — Celery 태스크 구현

- [x] `tasks.py` 신규 작성
  - `process_job(self, job_id: str)` 태스크
    - `bind=True`, `max_retries=3`
    - `AdminApiClient` 생성 → `IngestionJobRunner.run_job(job_id)` 실행
    - 예외 발생 시 `self.retry(exc=exc, countdown=60)` 로 재시도
    - 클라이언트 종료는 `finally` 블록에서 보장
  - `poll_queued_jobs()` 태스크
    - `list_queued_jobs()` 호출 → queued job 목록 수신
    - 이미 활성 상태인 job_id (Celery inspect active) 와 비교해 중복 제외
    - 새 job에 대해 `process_job.delay(job_id)` 호출

## Phase 4 — CLI 진입점 추가

- [x] `app.py` 에 `worker` 서브커맨드 추가
  - Celery 앱을 worker + beat 모드로 함께 시작
  - `--concurrency` 옵션 (기본값: 2)
  - `--loglevel` 옵션 (기본값: `info`)
  - 실행 예시: `ingestion-worker worker`

## Phase 5 — 통합 검증

- [x] Redis 실행 확인 (`redis://localhost:6379/0` 브로커 연결)
- [x] `ingestion-worker worker` 기동 후 Beat 스케줄 로그 확인
- [ ] Admin API에서 job을 `queued` 로 전환 → 워커가 5초 이내 감지하는지 확인
- [ ] job 완료 후 `status=succeeded` 로 전환 확인
- [ ] 의도적 실패 job으로 재시도(3회) 동작 확인

## 검증 메모

- 워커 기동 자체와 Beat 스케줄 실행은 확인했다.
- `poll_queued_jobs` 는 Admin API(`http://localhost:8081`)가 내려가 있을 때 `Connection refused` 를 남긴다.
- 전체 E2E 검증은 Admin API와 함께 다시 실행해야 한다.

## 커밋

- [ ] 단일 커밋, 한국어 메시지: `기능: celery-worker — ingestion job 자동 처리`
