# Proposal

## Change ID

`standardize-failure-codes`

## Summary

- **변경 목적**: RAG 파이프라인 실패 원인 코드 A01~A10을 표준화하여 도메인 enum + API 검증에 적용
- **변경 범위**:
  - 도메인: `FailureReasonCode` enum 신규 추가 (A01~A10)
  - API 검증: `questions.failure_reason_code` 입력 값 검증
  - API 검증: `qa_reviews.root_cause_code` 입력 값 검증 (기존 자유 문자열 → 표준 코드)
  - 문서: 코드 taxonomy 정의
- **제외 범위**: 자동 분류 ML, 코드 통계 집계 대시보드

## 실패 원인 코드 Taxonomy

| 코드 | 범주 | 설명 |
|---|---|---|
| A01 | 지식 | 관련 문서 없음 (knowledge gap) |
| A02 | 지식 | 문서 있으나 최신 아님 (stale content) |
| A03 | 파이프라인 | 파싱 실패 (HTML/PDF 처리 오류) |
| A04 | 파이프라인 | 검색 실패 (retrieval 0건) |
| A05 | 파이프라인 | 재랭킹 실패 (reranking 오류) |
| A06 | 생성 | 생성 답변 왜곡 (hallucination) |
| A07 | 이해 | 질문 의도 분류 실패 |
| A08 | 정책 | 정책상 답변 제한 |
| A09 | 사용자 | 질문 표현 모호함 |
| A10 | 채널 | UI/입력 문제 |

## Impact

- **영향 모듈**: `chat-runtime`, `qa-review`, `apps/admin-api`
- **영향 API**: `POST /admin/questions` (failure_reason_code 검증), `POST /admin/qa-reviews` (root_cause_code 검증)
- **영향 테스트**: `ChatRuntimeApiTests`, `QAReviewApiTests`

## Done Definition

- [ ] `FailureReasonCode` enum 정의 (shared-kernel 또는 각 모듈 내)
- [ ] `questions.failure_reason_code` 입력 검증 추가
- [ ] `qa_reviews.root_cause_code` 기존 자유 문자열에 표준 코드 허용 검증 추가 (하위 호환)
- [ ] API 오류 응답: 잘못된 코드 → 400 + 허용 목록 안내
- [ ] `./gradlew test` 전체 통과
