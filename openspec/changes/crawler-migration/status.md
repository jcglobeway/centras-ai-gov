# Status: crawler-migration

## 현재 단계

구현 진행 중

## 진행 상황

- [x] proposal.md 작성
- [x] tasks.md 작성
- [x] 구현 완료
- [x] 검증 완료 (BUILD SUCCESSFUL, 전체 테스트 통과)
- [ ] 아카이브

## 메모

- site-crawler-agent 위치: `/Users/parkseokje/Documents/GitHub/playground/site-crawler-agent`
- Playwright 어댑터(adapters/), LangGraph(graph/), institution YAML은 이식하지 않음
- KG 추출은 `KG_EXTRACTION_ENABLED=true` 환경변수로 선택적 활성화
- V046 마이그레이션: document_chunks에 metadata JSONB 추가
- 기존 flyway target: 45 → 46으로 업데이트 필요
