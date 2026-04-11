# Status

## Change ID

`activate-upload-page`

## Current Status

`validated`

## Progress

- Phase 0~4 구현 완료
- 테스트 통과: BUILD SUCCESSFUL (50 integration tests + 8 ArchUnit)
- 브라우저+DB 실검증 완료 (파일 업로드/크롤 소스 등록/즉시 실행)

## Notes

- 파일 업로드는 서버 로컬 디스크에 저장 (`${java.io.tmpdir}/ragops-uploads/{organizationId}/{timestamp}_{filename}`)
- 업로드된 파일에 대해 FILE_DROP 타입 크롤 소스와 QUEUED 잡이 자동 생성됨
- ingestion-worker가 실제 파일 처리를 담당 (별도 CLI 실행 필요: `ingestion-worker run --job-id <id>`)
- 수동 재인덱싱 잡 상태는 3초 폴링으로 실시간 반영
- 업로드 이력은 5초 폴링으로 자동 갱신
- 2026-04-10 09:16 KST 브라우저 업로드(`rag-admin-prd-v3.pdf`) 후 DB 확인:
  - `documents.id=doc_53056367`, `collection_name=one-by-one-test`, `crawl_source_id=crawl_src_995ebd39`
  - `crawl_sources.id=crawl_src_995ebd39`, `name=파일업로드: rag-admin-prd-v3.pdf`, `source_type=file_drop`
  - `ingestion_jobs.id=ing_job_b1365c38`, `trigger_type=file_upload`, `job_status=succeeded`
- 2026-04-10 09:17 KST 웹 크롤 소스 등록 후 DB 확인:
  - `crawl_sources.id=crawl_src_ffff3075`, `name=spec-activate-crawl-648359`, `source_type=website`, `source_uri=https://example.com/?v=648359`
- 2026-04-10 09:17 KST 즉시 실행 후 DB 확인:
  - `ingestion_jobs.id=ing_job_e2b0145c`, `crawl_source_id=crawl_src_ffff3075`, `trigger_type=manual`, `job_status=queued`
