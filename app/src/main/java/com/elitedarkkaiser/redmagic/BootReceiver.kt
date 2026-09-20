package com.elitedarkkaiser.redmagic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_USER_UNLOCKED) return

        if (!DeviceCompatibility.isSupportedDevice()) {
            return
        }

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            setTriggersDisabledUntilRestartStorage(
                context,
                false
            )
        }

        HardwareServiceActions.startChargingMode(context)
        if (CallLightingState.isEnabled(context)) {
            HardwareServiceActions.startCallLighting(context)
        }
        if (RgbStudioStorage.isEnabled(context)) {
            HardwareServiceActions.startRgbCycle(context)
        }
        if (SliderDualAppStorage.read(context).enabled) {
            HardwareServiceActions.startSliderDualApp(context)
        }

        val shouldStartTriggers =
            readTriggerPrefsSnapshot(context)
                .triggersAutoStart
        val shouldRunUnlockRule =
            action == Intent.ACTION_USER_UNLOCKED &&
                AutomationRulesStorage.profileName(
                    context,
                    AutomationRuleEvent.DEVICE_UNLOCKED
                ) != null

        if (shouldStartTriggers || shouldRunUnlockRule) {
            val pendingResult = goAsync()

            Thread({
                android.os.Process.setThreadPriority(
                    android.os.Process.THREAD_PRIORITY_BACKGROUND
                )

                try {
                    if (shouldRunUnlockRule) {
                        AutomationRuleExecutor.applyNow(
                            context.applicationContext,
                            AutomationRuleEvent
                                .DEVICE_UNLOCKED
                        )
                    }

                    if (shouldStartTriggers) {
                        HardwareServiceActions
                            .startTriggersIfAutoStartEnabled(
                                context.applicationContext
                            )
                    }
                } finally {
                    pendingResult.finish()
                }
            }, "RedMagicTriggerAutoStart").start()
        }

    }

}
