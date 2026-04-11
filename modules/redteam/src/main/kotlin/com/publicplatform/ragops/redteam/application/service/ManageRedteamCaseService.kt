package com.publicplatform.ragops.redteam.application.service

import com.publicplatform.ragops.redteam.application.port.`in`.ManageRedteamCaseUseCase
import com.publicplatform.ragops.redteam.application.port.out.LoadRedteamCasePort
import com.publicplatform.ragops.redteam.application.port.out.SaveRedteamCasePort
import com.publicplatform.ragops.redteam.domain.CreateRedteamCaseCommand
import com.publicplatform.ragops.redteam.domain.RedteamCaseSummary
import com.publicplatform.ragops.redteam.domain.UpdateRedteamCaseCommand
import java.time.Instant
import java.util.UUID

class ManageRedteamCaseService(
    private val loadRedteamCasePort: LoadRedteamCasePort,
    private val saveRedteamCasePort: SaveRedteamCasePort,
) : ManageRedteamCaseUseCase {

    override fun createCase(command: CreateRedteamCaseCommand): RedteamCaseSummary {
        require(command.title.isNotBlank()) { "title must not be blank" }
        require(command.queryText.isNotBlank()) { "queryText must not be blank" }

        val now = Instant.now()
        val case = RedteamCaseSummary(
            id = "rt_case_${UUID.randomUUID().toString().substring(0, 8)}",
            category = command.category,
            title = command.title,
            queryText = command.queryText,
            expectedBehavior = command.expectedBehavior,
            isActive = true,
            createdBy = command.createdBy,
            createdAt = now,
            updatedAt = now,
        )
        return saveRedteamCasePort.save(case)
    }

    override fun updateCase(id: String, command: UpdateRedteamCaseCommand): RedteamCaseSummary {
        val existing = loadRedteamCasePort.findById(id)
            ?: throw NoSuchElementException("RedteamCase not found: $id")

        val updated = existing.copy(
            title = command.title ?: existing.title,
            queryText = command.queryText ?: existing.queryText,
            expectedBehavior = command.expectedBehavior ?: existing.expectedBehavior,
            isActive = command.isActive ?: existing.isActive,
            updatedAt = Instant.now(),
        )
        return saveRedteamCasePort.update(updated)
    }

    override fun deleteCase(id: String) {
        loadRedteamCasePort.findById(id)
            ?: throw NoSuchElementException("RedteamCase not found: $id")
        saveRedteamCasePort.delete(id)
    }

    override fun listCases(): List<RedteamCaseSummary> =
        loadRedteamCasePort.findAll()
}
