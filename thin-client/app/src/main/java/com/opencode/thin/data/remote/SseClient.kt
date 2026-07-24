package com.opencode.thin.data.remote

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

data class SseEvent(
    val id: String?,
    val type: String?,
    val data: String,
)

class SseClient(
    private val okHttpClient: OkHttpClient,
) {
    fun events(): Flow<SseEvent> = callbackFlow {
        val config = ServerConfig
        val client = okHttpClient.newBuilder()
            .readTimeout(0, TimeUnit.SECONDS)
            .build()

        val requestBuilder = Request.Builder()
            .url("${config.baseUrl}/event")
            .header("Accept", "text/event-stream")

        if (config.password.isNotBlank()) {
            requestBuilder.header(
                "Authorization",
                Credentials.basic("opencode", config.password),
            )
        }

        val factory = EventSources.createFactory(client)
        var eventSource: EventSource? = null

        val listener = object : okhttp3.sse.EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String,
            ) {
                trySend(SseEvent(id, type, data))
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?,
            ) {
                close(t)
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        eventSource = factory.newEventSource(requestBuilder.build(), listener)

        awaitClose {
            eventSource?.cancel()
        }
    }
}
