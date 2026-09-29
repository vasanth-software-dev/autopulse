package com.autopulse.automation.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autopulse.automation.ui.theme.AccentGreen
import com.autopulse.automation.ui.theme.BorderDark
import com.autopulse.automation.ui.theme.Primary
import com.autopulse.automation.ui.theme.SurfaceDark
import com.autopulse.automation.ui.theme.TextSecondaryDark

@Composable
fun PermissionsScreen(
    viewModel: PermissionsViewModel = viewModel()
) {
    val permissions by viewModel.permissions.collectAsState()
    val diagnosticMessage by viewModel.diagnosticMessage.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh permissions state whenever user returns to this screen from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Required Android Permissions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "AutoPulse requires these permissions to detect incoming notifications, sustain 30-second repeating tasks, and dispatch Telegram messages.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark
            )
        }

        // Diagnostic Audit Button
        OutlinedButton(
            onClick = { viewModel.runDiagnosticAudit() },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.HealthAndSafety, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Run System Health Audit", fontWeight = FontWeight.SemiBold)
        }

        if (diagnosticMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = diagnosticMessage.orEmpty(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
            }
        }

        // 1. Notification Access (Listener Service)
        PermissionItemCard(
            title = "Notification Access",
            description = "Allows AutoPulse to listen for OLX buyer leads and extract notification titles and messages without continuous polling.",
            icon = Icons.Default.NotificationsActive,
            isGranted = permissions.isNotificationListenerGranted,
            onAction = { viewModel.openNotificationListenerSettings(context) }
        )

        // 2. Post Notifications
        PermissionItemCard(
            title = "Post Notifications",
            description = "Required to show the persistent foreground notification while a 30-second repeating task is running, providing a one-click STOP button.",
            icon = Icons.Default.Notifications,
            isGranted = permissions.isPostNotificationsGranted,
            onAction = { viewModel.openAppNotificationSettings(context) }
        )

        // 3. Battery Optimization
        PermissionItemCard(
            title = "Battery Optimization",
            description = "Exempts AutoPulse from Android Doze mode so 30-second repeat cycles run reliably without system throttling.",
            icon = Icons.Default.BatteryAlert,
            isGranted = permissions.isBatteryOptimizationIgnored,
            onAction = { viewModel.openBatteryOptimizationSettings(context) }
        )

        // 4. Exact Alarm Scheduling
        PermissionItemCard(
            title = "Exact Alarms",
            description = "Allows the system to wake the device at exact 30-second intervals if the screen is turned off.",
            icon = Icons.Default.Alarm,
            isGranted = permissions.isExactAlarmAllowed,
            onAction = { viewModel.openExactAlarmSettings(context) }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(
                1.dp,
                if (isGranted) AccentGreen.copy(alpha = 0.3f) else BorderDark,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isGranted) AccentGreen.copy(alpha = 0.15f) else Primary.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isGranted) AccentGreen else Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isGranted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ACTIVE",
                            color = AccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ENABLE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark,
                lineHeight = 18.sp
            )
        }
    }
}
