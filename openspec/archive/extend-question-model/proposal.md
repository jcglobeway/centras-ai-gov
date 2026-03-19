# Proposal

## Change ID

`extend-question-model`

## Summary

- **변경 목적**: `questions` 테이블에 민원 분류·분석에 필요한 컬럼 추가 (planning_draft 기준)
- **변경 범위**:
  - DB: V021 마이그레이션 — `questions` 테이블 컬럼 추가
  - DB: V021 — `chat_sessions` 테이블 컬럼 추가
  - 도메인: `Question`, `ChatSession` 도메인 모델 업데이트
  - API: Question 조회/생성 응답에 신규 필드 포함
- **제외 범위**: 자동 분류 ML, 카테고리 UI

## 추가 컬럼 상세

### questions 테이블
| 컬럼 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `question_category` | VARCHAR(50) | NULL | 민원 유형 (welfare/tax/traffic/facility/etc) |
| `answer_confidence` | DECIMAL(5,4) | NULL | RAG 신뢰도 점수 (0.0~1.0) |
| `failure_reason_code` | VARCHAR(10) | NULL | 실패 원인 코드 (A01~A10, 별도 change로 표준화) |
| `is_escalated` | BOOLEAN | false | 상담원 전환 여부 |

### chat_sessions 테이블
| 컬럼 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `session_end_type` | VARCHAR(30) | NULL | 해결종료/포기/재문의 |
| `total_question_count` | INTEGER | 0 | 세션 내 누적 질문 수 |

## Impact

- **영향 모듈**: `chat-runtime`, `apps/admin-api`
- **영향 API**: `GET /admin/questions`, `POST /admin/questions`, `GET /admin/chat-sessions/{id}`
- **영향 테스트**: `ChatRuntimeApiTests`

## Done Definition

- [ ] V021 마이그레이션 작성 (ALTER TABLE questions, chat_sessions)
- [ ] `QuestionEntity` / `ChatSessionEntity` 신규 컬럼 매핑
- [ ] `Question` / `ChatSession` 도메인 모델 필드 추가
- [ ] API 응답 DTO에 신규 필드 포함
- [ ] `./gradlew test` 전체 통과
