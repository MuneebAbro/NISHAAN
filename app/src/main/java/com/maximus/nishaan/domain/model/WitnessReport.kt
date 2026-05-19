package com.maximus.nishaan.domain.model

/**
 * Domain model representing a witness sighting report for a missing person (Feature 5).
 */
data class WitnessReport(
    val reportId: String = "",
    val sightingTime: Long,
    val neighborhood: String,
    val visualDetails: String,
    val contactInfo: String?,
    val anonymous: Boolean,
    val timestamp: Long
)
