# Tasks

## Phase 0 — OpenSpec 아티팩트

- [x] openspec/changes/activate-upload-page/proposal.md 작성
- [x] openspec/changes/activate-upload-page/tasks.md 작성
- [x] openspec/changes/activate-upload-page/status.md 작성

## Phase 1 — 버그 수정 (Frontend)

- [x] `upload/page.tsx` 로컬 IngestionJob 인터페이스 → `types.ts` import 교체

## Phase 2 — 웹 크롤링 등록 활성화 (Frontend)

- [x] `api.ts`에 `ingestionApi.createSource()` 추가
- [x] `upload/page.tsx` 웹 크롤링 폼: 기관/서비스/소스타입/렌더모드/수집모드 드롭다운 추가
- [x] 등록 버튼 → `POST /admin/crawl-sources` 연동

## Phase 3 — 수동 재인덱싱 활성화 (Frontend)

- [x] `api.ts`에 `ingestionApi.runSource()` 추가
- [x] `upload/page.tsx` 크롤 소스 드롭다운 + 즉시 실행 버튼 연동
- [x] `POST /admin/crawl-sources/{id}/run` 연동

## Phase 4 — 파일 업로드 (Backend + Frontend)

- [x] `RegisterDocumentUseCase.kt` 생성 (port/in)
- [x] `SaveDocumentRecordPort.kt` 생성 (port/out)
- [x] `RegisterDocumentCommand` domain에 추가
- [x] `RegisterDocumentService.kt` 생성 (application/service)
- [x] `SaveDocumentRecordPortAdapter.kt` 생성 (adapter/outbound/persistence)
- [x] `DocumentUploadController.kt` 생성
- [x] `ServiceConfiguration.kt`에 `registerDocumentService` Bean 추가
- [x] `RepositoryConfiguration.kt`에 `saveDocumentRecordPort` Bean 추가
- [x] `api.ts`에 `documentApi.upload()` 추가
- [x] `upload/page.tsx` 파일 업로드 영역 활성화 (드래그앤드롭 + 클릭)

## 검증

- [x] `./gradlew test` 통과 (50 integration tests + 8 ArchUnit — BUILD SUCCESSFUL)
- [x] 브라우저에서 파일 업로드 → DB 레코드 확인
- [x] 브라우저에서 크롤 소스 등록 → DB 레코드 확인
- [x] 브라우저에서 즉시 실행 → QUEUED 잡 생성 확인
