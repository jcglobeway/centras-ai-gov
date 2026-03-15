from __future__ import annotations

from enum import Enum
from typing import Optional

from pydantic import BaseModel


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
    id: str
    organization_id: str
    service_id: str
    name: str
    source_type: str
    source_uri: str
    render_mode: str
    collection_mode: str


class IngestionJob(BaseModel):
    id: str
    crawl_source_id: str
    organization_id: str
    service_id: Optional[str] = None
    job_type: str
    job_status: JobStatus
    job_stage: JobStage
    trigger_type: str
    attempt_count: int
    error_code: Optional[str] = None
