package com.maximus.nishaan.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.maximus.nishaan.core.util.Constants
import com.maximus.nishaan.domain.model.AgentTrace
import com.maximus.nishaan.domain.repository.AgentTraceRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Firestore implementation of AgentTraceRepository. */
class AgentTraceRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AgentTraceRepository {

    /** Observes agent traces for a crisis. Filters in-memory to avoid composite index. */
    override fun observeTracesByCrisis(crisisId: String): Flow<List<AgentTrace>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_AGENT_TRACES)
            .whereEqualTo("crisis_id", crisisId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val traces = snapshot?.documents?.mapNotNull { doc ->
                    mapDocumentToTrace(doc.id, doc.data)
                }?.sortedByDescending { it.timestamp }
                    .orEmpty()
                trySend(traces)
            }
        awaitClose { listener.remove() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapDocumentToTrace(id: String, data: Map<String, Any>?): AgentTrace? {
        data ?: return null
        return AgentTrace(
            traceId = id,
            agentName = data["agent_name"] as? String ?: "",
            crisisId = data["crisis_id"] as? String,
            missingPersonId = data["missing_person_id"] as? String,
            action = data["action"] as? String ?: "",
            reasoningSummary = data["reasoning_summary"] as? String ?: "",
            confidence = (data["confidence"] as? Number)?.toInt(),
            metadata = (data["metadata"] as? Map<String, Any>) ?: emptyMap(),
            timestamp = (data["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
        )
    }
}
