from __future__ import annotations

import os
import time
from typing import Any

import httpx

TTL = 60  # 캐시 유효 시간 (초)

_cache: dict[str, dict[str, Any]] = {}
_cache_ts: dict[str, float] = {}

DEFAULT_RAG_CONFIG: dict[str, Any] = {
    "systemPrompt": (
        "당신은 공공기관 민원 안내 챗봇입니다. "
        "제공된 문서를 기반으로 정확하고 친절하게 답변하세요. "
        "답변을 찾을 수 없으면 모른다고 말하세요."
    ),
    "tone": "formal",
    "topK": int(os.getenv("HYBRID_SEARCH_TOP_K", "10")),
    "similarityThreshold": float(os.getenv("SIMILARITY_THRESHOLD", "0.7")),
    "rerankerEnabled": os.getenv("RERANKER_ENABLED", "false").lower() == "true",
    "llmModel": os.getenv("OLLAMA_MODEL", "qwen3:8b"),
    "llmTemperature": 0.3,
    "llmMaxTokens": 500,
}


def fetch_rag_config(org_id: str, admin_api_url: str) -> dict[str, Any]:
    now = time.monotonic()

    # TTL 캐시 히트
    if org_id in _cache and now - _cache_ts.get(org_id, 0) < TTL:
        return _cache[org_id]

    try:
        internal_key = os.getenv("INTERNAL_SERVICE_KEY", "")
        response = httpx.get(
            f"{admin_api_url}/admin/organizations/{org_id}/rag-config",
            headers={"X-Internal-Key": internal_key},
            timeout=3.0,
        )
        if response.status_code == 200:
            config = response.json()
            _cache[org_id] = config
            _cache_ts[org_id] = now
            return config
    except Exception:
        pass

    return DEFAULT_RAG_CONFIG
