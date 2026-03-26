# Proposal

## Change ID

`question-embedding`

## Summary

- **변경 목적**: 질문 데이터의 분석 품질을 높이기 위해 두 가지를 추가한다.
  1. `failure_reason_code` 강화 — confidence < 0.4 시 A05(재랭킹 실패) 자동 분류
  2. 질문 임베딩 저장 + FAQ 후보 도출 — 질문 텍스트를 pgvector HNSW 인덱스에 저장하여 유사 질문 클러스터링 기반 FAQ 후보 엔드포인트 제공

- **변경 범위**:
  - `failure_reason_code`: rag-orchestrator에서 confidence < 0.4 → A05, zero-result → A04 (기존 유지)
  - `question_embedding`: questions 테이블에 vector(1024) 컬럼 추가, RAG 질의 임베딩을 저장
  - `GET /admin/faq-candidates`: pgvector `<=>` 연산자로 유사 질문 쌍을 조회해 FAQ 후보 반환

- **제외 범위**: 클러스터링 알고리즘 구현 없음 (유사도 임계값 기반 페어 도출만). 프론트엔드 변경 없음.

## Impact

- **영향 모듈**: `chat-runtime` (domain, port, adapter, service), `admin-api` (config, web adapter, http adapter)
- **영향 Python**: `rag-orchestrator/app.py` (A05 분류, query_embedding 반환)
- **영향 DB**: V029 TEXT 컬럼 추가, V030 PG-only vector(1024) 타입 변환 + HNSW 인덱스
- **영향 API**: `POST /admin/questions` — 내부적으로 임베딩 저장 추가 (요청/응답 형식 변경 없음). `GET /admin/faq-candidates` 신규

## Done Definition

- query-runner 실행 후 `questions.question_embedding`이 벡터 JSON 문자열로 채워진 것 확인
- confidence < 0.4인 경우 `failure_reason_code = 'A05'` 확인
- `GET /admin/faq-candidates?organization_id=org_acc&threshold=0.85` 응답에 유사 질문 쌍 반환 확인
- `JAVA_HOME=.../openjdk-25.0.2/Contents/Home ./gradlew test` 통과 (H2 flyway.target="29")
