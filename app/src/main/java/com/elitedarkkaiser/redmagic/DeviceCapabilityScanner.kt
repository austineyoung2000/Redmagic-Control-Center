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
        val model = Build.MODEL.orEmpty().ifBlank { prop("ro.product.model") }
        val marketName = prop("ro.product.marketname")
        val fingerprint = Build.FINGERPRINT.orEmpty().ifBlank { prop("ro.build.fingerprint") }

        val fanAvailable =
            exists("/sys/kernel/fan/fan_enable") ||
            exists("/sys/kernel/fan/fan_speed_level") ||
            exists("/sys/kernel/fan/fan_speed_count")

        val pumpAvailable =
            exists("/proc/driver/micropump/enable") ||
            exists("/proc/driver/micropump/mode")

        val ledAvailable =
            exists("/sys/class/leds/aw22xxx_led/effect") ||
            exists("/sys/class/leds/aw22xxx_led/cfg")

        val triggersAvailable =
            exists("/sys/class/leds/sar0/mode_operation") ||
            exists("/sys/class/leds/sar1/mode_operation")

        val sliderAvailable =
            exists("/proc/driver/slider") ||
            prop("persist.sys.nubia.slider").isNotBlank()

        val summary = buildString {
            append("Model: ").append(model.ifBlank { "unknown" })
            if (marketName.isNotBlank()) append(" / ").append(marketName)
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
            isKnownRedmagic11Pro = model.equals("NX809J", ignoreCase = true),
            fanAvailable = fanAvailable,
            pumpAvailable = pumpAvailable,
            ledAvailable = ledAvailable,
            triggersAvailable = triggersAvailable,
            sliderAvailable = sliderAvailable,
            summary = summary
        )
    }
}
