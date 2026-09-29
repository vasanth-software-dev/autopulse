package com.autopulse.automation.error

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.autopulse.automation.ui.MainActivity

class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashDetails = intent.getStringExtra(EXTRA_CRASH_INFO)
            ?: AutoPulseCrashHandler.getLastCrash(this)
            ?: "No error details available."

        // Root container (Dark background, full screen)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(dpToPx(20), dpToPx(36), dpToPx(20), dpToPx(24))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Header Title
        val titleView = TextView(this).apply {
            text = "AutoPulse Diagnostic Report"
            setTextColor(Color.parseColor("#FF5252"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
        }
        rootLayout.addView(titleView)

        // Subtitle explanation
        val subtitleView = TextView(this).apply {
            text = "The application encountered an unexpected issue and safely captured the diagnostics instead of abruptly auto-closing."
            setTextColor(Color.parseColor("#B0BEC5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dpToPx(8), 0, dpToPx(16))
        }
        rootLayout.addView(subtitleView)

        // Scrollable Stack Trace Box
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
        }

        val logView = TextView(this).apply {
            text = crashDetails
            setTextColor(Color.parseColor("#ECEFF1"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        scrollView.addView(logView)
        rootLayout.addView(scrollView)

        // Button Container
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dpToPx(16), 0, 0)
        }

        // Copy Details Button
        val copyButton = Button(this).apply {
            text = "Copy Error Details"
            setBackgroundColor(Color.parseColor("#00C853"))
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("AutoPulse Diagnostic", crashDetails))
                Toast.makeText(this@CrashActivity, "Diagnostic copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }
        buttonLayout.addView(copyButton)

        // Spacer
        buttonLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(8))
        })

        // Restart Application Button
        val restartButton = Button(this).apply {
            text = "Restart AutoPulse"
            setBackgroundColor(Color.parseColor("#2979FF"))
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                AutoPulseCrashHandler.clearLastCrash(this@CrashActivity)
                val restartIntent = Intent(this@CrashActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(restartIntent)
                finish()
            }
        }
        buttonLayout.addView(restartButton)

        rootLayout.addView(buttonLayout)
        setContentView(rootLayout)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val EXTRA_CRASH_INFO = "extra_crash_info"
    }
}
