# Proposal

## Change ID

`chat-history-qa-review`

## 배경

`/ops/chat-history` 상세 패널에서 대화 품질 분석에 필요한 지표가 일부 누락되어 있고,
QA 담당자가 개별 응답에 대한 검수를 인라인으로 등록할 수 없는 상태다.

현재 QA 검수는 `/qa/unresolved` 목록에서만 가능하며,
대화 이력을 보면서 즉시 검수를 등록하는 워크플로우가 없다.

## 변경 범위

### Phase 1 — 상세 패널 메트릭 보강 (프론트엔드 + 백엔드)

현재 `GET /admin/questions/{questionId}/context` 응답에서
`queryRewriteText`, `llmMs`, `postprocessMs`가 이미 DB에 있으나 응답에 포함되지 않고,
`contextPrecision`, `contextRecall`은 `GET /admin/questions` 응답에 누락되어 있다.

**백엔드**
- `QuestionContextSummary` / `QuestionContextResponse`에 `queryRewriteText`, `llmMs`, `postprocessMs` 추가
- `QuestionSummary` / `QuestionResponse`에 `contextPrecision`, `contextRecall` 추가 (ragas_evaluations JOIN)
- `JpaQuestionRepository.findAllWithAnswers()` native query에 두 컬럼 추가

**프론트엔드 (`chat-history/page.tsx`)**
- 상세 패널: 쿼리 재작성 텍스트 섹션 추가
- 상세 패널: Latency 세부 (검색 / LLM / 후처리) 추가
- 상세 패널: Context Precision, Context Recall 카드 추가 (기존 RAGAS 섹션 확장)

### Phase 2 — QA 검수 인라인 등록 (프론트엔드만)

기존 `POST /admin/qa-reviews` API를 그대로 사용.
상세 패널 하단에 인라인 폼을 추가한다.

**폼 구성**
- 검수 상태: `confirmed_issue`(이슈 확인) / `false_alarm`(오탐지) 라디오 선택
- 원인 코드: A01~A10 드롭다운 (`confirmed_issue` 선택 시만 노출)
- 조치 유형: `faq_create` / `document_fix_request` / `reindex_request` / `ops_issue` / `no_action`
- 코멘트: 자유 텍스트
- 저장: `POST /admin/qa-reviews` 호출

**상태 표시**
- 이미 검수된 질문: 패널 상단에 검수 결과 배지 표시
- 검수 완료 후: 폼 → 결과 요약으로 전환
- 검수 여부 확인: `GET /admin/qa-reviews?question_id={id}` (기존 API)

## 영향 범위

| 파일 | 변경 내용 |
|------|----------|
| `modules/chat-runtime/.../domain/RagSearchLog.kt` | `QuestionContextSummary`에 필드 추가 |
| `modules/chat-runtime/.../adapter/.../JpaQuestionRepository.kt` | native query 컬럼 추가 |
| `modules/chat-runtime/.../domain/QuestionSummary.kt` | `contextPrecision`, `contextRecall` 추가 |
| `modules/chat-runtime/.../adapter/.../LoadQuestionPortAdapter.kt` | 매핑 추가 |
| `apps/admin-api/.../web/QuestionController.kt` | DTO 필드 추가 |
| `frontend/src/lib/types.ts` | `Question`, `QuestionContext` 타입 확장 |
| `frontend/src/app/ops/chat-history/page.tsx` | 메트릭 섹션 + QA 폼 추가 |

## 비고

- 백엔드 신규 엔드포인트 없음 (기존 API 확장 + 기존 qa-reviews API 재사용)
- 기존 50개 테스트 영향 없음 (응답 필드 추가는 하위 호환)
- QA 상태 머신 (`pending → confirmed_issue / false_alarm`) 기존 로직 그대로 사용
