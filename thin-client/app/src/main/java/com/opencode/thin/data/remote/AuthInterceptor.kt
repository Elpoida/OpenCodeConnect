package com.opencode.thin.data.remote

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val passwordProvider: () -> String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val password = passwordProvider()
        val request = if (password.isNotBlank()) {
            chain.request().newBuilder()
                .header("Authorization", Credentials.basic("opencode", password))
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
