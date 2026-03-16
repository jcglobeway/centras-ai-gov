# Proposal: add-document-writer

## 목적
document-registry 모듈에 쓰기 포트/어댑터 추가 및 DocumentChunk JPA 엔티티 구현

## 배경
ingestion-worker가 청킹·임베딩 완료 후 청크를 저장할 수단이 없었음.
document_chunks 테이블(V016)은 있지만 Kotlin 구현이 없었음.

## 추가 파일
- `DocumentRegistryContracts.kt`: DocumentChunk, DocumentWriter 포트 추가
- `DocumentChunkEntity.kt`: document_chunks 테이블 JPA 엔티티
- `JpaDocumentChunkRepository.kt`: Spring Data JPA 레포지토리
- `DocumentWriterAdapter.kt`: DocumentWriter 구현체
- `RepositoryConfiguration.kt`: documentWriter 빈 등록
