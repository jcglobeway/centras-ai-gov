package com.publicplatform.ragops.adminapi.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

/**
 * pgvector 확장을 활성화하고 embedding_vector 컬럼을 vector(1024) 타입으로 변환한다.
 *
 * V016에서 TEXT로 생성된 embedding_vector를 vector(1024)로 변환한다.
 * CREATE EXTENSION IF NOT EXISTS는 idempotent하므로 중복 실행에 안전하다.
 */
class V018__EnablePgVector : BaseJavaMigration() {

    override fun migrate(context: Context) {
        context.connection.createStatement().use { stmt ->
            stmt.execute("CREATE EXTENSION IF NOT EXISTS vector")
            stmt.execute(
                "ALTER TABLE document_chunks ALTER COLUMN embedding_vector TYPE vector(1024) " +
                    "USING embedding_vector::vector",
            )
        }
    }
}
