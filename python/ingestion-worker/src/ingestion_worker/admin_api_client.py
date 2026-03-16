from __future__ import annotations

from typing import Optional

import httpx

from .models import CrawlSource, IngestionJob, JobStage, JobStatus


class AdminApiClient:
    """Admin API와 통신하는 HTTP 클라이언트."""

    def __init__(self, base_url: str, session_token: str):
        self.base_url = base_url.rstrip("/")
        self.session_token = session_token
        self.client = httpx.Client(
            base_url=self.base_url,
            headers={"X-Admin-Session-Id": session_token},
            timeout=30.0,
        )

    def get_ingestion_job(self, job_id: str) -> IngestionJob:
        """Ingestion job 정보를 조회한다."""
        response = self.client.get(f"/admin/ingestion-jobs/{job_id}")
        response.raise_for_status()
        return IngestionJob.model_validate(response.json())

    def get_crawl_source(self, source_id: str) -> CrawlSource:
        """Crawl source 정보를 조회한다."""
        response = self.client.get(f"/admin/crawl-sources/{source_id}")
        response.raise_for_status()
        return CrawlSource.model_validate(response.json())

    def transition_job_status(
        self,
        job_id: str,
        job_status: JobStatus,
        job_stage: JobStage,
        error_code: Optional[str] = None,
    ) -> IngestionJob:
        """Job 상태를 전이한다 (callback)."""
        payload = {
            "jobStatus": job_status.value,
            "jobStage": job_stage.value,
        }
        if error_code:
            payload["errorCode"] = error_code

        response = self.client.post(f"/admin/ingestion-jobs/{job_id}/status", json=payload)
        response.raise_for_status()
        data = response.json()

        # API 응답에서 전체 job 정보가 아니라 일부만 반환될 수 있으므로
        # 간단히 job_id와 상태만 담은 객체를 반환
        return IngestionJob(
            id=data["jobId"],
            crawl_source_id="",  # stub
            organization_id="",  # stub
            job_type="crawl",
            job_status=JobStatus(data["jobStatus"]),
            job_stage=JobStage(data["jobStage"]),
            trigger_type="manual",
            attempt_count=1,
        )

    def save_document_chunk(
        self,
        document_id: str,
        chunk_key: str,
        chunk_text: str,
        chunk_order: int,
        token_count: Optional[int],
        embedding_vector: Optional[list[float]],
        document_version_id: Optional[str] = None,
    ) -> dict:
        """문서 청크와 임베딩을 Admin API를 통해 저장한다."""
        payload: dict = {
            "documentId": document_id,
            "chunkKey": chunk_key,
            "chunkText": chunk_text,
            "chunkOrder": chunk_order,
        }
        if document_version_id:
            payload["documentVersionId"] = document_version_id
        if token_count is not None:
            payload["tokenCount"] = token_count
        if embedding_vector is not None:
            # vector를 JSON 직렬화 가능한 문자열로 변환
            payload["embeddingVector"] = "[" + ",".join(str(v) for v in embedding_vector) + "]"

        response = self.client.post("/admin/document-chunks", json=payload)
        response.raise_for_status()
        return response.json()

    def close(self):
        """HTTP 클라이언트를 종료한다."""
        self.client.close()
