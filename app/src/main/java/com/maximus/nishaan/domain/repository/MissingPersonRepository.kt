package com.maximus.nishaan.domain.repository

import com.maximus.nishaan.domain.model.MissingPerson
import kotlinx.coroutines.flow.Flow

/** Repository interface for missing person reports. */
interface MissingPersonRepository {
    /** Observes all missing persons, optionally filtered by crisis. */
    fun observeMissingPersons(crisisId: String? = null): Flow<List<MissingPerson>>

    /** Gets a single missing person report by ID. */
    suspend fun getMissingPersonById(reportId: String): Result<MissingPerson>

    /** Submits a new missing person report. */
    suspend fun submitReport(person: MissingPerson, photoBytes: ByteArray?): Result<String>

    /** Marks a missing person as found. */
    suspend fun markAsFound(reportId: String): Result<Unit>
}
