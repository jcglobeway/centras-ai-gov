"""
Hybrid retrieval pipeline: vector search + BM25 + RRF fusion + optional reranking.

흐름: hybrid_search() → vector_search() + bm25_search() → rrf_fusion() → rerank()
환경변수:
  RERANKER_ENABLED=true   FlashRank cross-encoder 리랭킹 활성화 (기본 false)
  HYBRID_SEARCH_TOP_K=10  각 검색기의 후보 수 (기본 10)
"""
from __future__ import annotations

import os
from typing import Optional

import httpx
import psycopg2
import psycopg2.extras


# ── Embedding ─────────────────────────────────────────────────────────────────

def get_embedding(text: str, ollama_url: str = "http://localhost:11434") -> Optional[list[float]]:
    """Ollama bge-m3를 사용해 text embedding을 생성한다."""
    try:
        response = httpx.post(
            f"{ollama_url}/api/embed",
            json={"model": "bge-m3", "input": text},
            timeout=30.0,
        )
        if response.status_code == 200:
            result = response.json()
            embeddings = result.get("embeddings") or result.get("embedding")
            if isinstance(embeddings, list) and embeddings:
                return embeddings[0] if isinstance(embeddings[0], list) else embeddings
        return None
    except Exception:
        return None


# ── Vector Search ─────────────────────────────────────────────────────────────

def vector_search(
    query_text: str,
    top_k: int = 10,
    ollama_url: str = "http://localhost:11434",
    db_connection_string: Optional[str] = None,
) -> list[dict]:
    """pgvector cosine similarity search로 관련 청크를 검색한다."""
    query_embedding = get_embedding(query_text, ollama_url)
    if query_embedding is None:
        return []

    conn_str = db_connection_string or os.getenv("DATABASE_URL")
    if not conn_str:
        return []

    try:
        conn = psycopg2.connect(conn_str)
        cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        embedding_literal = "[" + ",".join(str(v) for v in query_embedding) + "]"
        cur.execute(
            """
            SELECT dc.id, dc.chunk_text,
                   dc.embedding_vector <=> %s::vector AS distance
            FROM document_chunks dc
            JOIN documents d ON dc.document_id = d.id
            WHERE dc.embedding_vector IS NOT NULL
              AND d.visibility_scope IN ('public', 'organization')
            ORDER BY distance ASC
            LIMIT %s
            """,
            (embedding_literal, top_k),
        )
        rows = cur.fetchall()
        cur.close()
        conn.close()
        return [
            {"chunk_id": str(r["id"]), "chunk_text": str(r["chunk_text"]), "distance": float(r["distance"])}
            for r in rows if r["chunk_text"]
        ]
    except Exception:
        return []


# ── BM25 Search ───────────────────────────────────────────────────────────────

def bm25_search(
    query_text: str,
    top_k: int = 10,
    db_connection_string: Optional[str] = None,
) -> list[dict]:
    """
    DB에서 청크를 로드해 BM25 키워드 검색을 수행한다.

    공백 기반 토크나이저를 사용하므로 한국어 어절 단위 매칭이 가능하다.
    형태소 분석이 없어 완벽하지 않지만 벡터 검색의 보완재로 효과적이다.
    """
    conn_str = db_connection_string or os.getenv("DATABASE_URL")
    if not conn_str:
        return []

    try:
        from rank_bm25 import BM25Okapi

        conn = psycopg2.connect(conn_str)
        cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
        cur.execute(
            """
            SELECT dc.id, dc.chunk_text
            FROM document_chunks dc
            JOIN documents d ON dc.document_id = d.id
            WHERE dc.chunk_text IS NOT NULL
              AND d.visibility_scope IN ('public', 'organization')
            """,
        )
        rows = cur.fetchall()
        cur.close()
        conn.close()

        if not rows:
            return []

        corpus = [str(r["chunk_text"]) for r in rows]
        chunk_ids = [str(r["id"]) for r in rows]
        tokenized_corpus = [text.split() for text in corpus]

        bm25 = BM25Okapi(tokenized_corpus)
        scores = bm25.get_scores(query_text.split())

        ranked = sorted(
            zip(chunk_ids, corpus, scores),
            key=lambda x: x[2],
            reverse=True,
        )[:top_k]

        return [
            {"chunk_id": cid, "chunk_text": text, "bm25_score": float(score)}
            for cid, text, score in ranked
            if score > 0
        ]
    except Exception:
        return []


# ── RRF Fusion ────────────────────────────────────────────────────────────────

def rrf_fusion(
    vector_results: list[dict],
    bm25_results: list[dict],
    k: int = 60,
) -> list[dict]:
    """
    Reciprocal Rank Fusion으로 두 검색 결과를 합산한다.

    score(d) = sum(1 / (k + rank_i)) — 각 리스트에서 문서의 순위 기반 점수 합산.
    """
    scores: dict[str, float] = {}
    texts: dict[str, str] = {}

    for rank, doc in enumerate(vector_results, 1):
        cid = doc["chunk_id"]
        scores[cid] = scores.get(cid, 0.0) + 1.0 / (k + rank)
        texts[cid] = doc["chunk_text"]

    for rank, doc in enumerate(bm25_results, 1):
        cid = doc["chunk_id"]
        scores[cid] = scores.get(cid, 0.0) + 1.0 / (k + rank)
        texts[cid] = doc["chunk_text"]

    fused = sorted(scores.items(), key=lambda x: x[1], reverse=True)
    return [{"chunk_id": cid, "chunk_text": texts[cid], "rrf_score": score} for cid, score in fused]


# ── Reranking ─────────────────────────────────────────────────────────────────

def rerank(
    query: str,
    candidates: list[dict],
    top_n: int = 5,
) -> list[dict]:
    """
    FlashRank cross-encoder로 후보 청크를 재정렬한다.

    RERANKER_ENABLED=false이면 candidates[:top_n]을 그대로 반환한다.
    첫 실행 시 ms-marco-MiniLM-L-12-v2 모델(~33MB)을 다운로드한다.
    """
    try:
        from flashrank import Ranker, RerankRequest
        ranker = Ranker(model_name="ms-marco-MiniLM-L-12-v2", cache_dir="/tmp/flashrank")
        passages = [{"id": i, "text": c["chunk_text"]} for i, c in enumerate(candidates)]
        request = RerankRequest(query=query, passages=passages)
        results = ranker.rerank(request)
        reranked_ids = [r["id"] for r in results[:top_n]]
        return [candidates[i] for i in reranked_ids]
    except Exception:
        return candidates[:top_n]


# ── Hybrid Search (메인 진입점) ────────────────────────────────────────────────

def hybrid_search(
    query_text: str,
    top_k: int = 10,
    ollama_url: str = "http://localhost:11434",
    db_url: Optional[str] = None,
    reranker_enabled: bool = False,
    final_top_n: int = 5,
) -> list[dict]:
    """
    3단계 하이브리드 검색 파이프라인.

    1. vector_search: pgvector cosine similarity (top_k)
    2. bm25_search: BM25 키워드 검색 (top_k)
    3. rrf_fusion: RRF 점수 합산
    4. rerank: FlashRank cross-encoder (reranker_enabled=True 시)
    5. final_top_n개 반환

    결과 dict에는 chunk_id, chunk_text, distance(호환용) 포함.
    """
    vec_results = vector_search(query_text, top_k=top_k, ollama_url=ollama_url, db_connection_string=db_url)
    bm25_results = bm25_search(query_text, top_k=top_k, db_connection_string=db_url)

    if not vec_results and not bm25_results:
        return []

    fused = rrf_fusion(vec_results, bm25_results)

    if reranker_enabled and fused:
        final = rerank(query_text, fused, top_n=final_top_n)
    else:
        final = fused[:final_top_n]

    # distance 필드를 RRF score 기반으로 채워 기존 호환성 유지
    return [
        {
            "chunk_id": doc["chunk_id"],
            "chunk_text": doc["chunk_text"],
            "distance": str(1.0 - doc.get("rrf_score", 0.0)),
        }
        for doc in final
    ]
