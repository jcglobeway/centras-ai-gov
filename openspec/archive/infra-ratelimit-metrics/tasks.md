# Tasks

## Change ID

`infra-ratelimit-metrics`

## 구현 체크리스트

### P1. rag-orchestrator — 의존성

- [x] `pyproject.toml`: `psutil>=5.9.0` 추가

### P2. rag-orchestrator — 인프라 메트릭

- [x] `app.py`: `GET /metrics/infra` 엔드포인트 추가
  - `psutil.cpu_percent(interval=0.1)` → `cpuUsagePercent`
  - `psutil.virtual_memory().percent` → `memoryUsagePercent`
  - `gpuUsagePercent`: null (GPU 확장은 별도)
- [ ] 수동 검증: `curl http://localhost:8090/metrics/infra`

### P3. rag-orchestrator — Rate Limit 카운터

- [x] `app.py`: 전역 `_llm_counters` dict 추가
- [x] `app.py`: `GET /metrics/rate-limits` 엔드포인트 추가 (히트율 계산 포함)
- [x] `app.py`: `generate_answer_with_ollama` 에서 429 감지 → `llm_rate_limit_hits` increment
- [x] `retrieval.py`: 전역 `_embedding_counters` dict 추가
- [x] `retrieval.py`: `get_embedding` 에서 429 감지 → `embedding_rate_limit_hits` increment
- [ ] 수동 검증: `curl http://localhost:8090/metrics/rate-limits`

### P4. Admin API — 프록시 엔드포인트

- [x] `MetricsController.kt`: `RestTemplateBuilder` + `@Value("${rag.orchestrator.url}")` 추가
- [x] `MetricsController.kt`: `GET /admin/metrics/infra` 추가 (rag-orchestrator 프록시, 미실행 시 null 반환)
- [x] `MetricsController.kt`: `GET /admin/metrics/rate-limits` 추가 (rag-orchestrator 프록시, 미실행 시 null 반환)
- [x] `InfraMetricsResponse`, `RateLimitMetricsResponse` 데이터 클래스 추가
- [ ] 수동 검증: `curl http://localhost:8081/admin/metrics/infra`
- [ ] 수동 검증: `curl http://localhost:8081/admin/metrics/rate-limits`

### P5. Frontend — 패널 추가

- [x] `types.ts`: `InfraMetrics`, `RateLimitMetrics` 인터페이스 추가
- [x] `ops/page.tsx`: `useSWR("/api/admin/metrics/infra", fetcher)` 훅 추가
- [x] `ops/page.tsx`: `useSWR("/api/admin/metrics/rate-limits", fetcher)` 훅 추가
- [x] `ops/page.tsx`: "인프라 & Rate Limit" 섹션 추가 (이슈 알림 로그 위)
  - Row 1: CPU / Memory 카드 2개 (progress bar + ok/warn/critical)
  - Row 2: LLM Rate Limit 히트율 / Embedding Rate Limit 히트율 카드 2개
  - 데이터 없을 때 "rag-orchestrator 미실행" 메시지

### P6. 검증

- [ ] `./gradlew test` 통과 확인
- [ ] 프론트 `/ops` 화면에서 패널 렌더링 시각 확인
