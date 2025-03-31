package com.ptbox

import org.jetbrains.exposed.sql.Table

object Scans : Table() {
    val id = varchar("id", 36)
    val domain = varchar("domain", 255)
    val status = varchar("status", 50)
    val startTime = varchar("start_time", 50)
    val endTime = varchar("end_time", 50).nullable()
    val results = text("results").nullable()

    override val primaryKey = PrimaryKey(id)
}

