# Tasks: crawler-migration

## Phase 1 — 크롤러 이식 (완료)

### Python — 신규 파일

- [x] `crawler/autonomous.py` — AutonomousCrawler, RobotsChecker, SimHash 중복제거
- [x] `crawler/extractor.py` — ContentExtractor (섹션 구조 보존)
- [x] `crawler/chunker.py` — HierarchicalChunker
- [x] `crawler/__init__.py`
- [x] `kg_extractor.py` — KGExtractor (Ollama LLM, KG_EXTRACTION_ENABLED env var)

### Python — 수정 파일

- [x] `models.py` — CrawledPage, TextChunk 데이터 모델 추가
- [x] `admin_api_client.py` — save_document_chunk()에 metadata 파라미터 추가
- [x] `job_runner.py` — 멀티페이지 파이프라인으로 교체 (AutonomousCrawler 사용)
- [x] `pyproject.toml` — tenacity, loguru, langchain-ollama, lxml 의존성 추가

### Spring Boot — DB 마이그레이션

- [x] `V046__add_chunk_metadata.sql` — document_chunks에 metadata TEXT 컬럼 추가

### Spring Boot — 도메인/헥사고날

- [x] `DocumentChunk.kt` 도메인 모델 — metadata 필드 추가 (SaveDocumentChunkCommand, DocumentChunkSummary)
- [x] `DocumentChunkEntity.kt` — metadata 컬럼 추가
- [x] `SaveDocumentPortAdapter.kt` — metadata 필드 entity 생성 시 전달
- [x] `DocumentChunkController.kt` 신규 — POST /admin/document-chunks 엔드포인트

### Spring Boot — 테스트 설정

- [x] `application-test.yml` — flyway.target "46"으로 업데이트

### 검증

- [x] 기존 통합 테스트 전부 통과 확인 (BUILD SUCCESSFUL)

### 후속 정리

- [x] `crawl_executor.py` 삭제 — job_runner.py가 AutonomousCrawler를 직접 사용하므로 미사용
- [x] `transition_job_status()` 반환 타입 `None`으로 단순화
- [x] GGC 어댑터 이식: `crawler/adapters/base.py`, `crawler/adapters/ggc_go_kr.py`
- [x] `autonomous.py` — ADAPTER_REGISTRY, _detect_adapter(), crawl_all() 어댑터 훅 통합
- [x] `/ops/upload/page.tsx` — 파서 감지 뱃지 추가

---

## Phase 2 — 실제 기관 추가 + 버그 수정 (진행 중)

### 즉시 수행: 500 에러 수정

- [x] `DocumentChunkController.kt` 500 에러 원인 진단 (로그 확인, null 필드 검증, JPA 예외 추적)
- [x] 원인 수정 후 `POST /admin/document-chunks` 201 반환 검증
  - 원인: `document_chunks.embedding_vector`에 JPA가 `varchar`를 바인딩하고, 저장 시 `Instant` 타입도 JDBC가 직접 추론하지 못했음
  - 수정: `JdbcTemplate` + `CAST(? AS vector)` 저장, `created_at`은 `Timestamp`로 전달
  - 검증: 1024차원 벡터 payload로 `DocumentChunkApiTest` 통과

### DB Migration V049 — 실제 기관 3개 시드

- [x] `V049__add_real_organizations.sql` 작성
  - `organizations`: org_ggc (경기도의회), org_gjf (경기도일자리재단), org_gwangju_namgu (광주남구청)
  - `services`: cb-gyeonggido, gjf, gwangju-namgu (각 기관 연결)
  - `crawl_sources`: ggc.go.kr, gjf.or.kr, namgu.gwangju.kr (각 서비스 연결)
  - `admin_users` + `admin_user_roles`: 기관별 관리자 계정 (client_org_admin 역할)
- [x] 마이그레이션 적용 검증 (기존 테스트 flyway target 영향 없음 확인)

### GjfOrKrAdapter 셸

- [x] `crawler/adapters/gjf_or_kr.py` 생성
  - `BaseSiteAdapter` 상속
  - `URL_ROUTES: dict` 스텁 (빈 딕셔너리)
  - `parse()` 메서드 — `super().parse()` 위임 (범용 파서 폴백)
- [x] `crawler/adapters/__init__.py` — `GjfOrKrAdapter` export 추가
- [x] `autonomous.py` ADAPTER_REGISTRY — `(r"gjf\.or\.kr", GjfOrKrAdapter)` 항목 추가

### NamguGwangjuKrAdapter 셸

- [x] `crawler/adapters/namgu_gwangju_kr.py` 생성
  - `BaseSiteAdapter` 상속
  - `URL_ROUTES: dict` 스텁 (빈 딕셔너리)
  - `parse()` 메서드 — `super().parse()` 위임 (범용 파서 폴백)
- [x] `crawler/adapters/__init__.py` — `NamguGwangjuKrAdapter` export 추가
- [x] `autonomous.py` ADAPTER_REGISTRY — `(r"namgu\.gwangju\.kr", NamguGwangjuKrAdapter)` 항목 추가

### 검증

- [x] `ingestion-worker run --job-id <id>` (ggc.go.kr 대상) E2E 실행 성공 확인
- [x] 기존 통합 테스트 전부 통과 확인 (flyway target 미변경)
