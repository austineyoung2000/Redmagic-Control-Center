package com.elitedarkkaiser.redmagic

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean

object DeviceTemperatureMonitor {
    private const val FOREGROUND_INTERVAL_MS =
        3_000L

    private const val HOT_BACKGROUND_INTERVAL_MS =
        5_000L

    private const val INTERACTIVE_BACKGROUND_INTERVAL_MS =
        15_000L

    private const val SCREEN_OFF_BACKGROUND_INTERVAL_MS =
        30_000L

    private const val READ_CACHE_MS = 750L

    /*
     * Begin faster background sampling slightly below the
     * first automatic cooling thresholds.
     */
    private const val HOT_BACKGROUND_THRESHOLD_C = 33f

    enum class SamplingMode {
        FOREGROUND,
        BACKGROUND_CONTROL
    }

    private data class Registration(
        val mode: SamplingMode,
        val listener: (Float?) -> Unit
    )

    private val lock = Any()

    private val listeners =
        CopyOnWriteArraySet<Registration>()

    private var workerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var powerManager: PowerManager? = null

    private var thermalListener:
        PowerManager.OnThermalStatusChangedListener? = null

    @Volatile
    private var cachedTemperatureC: Float? = null

    @Volatile
    private var cachedAtMs = 0L

    private val temperaturePaths by lazy {
        DeviceCompatibility.temperaturePaths()
    }

    class Subscription internal constructor(
        private val closeAction: () -> Unit
    ) {
        private val closed = AtomicBoolean(false)

        fun close() {
            if (closed.compareAndSet(false, true)) {
                closeAction()
            }
        }
    }

    private val sampleRunnable =
        object : Runnable {
            override fun run() {
                val temperature =
                    readTemperatureC(force = true)

                TemperatureHistory.record(temperature)

                listeners.forEach { registration ->
                    runCatching {
                        registration.listener(
                            temperature
                        )
                    }
                }

                synchronized(lock) {
                    val activeHandler =
                        handler ?: return

                    if (listeners.isEmpty()) {
                        return
                    }

                    activeHandler.postDelayed(
                        this,
                        nextIntervalMs(temperature)
                    )
                }
            }
        }

    fun subscribe(
        context: Context,
        mode: SamplingMode =
            SamplingMode.BACKGROUND_CONTROL,
        listener: (Float?) -> Unit
    ): Subscription {
        val registration =
            Registration(
                mode = mode,
                listener = listener
            )

        synchronized(lock) {
            listeners.add(registration)

            ensureStarted(
                context.applicationContext
            )

            /*
             * A newly visible consumer receives a fresh sample
             * immediately instead of waiting for the previous
             * background interval.
             */
            handler?.removeCallbacks(sampleRunnable)
            handler?.post(sampleRunnable)
        }

        return Subscription {
            unsubscribe(registration)
        }
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
            temperaturePaths.firstNotNullOfOrNull {
                path ->
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

    private fun ensureStarted(
        context: Context
    ) {
        if (handler != null) {
            return
        }

        val thread = HandlerThread(
            "RedMagicTemperature",
            android.os.Process
                .THREAD_PRIORITY_BACKGROUND
        ).apply {
            start()
        }

        workerThread = thread
        handler = Handler(thread.looper)

        powerManager =
            context.getSystemService(
                PowerManager::class.java
            )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {
            val listener =
                PowerManager
                    .OnThermalStatusChangedListener {
                        synchronized(lock) {
                            handler?.removeCallbacks(
                                sampleRunnable
                            )
                            handler?.post(
                                sampleRunnable
                            )
                        }
                    }

            runCatching {
                powerManager
                    ?.addThermalStatusListener(
                        listener
                    )
                thermalListener = listener
            }
        }
    }

    private fun unsubscribe(
        registration: Registration
    ) {
        synchronized(lock) {
            listeners.remove(registration)

            if (listeners.isNotEmpty()) {
                handler?.removeCallbacks(
                    sampleRunnable
                )
                handler?.postDelayed(
                    sampleRunnable,
                    nextIntervalMs(
                        cachedTemperatureC
                    )
                )
                return
            }

            handler?.removeCallbacksAndMessages(null)

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                thermalListener?.let {
                    activeListener ->
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

    private fun nextIntervalMs(
        temperatureC: Float?
    ): Long {
        val interactive =
            powerManager?.isInteractive != false

        val foregroundRequested =
            interactive &&
                listeners.any {
                    it.mode ==
                        SamplingMode.FOREGROUND
                }

        if (foregroundRequested) {
            return FOREGROUND_INTERVAL_MS
        }

        if (
            temperatureC != null &&
            temperatureC >=
            HOT_BACKGROUND_THRESHOLD_C
        ) {
            return HOT_BACKGROUND_INTERVAL_MS
        }

        return if (interactive) {
            INTERACTIVE_BACKGROUND_INTERVAL_MS
        } else {
            SCREEN_OFF_BACKGROUND_INTERVAL_MS
        }
    }

    private fun normalizeTemperature(
        raw: Float?
    ): Float? {
        return when {
            raw == null -> null

            raw > 1_000f &&
                raw < 200_000f ->
                raw / 1_000f

            raw > 0f &&
                raw < 200f ->
                raw

            else -> null
        }
    }
}
