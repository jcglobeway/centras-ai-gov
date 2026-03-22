/**
 * RAG 파이프라인 실패 원인 표준 코드 taxonomy (A01~A10).
 *
 * A01~A05는 파이프라인 결함, A06~A07은 모델 결함, A08~A10은 외부 요인.
 * Python worker 또는 QA 리뷰어가 questions.failure_reason_code 컬럼에 기록하며,
 * fromCode()를 통해 코드 문자열을 enum으로 안전하게 변환한다.
 */
package com.publicplatform.ragops.chatruntime.domain

enum class FailureReasonCode(val code: String, val description: String) {
    A01("A01", "관련 문서 없음 — 지식 공백"),
    A02("A02", "문서 있으나 최신 아님 — 오래된 콘텐츠"),
    A03("A03", "파싱 실패 — HTML/PDF 처리 오류"),
    A04("A04", "검색 실패 — 검색 결과 0건"),
    A05("A05", "재랭킹 실패 — reranking 오류"),
    A06("A06", "생성 답변 왜곡 — hallucination"),
    A07("A07", "질문 의도 분류 실패"),
    A08("A08", "정책상 답변 제한"),
    A09("A09", "질문 표현 모호함"),
    A10("A10", "채널 UI/입력 문제");

    companion object {
        private val byCode = entries.associateBy { it.code }

        fun fromCode(code: String): FailureReasonCode =
            byCode[code] ?: throw InvalidFailureReasonCodeException(
                "알 수 없는 실패 원인 코드: $code. 허용 코드: ${byCode.keys.sorted().joinToString()}"
            )

        fun fromCodeOrNull(code: String?): FailureReasonCode? = code?.let { byCode[it] }

        fun isValid(code: String): Boolean = byCode.containsKey(code)
    }
}

class InvalidFailureReasonCodeException(message: String) : RuntimeException(message)
