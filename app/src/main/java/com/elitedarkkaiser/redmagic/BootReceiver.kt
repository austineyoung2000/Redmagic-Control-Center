package com.elitedarkkaiser.redmagic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_USER_UNLOCKED) return

        HardwareServiceActions.startChargingMode(context)
        if (CallLightingState.isEnabled(context)) {
            HardwareServiceActions.startCallLighting(context)
        }
        if (RgbStudioStorage.isEnabled(context)) {
            HardwareServiceActions.startRgbCycle(context)
        }

        if (readTriggerPrefsSnapshot(context).triggersAutoStart) {
            val pendingResult = goAsync()

            Thread({
                android.os.Process.setThreadPriority(
                    android.os.Process.THREAD_PRIORITY_BACKGROUND
                )

                try {
                    HardwareServiceActions
                        .startTriggersIfAutoStartEnabled(
                            context.applicationContext
                        )
                } finally {
                    pendingResult.finish()
                }
            }, "RedMagicTriggerAutoStart").start()
        }

    }

}
