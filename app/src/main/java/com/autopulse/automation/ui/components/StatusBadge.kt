package com.autopulse.automation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autopulse.automation.data.model.LogLevel
import com.autopulse.automation.data.model.TaskStatus
import com.autopulse.automation.ui.theme.AlertAmber
import com.autopulse.automation.ui.theme.AlertRed
import com.autopulse.automation.ui.theme.Primary
import com.autopulse.automation.ui.theme.StatusFailed
import com.autopulse.automation.ui.theme.StatusRunning
import com.autopulse.automation.ui.theme.StatusStopped

@Composable
fun TaskStatusBadge(status: TaskStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status) {
        TaskStatus.RUNNING -> StatusRunning.copy(alpha = 0.15f) to StatusRunning
        TaskStatus.STOPPED -> StatusStopped.copy(alpha = 0.15f) to StatusStopped
        TaskStatus.FAILED -> StatusFailed.copy(alpha = 0.15f) to StatusFailed
        TaskStatus.COMPLETED -> Primary.copy(alpha = 0.15f) to Primary
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.name,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun LogLevelBadge(level: LogLevel, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (level) {
        LogLevel.INFO -> Primary.copy(alpha = 0.15f) to Primary
        LogLevel.SUCCESS -> StatusRunning.copy(alpha = 0.15f) to StatusRunning
        LogLevel.WARN -> AlertAmber.copy(alpha = 0.15f) to AlertAmber
        LogLevel.ERROR -> AlertRed.copy(alpha = 0.15f) to AlertRed
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = level.name,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
