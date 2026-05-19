package com.maximus.nishaan.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.maximus.nishaan.core.util.Constants
import com.maximus.nishaan.domain.model.Crisis
import com.maximus.nishaan.domain.model.CrisisStatus
import com.maximus.nishaan.domain.model.CrisisType
import com.maximus.nishaan.domain.model.Severity
import com.maximus.nishaan.domain.repository.CrisisRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Firestore implementation of CrisisRepository. */
class CrisisRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CrisisRepository {

    /** Observes all crises via Firestore snapshot listener. Filters in-memory to avoid index. */
    override fun observeActiveCrises(): Flow<List<Crisis>> = callbackFlow {
        val activeStatuses = setOf("ACTIVE", "CONFIRMED", "MONITORING")
        val listener = firestore.collection(Constants.COLLECTION_CRISES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Don't close — keep listening for reconnection
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val crises = snapshot?.documents?.mapNotNull { doc ->
                    mapDocumentToCrisis(doc.id, doc.data)
                }?.filter { it.status.name in activeStatuses }
                    ?.sortedByDescending { it.createdAt }
                    .orEmpty()
                trySend(crises)
            }
        awaitClose { listener.remove() }
    }

    /** Gets a single crisis by its document ID. */
    override suspend fun getCrisisById(crisisId: String): Result<Crisis> = try {
        val doc = firestore.collection(Constants.COLLECTION_CRISES)
            .document(crisisId)
            .get()
            .await()
        val crisis = mapDocumentToCrisis(doc.id, doc.data)
        if (crisis != null) Result.success(crisis)
        else Result.failure(Exception("Crisis not found"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun mapDocumentToCrisis(id: String, data: Map<String, Any>?): Crisis? {
        data ?: return null
        val geoPoint = data["centroid"] as? com.google.firebase.firestore.GeoPoint
        return Crisis(
            crisisId = id,
            crisisType = try { CrisisType.valueOf(data["crisis_type"] as? String ?: "UNKNOWN") } catch (_: Exception) { CrisisType.UNKNOWN },
            severity = try { Severity.valueOf(data["severity"] as? String ?: "MONITORING") } catch (_: Exception) { Severity.MONITORING },
            confidence = (data["confidence"] as? Number)?.toInt() ?: 0,
            status = try { CrisisStatus.valueOf(data["status"] as? String ?: "MONITORING") } catch (_: Exception) { CrisisStatus.MONITORING },
            centroidLat = geoPoint?.latitude ?: 0.0,
            centroidLng = geoPoint?.longitude ?: 0.0,
            impactRadiusKm = (data["impact_radius_km"] as? Number)?.toDouble() ?: 1.0,
            titleEn = data["title_en"] as? String ?: "",
            titleUr = data["title_ur"] as? String ?: "",
            descriptionEn = data["description_en"] as? String ?: "",
            descriptionUr = data["description_ur"] as? String ?: "",
            signalIds = (data["signal_ids"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            assignedAgencies = (data["assigned_agencies"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            missingPersonsCount = (data["missing_persons_count"] as? Number)?.toInt() ?: 0,
            analystReasoning = data["analyst_reasoning"] as? String ?: "",
            createdAt = (data["created_at"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L,
            updatedAt = (data["updated_at"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L,
            verificationYes = (data["verification_yes"] as? Number)?.toInt() ?: 0,
            verificationNo = (data["verification_no"] as? Number)?.toInt() ?: 0,
            verificationUnsure = (data["verification_unsure"] as? Number)?.toInt() ?: 0,
            confidenceModifier = (data["confidence_modifier"] as? Number)?.toFloat() ?: 0f,
            spreadPrediction = (data["spread_prediction"] as? Map<*, *>)?.let {
                com.maximus.nishaan.domain.model.SpreadPrediction(
                    predictedRadiusKm = (it["predicted_radius_km"] as? Number)?.toDouble() ?: 1.0,
                    direction = it["direction"] as? String ?: "STATIONARY",
                    confidence = (it["confidence"] as? Number)?.toInt() ?: 0,
                    reasoningEn = it["reasoning_en"] as? String ?: "",
                    reasoningUr = it["reasoning_ur"] as? String ?: ""
                )
            }
        )
    }

    override fun observeVerifications(crisisId: String): Flow<List<com.maximus.nishaan.domain.model.Verification>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_CRISES)
            .document(crisisId)
            .collection("verifications")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val verifs = snapshot?.documents?.mapNotNull { doc ->
                    val uid = doc.getString("uid") ?: doc.id
                    val response = doc.getString("response") ?: return@mapNotNull null
                    val timestamp = (doc.get("timestamp") as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                    val anonymous = doc.getBoolean("anonymous") ?: false
                    com.maximus.nishaan.domain.model.Verification(uid, response, timestamp, anonymous)
                }.orEmpty()
                trySend(verifs)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun submitVerification(crisisId: String, verification: com.maximus.nishaan.domain.model.Verification): Result<Unit> = try {
        val crisisRef = firestore.collection(Constants.COLLECTION_CRISES).document(crisisId)
        val verificationRef = crisisRef.collection("verifications").document(verification.uid)
        
        val verificationData = mapOf(
            "uid" to verification.uid,
            "response" to verification.response,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "anonymous" to verification.anonymous
        )
        
        verificationRef.set(verificationData).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun observeTimeline(crisisId: String): Flow<List<com.maximus.nishaan.domain.model.TimelineEvent>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_CRISES)
            .document(crisisId)
            .collection("timeline")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val events = snapshot?.documents?.mapNotNull { doc ->
                    val eventId = doc.id
                    val title = doc.getString("title") ?: ""
                    val description = doc.getString("description") ?: ""
                    val timestamp = (doc.get("timestamp") as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                    val type = doc.getString("type") ?: "SYSTEM"
                    com.maximus.nishaan.domain.model.TimelineEvent(eventId, title, description, timestamp, type)
                }.orEmpty()
                trySend(events)
            }
        awaitClose { listener.remove() }
    }
}
