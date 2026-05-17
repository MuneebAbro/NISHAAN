package com.maximus.nishaan.feature.crisis

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maximus.nishaan.core.ui.UiState
import com.maximus.nishaan.domain.model.AgentTrace
import com.maximus.nishaan.domain.model.Crisis
import com.maximus.nishaan.domain.repository.AgentTraceRepository
import com.maximus.nishaan.domain.repository.CrisisRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** ViewModel for CrisisDetailFragment. */
class CrisisDetailViewModel(
    private val crisisRepository: CrisisRepository,
    private val agentTraceRepository: AgentTraceRepository
) : ViewModel() {

    private val _crisisState = MutableLiveData<UiState<Crisis>>(UiState.Loading)
    val crisisState: LiveData<UiState<Crisis>> = _crisisState

    private val _traces = MutableLiveData<List<AgentTrace>>(emptyList())
    val traces: LiveData<List<AgentTrace>> = _traces

    /** Loads crisis detail and starts observing agent traces. */
    fun loadCrisis(crisisId: String) {
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
    }
}
