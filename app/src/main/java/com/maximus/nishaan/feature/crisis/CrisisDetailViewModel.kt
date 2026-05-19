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
                .collect { verifs ->
                    _verifications.value = mergeVerifications(verifs, _userVote.value)
                }
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
            }.collect { vote ->
                _userVote.value = vote
                _verifications.value = mergeVerifications(_verifications.value.orEmpty(), vote)
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

        // Optimistically update local states immediately so UI is responsive and works fully in-app
        _userVote.value = response
        _verifications.value = mergeVerifications(_verifications.value.orEmpty(), response)

        viewModelScope.launch {
            // Persist locally in DataStore first
            val key = stringPreferencesKey("voted_crisis_$crisisId")
            dataStore.edit { preferences ->
                preferences[key] = response
            }

            // Attempt to write to Firestore subcollection, handling permission errors gracefully
            try {
                crisisRepository.submitVerification(crisisId, verification)
            } catch (e: Exception) {
                android.util.Log.d("CrisisDetailViewModel", "Firestore submitVerification failed: ${e.message}")
            }
        }
    }

    private fun mergeVerifications(list: List<Verification>, userVote: String?): List<Verification> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous_device"
        val result = list.toMutableList()
        if (userVote != null) {
            val existingIndex = result.indexOfFirst { it.uid == uid }
            val userVerif = Verification(
                uid = uid,
                response = userVote,
                timestamp = System.currentTimeMillis(),
                anonymous = FirebaseAuth.getInstance().currentUser?.isAnonymous ?: true
            )
            if (existingIndex >= 0) {
                result[existingIndex] = userVerif
            } else {
                result.add(userVerif)
            }
        } else {
            result.removeAll { it.uid == uid }
        }
        return result
    }
}
