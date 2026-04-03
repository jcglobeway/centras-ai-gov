# Todo

미결 OpenSpec 변경사항 통합 목록.
각 항목은 `openspec/changes/<change-id>/` 의 상세 문서를 참조한다.

---

## 진행 중

### eval-bulk-ragas — 대량 평가 데이터 배치 RAGAS 재실행

**Change**: `openspec/changes/eval-bulk-ragas/`
**상태**: `in-progress` — 2026-04-02 시작

- [x] Phase 1 — `export_db_eval_data.py` 구현 + `ragas_batch.py --from-eval-results` 추가
- [ ] Phase 2 — 실행: `eval-runner --from-eval-results eval_results.json`
- [ ] Phase 3 — 검증: DB에 RAGAS 지표 정상 저장 확인

---

## 예정 (planned)

### enhance-audit-log — 감사 로그 강화 (PII 감지·CSV export)

**Change**: `openspec/changes/enhance-audit-log/`
**상태**: `planned`

- PII 자동 감지 (Presidio) + 감사 로그 기록
- CSV export API
- 리스크: 한국어 PII 인식률 검증 필요

### enhance-anomaly-detection — 이상 탐지 알림

**Change**: `openspec/changes/enhance-anomaly-detection/`
**상태**: `planned`

- 지표 임계값 초과 시 이상 탐지 이벤트 생성
- AnomalyAlertScheduler + drift 요약 API

---

## 최근 완료 아카이브 현황

| Change ID | 완료일 | 요약 |
|-----------|--------|------|
| `org-rag-config` | 2026-04-03 | 기관별 RAG config 관리 (GET/PUT/rollback), rag-orchestrator 동적 로딩, 프론트 연동 |
| `eval-batch-restore` | 2026-04-03 | eval-runner PATCH 전략으로 ragas_evaluations 중복 행 방지 |
| `testcontainers-migration` | 2026-04-03 | H2 → Testcontainers(pgvector/pgvector:pg16) 전환, flyway.target "38" 적용 |
| `citation-metrics` | 2026-04-03 | Citation Coverage·Correctness 지표 추가 (DB V042, 백엔드, eval-runner, 프론트) |
| `bulk-injection-rag-quality` | 2026-04-01 | 하이브리드 RAG 파이프라인 (vector+BM25+RRF+FlashRank), 405건 대량 투입 |
| `statistics-dashboard-insight` | 2026-04-03 | 질문 길이 분포 API + RAG 개선 인사이트 대시보드 |
| `ragas-realtime-eval` | 2026-04-03 | Redis BRPOP 기반 실시간 RAGAS 평가 |
| `semantic-question-analysis` | 2026-04-03 | 질문 키워드·유사도·유형 분석 |

→ 전체 아카이브: `openspec/archive/`
