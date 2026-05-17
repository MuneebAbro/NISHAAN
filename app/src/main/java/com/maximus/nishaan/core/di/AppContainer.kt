package com.maximus.nishaan.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Manual dependency injection container.
 * Holds all repository instances and use cases.
 * Instantiated once in NishaanApplication.
 *
 * Repositories and use cases will be added here as features are built.
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nishaan_prefs")

class AppContainer(private val context: Context) {

    /** DataStore for user preferences (language, onboarding state). */
    val dataStore: DataStore<Preferences> = context.dataStore

    // ── Repositories (added as features are built) ──────────────────────
    // val authRepository: AuthRepository by lazy { AuthRepositoryImpl() }
    // val crisisRepository: CrisisRepository by lazy { CrisisRepositoryImpl(...) }
    // val missingPersonRepository: MissingPersonRepository by lazy { MissingPersonRepositoryImpl(...) }
    // val agentTraceRepository: AgentTraceRepository by lazy { AgentTraceRepositoryImpl(...) }
    // val userRepository: UserRepository by lazy { UserRepositoryImpl(...) }

    // ── Use Cases (added as features are built) ─────────────────────────
    // val signInUseCase by lazy { SignInUseCase(authRepository) }
    // val getActiveCrisesUseCase by lazy { GetActiveCrisesUseCase(crisisRepository) }
}
