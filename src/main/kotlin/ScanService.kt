package com.ptbox

import java.util.*

//object ScanService {
//    fun startScan(domain: String, tool: String): Scan {
//        val id = UUID.randomUUID().toString()
//        val startTime = System.currentTimeMillis()
//
//        val dockerCommand = when (tool) {
//            "theHarvester" -> "docker run --rm theharvester -d $domain -b all"
//            "amass" -> "docker run --rm amass enum -d $domain"
//            else -> throw IllegalArgumentException("Invalid tool")
//        }
//
//        Runtime.getRuntime().exec(dockerCommand)
//
//        val scan = Scan(id, domain, tool, startTime, null, "Running", null)
//        Database.saveScan(scan)
//        return scan
//    }
//
//    fun getScans(): List<Scan> = Database.getAllScans()
//}