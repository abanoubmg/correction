package com.example.smart_autocorrect

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Intent
import android.os.Bundle
import android.util.Log
import java.util.Locale
import org.json.JSONArray

class AutoCorrectAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "AutoCorrectService"
        var instance: AutoCorrectAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility service connected")
        
        // Start overlay service
        val overlayIntent = Intent(this, OverlayService::class.java)
        startService(overlayIntent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                handleTextChanged(event)
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                handleViewFocused(event)
            }
        }
    }

    private fun handleTextChanged(event: AccessibilityEvent) {
        val source = event.source ?: return
        val text = source.text?.toString() ?: return
        
        Log.d(TAG, "Text changed: $text")
        
        val corrections = checkForCorrections(text)
        if (corrections.isEmpty()) {
            OverlayService.instance?.dismissOverlay()
            return
        }

        val autoApply = autoCorrectEnabled()
        val overlayOn = overlayEnabled()
        val focusEnabled = showOnFocusEnabled()

        if (autoApply) {
            applyCorrectionsDirect(source, corrections)
            OverlayService.instance?.dismissOverlay()
            return
        }

        if (!overlayOn || !focusEnabled) {
            OverlayService.instance?.dismissOverlay()
            return
        }

        OverlayService.instance?.apply {
            setActiveEditText(source)
            showCorrections(corrections, source)
        }
    }

    private fun handleViewFocused(event: AccessibilityEvent) {
        val source = event.source ?: return
        if (source.isEditable) {
            Log.d(TAG, "Editable field focused: ${source.className}")
            val overlayOn = overlayEnabled()
            val autoApply = autoCorrectEnabled()
            val focusEnabled = showOnFocusEnabled()

            if (!focusEnabled) {
                if (!overlayOn) {
                    OverlayService.instance?.dismissOverlay()
                }
                return
            }

            val existingText = source.text?.toString()

            if (existingText.isNullOrEmpty()) {
                if (!overlayOn || autoApply) {
                    OverlayService.instance?.dismissOverlay()
                } else {
                    OverlayService.instance?.apply {
                        setActiveEditText(source)
                        setInitialText(existingText)
                        showCorrections(emptyList(), source)
                    }
                }
                return
            }

            val corrections = checkForCorrections(existingText)
            if (corrections.isEmpty()) {
                OverlayService.instance?.dismissOverlay()
                return
            }

            if (autoApply) {
                applyCorrectionsDirect(source, corrections)
                OverlayService.instance?.dismissOverlay()
                return
            }

            if (!overlayOn) {
                OverlayService.instance?.dismissOverlay()
                return
            }

            OverlayService.instance?.apply {
                setActiveEditText(source)
                setInitialText(existingText)
                showCorrections(corrections, source)
            }
        }
    }

    private fun checkForCorrections(text: String): List<CorrectionSuggestion> {
        val corrections = mutableListOf<CorrectionSuggestion>()
        val words = text.split("\\s+".toRegex())
        val correctionMap = getCorrectionMap()
        
        var startIndex = 0
        for (word in words) {
            val cleanWord = word
                .lowercase(Locale.getDefault())
                .replace(Regex("\\P{L}+"), "")
            val correction = correctionMap[cleanWord]
            
            if (correction != null) {
                val wordStart = text.indexOf(word, startIndex)
                corrections.add(
                    CorrectionSuggestion(
                        original = word,
                        correction = correction,
                        startIndex = wordStart,
                        endIndex = wordStart + word.length
                    )
                )
            }
            startIndex = text.indexOf(word, startIndex) + word.length
        }
        
        return corrections
    }

    private fun getCorrectionMap(): Map<String, String> {
        val locale = Locale.getDefault()
        val map = mutableMapOf(
            "teh" to "the",
            "recieve" to "receive",
            "seperate" to "separate",
            "definately" to "definitely",
            "occured" to "occurred",
            "necesary" to "necessary",
            "accomodate" to "accommodate",
            "acheive" to "achieve",
            "beleive" to "believe",
            "wierd" to "weird",
            "thier" to "their",
            "youre" to "you're",
            "its" to "it's",
            "dont" to "don't",
            "cant" to "can't"
        )

        try {
            val prefs = getSharedPreferences("smart_autocorrect", MODE_PRIVATE)
            val rulesJson = prefs.getString("correction_rules", null)
            if (!rulesJson.isNullOrBlank()) {
                val array = JSONArray(rulesJson)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val isActive = obj.optBoolean("isActive", true)
                    val misspelled = obj.optString("misspelled", "")
                    val correction = obj.optString("correction", "")
                    if (isActive && misspelled.isNotBlank() && correction.isNotBlank()) {
                        val key = misspelled.lowercase(locale).replace(Regex("\\P{L}+"), "")
                        if (key.isNotBlank()) {
                            map[key] = correction
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load custom correction rules", e)
        }

        return map
    }

    private fun overlayEnabled(): Boolean {
        return try {
            val prefs = getSharedPreferences("smart_autocorrect", MODE_PRIVATE)
            prefs.getBoolean("overlay_enabled", true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read overlay preference", e)
            true
        }
    }

    private fun showOnFocusEnabled(): Boolean {
        return try {
            val prefs = getSharedPreferences("smart_autocorrect", MODE_PRIVATE)
            prefs.getBoolean("show_on_focus", true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read focus preference", e)
            true
        }
    }

    private fun autoCorrectEnabled(): Boolean {
        return try {
            val prefs = getSharedPreferences("smart_autocorrect", MODE_PRIVATE)
            prefs.getBoolean("auto_correct_enabled", false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read auto correct preference", e)
            false
        }
    }

    private fun applyCorrectionsDirect(source: AccessibilityNodeInfo, corrections: List<CorrectionSuggestion>) {
        val baseText = source.text?.toString() ?: return
        val newText = buildCorrectedText(baseText, corrections)
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
        val success = source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        if (!success) {
            Log.w(TAG, "Failed to apply corrections directly")
        }
    }

    private fun buildCorrectedText(baseText: String, corrections: List<CorrectionSuggestion>): String {
        if (corrections.isEmpty()) return baseText
        val sorted = corrections.sortedBy { it.startIndex }
        val builder = StringBuilder()
        var lastIndex = 0
        for (correction in sorted) {
            val start = correction.startIndex.coerceIn(0, baseText.length)
            val end = correction.endIndex.coerceIn(start, baseText.length)
            if (start >= lastIndex) {
                builder.append(baseText.substring(lastIndex, start))
                builder.append(correction.correction)
                lastIndex = end
            }
        }
        if (lastIndex < baseText.length) {
            builder.append(baseText.substring(lastIndex))
        }
        return builder.toString()
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        
        // Stop overlay service
        val overlayIntent = Intent(this, OverlayService::class.java)
        stopService(overlayIntent)
    }
}

data class CorrectionSuggestion(
    val original: String,
    val correction: String,
    val startIndex: Int,
    val endIndex: Int
)
