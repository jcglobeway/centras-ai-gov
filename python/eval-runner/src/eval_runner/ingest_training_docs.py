"""
훈련 원천데이터 임베딩 인제스터.

TS_*.zip의 상담 대화 원문을 청크로 분할하고 Ollama bge-m3로 임베딩해
documents + document_chunks 테이블에 직접 삽입한다.

실행:
  ingest-training-docs --zip /path/to/TS_국립아시아문화전당.zip \\
                       --org-id org_acc --service-id svc_acc_chatbot \\
                       --limit 100
"""
from __future__ import annotations

import os
import uuid
from pathlib import Path
from typing import Optional

import httpx
import psycopg2
import psycopg2.extras
import typer

from eval_runner.training_data_loader import iter_source_docs

app = typer.Typer(help="훈련 원천데이터 pgvector 임베딩 인제스터")


@app.command()
def run(
    zip_path: Path = typer.Option(..., "--zip", help="TS_*.zip 경로"),
    org_id: str = typer.Option("org_acc", "--org-id"),
    service_id: str = typer.Option("svc_acc_chatbot", "--service-id"),
    limit: int = typer.Option(200, "--limit", help="처리할 최대 청크 수 (0=전체)"),
    chunk_size: int = typer.Option(800, "--chunk-size", help="청크 최대 문자 수"),
    dry_run: bool = typer.Option(False, "--dry-run", help="DB 삽입 없이 청크만 출력"),
) -> None:
    """원천데이터 zip → pgvector 임베딩 삽입."""
    ollama_url = os.getenv("OLLAMA_URL", "http://jcg-office.tailedf4dc.ts.net:11434")
    db_url = os.getenv("DATABASE_URL")

    if not dry_run and not db_url:
        typer.echo("[ingest] DATABASE_URL 환경변수 필요", err=True)
        raise typer.Exit(1)

    typer.echo(f"[ingest] {zip_path.name} → org={org_id}, limit={limit}")

    conn = psycopg2.connect(db_url) if not dry_run else None
    if conn:
        conn.autocommit = False

    inserted_docs: dict[str, str] = {}
    chunk_count = 0

    try:
        for chunk in iter_source_docs(zip_path, org_id, service_id, chunk_size, limit):
            doc_key = f"{org_id}_{chunk['source_id']}"

            if dry_run:
                typer.echo(f"[dry] {doc_key}[{chunk['chunk_idx']}]: {chunk['chunk_text'][:60]}...")
                chunk_count += 1
                continue

            # documents 테이블에 부모 레코드 삽입 (최초 청크만)
            if doc_key not in inserted_docs:
                doc_id = _ensure_document(conn, chunk, org_id)
                inserted_docs[doc_key] = doc_id
            else:
                doc_id = inserted_docs[doc_key]

            # Ollama bge-m3 임베딩 호출
            embedding = _embed(ollama_url, chunk["chunk_text"])
            if embedding is None:
                typer.echo(f"[ingest] 임베딩 실패: {doc_key}[{chunk['chunk_idx']}]", err=True)
                continue

            # document_chunks 삽입
            _insert_chunk(conn, doc_id, chunk, embedding)
            chunk_count += 1

            if chunk_count % 10 == 0:
                conn.commit()
                typer.echo(f"[ingest] {chunk_count}청크 처리됨 ({len(inserted_docs)}문서)")

        if conn:
            conn.commit()
        typer.echo(f"[ingest] 완료 — {chunk_count}청크, {len(inserted_docs)}문서")

    except Exception as e:
        if conn:
            conn.rollback()
        typer.echo(f"[ingest] 오류: {e}", err=True)
        raise typer.Exit(1)
    finally:
        if conn:
            conn.close()


# ── 헬퍼 함수 ────────────────────────────────────────────────────────────────

def _ensure_document(conn, chunk: dict, org_id: str) -> str:
    """documents 테이블에 레코드가 없으면 삽입하고 id 반환."""
    doc_id = f"doc_ts_{chunk['source_id']}_{uuid.uuid4().hex[:6]}"
    category = chunk.get("consulting_category") or "일반상담"
    date_str = chunk.get("consulting_date") or "20240101"
    if len(date_str) == 8:
        date_str = f"{date_str[:4]}-{date_str[4:6]}-{date_str[6:8]}"

    cur = conn.cursor()
    cur.execute(
        """
        INSERT INTO documents
          (id, organization_id, document_type, title, source_uri,
           version_label, published_at, ingestion_status, index_status,
           visibility_scope, last_ingested_at, last_indexed_at, created_at, updated_at)
        VALUES (%s,%s,'consultation',%s,%s,'v1.0',%s,
                'completed','indexed','organization',
                NOW(),NOW(),NOW(),NOW())
        ON CONFLICT (id) DO NOTHING
        """,
        (
            doc_id, org_id,
            f"상담 원문 {chunk['source_id']} ({category})",
            f"ts://{org_id}/{chunk['source_id']}",
            date_str,
        ),
    )
    cur.close()
    return doc_id


def _insert_chunk(conn, doc_id: str, chunk: dict, embedding: list[float]) -> None:
    """document_chunks 테이블에 청크 + 임베딩 삽입.

    스키마(V016+V018): id, document_id, chunk_key, chunk_text,
                       chunk_order, token_count, embedding_vector(vector)
    """
    chunk_id = f"chunk_{uuid.uuid4().hex[:12]}"
    chunk_key = f"chunk_{chunk['chunk_idx']}"
    embedding_str = "[" + ",".join(f"{v:.6f}" for v in embedding) + "]"
    cur = conn.cursor()
    cur.execute(
        """
        INSERT INTO document_chunks
          (id, document_id, chunk_key, chunk_text, chunk_order,
           token_count, embedding_vector, created_at)
        VALUES (%s, %s, %s, %s, %s,
                %s, %s::vector, NOW())
        ON CONFLICT (id) DO NOTHING
        """,
        (
            chunk_id, doc_id, chunk_key,
            chunk["chunk_text"], chunk["chunk_idx"],
            len(chunk["chunk_text"]) // 4,  # 대략적인 token_count
            embedding_str,
        ),
    )
    cur.close()


def _embed(ollama_url: str, text: str) -> Optional[list[float]]:
    """Ollama bge-m3 임베딩 API 호출."""
    try:
        resp = httpx.post(
            f"{ollama_url}/api/embed",
            json={"model": "bge-m3", "input": text},
            timeout=30.0,
        )
        resp.raise_for_status()
        data = resp.json()
        # Ollama /api/embed 응답: {"embeddings": [[...]]}
        embeddings = data.get("embeddings") or data.get("embedding")
        if isinstance(embeddings, list) and embeddings:
            emb = embeddings[0] if isinstance(embeddings[0], list) else embeddings
            return emb
    except Exception as e:
        typer.echo(f"[embed] 오류: {e}", err=True)
    return None


def main() -> None:
    from dotenv import load_dotenv
    load_dotenv()
    app()


if __name__ == "__main__":
    main()
