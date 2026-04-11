# Proposal: collection-lifecycle

## Change ID

`collection-lifecycle`

## Problem

현재 구조에서 컬렉션(crawl_sources의 collection_name 그룹) 단위로 인덱싱된 청크를 삭제할 방법이 없다. `document_chunks`는 `document_id`로 연결되어 있으나 `documents` 테이블에 `crawl_source_id`나 `collection_name`이 없어, 어떤 크롤 소스에서 생성된 청크인지 역추적이 불가능하다.

`ingestion_worker/job_runner.py` 131번째 줄에서 `document_id = f"doc_{source.id}"` 로 청크를 저장하고 있어, crawl_source → document → chunk 연결 고리를 DB 레벨에서 완성하면 컬렉션 단위 청크 삭제 API를 안전하게 구현할 수 있다.

## Proposed Solution

1. **V048 마이그레이션**: `documents` 테이블에 `collection_name VARCHAR(255)` 과 `crawl_source_id VARCHAR(255)` 컬럼을 nullable로 추가한다. 기존 데이터와의 하위 호환성을 유지한다.
2. **도메인 + 퍼시스턴스 확장**: `DocumentSummary` 및 `RegisterDocumentCommand`에 `collectionName`, `crawlSourceId` 필드를 추가하고, 파일 업로드 시 해당 값을 저장한다.
3. **DELETE /admin/collections/chunks API 신설**: query params `serviceId`, `collectionName`으로 해당 컬렉션에 속한 crawl_sources를 조회하고, 연결된 document_chunks를 일괄 삭제한 뒤 documents의 ingestion 상태를 초기화한다.
4. **프론트엔드**: 업로드 페이지 리인덱싱 섹션에 "컬렉션 삭제" 버튼을 추가한다(컬렉션 선택 시에만 표시).

## Out of Scope

- 컬렉션 메타데이터 CRUD (이름 변경, 설명 추가 등)
- crawl_source_id가 없는 기존 documents의 backfill 자동화
- RAG 검색 시 collection_name 기반 필터링
- 컬렉션 삭제 시 crawl_sources 레코드 자체 삭제

## Impact

- 영향 모듈: `modules/document-registry` (domain, port.in, port.out, service, adapter)
- 영향 API: `DELETE /admin/collections/chunks`, 파일 업로드 API (`POST /admin/documents/upload`)
- 영향 DB: `documents` 테이블 컬럼 추가 (V048), `document_chunks` 삭제 쿼리
- 영향 테스트: 기존 50개 통합 테스트 유지, 신규 컬렉션 삭제 API 테스트 추가
- 영향 프론트엔드: `frontend/src/app/ops/upload/page.tsx` 리인덱싱 섹션

## Done Definition

- `documents` 테이블에 `collection_name`, `crawl_source_id` 컬럼이 존재하며 파일 업로드 시 저장된다.
- `DELETE /admin/collections/chunks?serviceId=&collectionName=` 호출 시 해당 청크가 삭제되고 documents 상태가 초기화된다.
- 응답 형식: `{ "deletedChunks": Int, "resetDocuments": Int }`
- 기존 50개 통합 테스트 + ArchUnit 8개 규칙 모두 통과한다.
- 업로드 페이지에서 컬렉션 선택 시 "컬렉션 삭제" 버튼이 노출된다.