from __future__ import annotations

from typing import Optional

import httpx

from .models import CrawlSource, IngestionJob, JobStage, JobStatus


class AdminApiClient:
    """Admin API와 통신하는 HTTP 클라이언트."""

    def __init__(
        self,
        base_url: str,
        session_token: str = "",
        username: str = "",
        password: str = "",
    ):
        self.base_url = base_url.rstrip("/")
        self.username = username
        self.password = password
        self.session_token = session_token
        self.client = httpx.Client(
            base_url=self.base_url,
            headers={"X-Admin-Session-Id": session_token},
            timeout=30.0,
        )
        # 자격증명이 있으면 시작 시 자동 로그인
        if username and password and not session_token:
            self._login()

    def _login(self) -> None:
        """Admin API에 로그인하여 세션 토큰을 갱신한다."""
        response = self.client.post(
            "/admin/auth/login",
            json={"email": self.username, "password": self.password},
        )
        response.raise_for_status()
        body = response.json()
        # 호환: {session: {token}} 형식을 기본으로, 과거 응답 포맷도 허용한다.
        self.session_token = (
            body.get("session", {}).get("token")
            or body.get("sessionId")
            or body.get("token")
        )
        if not self.session_token:
            raise RuntimeError("Admin API login succeeded but session token is missing")
        self.client.headers.update({"X-Admin-Session-Id": self.session_token})

    def _request_with_retry(self, method: str, url: str, **kwargs):
        """401 응답 시 재로그인 후 재시도한다."""
        response = getattr(self.client, method)(url, **kwargs)
        if response.status_code == 401 and self.username and self.password:
            self._login()
            response = getattr(self.client, method)(url, **kwargs)
        return response

    def get_ingestion_job(self, job_id: str) -> IngestionJob:
        """Ingestion job 정보를 조회한다."""
        response = self._request_with_retry("get", f"/admin/ingestion-jobs/{job_id}")
        response.raise_for_status()
        return IngestionJob.model_validate(response.json())

    def get_crawl_source(self, source_id: str) -> CrawlSource:
        """Crawl source 정보를 조회한다."""
        response = self._request_with_retry("get", f"/admin/crawl-sources/{source_id}")
        response.raise_for_status()
        return CrawlSource.model_validate(response.json())

    def transition_job_status(
        self,
        job_id: str,
        job_status: JobStatus,
        job_stage: JobStage,
        error_code: Optional[str] = None,
    ) -> None:
        """Job 상태를 전이한다 (callback).

        Spring Boot API 응답은 { jobId, jobStatus, jobStage } 만 반환하며
        호출부에서 반환값을 사용하지 않으므로 None을 반환한다.
        """
        payload = {
            "jobStatus": job_status.value,
            "jobStage": job_stage.value,
        }
        if error_code:
            payload["errorCode"] = error_code

        response = self._request_with_retry(
            "post",
            f"/admin/ingestion-jobs/{job_id}/status",
            json=payload,
        )
        response.raise_for_status()

    def register_document(
        self,
        organization_id: str,
        title: str,
        source_uri: str,
        document_type: str = "webpage",
        visibility_scope: str = "organization",
        crawl_source_id: Optional[str] = None,
        collection_name: Optional[str] = None,
    ) -> str:
        """documents 테이블에 문서를 등록하고 document_id를 반환한다.

        document_chunks FK 제약을 만족시키기 위해 청크 저장 전에 반드시 호출해야 한다.
        """
        payload: dict = {
            "organizationId": organization_id,
            "title": title,
            "sourceUri": source_uri,
            "documentType": document_type,
            "visibilityScope": visibility_scope,
        }
        if crawl_source_id:
            payload["crawlSourceId"] = crawl_source_id
        if collection_name:
            payload["collectionName"] = collection_name

        response = self._request_with_retry("post", "/admin/documents", json=payload)
        response.raise_for_status()
        return response.json()["id"]

    def save_document_chunk(
        self,
        document_id: str,
        chunk_key: str,
        chunk_text: str,
        chunk_order: int,
        token_count: Optional[int],
        embedding_vector: Optional[list[float]],
        document_version_id: Optional[str] = None,
        metadata: Optional[dict] = None,
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
        if metadata:
            payload["metadata"] = metadata

        response = self._request_with_retry("post", "/admin/document-chunks", json=payload)
        response.raise_for_status()
        return response.json()

    def list_queued_jobs(self, page_size: int = 10) -> list[IngestionJob]:
        """queued 상태 ingestion job 목록을 조회한다."""
        response = self._request_with_retry(
            "get",
            "/admin/ingestion-jobs",
            params={"status": "queued", "page_size": page_size},
        )
        response.raise_for_status()
        items = response.json().get("items", [])
        return [IngestionJob.model_validate(item) for item in items]

    def close(self):
        """HTTP 클라이언트를 종료한다."""
        self.client.close()
