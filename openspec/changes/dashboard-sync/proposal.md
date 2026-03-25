# Proposal

## Change ID

`dashboard-sync`

## Summary

- **변경 목적**: 운영사/고객사/QA 3개 대시보드 포털과 백엔드 API 파이프라인의 구조적 불일치를 해소한다. 현재 프론트엔드가 기대하는 필드가 백엔드 응답에 없어 핵심 UI가 항상 `-` 또는 0건으로 표시된다.
- **변경 범위**: 백엔드 QuestionResponse 확장, 미결질문 answerStatus/latestReviewStatus 추가, QA리뷰 review_status 필터, V023 온디맨드 집계, 프론트 타입 동기화 및 페이지 필드명 수정
- **제외 범위**: 새 Flyway 마이그레이션 추가 없음. DB 스키마 변경 없음 (V023 컬럼은 이미 존재).

## Impact

- **영향 모듈**: `modules/chat-runtime`, `modules/metrics-reporting`, `modules/qa-review`, `apps/admin-api`
- **영향 API**: `GET /admin/questions`, `GET /admin/questions/unresolved`, `GET /admin/qa-reviews`, `GET /admin/metrics/daily`
- **영향 테스트**: ChatRuntimeApiTests, QAReviewApiTests — 기존 50개 테스트 통과 유지 필수

## Done Definition

- [ ] `GET /admin/questions` 응답에 `failureReasonCode`, `questionCategory`, `isEscalated`, `answerConfidence` 포함
- [ ] `GET /admin/questions/unresolved` 응답에 `answerStatus`, `latestReviewStatus` 포함
- [ ] `GET /admin/qa-reviews?review_status=confirmed_issue` 필터 작동
- [ ] `GET /admin/metrics/daily` 응답에 V023 7개 필드 반환 (seed 또는 온디맨드 집계)
- [ ] 프론트 `Question` 타입이 백엔드 필드명과 일치
- [ ] `client/failure/page.tsx` A01~A10 카운트 실제 데이터 표시
- [ ] `qa/page.tsx` confirmedCount가 페이지 필터 아닌 total 기반
- [ ] `qa/unresolved/page.tsx` latestReviewStatus 정상 표시
- [ ] 백엔드 테스트 50개 모두 통과
