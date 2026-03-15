# Proposal

## Change ID

`add-jpa-entities-ingestion-ops`

## Summary

### 변경 목적
- ingestion-ops 모듈을 PostgreSQL + JPA 기반으로 전환
- crawl_sources, ingestion_jobs 데이터 영속화
- Python worker가 실제 DB에서 source/job을 읽고 실행 가능하도록 구조 완성

### 변경 범위
- **Flyway migration**:
  - V006__create_crawl_sources.sql (seed 데이터 포함)
  - V007__create_ingestion_jobs.sql (seed 데이터 포함)

- **ingestion-ops JPA 엔티티**:
  - CrawlSourceEntity
  - IngestionJobEntity
  - toModel(), toEntity() 확장 함수

- **ingestion-ops JPA Repository**:
  - JpaCrawlSourceRepository (Spring Data JPA)
  - JpaIngestionJobRepository (Spring Data JPA)
  - CrawlSourceReaderAdapter, CrawlSourceWriterAdapter
  - IngestionJobReaderAdapter, IngestionJobWriterAdapter

- **Spring 설정**:
  - modules/ingestion-ops build.gradle.kts에 JPA 의존성 추가
  - AdminApiApplication @EnableJpaRepositories에 ingestion-ops 추가
  - RepositoryConfiguration에 ingestion-ops 어댑터 등록
  - admin-api DevelopmentIngestionStore 제거

### 제외 범위
- documents, document_versions 테이블 (별도 change)
- document-registry 모듈 JPA 연동
- ingestion 스케줄러 구현

## Impact

### 영향 모듈
- `modules/ingestion-ops`: JPA 엔티티, Repository, 어댑터 추가
- `apps/admin-api`: DevelopmentIngestionStore 제거, Bean 등록

### 영향 API
- 기존 API 계약 변경 없음 (내부 구현만 변경)
- 데이터가 영속화되어 재시작 후에도 유지됨

### 영향 테스트
- 기존 테스트 코드 변경 없음
- Flyway migration이 H2에서 자동 실행됨

## Done Definition

- [x] Flyway migration 2개 작성 (crawl_sources, ingestion_jobs + seed)
- [x] ingestion-ops JPA 엔티티 2개 작성 (toSummary, toEntity 포함)
- [x] JPA Repository 인터페이스 2개 작성
- [x] Repository 어댑터 4개 구현 (Reader/Writer, scope 필터링)
- [x] modules/ingestion-ops build.gradle.kts에 JPA 의존성 추가
- [x] AdminApiApplication에 ingestion-ops 패키지 추가
- [x] RepositoryConfiguration에 ingestion-ops Bean 4개 등록
- [x] admin-api DevelopmentIngestionStore 제거
- [x] 테스트 수정 (동적 job ID 사용)
- [x] ./gradlew test 전체 통과 (25 tests)
- [x] Flyway migration 7개 적용 확인 (H2)
