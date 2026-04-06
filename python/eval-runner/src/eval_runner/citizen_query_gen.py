"""
시민 질의 생성기.

TS_*.zip의 consulting_content에서 고객 발화를 추출하고,
Ollama로 독립적인 민원 질문으로 변환해 citizen_questions.json을 생성한다.

실행:
  citizen-query-gen \\
    --zip /path/to/TS_국립아시아문화전당.zip \\
    --limit 200 \\
    --output citizen_questions.json
"""
from __future__ import annotations

import json
import os
import re
import zipfile
from pathlib import Path
from typing import Optional

import httpx
import typer

app = typer.Typer(help="TS 원천데이터 → 시민 민원 질의 생성")

_DATA_ROOT = Path(__file__).parents[4] / "data" / "3.개방데이터" / "1.데이터" / "Training" / "01.원천데이터"
DEFAULT_ZIP    = _DATA_ROOT / "TS_국립아시아문화전당.zip"
DEFAULT_OUTPUT = Path(__file__).parents[4] / "python" / "eval-runner" / "citizen_questions.json"

# Ollama 변환 프롬프트
_REWRITE_PROMPT = """\
다음은 국립아시아문화전당 고객센터에 전화한 시민의 발화입니다.

이 발화를 자연스러운 민원 질문 하나로 바꿔주세요.
규칙:
- 문맥 없이도 이해되는 독립적인 질문이어야 합니다
- 존댓말로 작성하세요
- "네", "아", "감사합니다", "맞죠?" 처럼 짧은 응답이나 단순 확인은 null을 반환하세요
- 질문 문장만 출력하세요 (설명 없이)

시민 발화: {utterance}
민원 질문:"""


# ── 발화 추출 ─────────────────────────────────────────────────────────────────

def extract_citizen_utterances(zip_path: Path, limit: int = 0) -> list[dict]:
    """
    TS zip의 consulting_content에서 고객: 발화를 추출한다.

    반환: [{"utterance": str, "source_id": str, "category": str}, ...]
    """
    results: list[dict] = []
    with zipfile.ZipFile(zip_path) as zf:
        json_files = sorted(n for n in zf.namelist() if n.endswith(".json"))
        for name in json_files:
            if limit and len(results) >= limit * 3:  # 변환 후 탈락분 고려해 3배 수집
                break
            try:
                with zf.open(name) as f:
                    records = json.loads(f.read().decode("utf-8"))
            except Exception:
                continue

            for r in records:
                content = r.get("consulting_content", "")
                source_id = r.get("source_id", "")
                category = r.get("consulting_category", "")

                for line in content.split("\n"):
                    line = line.strip()
                    if not line.startswith("고객:"):
                        continue
                    text = line[3:].strip()
                    # 너무 짧거나 단순 응답은 건너뜀
                    if len(text) < 15:
                        continue
                    results.append({
                        "utterance": text,
                        "source_id": source_id,
                        "category": category,
                    })

    return results


# ── Ollama 변환 ───────────────────────────────────────────────────────────────

def rewrite_as_question(utterance: str, ollama_url: str, model: str) -> Optional[str]:
    """Ollama로 시민 발화를 독립적인 민원 질문으로 변환한다."""
    prompt = _REWRITE_PROMPT.format(utterance=utterance)
    try:
        resp = httpx.post(
            f"{ollama_url}/api/generate",
            json={
                "model": model,
                "prompt": prompt,
                "stream": False,
                "options": {"temperature": 0.3, "num_predict": 100},
            },
            timeout=30.0,
        )
        resp.raise_for_status()
        text = resp.json().get("response", "").strip()
        # null 반환 처리
        if not text or text.lower() in ("null", "없음", "해당없음"):
            return None
        # 첫 줄만 사용 (모델이 설명을 덧붙이는 경우 대비)
        first_line = text.split("\n")[0].strip()
        # 유효하지 않은 JSON escape 시퀀스 제거
        # valid: \", \\, \/, \b, \f, \n, \r, \t, \uXXXX
        first_line = re.sub(r'\\([^"\\/bfnrtu])', r'\1', first_line)
        # 최소 길이 검증
        if len(first_line) < 10:
            return None
        return first_line
    except Exception as e:
        typer.echo(f"  [변환 실패] {e}", err=True)
        return None


# ── 메인 커맨드 ───────────────────────────────────────────────────────────────

@app.command()
def run(
    zip_path: Path = typer.Option(DEFAULT_ZIP, "--zip", help="TS_*.zip 경로"),
    org_id: str = typer.Option("org_acc", "--org-id"),
    service_id: str = typer.Option("svc_acc_chatbot", "--service-id"),
    limit: int = typer.Option(200, "--limit", help="생성할 최대 질문 수"),
    output_path: Path = typer.Option(DEFAULT_OUTPUT, "--output"),
    dry_run: bool = typer.Option(False, "--dry-run", help="Ollama 호출 없이 추출 결과만 출력"),
) -> None:
    """TS zip → Ollama 변환 → citizen_questions.json 생성."""
    ollama_url = os.getenv("OLLAMA_URL", "https://jcg-office.tailedf4dc.ts.net:11434")
    model = os.getenv("OLLAMA_MODEL", "qwen3:8b")

    typer.echo(f"[gen] 발화 추출 중: {zip_path.name}")
    utterances = extract_citizen_utterances(zip_path, limit=limit)
    typer.echo(f"[gen] {len(utterances)}개 고객 발화 추출 완료")

    if dry_run:
        typer.echo("\n[dry-run] 샘플 발화:")
        for u in utterances[:5]:
            typer.echo(f"  - {u['utterance'][:80]}")
        return

    typer.echo(f"[gen] Ollama({model})로 민원 질문 변환 시작...")
    questions: list[dict] = []

    for i, item in enumerate(utterances, 1):
        if len(questions) >= limit:
            break

        utterance = item["utterance"]
        typer.echo(f"  [{i}/{min(len(utterances), limit*2)}] {utterance[:50]}...")

        question = rewrite_as_question(utterance, ollama_url, model)
        if not question:
            typer.echo("  → 건너뜀 (짧은 응답 또는 변환 실패)")
            continue

        typer.echo(f"  → {question[:70]}")
        questions.append({
            "question": question,
            "ground_truth": "",
            "org_id": org_id,
            "service_id": service_id,
            "task_category": "민원질의",
            "consulting_category": item["category"],
            "source_id": item["source_id"],
        })

    output_path.write_text(
        json.dumps(questions, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    typer.echo(f"\n[gen] 완료 → {output_path} ({len(questions)}건)")


def main() -> None:
    from pathlib import Path
    from dotenv import load_dotenv
    load_dotenv(Path(__file__).parents[3] / ".env", override=False)
    app()


if __name__ == "__main__":
    main()
