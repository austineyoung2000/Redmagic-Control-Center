package com.elitedarkkaiser.redmagic

import android.content.Context
import android.os.SystemClock

data class HapticFeedbackConfig(
    val enabled: Boolean = false,
    val strength: String = HapticFeedback.Strength.MEDIUM.key
)

object HapticFeedback {
    private const val PREFS = "hardware_haptic_feedback"
    private const val ENABLED_KEY = "enabled"
    private const val STRENGTH_KEY = "strength"

    private const val BASE = "/sys/class/leds/zte_vibrator"
    private const val DURATION = "$BASE/duration"
    private const val GAIN = "$BASE/gain"
    private const val ACTIVATE = "$BASE/activate"
    private const val MINIMUM_PULSE_GAP_MS = 90L

    enum class Strength(
        val key: String,
        val label: String,
        val gain: Int,
        val durationMs: Int
    ) {
        LOW("low", "Low", 80, 80),
        MEDIUM("medium", "Medium", 150, 100),
        HIGH("high", "High", 220, 120);

        companion object {
            fun fromKey(value: String?): Strength {
                return entries.firstOrNull {
                    it.key == value
                } ?: MEDIUM
            }
        }
    }

    enum class Event {
        TRIGGER,
        MAGIC_KEY,
        MASTER_PROFILE
    }

    private val pulseLock = Any()
    private var lastPulseAtMs = 0L

    fun read(context: Context): HapticFeedbackConfig {
        val prefs = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

        return HapticFeedbackConfig(
            enabled = prefs.getBoolean(ENABLED_KEY, false),
            strength = Strength.fromKey(
                prefs.getString(
                    STRENGTH_KEY,
                    Strength.MEDIUM.key
                )
            ).key
        )
    }

    fun save(
        context: Context,
        config: HapticFeedbackConfig
    ) {
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(ENABLED_KEY, config.enabled)
            .putString(
                STRENGTH_KEY,
                Strength.fromKey(config.strength).key
            )
            .apply()
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        save(
            context,
            read(context).copy(enabled = enabled)
        )
    }

    fun setStrength(
        context: Context,
        strength: Strength
    ) {
        save(
            context,
            read(context).copy(strength = strength.key)
        )
    }

    fun pulse(
        context: Context,
        event: Event
    ): Boolean {
        val config = read(context)
        if (!config.enabled) return false

        return pulseStrength(
            Strength.fromKey(config.strength),
            event.name
        )
    }

    fun testPulse(strength: Strength): Boolean {
        return pulseStrength(strength, "TEST")
    }

    private fun pulseStrength(
        strength: Strength,
        reason: String
    ): Boolean {
        if (!DeviceCompatibility.isSupportedDevice()) {
            return false
        }

        synchronized(pulseLock) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastPulseAtMs < MINIMUM_PULSE_GAP_MS) {
                return false
            }

            val succeeded = RootShell.exec(
                "echo ${strength.durationMs} > $DURATION; " +
                    "echo ${strength.gain} > $GAIN; " +
                    "echo 1 > $ACTIVATE"
            )

            if (succeeded) {
                lastPulseAtMs = SystemClock.elapsedRealtime()
                android.util.Log.d(
                    "RedmagicHaptics",
                    "pulse reason=$reason strength=${strength.key}"
                )
            }

            return succeeded
        }
    }
}
