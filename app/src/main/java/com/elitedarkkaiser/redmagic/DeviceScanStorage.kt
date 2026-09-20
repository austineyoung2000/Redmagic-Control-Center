package com.elitedarkkaiser.redmagic

import android.content.Context
import com.elitedarkkaiser.redmagic.storage.AppPrefs

private const val DEVICE_SCAN_MODEL = "device_scan_model"
private const val DEVICE_SCAN_SUMMARY = "device_scan_summary"
private const val DEVICE_SCAN_SUPPORTED_MODEL = "device_scan_supported_model"
private const val DEVICE_SCAN_FAN_AVAILABLE = "device_scan_fan_available"
private const val DEVICE_SCAN_PUMP_AVAILABLE = "device_scan_pump_available"
private const val DEVICE_SCAN_LED_AVAILABLE = "device_scan_led_available"
private const val DEVICE_SCAN_TRIGGERS_AVAILABLE = "device_scan_triggers_available"
private const val DEVICE_SCAN_LAST_RUN = "device_scan_last_run"
private const val DEVICE_SCAN_FINGERPRINT =
    "device_scan_fingerprint"

fun saveDeviceCapabilityReportStorage(context: Context, report: DeviceCapabilityReport) {
    context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(DEVICE_SCAN_MODEL, report.model)
        .putString(DEVICE_SCAN_SUMMARY, report.summary)
        .putBoolean(DEVICE_SCAN_SUPPORTED_MODEL, report.isKnownRedmagic11Pro)
        .putBoolean(DEVICE_SCAN_FAN_AVAILABLE, report.fanAvailable)
        .putBoolean(DEVICE_SCAN_PUMP_AVAILABLE, report.pumpAvailable)
        .putBoolean(DEVICE_SCAN_LED_AVAILABLE, report.ledAvailable)
        .putBoolean(DEVICE_SCAN_TRIGGERS_AVAILABLE, report.triggersAvailable)
        .putLong(DEVICE_SCAN_LAST_RUN, System.currentTimeMillis())
        .putString(
            DEVICE_SCAN_FINGERPRINT,
            report.fingerprint
        )
        .apply()
}

fun deviceScanSummaryStorage(context: Context): String {
    return context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        .getString(DEVICE_SCAN_SUMMARY, "Device scan pending…") ?: "Device scan pending…"
}


fun hasDeviceCapabilityReportStorage(context: Context): Boolean {
    val prefs = context.getSharedPreferences(
        AppPrefs.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    return prefs.contains(DEVICE_SCAN_SUMMARY) &&
        prefs.getString(
            DEVICE_SCAN_FINGERPRINT,
            null
        ) == android.os.Build.FINGERPRINT
}
