from __future__ import annotations

from .admin_api_client import AdminApiClient
from .crawl_executor import CrawlExecutor
from .models import JobStage, JobStatus


class IngestionJobRunner:
    """Ingestion job 실행 orchestrator."""

    def __init__(self, admin_api_client: AdminApiClient):
        self.admin_api = admin_api_client
        self.crawl_executor = CrawlExecutor()

    def run_job(self, job_id: str) -> None:
        """Job을 실행하고 lifecycle을 관리한다."""
        print(f"[JobRunner] Starting job: {job_id}")

        try:
            # 1. Job 정보 조회
            job = self.admin_api.get_ingestion_job(job_id)
            print(f"[JobRunner] Job loaded: {job.id}, status={job.job_status}")

            # 2. Crawl source 정보 조회
            source = self.admin_api.get_crawl_source(job.crawl_source_id)
            print(f"[JobRunner] Crawl source loaded: {source.name}, uri={source.source_uri}")

            # 3. Job status → RUNNING
            print("[JobRunner] Transitioning to RUNNING...")
            self.admin_api.transition_job_status(
                job_id=job_id,
                job_status=JobStatus.RUNNING,
                job_stage=JobStage.FETCH,
            )

            # 4. Crawl 실행 (FETCH stage)
            print("[JobRunner] Executing crawl...")
            crawl_result = self.crawl_executor.execute_crawl_sync(
                source_uri=source.source_uri,
                crawl_source_id=source.id,
            )

            if crawl_result["status"] == "error":
                # 실패 처리
                print(f"[JobRunner] Crawl failed: {crawl_result['error']}")
                self.admin_api.transition_job_status(
                    job_id=job_id,
                    job_status=JobStatus.FAILED,
                    job_stage=JobStage.FETCH,
                    error_code="CRAWL_ERROR",
                )
                return

            # 5. EXTRACT stage (스텁 - 실제 파싱은 이후)
            print("[JobRunner] Transitioning to EXTRACT stage...")
            self.admin_api.transition_job_status(
                job_id=job_id,
                job_status=JobStatus.RUNNING,
                job_stage=JobStage.EXTRACT,
            )
            print("[JobRunner] EXTRACT stage completed (stub)")

            # 6. COMPLETE stage
            print("[JobRunner] Transitioning to COMPLETE...")
            self.admin_api.transition_job_status(
                job_id=job_id,
                job_status=JobStatus.SUCCEEDED,
                job_stage=JobStage.COMPLETE,
            )

            print(f"[JobRunner] Job {job_id} completed successfully!")

        except Exception as e:
            print(f"[JobRunner] Unexpected error: {e}")
            # 최선의 노력으로 FAILED 상태로 전이 시도
            try:
                self.admin_api.transition_job_status(
                    job_id=job_id,
                    job_status=JobStatus.FAILED,
                    job_stage=JobStage.FETCH,
                    error_code="WORKER_ERROR",
                )
            except Exception as callback_error:
                print(f"[JobRunner] Failed to send error callback: {callback_error}")
            raise
