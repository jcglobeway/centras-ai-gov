from __future__ import annotations

import os
import sys

import typer

from .admin_api_client import AdminApiClient
from .job_runner import IngestionJobRunner

app = typer.Typer(help="Ingestion worker entrypoint.")


@app.command()
def run(
    job_id: str = typer.Option(..., "--job-id", help="Ingestion job ID to execute"),
    api_url: str = typer.Option(
        None,
        "--api-url",
        envvar="ADMIN_API_BASE_URL",
        help="Admin API base URL (default: http://localhost:8080)",
    ),
    session_token: str = typer.Option(
        None,
        "--session-token",
        envvar="ADMIN_API_SESSION_TOKEN",
        help="Admin API session token for authentication",
    ),
) -> None:
    """Run an ingestion job by ID."""
    # 환경 변수 또는 기본값 설정
    api_url = api_url or os.getenv("ADMIN_API_BASE_URL", "http://localhost:8080")
    session_token = session_token or os.getenv("ADMIN_API_SESSION_TOKEN", "")

    if not session_token:
        typer.echo("Error: ADMIN_API_SESSION_TOKEN is required", err=True)
        typer.echo("Set it via --session-token or ADMIN_API_SESSION_TOKEN env var", err=True)
        sys.exit(1)

    typer.echo(f"[Ingestion Worker] Starting job: {job_id}")
    typer.echo(f"[Ingestion Worker] Admin API: {api_url}")

    # Admin API Client 생성
    client = AdminApiClient(base_url=api_url, session_token=session_token)

    try:
        # Job Runner 실행
        runner = IngestionJobRunner(admin_api_client=client)
        runner.run_job(job_id=job_id)
        typer.echo(f"[Ingestion Worker] Job {job_id} completed successfully!")

    except Exception as e:
        typer.echo(f"[Ingestion Worker] Job failed: {e}", err=True)
        sys.exit(1)

    finally:
        client.close()


def main() -> None:
    app()


if __name__ == "__main__":
    main()

