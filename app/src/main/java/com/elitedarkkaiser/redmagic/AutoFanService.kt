package com.elitedarkkaiser.redmagic

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder

class AutoFanService : Service() {

    companion object {
        private const val HOT_POLL_MS = 15000L
        private const val COOL_POLL_MS = 60000L
        private const val HOT_TEMP_THRESHOLD_F = 95f
        private const val HYSTERESIS_F = 5f
    }

    private lateinit var workerThread: HandlerThread
    private lateinit var handler: Handler
    private var lastAppliedLevel = -1

    private val loop = object : Runnable {
        override fun run() {
            val tempF = HardwareController.readTemperatureF()
            val nextLevel = chooseStableFanLevel(tempF, lastAppliedLevel)

            if (!HardwareScreenPolicy.isScreenInteractive(this@AutoFanService)) {
                if (HardwareScreenPolicy.coolingShouldStopWhileScreenOff(tempF)) {
                    if (lastAppliedLevel != 0) {
                        HardwareController.enableFan(false)
                    }
                    lastAppliedLevel = 0
                    updateNotification(tempF, lastAppliedLevel)
                    handler.postDelayed(this, COOL_POLL_MS)
                    return
                }
            }

            if (nextLevel != null && nextLevel != lastAppliedLevel) {
                if (HardwareScreenPolicy.blockCoolingWhileScreenOffUnlessHot(this@AutoFanService, "auto-fan-screen-off")) {
                    lastAppliedLevel = 0
                    updateNotification(tempF, lastAppliedLevel)
                    handler.postDelayed(this, COOL_POLL_MS)
                    return
                }
                HardwareController.setFanLevel(nextLevel)
                lastAppliedLevel = nextLevel
            }

            updateNotification(tempF, lastAppliedLevel)
            val nextDelay = if ((tempF ?: 0f) >= HOT_TEMP_THRESHOLD_F) HOT_POLL_MS else COOL_POLL_MS
            handler.postDelayed(this, nextDelay)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(
            CoolingControlNotification.NOTIFICATION_ID,
            CoolingControlNotification.startFan(this)
        )

        workerThread = HandlerThread(
            "RedMagicAutoFan",
            android.os.Process.THREAD_PRIORITY_BACKGROUND
        ).apply {
            start()
        }
        handler = Handler(workerThread.looper)

        handler.post {
            lastAppliedLevel = HardwareController.readFanLevel() ?: -1
            loop.run()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        if (::handler.isInitialized) {
            handler.removeCallbacksAndMessages(null)
        }
        if (::workerThread.isInitialized) {
            workerThread.quitSafely()
        }
        CoolingControlNotification.stopFan(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun chooseStableFanLevel(tempF: Float?, currentLevel: Int): Int? {
        if (tempF == null) return currentLevel.takeIf { it >= 0 }

        val baseLevel = HardwareController.chooseAutoFanLevelForTempF(tempF)

        if (currentLevel < 0) return baseLevel

        if (baseLevel > currentLevel) {
            return baseLevel
        }

        if (baseLevel < currentLevel) {
            val holdThreshold = when (currentLevel) {
                5 -> 131f - HYSTERESIS_F
                4 -> 122f - HYSTERESIS_F
                3 -> 113f - HYSTERESIS_F
                2 -> 104f - HYSTERESIS_F
                1 -> 95f - HYSTERESIS_F
                else -> Float.MIN_VALUE
            }

            return if (tempF < holdThreshold) baseLevel else currentLevel
        }

        return currentLevel
    }

    private fun updateNotification(
        tempF: Float?,
        level: Int?
    ) {
        CoolingControlNotification.updateFan(
            this,
            tempF,
            level
        )
    }
}
