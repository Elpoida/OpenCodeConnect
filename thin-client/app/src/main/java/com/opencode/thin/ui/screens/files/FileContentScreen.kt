package com.opencode.thin.ui.screens.files

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.thin.data.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FileContentUiState(
    val content: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FileContentViewModel @Inject constructor(
    private val fileRepository: FileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FileContentUiState())
    val state: StateFlow<FileContentUiState> = _state.asStateFlow()

    fun loadFile(path: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            fileRepository.readFile(path)
                .onSuccess { content ->
                    _state.update {
                        it.copy(isLoading = false, content = content.content)
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileContentScreen(
    path: String,
    onBack: () -> Unit,
    viewModel: FileContentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(path) { viewModel.loadFile(path) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(path.split("/").lastOrNull() ?: "", fontSize = 16.sp) },
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
            } else {
                Text(
                    text = state.content,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}
