package com.autopulse.automation.ui.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autopulse.automation.data.model.CollisionStrategy
import com.autopulse.automation.data.model.ConditionType
import com.autopulse.automation.data.model.FieldToMatch
import com.autopulse.automation.data.model.MatchType
import com.autopulse.automation.ui.theme.AccentGreen
import com.autopulse.automation.ui.theme.AlertAmber
import com.autopulse.automation.ui.theme.BorderDark
import com.autopulse.automation.ui.theme.Primary
import com.autopulse.automation.ui.theme.SurfaceDark
import com.autopulse.automation.ui.theme.SurfaceVariantDark
import com.autopulse.automation.ui.theme.TextSecondaryDark

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuilderScreen(
    automationId: Long = 0L,
    onNavigateBack: () -> Unit,
    viewModel: BuilderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(automationId) {
        viewModel.loadAutomation(automationId)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Title
        Text(
            text = if (automationId == 0L) "Create Automation" else "Edit Automation",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // CARD 1: AUTOMATION INFO
        SectionCard(title = "Rule Identity", icon = Icons.Default.Apps) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Automation Name") },
                placeholder = { Text("e.g. OLX Lead Alert") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Collision Strategy (when 2nd lead arrives while repeating):", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)
            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(CollisionStrategy.IGNORE, CollisionStrategy.RESTART, CollisionStrategy.UPDATE_PAYLOAD).forEach { strat ->
                    val isSelected = uiState.collisionStrategy == strat
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.updateCollisionStrategy(strat) },
                        label = { Text(strat.name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // CARD 2: TRIGGER
        SectionCard(title = "TRIGGER", icon = Icons.Default.NotificationsActive, accentColor = Primary) {
            Text("WHEN: Notification Received", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Target Application:", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)
            Spacer(modifier = Modifier.height(6.dp))

            // Quick App Presets
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.selectedAppName == "OLX",
                    onClick = { viewModel.updateApp("com.olx.southasia", "OLX") },
                    label = { Text("OLX") }
                )
                FilterChip(
                    selected = uiState.selectedAppName.isBlank() && uiState.selectedPackageName.isBlank(),
                    onClick = { viewModel.updateApp("", "") },
                    label = { Text("Any App") }
                )
                FilterChip(
                    selected = uiState.selectedAppName == "WhatsApp",
                    onClick = { viewModel.updateApp("com.whatsapp", "WhatsApp") },
                    label = { Text("WhatsApp") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // App Package Name Input
            OutlinedTextField(
                value = uiState.selectedPackageName,
                onValueChange = { viewModel.updateApp(it, uiState.selectedAppName) },
                label = { Text("Package Name (blank for any app)") },
                placeholder = { Text("e.g. com.olx.southasia") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Match Type
            Text("App Match Mode:", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(MatchType.CONTAINS, MatchType.EXACT, MatchType.REGEX).forEach { mode ->
                    FilterChip(
                        selected = uiState.matchType == mode,
                        onClick = { viewModel.updateMatchType(mode) },
                        label = { Text(mode.name, fontSize = 11.sp) }
                    )
                }
            }
        }

        // CARD 3: CONDITION
        SectionCard(title = "CONDITION (OPTIONAL)", icon = Icons.Default.FilterAlt, accentColor = AccentGreen) {
            Text("Match Field:", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(FieldToMatch.ANY, FieldToMatch.NOTIFICATION_TITLE, FieldToMatch.NOTIFICATION_TEXT).forEach { field ->
                    FilterChip(
                        selected = uiState.conditionField == field,
                        onClick = { viewModel.updateConditionField(field) },
                        label = { Text(field.name.replace("NOTIFICATION_", ""), fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Condition Operator:", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(ConditionType.TEXT_CONTAINS, ConditionType.EXACT_MATCH, ConditionType.REGEX_MATCH).forEach { type ->
                    FilterChip(
                        selected = uiState.conditionType == type,
                        onClick = { viewModel.updateConditionType(type) },
                        label = { Text(type.name.replace("_", " "), fontSize = 10.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = uiState.conditionValue,
                onValueChange = { viewModel.updateConditionValue(it) },
                label = { Text("Filter Text") },
                placeholder = { Text("e.g. lead, inquiry, urgent") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Invert Condition (Does NOT match)", fontSize = 13.sp)
                Switch(
                    checked = uiState.isConditionNegated,
                    onCheckedChange = { viewModel.toggleConditionNegated() },
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                )
            }
        }

        // CARD 4: ACTIONS
        SectionCard(title = "ACTIONS", icon = Icons.Default.Repeat, accentColor = Color(0xFFFFB300)) {
            // Action 1: Repeat Task
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariantDark, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Repeat, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Action 1: Start Repeating Alert", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Box(
                            modifier = Modifier
                                .background(AccentGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Until STOP", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Alert Repeat Frequency", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Stepper + Text display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val current = uiState.intervalSeconds
                                val next = (current - 1).coerceAtLeast(1L)
                                viewModel.updateIntervalSeconds(next)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(SurfaceDark, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease interval", tint = Color.White)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .background(SurfaceDark, RoundedCornerShape(8.dp))
                                .border(1.dp, Primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${uiState.intervalSeconds}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "seconds",
                                    fontSize = 13.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val current = uiState.intervalSeconds
                                viewModel.updateIntervalSeconds(current + 1)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(SurfaceDark, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase interval", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Preset Chips (5s, 10s, 15s, 30s, 60s)
                    Text("Quick Presets:", fontSize = 11.sp, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5L, 10L, 15L, 30L, 60L).forEach { sec ->
                            val isSelected = uiState.intervalSeconds == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Primary else SurfaceDark)
                                    .border(1.dp, if (isSelected) Primary else BorderDark, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateIntervalSeconds(sec) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${sec}s",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TextSecondaryDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "⚡ Real-time alerts repeat every ${uiState.intervalSeconds}s until you tap STOP in Telegram or the App.",
                        color = Color(0xFFFFB300),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action 2: Send Telegram
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariantDark, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Action 2: Send Telegram Message", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Message Template:", style = MaterialTheme.typography.labelMedium, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = uiState.messageTemplate,
                        onValueChange = { viewModel.updateMessageTemplate(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Quick Insert Dynamic Variables:", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("app_name", "notification_title", "notification_text", "timestamp", "time").forEach { v ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Primary.copy(alpha = 0.2f))
                                    .clickable { viewModel.insertVariable(v) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("{{$v}}", color = Primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Error message if any
        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // SAVE BUTTON
        Button(
            onClick = {
                viewModel.saveAutomation {
                    onNavigateBack()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAVE AUTOMATION", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color = Primary,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
