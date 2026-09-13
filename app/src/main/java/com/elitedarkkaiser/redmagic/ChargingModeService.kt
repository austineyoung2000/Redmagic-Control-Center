package com.elitedarkkaiser.redmagic

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder

class ChargingModeService : Service() {

    private var lastEnabled: Boolean? = null
    private var lastCharging: Boolean? = null

    private lateinit var workerThread: HandlerThread
    private lateinit var handler: Handler

    private val evaluationLock = Any()
    private var forceNextEvaluation = false

    private val evaluationRunnable = Runnable {
        val force = synchronized(evaluationLock) {
            val requested = forceNextEvaluation
            forceNextEvaluation = false
            requested
        }

        evaluateChargingState(force)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            scheduleChargingEvaluation(force = false)
        }
    }

    override fun onCreate() {
        super.onCreate()

        workerThread = HandlerThread(
            "RedMagicChargingMode",
            android.os.Process.THREAD_PRIORITY_BACKGROUND
        ).apply {
            start()
        }
        handler = Handler(workerThread.looper)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }

        registerReceiver(receiver, filter)

        handler.post {
            ChargingLedRecovery.repairStaleChargingOwnership(
                this@ChargingModeService
            )
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        scheduleChargingEvaluation(force = true)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        runCatching {
            unregisterReceiver(receiver)
        }

        if (::handler.isInitialized) {
            handler.removeCallbacksAndMessages(null)
        }
        if (::workerThread.isInitialized) {
            workerThread.quitSafely()
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun scheduleChargingEvaluation(force: Boolean) {
        if (!::handler.isInitialized) return

        synchronized(evaluationLock) {
            forceNextEvaluation = forceNextEvaluation || force
        }

        handler.removeCallbacks(evaluationRunnable)
        handler.post(evaluationRunnable)
    }

    private fun evaluateChargingState(force: Boolean) {
        val enabled = ChargingLedState.isEnabled(this)
        val charging = ChargingLedState.isChargingNow(this)

        if (!force && lastEnabled == enabled && lastCharging == charging) {
            return
        }

        lastEnabled = enabled
        lastCharging = charging

        if (enabled && charging) {
            if (!ChargingLedState.isActive(this) || force) {
                ChargingLedState.setActive(this, true)
                ChargingLedState.applyChargingProfile(this)
            }
        } else {
            val wasActive = ChargingLedState.isActive(this)
            ChargingLedState.setActive(this, false)

            if (wasActive) {
                HardwareController.turnOffAllLeds()
                GameModeActions.startServiceSilentlyIfPermitted(this)
                HardwareServiceActions.startFanLed(this)
                HardwareServiceActions.enqueueFanLedRestore(this, delaySeconds = 1)
            }
        }
    }
}
