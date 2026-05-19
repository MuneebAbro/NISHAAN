package com.maximus.nishaan.domain.model

/**
 * Domain model representing a citizen's verification vote for a crisis.
 * Mapped from crises/{crisisId}/verifications/{uid} subcollection in Firestore.
 */
data class Verification(
    val uid: String,
    val response: String, // "YES" | "NO" | "UNSURE"
    val timestamp: Long,
    val anonymous: Boolean
)
