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
        """
        Job을 실행하고 lifecycle을 관리한다.

        단계: FETCH → EXTRACT → CHUNK → EMBED → INDEX → COMPLETE
        각 단계 실패 시 FAILED로 전이.
        """
        print(f"[JobRunner] Starting job: {job_id}")

        try:
            # 1. Job 정보 조회
            job = self.admin_api.get_ingestion_job(job_id)
            print(f"[JobRunner] Job loaded: {job.id}, status={job.job_status}")

            # 2. Crawl source 정보 조회
            source = self.admin_api.get_crawl_source(job.crawl_source_id)
            print(f"[JobRunner] Crawl source loaded: {source.name}, uri={source.source_uri}")

            # 3. FETCH 단계: URL 크롤링
            print("[JobRunner] Transitioning to RUNNING / FETCH...")
            self.admin_api.transition_job_status(
                job_id=job_id,
                job_status=JobStatus.RUNNING,
                job_stage=JobStage.FETCH,
            )

            crawl_result = self.crawl_executor.execute_crawl_sync(
                source_uri=source.source_uri,
                crawl_source_id=source.id,
            )

            if crawl_result["status"] == "error":
                print(f"[JobRunner] Crawl failed: {crawl_result['error']}")
                self.admin_api.transition_job_status(
                    job_id=job_id,
                    job_status=JobStatus.FAILED,
                    job_stage=JobStage.FETCH,
                    error_code="CRAWL_ERROR",
                )
                return

            # 4. EXTRACT 단계: HTML → 텍스트 추출
            print("[JobRunner] Transitioning to EXTRACT...")
            self.admin_api.transition_job_status(
                job_id=job_id,
                job_status=JobStatus.RUNNING,
                job_stage=JobStage.EXTRACT,
            )

            raw_text = self.crawl_executor.extract_text(crawl_result["html_content"])
            print(f"[JobRunner] Extracted text length: {len(raw_text)}")

            if not raw_text.strip():
                print("[JobRunner] No text extracted, marking as failed")
                self.admin_api.transition_job_status(
                    job_id=job_id,
                    job_status=JobStatus.FAILED,
                    job_stage=JobStage.EXTRACT,
                    error_code="EMPTY_CONTENT",
                )
                return

            # 5. CHUNK 단계: 텍스트 분할
            print("[JobRunner] Transitioning to CHUNK...")
            self.admin_api.transition_job_status(
                job_id=job_id,
                job_status=JobStatus.RUNNING,
                job_stage=JobStage.CHUNK,
            )

            chunks = self.crawl_executor.chunk_text(raw_text)
            print(f"[JobRunner] Created {len(chunks)} chunks")

            # 6. EMBED 단계: 임베딩 생성
            print("[JobRunner] Transitioning to EMBED...")
            self.admin_api.transition_job_status(
                job_id=job_id,
                job_status=JobStatus.RUNNING,
                job_stage=JobStage.EMBED,
            )

            chunk_embeddings: list[tuple[str, list[float] | None]] = []
            for i, chunk_text in enumerate(chunks):
                embedding = self.crawl_executor.embed_text(chunk_text)
                chunk_embeddings.append((chunk_text, embedding))
                if (i + 1) % 10 == 0:
                    print(f"[JobRunner] Embedded {i + 1}/{len(chunks)} chunks")

            embedded_count = sum(1 for _, emb in chunk_embeddings if emb is not None)
            print(f"[JobRunner] Successfully embedded {embedded_count}/{len(chunks)} chunks")

            # 7. INDEX 단계: Admin API를 통해 청크 저장
            print("[JobRunner] Transitioning to INDEX...")
            self.admin_api.transition_job_status(
                job_id=job_id,
                job_status=JobStatus.RUNNING,
                job_stage=JobStage.INDEX,
            )

            # document_id는 crawl_source_id 기반으로 생성 (실제로는 Admin API에서 할당)
            document_id = f"doc_{source.id}"
            saved_count = 0

            for i, (chunk_text, embedding) in enumerate(chunk_embeddings):
                try:
                    token_count = len(chunk_text.split())
                    self.admin_api.save_document_chunk(
                        document_id=document_id,
                        chunk_key=f"chunk_{i}",
                        chunk_text=chunk_text,
                        chunk_order=i,
                        token_count=token_count,
                        embedding_vector=embedding,
                    )
                    saved_count += 1
                except Exception as e:
                    print(f"[JobRunner] Failed to save chunk {i}: {e}")

            print(f"[JobRunner] Saved {saved_count}/{len(chunks)} chunks to index")

            # 8. COMPLETE
            print("[JobRunner] Transitioning to COMPLETE...")
            self.admin_api.transition_job_status(
                job_id=job_id,
                job_status=JobStatus.SUCCEEDED,
                job_stage=JobStage.COMPLETE,
            )

            print(f"[JobRunner] Job {job_id} completed successfully!")

        except Exception as e:
            print(f"[JobRunner] Unexpected error: {e}")
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
