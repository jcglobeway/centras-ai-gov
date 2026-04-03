# Proposal

## Change ID

`infra-ratelimit-metrics`

## Summary

### 변경 목적

운영자가 RAG 파이프라인의 외부 의존성 상태를 실시간으로 파악할 수 있도록 두 가지 인프라 지표를 Ops 대시보드에 추가한다.

1. **인스턴스 리소스 추적**: rag-orchestrator 프로세스가 실행 중인 서버의 CPU·Memory 사용률을 `psutil`로 직접 수집해 카드 형태로 표시한다. 외부 모니터링 서버(Prometheus 등) 없이 동작한다.
2. **Rate Limit 히트율 추적**: rag-orchestrator가 LLM/Embedding API 호출 시 발생하는 429(Too Many Requests) 에러 비율을 in-memory 카운터로 집계해 대시보드에 표시한다.

두 지표는 상관 관계가 높다 — Rate Limit 히트 증가 → 재시도 대기 → E2E Latency P95 상승. Ops 대시보드에서 나란히 배치해 원인 진단 시간을 단축한다.

### 변경 범위

| 레이어 | 변경 내용 |
|--------|-----------|
| Python / rag-orchestrator | `psutil` 의존성 추가 |
| Python / rag-orchestrator | `GET /metrics/infra` — psutil로 CPU·Memory 수집 |
| Python / rag-orchestrator | in-memory 카운터 + `GET /metrics/rate-limits` |
| Python / rag-orchestrator | LLM/Embedding 호출 시 429 감지 → 카운터 increment |
| Admin API (MetricsController) | `GET /admin/metrics/infra` — rag-orchestrator 프록시 |
| Admin API (MetricsController) | `GET /admin/metrics/rate-limits` — rag-orchestrator 프록시 |
| Frontend (ops/page.tsx) | "인프라 & Rate Limit" 패널 추가 |

### 제외 범위

- Prometheus 서버 / Node Exporter / DCGM Exporter (외부 인프라 불필요)
- `application.yml` Prometheus URL 설정 (제거)
- Micrometer 연동 (Spring Boot Actuator 변경 없음)
- GPU 사용률 (nvidia-ml-py 별도 의존성 필요, 추후 확장)
- Rate Limit 이력 DB 저장 (프로세스 재시작 시 카운터 초기화 허용)

## API 계약

### `GET /metrics/infra` (rag-orchestrator, port 8090)

psutil로 수집한 서버 리소스. 항상 응답 (psutil은 외부 의존성 없음):
```json
{
  "cpuUsagePercent": 42.5,
  "memoryUsagePercent": 68.1,
  "gpuUsagePercent": null,
  "collectedAt": "2026-04-03T09:00:00Z"
}
```

### `GET /metrics/rate-limits` (rag-orchestrator, port 8090)

```json
{
  "llmCallsTotal": 120,
  "llmRateLimitHits": 3,
  "llmRateLimitRate": 2.5,
  "embeddingCallsTotal": 80,
  "embeddingRateLimitHits": 0,
  "embeddingRateLimitRate": 0.0,
  "windowStartAt": "2026-04-03T00:00:00Z"
}
```

### `GET /admin/metrics/infra` (Admin API, port 8081)

rag-orchestrator 프록시. 미실행 시 null 반환:
```json
{
  "cpuUsagePercent": null,
  "memoryUsagePercent": null,
  "gpuUsagePercent": null,
  "collectedAt": null
}
```

### `GET /admin/metrics/rate-limits` (Admin API, port 8081)

rag-orchestrator 프록시. 미실행 시 null 반환:
```json
{
  "llmCallsTotal": null,
  "llmRateLimitHits": null,
  "llmRateLimitRate": null,
  "embeddingCallsTotal": null,
  "embeddingRateLimitHits": null,
  "embeddingRateLimitRate": null,
  "windowStartAt": null
}
```

## 임계값 기준

| 지표 | 정상 | 주의 | 위험 |
|------|------|------|------|
| CPU 사용률 | < 80% | 80~90% | ≥ 90% |
| Memory 사용률 | < 80% | 80~90% | ≥ 90% |
| LLM Rate Limit 히트율 | < 0.5% | 0.5~2% | ≥ 2% |
| Embedding Rate Limit 히트율 | < 0.5% | 0.5~2% | ≥ 2% |

## Impact

### 영향 모듈

- `python/rag-orchestrator/pyproject.toml` (psutil 의존성)
- `python/rag-orchestrator/src/rag_orchestrator/app.py`
- `python/rag-orchestrator/src/rag_orchestrator/retrieval.py`
- `apps/admin-api/.../metrics/adapter/inbound/web/MetricsController.kt`
- `frontend/src/app/ops/page.tsx`

### 영향 API

- `GET /metrics/infra` (rag-orchestrator, 신규)
- `GET /metrics/rate-limits` (rag-orchestrator, 신규)
- `GET /admin/metrics/infra` (Admin API, 신규)
- `GET /admin/metrics/rate-limits` (Admin API, 신규)

### 영향 테스트

- 기존 통합 테스트 영향 없음 (신규 엔드포인트 추가만)
- 신규 테스트 추가 없음 (rag-orchestrator 단위 테스트는 별도 관리)

## Done Definition

- [ ] rag-orchestrator 실행 후 `GET /metrics/infra` 호출 시 CPU·Memory 수치 반환
- [ ] rag-orchestrator 실행 후 `GET /metrics/rate-limits` 호출 시 JSON 반환
- [ ] LLM 호출 중 429 에러 발생 시 `llmRateLimitHits` 증가 확인
- [ ] `GET /admin/metrics/infra` → rag-orchestrator 미실행 시 null 필드 반환
- [ ] `GET /admin/metrics/rate-limits` → rag-orchestrator 미실행 시 null 필드 반환
- [ ] 프론트 `/ops` 화면에서 "인프라 & Rate Limit" 패널 렌더링 (데이터 없을 때 빈 상태 메시지 표시)
- [ ] `./gradlew test` 통과 (기존 50개 테스트 + 8개 ArchUnit 규칙 유지)
