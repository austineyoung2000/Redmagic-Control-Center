package com.elitedarkkaiser.redmagic

data class HardwareTelemetrySnapshot(
    val fanEnabled: Boolean?,
    val fanLevel: Int?,
    val fanRpm: Int?,
    val pumpEnabled: String?,
    val pumpFreq: String?,
    val pumpSpeed: String?,
    val temperatureC: Float?
) {
    val temperatureF: Float?
        get() = temperatureC?.let {
            (it * 9f / 5f) + 32f
        }
}

object HardwareTelemetry {
    private const val CACHE_MS = 15_000L

    private val readLock = Any()

    private var cachedSnapshot: HardwareTelemetrySnapshot? = null
    private var cachedSnapshotAtMs = 0L
    private var cachedTemperatureC: Float? = null
    private var cachedTemperatureAtMs = 0L

    private val temperaturePaths = listOf(
        "/sys/class/thermal/thermal_zone0/temp",
        "/sys/class/thermal/thermal_zone1/temp",
        "/sys/class/thermal/thermal_zone2/temp",
        "/sys/class/thermal/thermal_zone3/temp",
        "/sys/devices/virtual/thermal/thermal_zone0/temp",
        "/sys/devices/virtual/thermal/thermal_zone1/temp",
        "/sys/devices/virtual/thermal/thermal_zone2/temp",
        "/sys/devices/virtual/thermal/thermal_zone3/temp"
    )

    fun read(): HardwareTelemetrySnapshot {
        return synchronized(readLock) {
            val now = android.os.SystemClock.elapsedRealtime()
            val cached = cachedSnapshot

            if (
                cached != null &&
                now - cachedSnapshotAtMs < CACHE_MS
            ) {
                return@synchronized cached
            }

            val command = buildString {
                appendRead(
                    "fan_enabled",
                    "/sys/kernel/fan/fan_enable"
                )
                appendRead(
                    "fan_level",
                    "/sys/kernel/fan/fan_speed_level"
                )
                appendRead(
                    "fan_rpm",
                    "/sys/kernel/fan/fan_speed_count"
                )
                appendRead(
                    "pump_enabled",
                    "/proc/driver/micropump/enable"
                )
                appendRead(
                    "pump_freq",
                    "/proc/driver/micropump/freq"
                )
                appendRead(
                    "pump_speed",
                    "/proc/driver/micropump/speed"
                )

                append("temperature=''; ")
                append("for path in ")

                temperaturePaths.forEach {
                    append("'$it' ")
                }

                append("; do ")
                append(
                    "value=\$(cat \"\$path\" 2>/dev/null); "
                )
                append("if [ -n \"\$value\" ]; then ")
                append(
                    "temperature=\"\$value\"; break; fi; done; "
                )
                append(
                    "printf 'temperature=%s\\n' " +
                        "\"\$temperature\"; "
                )
            }

            val output = RootShell.execForOutput(command)

            if (output == null) {
                return@synchronized cached ?: emptySnapshot()
            }

            val values = output.lineSequence()
                .mapNotNull { line ->
                    val separator = line.indexOf('=')

                    if (separator <= 0) {
                        null
                    } else {
                        line.substring(0, separator) to
                            line.substring(separator + 1).trim()
                    }
                }
                .toMap()

            val temperature = normalizeTemperature(
                values["temperature"]?.toFloatOrNull()
            ) ?: cachedTemperatureC.takeIf {
                now - cachedTemperatureAtMs < CACHE_MS
            }

            val fresh = HardwareTelemetrySnapshot(
                fanEnabled = values["fan_enabled"]
                    ?.toIntOrNull()
                    ?.let { it != 0 },
                fanLevel = values["fan_level"]
                    ?.toIntOrNull()
                    ?.coerceIn(0, 5),
                fanRpm = values["fan_rpm"]?.toIntOrNull(),
                pumpEnabled =
                    values["pump_enabled"].nonBlankOrNull(),
                pumpFreq =
                    values["pump_freq"].nonBlankOrNull(),
                pumpSpeed =
                    values["pump_speed"].nonBlankOrNull(),
                temperatureC = temperature
            )

            cachedSnapshot = fresh
            cachedSnapshotAtMs = now

            if (temperature != null) {
                cachedTemperatureC = temperature
                cachedTemperatureAtMs = now
            }

            fresh
        }
    }

    fun readTemperatureC(): Float? {
        return synchronized(readLock) {
            val now = android.os.SystemClock.elapsedRealtime()
            val cached = cachedTemperatureC

            if (
                cached != null &&
                now - cachedTemperatureAtMs < CACHE_MS
            ) {
                cached
            } else {
                null
            }
        } ?: read().temperatureC
    }

    fun invalidateHardwareCache() {
        synchronized(readLock) {
            cachedSnapshotAtMs = 0L
        }
    }

    private fun StringBuilder.appendRead(
        label: String,
        path: String
    ) {
        append(
            "value=\$(cat '$path' 2>/dev/null); "
        )
        append(
            "printf '$label=%s\\n' \"\$value\"; "
        )
    }

    private fun normalizeTemperature(
        raw: Float?
    ): Float? {
        return when {
            raw == null -> null
            raw > 1000f && raw < 200000f ->
                raw / 1000f
            raw > 0f && raw < 200f ->
                raw
            else -> null
        }
    }

    private fun String?.nonBlankOrNull(): String? {
        return this?.takeIf { it.isNotBlank() }
    }

    private fun emptySnapshot(): HardwareTelemetrySnapshot {
        return HardwareTelemetrySnapshot(
            fanEnabled = null,
            fanLevel = null,
            fanRpm = null,
            pumpEnabled = null,
            pumpFreq = null,
            pumpSpeed = null,
            temperatureC = cachedTemperatureC
        )
    }
}
