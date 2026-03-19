# Proposal: frontend-qa-portal

## 목적

품질/지식관리 어드민 포털을 구현한다. qa_manager, knowledge_editor 역할이 접근한다.

## 범위

- `/qa` — 검수 대시보드 (미검수 건수, 리뷰 상태 분포)
- `/qa/unresolved` — 미응답/오답 관리 (미결 질문 목록 + QA 리뷰 액션)
- `/qa/documents` — 문서 관리 (목록, 버전 이력)
- `/qa/approvals` — 승인 워크플로우 (placeholder)

## 영향

- frontend-setup 기반 위에 구축
- API: `/api/admin/questions/unresolved`, `/api/admin/qa-reviews`, `/api/admin/documents`
