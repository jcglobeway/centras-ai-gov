# Status: unresolved-qa-assignee

## 상태: ✅ 완료 (2026-04-03)

## 구현 요약

미해결 질의 페이지를 이슈 트래커로 전환. 헥사고날 아키텍처 전 레이어(포트/서비스/어댑터/컨트롤러/프론트) 동시 구현.

### 핵심 변경

| 구분 | 파일 | 내용 |
|------|------|------|
| DB | V038 migration | `qa_reviews.assignee_id VARCHAR(64)` |
| 도메인 | `QAReview.kt` | `assigneeId: String?` |
| 포트 | `AssignQAReviewUseCase`, `UpdateQAReviewAssigneePort` | 신규 |
| 서비스 | `AssignQAReviewService` | 신규 |
| 어댑터 | `UpdateQAReviewAssigneePortAdapter` | JPA UPDATE |
| API | `PATCH /admin/qa-reviews/{id}` | 담당자 지정 |
| API | `GET /admin/questions/unresolved` | `assigneeId`, `latestReviewId` 포함 |
| 프론트 | `qa/unresolved/page.tsx` | 근거/카테고리/담당자 컬럼 + 모달 |

## 진행 이력

| 날짜 | 내용 |
|------|------|
| 2026-04-03 | 전체 구현 완료 |
| 2026-04-03 | 54개 테스트 통과 확인 (testcontainers 환경) |
| 2026-04-03 | dev DB flyway repair 완료 (V029/V031 체크섬, V038 out-of-order 수동 적용) |
