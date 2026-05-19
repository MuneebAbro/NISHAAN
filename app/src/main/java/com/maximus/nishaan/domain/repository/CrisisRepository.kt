package com.maximus.nishaan.domain.repository

import com.maximus.nishaan.domain.model.Crisis
import com.maximus.nishaan.domain.model.Verification
import com.maximus.nishaan.domain.model.TimelineEvent
import kotlinx.coroutines.flow.Flow

/** Repository interface for crisis data. Implemented in data layer. */
interface CrisisRepository {
    /** Observes all active/confirmed crises in real-time. */
    fun observeActiveCrises(): Flow<List<Crisis>>

    /** Gets a single crisis by ID. */
    suspend fun getCrisisById(crisisId: String): Result<Crisis>

    /** Observes verification submissions for a specific crisis in real-time. */
    fun observeVerifications(crisisId: String): Flow<List<Verification>>

    /** Submits a verification response for a crisis. */
    suspend fun submitVerification(crisisId: String, verification: Verification): Result<Unit>

    /** Observes timeline events for a specific crisis in real-time. */
    fun observeTimeline(crisisId: String): Flow<List<TimelineEvent>>
}
