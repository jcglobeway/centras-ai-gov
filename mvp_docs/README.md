# MVP Deliverables Index

## Purpose

이 폴더는 `planning_draft.md`, `planning_summary.md`, 기존 `mvp_worklog.md`를 바탕으로
MVP 산출물을 주제별로 분리한 구조화 문서 모음이다.

단일 작업 로그에 모든 내용을 누적하지 않고, 아래처럼 읽는 문서와 기록 문서를 분리한다.

## Document Map

1. [01_mvp_prd.md](/C:/Users/User/Documents/work/mvp_docs/01_mvp_prd.md)
MVP 목표, 범위, 사용자, KPI, 성공 기준

2. [02_role_flows.md](/C:/Users/User/Documents/work/mvp_docs/02_role_flows.md)
운영사, 고객사, QA 역할별 핵심 운영 플로우

3. [03_screen_spec.md](/C:/Users/User/Documents/work/mvp_docs/03_screen_spec.md)
MVP 우선 화면과 목적, 핵심 데이터, 주요 액션, ingestion 운영 화면

4. [04_data_api.md](/C:/Users/User/Documents/work/mvp_docs/04_data_api.md)
MVP 최소 데이터 모델, ingestion 메타데이터, 이벤트 로그, API 우선순위

5. [05_architecture_openrag.md](/C:/Users/User/Documents/work/mvp_docs/05_architecture_openrag.md)
시스템 경계, OpenRAG 검토, 도입 방식 판단

6. [06_access_policy.md](/C:/Users/User/Documents/work/mvp_docs/06_access_policy.md)
화면별 권한, 액션 정책, 감사로그 기준

7. [07_delivery_plan.md](/C:/Users/User/Documents/work/mvp_docs/07_delivery_plan.md)
개발 트랙, 구현 순서, PoC 분리 기준

8. [08_traceability_matrix.md](/C:/Users/User/Documents/work/mvp_docs/08_traceability_matrix.md)
화면, 권한 액션, API, 핵심 테이블의 연결 관계와 Sprint 1 구현 컷, ingestion 추적 키

9. [09_unresolved_qa_state_machine.md](/C:/Users/User/Documents/work/mvp_docs/09_unresolved_qa_state_machine.md)
미해결 질문, QA 검수, 후속 조치 상태 전이 규칙

10. [10_auth_authz_api.md](/C:/Users/User/Documents/work/mvp_docs/10_auth_authz_api.md)
관리자 인증, 세션, 역할, 권한 검사 API 계약

11. [11_traceability_test_cases.md](/C:/Users/User/Documents/work/mvp_docs/11_traceability_test_cases.md)
traceability 기준 최소 E2E, 권한, 상태 전이, 집계 정합성 테스트 시나리오

12. [12_test_strategy.md](/C:/Users/User/Documents/work/mvp_docs/12_test_strategy.md)
테스트 기준을 단위 테스트, API 테스트, E2E 테스트, 데이터 검증으로 분해한 실행 전략

13. [13_test_runner_structure.md](/C:/Users/User/Documents/work/mvp_docs/13_test_runner_structure.md)
테스트 러너 책임, 폴더 구조, fixture/seed 규칙, CI 실행 순서

14. [14_test_file_scaffold.md](/C:/Users/User/Documents/work/mvp_docs/14_test_file_scaffold.md)
Sprint 1 기준 테스트 파일명, 최소 책임, fixture/helper 연결, 생성 순서

15. [15_ingestion_browser_review.md](/C:/Users/User/Documents/work/mvp_docs/15_ingestion_browser_review.md)
ingestion 브라우저 런타임, Playwright 기본값, Lightpanda/OpenRAG PoC 경계

16. [16_springboot_kotlin_ddd_msa_review.md](/C:/Users/User/Documents/work/mvp_docs/16_springboot_kotlin_ddd_msa_review.md)
Spring Boot, Kotlin, DDD, MSA 방향성 검토와 Python ingestion/RAG 하이브리드 권장안

17. [99_worklog.md](/C:/Users/User/Documents/work/mvp_docs/99_worklog.md)
진행 기록과 다음 액션

## Implementation Status

- 문서 기준 구조를 반영한 초기 저장소 골격이 생성됐다.
- 루트에는 `Spring Boot + Kotlin` 멀티모듈과 `python` 워크스페이스, `tests` 진입 구조가 준비됐다.
- 다음 구현 시작점은 `apps/admin-api`의 인증/조직 스코프 API와 `python/ingestion-worker`의 crawl source 실행 루프다.

## Operating Rule

- 새로운 설계 결론은 해당 주제 문서에 먼저 반영한다.
- 진행 이력과 의사결정 메모는 `99_worklog.md`에 누적한다.
- 단일 문서에 전체 설계를 다시 합치지 않는다.
