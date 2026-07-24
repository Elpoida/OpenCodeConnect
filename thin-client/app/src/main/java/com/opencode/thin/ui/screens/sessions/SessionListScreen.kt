package com.opencode.thin.ui.screens.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.opencode.thin.data.model.Session
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    onSessionClick: (String) -> Unit,
    onFilesClick: () -> Unit,
    onProvidersClick: () -> Unit,
    onDisconnect: () -> Unit,
    onCreateSession: ((String) -> Unit)? = null,
    viewModel: SessionListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshSessionsSilently()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Sessions", fontSize = 18.sp) },
                actions = {
                    IconButton(
                        onClick = { viewModel.createSession { id -> onCreateSession?.invoke(id) ?: onSessionClick(id) } },
                        enabled = !state.isLoading,
                    ) { Icon(Icons.Filled.Add, "New Session") }
                    IconButton(onClick = onFilesClick) { Icon(Icons.Filled.Folder, "Files") }
                    IconButton(onClick = onProvidersClick) { Icon(Icons.Filled.Settings, "Providers") }
                    IconButton(onClick = onDisconnect) { Icon(Icons.Filled.LinkOff, "Disconnect") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.groups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No sessions yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            viewModel.createSession { id -> onCreateSession?.invoke(id) ?: onSessionClick(id) }
                        }) {
                            Icon(Icons.Filled.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create Session")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.groups.forEach { group ->
                        item(key = "header_${group.label}") {
                            Text(
                                text = group.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp),
                            )
                        }
                        items(group.sessions, key = { it.id }) { session ->
                            SwipeToDeleteSession(
                                session = session,
                                onClick = { onSessionClick(session.id) },
                                onSwiped = { sessionToDelete = session },
                            )
                        }
                    }
                }
            }
        }
    }

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete session?") },
            text = { Text("Are you sure you want to delete \"${session.title.ifEmpty { session.id.take(8) }}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSession(session.id); sessionToDelete = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(
                    onClick = { sessionToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = com.opencode.thin.ui.theme.AccentBlue),
                ) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteSession(session: Session, onClick: () -> Unit, onSwiped: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { onSwiped(); false } else false }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.error).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.onError)
                }
            }
        },
    ) { SessionCard(session, onClick) }
}

@Composable
fun SessionCard(session: Session, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Chat, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(session.title.ifEmpty { "Session ${session.id.take(8)}" }, fontWeight = FontWeight.Medium)
                session.time?.let { t ->
                    val df = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
                    Text(
                        text = "Created: ${df.format(Date(t.created))}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (t.updated != t.created) {
                        Text(
                            text = "Modified: ${df.format(Date(t.updated))}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
