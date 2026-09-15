package com.elitedarkkaiser.redmagic

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder

class GameModeService : Service() {

    companion object {
        const val EXTRA_APPLY_SAVED_PROFILE =
            "apply_saved_game_mode_profile"
        const val EXTRA_CONTINUE_AFTER_APPLY =
            "continue_game_mode_after_profile_apply"
    }

    private lateinit var workerThread: HandlerThread
    private lateinit var handler: Handler

    private var gameModeActiveFor: String? = null
    private var gameModeApplyPendingFor: String? = null
    private var pollingPausedForScreenOff = false

    private val activeGamePollMs = 120_000L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            handler.post {
                when (action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        pollingPausedForScreenOff = true
                        handler.removeCallbacks(pollRunnable)

                        if (gameModeActiveFor != null) {
                            restoreNormalProfile()
                            setGameModeLedOverrideActiveStorage(
                                this@GameModeService,
                                false
                            )
                            gameModeActiveFor = null
                        }

                        android.util.Log.i(
                            "RedmagicGameMode",
                            "screen off: paused game mode polling"
                        )
                        stopSelf()
                    }

                    Intent.ACTION_SCREEN_ON,
                    Intent.ACTION_USER_PRESENT -> {
                        pollingPausedForScreenOff = false
                        android.util.Log.i(
                            "RedmagicGameMode",
                            "screen on/unlock: waiting for foreground app event"
                        )
                    }
                }
            }
        }
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            try {
                val currentPkg = getForegroundPackageName()
                val tracked = getSavedGamePackagesStorage(this@GameModeService)

                if (!currentPkg.isNullOrBlank() && tracked.contains(currentPkg)) {
                    if (gameModeActiveFor != currentPkg) {
                        gameModeActiveFor = currentPkg
                        setGameModeLedOverrideActiveStorage(this@GameModeService, true)
                        applyGameModeProfile()
                    } else if (
                        gameModeApplyPendingFor == currentPkg &&
                        LedScreenPolicy.isScreenInteractive(this@GameModeService)
                    ) {
                        applyGameModeProfile()
                    }
                } else if (!currentPkg.isNullOrBlank()) {
                    if (gameModeActiveFor != null) {
                        restoreNormalProfile()
                        setGameModeLedOverrideActiveStorage(this@GameModeService, false)
                        gameModeActiveFor = null
                        stopSelf()
                    }
                }
            } catch (_: Throwable) {
            } finally {
                if (!pollingPausedForScreenOff && gameModeActiveFor != null) {
                    handler.postDelayed(this, activeGamePollMs)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        workerThread = HandlerThread(
            "RedMagicGameMode",
            android.os.Process.THREAD_PRIORITY_BACKGROUND
        ).apply {
            start()
        }
        handler = Handler(workerThread.looper)

        handler.post {
            ChargingLedRecovery.repairStaleChargingOwnership(
                this@GameModeService
            )
        }

        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
        )

        // No idle polling here. GameMode starts from an explicit foreground app event.
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        val pkg = intent?.getStringExtra("foreground_pkg")
        val applySavedProfile =
            intent?.getBooleanExtra(
                EXTRA_APPLY_SAVED_PROFILE,
                false
            ) == true
        val continueAfterApply =
            intent?.getBooleanExtra(
                EXTRA_CONTINUE_AFTER_APPLY,
                false
            ) == true

        handler.post {
            when {
                applySavedProfile -> {
                    applySavedProfileNow()

                    if (gameModeActiveFor != null) {
                        handler.removeCallbacks(pollRunnable)
                        handler.post(pollRunnable)
                    } else if (!continueAfterApply) {
                        stopSelf(startId)
                    }
                }

                !pkg.isNullOrBlank() -> {
                    handleForegroundPackage(pkg)
                }

                gameModeActiveFor != null -> {
                    handler.removeCallbacks(pollRunnable)
                    handler.post(pollRunnable)
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun applySavedProfileNow() {
        val profile =
            getSavedGameModeProfileStorage(this)

        GameModeActions.applyProfileNow(
            profile = profile,
            applyFanLed = { effect, color ->
                if (effect.startsWith("preset:")) {
                    HardwareController.setFanLedStockPreset(
                        effect.removePrefix("preset:")
                    )
                } else {
                    HardwareController.setFanLedEffect(
                        effect,
                        color
                    )
                }
            }
        )
    }

    private fun handleForegroundPackage(currentPkg: String) {
        if (pollingPausedForScreenOff) return

        val tracked = getSavedGamePackagesStorage(this)
        handler.removeCallbacks(pollRunnable)

        if (!tracked.contains(currentPkg)) {
            if (
                gameModeActiveFor != null ||
                isGameModeLedOverrideActiveStorage(this)
            ) {
                restoreNormalProfile()
                setGameModeLedOverrideActiveStorage(this, false)
                gameModeActiveFor = null

                android.util.Log.i(
                    "RedmagicGameMode",
                    "left tracked game: restored normal hardware profile"
                )
            }

            stopSelf()
            return
        }

        if (gameModeActiveFor != currentPkg) {
            gameModeActiveFor = currentPkg
            setGameModeLedOverrideActiveStorage(this, true)
            applyGameModeProfile()
        } else if (
            gameModeApplyPendingFor == currentPkg &&
            LedScreenPolicy.isScreenInteractive(this)
        ) {
            applyGameModeProfile()
        }

        handler.postDelayed(pollRunnable, activeGamePollMs)
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        runCatching {
            unregisterReceiver(screenReceiver)
        }

        val cleanupPosted = handler.post {
            handler.removeCallbacks(pollRunnable)

            if (gameModeActiveFor != null) {
                restoreNormalProfile()
                setGameModeLedOverrideActiveStorage(
                    this@GameModeService,
                    false
                )
                gameModeActiveFor = null
            }

            workerThread.quitSafely()
        }

        if (!cleanupPosted) {
            workerThread.quitSafely()
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun getForegroundPackageName(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - 15_000L

        val events = usm.queryEvents(start, end)
        val event = UsageEvents.Event()
        var lastForeground: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                val pkg = event.packageName
                if (!pkg.isNullOrBlank() && !shouldIgnorePackage(pkg)) {
                    lastForeground = pkg
                }
            }
        }

        return lastForeground
    }

    private fun shouldIgnorePackage(pkg: String): Boolean {
        if (pkg == packageName) return true
        if (pkg == "com.android.systemui") return true
        return false
    }

    

    private fun getProfileForPackage(pkg: String): Map<String, Any> {
        val prefs = getSharedPreferences("redmagic_hw_controls_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("game_profile_$pkg", null) ?: return emptyMap()

        return try {
            val obj = org.json.JSONObject(json)
            mapOf(
                "fanEnabled" to obj.optBoolean("fanEnabled", true),
                "fanLevel" to obj.optInt("fanLevel", 3),
                "fanLedEnabled" to obj.optBoolean("fanLedEnabled", true),
                "fanLedEffect" to obj.optString("fanLedEffect", "steady"),
                "fanLedColor" to obj.optInt("fanLedColor", 5),
                "fanLedModeType" to obj.optString("fanLedModeType", "basic"),
                "fanLedPresetValue" to obj.optString("fanLedPresetValue", ""),
                "logoLedEnabled" to obj.optBoolean("logoLedEnabled", true),
                "logoLedEffect" to obj.optString("logoLedEffect", "steady"),
                "logoLedColor" to obj.optInt("logoLedColor", 1),
                "shoulderLedEnabled" to obj.optBoolean("shoulderLedEnabled", true),
                "shoulderLedEffect" to obj.optString("shoulderLedEffect", "breathe"),
                "shoulderLedColor" to obj.optInt("shoulderLedColor", 8)
            )
        } catch (_: Throwable) {
            emptyMap()
        }
    }
    private fun applyGameModeProfile() {
        if (HardwareScreenPolicy.blockNormalLedsWhileScreenOff(this, "game-mode-apply-screen-off")) return
        val prefs = getSharedPreferences("redmagic_hw_controls_prefs", Context.MODE_PRIVATE)

        if (!LedOwnership.canGameModeApply(this)) {
            android.util.Log.i(
                "RedmagicLedOwnership",
                "GameModeService skipped game LED apply because owner=${LedOwnership.current(this)}"
            )
            return
        }

        val pkg = gameModeActiveFor ?: return
        val profile = getProfileForPackage(pkg)

        val fanEnabled = profile["fanEnabled"] as? Boolean ?: prefs.getBoolean("game_mode_fan_enabled", true)
        val fanLevel = profile["fanLevel"] as? Int ?: prefs.getInt("game_mode_fan_level", 3)
        val pumpEnabled = prefs.getBoolean("game_mode_pump_enabled", false)
        val pumpProfile = prefs.getString("game_mode_pump_profile", "quick") ?: "quick"

        val fanLedEnabled = profile["fanLedEnabled"] as? Boolean ?: prefs.getBoolean("game_mode_fan_led_enabled", true)
        val fanLedEffect = profile["fanLedEffect"] as? String ?: prefs.getString("game_mode_fan_led_effect", "steady") ?: "steady"
        val fanLedColor = profile["fanLedColor"] as? Int ?: prefs.getInt("game_mode_fan_led_color", 5)
        val fanLedModeType = profile["fanLedModeType"] as? String ?: "basic"
        val fanLedPresetValue = profile["fanLedPresetValue"] as? String ?: ""

        val logoLedEnabled = profile["logoLedEnabled"] as? Boolean ?: prefs.getBoolean("game_mode_logo_led_enabled", true)
        val logoLedEffect = profile["logoLedEffect"] as? String ?: prefs.getString("game_mode_logo_led_effect", "steady") ?: "steady"
        val logoLedColor = profile["logoLedColor"] as? Int ?: prefs.getInt("game_mode_logo_led_color", 1)

        val shoulderLedEnabled = profile["shoulderLedEnabled"] as? Boolean ?: prefs.getBoolean("game_mode_shoulder_led_enabled", true)
        val shoulderLedEffect = profile["shoulderLedEffect"] as? String ?: prefs.getString("game_mode_shoulder_led_effect", "breathe") ?: "breathe"
        val shoulderLedColor = profile["shoulderLedColor"] as? Int ?: prefs.getInt("game_mode_shoulder_led_color", 8)

        fun applyOnce(reason: String) {
            if (gameModeActiveFor != pkg) return

            if (fanEnabled) {
                HardwareController.setFanLevel(fanLevel)
            } else {
                HardwareController.enableFan(false)
            }

            if (pumpEnabled) {
                HardwareController.setPumpProfile(pumpProfile)
            } else {
                HardwareController.enablePump(false)
            }

            if (fanLedEnabled) {
                if (fanLedModeType == "preset" && fanLedPresetValue.isNotBlank()) {
                    HardwareController.setFanLedStockPreset(fanLedPresetValue)
                } else if (fanLedEffect.startsWith("preset:")) {
                    HardwareController.setFanLedStockPreset(fanLedEffect.removePrefix("preset:"))
                } else {
                    HardwareController.setFanLedEffect(fanLedEffect, fanLedColor)
                }
            } else {
                HardwareController.setFanLedEnabled(false)
            }

            if (logoLedEnabled) {
                HardwareController.setLogoLedEffect(logoLedEffect, logoLedColor)
            } else {
                HardwareController.setLogoLedEnabled(false)
            }

            if (shoulderLedEnabled) {
                HardwareController.setShoulderLedEffect(shoulderLedEffect, shoulderLedColor)
            } else {
                HardwareController.setShoulderLedEnabled(false)
            }

            android.util.Log.i(
                "RedmagicGameMode",
                "apply[$reason] pkg=$pkg fan=$fanLedEnabled/$fanLedEffect/$fanLedColor logo=$logoLedEnabled/$logoLedEffect/$logoLedColor shoulder=$shoulderLedEnabled/$shoulderLedEffect/$shoulderLedColor"
            )
        }

        applyOnce("now")
    }
    private fun restoreNormalProfile() {
        if (HardwareScreenPolicy.blockNormalLedsWhileScreenOff(this, "game-mode-restore-screen-off")) return
        if (LedScreenPolicy.blockNonChargingLedWriteIfScreenOff(this, "game-mode-restore-normal")) return

        val prefs = getSharedPreferences("redmagic_hw_controls_prefs", Context.MODE_PRIVATE)

        val fanEnabled = prefs.getBoolean("fan_enabled", false)
        val fanLevel = prefs.getInt("fan_level", 0)
        val pumpEnabled = prefs.getBoolean("pump_enabled", false)
        val pumpProfile = prefs.getString("pump_profile", "quick") ?: "quick"

        val fanLedEnabled = prefs.getBoolean("fan_led_enabled", false)
        val fanLedEffect = prefs.getString("fan_led_effect", "steady") ?: "steady"
        val fanLedColor = prefs.getInt("fan_led_color", 5)

        val logoLedEnabled = prefs.getBoolean("logo_led_enabled", true)
        val logoLedEffect = prefs.getString("logo_led_effect", "steady") ?: "steady"
        val logoLedColor = prefs.getInt("logo_led_color", 1)

        val shoulderLedEnabled = prefs.getBoolean("shoulder_led_enabled", true)
        val shoulderLedEffect = prefs.getString("shoulder_led_effect", "breathe") ?: "breathe"
        val shoulderLedColor = prefs.getInt("shoulder_led_color", 8)

        fun restoreOnce(reason: String) {
            if (fanEnabled) {
                HardwareController.setFanLevel(fanLevel)
            } else {
                HardwareController.enableFan(false)
            }

            if (pumpEnabled) {
                HardwareController.setPumpProfile(pumpProfile)
            } else {
                HardwareController.enablePump(false)
            }

            if (fanLedEnabled) {
                if (fanLedEffect.startsWith("preset:")) {
                    HardwareController.setFanLedStockPreset(fanLedEffect.removePrefix("preset:"))
                } else {
                    HardwareController.setFanLedEffect(fanLedEffect, fanLedColor)
                }
            } else {
                HardwareController.setFanLedEnabled(false)
            }

            if (logoLedEnabled) {
                HardwareController.setLogoLedEffect(logoLedEffect, logoLedColor)
            } else {
                HardwareController.setLogoLedEnabled(false)
            }

            if (shoulderLedEnabled) {
                HardwareController.setShoulderLedEffect(shoulderLedEffect, shoulderLedColor)
            } else {
                HardwareController.setShoulderLedEnabled(false)
            }

            android.util.Log.i(
                "RedmagicGameMode",
                "restore[$reason] fan=$fanLedEnabled/$fanLedEffect/$fanLedColor logo=$logoLedEnabled/$logoLedEffect/$logoLedColor shoulder=$shoulderLedEnabled/$shoulderLedEffect/$shoulderLedColor"
            )
        }

        restoreOnce("now")
    }
}
