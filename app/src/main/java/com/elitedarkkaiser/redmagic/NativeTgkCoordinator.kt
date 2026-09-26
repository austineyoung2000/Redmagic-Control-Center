package com.elitedarkkaiser.redmagic

import android.content.Context
import android.os.Build
import android.util.Size
import android.view.WindowManager

object NativeTgkRuntimeState {
    private val lock = Any()

    @Volatile
    private var activePackageName: String? = null

    @Volatile
    private var activeOrientation:
        NativeTgkOrientation? = null

    fun isActive(): Boolean {
        return activePackageName != null
    }

    fun activePackage(): String? {
        return activePackageName
    }

    fun matches(
        packageName: String,
        orientation: NativeTgkOrientation
    ): Boolean {
        return activePackageName == packageName &&
            activeOrientation == orientation
    }

    fun markActive(
        packageName: String,
        orientation: NativeTgkOrientation
    ) {
        synchronized(lock) {
            activePackageName = packageName
            activeOrientation = orientation
        }
    }

    fun clear() {
        synchronized(lock) {
            activePackageName = null
            activeOrientation = null
        }
    }

    fun clearIfMatches(
        packageName: String,
        orientation: NativeTgkOrientation
    ) {
        synchronized(lock) {
            if (
                activePackageName == packageName &&
                activeOrientation == orientation
            ) {
                activePackageName = null
                activeOrientation = null
            }
        }
    }
}

object NativeTgkCoordinator {
    private const val TAG = "RedmagicNativeTgk"

    fun currentOrientation(
        context: Context
    ): NativeTgkOrientation {
        val size = currentDisplaySize(context)

        return if (size.width >= size.height) {
            NativeTgkOrientation.LANDSCAPE
        } else {
            NativeTgkOrientation.PORTRAIT
        }
    }

    fun hasReadyMapping(
        context: Context,
        packageName: String,
        orientation: NativeTgkOrientation
    ): Boolean {
        val profile = NativeTgkStorage.getProfile(
            context,
            packageName
        ) ?: return false

        return profile.enabled &&
            profile.hasCompleteMapping(orientation)
    }

    fun applyForegroundMapping(
        context: Context,
        packageName: String,
        orientation: NativeTgkOrientation
    ): NativeTgkApplyResult {
        val profile = NativeTgkStorage.getProfile(
            context,
            packageName
        )

        if (profile == null || !profile.enabled) {
            return NativeTgkApplyResult(
                success = false,
                backend = null,
                state = null,
                message = "No enabled mapping for $packageName"
            )
        }

        val mapping = profile.mappingFor(orientation)
        if (mapping?.isComplete() != true) {
            return NativeTgkApplyResult(
                success = false,
                backend = null,
                state = null,
                message = "No complete $orientation mapping"
            )
        }

        val displaySize = currentDisplaySize(context)

        val result = NativeTgkBridge.applyMapping(
            context = context,
            mapping = mapping,
            displayWidth = displaySize.width,
            displayHeight = displaySize.height,
            hapticsEnabled = profile.hapticsEnabled
        )

        if (result.success) {
            android.util.Log.i(
                TAG,
                "Applied $orientation TGK mapping " +
                    "for $packageName using ${result.backend}"
            )
        } else {
            android.util.Log.e(
                TAG,
                "Failed to apply TGK mapping for " +
                    "$packageName: ${result.message}"
            )
        }

        return result
    }

    fun disable(
        context: Context,
        reason: String
    ): NativeTgkApplyResult {
        val result = NativeTgkBridge.disable(context)

        if (result.success) {
            android.util.Log.i(
                TAG,
                "Disabled native TGK: $reason"
            )
        } else {
            android.util.Log.e(
                TAG,
                "Native TGK cleanup failed: " +
                    "$reason: ${result.message}"
            )
        }

        return result
    }

    private fun currentDisplaySize(
        context: Context
    ): Size {
        val manager = context.getSystemService(
            WindowManager::class.java
        ) ?: error("WindowManager unavailable")

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = manager.currentWindowMetrics.bounds
            Size(bounds.width(), bounds.height())
        } else {
            @Suppress("DEPRECATION")
            val metrics = android.util.DisplayMetrics()

            @Suppress("DEPRECATION")
            manager.defaultDisplay.getRealMetrics(metrics)

            Size(
                metrics.widthPixels,
                metrics.heightPixels
            )
        }
    }
}
