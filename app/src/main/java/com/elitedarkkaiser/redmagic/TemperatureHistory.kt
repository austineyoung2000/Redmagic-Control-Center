package com.elitedarkkaiser.redmagic

import android.os.SystemClock

object TemperatureHistory {
    private const val HISTORY_WINDOW_MS =
        30L * 60L * 1_000L
    private const val MAX_SAMPLES = 600
    private const val MIN_SAMPLE_SPACING_MS = 500L

    data class Sample(
        val elapsedRealtimeMs: Long,
        val temperatureC: Float
    )

    private val lock = Any()
    private val samples = ArrayDeque<Sample>()

    fun record(temperatureC: Float?) {
        if (
            temperatureC == null ||
            !temperatureC.isFinite() ||
            temperatureC !in 0f..150f
        ) {
            return
        }

        val now = SystemClock.elapsedRealtime()

        synchronized(lock) {
            val previous = samples.lastOrNull()
            if (
                previous != null &&
                now - previous.elapsedRealtimeMs <
                    MIN_SAMPLE_SPACING_MS
            ) {
                return
            }

            samples.addLast(
                Sample(
                    elapsedRealtimeMs = now,
                    temperatureC = temperatureC
                )
            )

            val oldestAllowed =
                now - HISTORY_WINDOW_MS

            while (
                samples.isNotEmpty() &&
                (
                    samples.first()
                        .elapsedRealtimeMs <
                        oldestAllowed ||
                        samples.size > MAX_SAMPLES
                )
            ) {
                samples.removeFirst()
            }
        }
    }

    fun snapshot(): List<Sample> {
        return synchronized(lock) {
            samples.toList()
        }
    }
}
