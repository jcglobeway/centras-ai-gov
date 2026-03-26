package com.publicplatform.ragops.adminapi.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

/**
 * question_embedding 컬럼을 TEXT에서 vector(1024) 타입으로 변환하고 HNSW 인덱스를 생성한다.
 *
 * H2 인메모리 DB(테스트 환경)에서는 pgvector가 지원되지 않으므로 no-op 처리한다.
 * HNSW 파라미터: m=16, ef_construction=64 (기본값, 중소규모 데이터셋 최적화)
 */
class V030__QuestionEmbeddingVector : BaseJavaMigration() {

    override fun migrate(context: Context) {
        val dbName = context.connection.metaData.databaseProductName
        if (dbName.contains("H2", ignoreCase = true)) {
            return
        }

        context.connection.createStatement().use { stmt ->
            stmt.execute(
                "ALTER TABLE questions ALTER COLUMN question_embedding TYPE vector(1024) " +
                    "USING question_embedding::vector",
            )
            stmt.execute(
                "CREATE INDEX idx_questions_embedding ON questions " +
                    "USING hnsw (question_embedding vector_cosine_ops) " +
                    "WITH (m = 16, ef_construction = 64)",
            )
        }
    }
}
