package com.elitedarkkaiser.redmagic

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.elitedarkkaiser.redmagic.ui.AppTheme
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

object RgbStudioDialog {
    data class Deps(
        val dp: (Int) -> Int,
        val filterChip: (String, Boolean, () -> Unit) -> Button,
        val updateSelectableButton: (Button, Boolean) -> Unit,
        val onSaveAndApply: (RgbStudioState) -> Unit,
        val onApplyToAll: (String, Int) -> Unit,
        val onStopService: () -> Unit
    )

    private data class ColorChoice(
        val id: Int,
        val label: String,
        val hex: String
    )

    private val colorChoices = listOf(
        ColorChoice(1, "Red", "#FF1744"),
        ColorChoice(3, "Orange", "#FF8C00"),
        ColorChoice(4, "Yellow", "#FFD600"),
        ColorChoice(5, "Green", "#00E676"),
        ColorChoice(6, "Cyan", "#00E5FF"),
        ColorChoice(7, "Blue", "#2979FF"),
        ColorChoice(8, "Purple", "#A020F0"),
        ColorChoice(9, "Pink", "#FF69B4")
    )

    fun show(
        activity: Activity,
        initial: RgbStudioState,
        deps: Deps
    ) {
        var enabled = initial.enabled
        var syncZones = initial.syncZones
        var effect = initial.effect
        val colors = initial.colors.toMutableList()
        var timeoutMinutes = initial.screenOffTimeoutMinutes

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                deps.dp(18),
                deps.dp(8),
                deps.dp(18),
                deps.dp(12)
            )
        }

        fun label(text: String): TextView {
            return TextView(activity).apply {
                this.text = text
                textSize = 12f
                setTextColor(AppTheme.textSecondary)
                setPadding(0, deps.dp(14), 0, deps.dp(7))
            }
        }

        val enabledSwitch = MaterialSwitch(activity).apply {
            text = "Enable RGB color cycle"
            isChecked = enabled
            setTextColor(AppTheme.textPrimary)
            setOnCheckedChangeListener { _, checked ->
                enabled = checked
            }
        }
        content.addView(enabledSwitch)

        val syncSwitch = MaterialSwitch(activity).apply {
            text = "Synchronize all LED zones"
            isChecked = syncZones
            setTextColor(AppTheme.textPrimary)
        }
        content.addView(syncSwitch)

        val stopServiceButton = deps.filterChip(
            "Stop RGB service",
            false
        ) {
            enabled = false
            enabledSwitch.isChecked = false
            deps.onStopService()

            Toast.makeText(
                activity,
                "RGB service stopped",
                Toast.LENGTH_SHORT
            ).show()
        }

        content.addView(
            stopServiceButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                deps.dp(44)
            ).apply {
                topMargin = deps.dp(8)
            }
        )

        content.addView(label("Effect"))
        val effectRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val effectButtons = linkedMapOf<String, Button>()

        fun addEffect(label: String, value: String) {
            val button = deps.filterChip(label, effect == value) {
                effect = value
                effectButtons.forEach { (key, candidate) ->
                    deps.updateSelectableButton(candidate, key == effect)
                }
            }
            effectButtons[value] = button
            if (effectRow.childCount > 0) {
                effectRow.addView(View(activity), LinearLayout.LayoutParams(deps.dp(6), 1))
            }
            effectRow.addView(
                button,
                LinearLayout.LayoutParams(0, deps.dp(44), 1f)
            )
        }

        addEffect("Steady", "steady")
        addEffect("Breathe", "breathe")
        addEffect("Flash", "flashing")
        addEffect("Rapid", "rapid")
        content.addView(effectRow)

        content.addView(label("Color sequence"))
        content.addView(TextView(activity).apply {
            text = "Selected colors play from top to bottom. Keep at least one color enabled."
            textSize = 12f
            setTextColor(AppTheme.textSecondary)
            setPadding(0, 0, 0, deps.dp(4))
        })

        colorChoices.forEach { choice ->
            content.addView(MaterialCheckBox(activity).apply {
                text = choice.label
                isChecked = choice.id in colors
                textSize = 13f
                setTextColor(Color.parseColor(choice.hex))
                buttonTintList = ColorStateList.valueOf(
                    Color.parseColor(choice.hex)
                )
                setOnCheckedChangeListener { button, checked ->
                    if (checked) {
                        if (choice.id !in colors) {
                            val targetIndex = colorChoices
                                .indexOfFirst { it.id == choice.id }
                            val insertAt = colors.indexOfFirst { current ->
                                colorChoices.indexOfFirst { it.id == current } > targetIndex
                            }
                            if (insertAt >= 0) {
                                colors.add(insertAt, choice.id)
                            } else {
                                colors.add(choice.id)
                            }
                        }
                    } else if (colors.size > 1) {
                        colors.remove(choice.id)
                    } else {
                        button.isChecked = true
                        Toast.makeText(
                            activity,
                            "Keep at least one cycle color",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
        }

        fun speedControl(
            title: String,
            initialMs: Long
        ): Pair<LinearLayout, Slider> {
            val valueText = TextView(activity).apply {
                text = String.format("%.1f s", initialMs / 1000f)
                textSize = 12f
                setTextColor(AppTheme.textSecondary)
                gravity = Gravity.END
            }
            val slider = Slider(activity).apply {
                valueFrom = 0.5f
                valueTo = 6.0f
                stepSize = 0.1f
                value = (initialMs / 1000f).coerceIn(valueFrom, valueTo)
                addOnChangeListener { _, newValue, _ ->
                    valueText.text = String.format("%.1f s", newValue)
                }
            }
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                addView(LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(TextView(activity).apply {
                        text = title
                        textSize = 13f
                        setTextColor(AppTheme.textPrimary)
                    }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    addView(valueText)
                })
                addView(slider)
            }
            return row to slider
        }

        content.addView(label("Cycle speed"))
        val (logoSpeedRow, logoSpeed) = speedControl(
            "Synchronized / Logo",
            initial.logoSpeedMs
        )
        val (shoulderSpeedRow, shoulderSpeed) = speedControl(
            "Shoulder LEDs",
            initial.shoulderSpeedMs
        )
        val (fanSpeedRow, fanSpeed) = speedControl(
            "Fan LED",
            initial.fanSpeedMs
        )
        content.addView(logoSpeedRow)
        content.addView(shoulderSpeedRow)
        content.addView(fanSpeedRow)

        fun refreshSpeedVisibility() {
            shoulderSpeedRow.visibility = if (syncZones) View.GONE else View.VISIBLE
            fanSpeedRow.visibility = if (syncZones) View.GONE else View.VISIBLE
        }
        syncSwitch.setOnCheckedChangeListener { _, checked ->
            syncZones = checked
            refreshSpeedVisibility()
        }
        refreshSpeedVisibility()

        content.addView(label("Screen-off timeout"))
        content.addView(TextView(activity).apply {
            text = "The cycle pauses after this time with the screen off. Charging, calls, and Game Mode keep their existing priority."
            textSize = 12f
            setTextColor(AppTheme.textSecondary)
            setPadding(0, 0, 0, deps.dp(7))
        })

        val timeoutRows = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        val timeoutButtons = linkedMapOf<Int, Button>()
        val timeoutOptions = listOf(
            0 to "Immediate",
            1 to "1 min",
            5 to "5 min",
            15 to "15 min",
            30 to "30 min"
        )
        timeoutOptions.chunked(3).forEach { options ->
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                if (timeoutRows.childCount > 0) {
                    setPadding(0, deps.dp(6), 0, 0)
                }
            }
            options.forEach { (minutes, title) ->
                val button = deps.filterChip(
                    title,
                    timeoutMinutes == minutes
                ) {
                    timeoutMinutes = minutes
                    timeoutButtons.forEach { (value, candidate) ->
                        deps.updateSelectableButton(
                            candidate,
                            value == timeoutMinutes
                        )
                    }
                }
                timeoutButtons[minutes] = button
                if (row.childCount > 0) {
                    row.addView(View(activity), LinearLayout.LayoutParams(deps.dp(6), 1))
                }
                row.addView(
                    button,
                    LinearLayout.LayoutParams(0, deps.dp(42), 1f)
                )
            }
            timeoutRows.addView(row)
        }
        content.addView(timeoutRows)

        fun buildState(forceEnabled: Boolean? = null): RgbStudioState {
            val logoMs = (logoSpeed.value * 1000f).toLong()
            val shoulderMs = if (syncZones) {
                logoMs
            } else {
                (shoulderSpeed.value * 1000f).toLong()
            }
            val fanMs = if (syncZones) {
                logoMs
            } else {
                (fanSpeed.value * 1000f).toLong()
            }

            return RgbStudioState(
                enabled = forceEnabled ?: enabled,
                syncZones = syncZones,
                effect = effect,
                colors = colors.toList(),
                logoSpeedMs = logoMs,
                shoulderSpeedMs = shoulderMs,
                fanSpeedMs = fanMs,
                screenOffTimeoutMinutes = timeoutMinutes
            )
        }

        val scroll = ScrollView(activity).apply {
            addView(content)
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("RGB Studio")
            .setView(scroll)
            .setNegativeButton("CANCEL", null)
            .setNeutralButton("APPLY TO ALL") { _, _ ->
                deps.onApplyToAll(effect, colors.first())
            }
            .setPositiveButton("SAVE & APPLY") { _, _ ->
                deps.onSaveAndApply(buildState())
            }
            .show()
    }
}
