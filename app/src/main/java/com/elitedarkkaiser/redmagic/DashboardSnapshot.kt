package com.elitedarkkaiser.redmagic

import android.content.Context
import android.os.Build
import android.app.usage.UsageStatsManager

object DashboardSnapshot {

    private const val HARDWARE_CACHE_MS = 2_000L

    private val hardwareReadLock = Any()

    private val hardwarePaths = listOf(
        "/sys/kernel/fan/fan_enable",
        "/sys/kernel/fan/fan_speed_level",
        "/sys/kernel/fan/fan_speed_count",
        "/proc/driver/micropump/enable",
        "/proc/driver/micropump/freq",
        "/proc/driver/micropump/speed"
    )

    private data class HardwareSnapshot(
        val fanEnabled: String,
        val fanLevel: String,
        val fanRpm: String,
        val pumpEnabled: String,
        val pumpFreq: String,
        val pumpSpeed: String
    )

    private var cachedHardwareSnapshot: HardwareSnapshot? = null
    private var cachedHardwareAtMs = 0L

    private fun readHardwareSnapshot(): HardwareSnapshot {
        return synchronized(hardwareReadLock) {
            val now =
                android.os.SystemClock.elapsedRealtime()
            val cached = cachedHardwareSnapshot

            if (
                cached != null &&
                (now - cachedHardwareAtMs) < HARDWARE_CACHE_MS
            ) {
                return@synchronized cached
            }

            val values = readHardwareValues(hardwarePaths)
            val fresh = HardwareSnapshot(
                fanEnabled = values[0],
                fanLevel = values[1],
                fanRpm = values[2],
                pumpEnabled = values[3],
                pumpFreq = values[4],
                pumpSpeed = values[5]
            )

            cachedHardwareSnapshot = fresh
            cachedHardwareAtMs =
                android.os.SystemClock.elapsedRealtime()

            fresh
        }
    }

    fun readFanEnabled(): String =
        readHardwareSnapshot().fanEnabled

    fun readFanRpm(): String =
        readHardwareSnapshot().fanRpm

    fun readFanLevel(): String =
        readHardwareSnapshot().fanLevel

    fun readPumpEnabled(): String =
        readHardwareSnapshot().pumpEnabled

    fun readPumpFreq(): String =
        readHardwareSnapshot().pumpFreq

    fun readPumpSpeed(): String =
        readHardwareSnapshot().pumpSpeed

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
        val hardware = readHardwareSnapshot()

        val fanEnabled = hardware.fanEnabled
        val fanLevel = hardware.fanLevel
        val fanRpm = hardware.fanRpm
        val pumpEnabled = hardware.pumpEnabled
        val pumpFreq = hardware.pumpFreq
        val pumpSpeed = hardware.pumpSpeed

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
