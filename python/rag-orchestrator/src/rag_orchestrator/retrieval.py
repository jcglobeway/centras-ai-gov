"""Vector search retrieval using Ollama bge-m3 and pgvector."""
from __future__ import annotations

import os
from typing import Optional

import httpx
import psycopg2
import psycopg2.extras


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
    Query를 embedding하고 pgvector cosine similarity search를 수행한다.

    DB_URL 환경변수 또는 db_connection_string으로 PostgreSQL에 연결.
    Ollama가 없거나 DB 연결 실패 시 빈 리스트 반환.
    """
    # 1. Query embedding 생성
    query_embedding = get_embedding(query_text, ollama_url)

    if query_embedding is None:
        return []

    # 2. PostgreSQL 연결
    conn_str = db_connection_string or os.getenv("DATABASE_URL")
    if not conn_str:
        return []

    try:
        conn = psycopg2.connect(conn_str)
        cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)

        # pgvector cosine distance 연산자 (<=>)로 유사 청크 검색
        # documents.visibility_scope = 'public' 인 청크만 반환해 내부 문서 노출을 차단한다
        embedding_literal = "[" + ",".join(str(v) for v in query_embedding) + "]"
        cur.execute(
            """
            SELECT dc.id, dc.chunk_text, dc.embedding_vector <=> %s::vector AS distance
            FROM document_chunks dc
            JOIN documents d ON dc.document_id = d.id
            WHERE dc.embedding_vector IS NOT NULL
              AND d.visibility_scope = 'public'
            ORDER BY distance ASC
            LIMIT %s
            """,
            (embedding_literal, top_k),
        )

        rows = cur.fetchall()
        cur.close()
        conn.close()

        return [
            {
                "chunk_id": str(row["id"]),
                "chunk_text": str(row["chunk_text"]),
                "distance": str(row["distance"]),
            }
            for row in rows
            if row["chunk_text"]
        ]

    except Exception:
        return []
