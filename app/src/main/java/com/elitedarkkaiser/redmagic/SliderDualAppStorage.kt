package com.elitedarkkaiser.redmagic

import android.content.Context
import com.elitedarkkaiser.redmagic.storage.AppPrefs
import java.util.Calendar

data class SliderDualAppConfig(
    val enabled: Boolean = false,
    val defaultUpPackage: String? = null,
    val defaultDownPackage: String? = null,
    val scheduleEnabled: Boolean = false,
    val scheduleStartMinutes: Int = 22 * 60,
    val scheduleEndMinutes: Int = 7 * 60,
    val scheduledUpPackage: String? = null,
    val scheduledDownPackage: String? = null
) {
    fun packageForState(
        sliderUp: Boolean,
        minuteOfDay: Int = currentMinuteOfDay()
    ): String? {
        val scheduled = scheduleEnabled &&
            isInsideSchedule(
                minuteOfDay,
                scheduleStartMinutes,
                scheduleEndMinutes
            )

        return when {
            scheduled && sliderUp -> scheduledUpPackage
            scheduled -> scheduledDownPackage
            sliderUp -> defaultUpPackage
            else -> defaultDownPackage
        }
    }

    fun isComplete(): Boolean {
        if (
            defaultUpPackage.isNullOrBlank() ||
            defaultDownPackage.isNullOrBlank()
        ) {
            return false
        }

        return !scheduleEnabled ||
            (
                !scheduledUpPackage.isNullOrBlank() &&
                    !scheduledDownPackage.isNullOrBlank()
                )
    }

    companion object {
        fun isInsideSchedule(
            minuteOfDay: Int,
            startMinutes: Int,
            endMinutes: Int
        ): Boolean {
            val minute = minuteOfDay.coerceIn(0, 1439)
            val start = startMinutes.coerceIn(0, 1439)
            val end = endMinutes.coerceIn(0, 1439)

            return when {
                start == end -> true
                start < end -> minute in start until end
                else -> minute >= start || minute < end
            }
        }

        private fun currentMinuteOfDay(): Int {
            val calendar = Calendar.getInstance()
            return calendar.get(Calendar.HOUR_OF_DAY) * 60 +
                calendar.get(Calendar.MINUTE)
        }
    }
}

object SliderDualAppStorage {
    private const val ENABLED = "slider_dual_app_enabled"
    private const val DEFAULT_UP = "slider_dual_app_default_up"
    private const val DEFAULT_DOWN = "slider_dual_app_default_down"
    private const val SCHEDULE_ENABLED =
        "slider_dual_app_schedule_enabled"
    private const val SCHEDULE_START =
        "slider_dual_app_schedule_start"
    private const val SCHEDULE_END =
        "slider_dual_app_schedule_end"
    private const val SCHEDULED_UP =
        "slider_dual_app_scheduled_up"
    private const val SCHEDULED_DOWN =
        "slider_dual_app_scheduled_down"

    fun read(context: Context): SliderDualAppConfig {
        val prefs = context.getSharedPreferences(
            AppPrefs.PREFS_NAME,
            Context.MODE_PRIVATE
        )

        return SliderDualAppConfig(
            enabled = prefs.getBoolean(ENABLED, false),
            defaultUpPackage = prefs.getString(DEFAULT_UP, null),
            defaultDownPackage = prefs.getString(DEFAULT_DOWN, null),
            scheduleEnabled = prefs.getBoolean(
                SCHEDULE_ENABLED,
                false
            ),
            scheduleStartMinutes = prefs.getInt(
                SCHEDULE_START,
                22 * 60
            ).coerceIn(0, 1439),
            scheduleEndMinutes = prefs.getInt(
                SCHEDULE_END,
                7 * 60
            ).coerceIn(0, 1439),
            scheduledUpPackage = prefs.getString(
                SCHEDULED_UP,
                null
            ),
            scheduledDownPackage = prefs.getString(
                SCHEDULED_DOWN,
                null
            )
        )
    }

    fun save(context: Context, config: SliderDualAppConfig) {
        context.getSharedPreferences(
            AppPrefs.PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(ENABLED, config.enabled)
            .putString(DEFAULT_UP, config.defaultUpPackage)
            .putString(DEFAULT_DOWN, config.defaultDownPackage)
            .putBoolean(
                SCHEDULE_ENABLED,
                config.scheduleEnabled
            )
            .putInt(
                SCHEDULE_START,
                config.scheduleStartMinutes.coerceIn(0, 1439)
            )
            .putInt(
                SCHEDULE_END,
                config.scheduleEndMinutes.coerceIn(0, 1439)
            )
            .putString(SCHEDULED_UP, config.scheduledUpPackage)
            .putString(
                SCHEDULED_DOWN,
                config.scheduledDownPackage
            )
            .apply()
    }

    fun disable(context: Context) {
        val current = read(context)
        if (current.enabled) {
            save(context, current.copy(enabled = false))
        }
        HardwareServiceActions.stopSliderDualApp(context)
    }

    fun summary(context: Context): String {
        val config = read(context)
        return when {
            !config.enabled -> "DUAL-APP SLIDER: Off"
            config.scheduleEnabled ->
                "DUAL-APP SLIDER: On • Schedule On"
            else -> "DUAL-APP SLIDER: On"
        }
    }
}
