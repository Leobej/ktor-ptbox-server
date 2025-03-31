package com.ptbox

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        println("Initializing database...")
        val dbPath = System.getenv("DB_PATH") ?: "scans.db"
        println("Using database path: $dbPath")

        Database.connect(
            "jdbc:sqlite:$dbPath",
            driver = "org.sqlite.JDBC",
            setupConnection = { connection ->
                println("Database connection established")
                connection.autoCommit = false
            }
        )

        transaction {
            println("Creating tables if needed")  // Add this
            SchemaUtils.createMissingTablesAndColumns(Scans)
        }
    }
}