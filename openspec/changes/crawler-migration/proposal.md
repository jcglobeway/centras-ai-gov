# Proposal: crawler-migration

## Problem

현재 `python/ingestion-worker`의 크롤링 파이프라인은 seed URL 1개만 크롤하며, 링크 탐색이 없다. 텍스트 분할은 단순 RecursiveCharacterTextSplitter를 사용하고, KG(Knowledge Graph) 추출 기능이 없다. 이로 인해 멀티페이지 사이트에서 콘텐츠 수집률이 낮고 RAG 검색 품질이 제한된다.

`playground/site-crawler-agent`에는 이미 멀티페이지 재귀 크롤(`AutonomousCrawler`), 섹션 구조 보존 추출(`ContentExtractor`), 계층형 청킹(`HierarchicalChunker`), KG 추출(`KGExtractor`)이 구현되어 있다. 이를 ingestion-worker로 이식하고, Qdrant 의존성을 pgvector + Spring Boot Admin API로 대체한다.

## Proposed Solution

1. `python/ingestion-worker/src/ingestion_worker/crawler/` 패키지 신규 생성
   - `autonomous.py` — AutonomousCrawler (SimHash 중복제거, robots.txt, 재귀 링크 탐색)
   - `extractor.py` — ContentExtractor (섹션 구조 보존, 노이즈 태그 제거)
   - `chunker.py` — HierarchicalChunker (계층형 청킹)

2. `python/ingestion-worker/src/ingestion_worker/kg_extractor.py` 신규 생성
   - Ollama LLM 기반 엔티티/토픽/요약 추출
   - `KG_EXTRACTION_ENABLED=true` env var로 선택적 활성화

3. `python/ingestion-worker/src/ingestion_worker/models.py` 확장
   - `CrawledPage`, `TextChunk` 데이터 모델 추가

4. `python/ingestion-worker/src/ingestion_worker/job_runner.py` 교체
   - 단일 URL 크롤 → AutonomousCrawler 기반 멀티페이지 파이프라인으로 교체
   - KG 추출 결과를 청크 metadata로 전달

5. `python/ingestion-worker/src/ingestion_worker/admin_api_client.py` 확장
   - `save_document_chunk()`에 `metadata` 파라미터 추가

6. DB 변경: `V046__add_chunk_metadata.sql`
   - `document_chunks.metadata JSONB` 컬럼 추가 (KG 메타데이터 저장)

7. Spring Boot `DocumentChunkController` 신규 추가
   - `POST /admin/document-chunks` — Python ingestion-worker가 청크를 저장하는 엔드포인트
   - `metadata` 필드 지원

8. Spring Boot 도메인/엔티티/포트/서비스에 `metadata` 필드 추가

## Out of Scope

- Qdrant, Neo4j, RAG 쿼리 엔진, FastAPI 서버
- site-crawler-agent의 institution YAML 시스템, GGC 어댑터
- Playwright 어댑터 패턴 (기관별 커스텀 파싱)
- 멤버 페이지, 층별 안내 등 특수 페이지 타입 처리

## Success Criteria

- `ingestion-worker run --job-id <id>` 실행 시 seed URL에서 재귀적으로 링크를 탐색하여 멀티페이지 크롤
- SimHash 중복 페이지 자동 제거
- robots.txt 준수
- 계층형 청킹으로 섹션 구조 보존
- `KG_EXTRACTION_ENABLED=true` 시 엔티티/토픽/요약이 `document_chunks.metadata`에 저장
- 기존 50개 통합 테스트 전부 통과 (flyway target 46 반영)
