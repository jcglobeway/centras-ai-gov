# Status: crawler-migration

## 현재 단계

검증 완료 — 아카이브 대기

## 진행 상황

### Phase 1 (완료)
- [x] proposal.md 작성
- [x] tasks.md 작성
- [x] 크롤러 이식 구현 완료 (AutonomousCrawler, HierarchicalChunker, KGExtractor)
- [x] GGC 어댑터 이식 완료 (adapters/ 디렉토리, ADAPTER_REGISTRY, 자동 감지)
- [x] DocumentChunkController POST /admin/document-chunks 신규 구현
- [x] /ops/upload 파서 감지 뱃지 추가
- [x] 검증 완료 (BUILD SUCCESSFUL, 전체 테스트 통과)
- [x] 후속 정리 완료 (crawl_executor 제거, transition_job_status 단순화)

### Phase 2 (완료)
- [x] POST /admin/document-chunks 500 에러 수정
- [x] V049 DB 마이그레이션 — 경기도의회, 경기도일자리재단, 광주남구청 시드
- [x] GjfOrKrAdapter 셸 (gjf.or.kr, ADAPTER_REGISTRY 등록)
- [x] NamguGwangjuKrAdapter 셸 (namgu.gwangju.kr, ADAPTER_REGISTRY 등록)
- [x] E2E 검증
- [ ] 아카이브

## 다음 즉시 수행 항목

1. 아카이브 수행
2. 장시간 `running/fetch` 잔류 job 정리 정책 문서화 (운영 runbook)

## 메모

### Phase 1 메모
- site-crawler-agent 위치: `/Users/parkseokje/Documents/GitHub/playground/site-crawler-agent`
- Playwright 어댑터(adapters/), LangGraph(graph/), institution YAML은 이식하지 않음
- KG 추출은 `KG_EXTRACTION_ENABLED=true` 환경변수로 선택적 활성화
- V046 마이그레이션: document_chunks에 metadata TEXT 컬럼 추가
- GGC 어댑터 이식 완료: `crawler/adapters/ggc_go_kr.py` (Neo4j/institution.py 의존성 제거)
- adapterType은 DB에 저장하지 않음 — worker 실행 시 seed URL로 재감지 (ADAPTER_REGISTRY)
- GGC_MEMBER_API_KEY 미설정 시 HTML 폴백으로 graceful skip

### Phase 2 메모
- 프로덕션 DB 확인 데이터 (SSH to jcg-prod4 MySQL):
  - 경기도의회: service_id=cb-gyeonggido, site=https://www.ggc.go.kr
  - 경기도일자리재단: service_id=gjf, site=https://www.gjf.or.kr
  - 광주광역시 남구청: service_id=gwangju-namgu, site=https://www.namgu.gwangju.kr
- V049 번호 확정: V048은 add_collection_fields_to_documents.sql (이미 존재)
- GJF / Namgu 어댑터는 셸 구조를 실제 구현으로 전환 완료
- KG 추출기는 기존 KGExtractor 재사용 — 신규 개발 불필요
- `DocumentChunkController` 저장 경로는 `JdbcTemplate` + `CAST(? AS vector)`로 변경해 PostgreSQL `vector(1024)` 타입에 맞춤
- 2026-04-10 검증 실행:
  - `POST /admin/crawl-sources/crawl_src_ggc/run` → `jobId=ing_job_187634f1`
  - `ing_job_187634f1`를 `queued`로 정리 후 제한 옵션으로 수동 실행:
    - `CRAWLER_MAX_DEPTH=0`
    - `CRAWLER_MAX_PAGES=1`
  - 결과: `SUCCEEDED/COMPLETE`, 문서 `doc_3816222d` 생성, 청크 2건 임베딩/저장 완료
  - 참고: 이전 job `ing_job_cbd222bd`는 장시간 `running/fetch`로 잔류
  - 테스트 정리:
    - `application-test.yml` flyway target을 `54`로 맞춤 (redteam 전역 스키마 반영)
    - `QAReviewApiTests` 필터 파라미터를 `question_id`로 수정
    - `RedteamApiTests`를 전역 케이스셋 모델에 맞게 기대값 수정
  - 최종 검증:
    - `./gradlew :apps:admin-api:test --tests '*QAReviewApiTests*' --tests '*RedteamApiTests*'` 통과
    - `./gradlew :apps:admin-api:test` 전체 통과
