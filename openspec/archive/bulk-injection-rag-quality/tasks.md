# Tasks

## RAG 검색 품질 개선

- [x] `retrieval.py`: kiwipiepy 형태소 분석기 추가 (ImportError fallback 포함)
- [x] `retrieval.py`: BM25 corpus 캐시 (`_bm25_cache`) 구현
- [x] `retrieval.py`: `bm25_search()` 구현
- [x] `retrieval.py`: `rrf_fusion()` 구현
- [x] `retrieval.py`: FlashRank `rerank()` 싱글톤 구현
- [x] `retrieval.py`: `hybrid_search()` 메인 진입점 구현
- [x] `retrieval.py`: RRF → confidence 정규화 버그 수정 (`/ (2/61)`)
- [x] `pyproject.toml` (rag-orchestrator): `kiwipiepy>=0.18.0` 추가
- [x] `.env` (rag-orchestrator): `RERANKER_ENABLED=true` 설정
- [x] `app.py`: answer_text invalid JSON escape sanitize 추가

## 시민 질의 생성기

- [x] `citizen_query_gen.py` 신규 작성
- [x] `pyproject.toml` (eval-runner): `citizen-query-gen` 스크립트 등록
- [x] 발화 추출: `consulting_content`에서 `고객:` 파싱
- [x] Ollama 재작성 프롬프트 설계 및 구현
- [x] 출력 sanitize: invalid backslash escape 제거 (`re.sub`)
- [x] `citizen_questions.json` 200건 생성 완료

## 대량 질의 투입기

- [x] `bulk_query_runner.py` 신규 작성
- [x] `pyproject.toml` (eval-runner): `bulk-query-runner` 스크립트 등록
- [x] `group_by_source()`: source_id 기준 멀티턴 그룹핑
- [x] `BulkClient.create_session()`: POST /admin/simulator/sessions
- [x] `BulkClient.create_question()`: POST /admin/questions (timeout=120s)
- [x] `_auto_login()`: ADMIN_API_SESSION_TOKEN 없을 때 자동 로그인
- [x] `_backdate_questions()`: questions/answers/rag_search_logs 날짜 분산
- [x] `--input-json` 옵션: citizen_questions.json 직접 로드
- [x] `--channel` 옵션: simulator / api 구분
- [x] `--skip` / `--limit` 옵션: 페이징 지원
- [x] `--dry-run` 옵션: API 호출 없이 구조 미리보기

## 실행 결과

- [x] 시뮬레이터 배치: 200건 / 78세션 / 실패 0건 (channel=simulator)
- [x] API 배치: 200건 / 73세션 / 실패 0건 (channel=api)
- [x] 날짜 분산 완료: 2026-03-03 ~ 2026-04-01 (30일 균등)
- [x] 평균 신뢰도 확인: 54.5% (개선 전 ~3%)