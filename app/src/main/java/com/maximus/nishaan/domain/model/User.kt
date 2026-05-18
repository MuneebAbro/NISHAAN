package com.maximus.nishaan.domain.model

data class User(
    val uid: String,
    val email: String,
    val displayName: String? = null,
    val cnic: String? = null,
    val photoUrl: String? = null,
    val isVerified: Boolean = false
)
