package com.opencode.thin.data.repository

import com.opencode.thin.data.model.*
import com.opencode.thin.data.remote.OpencodeApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(
    private val api: OpencodeApi,
) {
    suspend fun getConfig(): Result<Config> = runCatching { api.getConfig() }

    suspend fun updateConfig(config: Config): Result<Config> = runCatching {
        api.updateConfig(config)
    }

    suspend fun listProviders(): Result<ProviderListResponse> = runCatching {
        api.listProviders()
    }

    suspend fun getConfigProviders(): Result<ProvidersConfigResponse> = runCatching {
        api.getConfigProviders()
    }

    suspend fun listAgents(): Result<List<Agent>> = runCatching { api.listAgents() }

    suspend fun setAuth(providerId: String, apiKey: String): Result<Boolean> = runCatching {
        api.setAuth(providerId, AuthBody(type = "api", key = apiKey))
    }

    suspend fun getMcpStatus(): Result<Map<String, McpStatus>> = runCatching {
        api.getMcpStatus()
    }

    suspend fun listCommands(): Result<List<Command>> = runCatching { api.listCommands() }
}
