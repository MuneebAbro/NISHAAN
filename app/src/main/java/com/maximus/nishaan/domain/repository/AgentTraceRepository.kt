package com.maximus.nishaan.domain.repository

import com.maximus.nishaan.domain.model.AgentTrace
import kotlinx.coroutines.flow.Flow

/** Repository interface for agent trace data. */
interface AgentTraceRepository {
    /** Observes agent traces for a specific crisis in real-time. */
    fun observeTracesByCrisis(crisisId: String): Flow<List<AgentTrace>>
}
