# Tasks: upload-page-v2

## Phase 0 — OpenSpec 아티팩트
- [x] proposal.md 작성
- [x] tasks.md 작성
- [x] status.md 작성

## Phase 1 — DB 마이그레이션
- [x] V047__add_collection_name_to_crawl_sources.sql 생성

## Phase 2 — 백엔드 도메인/퍼시스턴스
- [x] CrawlSourceSummary에 collectionName 필드 추가
- [x] CreateCrawlSourceCommand에 collectionName 필드 추가
- [x] CrawlSourceEntity에 collectionName 컬럼 추가
- [x] toSummary() / toEntity() 에 collectionName 매핑 추가
- [x] SaveCrawlSourcePortAdapter에 collectionName 전달
- [x] PersistIngestionJobPortAdapter CrawlSourceEntity 재생성 시 collectionName 유지

## Phase 3 — 백엔드 API 레이어
- [x] CreateCrawlSourceRequest에 collectionName 추가
- [x] CreateCrawlSourceCommand 생성 시 collectionName 전달
- [x] CrawlSourceResponse에 collectionName 추가
- [x] CrawlSourceSummary.toResponse() 에 collectionName 매핑
- [x] DocumentUploadController: 다중 파일(files) 처리로 변경
- [x] DocumentUploadController: collectionName 파라미터 추가
- [x] 반환 타입 List<DocumentUploadResponse> 로 변경

## Phase 4 — 프론트엔드
- [x] types.ts CrawlSource에 collectionName 추가
- [x] api.ts ingestionApi.createSource() body에 collectionName 추가
- [x] api.ts documentApi.upload() 다중 파일 + collectionName 지원
- [x] upload/page.tsx 파일 업로드 섹션 다중 파일 처리
- [x] upload/page.tsx 컬렉션 이름 입력 필드 추가 (파일 업로드)
- [x] upload/page.tsx 업로드 진행 표시 개선
- [x] upload/page.tsx 웹 크롤링 섹션 collectionName 입력 추가
- [x] upload/page.tsx 수동 재인덱싱 드롭다운 텍스트 개선

## 검증
- [x] ./gradlew test 전체 통과 확인
