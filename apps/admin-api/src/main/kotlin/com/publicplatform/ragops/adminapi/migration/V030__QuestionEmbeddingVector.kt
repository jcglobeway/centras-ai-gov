package com.publicplatform.ragops.adminapi.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

/**
 * question_embedding 컬럼에 HNSW 인덱스를 생성한다.
 *
 * V029에서 question_embedding이 vector(1024)로 생성되므로 타입 변환은 불필요하다.
 * HNSW 파라미터: m=16, ef_construction=64 (중소규모 데이터셋 최적화)
 */
class V030__QuestionEmbeddingVector : BaseJavaMigration() {

    override fun migrate(context: Context) {
        context.connection.createStatement().use { stmt ->
            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_questions_embedding ON questions " +
                    "USING hnsw (question_embedding vector_cosine_ops) " +
                    "WITH (m = 16, ef_construction = 64)",
            )
        }
    }
}
