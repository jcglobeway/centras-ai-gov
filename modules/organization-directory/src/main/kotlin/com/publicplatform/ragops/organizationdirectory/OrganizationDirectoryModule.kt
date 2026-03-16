package com.publicplatform.ragops.organizationdirectory

/**
 * 다중 테넌트 기관·서비스 레지스트리를 담당하는 바운디드 컨텍스트.
 *
 * 공공기관(Organization)과 해당 기관이 운영하는 서비스(Service)를 등록·조회한다.
 * 모든 비즈니스 테이블의 `organization_id` 격리 기준이 되는 최상위 스코핑 키를 제공한다.
 *
 * ## 주요 포트 계약
 * - [OrganizationRepository]: 기관 CRUD
 * - [ServiceRepository]: 서비스 CRUD
 * - [LoadOrganizationPort]: 기관 목록 조회 (세션 스코프 검증용)
 *
 * ## 주요 DB 테이블
 * - `organizations`, `services`
 *
 * ## 의존 관계
 * - identity-access 모듈에 의존하지 않음
 * - ingestion-ops, chat-runtime, document-registry, qa-review, metrics-reporting이
 *   `organization_id` 유효성 검증 시 이 모듈의 데이터를 참조
 */
class OrganizationDirectoryModule
