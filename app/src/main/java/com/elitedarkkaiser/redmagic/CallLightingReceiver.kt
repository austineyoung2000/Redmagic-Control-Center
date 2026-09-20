package com.elitedarkkaiser.redmagic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class CallLightingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!DeviceCompatibility.isSupportedDevice()) return
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        if (!CallLightingState.isEnabled(context)) return

        HardwareServiceActions.startCallLighting(context)
    }
}
