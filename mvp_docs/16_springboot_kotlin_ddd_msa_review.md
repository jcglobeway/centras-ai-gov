# Spring Boot Kotlin DDD MSA Review

## 1. Review Goal

- `Spring Boot + Kotlin`을 제품 본체의 기본 스택으로 둘 수 있는지 판단한다.
- `DDD`, `MSA`를 MVP부터 강하게 도입할지, 아니면 경계만 먼저 설계할지 결정한다.
- crawling ingestion, Python 기반 RAG orchestration, OpenRAG 연동 가능성까지 포함해 현실적인 구조를 제안한다.

## 2. Conclusion

- 제품 본체는 `Spring Boot + Kotlin`이 적합하다.
- 도메인 구조는 `DDD-lite` 또는 `modular monolith`로 시작하는 것이 맞다.
- MVP 단계에서 바로 `full MSA`로 가는 것은 비추천이다.
- Python은 `ingestion / crawling / RAG orchestration` 서브시스템으로 분리하는 것이 적합하다.
- OpenRAG는 `RAG Adapter` 뒤의 병렬 PoC 후보로만 유지한다.

## 3. Why Spring Boot And Kotlin Fit

- 이 제품의 본체는 AI 실험보다 `운영 플랫폼` 성격이 강하다.
- 핵심 복잡도는 `권한`, `기관 스코프`, `감사로그`, `QA 상태 전이`, `운영 화면`, `KPI 집계`에 있다.
- Spring Boot는 이런 업무 시스템에서 구조 통제, 트랜잭션 처리, 배치, 운영성이 강하다.
- Kotlin은 Java 대비 보일러플레이트를 줄이고, null safety와 data class 덕분에 계약 모델 관리가 편하다.

## 4. Why Not Full MSA First

- 현재 MVP는 기능보다 `운영 계약`을 먼저 정확히 만드는 단계다.
- 지금 경계가 완전히 고정되지 않은 상태에서 서비스 분리를 먼저 하면 배포 단위와 장애 지점만 늘어난다.
- `QA`, `metrics`, `document ops`, `auth`, `chat runtime`은 아직 강한 독립 배포 요구보다 데이터 일관성이 더 중요하다.
- ingestion 과 RAG만 예외적으로 외부 워커와 잘 맞는다.

## 5. Recommended Architecture Direction

### Product Core

- `Next.js` Admin Web
- `Spring Boot + Kotlin` Product API
- `PostgreSQL` Product DB
- `Redis` cache and queue trigger

### AI / Ingestion Side System

- `Python` ingestion worker
- `Python` RAG orchestration service
- `Playwright + parsing stack` for crawling
- `OpenSearch` or `pgvector` for retrieval index

### Integration Rule

- Spring Boot가 시스템 오브 레코드다.
- Python 서비스는 `worker` 또는 `adapter backend`다.
- 모든 운영 상태는 제품 DB 기준으로 기록한다.
- OpenRAG를 붙이더라도 제품 API와 DB 계약은 바꾸지 않는다.

## 6. DDD Recommendation

DDD는 도입하는 편이 맞다. 다만 `전략적 설계 + 모듈 경계`에 집중하고, 초기에 과도한 패턴 적용은 피한다.

### Recommended Bounded Context Candidates

- `Identity and Access`
- `Organization and Service Directory`
- `Chat Runtime`
- `Document Registry`
- `Ingestion Operations`
- `QA Review`
- `Metrics and Reporting`

### Aggregate Guidance

- `Organization`
- `AdminUser`
- `Question`
- `Answer`
- `Document`
- `QAReview`
- `IngestionJob`

### Practical Rule

- 패키지는 bounded context 기준으로 나눈다.
- 컨트롤러, 서비스, 엔티티 수평 분리보다 context 중심 수직 분리를 우선한다.
- JPA entity를 곧바로 외부 API 모델로 노출하지 않는다.
- 상태 전이와 권한 검증은 도메인 규칙으로 캡슐화한다.

## 7. MSA Recommendation

MSA는 `즉시 채택`보다 `추후 분리 가능한 모듈 경계 설계`가 맞다.

### Start As

- `modular monolith` + external workers

### Split Later If Needed

- `ingestion service`
- `rag orchestration service`
- `metrics batch service`

### Keep Inside Core For MVP

- auth and session
- organization scope
- admin api
- qa workflow
- dashboard api

## 8. Proposed Module Layout

Spring Boot 본체는 아래처럼 context 기반 모듈로 시작하는 것이 적합하다.

- `apps/admin-api`
- `modules/identity-access`
- `modules/organization-directory`
- `modules/chat-runtime`
- `modules/document-registry`
- `modules/ingestion-ops`
- `modules/qa-review`
- `modules/metrics-reporting`
- `modules/shared-kernel`

Python 쪽은 별도 워크스페이스로 분리한다.

- `python/ingestion-worker`
- `python/rag-orchestrator`
- `python/common`

## 9. Crawling And RAG Fit

이 프로젝트에서 crawling 과 RAG orchestration 은 Spring Boot보다 Python이 더 적합하다.

### Crawling

- 정적 HTML 수집
- 동적 페이지 렌더링
- 본문 추출
- 문서 포맷 파싱
- chunking and embedding

### RAG Orchestration

- query rewrite
- retrieval
- reranking
- citation normalization
- fallback decision

### Why Separate

- 라이브러리 생태계가 Python 쪽이 더 풍부하다.
- OpenRAG, Langflow, 문서 파서, 크롤링 도구와의 결합이 쉽다.
- 실패 지점을 제품 본체와 분리할 수 있다.

## 10. OpenRAG Position

- OpenRAG는 제품 본체가 아니다.
- `Python rag-orchestrator` 안의 구현체 후보 또는 PoC 백엔드로 둔다.
- 운영 플랫폼은 OpenRAG 비의존적으로 유지한다.
- retrieval 품질이 충분히 좋고 운영 복잡도가 감당 가능할 때만 채택 후보로 올린다.

## 11. Decision Matrix

### Adopt Now

- Spring Boot
- Kotlin
- modular monolith
- context-based package structure
- Python ingestion worker
- Python RAG adapter service

### Design Now, Split Later

- bounded contexts
- async event contracts
- worker callback contract
- ingestion / rag service boundary

### Do Not Adopt As Core Yet

- full MSA rollout
- OpenRAG as mandatory runtime
- Lightpanda as default browser runtime

## 12. Final Recommendation

가장 현실적인 권장안은 아래다.

- 제품 본체: `Next.js + Spring Boot + Kotlin + PostgreSQL + Redis`
- 아키텍처 방식: `DDD-lite modular monolith`
- 외부 실행 계층: `Python ingestion worker + Python rag orchestrator`
- 검색/벡터: `OpenSearch` 또는 `pgvector`
- OpenRAG: `RAG Adapter` 뒤 병렬 PoC

한 줄 결론:
`Spring Boot + Kotlin`은 채택하고, `DDD`는 적용하되, `MSA`는 경계만 먼저 설계하고 MVP는 모듈러 모놀리스로 가는 것이 맞다.
