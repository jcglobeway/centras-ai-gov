package com.publicplatform.ragops.identityaccess.adapter.outbound.persistence

import com.publicplatform.ragops.identityaccess.application.port.`in`.AuditLogFilter
import com.publicplatform.ragops.identityaccess.application.port.out.LoadAuditLogPort
import com.publicplatform.ragops.identityaccess.domain.AuditLogEntry
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.time.ZoneOffset

open class LoadAuditLogPortAdapter(
    private val jdbcTemplate: JdbcTemplate,
) : LoadAuditLogPort {

    override fun findByFilter(filter: AuditLogFilter, page: Int, pageSize: Int): List<AuditLogEntry> {
        val (where, params) = buildWhere(filter)
        val sql = "SELECT * FROM audit_logs$where ORDER BY created_at DESC LIMIT ? OFFSET ?"
        return jdbcTemplate.query(sql, rowMapper, *(params + pageSize + (page * pageSize)).toTypedArray())
    }

    override fun countByFilter(filter: AuditLogFilter): Long {
        val (where, params) = buildWhere(filter)
        val sql = "SELECT COUNT(*) FROM audit_logs$where"
        return jdbcTemplate.queryForObject(sql, Long::class.java, *params.toTypedArray()) ?: 0L
    }

    override fun findAllByFilter(filter: AuditLogFilter): List<AuditLogEntry> {
        val (where, params) = buildWhere(filter)
        val sql = "SELECT * FROM audit_logs$where ORDER BY created_at DESC"
        return jdbcTemplate.query(sql, rowMapper, *params.toTypedArray())
    }

    private fun buildWhere(filter: AuditLogFilter): Pair<String, List<Any>> {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()
        filter.from?.let {
            conditions += "created_at >= ?"
            params += it.atStartOfDay().toInstant(ZoneOffset.UTC)
        }
        filter.to?.let {
            conditions += "created_at < ?"
            params += it.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        }
        filter.actionCode?.let { conditions += "action_code = ?"; params += it }
        filter.organizationId?.let { conditions += "organization_id = ?"; params += it }
        filter.actorUserId?.let { conditions += "actor_user_id = ?"; params += it }
        val where = if (conditions.isEmpty()) "" else " WHERE " + conditions.joinToString(" AND ")
        return where to params
    }

    private val rowMapper = RowMapper<AuditLogEntry> { rs: ResultSet, _ ->
        AuditLogEntry(
            id = rs.getString("id"),
            actorUserId = rs.getString("actor_user_id"),
            actorRoleCode = rs.getString("actor_role_code"),
            organizationId = rs.getString("organization_id"),
            actionCode = rs.getString("action_code"),
            resourceType = rs.getString("resource_type"),
            resourceId = rs.getString("resource_id"),
            requestId = rs.getString("request_id"),
            traceId = rs.getString("trace_id"),
            resultCode = rs.getString("result_code"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
    }
}
