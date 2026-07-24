package com.opencode.thin.ui.screens.providers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.thin.data.model.ProviderItem
import com.opencode.thin.data.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProvidersUiState(
    val providers: List<ProviderItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProviderSelectorViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProvidersUiState())
    val state: StateFlow<ProvidersUiState> = _state.asStateFlow()

    init { loadProviders() }

    fun loadProviders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val allResult = configRepository.listProviders()
            val configResult = configRepository.getConfigProviders()

            val connectedIds = configResult.getOrNull()?.providers
                ?.filter { it.isConnected }
                ?.map { it.id }
                ?.toSet() ?: emptySet()

            val all = allResult.getOrNull()?.all ?: emptyList()
            val merged = all.map { brief ->
                ProviderItem(
                    id = brief.id,
                    name = brief.name,
                    isConnected = brief.id in connectedIds,
                )
            }

            val sorted = merged.sortedWith(
                compareByDescending<ProviderItem> { it.isConnected }
                    .thenBy { it.name.lowercase() }
            )

            _state.update { it.copy(isLoading = false, providers = sorted) }
        }
    }

    fun updateSearch(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun setApiKey(providerId: String, apiKey: String) {
        viewModelScope.launch {
            configRepository.setAuth(providerId, apiKey)
                .onSuccess { loadProviders() }
        }
    }

    val filteredProviders: List<ProviderItem>
        get() {
            val s = _state.value
            return if (s.searchQuery.isBlank()) s.providers
            else s.providers.filter {
                it.name.contains(s.searchQuery, ignoreCase = true) ||
                it.id.contains(s.searchQuery, ignoreCase = true)
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSelectorScreen(
    onBack: () -> Unit,
    viewModel: ProviderSelectorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showSearch by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (showSearch) {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::updateSearch,
                    onSearch = {},
                    active = false,
                    onActiveChange = {},
                    leadingIcon = {
                        IconButton(onClick = { showSearch = false; viewModel.updateSearch("") }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close")
                        }
                    },
                    placeholder = { Text("Search providers...") },
                    modifier = Modifier.fillMaxWidth(),
                ) {}
            } else {
                TopAppBar(
                    title = { Text("Providers", fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Filled.Search, "Search")
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val providers = viewModel.filteredProviders
                if (providers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No providers", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val connected = providers.takeWhile { it.isConnected }
                        val disconnected = providers.dropWhile { it.isConnected }

                        items(connected, key = { it.id }) { provider ->
                            ProviderCard(
                                provider = provider,
                                onSetKey = { key -> viewModel.setApiKey(provider.id, key) },
                            )
                        }

                        if (disconnected.isNotEmpty()) {
                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                )
                            }
                            items(disconnected, key = { it.id }) { provider ->
                                ProviderCard(
                                    provider = provider,
                                    onSetKey = { key -> viewModel.setApiKey(provider.id, key) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderCard(
    provider: ProviderItem,
    onSetKey: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = provider.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = provider.id,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (provider.isConnected) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = com.opencode.thin.ui.theme.AccentGreen.copy(alpha = 0.2f),
                ) {
                    Text(
                        text = "Connected",
                        fontSize = 11.sp,
                        color = com.opencode.thin.ui.theme.AccentGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            TextButton(
                onClick = { showDialog = true },
                colors = if (provider.isConnected) ButtonDefaults.textButtonColors()
                else ButtonDefaults.textButtonColors(contentColor = com.opencode.thin.ui.theme.AccentBlue),
            ) {
                Text(if (provider.isConnected) "Change Key" else "Set Key")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(provider.name) },
            text = {
                Column {
                    if (provider.isConnected) {
                        Text(
                            "Already connected. Enter a new key to update.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSetKey(apiKey)
                        showDialog = false
                    },
                    enabled = apiKey.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } },
        )
    }
}
