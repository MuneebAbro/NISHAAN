package com.maximus.nishaan.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import com.maximus.nishaan.core.util.Constants
import com.maximus.nishaan.domain.model.MissingPerson
import com.maximus.nishaan.domain.model.MissingPersonStatus
import com.maximus.nishaan.domain.repository.MissingPersonRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Firestore implementation of MissingPersonRepository. */
class MissingPersonRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : MissingPersonRepository {

    override fun observeMissingPersons(crisisId: String?): Flow<List<MissingPerson>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_MISSING_PERSONS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                var persons = snapshot?.documents?.mapNotNull { doc ->
                    mapDocumentToPerson(doc.id, doc.data)
                }.orEmpty()

                if (crisisId != null) {
                    persons = persons.filter { it.linkedCrisisId == crisisId }
                }
                persons = persons.sortedByDescending { it.submittedAt }
                trySend(persons)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getMissingPersonById(reportId: String): Result<MissingPerson> = try {
        val doc = firestore.collection(Constants.COLLECTION_MISSING_PERSONS)
            .document(reportId).get().await()
        val person = mapDocumentToPerson(doc.id, doc.data)
        if (person != null) Result.success(person)
        else Result.failure(Exception("Report not found"))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun submitReport(person: MissingPerson, photoBytes: ByteArray?): Result<String> = try {
        var photoUrl: String? = null
        val docRef = firestore.collection(Constants.COLLECTION_MISSING_PERSONS).document()
        val reportId = docRef.id

        if (photoBytes != null) {
            val photoRef = storage.reference
                .child("${Constants.STORAGE_MISSING_PHOTOS}/$reportId/photo.jpg")
            photoRef.putBytes(photoBytes).await()
            photoUrl = photoRef.downloadUrl.await().toString()
        }

        val data = hashMapOf(
            "submitted_by_uid" to person.submittedByUid,
            "reporter_phone" to person.reporterPhone,
            "reporter_relationship" to person.reporterRelationship,
            "person_name" to person.personName,
            "person_age" to person.personAge,
            "person_gender" to person.personGender,
            "description" to person.description,
            "last_seen_location" to GeoPoint(person.lastSeenLat, person.lastSeenLng),
            "last_seen_address" to person.lastSeenAddress,
            "photo_url" to photoUrl,
            "status" to "SEARCHING",
            "submitted_at" to Timestamp.now(),
            "updated_at" to Timestamp.now()
        )
        docRef.set(data).await()
        Result.success(reportId)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun markAsFound(reportId: String, proofUrl: String?, verifiedByUid: String?): Result<Unit> = try {
        val updateData = mutableMapOf<String, Any>(
            "status" to "FOUND",
            "updated_at" to Timestamp.now()
        )
        proofUrl?.let { updateData["found_proof_url"] = it }
        verifiedByUid?.let { updateData["found_verified_by"] = it }

        firestore.collection(Constants.COLLECTION_MISSING_PERSONS)
            .document(reportId)
            .update(updateData)
            .await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    private fun mapDocumentToPerson(id: String, data: Map<String, Any>?): MissingPerson? {
        data ?: return null
        val geo = data["last_seen_location"] as? GeoPoint
        return MissingPerson(
            reportId = id,
            submittedByUid = data["submitted_by_uid"] as? String ?: "",
            reporterPhone = data["reporter_phone"] as? String ?: "",
            reporterRelationship = data["reporter_relationship"] as? String ?: "",
            personName = data["person_name"] as? String ?: "",
            personAge = (data["person_age"] as? Number)?.toInt() ?: 0,
            personGender = data["person_gender"] as? String ?: "",
            description = data["description"] as? String ?: "",
            lastSeenLat = geo?.latitude ?: 0.0,
            lastSeenLng = geo?.longitude ?: 0.0,
            lastSeenAddress = data["last_seen_address"] as? String ?: "",
            photoUrl = data["photo_url"] as? String,
            linkedCrisisId = data["linked_crisis_id"] as? String,
            status = try { MissingPersonStatus.valueOf(data["status"] as? String ?: "SEARCHING") } catch (_: Exception) { MissingPersonStatus.SEARCHING },
            matchScore = (data["match_score"] as? Number)?.toDouble(),
            submittedAt = (data["timestamp"] as? Timestamp)?.toDate()?.time ?: (data["submitted_at"] as? Timestamp)?.toDate()?.time ?: 0L,
            updatedAt = (data["updated_at"] as? Timestamp)?.toDate()?.time ?: 0L,
            witnessReportsCount = (data["witness_reports_count"] as? Number)?.toInt() ?: 0
        )
    }
}
