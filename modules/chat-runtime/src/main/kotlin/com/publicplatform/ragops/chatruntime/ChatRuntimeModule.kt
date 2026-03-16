package com.publicplatform.ragops.chatruntime

/**
 * 시민 채팅 세션, 질문, 답변의 원본 기록을 관리하는 바운디드 컨텍스트.
 *
 * 질문 생성 시 RAG orchestrator를 호출하여 자동 답변을 생성하고,
 * 미해결 질문 큐를 제공하여 QA 검토 워크플로우의 진입점 역할을 한다.
 *
 * ## 주요 포트 계약
 * - [QuestionReader]: 질문 목록 조회, 미해결 큐 조회
 * - [QuestionWriter]: 질문 생성
 * - [AnswerReader]: 질문별 답변 조회
 * - [AnswerWriter]: 답변 생성
 * - [RagSearchLogWriter]: RAG 검색 로그 저장
 *
 * ## 미해결 큐 가시성 규칙
 * 다음 조건 중 하나를 만족하면 미해결 큐에 노출:
 * - `answer_status`가 `fallback`, `no_answer`, `error`
 * - `answer_status = answered`이지만 최신 QA 리뷰가 `confirmed_issue`
 * 최신 리뷰가 `resolved` 또는 `false_alarm`이면 제외.
 *
 * ## 주요 DB 테이블
 * - `chat_sessions`, `questions`, `answers`, `rag_search_logs`, `rag_retrieved_documents`
 *
 * ## 의존 관계
 * - organization-directory: 기관/서비스 스코프 검증
 * - qa-review 모듈 참조 없음 (순환 의존 방지: native SQL로 qa_reviews 직접 조회)
 */
class ChatRuntimeModule
