"""
공공 민원 Q&A 데이터 전처리 스크립트.

TL/VL 질의응답 ZIP 파일(3기관 × 2분할 = 6개)을 파싱해
V027 마이그레이션용 SQL(documents, document_versions, ingestion_jobs)과
eval_questions.json(100건 샘플)을 생성한다.

실행: python -m eval_runner.ingestion_prep
     python -m eval_runner.ingestion_prep --sample 100 --max-docs-per-zip 50
"""
from __future__ import annotations

import json
import random
import re
import uuid
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

import typer

app = typer.Typer(help="공공 Q&A 데이터 전처리 및 SQL 생성")

# ── 경로 기본값 ────────────────────────────────────────────────────────────────

_PROJECT_ROOT = Path(__file__).parents[4]  # centras-ai-gov/
DATA_BASE = _PROJECT_ROOT / "data" / "3.개방데이터" / "1.데이터"
OUTPUT_DIR = _PROJECT_ROOT / "apps" / "admin-api" / "src" / "main" / "resources" / "db" / "migration"
EVAL_OUTPUT = _PROJECT_ROOT / "python" / "eval-runner" / "eval_questions.json"

# ── 기관 매핑 ─────────────────────────────────────────────────────────────────

# ZIP 파일 이름 키워드 → (org_id, service_id, crawl_source_id)
ZIP_ORG_MAP = {
    "지방행정기관": ("org_seoul_120", "svc_welfare", "crawl_src_001"),
    "중앙행정기관": ("org_busan_220", "svc_faq",     "crawl_src_002"),
    "국립아시아문화전당": ("org_seoul_120", "svc_welfare", "crawl_src_001"),
}

CATEGORY_TASK_MAP = {
    "의문사형": "factoid",
    "절차안내형": "procedure",
    "자격확인형": "eligibility",
    "비교형": "comparison",
    "복합형": "complex",
}

# ── 도메인 모델 ───────────────────────────────────────────────────────────────

@dataclass
class ParsedDoc:
    doc_id: str
    org_id: str
    service_id: str
    crawl_source_id: str
    title: str
    body: str
    category: str
    published_date: str  # YYYY-MM-DD
    source_uri: str

@dataclass
class EvalItem:
    question: str
    context: str
    ground_truth: str
    task_category: str
    org_id: str
    service_id: str
    doc_id: str


# ── ZIP 파싱 ──────────────────────────────────────────────────────────────────

def _parse_date(raw: str) -> str:
    """YYYYMMDD → YYYY-MM-DD."""
    raw = re.sub(r"\D", "", raw)
    if len(raw) == 8:
        return f"{raw[:4]}-{raw[4:6]}-{raw[6:8]}"
    return "2024-01-01"


def _extract_title(content: str) -> str:
    m = re.search(r"제목\s*[:：]\s*(.+)", content)
    raw = m.group(1).strip() if m else content[:60].strip()
    # 개행·탭·캐리지리턴 제거 (SQL 문자열 안전)
    raw = re.sub(r"[\r\n\t]", " ", raw)
    raw = re.sub(r"_x000D_", "", raw)
    return raw[:100]


def _short_id() -> str:
    return uuid.uuid4().hex[:8]


def parse_zip(
    zip_path: Path,
    org_id: str,
    service_id: str,
    crawl_source_id: str,
    max_docs: int,
) -> tuple[list[ParsedDoc], list[EvalItem]]:
    docs: list[ParsedDoc] = []
    evals: list[EvalItem] = []

    with zipfile.ZipFile(zip_path) as zf:
        names = [n for n in zf.namelist() if n.endswith(".json")]
        random.shuffle(names)
        names = names[:max_docs]

        for name in names:
            try:
                with zf.open(name) as f:
                    raw = json.load(f)
            except Exception:
                continue

            if isinstance(raw, list):
                item = raw[0] if raw else None
            else:
                item = raw
            if not item:
                continue

            content = item.get("consulting_content", "")
            category = item.get("consulting_category", "기타")
            date_raw = item.get("consulting_date", "20240101")
            source_id = item.get("source_id", _short_id())

            doc_id = f"doc_pub_{_short_id()}"
            docs.append(ParsedDoc(
                doc_id=doc_id,
                org_id=org_id,
                service_id=service_id,
                crawl_source_id=crawl_source_id,
                title=_extract_title(content),
                body=content,
                category=category,
                published_date=_parse_date(date_raw),
                source_uri=f"https://data.go.kr/source/{source_id}",
            ))

            # Q&A 쌍 수집
            for inst_block in item.get("instructions", []):
                for qa in inst_block.get("data", []):
                    q = qa.get("instruction", "").strip()
                    ctx = qa.get("input", content).strip()
                    ans = qa.get("output", "").strip()
                    task_cat = qa.get("task_category", "기타")
                    if q and ans:
                        evals.append(EvalItem(
                            question=q,
                            context=ctx,
                            ground_truth=ans,
                            task_category=task_cat,
                            org_id=org_id,
                            service_id=service_id,
                            doc_id=doc_id,
                        ))

    return docs, evals


# ── SQL 생성 ──────────────────────────────────────────────────────────────────

def _esc(s: str) -> str:
    """SQL 문자열 이스케이프 — 개행·탭·캐리지리턴 제거 후 단따옴표 이스케이프."""
    s = s.replace("\r", "").replace("\n", " ").replace("\t", " ")
    s = re.sub(r"_x000D_", "", s)  # Excel XML 캐리지리턴 인코딩 제거
    return s.replace("'", "''")


def generate_sql(
    docs: list[ParsedDoc],
    existing_answer_ids: list[str],
) -> str:
    lines: list[str] = [
        "-- V027: 공공 민원 문서 메타데이터 + LLM 메트릭 보강",
        "-- generated by ingestion_prep.py",
        "",
    ]

    if docs:
        lines.append("-- ── documents ───────────────────────────────────────────────")
        doc_rows = []
        for d in docs:
            doc_rows.append(
                f"('{d.doc_id}', '{d.org_id}', '{d.service_id}', 'qa_pair', "
                f"'{_esc(d.title)}', '{d.source_uri}', NULL, "
                f"'{d.published_date}', 'completed', 'indexed', 'organization', "
                f"'{d.published_date} 00:00:00', '{d.published_date} 00:00:00', '{d.published_date} 00:00:00')"
            )
        lines.append("INSERT INTO documents (id, organization_id, service_id, document_type, title, source_uri,")
        lines.append("  version_label, published_at, ingestion_status, index_status, visibility_scope,")
        lines.append("  last_ingested_at, last_indexed_at, created_at) VALUES")
        lines.append(",\n".join(doc_rows) + ";")
        lines.append("")

        lines.append("-- ── document_versions ───────────────────────────────────────")
        ver_rows = []
        for d in docs:
            ver_id = f"docver_{_short_id()}"
            ver_rows.append(
                f"('{ver_id}', '{d.doc_id}', 1, '초기 수집', '{d.published_date} 00:00:00')"
            )
        lines.append("INSERT INTO document_versions (id, document_id, version_number, change_note, created_at) VALUES")
        lines.append(",\n".join(ver_rows) + ";")
        lines.append("")

        lines.append("-- ── ingestion_jobs (succeeded, per doc) ─────────────────────")
        job_rows = []
        for d in docs:
            job_id = f"ing_job_pub_{_short_id()}"
            job_rows.append(
                f"('{job_id}', '{d.org_id}', '{d.service_id}', '{d.crawl_source_id}', '{d.doc_id}', "
                f"'crawl', 'complete', 'succeeded', 'python_worker', 'manual', 1, NULL, "
                f"'{d.published_date} 00:00:00', '{d.published_date} 00:01:00', "
                f"'{d.published_date} 00:10:00', '{d.published_date} 00:00:00')"
            )
        lines.append("INSERT INTO ingestion_jobs (id, organization_id, service_id, crawl_source_id, document_id,")
        lines.append("  job_type, job_stage, status, runner_type, trigger_type, attempt_count, error_code,")
        lines.append("  requested_at, started_at, finished_at, created_at) VALUES")
        lines.append(",\n".join(job_rows) + ";")
        lines.append("")

    # LLM 메트릭 UPDATE (기존 answers 7건에 모델명/토큰/비용 채우기)
    if existing_answer_ids:
        lines.append("-- ── answers LLM 메트릭 UPDATE ──────────────────────────────")
        model_variants = [
            ("gpt-4o-mini", 0.00015, 0.00060),
            ("gpt-4o",      0.00250, 0.01000),
            ("gpt-4o-mini", 0.00015, 0.00060),
        ]
        for i, ans_id in enumerate(existing_answer_ids):
            model, input_price, output_price = model_variants[i % len(model_variants)]
            input_tokens = 820 + i * 37
            output_tokens = 210 + i * 18
            cost = round((input_tokens / 1_000_000) * input_price * 1_000_000
                         + (output_tokens / 1_000_000) * output_price * 1_000_000, 6)
            # 실제 계산: (tokens/1M) * price_per_1M_token
            cost = round(input_tokens * input_price / 1000 + output_tokens * output_price / 1000, 6)
            lines.append(
                f"UPDATE answers SET model_name='{model}', input_tokens={input_tokens}, "
                f"output_tokens={output_tokens}, estimated_cost_usd={cost} "
                f"WHERE id='{ans_id}';"
            )
        lines.append("")

    return "\n".join(lines)


# ── 샘플링 ────────────────────────────────────────────────────────────────────

TASK_CATEGORIES = ["의문사형", "절차안내형", "자격확인형", "비교형", "복합형"]
SAMPLE_COUNTS   = [30, 30, 20, 10, 10]  # 합계 100

def sample_eval_items(all_evals: list[EvalItem], total: int) -> list[EvalItem]:
    by_cat: dict[str, list[EvalItem]] = {cat: [] for cat in TASK_CATEGORIES}
    other: list[EvalItem] = []
    for e in all_evals:
        matched = False
        for cat in TASK_CATEGORIES:
            if cat in e.task_category:
                by_cat[cat].append(e)
                matched = True
                break
        if not matched:
            other.append(e)

    sampled: list[EvalItem] = []
    for cat, count in zip(TASK_CATEGORIES, SAMPLE_COUNTS):
        pool = by_cat[cat]
        random.shuffle(pool)
        sampled.extend(pool[:count])

    # 부족분은 other로 채움
    deficit = total - len(sampled)
    if deficit > 0:
        random.shuffle(other)
        sampled.extend(other[:deficit])

    random.shuffle(sampled)
    return sampled[:total]


# ── 메인 커맨드 ───────────────────────────────────────────────────────────────

@app.command()
def main(
    sample: int = typer.Option(100, help="eval_questions.json 샘플 건수"),
    max_docs_per_zip: int = typer.Option(50, help="ZIP당 최대 문서 수 (SQL 크기 제한)"),
    seed: int = typer.Option(42, help="랜덤 시드"),
    output_sql: Optional[Path] = typer.Option(None, help="SQL 출력 경로 (기본: V027 migration)"),
    output_eval: Optional[Path] = typer.Option(None, help="eval JSON 출력 경로"),
    dry_run: bool = typer.Option(False, help="파일을 쓰지 않고 통계만 출력"),
) -> None:
    """공공 Q&A ZIP을 파싱해 V027 SQL과 eval_questions.json을 생성한다."""
    random.seed(seed)

    # 기존 answers ID (V024 + V026 기준)
    existing_answer_ids = [
        "ans_004", "ans_005", "ans_006", "ans_007",
        "ans_008", "ans_009", "ans_010",
    ]

    zip_targets: list[tuple[Path, str, str, str]] = []
    for split in ("Training", "Validation"):
        label_dir = DATA_BASE / split / "02.라벨링데이터"
        if not label_dir.exists():
            typer.echo(f"[warn] 경로 없음: {label_dir}", err=True)
            continue
        for keyword, (org_id, svc_id, crawl_id) in ZIP_ORG_MAP.items():
            prefix = "TL" if split == "Training" else "VL"
            pattern = f"{prefix}_{keyword}_질의응답.zip"
            matched = list(label_dir.glob(pattern))
            if not matched:
                typer.echo(f"[warn] ZIP 없음: {label_dir / pattern}", err=True)
                continue
            zip_targets.append((matched[0], org_id, svc_id, crawl_id))

    all_docs: list[ParsedDoc] = []
    all_evals: list[EvalItem] = []

    for zip_path, org_id, svc_id, crawl_id in zip_targets:
        typer.echo(f"[prep] 파싱: {zip_path.name}")
        docs, evals = parse_zip(zip_path, org_id, svc_id, crawl_id, max_docs_per_zip)
        all_docs.extend(docs)
        all_evals.extend(evals)
        typer.echo(f"       → docs={len(docs)}, qa_pairs={len(evals)}")

    typer.echo(f"[prep] 전체 문서={len(all_docs)}, Q&A 쌍={len(all_evals)}")

    sampled = sample_eval_items(all_evals, sample)
    typer.echo(f"[prep] eval 샘플={len(sampled)}건")

    if dry_run:
        typer.echo("[dry-run] 파일 생성 건너뜀")
        return

    # SQL 출력
    sql_path = output_sql or (OUTPUT_DIR / "V027__seed_public_documents_and_llm_metrics.sql")
    sql_content = generate_sql(all_docs, existing_answer_ids)
    sql_path.write_text(sql_content, encoding="utf-8")
    typer.echo(f"[prep] SQL 저장: {sql_path} ({len(sql_content):,} bytes)")

    # eval_questions.json 출력
    eval_path = output_eval or EVAL_OUTPUT
    eval_data = [
        {
            "question": e.question,
            "context": e.context,
            "ground_truth": e.ground_truth,
            "task_category": e.task_category,
            "org_id": e.org_id,
            "service_id": e.service_id,
            "doc_id": e.doc_id,
        }
        for e in sampled
    ]
    eval_path.write_text(json.dumps(eval_data, ensure_ascii=False, indent=2), encoding="utf-8")
    typer.echo(f"[prep] eval JSON 저장: {eval_path}")


if __name__ == "__main__":
    app()
