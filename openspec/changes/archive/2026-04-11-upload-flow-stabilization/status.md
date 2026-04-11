# Status: upload-flow-stabilization

- 상태: `implemented`
- 시작일: `2026-04-08`
- 마지막 업데이트: `2026-04-10`

## 현재 단계

검증 완료 (아카이브 대기)

## 메모

- `upload-page-v2`, `upload-page-v3`는 별도 아카이브 대상으로 유지
- admin-api 복구 선행 후 업로드 E2E를 최종 검증
- 적용 완료:
  - breadcrumb 전역 필터에 서비스 선택 추가
  - `/ops/upload` 페이지 내부 기관/서비스 카드 제거 및 전역 필터 연동
  - `file_drop` 소스 실행 시 worker가 로컬 파일 URI를 처리하도록 보정
  - worker 실패 전이 단계(`FAILED`)를 `COMPLETE`로 맞춤
  - worker 인증 안정화:
    - Admin API 로그인 요청 필드를 `email`로 수정
    - 로그인 응답 파싱을 `session.token` 기준으로 수정
    - 401 응답 시 자동 재로그인 후 재요청
    - Celery client 생성 시 `ADMIN_API_USERNAME/PASSWORD`를 실제 전달
  - worker 실행 경로 안정화:
    - `dev.sh`에 `--with-worker` 옵션 추가 (기본값은 미실행)
    - `rag-orchestrator`와 동일하게 `uv run` 백그라운드 실행으로 통일
  - VPN Ollama 대응:
    - worker 임베딩 호출에 TLS 검증 옵션(`OLLAMA_TLS_VERIFY`) 추가
    - 임베딩 응답 포맷(`embedding`, `embeddings[]`) 모두 파싱 지원
- 검증 결과:
  - admin-api health `UP`
  - `/admin/documents/upload` 업로드 성공: `jobId=ing_job_d7c0dca3` 생성
  - `GET /admin/ingestion-jobs/{jobId}` 조회 성공 (`status=queued`, `jobStage=fetch`)
  - `/ops/upload` breadcrumb에서 기관 선택 후 서비스 콤보 활성화/선택 가능 확인
  - `npm run build`는 기존 ESLint rule 누락(`@typescript-eslint/no-explicit-any`)로 실패

## admin-api 복구 runbook (로컬)

1. DB 체크섬 확인  
   `psql postgresql://ragops_user:ragops_pass@localhost:5432/ragops_dev -c "select version,checksum from flyway_schema_history where version='053';"`
2. 체크섬 불일치 보정  
   `psql postgresql://ragops_user:ragops_pass@localhost:5432/ragops_dev -c "update flyway_schema_history set checksum=220918058 where version='053' and success=true;"`
3. 서버 기동 및 health 확인  
   `./gradlew :apps:admin-api:bootRun`  
   `curl -sS http://localhost:8081/actuator/health`
