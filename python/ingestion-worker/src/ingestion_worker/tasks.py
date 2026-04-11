from __future__ import annotations

import os
import re

import httpx
from celery.utils.log import get_task_logger

from .admin_api_client import AdminApiClient
from .celery_app import app
from .job_runner import IngestionJobRunner

logger = get_task_logger(__name__)

_PROCESSING: set[str] = set()


def _make_client() -> AdminApiClient:
    return AdminApiClient(
        base_url=os.getenv("ADMIN_API_BASE_URL", "http://localhost:8081"),
        session_token=os.getenv("ADMIN_API_SESSION_TOKEN", ""),
        username=os.getenv("ADMIN_API_USERNAME", ""),
        password=os.getenv("ADMIN_API_PASSWORD", ""),
    )


@app.task(bind=True, max_retries=3, default_retry_delay=60)
def process_job(self, job_id: str) -> None:
    """단일 ingestion job을 실행한다.

    이미 처리 중인 job_id는 건너뛰며, 예외 발생 시 최대 3회 재시도(60초 간격)한다.
    """
    if job_id in _PROCESSING:
        logger.info(f"[Celery] Job {job_id} already in progress, skipping")
        return

    _PROCESSING.add(job_id)
    client = _make_client()
    try:
        runner = IngestionJobRunner(admin_api_client=client)
        runner.run_job(job_id)
        logger.info(f"[Celery] Job {job_id} completed successfully")
    except Exception as exc:
        if isinstance(exc, httpx.HTTPStatusError) and exc.response.status_code in {400, 404}:
            logger.warning(f"[Celery] Job {job_id} non-retryable error: {exc}")
            return
        logger.error(f"[Celery] Job {job_id} failed: {exc}")
        raise self.retry(exc=exc, countdown=60)
    finally:
        _PROCESSING.discard(job_id)
        client.close()


@app.task
def poll_queued_jobs() -> None:
    """queued 상태 job을 조회해 아직 처리되지 않은 job을 Celery 워커에 위임한다.

    Celery inspect로 현재 활성 task의 job_id를 확인해 중복 투입을 방지한다.
    """
    client = _make_client()
    try:
        jobs = client.list_queued_jobs(page_size=10)
    except Exception as e:
        logger.warning(f"[Celery] poll_queued_jobs error: {e}")
        return
    finally:
        client.close()

    # 현재 워커에서 실행 중인 job_id 수집 (중복 방지)
    active_job_ids: set[str] = set(_PROCESSING)
    try:
        inspect = app.control.inspect(timeout=1.0)
        task_sets = [inspect.active() or {}, inspect.reserved() or {}, inspect.scheduled() or {}]
        for task_set in task_sets:
            for worker_tasks in task_set.values():
                for task in worker_tasks:
                    args = task.get("args")
                    if isinstance(args, (list, tuple)) and args:
                        active_job_ids.add(str(args[0]))
                        continue
                    if isinstance(args, str):
                        match = re.search(r"ing_job_[a-z0-9]+", args)
                        if match:
                            active_job_ids.add(match.group(0))
    except Exception:
        pass  # inspect 실패 시 _PROCESSING 기준으로만 중복 방지

    for job in jobs:
        job_id = job.id
        if job_id not in active_job_ids:
            logger.info(f"[Celery] Dispatching job {job_id}")
            process_job.delay(job_id)
        else:
            logger.debug(f"[Celery] Job {job_id} already active, skipping")
