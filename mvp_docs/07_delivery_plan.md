# Delivery Plan

상위 WBS와 Mermaid 마일스톤 시각화는 [17_development_wbs_milestones.md](/C:/Users/User/Documents/work/mvp_docs/17_development_wbs_milestones.md)를 기준으로 본다.

## 1. Planning Rule

- 구현 단위는 `화면`, `API`, `DB`, `배치`, `운영도구`로 나눈다.
- 각 티켓은 반드시 한 개 이상의 기준 문서를 가진다.
- OpenRAG 검증 티켓은 제품 본체 티켓과 분리한다.
- 우선순위는 `서비스 동작` -> `운영 가시성` -> `개선 루프` -> `집계/리포트` 순서로 둔다.

## 2. Milestone Structure

### Milestone 1. Foundation

- 목표: 기관 스코프와 관리자 접근 제어, 감사 로그 기반을 고정한다.
- 완료 기준: 운영사와 고객사 관리자가 같은 시스템에서 다른 권한으로 로그인하고 조직 스코프가 분리된다.

### Milestone 2. Chat Runtime

- 목표: 질문, 답변, 출처, retrieval 로그가 누락 없이 저장된다.
- 완료 기준: `POST /chat/questions` 호출 시 질문, 답변, 검색 로그, 출처 문서 연결이 한 트랜잭션 흐름으로 남는다.

### Milestone 3. QA Loop

- 목표: 미해결 질문을 조회하고 검수 결과를 저장할 수 있다.
- 완료 기준: `GET /admin/questions/unresolved`, `GET /admin/questions/{id}`, `POST /admin/qa-reviews`가 화면과 연결된다.

### Milestone 4. Document Ops

- 목표: 문서 상태와 재수집/재인덱싱 액션을 운영 화면에서 제어할 수 있다.
- 완료 기준: 문서 목록, 상태, 수동 액션, 검색 실패 로그를 운영사가 확인할 수 있다.

### Milestone 5. Dashboard

- 목표: 운영사와 고객사가 서로 다른 KPI를 조회할 수 있다.
- 완료 기준: 운영사 대시보드, 고객사 대시보드, 일별 KPI 집계가 기본 지표를 반환한다.

## 3. Track A: Product Foundation

### A-1. DB

- 티켓: `organizations`, `services` 테이블 확정
- 티켓: 관리자 계정과 역할 매핑 테이블 추가
- 티켓: 감사로그 테이블 추가
- 완료 기준: 기관, 서비스, 역할, 액션 로그를 저장할 수 있다.
- 기준 문서: `04_data_api.md`, `06_access_policy.md`

### A-2. Backend

- 티켓: 기관 스코프 미들웨어 추가
- 티켓: 역할 기반 접근 제어 추가
- 티켓: 공통 에러 응답 규격 적용
- 완료 기준: 화면/API 요청이 `ops_admin`, `client_admin`, `qa_admin` 정책과 일치한다.
- 기준 문서: `04_data_api.md`, `06_access_policy.md`

### A-3. Frontend

- 티켓: 로그인 이후 역할별 메뉴 분기
- 티켓: 권한 없는 화면 접근 차단
- 완료 기준: 역할별로 허용된 메뉴만 노출된다.
- 기준 문서: `03_screen_spec.md`, `06_access_policy.md`

## 4. Track B: Chat And Logging

### B-1. DB

- 티켓: `chat_sessions`, `questions`, `answers` 스키마 반영
- 티켓: `rag_search_logs`, `rag_retrieved_documents` 스키마 반영
- 완료 기준: 질문 단위로 답변과 retrieval 근거를 조인할 수 있다.
- 기준 문서: `04_data_api.md`

### B-2. Backend

- 티켓: `POST /chat/questions` 구현
- 티켓: 답변 상태값과 retrieval 상태값 저장 로직 구현
- 티켓: source document summary 응답 포맷 구현
- 완료 기준: 챗봇 호출 결과가 API 계약과 동일한 JSON으로 반환된다.
- 기준 문서: `04_data_api.md`

### B-3. Integration

- 티켓: RAG Adapter 인터페이스 정의
- 티켓: 기본 RAG 구현체 연결
- 티켓: 타임아웃/zero-result/fallback 로그 처리
- 완료 기준: RAG 구현체를 바꿔도 제품 API 계약은 변경되지 않는다.
- 기준 문서: `05_architecture_openrag.md`

## 5. Track C: QA And Improvement Loop

### C-1. DB

- 티켓: `qa_reviews` 스키마와 상태값 반영
- 티켓: root cause, action type 코드 테이블 또는 enum 반영
- 완료 기준: 검수 이력과 조치 타입이 누락 없이 저장된다.
- 기준 문서: `04_data_api.md`

### C-2. Backend

- 티켓: `GET /admin/questions/unresolved` 구현
- 티켓: `GET /admin/questions/{id}` 구현
- 티켓: `POST /admin/qa-reviews` 구현
- 완료 기준: 검수 목록, 상세, 처리 저장이 API 계약과 일치한다.
- 기준 문서: `04_data_api.md`

### C-3. Frontend

- 티켓: 미해결 질문 목록 화면 구현
- 티켓: 질문 상세와 retrieval 근거 패널 구현
- 티켓: QA 검수 입력 폼 구현
- 완료 기준: QA 담당자가 한 화면 흐름에서 검수 저장까지 끝낼 수 있다.
- 기준 문서: `03_screen_spec.md`, `06_access_policy.md`

## 6. Track D: Document And RAG Operations

### D-1. DB

- 티켓: `documents`, `document_chunks` 스키마 반영
- 티켓: ingestion/index 상태값 저장 규칙 반영
- 완료 기준: 문서 단위와 청크 단위 상태를 분리해 추적할 수 있다.
- 기준 문서: `04_data_api.md`

### D-2. Backend

- 티켓: `GET /admin/documents` 구현
- 티켓: `POST /admin/documents/{id}/reindex` 구현
- 티켓: `POST /admin/documents/{id}/reingest` 구현
- 티켓: `GET /admin/rag/search-logs` 구현
- 완료 기준: 운영사가 문서 운영 액션과 검색 실패 현황을 수행/조회할 수 있다.
- 기준 문서: `04_data_api.md`

### D-3. Frontend

- 티켓: 문서 목록과 상태 필터 구현
- 티켓: 재인덱싱/재수집 액션 버튼 구현
- 티켓: 검색 실패 로그 화면 구현
- 완료 기준: 운영사 화면에서 문서 운영 액션이 막힘 없이 수행된다.
- 기준 문서: `03_screen_spec.md`, `06_access_policy.md`

## 7. Track E: Dashboards And Metrics

### E-1. DB And Batch

- 티켓: `daily_metrics_org` 스키마 반영
- 티켓: 일별 KPI 집계 배치 구현
- 티켓: 지표 재계산 기준일 파라미터 추가
- 완료 기준: 일별 기관 KPI를 재생성할 수 있다.
- 기준 문서: `04_data_api.md`

### E-2. Backend

- 티켓: `GET /admin/ops/dashboard` 구현
- 티켓: `GET /admin/client/dashboard` 구현
- 티켓: `GET /admin/metrics/daily` 구현
- 완료 기준: 운영사와 고객사에 서로 다른 KPI 묶음을 반환한다.
- 기준 문서: `04_data_api.md`

### E-3. Frontend

- 티켓: 운영사 대시보드 카드/차트 구현
- 티켓: 고객사 대시보드 카드/차트 구현
- 티켓: 공통 필터 바 구현
- 완료 기준: 기간, 기관, 서비스 기준으로 KPI를 조회할 수 있다.
- 기준 문서: `03_screen_spec.md`

## 8. Dependency Rule

- Track A는 모든 트랙의 선행 조건이다.
- Track B 완료 전에는 Track C 상세 화면을 완성할 수 없다.
- Track D는 Track B의 문서/검색 로그 구조를 재사용한다.
- Track E는 Track B, Track C 이벤트가 쌓여야 지표 정확도가 확보된다.
- OpenRAG PoC는 Track B Integration 이후에만 붙인다.

## 9. Suggested Sprint Cut

### Sprint 1

- Track A 전부
- Track B의 DB/Backend

### Sprint 2

- Track B Frontend/Integration
- Track C 전부

### Sprint 3

- Track D 전부
- Track E의 DB/Batch

### Sprint 4

- Track E Frontend/Backend 마감
- 안정화, 권한, 감사로그 누락 점검

## 10. Ready / Done Criteria

### Ready

- 기준 문서 링크가 있다.
- 상태값과 에러 규격이 정의돼 있다.
- 화면 또는 API 소비 주체가 명확하다.

### Done

- API 계약 또는 화면 정책과 구현이 일치한다.
- 감사로그 또는 운영 로그가 필요한 액션에 연결돼 있다.
- 권한 검증이 포함돼 있다.
- 기본 성공/실패 케이스가 테스트됐다.

## 11. OpenRAG PoC Split

### MVP Fixed Scope

- Admin Web
- Product API / BFF
- Product DB
- KPI 집계
- QA 워크플로우

### OpenRAG PoC Scope

- ingestion 성능 검토
- retrieval 품질 비교
- flow 조정 가능성 검토
- 운영 복잡도 확인

### PoC Ticket Pack

- 티켓: OpenRAG 로컬/WSL 실행 경로 검증
- 티켓: 공공 문서 샘플 ingestion 실행
- 티켓: 우리 API 응답 포맷으로 매핑 가능한지 검증
- 티켓: 검색 로그 재정규화 가능 여부 확인
- 티켓: 운영 복잡도와 장애 포인트 기록

## 12. PoC Success Criteria

- 문서 ingestion가 공공 문서 형식에서 안정적으로 동작한다.
- 검색 품질이 최소 기준을 충족한다.
- 우리 스키마로 로그 재정규화가 가능하다.
- 운영 복잡도가 MVP 일정과 충돌하지 않는다.
