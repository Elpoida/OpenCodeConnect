package com.opencode.thin.data.repository

import com.opencode.thin.data.model.*
import com.opencode.thin.data.remote.OpencodeApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val api: OpencodeApi,
) {
    suspend fun listSessions(): Result<List<Session>> = runCatching {
        api.listSessions()
    }

    suspend fun createSession(title: String? = null): Result<Session> = runCatching {
        api.createSession(CreateSessionRequest(title = title))
    }

    suspend fun getSession(id: String): Result<Session> = runCatching {
        api.getSession(id)
    }

    suspend fun deleteSession(id: String): Result<Boolean> = runCatching {
        api.deleteSession(id)
    }

    suspend fun updateSession(id: String, title: String): Result<Session> = runCatching {
        api.updateSession(id, UpdateSessionRequest(title = title))
    }

    suspend fun forkSession(id: String, messageId: String? = null): Result<Session> = runCatching {
        api.forkSession(id, ForkSessionRequest(messageId = messageId))
    }

    suspend fun abortSession(id: String): Result<Boolean> = runCatching {
        api.abortSession(id)
    }

    suspend fun shareSession(id: String): Result<Session> = runCatching {
        api.shareSession(id)
    }

    suspend fun unshareSession(id: String): Result<Session> = runCatching {
        api.unshareSession(id)
    }

    suspend fun getMessages(id: String): Result<List<MessageResponse>> = runCatching {
        api.getSessionMessages(id)
    }

    suspend fun sendMessage(
        sessionId: String,
        text: String,
        messageId: String? = null,
        providerId: String? = null,
        modelId: String? = null,
        agent: String? = null,
    ): Result<MessageResponse> = runCatching {
        val model = if (providerId != null && modelId != null) {
            ModelRef(providerId, modelId)
        } else null
        api.sendMessage(
            sessionId,
            SendMessageRequest(
                parts = listOf(PartInput(type = "text", text = text)),
                messageId = messageId,
                model = model,
                agent = agent,
            ),
        )
    }

    suspend fun getSessionDiff(id: String): Result<List<FileDiff>> = runCatching {
        api.getSessionDiff(id)
    }

    suspend fun runShell(sessionId: String, command: String): Result<MessageResponse> = runCatching {
        api.runShell(sessionId, ShellRequest(command = command))
    }

    suspend fun respondToPermission(sessionId: String, permissionId: String, response: String): Result<Boolean> = runCatching {
        api.respondToPermission(sessionId, permissionId, PermissionResponse(response = response))
    }

    suspend fun respondToControl(response: String): Result<Boolean> = runCatching {
        api.respondToControl(ControlResponse(body = response))
    }
}
