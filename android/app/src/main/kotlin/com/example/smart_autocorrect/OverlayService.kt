package com.example.smart_autocorrect

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.util.Log
import android.os.Bundle

class OverlayService : Service() {
    
    companion object {
        private const val TAG = "OverlayService"
        var instance: OverlayService? = null
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var activeEditText: AccessibilityNodeInfo? = null
    private var currentCorrections: List<CorrectionSuggestion> = emptyList()

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Log.d(TAG, "Overlay service created")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun setActiveEditText(editText: AccessibilityNodeInfo) {
        activeEditText = editText
    }

    fun showCorrections(corrections: List<CorrectionSuggestion>, source: AccessibilityNodeInfo) {
        if (!source.refresh()) {
            Log.w(TAG, "Failed to refresh source node for corrections")
        }
        if (corrections.isEmpty()) {
            hideOverlay()
            return
        }

        currentCorrections = corrections
        activeEditText = source

        runOnUiThread {
            createOverlayView(corrections)
        }
    }

    private fun runOnUiThread(action: () -> Unit) {
        // Since this is a service, we need to post to main thread
        android.os.Handler(android.os.Looper.getMainLooper()).post(action)
    }

    private fun createOverlayView(corrections: List<CorrectionSuggestion>) {
        hideOverlay() // Remove existing overlay

        val inflater = LayoutInflater.from(this)
        overlayView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(android.graphics.Color.parseColor("#E3F2FD"))
        }

        val title = TextView(this).apply {
            text = "Smart AutoCorrect"
            textSize = 14f
            setTextColor(android.graphics.Color.BLACK)
            setPadding(0, 0, 0, 8)
        }
        (overlayView as LinearLayout).addView(title)

        corrections.forEach { correction ->
            val correctionLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 4, 0, 4)
            }

            val correctionText = TextView(this).apply {
                text = "${correction.original} → ${correction.correction}"
                textSize = 12f
                setTextColor(android.graphics.Color.DKGRAY)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val applyButton = Button(this).apply {
                text = "Apply"
                textSize = 10f
                setPadding(8, 4, 8, 4)
                setOnClickListener {
                    applyCorrection(correction)
                }
            }

            correctionLayout.addView(correctionText)
            correctionLayout.addView(applyButton)
            (overlayView as LinearLayout).addView(correctionLayout)
        }

        val dismissButton = Button(this).apply {
            text = "Dismiss"
            textSize = 12f
            setOnClickListener { hideOverlay() }
        }
        (overlayView as LinearLayout).addView(dismissButton)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 50
            y = 200
        }

        try {
            windowManager?.addView(overlayView, params)
            Log.d(TAG, "Overlay shown with ${corrections.size} corrections")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
        }
    }

    private fun applyCorrection(correction: CorrectionSuggestion) {
        val editText = activeEditText ?: return

        try {
            val currentText = editText.text?.toString() ?: return
            val newText = currentText.replaceRange(
                correction.startIndex,
                correction.endIndex,
                correction.correction
            )

            // Set the corrected text
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
            editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

            Log.d(TAG, "Applied correction: ${correction.original} → ${correction.correction}")
            hideOverlay()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply correction", e)
        }
    }

    private fun hideOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
                overlayView = null
                Log.d(TAG, "Overlay hidden")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide overlay", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        hideOverlay()
        Log.d(TAG, "Overlay service destroyed")
    }
}
