# Proposal: frontend-client-portal

## 목적

고객사 어드민 포털을 구현한다. client_org_admin, client_viewer 역할이 접근한다.

## 범위

- `/client` — 기관 대시보드 (해결율, 만족도, 전환율)
- `/client/performance` — 민원응대 성과 (질문 목록)
- `/client/failure` — 실패/전환 분석 (A01~A10 코드 분포)
- `/client/knowledge` — 지식 현황 (문서 목록)

## 영향

- frontend-setup 기반 위에 구축
- API: `/api/admin/questions`, `/api/admin/documents`, `/api/admin/metrics/daily`
