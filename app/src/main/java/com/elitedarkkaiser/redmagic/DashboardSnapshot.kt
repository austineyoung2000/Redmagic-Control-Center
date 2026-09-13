package com.elitedarkkaiser.redmagic

import android.content.Context
import android.os.Build
import android.app.usage.UsageStatsManager

object DashboardSnapshot {

    private fun read(path: String): String? {
        return RootShell.execForOutput("cat $path")?.trim()?.ifEmpty { null }
    }

    fun readFanEnabled(): String = read("/sys/kernel/fan/fan_enable") ?: "?"
    fun readFanRpm(): String = read("/sys/kernel/fan/fan_speed_count") ?: "?"
    fun readFanLevel(): String = read("/sys/kernel/fan/fan_speed_level") ?: "?"
    fun readPumpEnabled(): String = read("/proc/driver/micropump/enable") ?: "?"
    fun readPumpFreq(): String = read("/proc/driver/micropump/freq") ?: "?"
    fun readPumpSpeed(): String = read("/proc/driver/micropump/speed") ?: "?"

    fun readCpuTempC(): String {
        val temperature =
            HardwareController.readTemperatureC()
                ?: return "?"

        return String.format("%.1f", temperature)
    }

    fun readCpuTempF(): String {
        val temperature =
            HardwareController.readTemperatureF()
                ?: return "?"

        return String.format("%.1f", temperature)
    }

    private fun readHardwareValues(
        paths: List<String>
    ): List<String> {
        val command = buildString {
            for (path in paths) {
                append(
                    "value=\$(cat '$path' 2>/dev/null); "
                )
                append(
                    "if [ -n \"\$value\" ]; then " +
                        "printf '%s\\n' \"\$value\"; " +
                        "else echo '?'; fi; "
                )
            }
        }

        val output =
            RootShell.execForOutput(command)
                ?: return List(paths.size) { "?" }

        val values = output.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        return List(paths.size) { index ->
            values.getOrNull(index) ?: "?"
        }
    }

    fun currentForegroundPackage(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - 60_000L,
            now
        )
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    fun buildSummary(context: Context): String {
        val paths = listOf(
            "/sys/kernel/fan/fan_enable",
            "/sys/kernel/fan/fan_speed_level",
            "/sys/kernel/fan/fan_speed_count",
            "/proc/driver/micropump/enable",
            "/proc/driver/micropump/freq",
            "/proc/driver/micropump/speed"
        )
        val values = readHardwareValues(paths)

        val fanEnabled = values[0]
        val fanLevel = values[1]
        val fanRpm = values[2]
        val pumpEnabled = values[3]
        val pumpFreq = values[4]
        val pumpSpeed = values[5]

        val rooted =
            hasCachedRootAccessStorage(context) ||
                RootShell.hasRoot()
        val temperatureF = readCpuTempF()
        val foregroundPackage =
            currentForegroundPackage(context)
                ?: "Unavailable"

        return buildString {
            append("Model: ${Build.MODEL ?: "Unknown"}\n")
            append(
                "Root: ${if (rooted) "Granted" else "Missing"}\n"
            )
            append("CPU Temp: $temperatureF°F\n")
            append(
                "Fan: $fanEnabled • Level $fanLevel • " +
                    "$fanRpm RPM\n"
            )
            append(
                "Pump: $pumpEnabled • Freq $pumpFreq • " +
                    "Speed $pumpSpeed\n"
            )
            append("Foreground app: $foregroundPackage")
        }
    }
}
