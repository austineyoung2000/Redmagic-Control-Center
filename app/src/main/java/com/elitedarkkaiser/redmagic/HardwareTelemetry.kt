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
    private const val HARDWARE_CACHE_MS = 30_000L

    private val readLock = Any()

    private var cachedSnapshot:
        HardwareTelemetrySnapshot? = null

    private var cachedSnapshotAtMs = 0L

    fun read(): HardwareTelemetrySnapshot {
        return synchronized(readLock) {
            val now =
                android.os.SystemClock.elapsedRealtime()
            val cached = cachedSnapshot
            val temperature =
                DeviceTemperatureMonitor.readTemperatureC()

            if (
                cached != null &&
                now - cachedSnapshotAtMs <
                HARDWARE_CACHE_MS
            ) {
                return@synchronized cached.copy(
                    temperatureC = temperature
                )
            }

            /*
             * Only hardware values that require root remain
             * in this command. Temperature is read directly
             * through DeviceTemperatureMonitor.
             */
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
            }

            val output =
                RootShell.execForOutput(command)

            if (output == null) {
                return@synchronized (
                    cached ?: emptySnapshot()
                ).copy(
                    temperatureC = temperature
                )
            }

            val values = output.lineSequence()
                .mapNotNull { line ->
                    val separator = line.indexOf('=')

                    if (separator <= 0) {
                        null
                    } else {
                        line.substring(0, separator) to
                            line.substring(
                                separator + 1
                            ).trim()
                    }
                }
                .toMap()

            val fresh = HardwareTelemetrySnapshot(
                fanEnabled =
                    values["fan_enabled"]
                        ?.toIntOrNull()
                        ?.let { it != 0 },

                fanLevel =
                    values["fan_level"]
                        ?.toIntOrNull()
                        ?.coerceIn(0, 5),

                fanRpm =
                    values["fan_rpm"]
                        ?.toIntOrNull(),

                pumpEnabled =
                    values["pump_enabled"]
                        .nonBlankOrNull(),

                pumpFreq =
                    values["pump_freq"]
                        .nonBlankOrNull(),

                pumpSpeed =
                    values["pump_speed"]
                        .nonBlankOrNull(),

                temperatureC = temperature
            )

            cachedSnapshot = fresh
            cachedSnapshotAtMs = now
            fresh
        }
    }

    fun readTemperatureC(): Float? {
        return DeviceTemperatureMonitor
            .readTemperatureC()
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

    private fun String?.nonBlankOrNull(): String? {
        return this?.takeIf {
            it.isNotBlank()
        }
    }

    private fun emptySnapshot():
        HardwareTelemetrySnapshot {
        return HardwareTelemetrySnapshot(
            fanEnabled = null,
            fanLevel = null,
            fanRpm = null,
            pumpEnabled = null,
            pumpFreq = null,
            pumpSpeed = null,
            temperatureC =
                DeviceTemperatureMonitor
                    .readTemperatureC()
        )
    }
}
