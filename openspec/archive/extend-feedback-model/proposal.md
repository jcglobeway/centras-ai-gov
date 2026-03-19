# Proposal

## Change ID

`extend-feedback-model`

## Summary

- **변경 목적**: `feedbacks` 테이블(V019)에 행동 신호 기반 해결율·만족도 측정 컬럼 추가
- **변경 범위**:
  - DB: V022 마이그레이션 — `feedbacks` 테이블 컬럼 추가
  - 도메인: `Feedback` 도메인 모델 업데이트
  - API: Feedback 생성/조회 응답에 신규 필드 포함
- **제외 범위**: 행동 신호 자동 수집 SDK, 프론트엔드 클릭 트래킹

## 추가 컬럼 상세

### feedbacks 테이블
| 컬럼 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `feedback_type` | VARCHAR(30) | NULL | resolved/unresolved/follow_up |
| `clicked_link` | BOOLEAN | false | 답변 내 링크 클릭 여부 |
| `clicked_document` | BOOLEAN | false | 참조 문서 클릭 여부 |
| `target_action_type` | VARCHAR(30) | NULL | apply/download/view/inquiry 등 목표 행동 유형 |
| `target_action_completed` | BOOLEAN | false | 목표 행동 완료 여부 |
| `dwell_time_ms` | BIGINT | NULL | 답변 후 체류 시간(ms) |

## Impact

- **영향 모듈**: `chat-runtime`, `apps/admin-api`
- **영향 API**: `POST /admin/feedbacks`, `GET /admin/feedbacks`
- **영향 테스트**: `ChatRuntimeApiTests` (feedback 관련 케이스)

## Done Definition

- [ ] V022 마이그레이션 작성 (ALTER TABLE feedbacks)
- [ ] `FeedbackEntity` 신규 컬럼 매핑
- [ ] `Feedback` 도메인 모델 필드 추가
- [ ] API 요청/응답 DTO 업데이트
- [ ] `./gradlew test` 전체 통과
