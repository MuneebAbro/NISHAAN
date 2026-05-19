package com.maximus.nishaan.domain.model

/**
 * Domain model representing AI-predicted crisis spread forecast (Feature 3).
 */
data class SpreadPrediction(
    val predictedRadiusKm: Double,
    val direction: String,        // e.g. "NORTH_EAST"
    val confidence: Int,          // 0-100
    val reasoningEn: String,
    val reasoningUr: String
)
