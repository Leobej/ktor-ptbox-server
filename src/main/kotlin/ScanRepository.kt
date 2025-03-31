package com.ptbox

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

object ScanRepository {
    fun createScan(request: ScanRequest): ScanResult = transaction {
        val scanId = UUID.randomUUID().toString()
        Scans.insert {
            it[id] = scanId
            it[domain] = request.domain
            it[status] = "RUNNING"
            it[startTime] = Instant.now().toString()
        }
        ScanResult(id = scanId, domain = request.domain, status = "RUNNING", startTime = Instant.now().toString())
    }

    fun updateScanResults(scanId: String, results: HarvesterResults?) = transaction {
        Scans.update({ Scans.id eq scanId }) {
            it[endTime] = Instant.now().toString()
            it[status] = results?.let { "COMPLETED" } ?: "FAILED"
            it[this.results] = results?.let(Json::encodeToString)
        }
    }

    fun getScan(scanId: String): ScanResult? = transaction {
        Scans.select { Scans.id eq scanId }.singleOrNull()?.toScanResult()
    }

    fun getAllScans(): List<ScanResult> = transaction {
        Scans.selectAll().map { it.toScanResult() }
    }

    private fun ResultRow.toScanResult() = ScanResult(
        id = this[Scans.id],
        domain = this[Scans.domain],
        status = this[Scans.status],
        startTime = this[Scans.startTime],
        endTime = this[Scans.endTime],
        results = this[Scans.results]?.let { Json.decodeFromString(it) }
    )
}
