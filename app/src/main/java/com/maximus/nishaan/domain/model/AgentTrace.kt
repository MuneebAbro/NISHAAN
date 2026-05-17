package com.maximus.nishaan.domain.model

/**
 * Domain model for agent decision trace entries.
 * Displayed in real-time on the Agent Trace View screen.
 */
data class AgentTrace(
    val traceId: String,
    val agentName: String,
    val crisisId: String?,
    val missingPersonId: String?,
    val action: String,
    val reasoningSummary: String,
    val confidence: Int?,
    val metadata: Map<String, Any>,
    val timestamp: Long
)
