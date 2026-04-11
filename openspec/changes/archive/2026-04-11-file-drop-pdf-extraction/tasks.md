# Tasks: file-drop-pdf-extraction

## Phase 0 — OpenSpec 정렬
- [x] proposal.md 작성
- [x] tasks.md 작성
- [x] status.md 작성
- [x] 의존성 추가 승인 (`pdfplumber`, `PyPDF2`)

## Phase 1 — 구현
- [x] `job_runner.py`에서 `file_drop` 파일 타입 분기 추가
- [x] PDF 추출기 추가 (`pdfplumber` 우선, `PyPDF2` 폴백)
- [x] PDF 텍스트 후처리(공백/줄바꿈 정리) 추가
- [x] 추출 실패 시 명시적 오류 코드/로그 처리

## Phase 2 — 테스트
- [x] PDF `file_drop` 단위 테스트 추가
- [x] 비-PDF 파일 회귀 동작 확인
- [x] 기존 worker 인증/큐 처리 smoke test

## Phase 3 — E2E 검증
- [x] `/ops/upload` 업로드 소스(PDF) 기준 job 실행 확인
- [x] INDEX 저장 청크 가독성 확인
- [x] 검색 결과에서 해당 문서 문장 검색 확인

## Phase 4 — 문서화
- [x] worker 파일 처리 전략 문서 반영
- [x] 레거시 대비 차이점/제약 기록
