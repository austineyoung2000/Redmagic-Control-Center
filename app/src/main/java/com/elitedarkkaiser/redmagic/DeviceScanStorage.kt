package com.elitedarkkaiser.redmagic

import android.content.Context
import com.elitedarkkaiser.redmagic.storage.AppPrefs

private const val DEVICE_SCAN_MODEL = "device_scan_model"
private const val DEVICE_SCAN_SUMMARY = "device_scan_summary"
private const val DEVICE_SCAN_SUPPORTED_MODEL = "device_scan_supported_model"
private const val DEVICE_SCAN_FAN_AVAILABLE = "device_scan_fan_available"
private const val DEVICE_SCAN_FAN_RPM_AVAILABLE =
    "device_scan_fan_rpm_available"
private const val DEVICE_SCAN_PUMP_AVAILABLE = "device_scan_pump_available"
private const val DEVICE_SCAN_LED_AVAILABLE = "device_scan_led_available"
private const val DEVICE_SCAN_TRIGGERS_AVAILABLE = "device_scan_triggers_available"
private const val DEVICE_SCAN_SLIDER_AVAILABLE =
    "device_scan_slider_available"
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
        .putBoolean(
            DEVICE_SCAN_FAN_RPM_AVAILABLE,
            report.fanRpmAvailable
        )
        .putBoolean(DEVICE_SCAN_PUMP_AVAILABLE, report.pumpAvailable)
        .putBoolean(DEVICE_SCAN_LED_AVAILABLE, report.ledAvailable)
        .putBoolean(DEVICE_SCAN_TRIGGERS_AVAILABLE, report.triggersAvailable)
        .putBoolean(
            DEVICE_SCAN_SLIDER_AVAILABLE,
            report.sliderAvailable
        )
        .putLong(DEVICE_SCAN_LAST_RUN, System.currentTimeMillis())
        .putString(
            DEVICE_SCAN_FINGERPRINT,
            report.fingerprint
        )
        .apply()
}

data class DeviceCapabilities(
    val scanComplete: Boolean,
    val fanAvailable: Boolean,
    val fanRpmAvailable: Boolean,
    val pumpAvailable: Boolean,
    val ledAvailable: Boolean,
    val triggersAvailable: Boolean,
    val sliderAvailable: Boolean
) {
    companion object {
        fun unknown(): DeviceCapabilities {
            return DeviceCapabilities(
                scanComplete = false,
                fanAvailable = true,
                fanRpmAvailable = true,
                pumpAvailable = true,
                ledAvailable = true,
                triggersAvailable = true,
                sliderAvailable = true
            )
        }
    }
}

fun DeviceCapabilityReport.toDeviceCapabilities():
    DeviceCapabilities {
    return DeviceCapabilities(
        scanComplete = true,
        fanAvailable = fanAvailable,
        fanRpmAvailable = fanRpmAvailable,
        pumpAvailable = pumpAvailable,
        ledAvailable = ledAvailable,
        triggersAvailable = triggersAvailable,
        sliderAvailable = sliderAvailable
    )
}

fun deviceCapabilitiesStorage(
    context: Context
): DeviceCapabilities {
    if (!hasDeviceCapabilityReportStorage(context)) {
        return DeviceCapabilities.unknown()
    }

    val prefs = context.getSharedPreferences(
        AppPrefs.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    return DeviceCapabilities(
        scanComplete = true,
        fanAvailable = prefs.getBoolean(
            DEVICE_SCAN_FAN_AVAILABLE,
            false
        ),
        fanRpmAvailable = prefs.getBoolean(
            DEVICE_SCAN_FAN_RPM_AVAILABLE,
            false
        ),
        pumpAvailable = prefs.getBoolean(
            DEVICE_SCAN_PUMP_AVAILABLE,
            false
        ),
        ledAvailable = prefs.getBoolean(
            DEVICE_SCAN_LED_AVAILABLE,
            false
        ),
        triggersAvailable = prefs.getBoolean(
            DEVICE_SCAN_TRIGGERS_AVAILABLE,
            false
        ),
        sliderAvailable = prefs.getBoolean(
            DEVICE_SCAN_SLIDER_AVAILABLE,
            false
        )
    )
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
        prefs.contains(DEVICE_SCAN_FAN_RPM_AVAILABLE) &&
        prefs.contains(DEVICE_SCAN_SLIDER_AVAILABLE) &&
        prefs.getString(
            DEVICE_SCAN_FINGERPRINT,
            null
        ) == android.os.Build.FINGERPRINT
}
