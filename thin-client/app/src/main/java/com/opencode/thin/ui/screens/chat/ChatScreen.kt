package com.opencode.thin.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.opencode.thin.data.model.ChatMessage
import com.opencode.thin.data.model.Provider
import com.opencode.thin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    onBack: () -> Unit,
    onShell: () -> Unit,
    onGit: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(sessionId) { viewModel.loadSession(sessionId) }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Chat", fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AgentChip(state.currentAgent, onClick = viewModel::toggleAgent)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = viewModel.currentModelLabel.ifEmpty { "no model" },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = viewModel::showModelSelector) { Icon(Icons.Filled.ModelTraining, "Models") }
                    IconButton(onClick = onShell) { Icon(Icons.Filled.Terminal, "Shell") }
                    IconButton(onClick = onGit) { Icon(Icons.Filled.Commit, "Git") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp, shadowElevation = 4.dp, modifier = Modifier.imePadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = viewModel::updateInput,
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier.weight(1f), singleLine = false, maxLines = 4,
                        textStyle = TextStyle(fontSize = 14.sp), shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = if (state.isSending) viewModel::abort else viewModel::sendMessage,
                        enabled = state.isSending || state.inputText.isNotBlank(),
                    ) {
                        if (state.isSending) {
                            val pulseAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
                            LaunchedEffect(Unit) {
                                while (true) {
                                    pulseAlpha.animateTo(0.4f, animationSpec = androidx.compose.animation.core.tween(600))
                                    pulseAlpha.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(600))
                                }
                            }
                            Icon(Icons.Filled.Stop, "Stop", modifier = Modifier.graphicsLayer { alpha = pulseAlpha.value })
                        } else {
                            Icon(Icons.Filled.Send, "Send")
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (state.messages.isEmpty() && !state.isSending) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Send a message to start coding", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    state = listState, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                ) { items(state.messages, key = { it.id }) { msg -> MessageBubble(msg) } }
            }
            if (state.error != null) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = { TextButton(onClick = viewModel::clearError) { Text("Dismiss") } },
                ) { Text(state.error ?: "") }
            }
        }
    }

    state.pendingModelSelection?.let { sel ->
        Dialog(onDismissRequest = viewModel::cancelModelSelection) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Use ${sel.modelName} for...", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.confirmModelAssignment("build") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BuildBlue.copy(alpha = 0.25f), contentColor = BuildBlue),
                    ) { Text("BUILD") }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.confirmModelAssignment("plan") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PlanOrange.copy(alpha = 0.25f), contentColor = PlanOrange),
                    ) { Text("PLAN") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.confirmModelAssignment("both") },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("BOTH") }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = viewModel::cancelModelSelection, colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)) { Text("Cancel") }
                }
            }
        }
    }

    if (state.showModelSelector) {
        ModelSelectorDialog(
            providers = state.connectedProviders,
            currentAgent = state.currentAgent,
            currentModelId = viewModel.currentModelId,
            buildModelId = state.buildModelId,
            planModelId = state.planModelId,
            onSelect = viewModel::selectModel,
            onDismiss = viewModel::hideModelSelector,
        )
    }

    state.pendingPermission?.let { perm ->
        var inputText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {},
            title = { Text(perm.title) },
            text = {
                Column {
                    Text(perm.pattern ?: "The agent needs your response.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type your response...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        textStyle = TextStyle(fontSize = 14.sp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Or choose an option:", style = MaterialTheme.typography.titleSmall)
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (inputText.isNotBlank()) {
                        Button(
                            onClick = { viewModel.sendQuestionResponse(inputText.trim()) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Send Response") }
                    }
                    Button(
                        onClick = { viewModel.respondToPermission(perm.id, "once") },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Allow Once") }
                    Button(
                        onClick = { viewModel.respondToPermission(perm.id, "always") },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Always Allow") }
                    TextButton(
                        onClick = { viewModel.respondToPermission(perm.id, "reject") },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Deny", color = MaterialTheme.colorScheme.error) }
                }
            },
        )
    }

    state.pendingQuestion?.let { q ->
        var answerText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {},
            title = { Text(q.header ?: "Question") },
            text = {
                Column {
                    Text(q.question, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    if (q.options.isNotEmpty()) {
                        Text("Options:", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        q.options.forEach { opt ->
                            Surface(
                                onClick = { viewModel.respondToQuestion(opt.label) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(opt.label, fontSize = 14.sp)
                                    opt.description?.let {
                                        Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Or type your own:", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = answerText,
                        onValueChange = { answerText = it },
                        placeholder = { Text("Type your answer...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false, maxLines = 3,
                        textStyle = TextStyle(fontSize = 14.sp),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.respondToQuestion(answerText.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = answerText.isNotBlank(),
                ) { Text("Submit") }
            },
        )
    }
}

@Composable
fun AgentChip(agent: String, onClick: () -> Unit) {
    val isPlan = agent == "plan"
    val chipColor = if (isPlan) PlanOrange else BuildBlue
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = chipColor.copy(alpha = 0.2f),
    ) {
        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(agent.uppercase(), fontSize = 10.sp, color = chipColor)
            Spacer(Modifier.width(2.dp))
            Icon(Icons.Filled.SwapHoriz, null, Modifier.size(12.dp), tint = chipColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectorDialog(
    providers: List<Provider>,
    currentAgent: String,
    currentModelId: String,
    buildModelId: String,
    planModelId: String,
    onSelect: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Model") },
        text = {
            if (providers.isEmpty()) {
                Text("No models available. Connect a provider in Settings first.", fontSize = 14.sp)
            } else {
                LazyColumn {
                    item {
                        Text("Current: ${currentModelId.ifEmpty { "none" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text("PLAN: ${planModelId.ifEmpty { "using build model" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("BUILD: ${buildModelId.ifEmpty { "using plan model" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                    }
                    providers.forEach { provider ->
                        val modelList = provider.models.values.toList()
                        if (modelList.isEmpty()) return@forEach
                        item {
                            Text(provider.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                        }
                        items(modelList, key = { "${provider.id}/${it.id}" }) { model ->
                            val isBuild = model.id == buildModelId
                            val isPlan = model.id == planModelId
                            Surface(
                                onClick = { onSelect(provider.id, model.id) },
                                color = if (model.id == currentModelId) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (model.id == currentModelId) {
                                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Spacer(Modifier.width(18.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(model.name, fontSize = 13.sp)
                                        Row {
                                            if (isBuild) {
                                                Surface(shape = RoundedCornerShape(2.dp), color = BuildBlue.copy(alpha = 0.2f)) {
                                                    Text(" BUILD ", fontSize = 9.sp, color = BuildBlue, modifier = Modifier.padding(horizontal = 3.dp))
                                                }
                                            }
                                            if (isPlan) {
                                                Spacer(Modifier.width(4.dp))
                                                Surface(shape = RoundedCornerShape(2.dp), color = PlanOrange.copy(alpha = 0.2f)) {
                                                    Text(" PLAN ", fontSize = 9.sp, color = PlanOrange, modifier = Modifier.padding(horizontal = 3.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val bubbleColor = when {
        isUser -> DarkSurfaceVariant
        message.agent == "plan" -> PlanOrange
        else -> BuildBlue
    }
    val textColor = if (isUser) TextPrimary else Color.White
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        if (!isUser && message.agent != null) {
            Text(
                text = message.agent.uppercase(),
                fontSize = 10.sp,
                color = if (message.agent == "plan") PlanOrange else BuildBlue,
                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp),
            )
        }
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(12.dp, 12.dp, if (isUser) 4.dp else 12.dp, if (isUser) 12.dp else 4.dp),
            color = bubbleColor,
        ) {
            SelectionContainer {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = textColor,
                    style = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp),
                    softWrap = true,
                )
            }
        }
    }
}
