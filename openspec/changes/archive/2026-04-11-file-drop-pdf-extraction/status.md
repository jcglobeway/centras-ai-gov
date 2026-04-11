# Status: file-drop-pdf-extraction

- 상태: `implemented`
- 시작일: `2026-04-09`
- 마지막 업데이트: `2026-04-09`

## Progress

- OpenSpec change 초안/작업목록 작성 완료
- 레거시(`centras-ai-server-admin`) PDF 추출 방식 비교 완료
- 구현 완료:
  - `file_drop` 경로의 `file://` URI 지원
  - PDF 전용 파서(`pdfplumber` 우선, `PyPDF2` 폴백) 적용
  - PDF 텍스트 후처리 적용
  - 텍스트 추출 실패 시 `file_drop` 로딩 실패 처리
- 테스트/문서 반영 완료:
  - `test_job_runner_file_drop.py` 추가
  - worker README에 파일 처리 전략 반영
- 실검증 완료:
  - 소스 `crawl_src_53609443` 재실행으로 job `ing_job_e1ff4011` 생성/완료
  - 새 문서 `doc_1ec0039e` 생성, 청크/임베딩 저장 확인
  - rag-orchestrator `/generate` 응답에서 여권 편람 문장 기반 답변 확인
- 데이터 정리(운영 정합성):
  - `crawl_src_53609443` 기존 문서 6건 및 관련 청크 284건 삭제
  - 동일 소스 1회 재인덱싱(job `ing_job_aee9f993`)으로 단일 문서 상태 복원
  - 최종 문서: `doc_d755f35b` (청크 32, 임베딩 32)

## Verification

- `uv lock` 실행으로 의존성 반영 확인
- `uv run pytest -q` 실행 결과: `6 passed`
  - `tests/test_admin_api_client.py`
  - `tests/test_job_runner_file_drop.py`
- E2E 실행:
  - `POST /admin/crawl-sources/crawl_src_53609443/run` → `202` (`jobId=ing_job_e1ff4011`)
  - `uv run python -m ingestion_worker.app run --job-id ing_job_e1ff4011 ...`
  - 결과: `succeeded/complete`, `document_id=doc_1ec0039e`, `32/32` 청크 임베딩 저장
- 정리 후 재검증:
  - 기존 인덱스 삭제 후 `POST /admin/crawl-sources/crawl_src_53609443/run` → `202` (`jobId=ing_job_aee9f993`)
  - `uv run python -m ingestion_worker.app run --job-id ing_job_aee9f993 ...`
  - 결과: `succeeded/complete`, `document_id=doc_d755f35b`, `32/32` 청크 임베딩 저장
  - `vector_search` Top-5가 모두 정상 한글 본문 청크로 반환됨
- 청크 본문 샘플 확인:
  - 예: `18세 미만 ... 법정대리인 동의서 ...` 형태의 정상 한글 텍스트
- 검색 확인:
  - `POST http://localhost:8090/generate` 질의 응답 성공
  - `citation_count=5`, 답변 본문에 법정대리인 동의서/구비서류 문맥 포함
  - `vector_search` Top 결과 `chunk_5f18ab6b`가 `doc_1ec0039e`에 매핑됨

## Risks

- 스캔본 PDF는 OCR 미포함 범위이므로 텍스트 추출 실패 가능
- `rag_search_logs`는 `question_id`가 `questions` 테이블 FK를 만족하지 않으면 기록되지 않음
  - 이번 실검증 질의(`q_e2e_passport_001`)는 로그 미적재
