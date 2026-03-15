# Tasks

## 계획 단계
- [x] DevelopmentIngestionStore 구조 확인
- [x] crawl_sources, ingestion_jobs 스키마 확인 (mvp_docs/04_data_api.md)
- [x] 기존 seed 데이터 파악 (2 sources, 2 jobs)

## Flyway Migration
- [x] V006__create_crawl_sources.sql 작성
  - 테이블 생성 (organization_id FK, service_id FK)
  - 인덱스 추가
  - seed 데이터 (crawl_src_001, crawl_src_002)
- [x] V007__create_ingestion_jobs.sql 작성
  - 테이블 생성 (crawl_source_id FK)
  - 인덱스 추가
  - seed 데이터 (ing_job_101, ing_job_202)

## ingestion-ops 환경 설정
- [x] modules/ingestion-ops/build.gradle.kts에 JPA 의존성 추가
  - kotlin-spring, kotlin-jpa plugin
  - jakarta.persistence-api
  - spring-data-jpa
  - spring-context, spring-tx

## ingestion-ops JPA 엔티티
- [x] CrawlSourceEntity.kt
  - @Entity, @Table("crawl_sources")
  - 전체 필드 매핑
  - toSummary() + enum 변환 함수
- [x] IngestionJobEntity.kt
  - @Entity, @Table("ingestion_jobs")
  - 전체 필드 매핑
  - toSummary(), toEntity() + enum 변환 함수

## ingestion-ops JPA Repository
- [x] JpaCrawlSourceRepository.kt (Spring Data JPA interface)
- [x] JpaIngestionJobRepository.kt (Spring Data JPA interface)
- [x] CrawlSourceReaderAdapter.kt (포트 구현, scope 필터링)
- [x] CrawlSourceWriterAdapter.kt (포트 구현, @Transactional)
- [x] IngestionJobReaderAdapter.kt (포트 구현)
- [x] IngestionJobWriterAdapter.kt (포트 구현, 상태 머신 통합)
- [x] 모든 어댑터 `open class`로 설정

## admin-api 통합
- [x] AdminApiApplication @EnableJpaRepositories에 ingestion-ops 추가
- [x] AdminApiApplication @EntityScan에 ingestion-ops 추가
- [x] RepositoryConfiguration에 ingestion-ops Bean 4개 등록
- [x] DevelopmentIngestionStore 제거
- [x] 테스트 수정 (동적 job ID 사용)

## 검증
- [x] ./gradlew compileKotlin 성공
- [x] ./gradlew test 전체 통과 (25 tests)
- [x] Flyway migration 7개 적용 확인 (H2)

## 마무리
- [ ] 99_worklog.md 갱신
- [ ] status.md 완료 상태로 갱신
- [ ] proposal.md Done Definition 업데이트
- [ ] change를 archive로 이동
- [ ] 커밋 (한글 메시지)
