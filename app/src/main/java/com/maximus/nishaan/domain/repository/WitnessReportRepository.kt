package com.maximus.nishaan.domain.repository

import com.maximus.nishaan.domain.model.WitnessReport
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for submitting and observing witness sightings (Feature 5).
 */
interface WitnessReportRepository {
    suspend fun submitWitnessReport(missingPersonId: String, report: WitnessReport): Result<Unit>
    fun observeWitnessReportsCount(missingPersonId: String): Flow<Int>
}
