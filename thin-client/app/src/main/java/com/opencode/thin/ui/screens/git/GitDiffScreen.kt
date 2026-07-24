package com.opencode.thin.ui.screens.git

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.thin.data.model.FileDiff
import com.opencode.thin.data.repository.SessionRepository
import com.opencode.thin.ui.theme.AccentGreen
import com.opencode.thin.ui.theme.ErrorRed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GitDiffUiState(
    val diffs: List<FileDiff> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class GitDiffViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GitDiffUiState())
    val state: StateFlow<GitDiffUiState> = _state.asStateFlow()

    fun loadDiff(sessionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            sessionRepository.getSessionDiff(sessionId)
                .onSuccess { diffs ->
                    _state.update { it.copy(isLoading = false, diffs = diffs) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitDiffScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: GitDiffViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(sessionId) { viewModel.loadDiff(sessionId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Git Diff", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.diffs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { Text("No changes", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.diffs) { diff -> DiffCard(diff) }
                }
            }
        }
    }
}

@Composable
fun DiffCard(diff: FileDiff) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = diff.file,
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            )
            if (diff.additions > 0 || diff.deletions > 0) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("+${diff.additions}", color = AccentGreen, fontSize = 12.sp)
                    Text("-${diff.deletions}", color = ErrorRed, fontSize = 12.sp)
                }
            }
            if (diff.after.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = diff.after,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}
