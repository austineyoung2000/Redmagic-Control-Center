package com.elitedarkkaiser.redmagic

object MagicKeyActions {
    fun readModeValue(): Int {
        return RootShell.execForOutput(
            "settings get system fourth_physical_key_function_value"
        )?.trim()?.toIntOrNull() ?: -1
    }

    fun readModeLabel(): String {
        return when (readModeValue()) {
            1 -> "Camera"
            2 -> "GameSpace"
            3 -> "Sound Mode"
            4 -> "Flashlight"
            5 -> "Voice Recorder"
            16 -> "Launch App"
            17 -> "Launch Shortcut"
            0 -> "Disabled"
            else -> "Unknown"
        }
    }
    fun resolveAppLabel(context: android.content.Context, pkg: String?): String {
        if (pkg.isNullOrBlank()) return "Choose App"
        return try {
            val appInfo = context.packageManager.getApplicationInfo(pkg, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Throwable) {
            pkg
        }
    }

    fun applyStockMode(
        activity: android.app.Activity,
        label: String,
        applyMode: () -> Boolean,
        statusLabel: android.widget.TextView,
        sliderButton: android.widget.Button? = null,
        shortcutButton: android.widget.Button? = null,
        runBackground: (() -> Unit) -> Boolean,
        refreshStatus: () -> Unit
    ) {
        statusLabel.text = "Current: Applying $label…"

        val submitted = runBackground {
            SliderDualAppStorage.disable(activity)
            val ok = applyMode()

            statusLabel.post {
                if (activity.isFinishing || activity.isDestroyed) {
                    return@post
                }

                if (ok) {
                    saveMagicKeyAppPackageStorage(activity, null)
                    saveMagicKeyShortcutStorage(activity, null)
                    sliderButton?.text =
                        "MAGIC KEY APP: Choose App"
                    shortcutButton?.text =
                        "MAGIC KEY SHORTCUT: Choose Shortcut"
                    statusLabel.text = "Current: $label"
                    refreshStatus()

                    android.widget.Toast.makeText(
                        activity,
                        "Magic Key set to $label",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    statusLabel.text =
                        "Current: Failed to apply $label"

                    android.widget.Toast.makeText(
                        activity,
                        "Failed to set Magic Key to $label",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        if (!submitted) {
            statusLabel.text = "Current: Unable to update"

            android.widget.Toast.makeText(
                activity,
                "Unable to start the Magic Key update",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun applyLaunchAppMode(
        activity: android.app.Activity,
        pkg: String,
        label: String,
        statusLabel: android.widget.TextView,
        sliderButton: android.widget.Button,
        shortcutButton: android.widget.Button? = null,
        runBackground: (() -> Unit) -> Boolean,
        refreshStatus: () -> Unit
    ) {
        statusLabel.text = "Current: Applying Launch App…"
        sliderButton.isEnabled = false

        val submitted = runBackground {
            SliderDualAppStorage.disable(activity)
            val ok = HardwareController.setSliderLaunchApp(pkg)

            statusLabel.post {
                if (activity.isFinishing || activity.isDestroyed) {
                    return@post
                }

                sliderButton.isEnabled = true

                if (ok) {
                    saveMagicKeyAppPackageStorage(activity, pkg)
                    saveMagicKeyShortcutStorage(activity, null)
                    sliderButton.text = "MAGIC KEY APP: $label"
                    shortcutButton?.text =
                        "MAGIC KEY SHORTCUT: Choose Shortcut"
                    statusLabel.text = "Current: Launch App"
                    refreshStatus()

                    android.widget.Toast.makeText(
                        activity,
                        "Magic Key set to launch $label",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    statusLabel.text =
                        "Current: Failed to apply Launch App"

                    android.widget.Toast.makeText(
                        activity,
                        "Failed to set Magic Key app",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        if (!submitted) {
            sliderButton.isEnabled = true
            statusLabel.text = "Current: Unable to update"

            android.widget.Toast.makeText(
                activity,
                "Unable to start the Magic Key update",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun applyLaunchShortcutMode(
        activity: android.app.Activity,
        target: MagicKeyShortcutTarget,
        statusLabel: android.widget.TextView,
        shortcutButton: android.widget.Button,
        appButton: android.widget.Button? = null,
        runBackground: (() -> Unit) -> Boolean,
        refreshStatus: () -> Unit
    ) {
        statusLabel.text = "Current: Applying Launch Shortcut…"
        shortcutButton.isEnabled = false

        val submitted = runBackground {
            SliderDualAppStorage.disable(activity)
            val ok = HardwareController.setSliderLaunchShortcut(
                target.packageName,
                target.shortcutId
            )

            statusLabel.post {
                if (activity.isFinishing || activity.isDestroyed) {
                    return@post
                }

                shortcutButton.isEnabled = true

                if (ok) {
                    saveMagicKeyAppPackageStorage(activity, null)
                    saveMagicKeyShortcutStorage(activity, target)
                    appButton?.text =
                        "MAGIC KEY APP: Choose App"
                    shortcutButton.text =
                        "MAGIC KEY SHORTCUT: ${target.label}"
                    statusLabel.text = "Current: Launch Shortcut"
                    refreshStatus()

                    android.widget.Toast.makeText(
                        activity,
                        "Magic Key set to ${target.label}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    statusLabel.text =
                        "Current: Failed to apply Launch Shortcut"
                    android.widget.Toast.makeText(
                        activity,
                        "Failed to set Magic Key shortcut",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        if (!submitted) {
            shortcutButton.isEnabled = true
            statusLabel.text = "Current: Unable to update"
            android.widget.Toast.makeText(
                activity,
                "Unable to start the Magic Key update",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun disableMode(
        activity: android.app.Activity,
        statusLabel: android.widget.TextView,
        sliderButton: android.widget.Button? = null,
        shortcutButton: android.widget.Button? = null,
        runBackground: (() -> Unit) -> Boolean,
        refreshStatus: () -> Unit
    ) {
        statusLabel.text = "Current: Disabling…"

        val submitted = runBackground {
            SliderDualAppStorage.disable(activity)
            val ok =
                HardwareController.disableSliderSystemHandling()

            statusLabel.post {
                if (activity.isFinishing || activity.isDestroyed) {
                    return@post
                }

                if (ok) {
                    saveMagicKeyAppPackageStorage(activity, null)
                    saveMagicKeyShortcutStorage(activity, null)
                    sliderButton?.text =
                        "MAGIC KEY APP: Choose App"
                    shortcutButton?.text =
                        "MAGIC KEY SHORTCUT: Choose Shortcut"
                    statusLabel.text = "Current: Disabled"
                    refreshStatus()

                    android.widget.Toast.makeText(
                        activity,
                        "Magic Key disabled",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    statusLabel.text =
                        "Current: Failed to disable"

                    android.widget.Toast.makeText(
                        activity,
                        "Failed to disable Magic Key",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        if (!submitted) {
            statusLabel.text = "Current: Unable to update"

            android.widget.Toast.makeText(
                activity,
                "Unable to start the Magic Key update",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

}
