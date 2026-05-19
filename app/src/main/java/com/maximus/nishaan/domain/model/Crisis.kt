package com.maximus.nishaan.domain.model

/**
 * Domain model for a crisis event detected by the agent pipeline.
 * Mapped from Firestore CrisisDocument in the repository layer.
 */
data class Crisis(
    val crisisId: String,
    val crisisType: CrisisType,
    val severity: Severity,
    val confidence: Int,
    val status: CrisisStatus,
    val centroidLat: Double,
    val centroidLng: Double,
    val impactRadiusKm: Double,
    val titleEn: String,
    val titleUr: String,
    val descriptionEn: String,
    val descriptionUr: String,
    val signalIds: List<String>,
    val assignedAgencies: List<String>,
    val missingPersonsCount: Int,
    val analystReasoning: String,
    val createdAt: Long,
    val updatedAt: Long,
    val verificationYes: Int = 0,
    val verificationNo: Int = 0,
    val verificationUnsure: Int = 0,
    val confidenceModifier: Float = 0f,
    val spreadPrediction: SpreadPrediction? = null
)

enum class CrisisType {
    FLOOD, EARTHQUAKE, HEATWAVE, CIVIL_UNREST,
    TRAFFIC_ACCIDENT, INFRASTRUCTURE_FAILURE, UNKNOWN
}

enum class Severity {
    CRITICAL, HIGH, MEDIUM, LOW, MONITORING
}

enum class CrisisStatus {
    MONITORING, ACTIVE, CONFIRMED, RESOLVED
}
