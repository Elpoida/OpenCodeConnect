package com.opencode.thin.ui.screens.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.thin.data.model.ConnectionConfig
import com.opencode.thin.data.remote.OpencodeApi
import com.opencode.thin.data.remote.ServerConfig
import com.opencode.thin.data.repository.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectUiState(
    val baseUrl: String = "http://192.168.1.100:4096",
    val password: String = "",
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null,
    val serverVersion: String? = null,
)

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    private val opencodeApi: OpencodeApi,
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectUiState())
    val state: StateFlow<ConnectUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val config = connectionRepository.getConnectionConfig()
            _state.update {
                it.copy(baseUrl = config.baseUrl, password = config.password)
            }
        }
    }

    fun updateBaseUrl(url: String) {
        _state.update { it.copy(baseUrl = url, error = null) }
    }

    fun updatePassword(password: String) {
        _state.update { it.copy(password = password, error = null) }
    }

    fun connect(onConnected: () -> Unit) {
        val s = _state.value
        val rawUrl = s.baseUrl.trimEnd('/')
        val normalizedUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) "http://$rawUrl" else rawUrl
        val config = ConnectionConfig(normalizedUrl, s.password)

        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true, error = null) }
            connectionRepository.saveConnection(config)

            ServerConfig.baseUrl = config.baseUrl
            ServerConfig.password = config.password

            val result = runCatching { opencodeApi.health() }
            result.fold(
                onSuccess = { health ->
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = true,
                            serverVersion = health.version,
                        )
                    }
                    onConnected()
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            error = "Failed: ${e.message}",
                        )
                    }
                },
            )
        }
    }
}
