package com.elitedarkkaiser.redmagic

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder

class AutoPumpService : Service() {

    companion object {
        private const val HOT_POLL_MS = 15000L
        private const val COOL_POLL_MS = 60000L
        private const val HOT_TEMP_THRESHOLD_F = 95f
    }

    private lateinit var workerThread: HandlerThread
    private lateinit var handler: Handler
    private var lastProfile: String? = null

    private val pollRunnable = object : Runnable {
        override fun run() {
            val tempF = applyPumpRule()
            CoolingControlNotification.updatePump(
                this@AutoPumpService,
                tempF,
                lastProfile
            )
            val nextDelay = if ((tempF ?: 0f) >= HOT_TEMP_THRESHOLD_F) HOT_POLL_MS else COOL_POLL_MS
            handler.postDelayed(this, nextDelay)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(
            CoolingControlNotification.NOTIFICATION_ID,
            CoolingControlNotification.startPump(this)
        )

        workerThread = HandlerThread(
            "RedMagicAutoPump",
            android.os.Process.THREAD_PRIORITY_BACKGROUND
        ).apply {
            start()
        }
        handler = Handler(workerThread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        if (::handler.isInitialized) {
            handler.removeCallbacksAndMessages(null)
        }
        if (::workerThread.isInitialized) {
            workerThread.quitSafely()
        }
        CoolingControlNotification.stopPump(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun applyPumpRule(): Float? {
        val tempF = DashboardSnapshot.readCpuTempF().toFloatOrNull() ?: return null

        if (!HardwareScreenPolicy.isScreenInteractive(this@AutoPumpService)) {
            if (HardwareScreenPolicy.coolingShouldStopWhileScreenOff(tempF)) {
                if (lastProfile != "off") {
                    HardwareController.enablePump(false)
                }
                lastProfile = "off"
                return tempF
            }
        }

        val profile = when {
            tempF >= 105f -> "quick"
            tempF >= 95f -> "medium"
            else -> "slow"
        }

        if (profile != lastProfile) {
            if (HardwareScreenPolicy.blockCoolingWhileScreenOffUnlessHot(this@AutoPumpService, "auto-pump-screen-off")) {
                lastProfile = "off"
                return tempF
            }
            HardwareController.setPumpProfile(profile)
            lastProfile = profile

        }

        return tempF
    }
}
