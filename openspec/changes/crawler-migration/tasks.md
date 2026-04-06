# Tasks: crawler-migration

## Python — 신규 파일

- [x] `crawler/autonomous.py` — AutonomousCrawler, RobotsChecker, SimHash 중복제거
- [x] `crawler/extractor.py` — ContentExtractor (섹션 구조 보존)
- [x] `crawler/chunker.py` — HierarchicalChunker
- [x] `crawler/__init__.py`
- [x] `kg_extractor.py` — KGExtractor (Ollama LLM, KG_EXTRACTION_ENABLED env var)

## Python — 수정 파일

- [x] `models.py` — CrawledPage, TextChunk 데이터 모델 추가
- [x] `admin_api_client.py` — save_document_chunk()에 metadata 파라미터 추가
- [x] `job_runner.py` — 멀티페이지 파이프라인으로 교체 (AutonomousCrawler 사용)
- [x] `pyproject.toml` — tenacity, loguru, langchain-ollama, lxml 의존성 추가

## Spring Boot — DB 마이그레이션

- [x] `V046__add_chunk_metadata.sql` — document_chunks에 metadata TEXT 컬럼 추가

## Spring Boot — 도메인/헥사고날

- [x] `DocumentChunk.kt` 도메인 모델 — metadata 필드 추가 (SaveDocumentChunkCommand, DocumentChunkSummary)
- [x] `DocumentChunkEntity.kt` — metadata 컬럼 추가
- [x] `SaveDocumentPortAdapter.kt` — metadata 필드 entity 생성 시 전달
- [x] `DocumentChunkController.kt` 신규 — POST /admin/document-chunks 엔드포인트

## Spring Boot — 테스트 설정

- [x] `application-test.yml` — flyway.target "46"으로 업데이트

## 검증

- [x] 기존 통합 테스트 전부 통과 확인 (BUILD SUCCESSFUL)
