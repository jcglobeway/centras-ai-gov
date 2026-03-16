package com.publicplatform.ragops.qareview

/**
 * 미해결 질문에 대한 QA 검토 워크플로우를 담당하는 바운디드 컨텍스트.
 *
 * QA 리뷰는 append-only 레코드로 관리되며 상태 머신을 통해 검토 흐름을 제어한다.
 * `qa_admin` 역할만 리뷰를 생성할 수 있으며, 루트 원인 코드와 조치 유형이 필요하다.
 *
 * ## 주요 포트 계약
 * - [QAReviewReader]: 리뷰 목록 조회 (question_id 필터 포함)
 * - [QAReviewWriter]: 리뷰 생성
 *
 * ## QA 리뷰 상태 머신
 * ```
 * pending → confirmed_issue → resolved   (정상 흐름)
 * pending → false_alarm                  (false alarm 처리)
 * false_alarm → resolved                 (금지: 예외 발생)
 * ```
 * - `confirmed_issue` 전환 시: `root_cause_code`, `action_type` 필수
 * - `false_alarm` 전환 시: `action_type`은 반드시 `no_action`
 *
 * ## 주요 DB 테이블
 * - `qa_reviews`
 *
 * ## 의존 관계
 * - chat-runtime: `question_id` FK (V012 마이그레이션)
 * - 순환 의존 방지: chat-runtime이 qa_reviews를 조회할 때 네이티브 쿼리 사용
 */
class QAReviewModule
