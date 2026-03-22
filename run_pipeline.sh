#!/usr/bin/env bash
# ── 공공 RAG 평가 파이프라인 원샷 실행 스크립트 ───────────────────────────────
#
# 사전 조건:
#   - admin-api 실행 중 (기본 localhost:8081)
#   - Ollama 실행 중 + bge-m3 모델 풀 완료 (임베딩용)
#   - PostgreSQL 실행 중 + Flyway V027 마이그레이션 완료
#   - 환경 변수 설정:
#       ADMIN_API_BASE_URL       (기본: http://localhost:8081)
#       ADMIN_API_SESSION_TOKEN  (ops@jcg.com 세션 토큰)
#       RAGAS_JUDGE_PROVIDER     (기본: ollama)
#       RAGAS_OLLAMA_MODEL       (기본: qwen2.5:7b)
#
# 실행:
#   chmod +x run_pipeline.sh
#   ADMIN_API_SESSION_TOKEN=<token> ./run_pipeline.sh
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

ADMIN_API_BASE_URL="${ADMIN_API_BASE_URL:-http://localhost:8081}"
export ADMIN_API_BASE_URL

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EVAL_DIR="$SCRIPT_DIR/python/eval-runner"
INGESTION_DIR="$SCRIPT_DIR/python/ingestion-worker"

log() { echo "[pipeline] $*"; }
err() { echo "[pipeline][ERROR] $*" >&2; exit 1; }

# ── Step 1: ingestion_prep — V027 SQL + eval_questions.json 생성 ──────────────

log "Step 1: ingestion_prep — 공공 Q&A 파싱 및 SQL 생성"
cd "$EVAL_DIR"
python -m eval_runner.ingestion_prep \
    --sample 100 \
    --max-docs-per-zip 50 \
    --seed 42

SQL_FILE="$SCRIPT_DIR/apps/admin-api/src/main/resources/db/migration/V027__seed_public_documents_and_llm_metrics.sql"
EVAL_FILE="$EVAL_DIR/eval_questions.json"

[[ -f "$SQL_FILE" ]]  || err "V027 SQL 파일 생성 실패"
[[ -f "$EVAL_FILE" ]] || err "eval_questions.json 생성 실패"
log "  → V027 SQL: $SQL_FILE"
log "  → eval_questions.json: $EVAL_FILE"

# ── Step 2: Flyway 마이그레이션 (admin-api 재시작으로 자동 적용) ──────────────

log "Step 2: admin-api 연결 확인 ($ADMIN_API_BASE_URL)"
if ! curl -sf "$ADMIN_API_BASE_URL/actuator/health" > /dev/null 2>&1; then
    log "  [warn] admin-api health check 실패. 수동으로 확인하세요."
    log "  힌트: ./gradlew :apps:admin-api:bootRun"
fi

# ── Step 3: query_runner — 실제 RAG 질의 실행 ─────────────────────────────────

log "Step 3: query_runner — RAG 질의 실행 (100건)"
cd "$EVAL_DIR"
python -m eval_runner.query_runner \
    --input "$EVAL_FILE" \
    --output "$EVAL_DIR/eval_results.json" \
    --limit 100 \
    --delay 0.5

[[ -f "$EVAL_DIR/eval_results.json" ]] || err "eval_results.json 생성 실패"
log "  → eval_results.json: $EVAL_DIR/eval_results.json"

# ── Step 4: ragas_batch — RAGAS 지표 계산 및 전송 ────────────────────────────

log "Step 4: ragas_batch — RAGAS 평가 실행"
cd "$EVAL_DIR"
TODAY=$(date +%Y-%m-%d)
python -m eval_runner.ragas_batch \
    --date "$TODAY" \
    --page-size 100

# ── 완료 ──────────────────────────────────────────────────────────────────────

log "파이프라인 완료."
log "  대시보드 확인: http://localhost:3000/ops/quality"
log "  비용 확인:     http://localhost:3000/ops/cost"
