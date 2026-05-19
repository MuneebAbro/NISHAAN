package com.maximus.nishaan.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.maximus.nishaan.data.repository.AgentTraceRepositoryImpl
import com.maximus.nishaan.data.repository.CrisisRepositoryImpl
import com.maximus.nishaan.data.repository.MissingPersonRepositoryImpl
import com.maximus.nishaan.data.repository.UserRepositoryImpl
import com.maximus.nishaan.domain.repository.AgentTraceRepository
import com.maximus.nishaan.domain.repository.CrisisRepository
import com.maximus.nishaan.domain.repository.MissingPersonRepository
import com.maximus.nishaan.domain.repository.UserRepository

/**
 * Manual dependency injection container.
 * Holds all repository instances. Instantiated once in NishaanApplication.
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nishaan_prefs")

class AppContainer(private val context: Context) {

    /** DataStore for user preferences (language, onboarding state). */
    val dataStore: DataStore<Preferences> = context.dataStore

    // ── Repositories ────────────────────────────────────────────────────
    val crisisRepository: CrisisRepository by lazy { CrisisRepositoryImpl() }
    val agentTraceRepository: AgentTraceRepository by lazy { AgentTraceRepositoryImpl() }
    val missingPersonRepository: MissingPersonRepository by lazy { MissingPersonRepositoryImpl() }
    val userRepository: UserRepository by lazy { UserRepositoryImpl() }
    val witnessReportRepository: com.maximus.nishaan.domain.repository.WitnessReportRepository by lazy {
        com.maximus.nishaan.data.repository.WitnessReportRepositoryImpl(com.google.firebase.firestore.FirebaseFirestore.getInstance())
    }
}
