# Architecture And OpenRAG Review

## 1. Fixed Product Layers

- Citizen Chat UI
- Admin Web
- API / BFF
- Chat Orchestrator
- QA / Metrics Workers
- Product DB
- Audit Log

## 2. Swappable RAG Layer

RAG 실행 계층은 아래 둘 중 하나로 교체 가능하게 둔다.

- 자체 구현 retrieval and generation pipeline
- OpenRAG 기반 retrieval and generation subsystem

## 3. Recommended Request Flow

1. 시민 질문이 API로 유입된다.
2. Chat Orchestrator가 RAG Adapter를 호출한다.
3. RAG Adapter가 검색과 생성 결과를 반환한다.
4. 제품 API가 질문, 답변, retrieval 로그를 Product DB에 저장한다.
5. 이후 QA와 KPI 계층이 같은 데이터를 사용한다.

## 4. What OpenRAG Can Help With

- 문서 ingestion 실험
- 검색 파이프라인 실험
- OpenSearch 기반 인덱스 검증
- Langflow 기반 RAG 플로우 조정

## 5. What OpenRAG Does Not Replace

- 운영사 Admin
- 고객사 Admin
- QA 검수 UI
- 기관별 KPI 집계
- 감사로그
- 기관별 권한과 운영 정책

## 6. Environment Fit

- 현재 환경에서는 `WSL + Docker` 기반 검토가 현실적이다.
- PowerShell 단독 운영 환경을 전제로 바로 제품 핵심에 채택하는 것은 위험하다.
- 따라서 MVP 본체와 분리된 PoC 트랙으로 검토하는 것이 적절하다.

## 7. Recommendation

- MVP는 OpenRAG 비의존적으로 설계한다.
- OpenRAG는 `RAG Adapter` 뒤쪽에서만 선택적으로 붙인다.
- 질문, 답변, 검색 로그는 항상 우리 Product DB 기준으로 저장한다.

## 8. Decision

현재 권장안은 `제품 중심 설계 + OpenRAG 병렬 PoC`다.

## 9. OpenRAG PoC Scope

PoC 목적:
- ingestion, retrieval, generation 기본 파이프라인이 공공기관 문서셋에서 동작하는지 검증
- Product DB 스키마를 유지한 채 어댑터 계층으로 연동 가능한지 검증
- 운영용 본체가 아닌 실험용 RAG 백엔드 후보로 적합한지 판단

PoC 제외 범위:
- 운영사 Admin 대체
- 고객사 KPI 대시보드 대체
- QA 검수 워크플로우 대체
- 권한, 감사로그, 멀티기관 거버넌스 대체

## 10. OpenRAG PoC Checklist

### Environment

- `WSL + Docker`에서 기본 실행 가능 여부 확인
- 로컬 또는 사내 검증 환경에서 OpenSearch 연결 가능 여부 확인
- 샘플 문서 20~50건 기준 인덱싱 완료 시간 측정

### Ingestion

- PDF, HTML, 공고문 문서 포맷 처리 가능 여부 확인
- 문서 메타데이터를 `organization_id`, `service_id`, `document_type`와 함께 매핑 가능한지 확인
- 재수집 또는 재인덱싱 재실행 시 idempotent 처리 가능 여부 확인

### Retrieval

- zero-result 사례를 로그로 꺼낼 수 있는지 확인
- retrieved chunk와 원문 문서 식별자를 우리 스키마로 역매핑 가능한지 확인
- top-k, score, latency를 API 응답에 정규화 가능 여부 확인

### Generation

- 답변과 citation을 분리해서 받을 수 있는지 확인
- citation 누락 시 fallback 규칙 적용이 가능한지 확인
- 응답 시간과 실패 원인을 애플리케이션 로그로 수집 가능한지 확인

### Integration

- `POST /chat/questions` 경로에 어댑터 방식으로 연결 가능한지 확인
- `rag_search_logs`, `rag_retrieved_documents` 저장 필드가 충분한지 확인
- OpenRAG 장애 시 자체 fallback 또는 no-answer 응답으로 안전 종료 가능한지 확인

### Evaluation

- 샘플 질문 세트 기준 answer success rate 측정
- zero-result rate, 평균 latency, citation coverage 측정
- 운영 관점에서 수동 QA 검수가 가능한 수준으로 로그가 남는지 확인

## 11. PoC Exit Criteria

- 샘플 문서셋 인덱싱과 질의응답이 안정적으로 재현된다.
- 최소 1개 서비스에서 citation 포함 응답이 Product API 계약과 맞는다.
- retrieval 로그를 우리 DB 스키마에 무리 없이 적재할 수 있다.
- 장애 또는 timeout 시 사용자 응답이 안전하게 fallback 된다.

위 조건을 만족하면 `RAG Adapter` 뒤의 선택지로 유지한다.
만족하지 못하면 MVP 기간에는 자체 구현 또는 더 단순한 검색 스택으로 간다.

## 12. Related Ingestion Review

- crawling 브라우저 런타임과 Lightpanda 검토는 [15_ingestion_browser_review.md](/C:/Users/User/Documents/work/mvp_docs/15_ingestion_browser_review.md)에서 별도로 관리한다.
- 현재 기준 기본값은 `Python worker + Playwright`이고, OpenRAG와 Lightpanda는 둘 다 병렬 PoC 트랙이다.

## 13. Spring Boot Core Direction

- 제품 본체 백엔드는 `Spring Boot + Kotlin` 방향으로 검토한다.
- 다만 MVP 구조는 `full MSA`보다 `DDD-lite modular monolith`를 우선한다.
- ingestion, crawling, RAG orchestration 만 Python 서브시스템으로 분리하는 하이브리드 구성이 현재 기준 권장안이다.
- 상세 판단은 [16_springboot_kotlin_ddd_msa_review.md](/C:/Users/User/Documents/work/mvp_docs/16_springboot_kotlin_ddd_msa_review.md)에서 별도로 관리한다.
