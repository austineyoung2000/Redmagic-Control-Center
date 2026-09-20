package com.elitedarkkaiser.redmagic

import android.content.Context

object DeviceScanActions {
    fun runBackgroundScan(context: Context, force: Boolean = false) {
        if (!DeviceCompatibility.isSupportedDevice()) return
        if (!force && hasDeviceCapabilityReportStorage(context)) return

        Thread {
            val report = DeviceCapabilityScanner.scan()
            saveDeviceCapabilityReportStorage(context, report)
        }.start()
    }
}
