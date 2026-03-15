# Ingestion Browser And Crawling Stack Review

## 1. Goal

- Spring Boot 본체와 분리된 `Python ingestion / RAG orchestration` 서브시스템 기준으로 크롤링 런타임 후보를 검토한다.
- `OpenRAG` 접목 가능성과 `Lightpanda` 같은 경량 브라우저 런타임의 도입 가능성을 같이 판단한다.
- 결론은 `Sprint 1 기본값`, `병렬 PoC 후보`, `보류 항목`으로 나눈다.

## 2. Recommended Layer Split

- 제품 본체: `Next.js + Spring Boot + PostgreSQL + Redis`
- ingestion 제어: Spring Boot가 `crawl_sources`, `documents`, `ingestion_jobs`를 시스템 오브 레코드로 관리
- 실행 워커: Python worker가 crawl, parse, normalize, chunk, embed, index를 수행
- RAG orchestration: Python 또는 OpenRAG 기반 서브시스템이 retrieval and generation을 수행
- 제품 계약: 결과는 항상 `documents`, `document_versions`, `rag_search_logs`, `rag_retrieved_documents` 기준으로 정규화

## 3. Baseline Crawling Stack

기본 권장안:

- 정적 수집: `httpx + trafilatura` 또는 `BeautifulSoup`
- 동적 렌더링 수집: `Playwright`
- 파일 파싱: `Docling` 우선 검토
- 비동기 실행: `Celery` 또는 `RQ`
- 색인 대상 저장: `OpenSearch` 또는 `pgvector`

이 조합을 기본값으로 두는 이유는 런타임 안정성보다 제품 계약과 운영성을 먼저 확보해야 하기 때문이다.

## 4. Lightpanda Review

공식 저장소 기준 사실:

- Lightpanda는 headless 용도로 만든 오픈소스 브라우저다.
- JavaScript 실행을 지원하지만 Web API 지원은 `partial, WIP`로 명시돼 있다.
- `Playwright`, `Puppeteer`, `chromedp`와 `CDP`를 통해 호환된다고 설명한다.
- 성능 주장으로 `9x less memory`, `11x faster than Chrome`를 제시한다.
- 설치 경로는 현재 `nightly builds`와 Docker 이미지 중심이다.
- Windows 환경은 직접 네이티브가 아니라 `WSL2` 안에서 실행하는 방식을 안내한다.
- README 기준 상태는 `Beta`이며, 에러나 크래시를 만날 수 있다고 적고 있다.

해석:

- 장점: 고밀도 URL ingestion, 메모리 절감, CDP 기반 연결 실험에는 매력적이다.
- 한계: Web API 커버리지와 배포 안정성이 아직 Chrome 계열 대체 수준이라고 보긴 어렵다.
- 추가 리스크: Playwright가 공식 문서에서 `connectOverCDP`는 Playwright 고유 프로토콜 연결보다 fidelity가 낮다고 밝히고 있다.

따라서 `Playwright client -> Lightpanda CDP server` 조합은 가능하지만, 이 프로젝트에선 실험적 요소가 두 겹이다.
이 판단은 공식 자료를 근거로 한 추론이다.

## 5. OpenRAG Relationship

OpenRAG 공식 문서 기준 사실:

- Quickstart는 `Python 3.13`, `uv`, OpenAI API key를 전제로 한다.
- Windows에서는 `WSL` 사용이 필수다.
- quickstart 설치 과정은 의존성과 컨테이너를 함께 준비하고, Langflow와 OpenSearch 기반 서비스를 띄우는 흐름이다.

의미:

- OpenRAG는 `ingestion / retrieval / orchestration` 실험 플랫폼으로는 적합하다.
- 하지만 Windows 운영 환경에서 `OpenRAG + Lightpanda`를 함께 기본 경로로 두면 WSL 의존과 운영 복잡도가 더 커진다.
- 따라서 둘 다 MVP 본체가 아니라 `Python ingestion / RAG adapter` 뒤쪽 PoC 트랙으로 다루는 편이 안전하다.

## 6. Decision By Phase

### Sprint 1 Default

- 동적 페이지 수집 런타임은 `Playwright`를 기본값으로 둔다.
- Lightpanda는 기본 ingest 런타임으로 채택하지 않는다.
- OpenRAG는 기본 retrieval backend로 채택하지 않는다.

이유:

- 제품 팀이 먼저 검증해야 하는 것은 `권한`, `조직 범위`, `감사로그`, `문서 상태`, `QA traceability`다.
- ingest 런타임 자체가 불안정하면 `documents.ingestion_status`, `reindex`, `QA` 운영 판단이 모두 흔들린다.

### Parallel PoC

- Lightpanda는 `대량 URL fetch` 또는 `JS 렌더링이 가벼운 공공 페이지`를 대상으로 별도 PoC를 한다.
- OpenRAG는 `retrieval quality`, `citation`, `chunk traceability` 관점으로 별도 PoC를 한다.
- 두 PoC 모두 제품 API와 DB 계약을 바꾸지 않는 조건에서만 검토한다.

### Hold

- `Lightpanda + OpenRAG`를 동시에 Sprint 1 핵심 경로에 넣는 결정
- 관리자 기능과 QA 검수 로그를 OpenRAG 내부 모델에 의존시키는 결정
- Windows host 기준 네이티브 운영을 가정한 Lightpanda 채택

## 7. Practical Adoption Pattern

현실적인 적용 순서:

1. Spring Boot가 crawl source 등록과 ingestion job 생성을 담당한다.
2. Python worker는 기본적으로 `httpx/trafilatura`와 `Playwright`를 사용한다.
3. 특정 사이트군에서 Chrome 기반 브라우저 비용이 큰 경우에만 Lightpanda worker pool을 별도 트랙으로 붙인다.
4. retrieval backend는 초기엔 단순 스택으로 두고, 이후 OpenRAG adapter를 병렬 검증한다.
5. 어느 구현체를 쓰든 제품 저장 스키마는 바꾸지 않는다.

## 8. Lightpanda PoC Entry Criteria

- 대상 사이트가 Chrome 전용 고급 API 의존이 크지 않다.
- 필요한 기능이 `click`, `form`, `fetch/xhr`, `cookie`, `header`, `proxy` 수준에서 해결된다.
- 실패 시 즉시 Playwright 경로로 fallback 할 수 있다.
- 운영 환경이 `Linux` 또는 `WSL2` 기준으로 감당 가능하다.

## 9. Recommendation

- 기본안: `Spring Boot + Python worker + Playwright + OpenSearch/pgvector`
- PoC안: `Spring Boot + Python worker + Lightpanda subset + OpenRAG subset`
- 최종 판단: Lightpanda는 `검토 가치 있음`, 하지만 현재는 `기본 채택`보다 `선별된 URL ingestion 실험`이 맞다.

현재 문서 세트 기준 최종 권고는 아래와 같다.

- 제품 본체는 `Spring Boot` 중심으로 간다.
- crawling ingest 기본값은 `Playwright`로 둔다.
- Lightpanda는 `대량 수집 최적화` 후보로 병렬 PoC만 진행한다.
- OpenRAG는 `retrieval/orchestration` 후보로 병렬 PoC만 진행한다.

## 10. Sources

- Lightpanda GitHub README: https://github.com/lightpanda-io/browser
- Playwright `connectOverCDP` docs: https://playwright.dev/docs/api/class-browsertype#browser-type-connect-over-cdp
- OpenRAG Quickstart: https://docs.openr.ag/quickstart/
- OpenRAG Windows install guide: https://docs.openr.ag/install-windows/
