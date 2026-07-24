package com.opencode.thin.data.remote

import com.opencode.thin.data.model.*
import kotlinx.serialization.json.JsonObject
import retrofit2.http.*

interface OpencodeApi {

    @GET("global/health")
    suspend fun health(): HealthResponse

    @GET("project")
    suspend fun listProjects(): List<Project>

    @GET("project/current")
    suspend fun currentProject(): Project

    @GET("session")
    suspend fun listSessions(): List<Session>

    @POST("session")
    suspend fun createSession(@Body body: CreateSessionRequest): Session

    @GET("session/{id}")
    suspend fun getSession(@Path("id") id: String): Session

    @DELETE("session/{id}")
    suspend fun deleteSession(@Path("id") id: String): Boolean

    @PATCH("session/{id}")
    suspend fun updateSession(
        @Path("id") id: String,
        @Body body: UpdateSessionRequest,
    ): Session

    @GET("session/{id}/children")
    suspend fun getSessionChildren(@Path("id") id: String): List<Session>

    @POST("session/{id}/fork")
    suspend fun forkSession(
        @Path("id") id: String,
        @Body body: ForkSessionRequest,
    ): Session

    @POST("session/{id}/abort")
    suspend fun abortSession(@Path("id") id: String): Boolean

    @POST("session/{id}/share")
    suspend fun shareSession(@Path("id") id: String): Session

    @DELETE("session/{id}/share")
    suspend fun unshareSession(@Path("id") id: String): Session

    @GET("session/{id}/message")
    suspend fun getSessionMessages(@Path("id") id: String): List<MessageResponse>

    @POST("session/{id}/message")
    suspend fun sendMessage(
        @Path("id") id: String,
        @Body body: SendMessageRequest,
    ): MessageResponse

    @GET("session/{id}/message/{messageId}")
    suspend fun getMessage(
        @Path("id") id: String,
        @Path("messageId") messageId: String,
    ): MessageResponse

    @POST("session/{id}/shell")
    suspend fun runShell(
        @Path("id") id: String,
        @Body body: ShellRequest,
    ): MessageResponse

    @GET("session/{id}/diff")
    suspend fun getSessionDiff(@Path("id") id: String): List<FileDiff>

    @POST("session/{id}/init")
    suspend fun initSession(
        @Path("id") id: String,
        @Body body: InitRequest,
    ): Boolean

    @GET("file")
    suspend fun listFiles(@Query("path") path: String = ""): List<FileNode>

    @GET("file/content")
    suspend fun readFile(@Query("path") path: String): FileContent

    @GET("file/status")
    suspend fun fileStatus(): List<FileNode>

    @GET("find")
    suspend fun searchText(@Query("pattern") pattern: String): List<SearchResult>

    @GET("find/file")
    suspend fun findFiles(@Query("query") query: String): List<String>

    @GET("find/symbol")
    suspend fun findSymbols(@Query("query") query: String): List<SearchResult>

    @GET("config")
    suspend fun getConfig(): Config

    @PATCH("config")
    suspend fun updateConfig(@Body body: Config): Config

    @GET("config/providers")
    suspend fun getConfigProviders(): ProvidersConfigResponse

    @GET("provider")
    suspend fun listProviders(): ProviderListResponse

    @PUT("auth/{id}")
    suspend fun setAuth(
        @Path("id") providerId: String,
        @Body body: AuthBody,
    ): Boolean

    @POST("session/{id}/permissions/{permissionID}")
    suspend fun respondToPermission(
        @Path("id") sessionId: String,
        @Path("permissionID") permissionId: String,
        @Body body: PermissionResponse,
    ): Boolean

    @POST("tui/control/response")
    suspend fun respondToControl(@Body body: ControlResponse): Boolean

    @GET("vcs")
    suspend fun getVcsInfo(): VcsInfo

    @GET("path")
    suspend fun getPath(): ServerPath

    @GET("command")
    suspend fun listCommands(): List<Command>

    @GET("agent")
    suspend fun listAgents(): List<Agent>

    @GET("mcp")
    suspend fun getMcpStatus(): Map<String, McpStatus>

    @GET("lsp")
    suspend fun getLspStatus(): List<JsonObject>

    @GET("event")
    suspend fun events(): String
}
