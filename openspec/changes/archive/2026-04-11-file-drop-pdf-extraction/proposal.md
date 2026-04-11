# Proposal: file-drop-pdf-extraction

## Problem

현재 `ingestion-worker`의 `file_drop` 처리(`_load_file_drop_pages`)는 업로드 파일을 `read_bytes() → utf-8(ignore)`로 디코딩한다.  
PDF 바이너리가 그대로 텍스트로 해석되어 깨진 문자열이 청크로 저장되고, 검색 품질이 급격히 저하된다.

또한 파일 유형별 추출 전략이 없어 PDF/HWP/텍스트 파일을 동일 경로로 처리하고 있어, 업로드 기반 인덱싱의 정확도가 낮다.

## Legacy Reference (centras-ai-server-admin)

레거시 프로젝트(`/Users/parkseokje/Documents/GitHub/github-jcg/centras-ai-server-admin`)는 아래 방식으로 PDF 추출 품질을 보완한다.

- `services/document_processor.py`
  - `pdfplumber` 기반 페이지 텍스트 추출
  - 실패 시 `PyPDF2` 폴백
  - `_post_process_pdf_text` 후처리(공백/줄바꿈 정리)
- `docs/EMBEDDING_PROCESS_FLOW.md`
  - PDF 파서 조합 및 보정 흐름 문서화

이 패턴을 현재 `ingestion-worker`의 `file_drop` 경로에 최소 범위로 이식한다.

## Proposed Solution

1. `file_drop` 로더를 파일 확장자 기반 분기 처리로 변경한다.
2. PDF는 전용 추출기에서 처리한다.
   - 1차: `pdfplumber`
   - 2차 폴백: `PyPDF2`
   - 후처리: 공백/줄바꿈/비가시 문자 정리
3. 텍스트 추출 실패 시 현재처럼 무조건 성공 처리하지 않고, job 실패 코드와 로그를 명확히 남긴다.
4. 업로드 인덱싱 E2E를 1개 PDF 기준으로 재검증한다. (job 성공 + 저장 청크 가독성 + 검색 응답 확인)

## Out of Scope

- OCR 파이프라인 추가(EasyOCR/Tesseract 등)
- HWP 전용 파서 신규 도입
- 청킹 알고리즘 교체

## Impact

- 영향 모듈: `python/ingestion-worker`
- 영향 API: 없음(내부 worker 처리 로직 변경)
- 영향 테스트:
  - `file_drop` PDF 추출 단위 테스트 신규
  - 기존 worker auth/celery 경로 회귀 확인

## Approval Checkpoint

아래 의존성 추가가 필요하다. 구현 전 승인 필요:

- `pdfplumber`
- `PyPDF2`

## Success Criteria

- `/ops/upload`에서 업로드한 PDF가 바이너리 깨짐 없이 텍스트 청크로 저장된다.
- 동일 PDF 재인덱싱 시 job이 `succeeded/complete`로 종료된다.
- 검색 결과의 본문 미리보기가 사람이 읽을 수 있는 문장으로 확인된다.
