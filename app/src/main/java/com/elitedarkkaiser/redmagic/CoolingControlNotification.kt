package com.elitedarkkaiser.redmagic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Build

object CoolingControlNotification {
    const val NOTIFICATION_ID = 1101

    private const val CHANNEL_ID = "cooling_control_channel"

    private var fanActive = false
    private var pumpActive = false
    private var rgbActive = false
    private var fanLevel: Int? = null
    private var pumpProfile: String? = null
    private var rgbMode: String? = null
    private var temperatureF: Float? = null
    private var lastRenderedText: String? = null

    @Synchronized
    fun startFan(context: Context): Notification {
        fanActive = true
        createChannel(context)
        return buildNotification(context)
    }

    @Synchronized
    fun startPump(context: Context): Notification {
        pumpActive = true
        createChannel(context)
        return buildNotification(context)
    }

    @Synchronized
    fun startRgb(context: Context): Notification {
        rgbActive = true
        createChannel(context)
        return buildNotification(context)
    }

    @Synchronized
    fun updateFan(
        context: Context,
        tempF: Float?,
        level: Int?
    ) {
        fanActive = true
        temperatureF = tempF ?: temperatureF
        fanLevel = level?.takeIf { it >= 0 }
        publish(context)
    }

    @Synchronized
    fun updatePump(
        context: Context,
        tempF: Float?,
        profile: String?
    ) {
        pumpActive = true
        temperatureF = tempF ?: temperatureF
        pumpProfile = profile
        publish(context)
    }

    @Synchronized
    fun updateRgb(
        context: Context,
        mode: String
    ) {
        rgbActive = true
        rgbMode = mode
        publish(context)
    }

    @Synchronized
    fun stopFan(service: Service) {
        detach(service)
        fanActive = false
        fanLevel = null
        finishStop(service)
    }

    @Synchronized
    fun stopPump(service: Service) {
        detach(service)
        pumpActive = false
        pumpProfile = null
        finishStop(service)
    }

    @Synchronized
    fun stopRgb(service: Service) {
        detach(service)
        rgbActive = false
        rgbMode = null
        finishStop(service)
    }

    private fun finishStop(context: Context) {
        if (fanActive || pumpActive || rgbActive) {
            lastRenderedText = null
            publish(context)
        } else {
            lastRenderedText = null
            temperatureF = null
            manager(context).cancel(NOTIFICATION_ID)
        }
    }

    private fun publish(context: Context) {
        createChannel(context)

        val text = buildStatusText(context)
        if (text == lastRenderedText) return

        lastRenderedText = text
        manager(context).notify(
            NOTIFICATION_ID,
            buildNotification(context, text)
        )
    }

    private fun buildNotification(
        context: Context,
        text: String = buildStatusText(context)
    ): Notification {
        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CHANNEL_ID)
            } else {
                Notification.Builder(context)
            }

        return builder
            .setContentTitle("Redmagic HW Controls")
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun buildStatusText(
        context: Context
    ): String {
        val parts = mutableListOf<String>()

        if (fanActive) {
            parts += fanLevel?.let {
                "Fan: Level $it"
            } ?: "Fan: Starting"
        }

        if (pumpActive) {
            val profile = pumpProfile?.replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase()
                } else {
                    it.toString()
                }
            }

            parts += profile?.let {
                "Pump: $it"
            } ?: "Pump: Starting"
        }

        if (rgbActive) {
            parts += rgbMode?.let {
                "RGB: $it"
            } ?: "RGB: Starting"
        }

        temperatureF?.let {
            parts += "Temp: " +
                TempFormat.formatDisplayTempFromF(
                    it,
                    isUseFahrenheitStorage(context)
                )
        }

        return parts.joinToString(" • ").ifEmpty {
            "Cooling controls starting"
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Cooling Controls",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description =
                "Shows cooling hardware and RGB Studio status"
        }

        manager(context).createNotificationChannel(channel)
    }

    private fun detach(service: Service) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            service.stopForeground(
                Service.STOP_FOREGROUND_DETACH
            )
        } else {
            @Suppress("DEPRECATION")
            service.stopForeground(false)
        }
    }

    private fun manager(context: Context): NotificationManager =
        context.getSystemService(NotificationManager::class.java)
}
