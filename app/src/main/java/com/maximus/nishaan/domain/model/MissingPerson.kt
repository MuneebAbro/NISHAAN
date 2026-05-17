package com.maximus.nishaan.domain.model

/**
 * Domain model for a missing person report.
 * Mapped from Firestore missing_persons collection.
 */
data class MissingPerson(
    val reportId: String,
    val submittedByUid: String,
    val reporterPhone: String,
    val reporterRelationship: String,
    val personName: String,
    val personAge: Int,
    val personGender: String,
    val description: String,
    val lastSeenLat: Double,
    val lastSeenLng: Double,
    val lastSeenAddress: String,
    val photoUrl: String?,
    val linkedCrisisId: String?,
    val status: MissingPersonStatus,
    val matchScore: Double?,
    val submittedAt: Long,
    val updatedAt: Long
)

enum class MissingPersonStatus {
    SEARCHING, LINKED, POTENTIAL_DUPLICATE, FOUND
}
