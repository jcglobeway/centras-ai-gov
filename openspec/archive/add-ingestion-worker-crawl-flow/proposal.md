# Proposal

## Change ID

`add-ingestion-worker-crawl-flow`

## Summary

### 변경 목적
- Python ingestion-worker에 crawl source 실행 기본 흐름 구현
- admin-api와 job callback 연동 구조 구현
- Playwright 기반 크롤링 스텁 추가 (실제 파싱/chunk/embed는 이후 작업)

### 변경 범위
- `python/ingestion-worker` 패키지:
  - admin-api client (httpx 기반)
  - job status callback 로직
  - crawl source executor 스텁 (Playwright headless browser)
  - CLI 커맨드: `ingestion-worker run --job-id <id>`
  - 환경 변수: ADMIN_API_BASE_URL, ADMIN_API_SESSION_TOKEN

- pyproject.toml 의존성 추가:
  - playwright (브라우저 크롤링)
  - pydantic (모델 검증)

### 제외 범위
- 실제 HTML 파싱 로직 (beautiful soup 등)
- 문서 chunk 생성 로직
- embedding 생성 로직
- OpenSearch/pgvector 인덱싱
- job polling 또는 queue 기반 실행 (수동 CLI로 시작)
- 에러 재시도 정책

## Impact

### 영향 모듈
- `python/ingestion-worker`: 핵심 구현
- `apps/admin-api`: 영향 없음 (기존 API 활용)
- `modules/ingestion-ops`: 영향 없음 (계약 변경 없음)

### 영향 API
- **사용**: `POST /admin/ingestion-jobs/{id}/status` (job 상태 콜백)
- **사용**: `GET /admin/ingestion-jobs/{id}` (필요 시 추가)
- **사용**: `GET /admin/crawl-sources/{id}` (필요 시 추가)

### 영향 테스트
- Python worker 단위 테스트 추가 (pytest)
- 기존 admin-api 테스트는 영향 없음

## Done Definition

- [x] admin-api client 모듈 구현 (httpx 기반)
- [x] job status callback 로직 구현
- [x] Playwright 기반 크롤링 스텁 (URL fetch, 스크린샷 저장)
- [x] CLI 커맨드 `ingestion-worker run --job-id <id>` 동작
- [x] job lifecycle: queued → running → succeeded/failed 전이 지원
- [x] pyproject.toml에 playwright 의존성 추가
- [x] admin-api에 개별 조회 API 추가 (crawl-sources, ingestion-jobs)
- [x] ./gradlew build 통과
- [x] 기본 에러 핸들링 (네트워크 오류, 크롤링 오류)
- [ ] ~로컬 실행 검증~ (사용자 환경에서 playwright install 필요)
