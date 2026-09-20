package com.elitedarkkaiser.redmagic

import android.os.Build

data class DeviceCapabilityReport(
    val model: String,
    val marketName: String,
    val fingerprint: String,
    val isKnownRedmagic11Pro: Boolean,
    val fanAvailable: Boolean,
    val pumpAvailable: Boolean,
    val ledAvailable: Boolean,
    val triggersAvailable: Boolean,
    val sliderAvailable: Boolean,
    val summary: String
)

object DeviceCapabilityScanner {
    private fun output(command: String): String {
        return RootShell.execForOutput(command)?.trim().orEmpty()
    }

    private fun exists(path: String): Boolean {
        val safe = path.replace("'", "'\\''")
        return output("[ -e '$safe' ] && echo yes || echo no") == "yes"
    }

    private fun prop(name: String): String {
        return output("getprop $name")
    }

    fun scan(): DeviceCapabilityReport {
        val identity = DeviceCompatibility.identity()
        val model = identity.detectedModel
        val marketName = identity.marketName
        val fingerprint = Build.FINGERPRINT.orEmpty().ifBlank { prop("ro.build.fingerprint") }

        val fanAvailable =
            exists(DeviceCompatibility.Paths.FAN_ENABLE) &&
            exists(DeviceCompatibility.Paths.FAN_LEVEL) &&
            exists(DeviceCompatibility.Paths.FAN_RPM)

        val pumpAvailable =
            exists(DeviceCompatibility.Paths.PUMP_ENABLE) &&
            exists(DeviceCompatibility.Paths.PUMP_FREQ) &&
            exists(DeviceCompatibility.Paths.PUMP_SPEED)

        val ledAvailable =
            exists(DeviceCompatibility.Paths.LED_EFFECT) &&
            exists(DeviceCompatibility.Paths.LED_CFG)

        val triggersAvailable =
            exists(DeviceCompatibility.Paths.SAR0_MODE) &&
            exists(DeviceCompatibility.Paths.SAR1_MODE)

        val sliderAvailable =
            exists("/proc/driver/slider") ||
            prop("persist.sys.nubia.slider").isNotBlank()

        val summary = buildString {
            append("Model: ").append(model.ifBlank { "unknown" })
            if (marketName.isNotBlank()) append(" / ").append(marketName)
            append("\nCompatibility: ").append(
                if (identity.supported) {
                    "NX809J confirmed"
                } else {
                    "unsupported device"
                }
            )
            append("\nFan: ").append(if (fanAvailable) "available" else "missing")
            append("\nPump: ").append(if (pumpAvailable) "available" else "missing")
            append("\nLED: ").append(if (ledAvailable) "available" else "missing")
            append("\nTriggers: ").append(if (triggersAvailable) "available" else "missing")
            append("\nSlider: ").append(if (sliderAvailable) "available" else "unknown/missing")
        }

        return DeviceCapabilityReport(
            model = model,
            marketName = marketName,
            fingerprint = fingerprint,
            isKnownRedmagic11Pro = identity.supported,
            fanAvailable = fanAvailable,
            pumpAvailable = pumpAvailable,
            ledAvailable = ledAvailable,
            triggersAvailable = triggersAvailable,
            sliderAvailable = sliderAvailable,
            summary = summary
        )
    }
}
