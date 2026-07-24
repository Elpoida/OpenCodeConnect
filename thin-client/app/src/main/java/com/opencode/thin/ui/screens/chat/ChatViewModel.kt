package com.opencode.thin.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opencode.thin.data.model.ChatMessage
import com.opencode.thin.data.model.ModelItem
import com.opencode.thin.data.model.PendingPermission
import com.opencode.thin.data.model.PendingQuestion
import com.opencode.thin.data.model.QuestionOption
import com.opencode.thin.data.model.Provider
import com.opencode.thin.data.repository.ConfigRepository
import com.opencode.thin.data.repository.SessionRepository
import com.opencode.thin.data.remote.SseClient
import com.opencode.thin.data.remote.SseEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
    val currentAgent: String = "build",
    val buildModelId: String = "",
    val buildProviderId: String = "",
    val planModelId: String = "",
    val planProviderId: String = "",
    val connectedProviders: List<Provider> = emptyList(),
    val showModelSelector: Boolean = false,
    val pendingModelSelection: PendingModelSelection? = null,
    val pendingPermission: PendingPermission? = null,
    val pendingQuestion: PendingQuestion? = null,
)

data class PendingModelSelection(
    val providerId: String,
    val modelId: String,
    val modelName: String,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val configRepository: ConfigRepository,
    private val sseClient: SseClient,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var currentSessionId: String = ""
    private var currentAgent: String? = null
    private var streamingAgent: String? = null
    private var streamingMessageId: String? = null
    private var lastPartId: String? = null
    private var newMessageStarted: Boolean = false
    private val seenEventIds = mutableSetOf<String>()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun loadSession(sessionId: String) {
        currentSessionId = sessionId
        streamingMessageId = null
        streamingAgent = null
        lastPartId = null
        newMessageStarted = false
        seenEventIds.clear()
        viewModelScope.launch {
            val msgResult = sessionRepository.getMessages(sessionId)
            msgResult.onSuccess { messages ->
                _state.update { s ->
                    s.copy(
                        messages = messages.map { msg ->
                            ChatMessage(
                                id = msg.info.id,
                                role = msg.info.role,
                        content = msg.parts.mapNotNull { p ->
                            val text = p.text
                            if (!text.isNullOrBlank()) text
                            else {
                                val state = p.state
                                if (state != null) {
                                    val out = state["output"]?.jsonPrimitive?.contentOrNull
                                    val cmd = state["input"]?.jsonObject?.get("command")?.jsonPrimitive?.contentOrNull
                                    val err = state["error"]?.jsonPrimitive?.contentOrNull
                                    val result = buildString {
                                        if (cmd != null) { appendLine("$ $cmd") }
                                        if (!out.isNullOrBlank()) { append(out) }
                                        if (!err.isNullOrBlank()) { append("Error: $err") }
                                    }
                                    result.ifBlank { null }
                                } else null
                            }
                        }.joinToString("\n"),
                                agent = msg.info.agent,
                            )
                        },
                    )
                }
            }

            val sessionResult = sessionRepository.getSession(sessionId)
            sessionResult.onSuccess { session ->
                val modelId = session.model?.id ?: ""
                val providerId = session.model?.providerID ?: ""
                _state.update { s ->
                    s.copy(
                        buildModelId = if (s.buildModelId.isBlank()) modelId else s.buildModelId,
                        buildProviderId = if (s.buildProviderId.isBlank()) providerId else s.buildProviderId,
                        planModelId = if (s.planModelId.isBlank()) modelId else s.planModelId,
                        planProviderId = if (s.planProviderId.isBlank()) providerId else s.planProviderId,
                    )
                }
            }
        }
        startSseListener()
        loadConnectedProviders()
    }

    private fun loadConnectedProviders() {
        viewModelScope.launch {
            configRepository.getConfigProviders().onSuccess { response ->
                _state.update { it.copy(connectedProviders = response.providers) }
            }
        }
    }

    fun toggleAgent() {
        _state.update { it.copy(currentAgent = if (it.currentAgent == "build") "plan" else "build") }
    }

    fun showModelSelector() { _state.update { it.copy(showModelSelector = true) } }
    fun hideModelSelector() { _state.update { it.copy(showModelSelector = false) } }

    fun selectModel(providerId: String, modelId: String) {
        val provider = _state.value.connectedProviders.find { it.id == providerId }
        val modelName = provider?.models?.get(modelId)?.name ?: modelId
        _state.update { it.copy(pendingModelSelection = PendingModelSelection(providerId, modelId, modelName)) }
    }

    fun confirmModelAssignment(target: String) {
        val sel = _state.value.pendingModelSelection ?: return
        val pid = sel.providerId; val mid = sel.modelId
        _state.update { s ->
            when (target) {
                "build" -> s.copy(buildModelId = mid, buildProviderId = pid, pendingModelSelection = null, showModelSelector = false)
                "plan" -> s.copy(planModelId = mid, planProviderId = pid, pendingModelSelection = null, showModelSelector = false)
                "both" -> s.copy(
                    buildModelId = mid, buildProviderId = pid,
                    planModelId = mid, planProviderId = pid,
                    pendingModelSelection = null, showModelSelector = false,
                )
                else -> s.copy(pendingModelSelection = null)
            }
        }
    }

    fun cancelModelSelection() {
        _state.update { it.copy(pendingModelSelection = null) }
    }

    val currentModelId: String get() {
        val s = _state.value
        return if (s.currentAgent == "plan") s.planModelId.ifBlank { s.buildModelId }
        else s.buildModelId.ifBlank { s.planModelId }
    }

    val currentProviderId: String get() {
        val s = _state.value
        return if (s.currentAgent == "plan") s.planProviderId.ifBlank { s.buildProviderId }
        else s.buildProviderId.ifBlank { s.planProviderId }
    }

    val currentModelLabel: String get() {
        val s = _state.value
        val mid = if (s.currentAgent == "plan") s.planModelId else s.buildModelId
        val pid = if (s.currentAgent == "plan") s.planProviderId else s.buildProviderId
        if (mid.isBlank()) return ""
        val provider = s.connectedProviders.find { it.id == pid }
        val model = provider?.models?.get(mid)
        val modelName = model?.name ?: mid
        val providerName = provider?.name ?: pid
        return "$modelName ($providerName)"
    }

    fun updateInput(text: String) { _state.update { it.copy(inputText = text) } }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank() || currentSessionId.isBlank()) return

        val agent = _state.value.currentAgent
        val providerId = currentProviderId.ifBlank { null }
        val modelId = currentModelId.ifBlank { null }

        _state.update {
            it.copy(
                inputText = "",
                isSending = true,
                messages = it.messages + ChatMessage(
                    id = "u${System.currentTimeMillis()}", role = "user", content = text,
                ),
            )
        }

        viewModelScope.launch {
            sessionRepository.sendMessage(
                sessionId = currentSessionId, text = text,
                agent = agent, providerId = providerId, modelId = modelId,
            ).onFailure { e ->
                _state.update { it.copy(isSending = false, error = e.message) }
            }
        }
    }

    fun abort() {
        if (currentSessionId.isBlank()) return
        viewModelScope.launch {
            sessionRepository.abortSession(currentSessionId)
            _state.update { it.copy(isSending = false) }
        }
    }

    private fun startSseListener() {
        viewModelScope.launch {
            while (true) {
                try {
                    sseClient.events().collect { handleSseEvent(it) }
                } catch (_: Exception) { }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun handleSseEvent(event: SseEvent) {
        try {
            val root = json.parseToJsonElement(event.data).jsonObject
            val eventId = root["id"]?.jsonPrimitive?.contentOrNull
            if (eventId != null && !seenEventIds.add(eventId)) return
            val eventType = root["type"]?.jsonPrimitive?.contentOrNull ?: return
            val props = root["properties"]?.jsonObject ?: return
            when (eventType) {
                "message.updated" -> {
                    val info = props["info"]?.jsonObject
                    val role = info?.get("role")?.jsonPrimitive?.contentOrNull
                    val agent = info?.get("agent")?.jsonPrimitive?.contentOrNull
                    val msgId = info?.get("id")?.jsonPrimitive?.contentOrNull
                    if (role == "assistant" && agent != null && msgId != null) {
                        if (msgId != streamingMessageId) {
                            streamingMessageId = msgId
                            streamingAgent = agent
                            lastPartId = null
                            newMessageStarted = true
                        }
                    }
                }
                "message.part.delta" -> {
                    val delta = props["delta"]?.jsonPrimitive?.contentOrNull
                    val field = props["field"]?.jsonPrimitive?.contentOrNull
                    val partID = props["partID"]?.jsonPrimitive?.contentOrNull
                    if (delta != null && (field == null || field == "text")) {
                        if (partID != null && partID != lastPartId) {
                            lastPartId = partID
                            val cur = _state.value.messages.lastOrNull()
                            if (cur?.role == "assistant" && cur.agent == (streamingAgent ?: _state.value.currentAgent) && cur.content.isNotEmpty()) {
                                appendAssistantText("\n")
                            }
                        }
                        appendAssistantText(delta)
                    }
                }
                "message.part.updated" -> {
                    if (streamingAgent != null) {
                        val part = props["part"]?.jsonObject
                        val partType = part?.get("type")?.jsonPrimitive?.contentOrNull
                        if (partType == "tool") {
                            val state = part?.get("state")?.jsonObject
                            val output = state?.get("output")?.jsonPrimitive?.contentOrNull
                            val tool = part?.get("tool")?.jsonPrimitive?.contentOrNull
                            val input = state?.get("input")?.jsonObject
                            val command = input?.get("command")?.jsonPrimitive?.contentOrNull
                            val display = buildString {
                                if (tool == "bash" && command != null) {
                                    appendLine()
                                    appendLine("$ $command")
                                } else if (!output.isNullOrBlank()) {
                                    appendLine()
                                }
                                if (!output.isNullOrBlank()) {
                                    append(output)
                                }
                            }
                            if (display.isNotBlank()) appendAssistantText(display)
                        }
                    }
                }
                "session.idle" -> { _state.update { it.copy(isSending = false) }; streamingAgent = null; lastPartId = null }
                "session.status" -> {
                    val t = props["status"]?.jsonObject?.get("type")?.jsonPrimitive?.contentOrNull
                    if (t == "idle") { _state.update { it.copy(isSending = false) }; streamingAgent = null; lastPartId = null }
                }
                "error" -> {
                    val m = props["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                    _state.update { it.copy(isSending = false, error = m ?: "Unknown error") }
                }
                "permission.asked" -> {
                    val permId = props["id"]?.jsonPrimitive?.contentOrNull ?: return
                    val pType = props["permission"]?.jsonPrimitive?.contentOrNull ?: ""
                    val patterns = props["patterns"]?.jsonArray?.joinToString(", ") { it.jsonPrimitive?.contentOrNull ?: "" } ?: ""
                    val title = when (pType) {
                        "bash" -> "Run shell command"
                        "edit" -> "Edit file"
                        "webfetch" -> "Fetch URL"
                        "external_directory" -> "Access external directory"
                        else -> "Permission: $pType"
                    }
                    val pattern = when (pType) {
                        "bash" -> patterns
                        "edit" -> patterns
                        "external_directory" -> patterns
                        else -> ""
                    }
                    _state.update {
                        it.copy(pendingPermission = PendingPermission(permId, title, pType, pattern))
                    }
                }
                "permission.replied" -> {
                    _state.update { it.copy(pendingPermission = null) }
                }
                "question.asked" -> {
                    val queId = props["id"]?.jsonPrimitive?.contentOrNull ?: return
                    val questions = props["questions"]?.jsonArray
                    val first = questions?.getOrNull(0)?.jsonObject ?: return
                    val qText = first["question"]?.jsonPrimitive?.contentOrNull ?: return
                    val header = first["header"]?.jsonPrimitive?.contentOrNull
                    val opts = first["options"]?.jsonArray
                    val options = opts?.mapNotNull { opt ->
                        val obj = opt.jsonObject ?: return@mapNotNull null
                        val label = obj["label"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                        val desc = obj["description"]?.jsonPrimitive?.contentOrNull
                        QuestionOption(label, desc)
                    } ?: emptyList()
                    _state.update {
                        it.copy(pendingQuestion = PendingQuestion(queId, header, qText, options))
                    }
                }
            }
        } catch (_: Exception) { }
    }

    private fun appendAssistantText(text: String) {
        val agent = streamingAgent ?: _state.value.currentAgent
        val forceNew = newMessageStarted
        newMessageStarted = false
        _state.update { st ->
            val last = st.messages.lastOrNull()
            if (!forceNew && last?.role == "assistant" && last.agent == agent) {
                st.copy(messages = st.messages.dropLast(1) + last.copy(content = last.content + text))
            } else {
                st.copy(
                    messages = st.messages + ChatMessage(
                        id = "a${System.currentTimeMillis()}", role = "assistant", content = text, agent = agent,
                    ),
                )
            }
        }
    }

    fun respondToQuestion(answer: String) {
        _state.update {
            it.copy(
                pendingQuestion = null,
                isSending = true,
                messages = it.messages + ChatMessage(
                    id = "u${System.currentTimeMillis()}", role = "user", content = answer,
                ),
            )
        }
        val agent = _state.value.currentAgent
        val providerId = currentProviderId.ifBlank { null }
        val modelId = currentModelId.ifBlank { null }
        viewModelScope.launch {
            sessionRepository.abortSession(currentSessionId)
            sessionRepository.respondToControl(answer)
            sessionRepository.sendMessage(
                sessionId = currentSessionId, text = answer,
                agent = agent, providerId = providerId, modelId = modelId,
            ).onFailure { e ->
                _state.update { it.copy(isSending = false, error = e.message) }
            }
        }
    }

    fun clearError() { _state.update { it.copy(error = null) } }

    fun respondToPermission(permissionId: String, response: String) {
        viewModelScope.launch {
            sessionRepository.respondToPermission(currentSessionId, permissionId, response)
            _state.update { it.copy(pendingPermission = null) }
        }
    }

    fun sendQuestionResponse(text: String) {
        if (text.isBlank()) return
        _state.update {
            it.copy(
                isSending = true,
                pendingPermission = null,
                messages = it.messages + ChatMessage(
                    id = "u${System.currentTimeMillis()}", role = "user", content = text,
                ),
            )
        }
        val agent = _state.value.currentAgent
        val providerId = currentProviderId.ifBlank { null }
        val modelId = currentModelId.ifBlank { null }
        viewModelScope.launch {
            sessionRepository.sendMessage(
                sessionId = currentSessionId, text = text,
                agent = agent, providerId = providerId, modelId = modelId,
            ).onFailure { e ->
                _state.update { it.copy(isSending = false, error = e.message) }
            }
        }
    }
}
