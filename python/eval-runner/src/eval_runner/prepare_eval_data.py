"""
평가 데이터 준비 스크립트.

TL_*_질의응답.zip에서 Q&A 쌍을 추출해 eval_questions.json을 생성한다.

실행:
  prepare-eval-data --zip /path/to/TL_국립아시아문화전당_질의응답.zip \\
                   --org-id org_acc --service-id svc_acc_chatbot \\
                   --limit 50
"""
from __future__ import annotations

import json
from pathlib import Path

import typer

from eval_runner.training_data_loader import load_qa_pairs

app = typer.Typer(help="훈련 라벨링 데이터 → eval_questions.json 변환")

_PROJECT_ROOT = Path(__file__).parents[4]
DEFAULT_OUTPUT = _PROJECT_ROOT / "python" / "eval-runner" / "eval_questions.json"


@app.command()
def run(
    zip_path: Path = typer.Option(..., "--zip", help="TL_*_질의응답.zip 경로"),
    org_id: str = typer.Option("org_acc", "--org-id"),
    service_id: str = typer.Option("svc_acc_chatbot", "--service-id"),
    limit: int = typer.Option(50, "--limit", help="추출할 최대 Q&A 쌍 수"),
    output_path: Path = typer.Option(DEFAULT_OUTPUT, "--output"),
) -> None:
    """TL zip → eval_questions.json 생성."""
    typer.echo(f"[prepare] {zip_path.name} → limit={limit}")

    pairs = load_qa_pairs(zip_path, org_id, service_id, limit)
    if not pairs:
        typer.echo("[prepare] Q&A 쌍 없음", err=True)
        raise typer.Exit(1)

    output_path.write_text(
        json.dumps(pairs, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    typer.echo(f"[prepare] {len(pairs)}건 → {output_path}")

    # 샘플 출력
    sample = pairs[0]
    typer.echo(f"\n[샘플]")
    typer.echo(f"  질문: {sample['question']}")
    typer.echo(f"  정답: {sample['ground_truth']}")
    typer.echo(f"  카테고리: {sample['task_category']} / {sample['consulting_category']}")


def main() -> None:
    from dotenv import load_dotenv
    load_dotenv()
    app()


if __name__ == "__main__":
    main()
