package com.maximus.nishaan.domain.model

/**
 * Domain model representing an event in the crisis timeline.
 * Mapped from crises/{crisisId}/timeline subcollection in Firestore.
 */
data class TimelineEvent(
    val eventId: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val type: String // "SYSTEM" | "SENTINEL" | "ANALYST" | "COMMANDER"
)
