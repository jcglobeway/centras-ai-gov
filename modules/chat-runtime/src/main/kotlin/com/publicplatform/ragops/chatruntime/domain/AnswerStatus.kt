/**
 * 시민 질문에 대한 RAG 답변 결과 상태.
 *
 * ANSWERED: 정상 답변 생성. FALLBACK: 관련 문서를 찾았으나 신뢰도 미달로 대체 응답 반환.
 * NO_ANSWER: 관련 문서 없음. ERROR: 파이프라인 오류로 답변 생성 실패.
 */
package com.publicplatform.ragops.chatruntime.domain

enum class AnswerStatus { ANSWERED, FALLBACK, NO_ANSWER, ERROR }
