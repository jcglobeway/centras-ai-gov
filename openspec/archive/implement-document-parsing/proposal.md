# Proposal: implement-document-parsing

## 목적
CrawlExecutor stub을 완전한 파싱/청킹/임베딩/저장 파이프라인으로 구현

## 변경
- crawl_executor.py: HTML 추출(BeautifulSoup4) + 청킹(langchain-text-splitters) + 임베딩(Ollama bge-m3)
- job_runner.py: CHUNK, EMBED, INDEX 스테이지 추가
- pyproject.toml: beautifulsoup4, langchain-text-splitters 추가

## 파이프라인 흐름
```
fetch (Playwright) → extract (BS4) → chunk (RecursiveCharacterTextSplitter)
→ embed (Ollama bge-m3) → index (Admin API DocumentWriter)
→ complete
```

## 환경변수
- `OLLAMA_URL`: Ollama 서버 URL
- `ADMIN_API_BASE_URL`: Admin API URL
- `ADMIN_API_SESSION_TOKEN`: 인증 토큰
