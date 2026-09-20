package com.elitedarkkaiser.redmagic

import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.Locale

internal object SliderDualAppDialog {
    fun show(
        activity: MainActivity,
        statusLabel: TextView,
        pickerDeps: MagicKeyAppPickerDialog.Deps,
        runBackground: (() -> Unit) -> Boolean,
        onSaved: (SliderDualAppConfig) -> Unit
    ) {
        var config = SliderDualAppStorage.read(activity)
        var defaultUpLabel = appLabel(
            activity,
            config.defaultUpPackage
        )
        var defaultDownLabel = appLabel(
            activity,
            config.defaultDownPackage
        )
        var scheduledUpLabel = appLabel(
            activity,
            config.scheduledUpPackage
        )
        var scheduledDownLabel = appLabel(
            activity,
            config.scheduledDownPackage
        )

        val accent = Color.parseColor("#4EA1FF")
        val rowColor = Color.parseColor("#121A27")
        var dialogRef: AlertDialog? = null

        fun label(text: String, secondary: Boolean = false) =
            TextView(activity).apply {
                this.text = text
                textSize = if (secondary) 12f else 14f
                setTextColor(
                    if (secondary) {
                        pickerDeps.textSecondary
                    } else {
                        pickerDeps.textPrimary
                    }
                )
            }

        fun appButton(text: String) = MaterialButton(
            activity,
            null,
            com.google.android.material.R.attr
                .materialButtonOutlinedStyle
        ).apply {
            this.text = text
            isAllCaps = false
            setTextColor(pickerDeps.textPrimary)
            strokeColor = ColorStateList.valueOf(
                pickerDeps.borderColor
            )
            backgroundTintList = ColorStateList.valueOf(
                Color.TRANSPARENT
            )
            cornerRadius = pickerDeps.dp(14)
            minHeight = pickerDeps.dp(50)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = pickerDeps.dp(8)
            }
        }

        val enabledSwitch = MaterialSwitch(activity).apply {
            text = "Enable dual-app slider"
            setTextColor(pickerDeps.textPrimary)
            isChecked = config.enabled
        }

        val defaultUpButton = appButton(
            "Slider Up: $defaultUpLabel"
        )
        val defaultDownButton = appButton(
            "Slider Down: $defaultDownLabel"
        )
        val scheduleSwitch = MaterialSwitch(activity).apply {
            text = "Use scheduled app pair"
            setTextColor(pickerDeps.textPrimary)
            isChecked = config.scheduleEnabled
        }
        val startButton = appButton(
            "Schedule starts: ${formatTime(config.scheduleStartMinutes)}"
        )
        val endButton = appButton(
            "Schedule ends: ${formatTime(config.scheduleEndMinutes)}"
        )
        val scheduledUpButton = appButton(
            "Scheduled Up: $scheduledUpLabel"
        )
        val scheduledDownButton = appButton(
            "Scheduled Down: $scheduledDownLabel"
        )

        val scheduleControls = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(startButton)
            addView(endButton)
            addView(scheduledUpButton)
            addView(scheduledDownButton)
            visibility = if (config.scheduleEnabled) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        scheduleSwitch.setOnCheckedChangeListener { _, checked ->
            config = config.copy(scheduleEnabled = checked)
            scheduleControls.visibility = if (checked) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        defaultUpButton.setOnClickListener {
            choose(
                activity,
                "Choose Slider Up App",
                config.defaultUpPackage,
                pickerDeps
            ) { pkg, appName ->
                config = config.copy(defaultUpPackage = pkg)
                defaultUpLabel = appName
                defaultUpButton.text = "Slider Up: $appName"
            }
        }
        defaultDownButton.setOnClickListener {
            choose(
                activity,
                "Choose Slider Down App",
                config.defaultDownPackage,
                pickerDeps
            ) { pkg, appName ->
                config = config.copy(defaultDownPackage = pkg)
                defaultDownLabel = appName
                defaultDownButton.text = "Slider Down: $appName"
            }
        }
        scheduledUpButton.setOnClickListener {
            choose(
                activity,
                "Choose Scheduled Up App",
                config.scheduledUpPackage,
                pickerDeps
            ) { pkg, appName ->
                config = config.copy(scheduledUpPackage = pkg)
                scheduledUpLabel = appName
                scheduledUpButton.text = "Scheduled Up: $appName"
            }
        }
        scheduledDownButton.setOnClickListener {
            choose(
                activity,
                "Choose Scheduled Down App",
                config.scheduledDownPackage,
                pickerDeps
            ) { pkg, appName ->
                config = config.copy(scheduledDownPackage = pkg)
                scheduledDownLabel = appName
                scheduledDownButton.text =
                    "Scheduled Down: $appName"
            }
        }

        startButton.setOnClickListener {
            pickTime(
                activity,
                config.scheduleStartMinutes
            ) { minutes ->
                config = config.copy(
                    scheduleStartMinutes = minutes
                )
                startButton.text =
                    "Schedule starts: ${formatTime(minutes)}"
            }
        }
        endButton.setOnClickListener {
            pickTime(
                activity,
                config.scheduleEndMinutes
            ) { minutes ->
                config = config.copy(
                    scheduleEndMinutes = minutes
                )
                endButton.text =
                    "Schedule ends: ${formatTime(minutes)}"
            }
        }

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                pickerDeps.dp(18),
                pickerDeps.dp(18),
                pickerDeps.dp(18),
                pickerDeps.dp(18)
            )
            background = pickerDeps.roundedBg(
                pickerDeps.panelColor,
                pickerDeps.borderColor,
                22
            )
            addView(label("Dual-App Magic Key").apply {
                textSize = 18f
                setTypeface(pickerDeps.typeface, Typeface.BOLD)
            })
            addView(pickerDeps.space(pickerDeps.dp(8)))
            addView(label(
                "Launch one app when the slider moves up and " +
                    "another when it moves down. The optional " +
                    "schedule replaces both apps only during its window.",
                secondary = true
            ))
            addView(pickerDeps.space(pickerDeps.dp(14)))
            addView(enabledSwitch)
            addView(defaultUpButton)
            addView(defaultDownButton)
            addView(pickerDeps.space(pickerDeps.dp(12)))
            addView(scheduleSwitch)
            addView(scheduleControls)
        }

        val cancelButton = appButton("Cancel").apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            setOnClickListener { dialogRef?.dismiss() }
        }
        lateinit var saveButton: MaterialButton
        saveButton = appButton("Save").apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = pickerDeps.dp(10)
            }
            backgroundTintList = ColorStateList.valueOf(accent)
            setOnClickListener {
                val updated = config.copy(
                    enabled = enabledSwitch.isChecked,
                    scheduleEnabled = scheduleSwitch.isChecked
                )

                if (updated.enabled && !updated.isComplete()) {
                    Toast.makeText(
                        activity,
                        if (updated.scheduleEnabled) {
                            "Choose both default apps and both scheduled apps"
                        } else {
                            "Choose both default slider apps"
                        },
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                saveButton.isEnabled = false
                val submitted = runBackground {
                    val hardwareReady =
                        !updated.enabled ||
                            HardwareController
                                .disableSliderSystemHandling()

                    if (hardwareReady) {
                        SliderDualAppStorage.save(
                            activity,
                            updated
                        )
                        if (updated.enabled) {
                            HardwareServiceActions
                                .startSliderDualApp(activity)
                        } else {
                            HardwareServiceActions
                                .stopSliderDualApp(activity)
                        }
                    }

                    saveButton.post {
                        if (
                            activity.isFinishing ||
                            activity.isDestroyed
                        ) return@post

                        saveButton.isEnabled = true
                        if (hardwareReady) {
                            statusLabel.text = if (updated.enabled) {
                                "Current: Dual App Slider"
                            } else {
                                "Current: Disabled"
                            }
                            onSaved(updated)
                            dialogRef?.dismiss()
                            Toast.makeText(
                                activity,
                                if (updated.enabled) {
                                    "Dual-app slider enabled"
                                } else {
                                    "Dual-app slider disabled"
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                activity,
                                "Unable to take control of the Magic Key",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                if (!submitted) {
                    saveButton.isEnabled = true
                    Toast.makeText(
                        activity,
                        "Unable to save dual-app settings",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        content.addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, pickerDeps.dp(14), 0, 0)
            addView(cancelButton)
            addView(saveButton)
        })

        val root = ScrollView(activity).apply {
            setBackgroundColor(Color.parseColor("#070B12"))
            setPadding(
                pickerDeps.dp(12),
                pickerDeps.dp(12),
                pickerDeps.dp(12),
                pickerDeps.dp(12)
            )
            addView(content)
        }

        dialogRef = MaterialAlertDialogBuilder(activity)
            .setView(root)
            .setCancelable(true)
            .create()
        dialogRef?.show()
        dialogRef?.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
    }

    private fun choose(
        activity: MainActivity,
        title: String,
        selectedPackage: String?,
        deps: MagicKeyAppPickerDialog.Deps,
        onSelected: (String, String) -> Unit
    ) {
        MagicKeyAppPickerDialog.chooseApp(
            activity = activity,
            title = title,
            subtitle =
                "Uses the same launchable-app list as Game Mode",
            selectedPackage = selectedPackage,
            deps = deps
        ) { selected ->
            onSelected(selected.pkg, selected.label)
        }
    }

    private fun pickTime(
        activity: MainActivity,
        currentMinutes: Int,
        onSelected: (Int) -> Unit
    ) {
        TimePickerDialog(
            activity,
            { _, hour, minute ->
                onSelected(hour * 60 + minute)
            },
            currentMinutes / 60,
            currentMinutes % 60,
            false
        ).show()
    }

    private fun formatTime(minutes: Int): String {
        val normalized = minutes.coerceIn(0, 1439)
        val hour = normalized / 60
        val minute = normalized % 60
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val suffix = if (hour < 12) "AM" else "PM"
        return String.format(
            Locale.US,
            "%d:%02d %s",
            displayHour,
            minute,
            suffix
        )
    }

    private fun appLabel(
        activity: MainActivity,
        pkg: String?
    ): String {
        return if (pkg.isNullOrBlank()) {
            "Choose App"
        } else {
            MagicKeyActions.resolveAppLabel(activity, pkg)
        }
    }
}
