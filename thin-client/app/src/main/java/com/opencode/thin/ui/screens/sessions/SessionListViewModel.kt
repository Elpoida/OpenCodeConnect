package com.opencode.thin.ui.screens.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.thin.data.model.Session
import com.opencode.thin.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class SessionGroup(
    val label: String,
    val sessions: List<Session>,
)

data class SessionsUiState(
    val groups: List<SessionGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SessionListViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionsUiState())
    val state: StateFlow<SessionsUiState> = _state.asStateFlow()

    init { loadSessions() }

    fun loadSessions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            sessionRepository.listSessions()
                .onSuccess { sessions -> _state.update { it.copy(isLoading = false, groups = groupSessions(sessions)) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun refreshSessionsSilently() {
        viewModelScope.launch {
            sessionRepository.listSessions()
                .onSuccess { sessions -> _state.update { it.copy(groups = groupSessions(sessions)) } }
        }
    }

    fun createSession(onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            sessionRepository.createSession()
                .onSuccess { session -> loadSessions(); onCreated(session.id) }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch {
            sessionRepository.deleteSession(id).onSuccess { loadSessions() }
        }
    }

    fun clearError() { _state.update { it.copy(error = null) } }

    private fun groupSessions(sessions: List<Session>): List<SessionGroup> {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        val todayStart = applyDayBoundary(cal, 0).timeInMillis
        val yesterdayStart = applyDayBoundary(cal, -1).timeInMillis
        val weekAgo = now - 7 * 86400000L
        val monthAgo = now - 30 * 86400000L

        val sorted = sessions.sortedByDescending { it.time?.updated ?: it.time?.created ?: 0 }

        val groups = mutableListOf<SessionGroup>()
        var remaining = sorted

        fun take(label: String, pred: (Session) -> Boolean) {
            val (match, rest) = remaining.partition(pred)
            if (match.isNotEmpty()) groups.add(SessionGroup(label, match))
            remaining = rest
        }

        val ts = { s: Session -> s.time?.updated ?: s.time?.created ?: 0 }
        take("Today") { s -> ts(s) >= todayStart }
        take("Yesterday") { s -> ts(s) in yesterdayStart until todayStart }
        take("This Week") { s -> ts(s) in weekAgo until yesterdayStart }
        take("This Month") { s -> ts(s) in monthAgo until weekAgo }
        take("Older") { s -> ts(s) < monthAgo }

        return groups
    }

    private fun applyDayBoundary(cal: Calendar, dayOffset: Int): Calendar {
        cal.add(Calendar.DAY_OF_MONTH, dayOffset)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }
}
