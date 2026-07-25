package com.opencode.thin.ui.screens.git

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
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

data class GitActionState(
    val isRunning: Boolean = false,
    val error: String? = null,
)

data class GitDiffUiState(
    val diffs: List<FileDiff> = emptyList(),
    val branch: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val stageAll: GitActionState = GitActionState(),
    val unstageAll: GitActionState = GitActionState(),
    val commit: GitActionState = GitActionState(),
    val discardAll: GitActionState = GitActionState(),
)

@HiltViewModel
class GitDiffViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GitDiffUiState())
    val state: StateFlow<GitDiffUiState> = _state.asStateFlow()

    private val _commitMessage = MutableStateFlow("")
    val commitMessage: StateFlow<String> = _commitMessage.asStateFlow()

    private val _showCommitDialog = MutableStateFlow(false)
    val showCommitDialog: StateFlow<Boolean> = _showCommitDialog.asStateFlow()

    private val _showDiscardDialog = MutableStateFlow(false)
    val showDiscardDialog: StateFlow<Boolean> = _showDiscardDialog.asStateFlow()

    init { loadAll() }

    private fun loadAll() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            sessionRepository.getVcsInfo()
                .onSuccess { _state.update { s -> s.copy(branch = it.branch) } }
                .onFailure { _state.update { s -> s.copy(error = it.message) } }
            sessionRepository.getVcsDiff()
                .onSuccess { diffs ->
                    _state.update { it.copy(isLoading = false, diffs = diffs, error = null) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun loadDiff() {
        viewModelScope.launch {
            sessionRepository.getVcsDiff()
                .onSuccess { diffs ->
                    _state.update { it.copy(diffs = diffs, error = null) }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
        }
    }

    fun stageAll(sessionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(stageAll = GitActionState(isRunning = true)) }
            sessionRepository.runShell(sessionId, "command git add -A")
                .onSuccess { loadDiff() }
                .onFailure { e ->
                    _state.update { it.copy(stageAll = GitActionState(error = e.message)) }
                }
        }
    }

    fun unstageAll(sessionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(unstageAll = GitActionState(isRunning = true)) }
            sessionRepository.runShell(sessionId, "command git restore --staged .")
                .onSuccess { loadDiff() }
                .onFailure { e ->
                    _state.update { it.copy(unstageAll = GitActionState(error = e.message)) }
                }
        }
    }

    fun commit(sessionId: String, message: String) {
        viewModelScope.launch {
            _state.update { it.copy(commit = GitActionState(isRunning = true)) }
            val escaped = message.replace("'", "'\\''")
            sessionRepository.runShell(sessionId, "command git commit -m '$escaped'")
                .onSuccess {
                    _commitMessage.value = ""
                    _showCommitDialog.value = false
                    loadDiff()
                }
                .onFailure { e ->
                    _state.update { it.copy(commit = GitActionState(error = e.message)) }
                }
        }
    }

    fun discardAll(sessionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(discardAll = GitActionState(isRunning = true)) }
            _showDiscardDialog.value = false
            sessionRepository.runShell(sessionId, "command git restore .")
                .onSuccess { loadDiff() }
                .onFailure { e ->
                    _state.update { it.copy(discardAll = GitActionState(error = e.message)) }
                }
        }
    }

    fun showCommit() { _showCommitDialog.value = true }
    fun hideCommit() { _showCommitDialog.value = false }
    fun updateCommitMessage(msg: String) { _commitMessage.value = msg }
    fun showDiscard() { _showDiscardDialog.value = true }
    fun hideDiscard() { _showDiscardDialog.value = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitDiffScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: GitDiffViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val showCommit by viewModel.showCommitDialog.collectAsState()
    val showDiscard by viewModel.showDiscardDialog.collectAsState()
    val commitMsg by viewModel.commitMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.stageAll.error, state.unstageAll.error, state.commit.error, state.discardAll.error) {
        val err = state.stageAll.error ?: state.unstageAll.error ?: state.commit.error ?: state.discardAll.error
        if (err != null) {
            snackbarHostState.showSnackbar(err, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Git Diff", fontSize = 18.sp)
                        if (state.branch.isNotBlank()) {
                            Text(
                                state.branch,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.diffs.isNotEmpty() || state.isLoading) {
                GitActionBar(
                    hasChanges = state.diffs.isNotEmpty(),
                    stageLoading = state.stageAll.isRunning,
                    unstageLoading = state.unstageAll.isRunning,
                    commitLoading = state.commit.isRunning,
                    discardLoading = state.discardAll.isRunning,
                    onStageAll = { viewModel.stageAll(sessionId) },
                    onUnstageAll = { viewModel.unstageAll(sessionId) },
                    onCommit = { viewModel.showCommit() },
                    onDiscard = { viewModel.showDiscard() },
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    state.error != null && state.diffs.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                state.error ?: "Error",
                                color = ErrorRed,
                                fontSize = 14.sp,
                            )
                        }
                    }
                    state.diffs.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No changes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.diffs, key = { it.file }) { diff -> DiffCard(diff) }
                        }
                    }
                }
            }
        }
    }

    if (showCommit) {
        CommitDialog(
            message = commitMsg,
            onMessageChange = viewModel::updateCommitMessage,
            onConfirm = { viewModel.commit(sessionId, commitMsg) },
            onDismiss = viewModel::hideCommit,
            isRunning = state.commit.isRunning,
        )
    }

    if (showDiscard) {
        DiscardDialog(
            onConfirm = { viewModel.discardAll(sessionId) },
            onDismiss = viewModel::hideDiscard,
        )
    }
}

@Composable
private fun GitActionBar(
    hasChanges: Boolean,
    stageLoading: Boolean,
    unstageLoading: Boolean,
    commitLoading: Boolean,
    discardLoading: Boolean,
    onStageAll: () -> Unit,
    onUnstageAll: () -> Unit,
    onCommit: () -> Unit,
    onDiscard: () -> Unit,
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilledTonalButton(
                onClick = onStageAll,
                enabled = !stageLoading && hasChanges,
                modifier = Modifier.height(36.dp),
            ) {
                if (stageLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("Stage All", fontSize = 12.sp, maxLines = 1)
            }
            FilledTonalButton(
                onClick = onUnstageAll,
                enabled = !unstageLoading && hasChanges,
                modifier = Modifier.height(36.dp),
            ) {
                if (unstageLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("Unstage", fontSize = 12.sp, maxLines = 1)
            }
            FilledTonalButton(
                onClick = onCommit,
                enabled = !commitLoading && hasChanges,
                modifier = Modifier.height(36.dp),
            ) {
                if (commitLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("Commit", fontSize = 12.sp, maxLines = 1)
            }
            FilledTonalButton(
                onClick = onDiscard,
                enabled = !discardLoading && hasChanges,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = ErrorRed.copy(alpha = 0.12f),
                    contentColor = ErrorRed,
                ),
                modifier = Modifier.height(36.dp),
            ) {
                if (discardLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("Discard", fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CommitDialog(
    message: String,
    onMessageChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isRunning: Boolean,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Commit Changes") },
        text = {
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                label = { Text("Commit message") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRunning,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = message.isNotBlank() && !isRunning,
            ) {
                if (isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Commit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isRunning) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun DiscardDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard Changes?", color = ErrorRed) },
        text = {
            Text("This will permanently discard all unstaged changes in tracked files. This cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
            ) {
                Text("Discard")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun DiffCard(diff: FileDiff) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = diff.file,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusBadge(diff.status)
            }
            if (diff.additions > 0 || diff.deletions > 0) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("+${diff.additions}", color = AccentGreen, fontSize = 12.sp)
                    Text("-${diff.deletions}", color = ErrorRed, fontSize = 12.sp)
                }
            }
            if (diff.patch.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = diff.patch,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    if (status.isBlank()) return
    val (color, label) = when (status) {
        "added" -> AccentGreen to "added"
        "deleted" -> ErrorRed to "deleted"
        "modified" -> MaterialTheme.colorScheme.primary to "modified"
        else -> MaterialTheme.colorScheme.onSurfaceVariant to status
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
