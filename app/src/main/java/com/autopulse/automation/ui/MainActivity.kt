package com.autopulse.automation.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autopulse.automation.error.AutoPulseCrashHandler
import com.autopulse.automation.ui.theme.AutoPulseTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
        } catch (t: Throwable) {
            android.util.Log.w("MainActivity", "Edge-to-edge initialization warning", t)
        }

        val previousCrash = AutoPulseCrashHandler.getLastCrash(this)

        setContent {
            AutoPulseTheme {
                var crashInfoToDisplay by remember { mutableStateOf(previousCrash) }

                Box(modifier = Modifier.fillMaxSize()) {
                    AutoPulseApp()

                    if (crashInfoToDisplay != null) {
                        AlertDialog(
                            onDismissRequest = {
                                AutoPulseCrashHandler.clearLastCrash(this@MainActivity)
                                crashInfoToDisplay = null
                            },
                            title = {
                                Text(
                                    text = "Diagnostic Report",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252)
                                )
                            },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "AutoPulse recorded an issue on previous run:",
                                        fontSize = 13.sp,
                                        color = Color.LightGray
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = crashInfoToDisplay.orEmpty(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        modifier = Modifier
                                            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Crash Log", crashInfoToDisplay))
                                        Toast.makeText(this@MainActivity, "Crash details copied to clipboard", Toast.LENGTH_SHORT).show()
                                        AutoPulseCrashHandler.clearLastCrash(this@MainActivity)
                                        crashInfoToDisplay = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                                ) {
                                    Text("Copy & Dismiss", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                OutlinedButton(
                                    onClick = {
                                        AutoPulseCrashHandler.clearLastCrash(this@MainActivity)
                                        crashInfoToDisplay = null
                                    }
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
