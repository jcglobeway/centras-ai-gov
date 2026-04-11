# Proposal: crawler-migration

## Problem

현재 `python/ingestion-worker`의 크롤링 파이프라인은 seed URL 1개만 크롤하며, 링크 탐색이 없다. 텍스트 분할은 단순 RecursiveCharacterTextSplitter를 사용하고, KG(Knowledge Graph) 추출 기능이 없다. 이로 인해 멀티페이지 사이트에서 콘텐츠 수집률이 낮고 RAG 검색 품질이 제한된다.

`playground/site-crawler-agent`에는 이미 멀티페이지 재귀 크롤(`AutonomousCrawler`), 섹션 구조 보존 추출(`ContentExtractor`), 계층형 청킹(`HierarchicalChunker`), KG 추출(`KGExtractor`)이 구현되어 있다. 이를 ingestion-worker로 이식하고, Qdrant 의존성을 pgvector + Spring Boot Admin API로 대체한다.

## Phase 1 완료 (이식 완료)

1. `crawler/` 패키지: `autonomous.py`, `extractor.py`, `chunker.py`, `__init__.py`
2. `kg_extractor.py`: Ollama LLM 기반, `KG_EXTRACTION_ENABLED` env var 제어
3. `models.py`: `CrawledPage`, `TextChunk` 데이터 모델
4. `job_runner.py`: AutonomousCrawler 기반 멀티페이지 파이프라인
5. `admin_api_client.py`: `save_document_chunk()` metadata 파라미터 지원
6. `V046__add_chunk_metadata.sql`: `document_chunks.metadata TEXT` 컬럼
7. `DocumentChunkController`: `POST /admin/document-chunks` 엔드포인트
8. `crawler/adapters/`: `base.py`, `ggc_go_kr.py`, ADAPTER_REGISTRY 자동 감지

## Phase 2 현재 범위 (잔여 작업)

프로덕션 배포를 위한 실제 기관 3개 추가 및 E2E 테스트 블로킹 버그 수정.

### 1. DB Migration V049 — 실제 기관 3개 추가

프로덕션 DB 확인된 데이터 기준으로 기관, 서비스, 크롤 소스, 관리자 계정을 시드한다.

| 기관 | org_id | service_id | 사이트 |
|------|--------|------------|--------|
| 경기도의회 | org_ggc | cb-gyeonggido | https://www.ggc.go.kr |
| 경기도일자리재단 | org_gjf | gjf | https://www.gjf.or.kr |
| 광주광역시 남구청 | org_gwangju_namgu | gwangju-namgu | https://www.namgu.gwangju.kr |

### 2. GjfOrKrAdapter 셸 생성

`gjf.or.kr` 도메인용 어댑터를 ADAPTER_REGISTRY에 등록. 실제 파서 로직은 추후 구현이며, 이번에는 `URL_ROUTES` 스텁만 포함한다.

### 3. NamguGwangjuKrAdapter 셸 생성

`namgu.gwangju.kr` 도메인용 어댑터를 ADAPTER_REGISTRY에 등록. 동일하게 셸 구조만 작성한다.

### 4. POST /admin/document-chunks 500 에러 수정

현재 E2E 파이프라인에서 ingestion-worker가 청크를 저장할 때 500 Internal Server Error가 발생한다. 이 버그가 수정되기 전까지 실제 크롤-색인 파이프라인을 검증할 수 없다.

## Out of Scope

- Qdrant, Neo4j 연동
- GJF / Namgu 어댑터 실제 파서 로직 구현 (URL_ROUTES 스텁만)
- KG 추출기 신규 개발 (기존 KGExtractor는 이미 이식됨)
- job.gg.go.kr (Jobaba) 별도 크롤 소스 — V049 범위 외

## Success Criteria

- `POST /admin/document-chunks` 호출 시 500 에러 없이 201 반환
- `ingestion-worker run --job-id <id>` 실행 시 ggc.go.kr / gjf.or.kr / namgu.gwangju.kr 대상으로 멀티페이지 크롤 가능 (어댑터 자동 감지)
- V049 마이그레이션 적용 후 3개 기관 데이터 DB에 존재
- 기존 통합 테스트 전부 통과 유지
