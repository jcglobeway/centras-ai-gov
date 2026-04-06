"""
Hybrid retrieval pipeline: vector search + BM25 + RRF fusion + optional reranking.

흐름: hybrid_search() → vector_search() + bm25_search() → rrf_fusion() → rerank()
환경변수:
  RERANKER_ENABLED=true   FlashRank cross-encoder 리랭킹 활성화 (기본 false)
  HYBRID_SEARCH_TOP_K=10  각 검색기의 후보 수 (기본 10)
"""
from __future__ import annotations

import hashlib
import os
from datetime import datetime
from typing import Optional

import httpx
import psycopg2
import psycopg2.extras


# ── 형태소 분석기 (선택적) ────────────────────────────────────────────────────
# kiwipiepy가 설치돼 있으면 사용하고, 없으면 공백 분리로 폴백한다.

try:
    from kiwipiepy import Kiwi
    _kiwi = Kiwi()

    def _tokenize(text: str) -> list[str]:
        """kiwipiepy 형태소 분석 — 명사/동사/형용사 어근만 추출."""
        tokens = []
        for token in _kiwi.tokenize(text):
            # 내용어(NNG, NNP, VV, VA, XR)만 사용해 노이즈 제거
            if token.tag[:2] in ("NN", "VV", "VA", "XR", "SL"):
                tokens.append(token.form)
        return tokens if tokens else text.split()

except ImportError:
    # kiwipiepy 미설치 시 공백 분리 폴백
    def _tokenize(text: str) -> list[str]:  # type: ignore[misc]
        return text.split()


# ── BM25 corpus 캐시 ──────────────────────────────────────────────────────────
# corpus 내용의 해시가 변하면 자동으로 재빌드한다.

_bm25_cache: dict = {
    "hash": None,
    "bm25": None,
    "chunk_ids": [],
    "corpus": [],
}


def _load_bm25(conn_str: str):
    """
    DB에서 청크를 로드하고 BM25 인덱스를 빌드한다.

    corpus 해시가 이전과 같으면 캐시를 그대로 반환해 DB 왕복을 줄인다.
    """
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
        ORDER BY dc.id
        """,
    )
    rows = cur.fetchall()
    cur.close()
    conn.close()

    if not rows:
        return None, [], []

    corpus = [str(r["chunk_text"]) for r in rows]
    chunk_ids = [str(r["id"]) for r in rows]

    # 간단한 해시: 청크 수 + 마지막 ID
    corpus_hash = hashlib.md5(f"{len(corpus)}:{chunk_ids[-1]}".encode()).hexdigest()

    if _bm25_cache["hash"] == corpus_hash:
        return _bm25_cache["bm25"], _bm25_cache["chunk_ids"], _bm25_cache["corpus"]

    tokenized = [_tokenize(text) for text in corpus]
    bm25 = BM25Okapi(tokenized)

    _bm25_cache["hash"] = corpus_hash
    _bm25_cache["bm25"] = bm25
    _bm25_cache["chunk_ids"] = chunk_ids
    _bm25_cache["corpus"] = corpus

    return bm25, chunk_ids, corpus


# ── Embedding ─────────────────────────────────────────────────────────────────

def get_embedding(text: str, ollama_url: str = "http://localhost:11434") -> Optional[list[float]]:
    """Ollama bge-m3를 사용해 text embedding을 생성한다."""
    try:
        response = httpx.post(
            f"{ollama_url}/api/embed",
            json={"model": "bge-m3", "input": text},
            timeout=30.0,
            verify=False,
        )
        if response.status_code != 200:
            return None
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
    BM25 키워드 검색을 수행한다.

    kiwipiepy가 설치된 경우 형태소 분석으로 토크나이징하고,
    없으면 공백 분리로 폴백한다.
    corpus는 모듈 레벨 캐시로 유지해 매 요청마다 DB를 재로드하지 않는다.
    """
    conn_str = db_connection_string or os.getenv("DATABASE_URL")
    if not conn_str:
        return []

    try:
        bm25, chunk_ids, corpus = _load_bm25(conn_str)
        if bm25 is None:
            return []

        query_tokens = _tokenize(query_text)
        scores = bm25.get_scores(query_tokens)

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

_flashrank_ranker = None


def _get_ranker():
    # 프로세스 내 최초 1회만 모델을 로드한다
    global _flashrank_ranker
    if _flashrank_ranker is None:
        from flashrank import Ranker
        _flashrank_ranker = Ranker(model_name="ms-marco-MiniLM-L-12-v2", cache_dir="/tmp/flashrank")
    return _flashrank_ranker


def rerank(
    query: str,
    candidates: list[dict],
    top_n: int = 5,
    ollama_url: str = "http://localhost:11434",
) -> list[dict]:
    """
    FlashRank cross-encoder로 후보 청크를 재정렬한다.

    Ranker는 모듈 레벨 싱글톤으로 유지해 최초 1회만 로드한다.
    실패 시 원래 순서 그대로 top_n개를 반환한다.
    """
    try:
        from flashrank import RerankRequest
        ranker = _get_ranker()
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
    2. bm25_search: BM25 키워드 검색 — kiwipiepy 형태소 분석 (top_k)
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
        final = rerank(query_text, fused, top_n=final_top_n, ollama_url=ollama_url)
    else:
        final = fused[:final_top_n]

    # RRF 점수를 [0, 1] confidence로 정규화해 distance 필드에 담는다.
    # 이론적 최댓값: 두 리스트 모두 1위일 때 2/(k+1) = 2/61 ≈ 0.0328
    # distance = 1 - (rrf_score / max_rrf) → confidence = 1 - distance
    _max_rrf = 2.0 / (60 + 1)
    return [
        {
            "chunk_id": doc["chunk_id"],
            "chunk_text": doc["chunk_text"],
            "distance": str(max(0.0, 1.0 - doc.get("rrf_score", 0.0) / _max_rrf)),
        }
        for doc in final
    ]
