package com.example.smart_autocorrect

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.util.Log
import android.os.Bundle
import com.example.smart_autocorrect.R
import android.widget.TextView

class OverlayService : Service() {
    
    companion object {
        private const val TAG = "OverlayService"
        var instance: OverlayService? = null
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var lastOverlayX: Int = 50
    private var lastOverlayY: Int = 200
    private var activeEditText: AccessibilityNodeInfo? = null
    private var currentCorrections: List<CorrectionSuggestion> = emptyList()
    private var pendingCorrections: MutableSet<CorrectionSuggestion> = mutableSetOf()
    private var appliedCorrections: MutableSet<CorrectionSuggestion> = mutableSetOf()
    private var initialFocusText: String? = null
    private var originalSnapshot: String? = null
    private var autoAppliedMode: Boolean = false

    private var suggestionContainer: LinearLayout? = null
    private var statusText: TextView? = null
    private var applyAllButton: Button? = null
    private var revertButton: Button? = null
    private var dismissButton: Button? = null
    private var scrollView: ScrollView? = null

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

    fun setInitialText(text: String?) {
        initialFocusText = text
    }

    fun dismissOverlay() {
        runOnUiThread {
            hideOverlay()
        }
    }

    fun showCorrections(
        corrections: List<CorrectionSuggestion>,
        source: AccessibilityNodeInfo,
        autoApply: Boolean = false
    ) {
        if (!source.refresh()) {
            Log.w(TAG, "Failed to refresh source node for corrections")
        }
        currentCorrections = corrections
        activeEditText = source
        autoAppliedMode = autoApply
        originalSnapshot = source.text?.toString() ?: originalSnapshot
        if (initialFocusText == null) {
            initialFocusText = originalSnapshot
        }

        pendingCorrections = corrections.toMutableSet()
        appliedCorrections.clear()

        runOnUiThread {
            createOrUpdateOverlay()
            if (autoApply) {
                applyAllCorrections(autoTriggered = true)
            } else {
                refreshOverlayUI()
            }
        }
    }

    private fun runOnUiThread(action: () -> Unit) {
        // Since this is a service, we need to post to main thread
        android.os.Handler(android.os.Looper.getMainLooper()).post(action)
    }

    private fun createOrUpdateOverlay() {
        val inflater = LayoutInflater.from(this)

        if (overlayView == null) {
            overlayView = inflater.inflate(R.layout.overlay_corrections, null)
            suggestionContainer = overlayView?.findViewById(R.id.correction_list)
            statusText = overlayView?.findViewById(R.id.correction_status)
            applyAllButton = overlayView?.findViewById(R.id.button_apply_all)
            revertButton = overlayView?.findViewById(R.id.button_revert_all)
            dismissButton = overlayView?.findViewById(R.id.button_dismiss)
            scrollView = overlayView?.findViewById(R.id.correction_scroll)

            overlayView?.findViewById<View>(R.id.drag_handle)?.setOnTouchListener { _, event ->
                handleDrag(event)
                true
            }

            applyAllButton?.setOnClickListener {
                applyAllCorrections(autoTriggered = false)
            }

            revertButton?.setOnClickListener {
                revertAllCorrections()
            }

            dismissButton?.setOnClickListener {
                hideOverlay()
            }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

            overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = lastOverlayX
                y = lastOverlayY
        }

        try {
                windowManager?.addView(overlayView, overlayParams)
                Log.d(TAG, "Overlay created")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
                return
            }
        }
    }

    private fun applyCorrection(correction: CorrectionSuggestion) {
        val editText = activeEditText ?: return

        try {
            if (!pendingCorrections.contains(correction)) {
                Log.d(TAG, "Correction already applied: ${correction.original}")
                return
            }

            appliedCorrections.add(correction)
            pendingCorrections.remove(correction)
            val baseText = originalSnapshot ?: editText.text?.toString() ?: return
            val newText = buildCorrectedText(baseText, appliedCorrections)

            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
            editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

            Log.d(TAG, "Applied correction: ${correction.original} → ${correction.correction}")
            refreshOverlayUI()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply correction", e)
        }
    }

    private fun applyAllCorrections(autoTriggered: Boolean) {
        val editText = activeEditText ?: return
        if (pendingCorrections.isEmpty()) {
            refreshOverlayUI()
            return
        }

        try {
            appliedCorrections.addAll(pendingCorrections)
            pendingCorrections.clear()
            val baseText = originalSnapshot ?: editText.text?.toString() ?: return
            val newText = buildCorrectedText(baseText, appliedCorrections)

            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
            editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

            Log.d(TAG, "Applied ${appliedCorrections.size} corrections (${if (autoTriggered) "auto" else "manual"})")
            refreshOverlayUI()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply all corrections", e)
        }
    }

    private fun revertAllCorrections() {
        val editText = activeEditText ?: return
        val original = initialFocusText ?: originalSnapshot ?: return

        try {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, original)
            editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

            appliedCorrections.clear()
            pendingCorrections = currentCorrections.toMutableSet()
            Log.d(TAG, "Reverted to original text")
            refreshOverlayUI()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to revert", e)
        }
    }

    private fun buildCorrectedText(baseText: String, corrections: Set<CorrectionSuggestion>): String {
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

    private fun refreshOverlayUI() {
        val container = suggestionContainer ?: return
        container.removeAllViews()

        val inflater = LayoutInflater.from(this)

        currentCorrections.forEach { correction ->
            val row = inflater.inflate(R.layout.overlay_correction_item, container, false)
            val textView = row.findViewById<TextView>(R.id.correction_text)
            val applyButton = row.findViewById<Button>(R.id.button_apply_single)
            val statusChip = row.findViewById<TextView>(R.id.correction_status_chip)

            textView.text = "${correction.original} → ${correction.correction}"

            if (pendingCorrections.contains(correction)) {
                applyButton.visibility = View.VISIBLE
                statusChip.visibility = View.GONE
                applyButton.setOnClickListener { applyCorrection(correction) }
            } else {
                applyButton.visibility = View.GONE
                statusChip.visibility = View.VISIBLE
                statusChip.text = getString(R.string.overlay_chip_applied)
            }

            container.addView(row)
        }

        statusText?.text = when {
            autoAppliedMode && appliedCorrections.isNotEmpty() -> getString(R.string.overlay_status_auto_applied)
            appliedCorrections.isNotEmpty() && pendingCorrections.isEmpty() -> getString(R.string.overlay_status_all_applied)
            appliedCorrections.isEmpty() -> getString(R.string.overlay_status_suggestions)
            else -> getString(R.string.overlay_status_partial_applied)
        }

        applyAllButton?.isEnabled = pendingCorrections.isNotEmpty()
        revertButton?.isEnabled = appliedCorrections.isNotEmpty() || (initialFocusText != null && activeEditText?.text?.toString() != initialFocusText)
        dismissButton?.isEnabled = true

        overlayView?.alpha = 0.95f
    }

    private fun handleDrag(event: MotionEvent) {
        val params = overlayParams ?: return
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragTouchX = event.rawX
                dragTouchY = event.rawY
                dragStartX = params.x
                dragStartY = params.y
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = (event.rawX - dragTouchX).toInt()
                val deltaY = (event.rawY - dragTouchY).toInt()
                params.x = dragStartX + deltaX
                params.y = dragStartY + deltaY
                lastOverlayX = params.x
                lastOverlayY = params.y
                windowManager?.updateViewLayout(overlayView, params)
            }
        }
    }

    private var dragTouchX: Float = 0f
    private var dragTouchY: Float = 0f
    private var dragStartX: Int = 0
    private var dragStartY: Int = 0

    private fun hideOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
                overlayView = null
                overlayParams = null
                suggestionContainer = null
                statusText = null
                applyAllButton = null
                revertButton = null
                dismissButton = null
                scrollView = null
                originalSnapshot = null
                initialFocusText = null
                pendingCorrections.clear()
                appliedCorrections.clear()
                autoAppliedMode = false
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
