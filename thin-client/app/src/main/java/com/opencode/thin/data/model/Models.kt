package com.opencode.thin.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray

@Serializable
data class HealthResponse(
    val healthy: Boolean,
    val version: String,
)

@Serializable
data class Project(
    val id: String,
    val worktree: String = "",
)

@Serializable
data class Session(
    val id: String,
    @SerialName("projectID") val projectID: String,
    val directory: String,
    @SerialName("parentID") val parentID: String? = null,
    val title: String = "",
    val version: String = "",
    val agent: String? = null,
    val model: SessionModel? = null,
    val time: SessionTime? = null,
    val share: SessionShare? = null,
)

@Serializable
data class SessionModel(
    val id: String = "",
    @SerialName("providerID") val providerID: String = "",
)

@Serializable
data class SessionTime(
    val created: Long = 0,
    val updated: Long = 0,
)

@Serializable
data class SessionShare(
    val url: String = "",
)

@Serializable
data class CreateSessionRequest(
    @SerialName("parentID") val parentID: String? = null,
    val title: String? = null,
)

@Serializable
data class UpdateSessionRequest(
    val title: String? = null,
)

@Serializable
data class ForkSessionRequest(
    @SerialName("messageID") val messageId: String? = null,
)

@Serializable
data class InitRequest(
    @SerialName("messageID") val messageId: String? = null,
    @SerialName("providerID") val providerId: String? = null,
    @SerialName("modelID") val modelId: String? = null,
)

@Serializable
data class MessageResponse(
    val info: Message,
    val parts: List<Part>,
)

@Serializable
data class Message(
    val id: String,
    @SerialName("sessionID") val sessionID: String,
    val role: String,
    val agent: String? = null,
    val error: JsonObject? = null,
    val time: MessageTime? = null,
)

@Serializable
data class MessageTime(
    val created: Long = 0,
    val completed: Long? = null,
)

@Serializable
data class Part(
    val id: String,
    @SerialName("sessionID") val sessionID: String? = null,
    @SerialName("messageID") val messageID: String? = null,
    val type: String,
    val text: String? = null,
    val tool: String? = null,
    val state: JsonObject? = null,
    val language: String? = null,
)

@Serializable
data class SendMessageRequest(
    val parts: List<PartInput>,
    @SerialName("messageID") val messageId: String? = null,
    val model: ModelRef? = null,
    val agent: String? = null,
)

@Serializable
data class PartInput(
    val type: String,
    val text: String,
)

@Serializable
data class ModelRef(
    @SerialName("providerID") val providerID: String,
    @SerialName("modelID") val modelID: String,
)

@Serializable
data class ShellRequest(
    val command: String,
    @EncodeDefault val agent: String = "build",
    val model: ModelRef? = null,
)

@Serializable
data class FileDiff(
    val file: String = "",
    val patch: String = "",
    val additions: Int = 0,
    val deletions: Int = 0,
    val status: String = "",
)

@Serializable
data class FileNode(
    val name: String,
    val path: String,
    val absolute: String = "",
    val type: String,
    val ignored: Boolean = false,
    val children: List<FileNode>? = null,
)

@Serializable
data class FileContent(
    val type: String,
    val content: String,
    val encoding: String? = null,
    @SerialName("mimeType") val mimeType: String? = null,
)

@Serializable
data class SearchResult(
    val path: String,
    val lines: String = "",
    @SerialName("line_number") val lineNumber: Int = 0,
    @SerialName("absolute_offset") val absoluteOffset: Int = 0,
    val submatches: List<Submatch> = emptyList(),
)

@Serializable
data class Submatch(val content: String)

@Serializable
data class Config(
    val theme: String? = null,
    val model: String? = null,
    val agent: String? = null,
)

@Serializable
data class ProviderBrief(
    val id: String,
    val name: String,
    val env: List<String> = emptyList(),
)

@Serializable
data class ProviderListResponse(
    val all: List<ProviderBrief> = emptyList(),
)

@Serializable
data class ProvidersConfigResponse(
    val providers: List<Provider> = emptyList(),
    val default: Map<String, String> = emptyMap(),
)

@Serializable
data class Provider(
    val id: String,
    val name: String,
    val source: String = "env",
    val env: List<String> = emptyList(),
    val key: String? = null,
    val models: Map<String, Model> = emptyMap(),
) {
    val isConnected: Boolean get() = key != null || source == "env"
}

@Serializable
data class Model(
    val id: String,
    val name: String,
)

@Serializable
data class AuthBody(
    val type: String,
    val key: String,
)

@Serializable
data class Agent(
    val name: String,
    val description: String? = null,
    val mode: String = "primary",
)

@Serializable
data class McpStatus(
    val status: String? = null,
)

@Serializable
data class VcsInfo(
    val branch: String = "",
    @SerialName("default_branch") val defaultBranch: String = "",
)

@Serializable
data class ServerPath(
    val directory: String = "",
    val home: String? = null,
    val worktree: String? = null,
)

@Serializable
data class Command(
    val name: String,
    val description: String? = null,
)

data class ConnectionConfig(
    val baseUrl: String,
    val password: String = "",
)

data class ChatMessage(
    val id: String,
    val role: String,
    val content: String,
    val agent: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

data class ProviderItem(
    val id: String,
    val name: String,
    val isConnected: Boolean,
)

data class ModelItem(
    val providerId: String,
    val modelId: String,
    val label: String,
)

@Serializable
data class PermissionResponse(
    val response: String,
    val remember: Boolean = false,
)

@Serializable
data class ControlResponse(
    val body: String,
)

data class PendingPermission(
    val id: String,
    val title: String,
    val type: String,
    val pattern: String? = null,
    val messageId: String? = null,
)

data class QuestionOption(
    val label: String,
    val description: String? = null,
)

data class PendingQuestion(
    val id: String,
    val header: String?,
    val question: String,
    val options: List<QuestionOption>,
)
