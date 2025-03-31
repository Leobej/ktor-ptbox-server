package com.ptbox

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class HarvesterResults(
    val hosts: List<String> = emptyList(),
    val ips: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val shodan: List<String> = emptyList(),
    val dns: List<String> = emptyList(),
    val urls: List<String> = emptyList(),
    val vulnerabilities: List<String> = emptyList(),
    @Transient val otherData: Map<String, String> = emptyMap()
)
