package com.elitedarkkaiser.redmagic

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class FanLedRestoreWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        if (!DeviceCompatibility.isSupportedDevice()) {
            return Result.success()
        }

        val prefs = applicationContext.getSharedPreferences(
            "redmagic_hw_controls_prefs",
            Context.MODE_PRIVATE
        )

        val enabled = prefs.getBoolean("fan_led_enabled", false)
        val effect = prefs.getString("fan_led_effect", "steady") ?: "steady"
        val color = prefs.getInt("fan_led_color", 5)

        return try {
            val ok = if (enabled) {
                HardwareController.setFanLedEffect(effect, color)
            } else {
                HardwareController.setFanLedEnabled(false)
            }

            if (ok) Result.success() else Result.retry()
        } catch (_: Throwable) {
            Result.retry()
        }
    }
}
