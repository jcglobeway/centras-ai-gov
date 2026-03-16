package com.publicplatform.ragops.documentregistry

/**
 * 문서 메타데이터, 버전, 청크 인덱스 상태를 관리하는 바운디드 컨텍스트.
 *
 * 수집된 문서의 메타데이터와 버전 이력을 추적하고, 청크 및 임베딩 벡터를 저장한다.
 * Python ingestion-worker가 청킹·임베딩 완료 후 SaveDocumentPort를 통해 청크를 저장한다.
 *
 * ## 주요 포트 계약
 * - [LoadDocumentPort]: 문서 목록 조회 (기관 스코프 필터)
 * - [LoadDocumentVersionPort]: 문서 버전 이력 조회
 * - [SaveDocumentPort]: 문서 저장, 수집/인덱스 상태 갱신, 청크 저장
 *
 * ## 주요 DB 테이블
 * - `documents`, `document_versions`, `document_chunks`
 *
 * ## 임베딩 벡터 저장
 * - H2(테스트): `TEXT` 타입으로 저장
 * - PostgreSQL(운영): `vector(1024)` 타입 (V018 마이그레이션으로 변환)
 *
 * ## 의존 관계
 * - ingestion-ops: 수집 작업 완료 후 문서 상태 갱신
 * - chat-runtime의 RAG retrieval이 `document_chunks`를 직접 참조
 */
class DocumentRegistryModule
