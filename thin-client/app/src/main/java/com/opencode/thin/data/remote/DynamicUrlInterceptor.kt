package com.opencode.thin.data.remote

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

class DynamicUrlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val config = ServerConfig
        val original = chain.request()

        val newUrl = original.url.toString()
            .replace("http://placeholder/", "${config.baseUrl}/")

        val request = original.newBuilder().url(newUrl)

        if (config.password.isNotBlank()) {
            request.header("Authorization", Credentials.basic("opencode", config.password))
        }

        return chain.proceed(request.build())
    }
}
