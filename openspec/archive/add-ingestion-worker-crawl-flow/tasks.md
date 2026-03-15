# Tasks

## 계획 단계
- [x] 요구 범위 재확인
- [x] admin-api ingestion API 계약 확인 (IngestionCommandController)
- [x] job status 전이 규칙 확인
- [x] 현재 python/ingestion-worker 구조 확인

## 환경 설정
- [x] pyproject.toml에 playwright 의존성 추가
- [x] pyproject.toml에 pytest 의존성 추가 (dev)
- [ ] playwright install 실행 (chromium 설치) - 로컬 테스트 시 필요

## Admin API 개별 조회 추가
- [x] GET /admin/crawl-sources/{id} 추가
- [x] GET /admin/ingestion-jobs/{id} 추가

## 모델 정의
- [x] models.py 생성
  - CrawlSource 모델
  - IngestionJob 모델
  - JobStatus, JobStage enum

## Admin API Client
- [x] admin_api_client.py 생성
  - AdminApiClient 클래스 (httpx 기반)
  - get_ingestion_job(job_id) 메서드
  - get_crawl_source(source_id) 메서드
  - transition_job_status(job_id, status, stage) 메서드
  - 인증 헤더 처리 (X-Admin-Session-Id)

## Crawl Executor
- [x] crawl_executor.py 생성
  - CrawlExecutor 클래스
  - execute_crawl(source_uri) 메서드 스텁
  - Playwright browser context 생성
  - URL fetch 및 스크린샷 저장 (임시)
  - 에러 핸들링 (timeout, network error)

## Job Runner
- [x] job_runner.py 생성
  - IngestionJobRunner 클래스
  - run_job(job_id) 메서드
  - job lifecycle 관리 (queued → running → succeeded/failed)
  - 각 stage별 callback

## CLI 통합
- [x] app.py 수정
  - run 커맨드에 --job-id 옵션 추가
  - 환경 변수 읽기 (ADMIN_API_BASE_URL, ADMIN_API_SESSION_TOKEN)
  - IngestionJobRunner 호출

## 테스트
- [ ] 로컬 admin-api 실행 (./gradlew :apps:admin-api:bootRun)
- [ ] crawl source 생성 (POST /admin/crawl-sources)
- [ ] job 생성 (POST /admin/crawl-sources/{id}/run)
- [ ] CLI 실행 (ingestion-worker run --job-id <id>)
- [ ] job 상태 전이 확인 (GET /admin/ingestion-jobs)

## 마무리
- [ ] 99_worklog.md 갱신
- [ ] status.md 완료 상태로 갱신
- [ ] proposal.md Done Definition 업데이트
- [ ] change를 archive로 이동
- [ ] 커밋 (한글 메시지)
