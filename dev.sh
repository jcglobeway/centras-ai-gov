#!/usr/bin/env bash
# dev.sh — admin-api + rag-orchestrator + frontend 일괄 시작
#
# 사용법:
#   ./dev.sh           # 전체 시작
#   ./dev.sh --no-rag  # rag-orchestrator 제외
#   ./dev.sh --clean   # Gradle clean 후 시작 (캐시 문제 해결 시 사용)
#
# 종료: Ctrl+C → 모든 프로세스 일괄 종료
# 로그: logs/ 디렉토리에 서비스별 파일로 저장

set -euo pipefail

JAVA_HOME="${JAVA_HOME:-/Users/parkseokje/Library/Java/JavaVirtualMachines/openjdk-25.0.2/Contents/Home}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOGS_DIR="$SCRIPT_DIR/logs"

START_RAG=true
CLEAN_BUILD=false
for arg in "$@"; do
  [[ "$arg" == "--no-rag" ]] && START_RAG=false
  [[ "$arg" == "--clean"  ]] && CLEAN_BUILD=true
done

mkdir -p "$LOGS_DIR"

log()  { echo "[dev] $*"; }
url()  { echo "  → $1"; }

# ── 기존 프로세스 종료 ───────────────────────────────────────────────────────

log "Killing existing processes on ports 8081 8090 3000..."
for port in 8081 8090 3000; do
  pid=$(lsof -ti:"$port" 2>/dev/null) && kill -9 $pid 2>/dev/null && log "  killed port $port (pid $pid)" || true
done
sleep 1

# ── admin-api (port 8081) ────────────────────────────────────────────────────

if $CLEAN_BUILD; then
  log "Cleaning build cache..."
  JAVA_HOME="$JAVA_HOME" "$SCRIPT_DIR/gradlew" clean > "$LOGS_DIR/admin-api.log" 2>&1
fi

log "Starting admin-api (port 8081)..."
JAVA_HOME="$JAVA_HOME" "$SCRIPT_DIR/gradlew" :apps:admin-api:bootRun \
  >> "$LOGS_DIR/admin-api.log" 2>&1 &
ADMIN_PID=$!

# ── rag-orchestrator (port 8090) ─────────────────────────────────────────────

RAG_PID=""
if $START_RAG; then
  log "Starting rag-orchestrator (port 8090)..."
  (cd "$SCRIPT_DIR/python/rag-orchestrator" && uv run rag-orchestrator) \
    > "$LOGS_DIR/rag-orchestrator.log" 2>&1 &
  RAG_PID=$!
else
  log "Skipping rag-orchestrator (--no-rag)"
fi

# ── frontend (port 3000) ─────────────────────────────────────────────────────

log "Starting frontend (port 3000)..."
(cd "$SCRIPT_DIR/frontend" && npm run dev) \
  > "$LOGS_DIR/frontend.log" 2>&1 &
FRONT_PID=$!

# ── 시작 안내 ─────────────────────────────────────────────────────────────────

echo ""
log "All services starting. URLs:"
url "Admin API   → http://localhost:8081"
$START_RAG && url "Orchestrator → http://localhost:8090"
url "Frontend    → http://localhost:3000"
echo ""
log "Logs: tail -f $LOGS_DIR/*.log"
log "Stop:  Ctrl+C"
echo ""

# ── 종료 처리 ─────────────────────────────────────────────────────────────────

cleanup() {
  echo ""
  log "Stopping all services..."
  kill "$ADMIN_PID" 2>/dev/null || true
  [[ -n "$RAG_PID" ]] && kill "$RAG_PID" 2>/dev/null || true
  kill "$FRONT_PID" 2>/dev/null || true
  log "Done."
  exit 0
}

trap cleanup INT TERM

wait
