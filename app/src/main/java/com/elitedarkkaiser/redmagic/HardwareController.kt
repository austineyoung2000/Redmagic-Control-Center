package com.elitedarkkaiser.redmagic

import kotlin.math.abs
import kotlin.math.round
import java.util.concurrent.ConcurrentHashMap

object HardwareController {

    private data class RecentHardwareWrite(
        val command: String,
        val completedAtMs: Long
    )

    private val recentHardwareWrites =
        ConcurrentHashMap<String, RecentHardwareWrite>()

    private const val DUPLICATE_WRITE_SKIP_MS = 2_000L

    @Synchronized
    private fun execHardwareWrite(resource: String, command: String): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        val previous = recentHardwareWrites[resource]

        if (
            previous != null &&
            previous.command == command &&
            (now - previous.completedAtMs) < DUPLICATE_WRITE_SKIP_MS
        ) {
            android.util.Log.d(
                "HardwareController",
                "skip duplicate write resource=$resource"
            )
            return true
        }

        val succeeded = RootShell.exec(command)
        if (succeeded) {
            recentHardwareWrites[resource] = RecentHardwareWrite(
                command = command,
                completedAtMs = android.os.SystemClock.elapsedRealtime()
            )
        }

        return succeeded
    }

    private const val FAN_ENABLE = "/sys/kernel/fan/fan_enable"
    private const val FAN_LEVEL = "/sys/kernel/fan/fan_speed_level"
    private const val FAN_PWM = "/sys/kernel/fan/fan_speed_pwm"
    private const val FAN_RPM = "/sys/kernel/fan/fan_speed_count"

    private const val PUMP_ENABLE = "/proc/driver/micropump/enable"
    private const val PUMP_FREQ = "/proc/driver/micropump/freq"
    private const val PUMP_SPEED = "/proc/driver/micropump/speed"

    private const val LED_EFFECT = "/sys/class/leds/aw22xxx_led/effect"
    private const val LED_CFG = "/sys/class/leds/aw22xxx_led/cfg"

    private const val SAR0_MODE = "/sys/class/leds/sar0/mode_operation"
    private const val SAR1_MODE = "/sys/class/leds/sar1/mode_operation"

    private const val HAPTIC_DURATION = "/sys/class/leds/zte_vibrator/duration"
    private const val HAPTIC_GAIN = "/sys/class/leds/zte_vibrator/gain"
    private const val HAPTIC_ACTIVATE = "/sys/class/leds/zte_vibrator/activate"

    fun enableFan(enabled: Boolean): Boolean {
        return execHardwareWrite("fan_control", "echo ${if (enabled) 1 else 0} > $FAN_ENABLE")
    }

    fun isFanEnabled(): Boolean {
        return RootShell.execForOutput("cat $FAN_ENABLE")?.trim() == "1"
    }

    fun setFanLevel(level: Int): Boolean {
        val safe = level.coerceIn(0, 5)
        val cmds = if (safe == 0) {
            "echo 0 > $FAN_LEVEL; echo 0 > $FAN_ENABLE"
        } else {
            "echo 1 > $FAN_ENABLE; echo $safe > $FAN_LEVEL"
        }
        return execHardwareWrite("fan_control", cmds)
    }

    fun setFanPwm(value: Int): Boolean {
        val safe = value.coerceIn(0, 255)
        val cmds = if (safe == 0) {
            "echo 0 > $FAN_PWM; echo 0 > $FAN_ENABLE"
        } else {
            "echo 1 > $FAN_ENABLE; echo $safe > $FAN_PWM"
        }
        return execHardwareWrite("fan_control", cmds)
    }

    fun readFanRpm(): Int? {
        return RootShell.execForOutput("cat $FAN_RPM")?.trim()?.toIntOrNull()
    }

    fun readFanLevel(): Int? {
        return RootShell.execForOutput("cat $FAN_LEVEL")?.trim()?.toIntOrNull()?.coerceIn(0, 5)
    }

    fun enablePump(enabled: Boolean): Boolean {
        return execHardwareWrite("pump_control", "echo ${if (enabled) 1 else 0} > $PUMP_ENABLE")
    }

    fun setPumpProfile(profile: String): Boolean {
        val cmd = when (profile.lowercase()) {
            "slow" -> "echo 1 > $PUMP_ENABLE; echo 4 > $PUMP_FREQ; echo 40 > $PUMP_SPEED"
            "medium" -> "echo 1 > $PUMP_ENABLE; echo 4 > $PUMP_FREQ; echo 60 > $PUMP_SPEED"
            "quick" -> "echo 1 > $PUMP_ENABLE; echo 4 > $PUMP_FREQ; echo 80 > $PUMP_SPEED"
            "experimental" -> "echo 1 > $PUMP_ENABLE; echo 4 > $PUMP_FREQ; echo 90 > $PUMP_SPEED"
            "off" -> "echo 0 > $PUMP_ENABLE"
            else -> "echo 1 > $PUMP_ENABLE; echo 4 > $PUMP_FREQ; echo 80 > $PUMP_SPEED"
        }
        return execHardwareWrite("pump_control", cmd)
    }

    fun readPumpEnabled(): String? = RootShell.execForOutput("cat $PUMP_ENABLE")
    fun readPumpFreq(): String? = RootShell.execForOutput("cat $PUMP_FREQ")
    fun readPumpSpeed(): String? = RootShell.execForOutput("cat $PUMP_SPEED")

    fun setFanLedEnabled(enabled: Boolean): Boolean {
        return if (enabled) {
            execHardwareWrite("led_control", "echo 0x3002005 > $LED_EFFECT; echo 1 > $LED_CFG")
        } else {
            execHardwareWrite("led_control", "echo 0x3000000 > $LED_EFFECT; echo 1 > $LED_CFG")
        }
    }

    fun setFanLedStockPreset(effectValue: String): Boolean {
        val safeEffectValue = effectValue.takeIf { it in FAN_LED_STOCK_PRESETS } ?: return false
        return execHardwareWrite("led_control", "echo 1 > $FAN_ENABLE; echo $safeEffectValue > $LED_EFFECT; echo 1 > $LED_CFG")
    }

    fun setLogoLedEnabled(enabled: Boolean): Boolean {
        return if (enabled) {
            execHardwareWrite("led_control", "echo 0x1002001 > $LED_EFFECT; echo 1 > $LED_CFG")
        } else {
            execHardwareWrite("led_control", "echo 0x1000000 > $LED_EFFECT; echo 1 > $LED_CFG")
        }
    }

    fun setShoulderLedEnabled(enabled: Boolean): Boolean {
        return if (enabled) {
            execHardwareWrite("led_control", "echo 1 > $FAN_ENABLE; echo 0x2002005 > $LED_EFFECT; echo 1 > $LED_CFG")
        } else {
            execHardwareWrite("led_control", "echo 1 > $FAN_ENABLE; echo 0x2000000 > $LED_EFFECT; echo 1 > $LED_CFG")
        }
    }

    private val FAN_LED_STOCK_PRESETS = setOf(
        "0x3002101",
        "0x3002102",
        "0x3002103",
        "0x3002104",
        "0x3002105",
        "0x3002106",
        "0x3002107",
        "0x3002108"
    )

    private enum class LedZone(val zonePrefix: String, val enableFanFirst: Boolean) {
        LOGO("1", false),
        SHOULDER("2", true),
        FAN("3", true)
    }

    private fun mapUnifiedLedColor(color: Int): Int {
        return when (color) {
            1 -> 1  // red
            3 -> 3  // orange
            4 -> 4  // yellow
            5 -> 5  // green
            6 -> 6  // cyan
            7 -> 7  // blue
            8 -> 8  // purple
            9 -> 9  // pink
            else -> 1
        }
    }

    private fun mapUnifiedLedEffect(effectName: String): String {
        return when (effectName.lowercase()) {
            "steady" -> "00200"
            "breathe" -> "00300"
            "flashing" -> "00400"
            else -> "00200"
        }
    }

    private fun buildUnifiedLedEffectValue(zone: LedZone, effectName: String, color: Int): String {
        val colorCode = mapUnifiedLedColor(color)
        val effectCode = mapUnifiedLedEffect(effectName)
        return "0x${zone.zonePrefix}${effectCode}${Integer.toHexString(colorCode)}"
    }

    private fun setUnifiedLedEffect(zone: LedZone, effectName: String, color: Int): Boolean {
        val effectValue = buildUnifiedLedEffectValue(zone, effectName, color)
        val cmd = if (zone.enableFanFirst) {
            "echo 1 > $FAN_ENABLE; echo $effectValue > $LED_EFFECT; echo 1 > $LED_CFG"
        } else {
            "echo $effectValue > $LED_EFFECT; echo 1 > $LED_CFG"
        }
        return execHardwareWrite("led_control", cmd)
    }

    fun setShoulderLedEffect(effectName: String, color: Int): Boolean {
        val colorCode = when (color) {
            1 -> 1  // red
            3 -> 3  // orange
            4 -> 4  // yellow
            5 -> 5  // green
            6 -> 6  // cyan
            7 -> 7  // blue
            8 -> 8  // purple
            9 -> 9  // pink
            else -> 5
        }

        val effectValue = when (effectName.lowercase()) {
            "steady" -> "0x200200${Integer.toHexString(colorCode)}"
            "breathe" -> "0x200300${Integer.toHexString(colorCode)}"
            "flashing" -> "0x200400${Integer.toHexString(colorCode)}"
            else -> "0x200200${Integer.toHexString(colorCode)}"
        }

        val command =
            "echo 1 > $FAN_ENABLE; echo $effectValue > $LED_EFFECT; echo 1 > $LED_CFG"
        return execHardwareWrite("led_control", command)
    }

    fun setLogoLedEffect(effectName: String, color: Int): Boolean {
        return setUnifiedLedEffect(LedZone.LOGO, effectName, color)
    }

    fun setFanLedEffect(effectName: String, color: Int): Boolean {
        return setUnifiedLedEffect(LedZone.FAN, effectName, color)
    }

    fun turnOffAllLeds(): Boolean {
        val cmd = buildString {
            for (z in 1..3) {
                append("echo 0x${z}000000 > $LED_EFFECT; ")
                append("echo 1 > $LED_CFG; ")
            }
        }
        return execHardwareWrite("led_control", cmd)
    }

    fun enableTriggers(): Boolean {
        return execHardwareWrite("trigger_control", "echo 1 > $SAR0_MODE; echo 1 > $SAR1_MODE")
    }

    fun disableTriggers(): Boolean {
        return execHardwareWrite("trigger_control", "echo 0 > $SAR0_MODE; echo 0 > $SAR1_MODE")
    }

    fun injectTap(x: Int, y: Int): Boolean {
        return RootShell.exec("input tap $x $y")
    }

    private fun setSliderStockFunction(value: Int): Boolean {
        val cmd = "settings put system fourth_physical_key_function_value $value; " +
            "settings put system physical_key_function_app_value cn.nubia.gamelauncher"
        return execHardwareWrite("slider_control", cmd)
    }

    fun setSliderOpenCamera(): Boolean = setSliderStockFunction(1)

    fun setSliderOpenGameSpace(): Boolean = setSliderStockFunction(2)

    fun setSliderSoundMode(): Boolean = setSliderStockFunction(3)

    fun setSliderFlashlight(): Boolean = setSliderStockFunction(4)

    fun setSliderVoiceRecorder(): Boolean = setSliderStockFunction(5)

    fun setSliderLaunchApp(pkg: String): Boolean {
        val cmd = "settings put system fourth_physical_key_function_value 16; " +
            "settings put system physical_key_function_app_value $pkg"
        return execHardwareWrite("slider_control", cmd)
    }

    fun disableSliderSystemHandling(): Boolean {
        return execHardwareWrite("slider_control", "settings put system fourth_physical_key_function_value 0")
    }

    fun readSliderState(): String? {
        return RootShell.execForOutput("settings get global zte_keypad_slide_on_or_off")?.trim()
    }

    fun vibrate(durationMs: Int, gain: Int): Boolean {
        val d = durationMs.coerceIn(1, 5000)
        val g = gain.coerceIn(0, 255)
        val cmd = "echo $d > $HAPTIC_DURATION; echo $g > $HAPTIC_GAIN; echo 1 > $HAPTIC_ACTIVATE"
        return RootShell.exec(cmd)
    }

    fun readTemperatureC(): Float? {
        val candidates = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/thermal/thermal_zone2/temp",
            "/sys/class/thermal/thermal_zone3/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp",
            "/sys/devices/virtual/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone2/temp",
            "/sys/devices/virtual/thermal/thermal_zone3/temp"
        )

        for (path in candidates) {
            val raw = RootShell.execForOutput("cat $path 2>/dev/null")?.trim()?.toFloatOrNull() ?: continue
            if (raw > 1000f && raw < 200000f) return raw / 1000f
            if (raw > 0f && raw < 200f) return raw
        }

        return null
    }

    fun readTemperatureF(): Float? {
        val c = readTemperatureC() ?: return null
        return (c * 9f / 5f) + 32f
    }

    fun chooseFanLevelForTempF(tempF: Float, curve: String): Int {
        return when (curve.toLowerCase()) {
            "quiet" -> when {
                tempF < 95f -> 0
                else -> 1
            }
            "turbo" -> when {
                tempF < 100f -> 4
                else -> 5
            }
            else -> when {
                tempF < 100f -> 2
                else -> 3
            }
        }
    }

    fun chooseAutoFanLevelForTempF(tempF: Float): Int {
        return when {
            tempF < 95f -> 0
            tempF < 104f -> 1
            tempF < 113f -> 2
            tempF < 122f -> 3
            tempF < 131f -> 4
            else -> 5
        }
    }

    fun applyFanCurve(curve: String): Int? {
        val tempF = readTemperatureF() ?: return null
        val level = chooseFanLevelForTempF(tempF, curve)
        setFanLevel(level)
        return level
    }

    fun applyAutoFanCurve(): Int? {
        val tempF = readTemperatureF() ?: return null
        val level = chooseAutoFanLevelForTempF(tempF)
        setFanLevel(level)
        return level
    }

    fun readCpuModel(): String {
        return "Snapdragon 8 Elite Gen 5"
    }

    fun readRamInfo(): String {
        val memInfo = RootShell.execForOutput("cat /proc/meminfo 2>/dev/null") ?: return "Unknown"
        val totalLine = memInfo.lines().firstOrNull { it.startsWith("MemTotal:") } ?: return "Unknown"
        val kb = totalLine.substringAfter("MemTotal:").trim().substringBefore(" ").toLongOrNull() ?: return "Unknown"

        val gb = kb / 1024.0 / 1024.0
        val rounded = round(gb).toInt()
        val supported = listOf(12, 16, 24)
        val nearest = supported.minByOrNull { abs(it - rounded) } ?: rounded

        return "$nearest GB"
    }

    fun readShortRomFingerprint(): String {
        val fp = RootShell.execForOutput("getprop ro.build.fingerprint")?.trim().orEmpty()
        if (fp.isNotBlank()) return fp

        val displayId = RootShell.execForOutput("getprop ro.build.display.id")?.trim().orEmpty()
        if (displayId.isNotBlank()) return displayId

        val incremental = RootShell.execForOutput("getprop ro.build.version.incremental")?.trim().orEmpty()
        if (incremental.isNotBlank()) return incremental

        return "Unknown"
    }


    fun applyHardwareProfile(profile: HardwareProfile): Boolean {
        if (profile.fanEnabled) {
            setFanLevel(profile.fanLevel)
        } else {
            enableFan(false)
        }

        if (profile.pumpEnabled) {
            setPumpProfile(profile.pumpProfile)
        } else {
            enablePump(false)
        }

        if (profile.fanLedEnabled) {
            setFanLedEffect(profile.fanLedEffect, profile.fanLedColor)
        } else {
            setFanLedEnabled(false)
        }

        if (profile.logoLedEnabled) {
            setLogoLedEffect(profile.logoLedEffect, profile.logoLedColor)
        } else {
            setLogoLedEnabled(false)
        }

        if (profile.shoulderLedEnabled) {
            setShoulderLedEffect(profile.shoulderLedEffect, profile.shoulderLedColor)
        } else {
            setShoulderLedEnabled(false)
        }

        return true
    }

}
