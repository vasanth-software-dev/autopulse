package com.autopulse.automation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autopulse.automation.data.model.AutomationWithRules
import com.autopulse.automation.ui.theme.AccentGreen
import com.autopulse.automation.ui.theme.AlertRed
import com.autopulse.automation.ui.theme.BorderDark
import com.autopulse.automation.ui.theme.Primary
import com.autopulse.automation.ui.theme.SurfaceDark
import com.autopulse.automation.ui.theme.TextSecondaryDark

@Composable
fun AutomationCard(
    automationWithRules: AutomationWithRules,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val automation = automationWithRules.automation
    val triggers = automationWithRules.triggers
    val conditions = automationWithRules.conditions
    val actions = automationWithRules.actions

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .clickable { onEdit() }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = automation.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (automation.description.isNotBlank()) {
                        Text(
                            text = automation.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark,
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = automation.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trigger & Actions Summary Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Trigger summary
                val triggerApp = triggers.firstOrNull()?.appName ?: triggers.firstOrNull()?.packageName ?: "Event"
                SummaryPill(
                    icon = Icons.Default.NotificationsActive,
                    label = "When: $triggerApp",
                    tint = Primary
                )

                // Condition count or summary
                if (conditions.isNotEmpty()) {
                    val cond = conditions.first()
                    SummaryPill(
                        label = "If: \"${cond.value}\"",
                        tint = AccentGreen
                    )
                }

                // Action count
                if (actions.isNotEmpty()) {
                    SummaryPill(
                        icon = Icons.Default.Repeat,
                        label = "${actions.size} Actions",
                        tint = Color(0xFFFFB300)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Automation",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Automation",
                        tint = AlertRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryPill(
    label: String,
    tint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Box(
        modifier = Modifier
            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 11.sp,
                color = tint,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
