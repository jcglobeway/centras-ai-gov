package com.publicplatform.ragops.adminapi.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

/**
 * pgvector 확장을 활성화하고 embedding_vector 컬럼을 vector(1024) 타입으로 변환한다.
 *
 * H2 인메모리 DB(테스트 환경)에서는 pgvector가 지원되지 않으므로
 * DB 종류를 감지해 PostgreSQL에서만 실행한다.
 * H2에서는 embedding_vector 컬럼이 TEXT 타입으로 그대로 유지된다.
 */
class V018__EnablePgVector : BaseJavaMigration() {

    override fun migrate(context: Context) {
        val dbName = context.connection.metaData.databaseProductName
        if (dbName.contains("H2", ignoreCase = true)) {
            return
        }

        context.connection.createStatement().use { stmt ->
            stmt.execute("CREATE EXTENSION IF NOT EXISTS vector")
            stmt.execute(
                "ALTER TABLE document_chunks ALTER COLUMN embedding_vector TYPE vector(1024) " +
                    "USING embedding_vector::vector",
            )
        }
    }
}
