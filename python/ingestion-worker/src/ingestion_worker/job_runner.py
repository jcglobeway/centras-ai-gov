from __future__ import annotations

import asyncio
import os

import httpx
from loguru import logger

from .admin_api_client import AdminApiClient
from .crawler import AutonomousCrawler, HierarchicalChunker
from .kg_extractor import KGExtractor
from .models import CrawledPage, JobStage, JobStatus


class IngestionJobRunner:
    """Ingestion job 실행 orchestrator.

    단계: FETCH → EXTRACT → CHUNK → EMBED → INDEX → COMPLETE

    FETCH: AutonomousCrawler로 멀티페이지 재귀 크롤
    EXTRACT: ContentExtractor로 섹션 구조 추출 (크롤 단계 내 수행)
    CHUNK: HierarchicalChunker로 계층형 청킹
    EMBED: Ollama bge-m3로 임베딩 생성
    INDEX: Admin API POST /admin/document-chunks로 저장
    """

    def __init__(self, admin_api_client: AdminApiClient):
        self.admin_api = admin_api_client
        self.ollama_url = os.getenv("OLLAMA_URL", "http://localhost:11434")

        self._max_depth = int(os.getenv("CRAWLER_MAX_DEPTH", "3"))
        self._max_pages = int(os.getenv("CRAWLER_MAX_PAGES", "100"))
        self._crawler_delay = float(os.getenv("CRAWLER_DELAY", "1.0"))
        self._concurrency = int(os.getenv("CRAWLER_CONCURRENCY", "3"))

    def run_job(self, job_id: str) -> None:
        """Job을 실행하고 lifecycle을 관리한다."""
        logger.info(f"[JobRunner] Starting job: {job_id}")

        try:
            job = self.admin_api.get_ingestion_job(job_id)
            logger.info(f"[JobRunner] Job loaded: {job.id}, status={job.job_status}")

            source = self.admin_api.get_crawl_source(job.crawl_source_id)
            logger.info(f"[JobRunner] Crawl source: {source.name}, uri={source.source_uri}")

            # FETCH + EXTRACT: 멀티페이지 크롤
            logger.info("[JobRunner] Transitioning to RUNNING / FETCH...")
            self.admin_api.transition_job_status(
                job_id=job_id,
                job_status=JobStatus.RUNNING,
                job_stage=JobStage.FETCH,
            )

            pages = asyncio.run(self._crawl_all(source.source_uri))

            if not pages:
                logger.error("[JobRunner] 크롤 결과 없음")
                self.admin_api.transition_job_status(
                    job_id=job_id,
                    job_status=JobStatus.FAILED,
                    job_stage=JobStage.FETCH,
                    error_code="CRAWL_ERROR",
                )
                return

            logger.info(f"[JobRunner] 크롤 완료: {len(pages)} 페이지")

            # EXTRACT 단계 전환 (크롤과 함께 수행됨)
            self.admin_api.transition_job_status(
                job_id=job_id,
                job_status=JobStatus.RUNNING,
                job_stage=JobStage.EXTRACT,
            )

            # KG 추출 (선택적)
            kg_extractor = KGExtractor() if KGExtractor.is_enabled() else None
            if kg_extractor:
                logger.info("[JobRunner] KG 추출 활성화됨")
                pages = asyncio.run(self._enrich_with_kg(pages, kg_extractor))

            # CHUNK
            logger.info("[JobRunner] Transitioning to CHUNK...")
            self.admin_api.transition_job_status(
                job_id=job_id,
                job_status=JobStatus.RUNNING,
                job_stage=JobStage.CHUNK,
            )

            chunker = HierarchicalChunker()
            all_chunks = []
            for page in pages:
                sections = page.metadata.get("sections", [])
                chunks = chunker.chunk(sections, page.content, page.url)
                # 청크에 페이지 메타데이터 병합 (KG 포함)
                for chunk in chunks:
                    chunk.metadata.update({
                        k: v for k, v in page.metadata.items()
                        if k != "sections"
                    })
                all_chunks.extend(chunks)

            logger.info(f"[JobRunner] 총 {len(all_chunks)} 청크 생성")

            # EMBED
            logger.info("[JobRunner] Transitioning to EMBED...")
            self.admin_api.transition_job_status(
                job_id=job_id,
                job_status=JobStatus.RUNNING,
                job_stage=JobStage.EMBED,
            )

            chunk_with_embeddings = []
            for i, chunk in enumerate(all_chunks):
                embedding = self._embed_text(chunk.chunk_text)
                chunk_with_embeddings.append((chunk, embedding))
                if (i + 1) % 10 == 0:
                    logger.info(f"[JobRunner] 임베딩 {i + 1}/{len(all_chunks)}")

            embedded_count = sum(1 for _, emb in chunk_with_embeddings if emb is not None)
            logger.info(f"[JobRunner] 임베딩 완료: {embedded_count}/{len(all_chunks)}")

            # INDEX
            logger.info("[JobRunner] Transitioning to INDEX...")
            self.admin_api.transition_job_status(
                job_id=job_id,
                job_status=JobStatus.RUNNING,
                job_stage=JobStage.INDEX,
            )

            document_id = f"doc_{source.id}"
            saved_count = 0

            for chunk, embedding in chunk_with_embeddings:
                try:
                    self.admin_api.save_document_chunk(
                        document_id=document_id,
                        chunk_key=chunk.chunk_key,
                        chunk_text=chunk.chunk_text,
                        chunk_order=chunk.chunk_order,
                        token_count=chunk.token_count,
                        embedding_vector=embedding,
                        metadata=chunk.metadata if chunk.metadata else None,
                    )
                    saved_count += 1
                except Exception as e:
                    logger.warning(f"[JobRunner] 청크 저장 실패 {chunk.chunk_key}: {e}")

            logger.info(f"[JobRunner] 저장 완료: {saved_count}/{len(all_chunks)}")

            # COMPLETE
            self.admin_api.transition_job_status(
                job_id=job_id,
                job_status=JobStatus.SUCCEEDED,
                job_stage=JobStage.COMPLETE,
            )
            logger.info(f"[JobRunner] Job {job_id} completed successfully!")

        except Exception as e:
            logger.error(f"[JobRunner] Unexpected error: {e}")
            try:
                self.admin_api.transition_job_status(
                    job_id=job_id,
                    job_status=JobStatus.FAILED,
                    job_stage=JobStage.FETCH,
                    error_code="WORKER_ERROR",
                )
            except Exception as callback_error:
                logger.error(f"[JobRunner] Failed to send error callback: {callback_error}")
            raise

    async def _crawl_all(self, seed_url: str) -> list[CrawledPage]:
        async with AutonomousCrawler(
            max_depth=self._max_depth,
            max_pages=self._max_pages,
            concurrency=self._concurrency,
            delay=self._crawler_delay,
        ) as crawler:
            return await crawler.crawl_all(seed_url)

    async def _enrich_with_kg(
        self,
        pages: list[CrawledPage],
        extractor: KGExtractor,
    ) -> list[CrawledPage]:
        enriched = []
        for page in pages:
            kg_meta = await extractor.extract(page)
            if kg_meta:
                page.metadata.update(kg_meta)
            enriched.append(page)
        return enriched

    def _embed_text(self, text: str) -> list[float] | None:
        try:
            response = httpx.post(
                f"{self.ollama_url}/api/embeddings",
                json={"model": "bge-m3", "prompt": text},
                timeout=10.0,
            )
            if response.status_code == 200:
                return response.json().get("embedding")
            return None
        except Exception:
            return None
