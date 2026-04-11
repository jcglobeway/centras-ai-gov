# Proposal

## Change ID

`activate-upload-page`

## Summary

- `/ops/upload` 페이지의 3개 섹션(파일 업로드, 웹 크롤링 등록, 수동 재인덱싱)을 실제 API와 연동하여 동작하게 함
- 파일 업로드를 위한 백엔드 엔드포인트 `POST /admin/documents/upload` 신규 추가
- 업로드 이력 테이블의 TypeScript 인터페이스 버그 수정
- 기존 `POST /admin/crawl-sources`, `POST /admin/crawl-sources/{id}/run` API 프론트엔드 연동

## Impact

- 영향 모듈: `modules/document-registry` (RegisterDocumentUseCase 신규), `apps/admin-api` (DocumentUploadController 신규, ServiceConfiguration/RepositoryConfiguration 수정)
- 영향 API: `POST /admin/documents/upload` (신규), `POST /admin/crawl-sources`, `POST /admin/crawl-sources/{id}/run`
- 영향 테스트: 기존 50개 통과 유지. ArchUnit Rule 위반 없음.

## Done Definition

- 파일 드래그앤드롭 후 업로드 클릭 → `documents` 테이블 + `ingestion_jobs` 테이블에 레코드 생성
- 웹 크롤링 등록 폼 입력 후 등록 클릭 → `crawl_sources` 테이블에 레코드 생성
- 크롤 소스 선택 후 즉시 실행 클릭 → `ingestion_jobs` 테이블에 QUEUED 잡 생성
- 업로드 이력 테이블이 실제 잡 데이터를 올바르게 표시
- 기존 통합 테스트 50개 + ArchUnit 8개 모두 통과
