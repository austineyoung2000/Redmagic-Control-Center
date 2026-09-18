package com.elitedarkkaiser.redmagic

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet

object DeviceTemperatureMonitor {
    private const val SCREEN_ON_INTERVAL_MS = 3_000L
    private const val SCREEN_OFF_INTERVAL_MS = 15_000L
    private const val READ_CACHE_MS = 750L

    private val lock = Any()
    private val listeners =
        CopyOnWriteArraySet<(Float?) -> Unit>()

    private var workerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var powerManager: PowerManager? = null
    private var thermalListener:
        PowerManager.OnThermalStatusChangedListener? = null

    @Volatile
    private var cachedTemperatureC: Float? = null

    @Volatile
    private var cachedAtMs = 0L

    /*
     * thermal_zone0 is cpullc-0-0 on the RedMagic 11 Pro.
     * It matches the sensor previously selected by the root
     * temperature command and preserves existing fan curves.
     */
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

    class Subscription internal constructor(
        private val listener: (Float?) -> Unit
    ) {
        fun close() {
            unsubscribe(listener)
        }
    }

    private val sampleRunnable = object : Runnable {
        override fun run() {
            val temperature =
                readTemperatureC(force = true)

            listeners.forEach { listener ->
                runCatching {
                    listener(temperature)
                }
            }

            synchronized(lock) {
                val activeHandler = handler ?: return
                if (listeners.isEmpty()) return

                activeHandler.postDelayed(
                    this,
                    nextIntervalMs()
                )
            }
        }
    }

    fun subscribe(
        context: Context,
        listener: (Float?) -> Unit
    ): Subscription {
        synchronized(lock) {
            listeners.add(listener)
            ensureStarted(context.applicationContext)

            handler?.removeCallbacks(sampleRunnable)
            handler?.post(sampleRunnable)
        }

        return Subscription(listener)
    }

    fun readTemperatureC(
        force: Boolean = false
    ): Float? {
        val now =
            android.os.SystemClock.elapsedRealtime()
        val cached = cachedTemperatureC

        if (
            !force &&
            cached != null &&
            now - cachedAtMs < READ_CACHE_MS
        ) {
            return cached
        }

        val fresh =
            temperaturePaths.firstNotNullOfOrNull { path ->
                runCatching {
                    normalizeTemperature(
                        File(path)
                            .readText()
                            .trim()
                            .toFloatOrNull()
                    )
                }.getOrNull()
            }

        if (fresh != null) {
            cachedTemperatureC = fresh
            cachedAtMs = now
            return fresh
        }

        return cached
    }

    private fun ensureStarted(context: Context) {
        if (handler != null) return

        val thread = HandlerThread(
            "RedMagicTemperature",
            android.os.Process.THREAD_PRIORITY_BACKGROUND
        ).apply {
            start()
        }

        workerThread = thread
        handler = Handler(thread.looper)
        powerManager =
            context.getSystemService(PowerManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val listener =
                PowerManager.OnThermalStatusChangedListener {
                    synchronized(lock) {
                        handler?.removeCallbacks(
                            sampleRunnable
                        )
                        handler?.post(sampleRunnable)
                    }
                }

            runCatching {
                powerManager?.addThermalStatusListener(
                    listener
                )
                thermalListener = listener
            }
        }
    }

    private fun unsubscribe(
        listener: (Float?) -> Unit
    ) {
        synchronized(lock) {
            listeners.remove(listener)
            if (listeners.isNotEmpty()) return

            handler?.removeCallbacksAndMessages(null)

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                thermalListener?.let { activeListener ->
                    runCatching {
                        powerManager
                            ?.removeThermalStatusListener(
                                activeListener
                            )
                    }
                }
            }

            thermalListener = null
            powerManager = null
            handler = null

            workerThread?.quitSafely()
            workerThread = null
        }
    }

    private fun nextIntervalMs(): Long {
        return if (
            powerManager?.isInteractive != false
        ) {
            SCREEN_ON_INTERVAL_MS
        } else {
            SCREEN_OFF_INTERVAL_MS
        }
    }

    private fun normalizeTemperature(
        raw: Float?
    ): Float? {
        return when {
            raw == null -> null

            raw > 1_000f && raw < 200_000f ->
                raw / 1_000f

            raw > 0f && raw < 200f ->
                raw

            else -> null
        }
    }
}
