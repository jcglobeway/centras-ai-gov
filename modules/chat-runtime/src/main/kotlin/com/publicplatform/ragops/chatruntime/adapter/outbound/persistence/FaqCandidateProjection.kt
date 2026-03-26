package com.publicplatform.ragops.chatruntime.adapter.outbound.persistence

interface FaqCandidateProjection {
    fun getQuestionId(): String
    fun getQuestionText(): String
    fun getQuestionCategory(): String?
    fun getSimilarId(): String
    fun getSimilarText(): String
    fun getSimilarity(): Double
}
