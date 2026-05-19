package com.maximus.nishaan.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.maximus.nishaan.domain.model.WitnessReport
import com.maximus.nishaan.domain.repository.WitnessReportRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Implementation of WitnessReportRepository using Firebase Firestore (Feature 5).
 */
class WitnessReportRepositoryImpl(
    private val firestore: FirebaseFirestore
) : WitnessReportRepository {

    override suspend fun submitWitnessReport(missingPersonId: String, report: WitnessReport): Result<Unit> = try {
        val missingPersonRef = firestore.collection("missing_persons").document(missingPersonId)
        val reportRef = missingPersonRef.collection("witness_reports").document()

        val reportData = mapOf(
            "sighting_time" to Timestamp(Date(report.sightingTime)),
            "neighborhood" to report.neighborhood,
            "visual_details" to report.visualDetails,
            "contact_info" to report.contactInfo,
            "anonymous" to report.anonymous,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        
        reportRef.set(reportData).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun observeWitnessReportsCount(missingPersonId: String): Flow<Int> = callbackFlow {
        val docRef = firestore.collection("missing_persons").document(missingPersonId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(0)
                return@addSnapshotListener
            }
            val count = snapshot?.getLong("witness_reports_count")?.toInt() ?: 0
            trySend(count)
        }
        awaitClose { listener.remove() }
    }

    override fun observeWitnessReports(missingPersonId: String): Flow<List<WitnessReport>> = callbackFlow {
        val ref = firestore.collection("missing_persons").document(missingPersonId).collection("witness_reports")
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val reports = snapshot?.documents?.mapNotNull { doc ->
                val sightingTimeVal = doc.getTimestamp("sighting_time")?.toDate()?.time ?: doc.getLong("sighting_time") ?: 0L
                val timestampVal = doc.getTimestamp("timestamp")?.toDate()?.time ?: doc.getLong("timestamp") ?: 0L
                WitnessReport(
                    reportId = doc.id,
                    sightingTime = sightingTimeVal,
                    neighborhood = doc.getString("neighborhood").orEmpty(),
                    visualDetails = doc.getString("visual_details").orEmpty(),
                    contactInfo = doc.getString("contact_info"),
                    anonymous = doc.getBoolean("anonymous") ?: true,
                    timestamp = timestampVal
                )
            } ?: emptyList()
            trySend(reports)
        }
        awaitClose { listener.remove() }
    }
}
