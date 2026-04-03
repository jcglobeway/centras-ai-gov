# Proposal: testcontainers-migration

## Problem

현재 테스트 환경은 H2 in-memory DB(MODE=PostgreSQL)를 사용하고 있으며, 이로 인해 세 가지 구조적 문제가 발생한다.

**1. pgvector 타입 우회 처리**
- `V016__create_document_chunks.sql`: `embedding_vector` 컬럼을 `TEXT`로 선언 (pgvector 불가)
- `V018__EnablePgVector.kt`: H2 감지 시 early return — pgvector extension 활성화 및 `vector(1024)` 타입 변환 skip
- `V029__add_question_embedding.sql`: `question_embedding` 컬럼을 `TEXT`로 선언
- `V030__QuestionEmbeddingVector.kt`: H2 감지 시 early return — 타입 변환 및 HNSW 인덱스 생성 skip

**2. 벡터 검색 쿼리 미검증**
- pgvector 코사인 유사도 쿼리(`<=>` 연산자), `::vector` 캐스트 등 PostgreSQL 전용 SQL이 테스트에서 전혀 실행되지 않는다.
- 실제 운영 환경에서만 이 경로가 실행되므로 회귀 탐지가 불가능하다.

**3. SQL 방언 불일치**
- H2 MODE=PostgreSQL이 모든 PostgreSQL 구문을 지원하지 않아, 일부 SQL 마이그레이션에서 방언 차이로 인한 잠재적 오류가 숨겨진다.

## Proposed Solution

Spring Boot 3.x의 `@ServiceConnection` + Testcontainers를 사용해 테스트 환경을 실제 PostgreSQL(pgvector 내장)로 교체한다.

### 핵심 변경

1. **의존성 교체** (`build.gradle.kts`)
   - 제거: `testImplementation("com.h2database:h2")`
   - 추가: `testImplementation("org.springframework.boot:spring-boot-testcontainers")`
   - 추가: `testImplementation("org.testcontainers:postgresql")`

2. **BaseApiTest 싱글턴 컨테이너 패턴** (`BaseApiTest.kt`)
   - `companion object`에 `static @Container PostgreSQLContainer("pgvector/pgvector:pg16")` 선언
   - `@Testcontainers` + `@ServiceConnection` 자동 연결 — datasource URL/credentials 수동 설정 불필요
   - 컨테이너는 모든 테스트 클래스에서 재사용 (JVM당 1회 기동)

3. **application-test.yml 정리**
   - H2 datasource URL, driver-class-name, H2Dialect 설정 제거
   - Spring Boot `@ServiceConnection`이 datasource를 자동 주입하므로 datasource 섹션 불필요

4. **pgvector 마이그레이션 단순화** (선택적)
   - V016: `embedding_vector TEXT` → `embedding_vector vector(1024)` (H2 우회 불필요)
   - V018: H2 감지 로직 제거, 항상 `CREATE EXTENSION IF NOT EXISTS vector` 실행
   - V029: `question_embedding TEXT` → `question_embedding vector(1024)`
   - V030: H2 감지 로직 제거, 항상 타입 변환 및 HNSW 인덱스 생성

## Out of Scope

- Python 서비스(rag-orchestrator, eval-runner) 테스트 변경
- 프론트엔드 테스트 변경
- Testcontainers를 활용한 신규 벡터 검색 테스트 추가 (별도 change로 추적)
- CI/CD 파이프라인 설정 (Docker-in-Docker 활성화 등은 인프라 팀 소관)

## Success Criteria

- 50개 기존 통합 테스트 + 8개 ArchUnit 규칙이 PostgreSQL(Testcontainers) 위에서 100% 통과
- H2 관련 의존성 및 설정이 코드베이스에서 완전히 제거됨
- V018, V030 Kotlin 마이그레이션에서 H2 감지 분기(early return)가 제거됨
- V016, V029 SQL 마이그레이션에서 pgvector 타입이 직접 사용됨
- `./gradlew :apps:admin-api:test` 단일 명령으로 테스트 실행 가능 (Docker 데몬 필요)
