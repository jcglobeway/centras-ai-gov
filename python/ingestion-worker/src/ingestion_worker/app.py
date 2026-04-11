from __future__ import annotations

import os
import sys
from pathlib import Path

import typer

from .admin_api_client import AdminApiClient
from .job_runner import IngestionJobRunner

app = typer.Typer(help="Ingestion worker entrypoint.")


def _load_dotenv_fallback() -> None:
    """python-dotenv 미설치 환경에서도 .env를 간단히 로드한다."""
    env_path = Path.cwd() / ".env"
    if not env_path.exists():
        return
    for line in env_path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        if not key:
            continue
        os.environ[key] = value.strip()


@app.command()
def run(
    job_id: str = typer.Option(..., "--job-id", help="Ingestion job ID to execute"),
    api_url: str = typer.Option(
        None,
        "--api-url",
        envvar="ADMIN_API_BASE_URL",
        help="Admin API base URL (default: http://localhost:8081)",
    ),
    session_token: str = typer.Option(
        None,
        "--session-token",
        envvar="ADMIN_API_SESSION_TOKEN",
        help="Admin API session token for authentication",
    ),
    username: str = typer.Option(
        None,
        "--username",
        envvar="ADMIN_API_USERNAME",
        help="Admin API login email (used when session token expires)",
    ),
    password: str = typer.Option(
        None,
        "--password",
        envvar="ADMIN_API_PASSWORD",
        help="Admin API login password",
    ),
) -> None:
    """Run an ingestion job by ID."""
    # 환경 변수 또는 기본값 설정
    api_url = api_url or os.getenv("ADMIN_API_BASE_URL", "http://localhost:8081")
    session_token = session_token or os.getenv("ADMIN_API_SESSION_TOKEN", "")
    username = username or os.getenv("ADMIN_API_USERNAME", "")
    password = password or os.getenv("ADMIN_API_PASSWORD", "")

    if not session_token and not (username and password):
        typer.echo("Error: session token or username/password is required", err=True)
        typer.echo(
            "Set ADMIN_API_SESSION_TOKEN or ADMIN_API_USERNAME/ADMIN_API_PASSWORD",
            err=True,
        )
        sys.exit(1)

    typer.echo(f"[Ingestion Worker] Starting job: {job_id}")
    typer.echo(f"[Ingestion Worker] Admin API: {api_url}")

    # Admin API Client 생성
    client = AdminApiClient(
        base_url=api_url,
        session_token=session_token,
        username=username,
        password=password,
    )

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


@app.command()
def worker(
    concurrency: int = typer.Option(2, help="동시 처리 워커 수"),
    loglevel: str = typer.Option("info", help="로그 레벨"),
    api_url: str = typer.Option(None, envvar="ADMIN_API_BASE_URL"),
    username: str = typer.Option(None, envvar="ADMIN_API_USERNAME"),
    password: str = typer.Option(None, envvar="ADMIN_API_PASSWORD"),
    session_token: str = typer.Option(None, envvar="ADMIN_API_SESSION_TOKEN"),
) -> None:
    """Celery 워커를 시작합니다 (Beat 포함). .env 파일을 자동으로 로드합니다."""
    try:
        from dotenv import load_dotenv
        load_dotenv(override=True)
    except ImportError:
        _load_dotenv_fallback()

    import os
    os.environ.setdefault("ADMIN_API_BASE_URL", api_url or "http://localhost:8081")
    if username:
        os.environ["ADMIN_API_USERNAME"] = username
    if password:
        os.environ["ADMIN_API_PASSWORD"] = password
    if session_token:
        os.environ["ADMIN_API_SESSION_TOKEN"] = session_token

    from .celery_app import app as celery_app
    import ingestion_worker.tasks  # noqa: F401 — task 등록

    typer.echo("[Worker] Starting Celery worker + Beat...")
    celery_app.worker_main([
        "worker",
        f"--concurrency={concurrency}",
        f"--loglevel={loglevel}",
        "--beat",
        "-Q", "celery",
    ])


def main() -> None:
    app()


if __name__ == "__main__":
    main()
