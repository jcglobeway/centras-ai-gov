# Proposal: upload-page-v2

## Problem

현재 파일 업로드 화면(`/ops/upload`)은 한 번에 파일 하나만 업로드할 수 있어, 다수의 공공문서(PDF, HWP 등)를 일괄 수집해야 하는 운영 상황에서 비효율적이다. 또한 업로드된 문서가 어떤 주제 컬렉션(예: "여권서식편람")에 속하는지 분류할 수 없어 RAG 검색 품질 개선에 한계가 있다.

## Proposed Solution

1. **다중 파일 업로드**: `<input multiple>` + 개별 API 루프로 여러 파일을 한 번에 처리하고, 진행 상황(1/3 형태)을 표시한다.
2. **컬렉션 태그(collectionName)**: 크롤 소스(`crawl_sources`)에 `collection_name` 컬럼을 추가하여 파일 업로드 및 웹 크롤링 등록 시 컬렉션 이름을 선택적으로 지정할 수 있게 한다.
3. **드롭다운 텍스트 개선**: 수동 재인덱싱 섹션의 소스 선택 드롭다운에 컬렉션 이름을 표시하여 가독성을 높인다.

## Out of Scope

- 컬렉션 관리 CRUD (별도 change로 분리)
- 기존 크롤 소스에 collectionName 일괄 backfill
- RAG 검색 시 collectionName 기반 필터링

## Success Criteria

- 다중 파일 선택 후 일괄 업로드 가능
- collectionName이 crawl_sources 테이블에 저장됨
- 기존 50개 통합 테스트 + ArchUnit 8개 규칙 모두 통과
