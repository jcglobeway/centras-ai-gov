# Tasks: upload-flow-stabilization

## Phase 0 — OpenSpec 아티팩트
- [x] proposal.md 작성
- [x] tasks.md 작성
- [x] status.md 작성

## Phase 1 — admin-api 복구 기준 정리
- [x] Flyway `V053` checksum mismatch 복구 절차 문서화
- [x] admin-api 정상 기동 및 `/actuator/health` 확인

## Phase 2 — 전역 필터 확장
- [x] `filter-context`에 `serviceId` 상태 추가 (저장/복원 포함)
- [x] `nav-config` 필터 설정에 `service` 플래그 추가
- [x] `Breadcrumb`에서 `PageFilters`로 `showService` 전달
- [x] `PageFilters`에 서비스 드롭다운 추가 (org 선택 시 로딩/초기화)

## Phase 3 — `/ops/upload` 전역 필터 연동
- [x] 페이지 내부 기관/서비스 선택 카드 제거
- [x] 전역 필터(`useFilter`)의 `orgId`, `serviceId`를 업로드/크롤/리인덱싱 로직에 연결
- [x] gate 조건을 전역 필터 기준으로 유지

## Phase 4 — 검증
- [x] admin-api health `UP` 확인
- [x] `/ops/upload` 파일 업로드 시 `jobId` 생성 확인
- [x] 업로드 후 job 상태 조회 가능 확인
- [x] breadcrumb에서 기관/서비스 선택 동작 확인

## Phase 5 — worker 안정화
- [x] `AdminApiClient` 로그인 요청/응답 포맷을 실제 Admin API 스펙(`email`, `session.token`)과 일치시킴
- [x] 401 응답 시 자동 재로그인 경유로 API 재시도 적용
- [x] Celery `tasks._make_client()`에 `ADMIN_API_USERNAME/PASSWORD` 전달 연결
- [x] worker 인증 동작 회귀 테스트 추가
- [x] `dev.sh --with-worker` 플래그 추가 (기본 미포함, `uv run` 선택 실행)
- [x] `OLLAMA_URL=https` 환경에서 임베딩 TLS 검증 옵션(`OLLAMA_TLS_VERIFY`) 지원

## 검증 메모

- admin-api health: `{"status":"UP"}` 확인
- 광주남구청 기존 컬렉션(`여권실무편람`) 소스로 `jobId` 생성 및 `succeeded/complete` 확인
- `ingestion-worker` 실행 시 `file_drop` invalid URL / 상태전이(FAILED+FETCH) 문제를 수정해 파싱 단계 진입 확인
- `frontend npm run build`는 기존 ESLint 설정 오류(`ops/statistics/page.tsx` 룰 누락)로 실패하며, 이번 변경으로 인한 신규 타입/훅 오류는 해소
