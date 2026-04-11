# Proposal: Celery 기반 Ingestion Job 자동 처리

## Change ID

`celery-worker`

## 배경

현재 `ingestion-worker`는 CLI 도구로만 존재한다.
Admin API가 ingestion job을 생성해 `status=queued`로 전환해도
운영자가 수동으로 `ingestion-worker run --job-id <id>`를 실행해야 파이프라인이 시작된다.

이 수동 단계를 제거하고 queued 상태 job을 자동으로 감지·실행하는
비동기 워커를 도입한다.

## 목표

- Admin API가 job을 `queued`로 전환하면 워커가 자동으로 감지해 파이프라인을 실행한다.
- 기존 `IngestionJobRunner.run_job()` 로직은 그대로 재사용한다.
- Admin API 코드 변경 없이 폴링 방식으로 구현한다 (webhook/event 방식 제외).

## 아키텍처 결정

**Celery Beat 폴링 방식**을 채택한다.

- Celery Beat가 5초마다 `GET /admin/ingestion-jobs?status=queued` 를 호출한다.
- 새 queued job을 발견하면 `process_job.delay(job_id)` 로 Celery 워커에 위임한다.
- Celery 워커는 기존 `IngestionJobRunner.run_job(job_id)` 를 실행한다.
- Redis는 이미 docker-compose 스택에 포함되어 있으므로 브로커/백엔드로 재사용한다.

대안으로 검토한 방식:
- **Webhook 방식**: Admin API 수정 필요 — 이번 범위 제외
- **Redis Pub/Sub 직접 구독**: Celery 없이 직접 구현 — 재시도·동시성 관리 비용이 큼

## 수정 파일

| 파일 | 변경 종류 | 내용 |
|------|----------|------|
| `python/ingestion-worker/pyproject.toml` | 수정 | `celery[redis]>=5.3.0` 의존성 추가 |
| `python/ingestion-worker/src/ingestion_worker/celery_app.py` | 신규 | Celery 앱 초기화, Redis 브로커/백엔드 설정 |
| `python/ingestion-worker/src/ingestion_worker/tasks.py` | 신규 | `process_job` 태스크, `poll_queued_jobs` 주기 태스크 |
| `python/ingestion-worker/src/ingestion_worker/admin_api_client.py` | 수정 | `list_queued_jobs()` 메서드 추가 |
| `python/ingestion-worker/src/ingestion_worker/app.py` | 수정 | `worker` CLI 서브커맨드 추가 |

## 제외 범위

- Admin API (Spring Boot) 코드 변경 없음
- Webhook / push 방식 도입 없음
- Celery Flower 모니터링 UI 설정 없음
- Docker 이미지 / Kubernetes 배포 설정 없음
- ingestion-worker 기존 `run` 커맨드 동작 변경 없음

## 완료 조건

- `ingestion-worker worker` 명령으로 Celery 워커 + Beat가 함께 시작된다.
- Admin API에서 job이 `queued`로 전환된 후 5초 이내에 워커가 감지해 `running` 으로 전환한다.
- job 실패 시 최대 3회 재시도(60초 간격)한다.
- 동일 job이 중복 처리되지 않는다 (이미 처리 중인 job_id는 재투입하지 않음).
- 환경변수 `REDIS_URL`, `ADMIN_API_BASE_URL`, `ADMIN_API_SESSION_TOKEN` 으로 설정한다.
