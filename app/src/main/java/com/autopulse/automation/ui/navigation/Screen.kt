package com.autopulse.automation.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null,
    val showInBottomNav: Boolean = false
) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard, showInBottomNav = true)
    data object Automations : Screen("automations", "Automations", Icons.Default.ListAlt, showInBottomNav = true)
    data object RunningTasks : Screen("tasks", "Tasks", Icons.Default.PlayCircle, showInBottomNav = true)
    data object Logs : Screen("logs", "Logs", Icons.Default.Terminal, showInBottomNav = true)

    // Secondary / Config screens
    data object Builder : Screen("builder?automationId={automationId}", "Rule Builder") {
        fun createRoute(automationId: Long = 0L) = "builder?automationId=$automationId"
    }
    data object TelegramSettings : Screen("telegram_settings", "Telegram Bot", Icons.Default.Send)
    data object Permissions : Screen("permissions", "Permissions", Icons.Default.Security)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(
                Dashboard,
                Automations,
                RunningTasks,
                Logs
            )
    }
}
