/**
 * 시민 질문에 대한 RAG 답변 결과 상태.
 *
 * ANSWERED: 정상 답변 생성. FALLBACK: 관련 문서를 찾았으나 신뢰도 미달로 대체 응답 반환.
 * NO_ANSWER: 관련 문서 없음. ERROR: 파이프라인 오류로 답변 생성 실패.
 *
 * value: DB/API에서 사용하는 소문자 문자열 표현.
 * fromString(): DB 값 복원 시 사용. 알 수 없는 값은 ERROR로 fallback.
 * fromStringStrict(): HTTP 입력 파싱 시 사용. 알 수 없는 값은 예외 발생.
 */
package com.publicplatform.ragops.chatruntime.domain

enum class AnswerStatus(val value: String) {
    ANSWERED("answered"),
    FALLBACK("fallback"),
    NO_ANSWER("no_answer"),
    ERROR("error");

    companion object {
        fun fromString(value: String): AnswerStatus =
            entries.firstOrNull { it.value == value } ?: ERROR

        fun fromStringStrict(value: String): AnswerStatus =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid answer_status: $value")
    }
}
