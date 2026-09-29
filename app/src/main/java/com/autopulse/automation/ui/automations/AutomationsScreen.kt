package com.autopulse.automation.ui.automations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autopulse.automation.data.model.AutomationWithRules
import com.autopulse.automation.ui.components.AutomationCard
import com.autopulse.automation.ui.theme.AlertRed
import com.autopulse.automation.ui.theme.Primary
import com.autopulse.automation.ui.theme.TextSecondaryDark

@Composable
fun AutomationsScreen(
    onNavigateToBuilder: (Long) -> Unit,
    viewModel: AutomationsViewModel = viewModel()
) {
    val automations by viewModel.automations.collectAsState()
    var automationToDelete by remember { mutableStateOf<AutomationWithRules?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (automations.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Rule,
                    contentDescription = null,
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Automations Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Create your first rule to trigger actions from OLX leads, notifications, and scheduled tasks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Configured Automations (${automations.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(automations, key = { it.automation.id }) { item ->
                    AutomationCard(
                        automationWithRules = item,
                        onToggleEnabled = { enabled ->
                            viewModel.toggleEnabled(item.automation.id, enabled)
                        },
                        onEdit = {
                            onNavigateToBuilder(item.automation.id)
                        },
                        onDelete = {
                            automationToDelete = item
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }

        FloatingActionButton(
            onClick = { onNavigateToBuilder(0L) },
            containerColor = Primary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Automation")
        }

        // Delete Confirmation Dialog
        automationToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { automationToDelete = null },
                title = { Text("Delete Automation?") },
                text = { Text("Are you sure you want to delete '${item.automation.name}'? This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteAutomation(item.automation.id)
                            automationToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = AlertRed)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { automationToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
