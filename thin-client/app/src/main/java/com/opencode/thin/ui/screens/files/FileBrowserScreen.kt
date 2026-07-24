package com.opencode.thin.ui.screens.files

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opencode.thin.data.model.FileNode
import com.opencode.thin.data.repository.FileRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FileBrowserUiState(
    val currentPath: String = "",
    val serverDirectory: String = "",
    val files: List<FileNode> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val fileRepository: FileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FileBrowserUiState())
    val state: StateFlow<FileBrowserUiState> = _state.asStateFlow()
    private val pathStack = mutableListOf<String>()

    init {
        loadPath()
        loadFiles("")
    }

    private fun loadPath() {
        viewModelScope.launch {
            fileRepository.getPath().onSuccess { path ->
                _state.update { it.copy(serverDirectory = path.directory) }
            }
        }
    }

    fun loadFiles(path: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            fileRepository.listFiles(path)
                .onSuccess { files ->
                    _state.update {
                        it.copy(isLoading = false, currentPath = path, files = files)
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, error = "Failed to load: ${e.message}")
                    }
                }
        }
    }

    fun enterDirectory(path: String) {
        pathStack.add(_state.value.currentPath)
        loadFiles(path)
    }

    fun goBack() {
        if (pathStack.isNotEmpty()) {
            loadFiles(pathStack.removeAt(pathStack.lastIndex))
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    onBack: () -> Unit,
    onFileClick: (String) -> Unit,
    viewModel: FileBrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.currentPath.ifEmpty { state.serverDirectory.ifEmpty { "Files" } },
                            fontSize = 16.sp,
                            maxLines = 1,
                            modifier = Modifier.clickable {
                                val path = state.currentPath.ifBlank { state.serverDirectory }
                                if (path.isNotBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("path", path))
                                    scope.launch { snackbarHostState.showSnackbar("Path copied") }
                                }
                            },
                        )
                        if (state.currentPath.isNotEmpty()) {
                            Text(
                                text = state.serverDirectory,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadFiles(state.currentPath) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(contentPadding = PaddingValues(8.dp)) {
                        if (state.currentPath.isNotEmpty()) {
                            item {
                                ListItem(
                                    headlineContent = { Text("..") },
                                    leadingContent = { Icon(Icons.Filled.FolderOpen, null) },
                                    modifier = Modifier.clickable { viewModel.goBack() },
                                )
                            }
                        }
                        items(state.files, key = { "${it.path}_${it.name}" }) { node ->
                            ListItem(
                                headlineContent = { Text(node.name) },
                                leadingContent = {
                                    Icon(
                                        if (node.type == "directory") Icons.Filled.Folder
                                        else Icons.Filled.Description,
                                        null,
                                        tint = if (node.type == "directory")
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                modifier = Modifier.clickable {
                                    if (node.type == "directory") {
                                        viewModel.enterDirectory(node.path)
                                    } else {
                                        onFileClick(node.path)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
