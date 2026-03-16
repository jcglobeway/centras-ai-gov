package com.publicplatform.ragops.ingestionops

/**
 * 문서 수집 파이프라인의 실행 정책과 작업 생명주기를 관리하는 바운디드 컨텍스트.
 *
 * 크롤 소스(CrawlSource) 등록과 수집 작업(IngestionJob) 상태 전이를 처리한다.
 * Python ingestion-worker가 작업 상태를 Admin API를 통해 콜백으로 갱신한다.
 *
 * ## 주요 포트 계약
 * - [LoadCrawlSourcePort]: 크롤 소스 조회 (목록, 단건)
 * - [SaveCrawlSourcePort]: 크롤 소스 생성
 * - [LoadIngestionJobPort]: 수집 작업 조회
 * - [PersistIngestionJobPort]: 작업 생성, 상태 전이
 *
 * ## 수집 작업 상태 머신
 * ```
 * pending → queued → running → succeeded
 *                            → failed
 *                            → cancelled
 * ```
 * 트리거 유형: `schedule`, `manual`, `qa_request`, `document_event`
 * 실행 유형: `python_worker`, `openrag_flow`, `spring_batch`
 *
 * ## 주요 DB 테이블
 * - `crawl_sources`, `ingestion_jobs`
 *
 * ## 의존 관계
 * - organization-directory: 기관/서비스 스코프 검증
 */
class IngestionOpsModule
