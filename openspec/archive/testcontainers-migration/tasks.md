# Tasks: testcontainers-migration

## Phase 1: 핵심 전환 (필수)

### 1-1. 의존성 교체
- [ ] `apps/admin-api/build.gradle.kts`에서 `testImplementation("com.h2database:h2")` 제거
- [ ] `testImplementation("org.springframework.boot:spring-boot-testcontainers")` 추가
- [ ] `testImplementation("org.testcontainers:postgresql")` 추가

### 1-2. BaseApiTest 싱글턴 컨테이너 구성
- [ ] `@Testcontainers` 어노테이션 추가
- [ ] `companion object`에 `static @Container PostgreSQLContainer("pgvector/pgvector:pg16")` 선언
  - `withReuse(false)` 기본값 유지 (테스트 격리 보장)
- [ ] `@ServiceConnection` 적용 — datasource 자동 주입 확인

### 1-3. application-test.yml H2 설정 제거
- [ ] `spring.datasource.url` (H2 JDBC URL) 제거
- [ ] `spring.datasource.driver-class-name: org.h2.Driver` 제거
- [ ] `spring.datasource.username`, `spring.datasource.password` 제거 (`@ServiceConnection`이 대체)
- [ ] `spring.jpa.properties.hibernate.dialect: org.hibernate.dialect.H2Dialect` 제거
- [ ] `spring.jpa.hibernate.ddl-auto: update` → `validate` 또는 `none`으로 변경 (Flyway가 스키마 관리)

### 1-4. 빌드 및 전체 테스트 통과 확인
- [ ] `JAVA_HOME=.../openjdk-25.0.2/Contents/Home ./gradlew :apps:admin-api:test` 실행
- [ ] 50개 통합 테스트 전부 PASS 확인
- [ ] 8개 ArchUnit 규칙 전부 PASS 확인
- [ ] 컨테이너 기동 시간 측정 (허용 기준: 60초 이내)

## Phase 2: H2 우회 코드 정리 (선택적)

Phase 1 완료 및 테스트 통과 후 진행.

### 2-1. V016 마이그레이션 정리
- [ ] `apps/admin-api/src/main/resources/db/migration/V016__create_document_chunks.sql`
  - `embedding_vector TEXT` → `embedding_vector vector(1024)` 변경
  - H2 호환 주석 제거

### 2-2. V018 Kotlin 마이그레이션 정리
- [ ] `V018__EnablePgVector.kt`에서 H2 감지 분기 제거
  ```kotlin
  // 제거 대상
  val dbName = context.connection.metaData.databaseProductName
  if (dbName.contains("H2", ignoreCase = true)) { return }
  ```
- [ ] `CREATE EXTENSION IF NOT EXISTS vector`는 유지 (idempotent)
- [ ] V016이 이미 `vector(1024)`를 사용하므로 `ALTER TABLE ... TYPE vector` 구문 제거 (중복)

### 2-3. V029 마이그레이션 정리
- [ ] `apps/admin-api/src/main/resources/db/migration/V029__add_question_embedding.sql`
  - `question_embedding TEXT` → `question_embedding vector(1024)` 변경
  - H2 호환 주석 제거

### 2-4. V030 Kotlin 마이그레이션 정리
- [ ] `V030__QuestionEmbeddingVector.kt`에서 H2 감지 분기 제거
- [ ] V029가 이미 `vector(1024)`를 사용하므로 `ALTER TABLE ... TYPE vector` 구문 제거 (중복)
- [ ] HNSW 인덱스 생성 구문만 유지 (V029 SQL에서는 인덱스 생성 불가)

### 2-5. Phase 2 완료 후 테스트 재확인
- [ ] `./gradlew :apps:admin-api:test` 재실행, 전체 PASS 확인
- [ ] `document_chunks.embedding_vector` 컬럼 타입이 `vector(1024)`임을 마이그레이션 로그로 확인
- [ ] `questions.question_embedding` 컬럼 타입이 `vector(1024)`임을 마이그레이션 로그로 확인

## 검증

- [ ] `com.h2database:h2` 의존성이 빌드 파일 어디에도 존재하지 않음 확인 (`grep -r "h2database"`)
- [ ] `H2Dialect` 또는 `H2` 문자열이 설정 파일에 없음 확인
- [ ] Kotlin 마이그레이션 파일에 H2 감지 분기가 없음 확인
