package com.autopulse.automation.ui.telegram

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autopulse.automation.ui.theme.AccentGreen
import com.autopulse.automation.ui.theme.AlertRed
import com.autopulse.automation.ui.theme.BorderDark
import com.autopulse.automation.ui.theme.Primary
import com.autopulse.automation.ui.theme.SurfaceDark
import com.autopulse.automation.ui.theme.TextSecondaryDark

@Composable
fun TelegramSettingsScreen(
    viewModel: TelegramSettingsViewModel = viewModel()
) {
    val initialToken by viewModel.botToken.collectAsState()
    val initialChatId by viewModel.chatId.collectAsState()
    val testState by viewModel.testState.collectAsState()

    var token by remember(initialToken) { mutableStateOf(initialToken) }
    var chatId by remember(initialChatId) { mutableStateOf(initialChatId) }
    var isTokenVisible by remember { mutableStateOf(false) }
    var saveSuccessNotice by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Telegram Bot Configuration",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Phone 1 will dispatch notifications directly to your Telegram chat or channel on Phone 2.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondaryDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Info / Instruction Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Quick Setup Guide",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1. Open @BotFather on Telegram and send /newbot to create your bot.\n" +
                                "2. Copy the HTTP API token into the field below.\n" +
                                "3. Send a message to your bot or @userinfobot to find your Chat ID.\n" +
                                "4. Click 'Test Connection' and 'Send Test' to verify delivery.",
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Bot Token Input
        OutlinedTextField(
            value = token,
            onValueChange = {
                token = it
                saveSuccessNotice = false
                viewModel.resetState()
            },
            label = { Text("Telegram Bot Token") },
            placeholder = { Text("e.g. 7123456789:AAHfk3x-...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                    Icon(
                        imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle token visibility",
                        tint = TextSecondaryDark
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = BorderDark,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Chat ID Input
        OutlinedTextField(
            value = chatId,
            onValueChange = {
                chatId = it
                saveSuccessNotice = false
                viewModel.resetState()
            },
            label = { Text("Telegram Chat ID") },
            placeholder = { Text("e.g. 123456789 or -100123456789") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = BorderDark,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Save Settings Button
        Button(
            onClick = {
                viewModel.save(token, chatId)
                saveSuccessNotice = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Credentials Securely", fontWeight = FontWeight.Bold)
        }

        if (saveSuccessNotice) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "✓ Credentials securely saved in Encrypted SharedPreferences.",
                color = AccentGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Connectivity Diagnostics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.testConnection(token) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
                enabled = testState !is TelegramTestState.Loading
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Connection", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { viewModel.sendTestMessage(token, chatId) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
                enabled = testState !is TelegramTestState.Loading
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send Test", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diagnostic Result Box
        when (val state = testState) {
            is TelegramTestState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Contacting Telegram API...", color = TextSecondaryDark, fontSize = 13.sp)
                    }
                }
            }
            is TelegramTestState.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentGreen.copy(alpha = 0.12f))
                        .border(1.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = state.message, color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            is TelegramTestState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AlertRed.copy(alpha = 0.12f))
                        .border(1.dp, AlertRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = AlertRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = state.error, color = AlertRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            is TelegramTestState.Idle -> {
                // Nothing to show
            }
        }
    }
}
