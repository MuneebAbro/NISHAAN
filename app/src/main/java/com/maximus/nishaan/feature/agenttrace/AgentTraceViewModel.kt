package com.maximus.nishaan.feature.agenttrace

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maximus.nishaan.domain.model.AgentTrace
import com.maximus.nishaan.domain.repository.AgentTraceRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** ViewModel for AgentTraceFragment. Observes real-time trace updates. */
class AgentTraceViewModel(
    private val repository: AgentTraceRepository
) : ViewModel() {

    private val _traces = MutableLiveData<List<AgentTrace>>(emptyList())
    val traces: LiveData<List<AgentTrace>> = _traces

    private val _hasNewActivity = MutableLiveData(false)
    val hasNewActivity: LiveData<Boolean> = _hasNewActivity

    private var previousCount = 0

    /** Starts observing traces for the given crisis. */
    fun observeCrisis(crisisId: String) {
        viewModelScope.launch {
            repository.observeTracesByCrisis(crisisId)
                .catch { /* Silently handle — keep existing data */ }
                .collect { newTraces ->
                    if (newTraces.size > previousCount && previousCount > 0) {
                        _hasNewActivity.value = true
                    }
                    previousCount = newTraces.size
                    _traces.value = newTraces
                }
        }
    }

    /** Called when user scrolls to top, clearing the new activity indicator. */
    fun clearNewActivity() {
        _hasNewActivity.value = false
    }
}
