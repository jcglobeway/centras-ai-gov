# Proposal: integrate-rag-search-logs

## 목적
V017로 추가된 rag_search_logs/rag_retrieved_documents 테이블의 Kotlin 구현 완성

## 배경
V017 마이그레이션으로 테이블은 생성되었지만 JPA 엔티티, 포트, 어댑터가 없어
RAG 검색 로그를 기록할 수 없었음.

## 추가 파일
- `ChatRuntimeContracts.kt`: RagSearchLogWriter 포트 추가
- `RagSearchLogEntity.kt`: rag_search_logs 테이블 JPA 엔티티
- `RagRetrievedDocumentEntity.kt`: rag_retrieved_documents 테이블 JPA 엔티티
- `JpaRagSearchLogRepository.kt`: Spring Data JPA 레포지토리
- `RagSearchLogWriterAdapter.kt`: RagSearchLogWriter 구현체
- `RepositoryConfiguration.kt`: ragSearchLogWriter 빈 등록
