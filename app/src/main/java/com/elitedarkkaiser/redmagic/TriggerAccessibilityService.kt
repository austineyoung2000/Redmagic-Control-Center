package com.elitedarkkaiser.redmagic

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class TriggerAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg.isBlank() || pkg == packageName || pkg == "com.android.systemui") return

        val isTrackedGame = getSavedGamePackagesStorage(this).contains(pkg)
        val gameModeActive = isGameModeLedOverrideActiveStorage(this)

        // Start GameModeService when entering a tracked game, or send one
        // final foreground event while Game Mode is active so it can restore
        // the normal profile immediately after leaving the game.
        if (!isTrackedGame && !gameModeActive) return

        startService(Intent(this, GameModeService::class.java).apply {
            putExtra("foreground_pkg", pkg)
        })
    }
    override fun onInterrupt() = Unit

    override fun onServiceConnected() {
        super.onServiceConnected()
        HardwareController.enableTriggers()
    }

    private fun prefs() = getSharedPreferences("triggers", Context.MODE_PRIVATE)

    private fun getAction(key: String): String {
        return prefs().getString(key, "NONE") ?: "NONE"
    }

    private fun runRoot(cmd: String) {
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        } catch (_: Throwable) {
        }
    }

    private fun performAction(action: String) {
        when (action) {
            "VOL_UP" -> runRoot("input keyevent 24")
            "VOL_DOWN" -> runRoot("input keyevent 25")
            "MEDIA_PLAY_PAUSE" -> runRoot("input keyevent 85")
            "MEDIA_NEXT" -> runRoot("input keyevent 87")
            "MEDIA_PREVIOUS" -> runRoot("input keyevent 88")
            "NONE" -> Unit
            else -> Unit
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        return when (event.keyCode) {
            KeyEvent.KEYCODE_F7 -> {
                performAction(getAction("left_trigger"))
                true
            }

            KeyEvent.KEYCODE_F8 -> {
                performAction(getAction("right_trigger"))
                true
            }

            else -> false
        }
    }
}
