package com.example.smart_autocorrect

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Intent
import android.os.Bundle
import android.util.Log
import java.util.Locale

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
        
        // Check for misspelled words and send to overlay service
        val corrections = checkForCorrections(text)
        if (corrections.isNotEmpty()) {
            OverlayService.instance?.showCorrections(corrections, source)
        }
    }

    private fun handleViewFocused(event: AccessibilityEvent) {
        val source = event.source ?: return
        if (source.isEditable) {
            Log.d(TAG, "Editable field focused: ${source.className}")
            OverlayService.instance?.setActiveEditText(source)

            val existingText = source.text?.toString()?.trim()
            if (!existingText.isNullOrEmpty()) {
                val corrections = checkForCorrections(existingText)
                if (corrections.isNotEmpty()) {
                    OverlayService.instance?.showCorrections(corrections, source)
                } else {
                    OverlayService.instance?.showCorrections(emptyList(), source)
                }
            } else {
                OverlayService.instance?.showCorrections(emptyList(), source)
            }
        }
    }

    private fun checkForCorrections(text: String): List<CorrectionSuggestion> {
        val corrections = mutableListOf<CorrectionSuggestion>()
        val words = text.split("\\s+".toRegex())
        
        // Simple correction dictionary - in real app this would be loaded from storage
        val correctionMap = mapOf(
            "teh" to "the",
            "recieve" to "receive",
            "seperate" to "separate",
            "definately" to "definitely",
            "occured" to "occurred",
            "necesary" to "necessary",
            "accomodate" to "accommodate",
            "acheive" to "achieve",
            "beleive" to "believe",
            "wierd" to "weird"
        )
        
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
