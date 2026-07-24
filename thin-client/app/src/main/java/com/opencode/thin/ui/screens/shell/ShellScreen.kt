package com.opencode.thin.ui.screens.shell

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.opencode.thin.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import javax.inject.Inject

data class ShellCommand(
    val id: String,
    val command: String,
    val output: String,
    val isRunning: Boolean = false,
)

data class ShellUiState(
    val commands: List<ShellCommand> = emptyList(),
    val input: String = "",
    val isRunning: Boolean = false,
)

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ShellUiState())
    val state: StateFlow<ShellUiState> = _state.asStateFlow()

    private var currentSessionId: String = ""

    fun setSession(sessionId: String) { currentSessionId = sessionId }

    fun updateInput(text: String) { _state.update { it.copy(input = text) } }

    fun runCommand() {
        val raw = _state.value.input.trim()
        if (raw.isBlank()) return
        val firstWord = raw.split("\\s+".toRegex()).firstOrNull() ?: raw
        val builtins = setOf("cd", "export", "alias", "unset", "source", ".", "set", "typeset", "declare", "local", "readonly", "exit", "return", "exec", "eval", "let", "trap", "bg", "fg", "jobs", "disown", "wait", "ulimit", "umask")
        val isBuiltin = firstWord in builtins
        val isAssignment = firstWord.contains("=") && !firstWord.startsWith("-")
        val cmd = if (isBuiltin || isAssignment) raw else "command $raw"
        val sid = currentSessionId
        if (sid.isBlank()) return

        val shellCmd = ShellCommand(
            id = "sh${System.currentTimeMillis()}",
            command = raw,
            output = "",
            isRunning = true,
        )

        _state.update {
            it.copy(
                input = "",
                commands = it.commands + shellCmd,
                isRunning = true,
            )
        }

        viewModelScope.launch {
            sessionRepository.runShell(sid, cmd)
                .onSuccess { msg ->
                    val output = buildString {
                        for (p in msg.parts) {
                            val text = p.text
                            if (!text.isNullOrBlank()) { appendLine(text); continue }
                            val state = p.state
                            if (state != null) {
                                val out = state["output"]?.jsonPrimitive?.contentOrNull
                                if (!out.isNullOrBlank()) { append(out); continue }
                                val meta = state["metadata"]?.jsonObject
                                val metaOut = meta?.get("output")?.jsonPrimitive?.contentOrNull
                                if (!metaOut.isNullOrBlank()) { append(metaOut); continue }
                                val err = state["error"]?.jsonPrimitive?.contentOrNull
                                if (!err.isNullOrBlank()) { appendLine("Error: $err"); continue }
                            }
                        }
                    }.trimEnd()
                    _state.update { st ->
                        st.copy(
                            isRunning = false,
                            commands = st.commands.map { sc ->
                                if (sc.id == shellCmd.id) sc.copy(output = output, isRunning = false)
                                else sc
                            },
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { state ->
                        state.copy(
                            isRunning = false,
                            commands = state.commands.map { sc ->
                                if (sc.id == shellCmd.id) sc.copy(output = "Error: ${e.message}", isRunning = false)
                                else sc
                            },
                        )
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: ShellViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(sessionId) { viewModel.setSession(sessionId) }
    LaunchedEffect(state.commands.size) {
        if (state.commands.isNotEmpty()) {
            listState.animateScrollToItem(state.commands.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shell", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.input,
                        onValueChange = viewModel::updateInput,
                        placeholder = { Text("$ ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = viewModel::runCommand,
                        enabled = state.input.isNotBlank() && !state.isRunning,
                    ) {
                        Icon(Icons.Filled.PlayArrow, "Run")
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.commands, key = { it.id }) { cmd ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "$ ${cmd.command}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (cmd.output.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = cmd.output,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                            )
                        } else if (!cmd.isRunning) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "(no output)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (cmd.isRunning) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
