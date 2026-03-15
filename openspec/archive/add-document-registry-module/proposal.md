# Proposal

## Change ID

`add-document-registry-module`

## Summary

### 변경 목적
- Document Registry 모듈 구현 (documents, document_versions)
- Ingestion과 연동 (crawl → documents)
- 문서 메타데이터 관리 및 버전 추적

### 변경 범위
- Flyway migration: V013 documents, V014 document_versions
- document-registry JPA 엔티티 2개
- document-registry Repository + 어댑터
- Document API

### 제외 범위
- document_chunks (별도)
- 실제 파일 저장 로직
- 문서 파싱 로직

## Impact

- modules/document-registry: JPA 구현
- apps/admin-api: Document API

## Done Definition

- [ ] Flyway migration 2개
- [ ] JPA 엔티티 2개
- [ ] Repository 어댑터
- [ ] API 추가
- [ ] 테스트
- [ ] ./gradlew test 통과
