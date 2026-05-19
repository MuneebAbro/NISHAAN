package com.maximus.nishaan.feature.crisis

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.maximus.nishaan.core.ui.UiState
import com.maximus.nishaan.domain.model.AgentTrace
import com.maximus.nishaan.domain.model.Crisis
import com.maximus.nishaan.domain.model.Verification
import com.maximus.nishaan.domain.repository.AgentTraceRepository
import com.maximus.nishaan.domain.repository.CrisisRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** ViewModel for CrisisDetailFragment. */
class CrisisDetailViewModel(
    private val crisisRepository: CrisisRepository,
    private val agentTraceRepository: AgentTraceRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _crisisState = MutableLiveData<UiState<Crisis>>(UiState.Loading)
    val crisisState: LiveData<UiState<Crisis>> = _crisisState

    private val _traces = MutableLiveData<List<AgentTrace>>(emptyList())
    val traces: LiveData<List<AgentTrace>> = _traces

    private val _verifications = MutableLiveData<List<Verification>>(emptyList())
    val verifications: LiveData<List<Verification>> = _verifications

    private val _userVote = MutableLiveData<String?>(null)
    val userVote: LiveData<String?> = _userVote

    private val _timelineEvents = MutableLiveData<List<com.maximus.nishaan.domain.model.TimelineEvent>>(emptyList())
    val timelineEvents: LiveData<List<com.maximus.nishaan.domain.model.TimelineEvent>> = _timelineEvents

    private var currentCrisisId: String? = null

    /** Loads crisis detail and starts observing agent traces. */
    fun loadCrisis(crisisId: String) {
        currentCrisisId = crisisId
        
        viewModelScope.launch {
            _crisisState.value = UiState.Loading
            val result = crisisRepository.getCrisisById(crisisId)
            result.fold(
                onSuccess = { _crisisState.value = UiState.Success(it) },
                onFailure = { _crisisState.value = UiState.Error(it.message ?: "Failed to load crisis") }
            )
        }

        // Observe agent traces in real-time
        viewModelScope.launch {
            agentTraceRepository.observeTracesByCrisis(crisisId)
                .catch { /* Log error, keep existing traces */ }
                .collect { _traces.value = it }
        }

        // Observe verifications in real-time
        viewModelScope.launch {
            crisisRepository.observeVerifications(crisisId)
                .catch { }
                .collect { _verifications.value = it }
        }

        // Observe timeline in real-time
        viewModelScope.launch {
            crisisRepository.observeTimeline(crisisId)
                .catch { }
                .collect { _timelineEvents.value = it }
        }

        // Load voted response from DataStore
        viewModelScope.launch {
            val key = stringPreferencesKey("voted_crisis_$crisisId")
            dataStore.data.map { preferences ->
                preferences[key]
            }.collect {
                _userVote.value = it
            }
        }
    }

    /** Submits a verification vote response for the crisis. */
    fun submitVote(response: String) {
        val crisisId = currentCrisisId ?: return
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid ?: "anonymous_device"
        val isAnonymous = currentUser?.isAnonymous ?: true

        val verification = Verification(
            uid = uid,
            response = response,
            timestamp = System.currentTimeMillis(),
            anonymous = isAnonymous
        )

        viewModelScope.launch {
            val result = crisisRepository.submitVerification(crisisId, verification)
            if (result.isSuccess) {
                // Persist in DataStore
                val key = stringPreferencesKey("voted_crisis_$crisisId")
                dataStore.edit { preferences ->
                    preferences[key] = response
                }
            }
        }
    }
}
