# MVP Worklog

## 2026-03-15

### Summary

- 문서 기준 합의를 실제 저장소 골격으로 전환
- `Spring Boot + Kotlin` 멀티모듈 루트와 `apps/admin-api` 부트 앱 생성
- bounded context 기준 `modules/*` 초기 구조 생성
- `python/ingestion-worker`, `python/rag-orchestrator`, `tests/*` 진입 구조 생성
- 단일 작업 로그 중심 구조를 해체하고 주제별 문서 구조로 재편
- MVP 목표, 역할 플로우, 화면, 데이터/API, 아키텍처, 권한, 전달 계획을 분리
- OpenRAG는 MVP 본체가 아니라 병렬 PoC 대상으로 유지
- `04_data_api.md`를 요청/응답 예시, 상태값, 인덱스 초안 수준까지 상세화
- `03_screen_spec.md`에 화면별 상태값과 필터 규칙을 추가
- `05_architecture_openrag.md`에 PoC 범위, 체크리스트, 종료 기준을 추가
- `07_delivery_plan.md`를 마일스톤, 트랙별 구현 티켓, 의존성, Sprint 컷 기준으로 상세화
- `08_traceability_matrix.md`를 추가해 화면, API, 핵심 테이블 매핑을 한 장으로 정리
- `09_unresolved_qa_state_machine.md`를 추가해 unresolved/QA 상태 전이와 집계 규칙을 분리
- `10_auth_authz_api.md`를 추가해 관리자 인증, 세션, 역할, 권한 검사 API 계약을 분리
- `08_traceability_matrix.md`에 화면별 선행 권한과 Sprint 1 인증/권한 의존성을 연결
- `11_traceability_test_cases.md`를 추가해 화면, 권한, API, DB 기준 최소 회귀팩과 E2E 시나리오를 고정
- `12_test_strategy.md`를 추가해 traceability 테스트를 unit, api, e2e, data 검증 레벨로 분해
- `13_test_runner_structure.md`를 추가해 테스트 러너 책임, 폴더 구조, fixture/seed 경계, CI 실행 순서를 고정
- `14_test_file_scaffold.md`를 추가해 Sprint 1 테스트 파일명, 최소 책임, helper/fixture 연결, 생성 순서를 고정
- `15_ingestion_browser_review.md`를 추가해 crawling 런타임, Playwright 기본값, Lightpanda/OpenRAG PoC 경계를 정리
- `04_data_api.md`에 `crawl_sources`, `document_versions`, `ingestion_jobs`와 ingestion API 계약을 추가
- `03_screen_spec.md`에 `Crawl Source Management`, `Ingestion Job Monitor` 화면을 추가
- `08_traceability_matrix.md`에 ingestion 화면의 권한, API, DB, trace 키 연결을 추가
- `16_springboot_kotlin_ddd_msa_review.md`를 추가해 Spring Boot, Kotlin, DDD, MSA 방향성과 Python ingestion/RAG 하이브리드 권장안을 정리
- `apps/admin-api`에 `GET /healthz`, `GET /admin/auth/me` 개발용 엔드포인트와 MockMvc 테스트를 추가
- 루트에 `ENV_SETUP.md`를 추가해 Windows 기준 JDK 21, Gradle Wrapper, 첫 검증 절차를 정리
- 사용자 영역 `JDK 21`, `Gradle 9.4.0`, `Gradle Wrapper`를 준비하고 `.\gradlew.bat test` 통과 확인
- `identity-access`, `organization-directory` 모듈에 세션 복원/조직 조회 계약을 추가하고 `admin-api`를 모듈 계약 기반으로 정리
- 작업 폴더를 `git init -b main`으로 초기화하고 로컬 Git 추적 기반을 마련
- `ingestion-ops` 모듈에 crawl source/job 조회 계약을 추가하고 `admin-api`에 범위 기반 조회 엔드포인트를 연결
- `identity-access`에 `AdminSessionRepository` 포트를 추가하고 `X-Admin-Session-Id` 기반 세션 복원 경로를 연결
- OpenSpec 기반 변경 추적 구조와 템플릿을 추가하고, 이후 중요한 변경은 change 단위로 관리하기로 결정

### Current Decision

- 제품 중심은 `Admin/API/DB`다.
- OpenRAG는 `RAG Adapter` 뒤에서만 선택적으로 검토한다.
- 신규 논의 내용은 해당 주제 문서에 먼저 반영한다.
- API 계약은 RAG 구현체와 분리해 제품 스키마 기준으로 고정한다.
- 전달 계획은 `Track -> DB/Backend/Frontend -> 완료 기준` 구조로 관리한다.
- 구현 영향도 판단은 `08_traceability_matrix.md`를 기준으로 본다.
- unresolved/QA 운영 상태는 `answer_status`, `review_status`, `resolution_status` 3층 구조로 관리한다.
- 인증/권한 계약은 화면 정책과 분리해 `세션 -> 역할 -> 액션 -> 조직 범위` 흐름으로 고정한다.
- traceability 기준 문서는 이제 `화면 -> 권한 액션 -> API -> DB` 순서로 본다.
- 테스트 기준은 `traceability -> 상태 전이 -> API 계약` 순서로 연결한다.
- 자동화 전략은 `unit -> api -> e2e -> data verification` 순서로 진행한다.
- 테스트 저장소 구조는 `tests/unit`, `tests/api`, `tests/e2e`, `tests/data` 4축으로 고정한다.
- OpenRAG는 테스트에서도 제품 계약 바깥의 stub 대상이다.
- Sprint 1 테스트 착수 기준은 문서가 아니라 실제 파일명 단위로 관리한다.
- ingestion 기본 브라우저 런타임은 `Playwright`다.
- Lightpanda는 `기본 채택`이 아니라 `선별 URL 대상 병렬 PoC`다.
- `OpenRAG + Lightpanda` 동시 기본 채택은 MVP 기간에 보류한다.
- ingestion 시스템 오브 레코드는 `crawl_sources -> ingestion_jobs -> documents/document_versions` 흐름으로 관리한다.
- ingestion 운영 화면은 `source 관리`와 `job 모니터링`을 분리한다.
- ingestion 추적 키는 `crawl_source_id`, `ingestion_job_id`, `document_id`를 함께 본다.
- 제품 본체 백엔드의 현재 우선안은 `Spring Boot + Kotlin`이다.
- 아키텍처 기본값은 `DDD-lite modular monolith + Python workers`다.
- `full MSA`는 MVP 즉시 채택이 아니라 추후 분리 가능한 경계 설계 대상으로 둔다.
- 저장소 초기 골격은 이미 생성했고, 다음 단계는 각 모듈 계약과 엔드포인트를 채우는 것이다.
- 로컬 부트스트랩은 완료됐고 기본 실행 명령은 `.\gradlew.bat test` 다.
- `admin-api`는 이제 모듈 계약을 통해 세션을 복원하며, 현재 구현체는 개발용 인메모리 adapter 다.
- 개발 환경에서도 `X-Admin-Session-Id` 경로를 우선 사용해 저장소 기반 세션 복원을 먼저 검증한다.
- ingestion 조회도 세션 조직 범위를 따라가며, 전역 역할은 전체 source/job을 조회한다.
- 중요한 변경은 `OpenSpec change -> 구현 -> 검증 -> 한글 커밋` 순서로 관리한다.

### Next Actions

1. `modules/ingestion-ops`에 쓰기 계약과 job 상태 전이 규칙을 추가
2. `identity-access`에 실제 세션 저장소/권한 부여 규칙 포트를 분리
3. `python/ingestion-worker`에 crawl source 실행 흐름과 job callback 스텁 추가
4. `tests/api`, `tests/e2e`에 ingestion 범위 회귀 케이스를 추가
5. 다음 구현부터 `openspec/changes/<change-id>`를 먼저 생성하고 진행 상태를 갱신
