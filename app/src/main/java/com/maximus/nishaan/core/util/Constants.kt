package com.maximus.nishaan.core.util

/** App-wide constants. */
object Constants {

    // Agent names
    const val AGENT_SENTINEL = "SENTINEL"
    const val AGENT_ANALYST = "ANALYST"
    const val AGENT_COMMANDER = "COMMANDER"
    const val AGENT_MATCHER = "MATCHER"

    // Firestore collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_SIGNALS = "signals"
    const val COLLECTION_CRISES = "crises"
    const val COLLECTION_MISSING_PERSONS = "missing_persons"
    const val COLLECTION_AGENT_TRACES = "agent_traces"
    const val COLLECTION_EYEWITNESS_REPORTS = "eyewitness_reports"

    // Firebase Storage paths
    const val STORAGE_MISSING_PHOTOS = "missing_persons_photos"

    // Geofence / Alert thresholds
    const val ALERT_RADIUS_KM = 3.0
    const val MATCHER_LINK_RADIUS_KM = 5.0
    const val MATCH_SIMILARITY_THRESHOLD = 0.85

    // Confidence thresholds
    const val CONFIDENCE_MONITORING_MAX = 39
    const val CONFIDENCE_ACTIVE_MIN = 40
    const val CONFIDENCE_ACTIVE_MAX = 69
    const val CONFIDENCE_CONFIRMED_MIN = 70
    const val CONFIDENCE_MAX = 95

    // Limits
    const val MAX_ACTIVE_REPORTS_PER_USER = 10
    const val MAX_PHOTO_SIZE_BYTES = 5 * 1024 * 1024 // 5MB
    const val PHOTO_MAX_DIMENSION_PX = 800
    const val DESCRIPTION_MAX_LENGTH = 500

    // DataStore keys
    const val PREF_ONBOARDING_COMPLETE = "onboarding_complete"
    const val PREF_LANGUAGE = "language"

    // Notification channels
    const val CHANNEL_CRITICAL = "channel_critical"
    const val CHANNEL_INFORMATIONAL = "channel_informational"
    const val CHANNEL_MISSING_PERSONS = "channel_missing_persons"
}
