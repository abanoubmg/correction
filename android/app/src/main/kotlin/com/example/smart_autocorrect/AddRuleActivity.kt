package com.example.smart_autocorrect

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat

class AddRuleActivity : Activity() {

    companion object {
        private const val TAG = "AddRuleActivity"
        const val EXTRA_SELECTED_TEXT = "selected_text"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedText = when {
            intent?.hasExtra(Intent.EXTRA_PROCESS_TEXT) == true ->
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            intent?.hasExtra(Intent.EXTRA_TEXT) == true ->
                intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            else -> null
        }

        val prefs = getSharedPreferences("smart_autocorrect", MODE_PRIVATE)
        if (!selectedText.isNullOrBlank()) {
            prefs.edit().putString("pending_misspelled", selectedText).apply()
        }

        // Launch main activity (Flutter) to open rules screen
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_SELECTED_TEXT, selectedText)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        try {
            startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch MainActivity", e)
        }

        setResult(Activity.RESULT_OK)
        finish()
    }
}

