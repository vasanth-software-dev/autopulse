package com.autopulse.automation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.autopulse.automation.ui.automations.AutomationsScreen
import com.autopulse.automation.ui.builder.BuilderScreen
import com.autopulse.automation.ui.dashboard.DashboardScreen
import com.autopulse.automation.ui.logs.LogsScreen
import com.autopulse.automation.ui.permissions.PermissionsScreen
import com.autopulse.automation.ui.tasks.TasksScreen
import com.autopulse.automation.ui.telegram.TelegramSettingsScreen

@Composable
fun AutoPulseNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToBuilder = { id ->
                    navController.navigate(Screen.Builder.createRoute(id))
                },
                onNavigateToTasks = {
                    navController.navigate(Screen.RunningTasks.route)
                },
                onNavigateToTelegram = {
                    navController.navigate(Screen.TelegramSettings.route)
                },
                onNavigateToPermissions = {
                    navController.navigate(Screen.Permissions.route)
                }
            )
        }

        composable(Screen.Automations.route) {
            AutomationsScreen(
                onNavigateToBuilder = { id ->
                    navController.navigate(Screen.Builder.createRoute(id))
                }
            )
        }

        composable(Screen.RunningTasks.route) {
            TasksScreen()
        }

        composable(Screen.Logs.route) {
            LogsScreen()
        }

        composable(Screen.TelegramSettings.route) {
            TelegramSettingsScreen()
        }

        composable(Screen.Permissions.route) {
            PermissionsScreen()
        }

        composable(
            route = Screen.Builder.route,
            arguments = listOf(
                navArgument("automationId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val automationId = backStackEntry.arguments?.getLong("automationId") ?: 0L
            BuilderScreen(
                automationId = automationId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
