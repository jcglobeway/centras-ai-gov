# Proposal: frontend-ops-portal

## 목적

운영사 어드민 포털을 구현한다. super_admin, ops_admin 역할이 접근한다.

## 범위

- `/ops` — 운영 대시보드 (기관 헬스맵, KPI 추세)
- `/ops/organizations` — 기관/서비스 목록
- `/ops/indexing` — 인제스션 잡 현황
- `/ops/quality` — 품질 모니터링 (fallback율, 신뢰도)
- `/ops/incidents` — 장애 관리 (placeholder)

## 영향

- frontend-setup 기반 위에 구축
- API: `/api/admin/organizations`, `/api/admin/ingestion-jobs`, `/api/admin/metrics/daily`
