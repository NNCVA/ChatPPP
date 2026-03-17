package com.chatppp.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chatppp.app.ui.chat.ChatRoute
import com.chatppp.app.ui.conversations.ConversationListRoute
import com.chatppp.app.ui.settings.SettingsRoute

@Composable
fun ChatPppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = ChatPppDestination.Chat.route
    ) {
        composable(ChatPppDestination.Chat.route) {
            ChatRoute(
                onOpenConversations = { navController.navigate(ChatPppDestination.Conversations.route) },
                onOpenSettings = { navController.navigate(ChatPppDestination.Settings.route) }
            )
        }

        composable(ChatPppDestination.Conversations.route) {
            ConversationListRoute(
                onBack = { navController.popBackStack() },
                onConversationClick = { navController.popBackStack() }
            )
        }

        composable(ChatPppDestination.Settings.route) {
            SettingsRoute(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private enum class ChatPppDestination(val route: String) {
    Chat("chat"),
    Conversations("conversations"),
    Settings("settings")
}
