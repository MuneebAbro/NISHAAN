package com.maximus.nishaan.domain.repository

import com.maximus.nishaan.domain.model.Crisis
import kotlinx.coroutines.flow.Flow

/** Repository interface for crisis data. Implemented in data layer. */
interface CrisisRepository {
    /** Observes all active/confirmed crises in real-time. */
    fun observeActiveCrises(): Flow<List<Crisis>>

    /** Gets a single crisis by ID. */
    suspend fun getCrisisById(crisisId: String): Result<Crisis>
}
