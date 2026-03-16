# Proposal: implement-vector-search

## 목적
retrieval.py stub을 실제 pgvector 검색으로 교체하고 검색 로그를 Admin API로 콜백

## 변경
- retrieval.py: psycopg2로 실제 vector_search 구현
- app.py: 검색 결과를 Admin API rag-search-log 엔드포인트로 전송
- pyproject.toml: psycopg2-binary 의존성 추가

## 환경변수
- `DATABASE_URL`: PostgreSQL 연결 문자열
- `OLLAMA_URL`: Ollama 서버 URL (기본: http://localhost:11434)
- `ADMIN_API_BASE_URL`: Admin API URL (기본: http://localhost:8081)
