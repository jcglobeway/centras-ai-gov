# Ingestion Worker

역할:

- crawl source 실행
- 문서 정규화
- parsing, chunking, embedding
- index 적재
- Product API 또는 DB에 job 상태 콜백

## 안정 실행 가이드

worker는 매번 수동 재기동 대상이 아니다. 장기 실행 기준으로 아래 인증 설정을 사용한다.

- `ADMIN_API_BASE_URL`: Admin API 주소 (기본 `http://localhost:8081`)
- `ADMIN_API_USERNAME`: Admin 로그인 이메일
- `ADMIN_API_PASSWORD`: Admin 로그인 비밀번호
- `ADMIN_API_SESSION_TOKEN`: 선택값. 있으면 먼저 사용하고, 401 발생 시 username/password로 자동 재로그인
- `OLLAMA_URL`: 임베딩/LLM 서버 주소 (예: `https://...:11434`)
- `OLLAMA_TLS_VERIFY`: HTTPS 인증서 검증 여부 (`true`/`false`, 기본 `false`)

실행 예시:

```bash
cd python/ingestion-worker
uv run python -m ingestion_worker.app worker --concurrency 1 --loglevel info
```

## file_drop 파일 처리 전략

- 경로 처리:
  - `source_uri`가 일반 경로(`/tmp/a.pdf`) 또는 `file://` URI 둘 다 지원
- 파일별 추출:
  - `.pdf`: `pdfplumber` 우선, 실패 시 `PyPDF2` 폴백
  - 그 외: UTF-8 텍스트 디코딩
- PDF 후처리:
  - 공백/줄바꿈 정규화로 청크 가독성 개선
- 실패 처리:
  - 텍스트를 추출하지 못하면 `file_drop` 로딩 실패로 처리되어 job이 실패 전이됨
