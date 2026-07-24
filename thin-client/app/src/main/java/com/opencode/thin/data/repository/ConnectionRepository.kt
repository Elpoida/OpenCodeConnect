package com.opencode.thin.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.opencode.thin.data.model.ConnectionConfig
import com.opencode.thin.data.model.HealthResponse
import com.opencode.thin.data.remote.OpencodeApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_BASE_URL = stringPreferencesKey("server_base_url")
        private val KEY_PASSWORD = stringPreferencesKey("server_password")
    }

    val connectionConfig: Flow<ConnectionConfig> = dataStore.data.map { prefs ->
        ConnectionConfig(
            baseUrl = prefs[KEY_BASE_URL] ?: "http://192.168.1.100:4096",
            password = prefs[KEY_PASSWORD] ?: "",
        )
    }

    suspend fun saveConnection(config: ConnectionConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = config.baseUrl
            prefs[KEY_PASSWORD] = config.password
        }
    }

    suspend fun getConnectionConfig(): ConnectionConfig = connectionConfig.first()
}
