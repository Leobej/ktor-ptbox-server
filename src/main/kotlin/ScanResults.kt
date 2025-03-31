package com.ptbox

import kotlinx.serialization.Serializable

@Serializable
data class ScanResult(
    val id: String,
    val domain: String,
    val status: String,
    val startTime: String,
    val endTime: String? = null,
    val filePath: String? = null,
    val results: HarvesterResults? = null
)