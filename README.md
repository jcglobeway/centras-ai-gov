# Public RAG Ops Platform

문서 단계에서 합의한 MVP 구조를 실제 저장소 골격으로 옮긴 초기 리포지토리다.

## Stack Direction

- Admin Web: `Next.js` 예정
- Product Core: `Spring Boot + Kotlin`
- Architecture: `DDD-lite modular monolith`
- AI Side System: `Python ingestion-worker`, `Python rag-orchestrator`
- Storage: `PostgreSQL`, `Redis`, object storage
- Retrieval: `OpenSearch` 또는 `pgvector`

## Current Layout

- `apps/admin-api`
- `modules/shared-kernel`
- `modules/identity-access`
- `modules/organization-directory`
- `modules/chat-runtime`
- `modules/document-registry`
- `modules/ingestion-ops`
- `modules/qa-review`
- `modules/metrics-reporting`
- `python/ingestion-worker`
- `python/rag-orchestrator`
- `python/common`
- `tests/unit`
- `tests/api`
- `tests/e2e`
- `tests/data`
- `mvp_docs`

## Immediate Next Steps

1. `admin-api`의 개발용 session adapter를 실제 세션 저장소 기반 구현으로 교체한다.
2. `modules/ingestion-ops`를 쓰기 계약과 상태 전이 규칙까지 확장한다.
3. `python/ingestion-worker`에 crawl source 실행 루프를 추가한다.
4. `python/rag-orchestrator`에 adapter 인터페이스와 citation 응답 계약을 추가한다.

## Bootstrap

- 환경 설치 가이드: `ENV_SETUP.md`
- 현재 상태: `JDK 21`, `Gradle 9.4.0`, `gradlew.bat` 준비 완료
- 저장소 상태: `git init -b main` 완료
- 기본 실행: `.\gradlew.bat test`
