# Proposal: 대량 질의 투입 + RAG 검색 품질 개선

## 배경

운영 대시보드에 유의미한 통계를 쌓기 위해 국립아시아문화전당(org_acc) 실제 데이터를
API에 대량 투입하는 파이프라인이 필요했다.

동시에 기존 RAG 검색이 vector search 단독으로만 구성되어 있어
신뢰도 점수가 항상 ~3%로 계산되는 버그와 키워드 검색 미지원 문제가 있었다.

## 범위

### 1. RAG 검색 품질 개선 (`python/rag-orchestrator`)
- BM25 키워드 검색 + kiwipiepy 한국어 형태소 분석 추가
- RRF(Reciprocal Rank Fusion)로 vector + BM25 결과 합산
- FlashRank cross-encoder 리랭킹 (`ms-marco-MiniLM-L-12-v2`)
- RRF → confidence 정규화 버그 수정

### 2. 대량 질의 투입 파이프라인 (`python/eval-runner`)
- `citizen_query_gen`: TS 원천데이터 고객 발화 → Ollama 변환 → 독립 민원 질문
- `bulk_query_runner`: 멀티턴 세션 지원, 날짜 분산, 채널 구분

## 제외 범위

- 프론트엔드 변경 없음
- admin-api / DB 스키마 변경 없음
- 기존 `query_runner.py` / `eval_runner.py` 변경 없음
