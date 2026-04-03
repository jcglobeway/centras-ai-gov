# Tasks

## Phase 1 — 상세 패널 메트릭 보강

### 백엔드

- [x] `QuestionSummary`에 `contextPrecision`, `contextRecall` 필드 추가
- [x] `JpaQuestionRepository.findAllWithAnswers()` native query에 `re.context_precision`, `re.context_recall` 추가
- [x] `QuestionWithAnswerRow` 프로젝션에 getter 추가
- [x] `LoadQuestionPortAdapter.listQuestionsWithAnswers()` 매핑 추가
- [x] `QuestionResponse` DTO에 두 필드 추가 + `toResponse()` 매핑
- [x] `QuestionContextSummary`에 `queryRewriteText`, `llmMs`, `postprocessMs` 추가
- [x] `LoadRagSearchLogPortAdapter.getQuestionContext()` 매핑 추가
- [x] `QuestionContextResponse` DTO 필드 추가
- [x] 컴파일 + 테스트 통과 확인

### 프론트엔드

- [x] `types.ts` — `Question`에 `contextPrecision`, `contextRecall` 추가
- [x] `types.ts` — `QuestionContext`에 `queryRewriteText`, `llmMs`, `postprocessMs` 추가
- [x] 상세 패널: 쿼리 재작성 섹션 추가 (`queryRewriteText` 있을 때만)
- [x] 상세 패널: RAGAS 카드에 `contextPrecision`, `contextRecall` 추가
- [x] 상세 패널: Latency 세부 행 추가 (검색 / LLM / 후처리)

## Phase 2 — QA 검수 인라인 등록

### 프론트엔드

- [x] `types.ts` — `RootCauseCode` 타입을 A01~A10으로 수정 (현재 영어 코드와 불일치)
- [x] 상세 패널: `GET /admin/qa-reviews?question_id={id}` SWR 훅 추가 (기존 검수 여부 확인)
- [x] 상세 패널: 기존 검수 결과 배지 표시 (검수된 경우)
- [x] 상세 패널: QA 검수 폼 컴포넌트 구현
  - 상태 라디오 (이슈 확인 / 오탐지)
  - 원인 코드 드롭다운 (A01~A10, confirmed_issue 시만)
  - 조치 유형 드롭다운
  - 코멘트 텍스트에어리어
  - 저장 / 취소 버튼
- [x] `POST /admin/qa-reviews` 호출 + 성공 시 결과 표시로 전환
- [x] 저장 성공 시 `mutate()` 호출해 QA 목록 캐시 무효화
