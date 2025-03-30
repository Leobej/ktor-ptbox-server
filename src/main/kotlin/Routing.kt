package com.ptbox

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.util.logging.Logger
//
//fun Application.configureRouting() {
//    val log = LoggerFactory.getLogger(this.javaClass)
//    routing {
//        get("/") {
//            call.respondText("Hello World!")
//        }
//        post("/scan") {
//            log.info("calling scan endpoint" )
//            val request = call.receive<ScanRequest>()
//            val scan = ScanService.startScan(request.domain, request.tool)
//            call.respond(HttpStatusCode.OK, scan)
//        }
//
//        get("/scans") {
//            log.info("calling scans endpoint" )
//            call.respond(HttpStatusCode.OK, ScanService.getScans())
//        }
//    }
//
//
//
//}
