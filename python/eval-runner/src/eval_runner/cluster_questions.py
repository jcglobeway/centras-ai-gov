"""
의미 기반 질문 클러스터링 배치.

LLM으로 핵심 키워드를 추출하고, bge-m3 임베딩 + 코사인 유사도 2-stage로
유사 질문을 클러스터링해 DB에 저장한다.

실행: cluster-questions --org-id org_acc --days 7
     cluster-questions --org-id org_acc --days 7 --dry-run
"""
from __future__ import annotations

import json
import math
import os
import uuid
from datetime import date, timedelta
from typing import Optional

import numpy as np
import psycopg2
import psycopg2.extras
import typer
from eval_runner.embedding_provider import get_embedding_provider
from eval_runner.llm_provider import get_llm_provider

app = typer.Typer(help="의미 기반 질문 클러스터링 배치")


# ── DB helpers ────────────────────────────────────────────────────────────────

def get_db_conn():
    db_url = os.getenv("DATABASE_URL")
    if not db_url:
        raise RuntimeError("DATABASE_URL 환경변수가 필요합니다.")
    return psycopg2.connect(db_url)


def fetch_questions(conn, org_id: Optional[str], days: int) -> list[dict]:
    from_date = date.today() - timedelta(days=days)
    sql = """
        SELECT id, organization_id, question_text
        FROM questions
        WHERE created_at::date >= %s
    """
    params: list = [from_date]
    if org_id:
        sql += " AND organization_id = %s"
        params.append(org_id)
    sql += " ORDER BY created_at DESC LIMIT 2000"
    with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
        cur.execute(sql, params)
        return [dict(r) for r in cur.fetchall()]


# ── Keyword extraction ────────────────────────────────────────────────────────

def extract_keywords(llm, texts: list[str], batch_size: int = 100) -> list[str]:
    """LLM으로 키워드 추출. 배치 처리."""
    all_keywords: list[str] = []
    for i in range(0, len(texts), batch_size):
        batch = texts[i:i + batch_size]
        numbered = "\n".join(f"{j+1}. {t}" for j, t in enumerate(batch))
        prompt = (
            "다음 질문 목록에서 자주 등장하는 핵심 명사 키워드 20개를 추출해줘.\n"
            "조사/부사/동사/형용사는 제외하고 명사 위주로만. JSON 배열로만 응답해.\n"
            f"예시: [\"주차\", \"요금\", \"민원\"]\n\n질문 목록:\n{numbered}"
        )
        try:
            raw = llm.generate(prompt)
            start = raw.find("[")
            end = raw.rfind("]") + 1
            if start != -1 and end > start:
                keywords = json.loads(raw[start:end])
                all_keywords.extend([k for k in keywords if isinstance(k, str) and len(k) >= 2])
        except Exception as e:
            typer.echo(f"[keywords] 배치 {i//batch_size+1} 파싱 실패: {e}", err=True)
    # 중복 제거 (순서 유지)
    seen: set[str] = set()
    unique = []
    for k in all_keywords:
        if k not in seen:
            seen.add(k)
            unique.append(k)
    return unique


def count_keyword_occurrences(keywords: list[str], texts: list[str]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for kw in keywords:
        counts[kw] = sum(1 for t in texts if kw in t)
    return {k: v for k, v in counts.items() if v > 0}


def upsert_keyword_stats(conn, run_date: date, org_id: str, counts: dict[str, int], dry_run: bool) -> None:
    if dry_run:
        typer.echo(f"[dry-run] keyword_stats: {len(counts)}개 키워드")
        for kw, cnt in sorted(counts.items(), key=lambda x: -x[1])[:10]:
            typer.echo(f"  {kw}: {cnt}")
        return

    # 해당 run_date + org_id 기존 데이터 삭제 후 재삽입
    with conn.cursor() as cur:
        cur.execute(
            "DELETE FROM question_keyword_stats WHERE run_date = %s AND organization_id = %s",
            (run_date, org_id),
        )
        for kw, cnt in counts.items():
            cur.execute(
                """
                INSERT INTO question_keyword_stats (id, run_date, organization_id, keyword, count)
                VALUES (%s, %s, %s, %s, %s)
                """,
                (f"qks_{uuid.uuid4().hex[:8]}", run_date, org_id, kw, cnt),
            )
    conn.commit()
    typer.echo(f"[keywords] {len(counts)}개 저장 완료 (org={org_id}, date={run_date})")


# ── Similarity clustering ─────────────────────────────────────────────────────

def cosine_similarity_matrix(embeddings: np.ndarray) -> np.ndarray:
    norms = np.linalg.norm(embeddings, axis=1, keepdims=True)
    norms = np.where(norms == 0, 1e-9, norms)
    normed = embeddings / norms
    return normed @ normed.T


def find_candidate_pairs(sim_matrix: np.ndarray, threshold: float) -> list[tuple[int, int]]:
    n = sim_matrix.shape[0]
    pairs = []
    for i in range(n):
        for j in range(i + 1, n):
            if sim_matrix[i, j] >= threshold:
                pairs.append((i, j))
    return pairs


def verify_pairs_with_llm(llm, pairs: list[tuple[int, int]], texts: list[str], batch_size: int = 20) -> list[tuple[int, int]]:
    """LLM으로 후보 쌍의 의도 동일 여부 검증."""
    verified = []
    for i in range(0, len(pairs), batch_size):
        batch = pairs[i:i + batch_size]
        for (a, b) in batch:
            prompt = (
                "다음 두 질문이 같은 의도인가? yes 또는 no만 답해.\n"
                f"질문1: {texts[a]}\n"
                f"질문2: {texts[b]}"
            )
            try:
                ans = llm.generate(prompt).lower().strip()
                if ans.startswith("yes"):
                    verified.append((a, b))
            except Exception as e:
                typer.echo(f"[verify] 실패 ({a},{b}): {e}", err=True)
    return verified


def build_clusters(pairs: list[tuple[int, int]], n: int) -> list[list[int]]:
    """Union-Find로 클러스터 구성."""
    parent = list(range(n))

    def find(x: int) -> int:
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    def union(x: int, y: int) -> None:
        parent[find(x)] = find(y)

    for a, b in pairs:
        union(a, b)

    groups: dict[int, list[int]] = {}
    for i in range(n):
        root = find(i)
        groups.setdefault(root, []).append(i)

    return [g for g in groups.values() if len(g) >= 2]


def upsert_similarity_groups(
    conn,
    run_date: date,
    org_id: str,
    clusters: list[list[int]],
    texts: list[str],
    sim_matrix: np.ndarray,
    dry_run: bool,
) -> None:
    if dry_run:
        typer.echo(f"[dry-run] similarity_groups: {len(clusters)}개 클러스터")
        for cluster in clusters[:5]:
            rep = min(cluster, key=lambda i: len(texts[i]))
            typer.echo(f"  [{len(cluster)}개] {texts[rep][:50]}...")
        return

    with conn.cursor() as cur:
        cur.execute(
            "DELETE FROM question_similarity_groups WHERE run_date = %s AND organization_id = %s",
            (run_date, org_id),
        )
        for cluster in clusters:
            rep_idx = min(cluster, key=lambda i: len(texts[i]))
            rep_text = texts[rep_idx]
            # 클러스터 내 평균 유사도
            sims = [
                float(sim_matrix[a, b])
                for idx_a, a in enumerate(cluster)
                for b in cluster[idx_a + 1:]
            ]
            avg_sim = float(np.mean(sims)) if sims else 0.0
            sample = [texts[i] for i in cluster[:5]]
            cur.execute(
                """
                INSERT INTO question_similarity_groups
                  (id, run_date, organization_id, representative_text, question_count, avg_similarity, sample_texts)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    f"qsg_{uuid.uuid4().hex[:8]}",
                    run_date,
                    org_id,
                    rep_text,
                    len(cluster),
                    round(avg_sim, 4),
                    json.dumps(sample, ensure_ascii=False),
                ),
            )
    conn.commit()
    typer.echo(f"[clusters] {len(clusters)}개 저장 완료 (org={org_id}, date={run_date})")


# ── Question type classification ─────────────────────────────────────────────

def derive_question_types(llm, texts: list[str], sample_size: int = 80) -> list[str]:
    """LLM으로 기관 특화 질문 유형 레이블을 동적으로 도출한다."""
    sample = texts[:sample_size]
    numbered = "\n".join(f"{i+1}. {t}" for i, t in enumerate(sample))
    prompt = (
        "다음은 한 기관의 민원·서비스 질문 목록이다.\n"
        "이 질문들을 대표하는 5~7개의 주요 유형 레이블을 도출해줘.\n"
        "해당 기관 도메인에 특화된 구체적인 이름으로 만들어줘. "
        "(예: '예약/접수', '운영시간', '공연/전시' 등)\n"
        "JSON 배열로만 응답해. 예: [\"유형A\", \"유형B\"]\n\n"
        f"질문 목록:\n{numbered}"
    )
    try:
        raw = llm.generate(prompt)
        start = raw.find("[")
        end = raw.rfind("]") + 1
        if start != -1 and end > start:
            types = json.loads(raw[start:end])
            return [t for t in types if isinstance(t, str) and len(t) >= 2]
    except Exception as e:
        typer.echo(f"[type-derive] 파싱 실패: {e}", err=True)
    return ["정보 조회", "절차/방법", "민원/신고", "기타"]


def classify_questions(llm, type_labels: list[str], texts: list[str], batch_size: int = 30) -> dict[str, int]:
    """LLM으로 질문을 도출된 유형으로 분류해 카운트를 반환한다."""
    type_str = ", ".join(f'"{t}"' for t in type_labels)
    counts: dict[str, int] = {t: 0 for t in type_labels}
    counts["기타"] = 0

    for i in range(0, len(texts), batch_size):
        batch = texts[i:i + batch_size]
        numbered = "\n".join(f"{j+1}. {t}" for j, t in enumerate(batch))
        prompt = (
            f"다음 각 질문을 아래 유형 중 하나로 분류해줘: {type_str}\n"
            "해당 유형이 없으면 '기타'로 분류해.\n"
            "각 줄에 '번호: 유형' 형식으로만 답해. 설명 없이.\n"
            f"예:\n1: 운영시간\n2: 예약/접수\n\n질문 목록:\n{numbered}"
        )
        try:
            raw = llm.generate(prompt)
            for line in raw.strip().splitlines():
                line = line.strip()
                if ":" not in line:
                    continue
                _, type_part = line.split(":", 1)
                type_part = type_part.strip().strip('"').strip("'")
                if type_part in counts:
                    counts[type_part] += 1
                else:
                    counts["기타"] += 1
        except Exception as e:
            typer.echo(f"[type-classify] 배치 {i//batch_size+1} 실패: {e}", err=True)

    return counts


def upsert_type_stats(conn, run_date: date, org_id: str, counts: dict[str, int], dry_run: bool) -> None:
    if dry_run:
        typer.echo(f"[dry-run] question_type_stats: {len(counts)}개 유형")
        for t, c in sorted(counts.items(), key=lambda x: -x[1]):
            typer.echo(f"  {t}: {c}")
        return

    with conn.cursor() as cur:
        cur.execute(
            "DELETE FROM question_type_stats WHERE run_date = %s AND organization_id = %s",
            (run_date, org_id),
        )
        for type_label, count in counts.items():
            cur.execute(
                """
                INSERT INTO question_type_stats (id, run_date, organization_id, type_label, count)
                VALUES (%s, %s, %s, %s, %s)
                """,
                (f"qts_{uuid.uuid4().hex[:8]}", run_date, org_id, type_label, count),
            )
    conn.commit()
    typer.echo(f"[types] {len(counts)}개 유형 저장 완료 (org={org_id}, date={run_date})")


# ── Main ──────────────────────────────────────────────────────────────────────

@app.command()
def run(
    org_id: Optional[str] = typer.Option(None, "--org-id", help="조직 ID (미입력 시 전체)"),
    days: int = typer.Option(7, "--days", help="분석 대상 기간 (일)"),
    threshold: float = typer.Option(0.75, "--threshold", help="코사인 유사도 임계값"),
    dry_run: bool = typer.Option(False, "--dry-run", help="DB 저장 없이 결과만 출력"),
    cluster_limit: int = typer.Option(200, "--cluster-limit", help="클러스터링 대상 최대 질문 수 (임베딩 비용 제한)"),
) -> None:
    """LLM 키워드 추출 + 임베딩 기반 유사 질문 클러스터링 배치."""
    run_date = date.today()
    typer.echo(f"[cluster-questions] 시작 — org={org_id or '전체'}, days={days}, threshold={threshold}, dry_run={dry_run}")

    emb_provider = get_embedding_provider()
    llm_provider = get_llm_provider()

    conn = get_db_conn()
    try:
        questions = fetch_questions(conn, org_id, days)
        if not questions:
            typer.echo("[cluster-questions] 질문 데이터 없음. 종료.")
            return

        typer.echo(f"[cluster-questions] 질문 {len(questions)}개 조회")
        texts = [q["question_text"] for q in questions]

        # 조직별로 분리 처리
        orgs: dict[str, list[int]] = {}
        for i, q in enumerate(questions):
            orgs.setdefault(q["organization_id"], []).append(i)

        for oid, indices in orgs.items():
            org_texts = [texts[i] for i in indices]
            typer.echo(f"\n[org={oid}] 질문 {len(org_texts)}개 처리 중...")

            # Phase 1: 키워드 추출
            typer.echo("[keywords] LLM 키워드 추출 중...")
            keywords = extract_keywords(llm_provider, org_texts)
            counts = count_keyword_occurrences(keywords, org_texts)
            upsert_keyword_stats(conn, run_date, oid, counts, dry_run)

            # Phase 2: 유사 질문 클러스터링
            if len(org_texts) < 2:
                typer.echo("[clusters] 질문 수 부족 — 건너뜀")
                continue

            cluster_texts = org_texts[:cluster_limit]
            typer.echo(f"[clusters] 임베딩 생성 중... ({len(cluster_texts)}개, 최대 {cluster_limit})")
            embeddings_list = emb_provider.embed(cluster_texts)
            org_texts = cluster_texts  # 이하 처리도 동일 슬라이스
            embeddings = np.array(embeddings_list, dtype=np.float32)

            typer.echo("[clusters] 코사인 유사도 계산 중...")
            sim_matrix = cosine_similarity_matrix(embeddings)
            candidates = find_candidate_pairs(sim_matrix, threshold)
            typer.echo(f"[clusters] 후보 쌍 {len(candidates)}개 → LLM 검증 중...")

            if candidates:
                verified = verify_pairs_with_llm(llm_provider, candidates, org_texts)
                typer.echo(f"[clusters] 검증 통과 {len(verified)}개 쌍")
                clusters = build_clusters(verified, len(org_texts))
            else:
                clusters = []

            upsert_similarity_groups(conn, run_date, oid, clusters, org_texts, sim_matrix, dry_run)

            # Phase 3: 질문 유형 분류
            typer.echo("[types] LLM 질문 유형 도출 중...")
            type_labels = derive_question_types(llm_provider, org_texts)
            typer.echo(f"[types] 도출된 유형: {type_labels}")
            typer.echo(f"[types] 전체 질문 분류 중... ({len(org_texts)}개)")
            type_counts = classify_questions(llm_provider, type_labels, org_texts)
            upsert_type_stats(conn, run_date, oid, type_counts, dry_run)

    finally:
        conn.close()

    typer.echo("\n[cluster-questions] 완료")


def main() -> None:
    from pathlib import Path
    from dotenv import load_dotenv
    load_dotenv(Path(__file__).parents[3] / ".env", override=False)
    app()


if __name__ == "__main__":
    main()
