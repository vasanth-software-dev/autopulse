package com.autopulse.automation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.autopulse.automation.ui.navigation.AutoPulseNavGraph
import com.autopulse.automation.ui.navigation.Screen
import com.autopulse.automation.ui.theme.Primary
import com.autopulse.automation.ui.theme.SurfaceDark
import com.autopulse.automation.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPulseApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentScreen = when {
        currentRoute == Screen.Dashboard.route -> Screen.Dashboard
        currentRoute == Screen.Automations.route -> Screen.Automations
        currentRoute == Screen.RunningTasks.route -> Screen.RunningTasks
        currentRoute == Screen.Logs.route -> Screen.Logs
        currentRoute == Screen.TelegramSettings.route -> Screen.TelegramSettings
        currentRoute == Screen.Permissions.route -> Screen.Permissions
        currentRoute?.startsWith("builder") == true -> Screen.Builder
        else -> Screen.Dashboard
    }

    val isTopLevel = Screen.bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentScreen.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (!isTopLevel) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (currentScreen != Screen.Permissions) {
                        IconButton(onClick = { navController.navigate(Screen.Permissions.route) }) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Permissions",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    if (currentScreen != Screen.TelegramSettings) {
                        IconButton(onClick = { navController.navigate(Screen.TelegramSettings.route) }) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Telegram Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = TextSecondaryDark
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            screen.icon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            indicatorColor = Primary.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        AutoPulseNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
