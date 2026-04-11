# Tasks: collection-lifecycle

## Phase 0 — OpenSpec 아티팩트
- [x] proposal.md 작성
- [x] tasks.md 작성
- [x] status.md 작성

## Phase 1 — DB 마이그레이션
- [x] V048__add_collection_fields_to_documents.sql 생성
  - `documents` 테이블에 `collection_name VARCHAR(255) NULL` 추가
  - `documents` 테이블에 `crawl_source_id VARCHAR(255) NULL` 추가

## Phase 2 — 도메인 및 퍼시스턴스 확장
- [x] `DocumentSummary`에 `collectionName: String?`, `crawlSourceId: String?` 필드 추가 (default null)
- [x] `RegisterDocumentCommand`에 `collectionName: String?`, `crawlSourceId: String?` 필드 추가 (default null)
- [x] `DocumentEntity`에 `collectionName`, `crawlSourceId` 컬럼 추가
- [x] `toSummary()` 매퍼에 신규 필드 매핑 추가
- [x] `SaveDocumentRecordPortAdapter`에서 `collectionName`, `crawlSourceId` entity에 전달

## Phase 3 — 컬렉션 청크 삭제 포트 및 서비스 구현
- [x] `DeleteCollectionChunksPort` 인터페이스 생성 (`application/port/out/`)
  - `deleteChunksByCollection(serviceId, collectionName): DeleteCollectionResult`
- [x] `DeleteCollectionChunksUseCase` 인터페이스 생성 (`application/port/in/`)
  - 입력: `serviceId`, `collectionName`
  - 출력: `DeleteCollectionResult(deletedChunks, resetDocuments)`
- [x] `DeleteCollectionChunksService` 구현체 생성 (`application/service/`)
  - Port에 위임
- [x] `DeleteCollectionChunksPortAdapter` 구현 (`adapter/outbound/persistence/`)
  - `open class` 선언
  - JdbcTemplate native query로 crawl_sources JOIN 후 document_chunks 삭제
  - documents ingestion_status='pending', index_status='not_indexed' 초기화

## Phase 4 — API 레이어
- [x] 신규 `CollectionController.kt` 생성, 엔드포인트 추가
  - `DELETE /admin/collections/chunks?serviceId=&collectionName=`
  - 응답: `{ "deletedChunks": Int, "resetDocuments": Int }`
- [x] 파일 업로드 API (`DocumentUploadController.kt`)에서 crawl source 먼저 생성 후 `collectionName`, `crawlSourceId` 를 `RegisterDocumentCommand`에 전달

## Phase 5 — Bean 등록
- [x] `ServiceConfiguration.kt`에 `DeleteCollectionChunksService` Bean 등록
- [x] `RepositoryConfiguration.kt`에 `DeleteCollectionChunksPortAdapter` Bean 등록

## Phase 6 — 프론트엔드
- [x] `upload/page.tsx` 수동 재인덱싱 섹션: 컬렉션 선택 시 "컬렉션 청크 삭제" 버튼 표시
- [x] 버튼 클릭 시 `DELETE /api/admin/collections/chunks?serviceId=&collectionName=` 호출
- [x] 삭제 결과(`deletedChunks`, `resetDocuments`)를 인라인 메시지로 표시

## 검증
- [x] V048 마이그레이션 생성 완료 (`V048__add_collection_fields_to_documents.sql`)
- [x] `DELETE /admin/collections/chunks` E2E 시나리오 테스트 작성
- [x] `./gradlew :apps:admin-api:compileKotlin` 성공 확인
- [x] 기존 53개 테스트 통과 확인 (QAReviewApiTests.qa_review_list_can_be_filtered 1건은 pre-existing 실패, 변경사항 무관)
