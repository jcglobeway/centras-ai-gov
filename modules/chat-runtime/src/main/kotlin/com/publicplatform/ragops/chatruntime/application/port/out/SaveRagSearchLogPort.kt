package com.publicplatform.ragops.chatruntime.application.port.out

import com.publicplatform.ragops.chatruntime.domain.CreateRagRetrievedDocumentCommand
import com.publicplatform.ragops.chatruntime.domain.CreateRagSearchLogCommand
import com.publicplatform.ragops.chatruntime.domain.RagSearchLogSummary

interface SaveRagSearchLogPort {
    fun saveSearchLog(command: CreateRagSearchLogCommand): RagSearchLogSummary
    fun saveRetrievedDocument(command: CreateRagRetrievedDocumentCommand)
}
