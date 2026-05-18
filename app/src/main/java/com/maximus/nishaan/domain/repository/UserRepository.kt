package com.maximus.nishaan.domain.repository

import com.maximus.nishaan.domain.model.User

interface UserRepository {
    suspend fun getUserProfile(uid: String): Result<User?>
    suspend fun saveUserProfile(user: User): Result<Unit>
    suspend fun uploadProfileImage(uid: String, imageUri: android.net.Uri): Result<String>
    fun clearCache()
}
