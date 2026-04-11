from __future__ import annotations

from enum import Enum
from typing import Any, Optional

from pydantic import BaseModel, Field


class JobStatus(str, Enum):
    QUEUED = "queued"
    RUNNING = "running"
    SUCCEEDED = "succeeded"
    PARTIAL_SUCCESS = "partial_success"
    FAILED = "failed"
    CANCELLED = "cancelled"


class JobStage(str, Enum):
    FETCH = "fetch"
    EXTRACT = "extract"
    NORMALIZE = "normalize"
    CHUNK = "chunk"
    EMBED = "embed"
    INDEX = "index"
    COMPLETE = "complete"


class CrawlSource(BaseModel):
    model_config = {"populate_by_name": True}

    id: str
    organization_id: str = Field(alias="organizationId")
    service_id: str = Field(alias="serviceId")
    name: str
    source_type: str = Field(alias="sourceType")
    source_uri: str = Field(alias="sourceUri")
    render_mode: str = Field(alias="renderMode")
    collection_mode: str = Field(alias="collectionMode")


class IngestionJob(BaseModel):
    model_config = {"populate_by_name": True}

    id: str
    crawl_source_id: str = Field(alias="crawlSourceId")
    organization_id: str = Field(alias="organizationId")
    service_id: Optional[str] = Field(default=None, alias="serviceId")
    job_type: str = Field(alias="jobType")
    job_status: JobStatus = Field(alias="status")
    job_stage: JobStage = Field(alias="jobStage")
    trigger_type: str = Field(alias="triggerType")
    attempt_count: int = Field(alias="attemptCount")
    error_code: Optional[str] = Field(default=None, alias="errorCode")


class CrawledPage(BaseModel):
    url: str
    title: str
    content: str
    links: list[str]
    depth: int
    parent_url: Optional[str] = None
    page_type: str = "general"
    content_hash: int = 0
    metadata: dict[str, Any] = {}


class TextChunk(BaseModel):
    chunk_key: str
    chunk_text: str
    chunk_order: int
    token_count: int
    source_url: str
    heading: Optional[str] = None
    metadata: dict[str, Any] = {}
