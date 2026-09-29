package com.autopulse.automation.telegram

import com.autopulse.automation.event.NotificationEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramMessageFormatterTest {

    @Test
    fun testHtmlEscaping() {
        val raw = "Buyer: John <deal maker> & 'expert' with 10% \"discount\""
        val escaped = TelegramMessageFormatter.escapeHtml(raw)

        assertEquals("Buyer: John &lt;deal maker&gt; &amp; 'expert' with 10% &quot;discount&quot;", escaped)
        assertFalse(escaped.contains("<deal maker>"))
        assertTrue(escaped.contains("&lt;deal maker&gt;"))
    }

    @Test
    fun testDefaultLeadFormattingWithVariables() {
        val event = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX",
            title = "Inquiry: Sony PS5 & Games",
            text = "Is price < 40000 negotiable?",
            timestamp = 1717200000000L
        )

        val formattedHtml = TelegramMessageFormatter.formatHtmlMessage(null, event)

        // Verifies bold tags are intact
        assertTrue(formattedHtml.contains("<b>NEW OLX LEAD</b>"))
        assertTrue(formattedHtml.contains("<b>Title:</b> Inquiry: Sony PS5 &amp; Games"))
        assertTrue(formattedHtml.contains("<b>Message:</b> Is price &lt; 40000 negotiable?"))
        // Verifies raw unescaped characters are not present
        assertFalse(formattedHtml.contains("< 40000"))
        assertFalse(formattedHtml.contains("PS5 & Games"))
    }

    @Test
    fun testCustomTemplateInterpolation() {
        val customTemplate = "🚨 ALERT from {{app_name}}: {{notification_title}} - {{notification_text}}"
        val event = NotificationEvent(
            packageName = "com.olx.southasia",
            appName = "OLX India",
            title = "Direct Lead",
            text = "Call me back"
        )

        val result = TelegramMessageFormatter.formatHtmlMessage(customTemplate, event)
        assertEquals("🚨 ALERT from OLX India: Direct Lead - Call me back", result)
    }
}
