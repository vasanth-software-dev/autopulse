package com.autopulse.automation.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autopulse.automation.ui.components.LogLevelBadge
import com.autopulse.automation.ui.components.MetricCard
import com.autopulse.automation.ui.theme.AccentGreen
import com.autopulse.automation.ui.theme.AlertAmber
import com.autopulse.automation.ui.theme.AlertRed
import com.autopulse.automation.ui.theme.BorderDark
import com.autopulse.automation.ui.theme.Primary
import com.autopulse.automation.ui.theme.SurfaceDark
import com.autopulse.automation.ui.theme.TextSecondaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToBuilder: (Long) -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToTelegram: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by uiStateFlow(viewModel)

    LaunchedEffect(Unit) {
        viewModel.refreshSystemStatus()
    }

    val scrollState = rememberScrollState()
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Readiness Status Alert Banner
        if (!uiState.isNotificationListenerEnabled || !uiState.isTelegramConfigured) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AlertAmber.copy(alpha = 0.12f))
                    .border(1.dp, AlertAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable {
                        if (!uiState.isNotificationListenerEnabled) onNavigateToPermissions()
                        else onNavigateToTelegram()
                    }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AlertAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Setup Action Required",
                            fontWeight = FontWeight.Bold,
                            color = AlertAmber,
                            fontSize = 14.sp
                        )
                        val message = when {
                            !uiState.isNotificationListenerEnabled -> "Enable Notification Access to detect leads"
                            !uiState.isTelegramConfigured -> "Configure Telegram Bot Token & Chat ID"
                            else -> "Review background permissions"
                        }
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "FIX",
                        fontWeight = FontWeight.Bold,
                        color = AlertAmber,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section Title
        Text(
            text = "Engine Overview",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Real-time automation metrics & status",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondaryDark
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2x2 Metric Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "AUTOMATIONS",
                value = "${uiState.enabledAutomations} / ${uiState.totalAutomations}",
                subtitle = "Active rules",
                icon = Icons.Default.Bolt,
                accentColor = Primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "RUNNING TASKS",
                value = "${uiState.runningTasks}",
                subtitle = "Active repeaters",
                icon = Icons.Default.PlayCircle,
                accentColor = if (uiState.runningTasks > 0) AccentGreen else TextSecondaryDark,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTasks() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // System Health Row
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "System Services",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ServiceStatusItem(
                        title = "Notification Service",
                        isActive = uiState.isNotificationListenerEnabled,
                        onClick = onNavigateToPermissions
                    )
                    ServiceStatusItem(
                        title = "Telegram Bot",
                        isActive = uiState.isTelegramConfigured,
                        onClick = onNavigateToTelegram
                    )
                    ServiceStatusItem(
                        title = "Battery Unrestricted",
                        isActive = uiState.isBatteryOptimizationIgnored,
                        onClick = onNavigateToPermissions
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Controls
        Text(
            text = "Quick Controls",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { onNavigateToBuilder(0L) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Rule", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onNavigateToTelegram,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Telegram", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Simulator Station Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF131D31))
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("OLX Alert Simulator", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Simulate 'You have new messages' to test live repeaters", fontSize = 11.sp, color = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { viewModel.simulateOlxLead() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF233049)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFB300)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger Test OLX Alert", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Last Activity Preview
        Text(
            text = "Latest Activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        val lastLog = uiState.lastExecutionLog
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            if (lastLog != null) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LogLevelBadge(level = lastLog.level)
                        Text(
                            text = timeFormat.format(Date(lastLog.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = lastLog.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (lastLog.automationName != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rule: ${lastLog.automationName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary
                        )
                    }
                }
            } else {
                Text(
                    text = "No recent execution activity. Ready to monitor events.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark
                )
            }
        }
    }
}

@Composable
private fun uiStateFlow(viewModel: DashboardViewModel) = viewModel.uiState.collectAsState()

@Composable
private fun ServiceStatusItem(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isActive) AccentGreen else AlertAmber,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            color = TextSecondaryDark
        )
        Text(
            text = if (isActive) "ACTIVE" else "CHECK",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) AccentGreen else AlertAmber
        )
    }
}
