rootProject.name = "public-rag-ops-platform"

include(
    ":apps:admin-api",
    ":modules:shared-kernel",
    ":modules:identity-access",
    ":modules:organization-directory",
    ":modules:chat-runtime",
    ":modules:document-registry",
    ":modules:ingestion-ops",
    ":modules:qa-review",
    ":modules:metrics-reporting",
)

