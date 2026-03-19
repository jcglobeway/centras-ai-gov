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

        fun isValid(code: String): Boolean = byCode.containsKey(code)
    }
}

class InvalidFailureReasonCodeException(message: String) : RuntimeException(message)
