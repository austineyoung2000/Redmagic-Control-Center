package com.elitedarkkaiser.redmagic.hardware

import android.os.Build
import com.elitedarkkaiser.redmagic.DeviceCompatibility
import com.elitedarkkaiser.redmagic.HardwareController
import com.elitedarkkaiser.redmagic.state.DeviceState

object DeviceDetector {
    val REQUIRED_MODEL =
        DeviceCompatibility.REQUIRED_MODEL

    fun readState(): DeviceState {
        val identity = DeviceCompatibility.identity()
        val buildModel =
            identity.buildModel.ifBlank { "Unknown" }
        val productModel =
            identity.productModel.ifBlank { "Unknown" }
        val vendorModel =
            identity.vendorModel.ifBlank { "Unknown" }
        val marketName =
            identity.marketName.ifBlank { "Unknown" }

        return DeviceState(
            supported = identity.supported,
            rooted = RootProvider.hasRoot(),
            model = buildModel,
            productModel = productModel,
            vendorModel = vendorModel,
            marketName = marketName,
            fingerprint = Build.FINGERPRINT ?: "Unknown",
            cpuModel = HardwareController.readCpuModel(),
            ramText = HardwareController.readRamInfo()
        )
    }

    fun isSupportedDevice(): Boolean {
        return DeviceCompatibility.isSupportedDevice()
    }
}
