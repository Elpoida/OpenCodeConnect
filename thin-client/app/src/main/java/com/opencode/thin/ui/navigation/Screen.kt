package com.opencode.thin.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String, val label: String) {
    data object Connect : Screen("connect", "Connect")
    data object Chat : Screen("chat/{sessionId}", "Chat") {
        fun createRoute(sessionId: String) = "chat/$sessionId"
    }
    data object Sessions : Screen("sessions", "Sessions")
    data object Files : Screen("files", "Files")
    data object FileContent : Screen("file_content/{path}", "File") {
        fun createRoute(path: String): String {
            val encoded = URLEncoder.encode(path, "UTF-8")
            return "file_content/$encoded"
        }
    }
    data object Shell : Screen("shell/{sessionId}", "Shell") {
        fun createRoute(sessionId: String) = "shell/$sessionId"
    }
    data object Providers : Screen("providers", "Providers")
    data object Git : Screen("git/{sessionId}", "Git") {
        fun createRoute(sessionId: String) = "git/$sessionId"
    }
    data object Settings : Screen("settings", "Settings")
}
