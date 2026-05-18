package com.maximus.nishaan.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.maximus.nishaan.domain.model.User
import com.maximus.nishaan.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl : UserRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    override suspend fun getUserProfile(uid: String): Result<User?> {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val user = User(
                    uid = uid,
                    email = doc.getString("email").orEmpty(),
                    displayName = doc.getString("displayName"),
                    cnic = doc.getString("cnic"),
                    photoUrl = doc.getString("photoUrl"),
                    isVerified = doc.getBoolean("isVerified") ?: false
                )
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveUserProfile(user: User): Result<Unit> {
        return try {
            val data = mapOf(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to user.displayName,
                "cnic" to user.cnic,
                "photoUrl" to user.photoUrl,
                "isVerified" to (user.cnic != null && user.photoUrl != null)
            )
            firestore.collection("users").document(user.uid).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadProfileImage(uid: String, imageUri: Uri): Result<String> {
        return try {
            val ref = storage.reference.child("profiles/$uid.jpg")
            ref.putFile(imageUri).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
