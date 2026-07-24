package com.opencode.thin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import java.net.URLDecoder
import com.opencode.thin.ui.screens.chat.ChatScreen
import com.opencode.thin.ui.screens.connect.ConnectScreen
import com.opencode.thin.ui.screens.files.FileBrowserScreen
import com.opencode.thin.ui.screens.files.FileContentScreen
import com.opencode.thin.ui.screens.git.GitDiffScreen
import com.opencode.thin.ui.screens.providers.ProviderSelectorScreen
import com.opencode.thin.ui.screens.sessions.SessionListScreen
import com.opencode.thin.ui.screens.shell.ShellScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Connect.route) {

        composable(Screen.Connect.route) {
            ConnectScreen(
                onConnected = {
                    navController.navigate(Screen.Sessions.route) {
                        popUpTo(Screen.Connect.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Sessions.route) {
            SessionListScreen(
                onSessionClick = { sessionId ->
                    navController.navigate(Screen.Chat.createRoute(sessionId))
                },
                onCreateSession = { sessionId ->
                    navController.navigate(Screen.Chat.createRoute(sessionId))
                },
                onFilesClick = {
                    navController.navigate(Screen.Files.route)
                },
                onProvidersClick = {
                    navController.navigate(Screen.Providers.route)
                },
                onDisconnect = {
                    navController.navigate(Screen.Connect.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            ChatScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
                onShell = { navController.navigate(Screen.Shell.createRoute(sessionId)) },
                onGit = { navController.navigate(Screen.Git.createRoute(sessionId)) },
            )
        }

        composable(Screen.Files.route) {
            FileBrowserScreen(
                onBack = { navController.popBackStack() },
                onFileClick = { path ->
                    navController.navigate(Screen.FileContent.createRoute(path))
                },
            )
        }

    composable(
        route = Screen.FileContent.route,
        arguments = listOf(navArgument("path") { type = NavType.StringType }),
    ) { backStackEntry ->
        val raw = backStackEntry.arguments?.getString("path") ?: ""
        val path = URLDecoder.decode(raw, "UTF-8")
        FileContentScreen(
                path = path,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Shell.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            ShellScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Providers.route) {
            ProviderSelectorScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Git.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            GitDiffScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
