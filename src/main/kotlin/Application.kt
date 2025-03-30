package com.ptbox

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

@Serializable
data class ScanRequest(val domain: String)

@Serializable
data class ScanResult(
    val id: String,
    val domain: String,
    val status: String,
    val startTime: String,
    val endTime: String? = null,
    val filePath: String? = null
)

val scanStorage = Collections.synchronizedList(mutableListOf<ScanResult>())
val logger = LoggerFactory.getLogger("com.example.Application")

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        post("/scans") {
            val request = call.receive<ScanRequest>()
            val scanId = UUID.randomUUID().toString()
            val scan = ScanResult(
                id = scanId,
                domain = request.domain,
                status = "RUNNING",
                startTime = Instant.now().toString()
            )
            scanStorage.add(scan)

            launchScanAsync(
                scanId = scanId,
                domain = request.domain,
                outputDir = Paths.get("harvester_results"),
                storage = scanStorage
            )

            call.respond(scan)
        }
    }
}

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun launchScanAsync(
    scanId: String,
    domain: String,
    outputDir: Path,
    storage: MutableList<ScanResult>
) {
    Thread {
        val outputFile = outputDir.resolve("$scanId.json")
        try {
            val command = listOf(
                "docker", "run", "--rm",
                "-v", "${outputDir.toAbsolutePath()}:/app/output",
                "secsi/theharvester",
                "-d", domain,
                "-b", "all",
                "-f", "/app/output/$scanId.json"
            )

            logger.info("Executing command: ${command.joinToString(" ")}")

            val process = ProcessBuilder()
                .command(command)
                .start()

            val exitCode = process.waitFor()
            logger.info("Process exited with code: $exitCode")

            val scanIndex = storage.indexOfFirst { it.id == scanId }

            if (scanIndex != -1) {
                storage[scanIndex] = storage[scanIndex].copy(
                    status = if (exitCode == 0) "COMPLETED" else "FAILED",
                    endTime = Instant.now().toString(),
                    filePath = if (exitCode == 0) outputFile.toString() else null
                )
            }
        } catch (e: Exception) {
            logger.error("Error executing scan for $domain: ${e.message}", e)
            val scanIndex = storage.indexOfFirst { it.id == scanId }
            if (scanIndex != -1) {
                storage[scanIndex] = storage[scanIndex].copy(
                    status = "FAILED",
                    endTime = Instant.now().toString()
                )
            }
        }
    }.start()
}