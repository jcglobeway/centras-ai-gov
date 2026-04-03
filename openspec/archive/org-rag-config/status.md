# Status: org-rag-config

- 상태: `done`
- 시작일: `2026-04-03`
- 완료일: `2026-04-03`

## Progress

- V040/V041 마이그레이션 (`org_rag_configs`, `org_rag_config_versions`) 완료
- 백엔드 헥사고날 전 레이어 구현 완료
  - GET/PUT `/admin/organizations/{orgId}/rag-config`
  - GET `/admin/organizations/{orgId}/rag-config/versions`
  - POST `/admin/organizations/{orgId}/rag-config/rollback/{version}`
  - 저장 시 버전 자동 증가 + 이력 기록
- RAG Orchestrator `config_client.py` 신설 — org별 동적 config 로딩 (TTL 60s 인메모리 캐시)
- 프론트엔드 `/ops/prompt`, `/ops/rag-params` API 연동 완료
- Testcontainers(pgvector/pgvector:pg16) 전환으로 H2 비호환 이슈 해소, 전체 테스트 통과

## Verification

- `PUT /admin/organizations/org_acc/rag-config` → 국립아시아문화전당 최적화 프롬프트 저장 (v1)
- `POST /generate` → org_acc 프롬프트 적용 실답변 확인 (citation_count: 5, confidence: 0.77)
- 전체 테스트 BUILD SUCCESSFUL (flyway.target: "38")

## 잔여 리스크

- rag-orchestrator 캐시가 프로세스 메모리 기반 → 수평 확장 시 Redis 분산 캐시 필요
- PUT 저장 후 최대 60초 지연 반영 (TTL 만료 전)
