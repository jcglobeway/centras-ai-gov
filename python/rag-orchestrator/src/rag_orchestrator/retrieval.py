"""Vector search retrieval using Ollama bge-m3 and pgvector."""
from __future__ import annotations

import os
from typing import Optional

import httpx


def get_embedding(text: str, ollama_url: str = "http://localhost:11434") -> Optional[list[float]]:
    """Ollama bge-m3를 사용해 text embedding을 생성한다."""
    model = "bge-m3"

    try:
        response = httpx.post(
            f"{ollama_url}/api/embeddings",
            json={
                "model": model,
                "prompt": text,
            },
            timeout=10.0,
        )

        if response.status_code == 200:
            result = response.json()
            return result.get("embedding")
        else:
            return None

    except Exception:
        return None


def vector_search(
    query_text: str,
    top_k: int = 3,
    ollama_url: str = "http://localhost:11434",
    db_connection_string: Optional[str] = None,
) -> list[dict[str, str]]:
    """
    Query를 embedding하고 vector similarity search를 수행한다.

    현재는 stub 구현 (PostgreSQL 연결 필요).
    """
    # 1. Query embedding 생성
    query_embedding = get_embedding(query_text, ollama_url)

    if query_embedding is None:
        # Embedding 생성 실패 시 빈 결과
        return []

    # 2. Vector similarity search (PostgreSQL)
    # 실제 구현 시: psycopg2로 PostgreSQL 연결
    # SELECT chunk_text, embedding_vector <=> %s AS distance
    # FROM document_chunks
    # ORDER BY distance ASC
    # LIMIT %s;

    # 현재는 stub: 하드코딩된 chunk 반환
    return [
        {
            "chunk_id": "chunk_001",
            "chunk_text": "서울시 복지 혜택 신청 안내: 온라인 또는 주민센터를 방문하여 신청할 수 있습니다. 필요 서류는 신분증과 소득 증빙 서류입니다.",
            "distance": "0.15",
        },
        {
            "chunk_id": "chunk_002",
            "chunk_text": "처리 기간은 신청 후 약 7-14일 정도 소요됩니다. 승인 시 담당자가 연락을 드립니다.",
            "distance": "0.22",
        },
    ]
