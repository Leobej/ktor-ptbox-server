package com.ptbox

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

fun Application.module() {
    DatabaseFactory.init()
    install(CORS) {
        allowHost("localhost:3000")
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
    }

    install(ContentNegotiation) { json() }

    routing {
        post("/scans") {
            val request = call.receive<ScanRequest>()
            val scan = ScanRepository.createScan(request)
            launchScan(scan.id, request.domain)
            call.respond(scan)
        }

        get("/scans/{id}") {
            val scanId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing ID")
            val scan = ScanRepository.getScan(scanId)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Scan not found")
            call.respond(scan)
        }

        get("/scans") { call.respond(ScanRepository.getAllScans()) }
    }
}

object DockerHelper {
    private val client: DockerClient by lazy {
        DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost("unix:///var/run/docker.sock")
            .build()
            .let { config ->
                DockerClientImpl.getInstance(
                    config,
                    ApacheDockerHttpClient.Builder()
                        .dockerHost(config.dockerHost)
                        .build()
                )
            }
    }

    suspend fun runContainer(image: String, cmd: List<String>, binds: Map<String, String>): Int {
        return withContext(Dispatchers.IO) {
            try {
                val container = client.createContainerCmd(image)
                    .withCmd(*cmd.toTypedArray())
                    .withBinds(binds.map { (host, container) ->
                        com.github.dockerjava.api.model.Bind(
                            host,
                            com.github.dockerjava.api.model.Volume(container)
                        )
                    })
                    .exec()

                client.startContainerCmd(container.id).exec()
                client.waitContainerCmd(container.id)
                    .exec(com.github.dockerjava.core.command.WaitContainerResultCallback())
                    .awaitStatusCode()
            } catch (e: Exception) {
                LoggerFactory.getLogger("DockerHelper").error("Container failed", e)
                -1
            }
        }
    }
}

fun launchScan(scanId: String, domain: String) {
    CoroutineScope(Dispatchers.IO).launch {
        val outputDir = Paths.get("/app/harvester_results").also {
            Files.createDirectories(it)
        }

        try {
            val exitCode = DockerHelper.runContainer(
                image = "secsi/theharvester",
                cmd = listOf(
                    "-d", domain,
                    "-b", "bing,duckduckgo",
                    "-f", "/output/$scanId.json"
                ),
                binds = mapOf(
                    outputDir.toString() to "/output"
                )
            )

            if (exitCode == 0) {
                val resultsFile = outputDir.resolve("$scanId.json")
                if (Files.exists(resultsFile)) {
                    val results = Json.decodeFromString<HarvesterResults>(
                        resultsFile.toFile().readText()
                    )
                    ScanRepository.updateScanResults(scanId, results)
                } else {
                    throw IOException("Results file not found at ${resultsFile}")
                }
            } else {
                throw RuntimeException("theHarvester failed with code $exitCode")
            }
        } catch (e: Exception) {
            LoggerFactory.getLogger("ScanLauncher").error("""
                Scan failed!
                Domain: $domain
                Error: ${e.message}
                Output dir: ${outputDir.toAbsolutePath()}
                Files: ${outputDir.toFile().list()?.joinToString()}
            """.trimIndent())
            ScanRepository.updateScanResults(scanId, null)
        }
    }
}

fun main() {
    DatabaseFactory.init()
    embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module).start(wait = true)
}