package com.publicplatform.ragops.documentregistry.domain

import java.time.Instant

data class DocumentVersionSummary(
    val id: String,
    val documentId: String,
    val versionLabel: String,
    val contentHash: String?,
    val sourceEtag: String?,
    val sourceLastModifiedAt: Instant?,
    val changeDetected: Boolean,
    val snapshotUri: String?,
    val parsedTextUri: String?,
    val createdAt: Instant,
)
