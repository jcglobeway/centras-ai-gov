from pathlib import Path
from dotenv import load_dotenv

# python/.env — 모든 Python 서비스가 참조하는 단일 환경변수 파일
_ENV_FILE = Path(__file__).parents[1] / ".env"


def load_env() -> None:
    """python/.env를 로드한다. 이미 설정된 환경변수는 덮어쓰지 않는다."""
    load_dotenv(_ENV_FILE, override=False)
