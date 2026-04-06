"""
대량 질의 데이터 주입 스크립트 (멀티턴 지원).

TL_국립아시아문화전당_질의응답.zip에서 Q&A를 추출해 Admin API에 대량 투입한다.
source_id 기준으로 그룹핑해 동일 상담 원문의 질문들을 하나의 세션(멀티턴)으로 묶는다.

실행:
  bulk-query-runner \\
    --zip /path/to/TL_국립아시아문화전당_질의응답.zip \\
    --limit 200 --max-turns 3 --delay 1.0

사전 조건:
  - admin-api: http://localhost:8081 (또는 ADMIN_API_BASE_URL)
  - rag-orchestrator: http://localhost:8090 (또는 RAG_ORCHESTRATOR_URL)
"""
from __future__ import annotations

import collections
import itertools
import json
import os
import time
from pathlib import Path
from typing import Optional

import httpx
import typer

from eval_runner.training_data_loader import load_qa_pairs

app = typer.Typer(help="대량 질의 투입 (멀티턴 세션 지원)")

_DATA_ROOT = Path(__file__).parents[4] / "data" / "3.개방데이터" / "1.데이터" / "Training" / "02.라벨링데이터"
DEFAULT_ZIP = _DATA_ROOT / "TL_국립아시아문화전당_질의응답.zip"


# ── Admin API + RAG 오케스트레이터 클라이언트 ─────────────────────────────────

class BulkClient:
    def __init__(self, admin_url: str, rag_url: str, session_token: str, channel: str = "simulator") -> None:
        self.admin_url = admin_url.rstrip("/")
        self.rag_url = rag_url.rstrip("/")
        self._session_token = session_token
        self._channel = channel
        self._headers = {
            "X-Admin-Session-Id": session_token,
            "Content-Type": "application/json",
        }

    @classmethod
    def create(cls, admin_url: str, rag_url: str, channel: str = "simulator") -> "BulkClient":
        """env에서 세션 토큰을 읽고, 없으면 자동 로그인한다."""
        token = os.getenv("ADMIN_API_SESSION_TOKEN", "").strip()
        if not token:
            token = _auto_login(admin_url)
        return cls(admin_url, rag_url, token, channel)

    # ── simulator session ──────────────────────────────────────────────────

    def create_session(self, org_id: str, service_id: str) -> Optional[str]:
        """POST /admin/simulator/sessions → sessionId."""
        try:
            resp = httpx.post(
                f"{self.admin_url}/admin/simulator/sessions",
                json={"organizationId": org_id, "serviceId": service_id},
                headers=self._headers,
                timeout=10.0,
            )
            resp.raise_for_status()
            return resp.json().get("sessionId")
        except Exception as e:
            typer.echo(f"  [세션 생성 실패] {e}", err=True)
            return None

    # ── question ───────────────────────────────────────────────────────────

    def create_question(
        self,
        org_id: str,
        service_id: str,
        session_id: str,
        question_text: str,
        question_category: Optional[str] = None,
        question_intent_label: Optional[str] = None,
    ) -> Optional[str]:
        """POST /admin/questions → questionId."""
        try:
            resp = httpx.post(
                f"{self.admin_url}/admin/questions",
                json={
                    "organizationId": org_id,
                    "serviceId": service_id,
                    "chatSessionId": session_id,
                    "questionText": question_text,
                    "channel": self._channel,
                    "questionCategory": question_category,
                    "questionIntentLabel": question_intent_label,
                },
                headers=self._headers,
                # admin-api가 내부적으로 RAG 오케스트레이터를 동기 호출하므로
                # Ollama LLM 응답 시간(30-90초)을 수용할 수 있는 타임아웃 설정
                timeout=120.0,
            )
            resp.raise_for_status()
            return resp.json().get("questionId")
        except Exception as e:
            typer.echo(f"  [질문 생성 실패] {e}", err=True)
            return None

    # ── RAG generate ───────────────────────────────────────────────────────
    # 참고: POST /admin/questions가 이미 RAG 오케스트레이터를 내부 호출한다.
    # generate_answer는 admin-api를 우회해 직접 RAG를 호출하는 경우에만 사용한다.

    def generate_answer(
        self,
        question_id: str,
        question_text: str,
        org_id: str,
        service_id: str,
    ) -> bool:
        """POST /generate → RAG 검색 + 답변 생성 + rag_search_log 기록."""
        try:
            resp = httpx.post(
                f"{self.rag_url}/generate",
                json={
                    "question_id": question_id,
                    "question_text": question_text,
                    "organization_id": org_id,
                    "service_id": service_id,
                },
                timeout=60.0,
            )
            resp.raise_for_status()
            return True
        except Exception as e:
            typer.echo(f"  [RAG 생성 실패] {e}", err=True)
            return False


# ── 자동 로그인 ───────────────────────────────────────────────────────────────

def _auto_login(admin_url: str) -> str:
    """super_admin 계정으로 로그인해 세션 토큰을 반환한다."""
    email = os.getenv("ADMIN_EMAIL", "super@jcg.com")
    password = os.getenv("ADMIN_PASSWORD", "pass1234")
    try:
        resp = httpx.post(
            f"{admin_url}/admin/auth/login",
            json={"email": email, "password": password},
            timeout=10.0,
        )
        resp.raise_for_status()
        token = resp.json().get("session", {}).get("token", "")
        if token:
            typer.echo(f"[auth] 자동 로그인 성공 (token: {token[:8]}...)")
        return token
    except Exception as e:
        typer.echo(f"[auth] 로그인 실패: {e} — 세션 없이 진행합니다.", err=True)
        return ""


# ── 그룹핑 ────────────────────────────────────────────────────────────────────

def group_by_source(pairs: list[dict], max_turns: int) -> list[list[dict]]:
    """
    source_id 기준으로 Q&A를 그룹핑한다.

    같은 source_id의 질문들은 멀티턴 대화로 묶인다.
    max_turns 초과분은 별도 그룹으로 분할한다.
    source_id가 없는 항목은 단일턴 그룹으로 처리한다.
    """
    grouped: dict[str, list[dict]] = collections.defaultdict(list)
    no_source: list[dict] = []

    for pair in pairs:
        src = pair.get("source_id", "").strip()
        if src:
            grouped[src].append(pair)
        else:
            no_source.append(pair)

    sessions: list[list[dict]] = []

    for src_id, items in grouped.items():
        # max_turns 단위로 분할
        for chunk_start in range(0, len(items), max_turns):
            sessions.append(items[chunk_start : chunk_start + max_turns])

    # source_id 없는 항목은 1건씩 단일 세션
    for item in no_source:
        sessions.append([item])

    return sessions


# ── 메인 커맨드 ───────────────────────────────────────────────────────────────

@app.command()
def run(
    zip_path: Path = typer.Option(DEFAULT_ZIP, "--zip", help="TL_*_질의응답.zip 경로 (--input-json 없을 때 사용)"),
    input_json: Optional[Path] = typer.Option(None, "--input-json", help="citizen_questions.json 등 사전 생성된 질문 파일"),
    org_id: str = typer.Option("org_acc", "--org-id"),
    service_id: str = typer.Option("svc_acc_chatbot", "--service-id"),
    limit: int = typer.Option(200, "--limit", help="처리할 최대 Q&A 쌍 수 (0=전체)"),
    skip: int = typer.Option(0, "--skip", help="앞에서 건너뛸 Q&A 쌍 수"),
    max_turns: int = typer.Option(3, "--max-turns", help="세션당 최대 질문 수"),
    delay: float = typer.Option(1.0, "--delay", help="질문 간 대기(초)"),
    session_delay: float = typer.Option(0.3, "--session-delay", help="세션 간 대기(초)"),
    channel: str = typer.Option("simulator", "--channel", help="질문 채널 (simulator | api | web)"),
    dry_run: bool = typer.Option(False, "--dry-run", help="API 호출 없이 구조만 출력"),
    multi_turn_only: bool = typer.Option(False, "--multi-turn-only", help="멀티턴 그룹만 처리"),
) -> None:
    """TL zip → 대량 질의 투입 (멀티턴 세션)."""
    admin_url = os.getenv("ADMIN_API_BASE_URL", "http://localhost:8081")
    rag_url   = os.getenv("RAG_ORCHESTRATOR_URL", "http://localhost:8090")

    if not zip_path.exists():
        typer.echo(f"[bulk] 파일 없음: {zip_path}", err=True)
        raise typer.Exit(1)

    # Q&A 로드: JSON 파일 우선, 없으면 zip에서 추출
    if input_json and input_json.exists():
        typer.echo(f"[bulk] JSON 로드: {input_json.name} (skip={skip}, limit={limit}, channel={channel})")
        import json as _json
        all_pairs = _json.loads(input_json.read_text(encoding="utf-8"))
    else:
        typer.echo(f"[bulk] 데이터 로드 중: {zip_path.name} (skip={skip}, limit={limit}, channel={channel})")
        all_pairs = load_qa_pairs(zip_path, org_id, service_id, limit=0)
    pairs = all_pairs[skip: skip + limit] if limit else all_pairs[skip:]
    typer.echo(f"[bulk] {len(pairs)}건 로드 완료")

    # 그룹핑
    sessions = group_by_source(pairs, max_turns)
    if multi_turn_only:
        sessions = [s for s in sessions if len(s) > 1]
        typer.echo(f"[bulk] 멀티턴 필터 적용 → {len(sessions)}개 세션")
    else:
        multi_count = sum(1 for s in sessions if len(s) > 1)
        single_count = len(sessions) - multi_count
        typer.echo(f"[bulk] 총 {len(sessions)}개 세션 (멀티턴: {multi_count}, 단일턴: {single_count})")

    if dry_run:
        _print_dry_run(sessions)
        return

    # 실제 실행
    client = BulkClient.create(admin_url, rag_url, channel)
    db_url = os.getenv("DATABASE_URL")
    stats = {"sessions": 0, "questions": 0, "answers": 0, "failures": 0}
    all_question_ids: list[tuple[str, str]] = []  # (question_id, target_date)

    # 날짜 분산: 최근 30일 범위를 세션 수로 균등 분할
    import datetime
    today = datetime.date.today()
    date_pool = [str(today - datetime.timedelta(days=i)) for i in range(29, -1, -1)]  # 30일 전 → 오늘

    for sess_idx, turn_items in enumerate(sessions, 1):
        is_multi = len(turn_items) > 1
        turn_label = f"{len(turn_items)}턴" if is_multi else "단일"
        category = turn_items[0].get("consulting_category", "")
        # 세션마다 날짜 순환 배정
        target_date = date_pool[(sess_idx - 1) % len(date_pool)]
        typer.echo(
            f"\n[세션 {sess_idx}/{len(sessions)}] {turn_label} | {target_date} | 카테고리: {category}"
        )

        # 세션 생성
        session_id = client.create_session(org_id, service_id)
        if not session_id:
            stats["failures"] += len(turn_items)
            continue
        stats["sessions"] += 1

        for turn_idx, item in enumerate(turn_items, 1):
            q_text = item["question"]
            typer.echo(f"  [{turn_idx}/{len(turn_items)}] {q_text[:60]}...")

            question_id = client.create_question(
                org_id=org_id,
                service_id=service_id,
                session_id=session_id,
                question_text=q_text,
                question_category=item.get("consulting_category"),
                question_intent_label=item.get("task_category"),
            )
            stats["questions"] += 1

            if not question_id:
                stats["failures"] += 1
                time.sleep(delay)
                continue

            all_question_ids.append((question_id, target_date))
            stats["answers"] += 1
            typer.echo(f"  → 완료 ({question_id})")

            if delay > 0:
                time.sleep(delay)

        if session_delay > 0:
            time.sleep(session_delay)

    # 날짜 분산: DB에서 created_at을 target_date로 업데이트
    if db_url and all_question_ids:
        _backdate_questions(db_url, all_question_ids)

    # 최종 통계
    typer.echo("\n" + "=" * 60)
    typer.echo(f"[완료] 세션: {stats['sessions']}  질문: {stats['questions']}  "
               f"답변: {stats['answers']}  실패: {stats['failures']}")


def _backdate_questions(db_url: str, question_dates: list[tuple[str, str]]) -> None:
    """questions / answers / rag_search_logs의 created_at을 target_date로 업데이트한다."""
    import psycopg2
    try:
        conn = psycopg2.connect(db_url)
        cur = conn.cursor()
        for question_id, date_str in question_dates:
            # 랜덤 시각 추가 (9시~18시 사이)
            import random
            hour = random.randint(9, 17)
            minute = random.randint(0, 59)
            ts = f"{date_str} {hour:02d}:{minute:02d}:00"
            cur.execute("UPDATE questions SET created_at = %s WHERE id = %s", (ts, question_id))
            cur.execute("UPDATE answers SET created_at = %s WHERE question_id = %s", (ts, question_id))
            cur.execute("UPDATE rag_search_logs SET created_at = %s WHERE question_id = %s", (ts, question_id))
        conn.commit()
        cur.close()
        conn.close()
        typer.echo(f"[날짜] {len(question_dates)}건 날짜 분산 완료")
    except Exception as e:
        typer.echo(f"[날짜] 업데이트 실패: {e}", err=True)


def _print_dry_run(sessions: list[list[dict]]) -> None:
    typer.echo("\n[dry-run] 세션 구조 미리보기 (API 호출 없음)")
    for i, items in enumerate(sessions[:5], 1):
        typer.echo(f"\n  세션 {i} ({len(items)}턴) — src: {items[0].get('source_id', 'N/A')}")
        for j, item in enumerate(items, 1):
            typer.echo(f"    [{j}] {item['question'][:70]}")
    if len(sessions) > 5:
        typer.echo(f"\n  ... 외 {len(sessions) - 5}개 세션")
    total_q = sum(len(s) for s in sessions)
    typer.echo(f"\n[dry-run] 총 {len(sessions)}세션 / {total_q}질문 예정")


def main() -> None:
    from pathlib import Path
    from dotenv import load_dotenv
    load_dotenv(Path(__file__).parents[3] / ".env", override=False)
    app()


if __name__ == "__main__":
    main()
