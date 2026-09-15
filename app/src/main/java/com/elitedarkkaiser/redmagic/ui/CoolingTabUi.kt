package com.elitedarkkaiser.redmagic.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import com.google.android.material.checkbox.MaterialCheckBox
import android.widget.LinearLayout
import com.google.android.material.slider.Slider
import android.widget.TextView
import com.elitedarkkaiser.redmagic.HardwareController
import com.google.android.material.materialswitch.MaterialSwitch

object CoolingTabUi {
    data class Refs(
        val tempText: TextView,
        val curveStatusText: TextView,
        val fanSeek: Slider,
        val autoCurveCheck: CheckBox,
        val quietCardRef: LinearLayout,
        val balancedCardRef: LinearLayout,
        val turboCardRef: LinearLayout,
        val smartPumpStatusView: TextView,
        val smartPumpSpeedView: TextView
    )

    data class Result(
        val view: LinearLayout,
        val refs: Refs
    )

    fun create(deps: CoolingTabDeps): Result {
        val container = deps.scrollTabContainer()

        val tempText = TextView(container.context).apply {
            text = "Current temp: --"
            textSize = 13f
            setTextColor(AppTheme.textSecondary)
            setPadding(0, deps.dp(6), 0, deps.dp(4))
        }

        lateinit var curveStatusText: TextView
        lateinit var autoCurveCheck: CheckBox

        val fanSeek = Slider(container.context).apply {
            valueFrom = 0f
            valueTo = 5f
            stepSize = 1f
            value = 0f

            addOnSliderTouchListener(
                object : Slider.OnSliderTouchListener {
                    override fun onStartTrackingTouch(
                        slider: Slider
                    ) = Unit

                    override fun onStopTrackingTouch(
                        slider: Slider
                    ) {
                        if (deps.getAutoFanCurveEnabled()) {
                            return
                        }

                        val level = slider.value.toInt()
                        deps.runBackground {
                            HardwareController.setFanLevel(level)
                            deps.refreshStatus()
                        }
                    }
                }
            )
        }

        val fanOnBtn = deps.actionButton("FAN ON", false) {
            deps.runBackground {
                HardwareController.enableFan(true)
                deps.refreshStatus()
            }
        }

        val fanOffBtn = deps.actionButton("FAN OFF", true) {
            deps.runBackground {
                HardwareController.enableFan(false)
                deps.refreshStatus()
            }
        }

        val rpmBtn = deps.actionButton("READ RPM", false) {
            deps.refreshStatus()
        }

        val quietCardRef = LinearLayout(container.context)
        val balancedCardRef = LinearLayout(container.context)
        val turboCardRef = LinearLayout(container.context)

        val modeRow = LinearLayout(container.context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun applyFanCurve(
            curve: String,
            displayName: String
        ) {
            if (deps.getAutoFanCurveEnabled()) return

            deps.setSelectedCurve(curve)
            deps.setSelectedCurveSaved(curve)
            curveStatusText.text =
                "Selected curve: $displayName • Applying…"

            val submitted = deps.runBackground {
                val level =
                    HardwareController.applyFanCurve(curve)

                curveStatusText.post {
                    /*
                     * Ignore a completion from an older queued request
                     * when the user has already selected another curve.
                     */
                    if (deps.getSelectedCurve() != curve) {
                        return@post
                    }

                    if (level != null) {
                        fanSeek.value = level.toFloat()
                        curveStatusText.text =
                            "Selected curve: $displayName • Applied"
                    } else {
                        curveStatusText.text =
                            "Selected curve: $displayName • " +
                                "Temperature unavailable"
                    }
                }

                deps.refreshStatus()
            }

            if (!submitted) {
                curveStatusText.text =
                    "Selected curve: $displayName • " +
                        "Unable to apply"
            }
        }

        val quietChip = deps.segmentedChip(
            "Quiet",
            deps.getSelectedCurve() == "quiet"
        ) {
            applyFanCurve("quiet", "Quiet")
        }

        val balancedChip = deps.segmentedChip(
            "Balanced",
            deps.getSelectedCurve() == "balanced"
        ) {
            applyFanCurve("balanced", "Balanced")
        }

        val turboChip = deps.segmentedChip(
            "Turbo",
            deps.getSelectedCurve() == "turbo"
        ) {
            applyFanCurve("turbo", "Turbo")
        }

        modeRow.addView(quietChip)
        modeRow.addView(deps.space(deps.dp(8)))
        modeRow.addView(balancedChip)
        modeRow.addView(deps.space(deps.dp(8)))
        modeRow.addView(turboChip)

        curveStatusText = TextView(container.context).apply {
            text = "Selected curve: ${deps.getSelectedCurve().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
            textSize = 13f
            setTextColor(AppTheme.textSecondary)
            setPadding(0, deps.dp(6), 0, deps.dp(4))
        }

        autoCurveCheck = MaterialCheckBox(container.context).apply {
            text = "Automatic fan control based on temperature"
            setTextColor(AppTheme.textPrimary)
            textSize = 13f
            setPadding(0, deps.dp(6), 0, deps.dp(4))
            setOnCheckedChangeListener { _, checked ->
                deps.setAutoFanCurveEnabled(checked)
                deps.setAutoFanEnabledSaved(checked)
                deps.updateManualCurveUiState()

                if (checked) {
                    deps.startAutoFanService()
                    curveStatusText.text = "Auto fan curve active • Running in background service"
                } else {
                    deps.stopAutoFanService()
                    curveStatusText.text = "Selected curve: ${deps.getSelectedCurve()} • Manual control"
                }

                deps.refreshStatus()
            }
        }

        lateinit var smartPumpStatusView: TextView
        lateinit var smartPumpSpeedView: TextView

        lateinit var pumpCard: LinearLayout

        val coolingCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("❄", "COOLING"))
            addView(tempText)

            val tempUnitRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, deps.dp(8), 0, deps.dp(4))
            }

            val tempUnitLabel = TextView(context).apply {
                text = "Use Fahrenheit"
                textSize = 13f
                setTextColor(AppTheme.textPrimary)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tempUnitSwitch = MaterialSwitch(context).apply {
                isChecked = deps.getUseFahrenheit()
                setOnCheckedChangeListener { _, checked ->
                    deps.setUseFahrenheit(checked)
                    deps.saveUseFahrenheit(checked)
                    deps.refreshStatus()
                }
            }

            tempUnitRow.addView(tempUnitLabel)
            tempUnitRow.addView(tempUnitSwitch)

            addView(tempUnitRow)
            addView(deps.subtleLabel("Fan level"))
            addView(fanSeek)
            addView(deps.row(fanOnBtn, fanOffBtn))
            addView(deps.singleRow(rpmBtn))
            addView(deps.spacer(deps.dp(16)))
            addView(deps.spacer(deps.dp(16)))

            pumpCard = deps.sectionPanel().apply {
                addView(deps.sectionHeader("◉", "PUMP"))
                addView(deps.bodyText("Liquid cooling pump control with manual speed, auto temperature control, and live diagnostics."))
                addView(deps.spacer(deps.dp(10)))

                lateinit var pumpPowerSwitch: MaterialSwitch
                lateinit var autoPumpSwitch: MaterialSwitch
                var updatingPumpSwitches = false

                fun syncPumpSwitches() {
                    updatingPumpSwitches = true
                    try {
                        pumpPowerSwitch.isChecked = deps.getPumpEnabled()
                        autoPumpSwitch.isChecked = deps.getAutoPumpEnabled()
                    } finally {
                        updatingPumpSwitches = false
                    }
                }

                fun manualSpeedLabel(): String {
                    return deps.getPumpProfile().replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase() else it.toString()
                    }
                }

                fun manualSpeedValue(): Int {
                    return when (deps.getPumpProfile().lowercase()) {
                        "slow" -> 40
                        "medium" -> 60
                        "quick" -> 80
                        "experimental" -> 90
                        else -> 80
                    }
                }

                fun refreshPumpDiagnostics() {
                    if (deps.getAutoPumpEnabled()) {
                        val status = deps.buildAutoPumpStatusText()
                        smartPumpStatusView.text = status.first
                        smartPumpSpeedView.text = status.second
                    } else {
                        smartPumpStatusView.text = if (deps.getPumpEnabled()) {
                            "Pump Mode: MANUAL • ${manualSpeedLabel()}"
                        } else {
                            "Pump Mode: OFF"
                        }
                        smartPumpSpeedView.text = if (deps.getPumpEnabled()) {
                            "Speed: ${manualSpeedValue()} • Freq: 4"
                        } else {
                            "Speed: 0 • Freq: 0"
                        }
                    }
                }

                fun setManualControlsEnabled(enabled: Boolean) {
                    pumpPowerSwitch.isEnabled = enabled
                }

                val pumpPowerTitle = TextView(context).apply {
                    text = "Pump Power"
                    textSize = 15f
                    setTextColor(AppTheme.textPrimary)
                    typeface = Typeface.create(AppTheme.appTypeface, Typeface.BOLD)
                }

                val pumpPowerDesc = TextView(context).apply {
                    text = "Turn the liquid cooling micropump on or off."
                    textSize = 12f
                    setTextColor(AppTheme.textSecondary)
                    setPadding(0, deps.dp(4), 0, 0)
                }

                pumpPowerSwitch = MaterialSwitch(context).apply {
                    isChecked = deps.getPumpEnabled()
                    setOnCheckedChangeListener { _, checked ->
                        if (updatingPumpSwitches) {
                            return@setOnCheckedChangeListener
                        }

                        deps.setPumpEnabled(checked)
                        deps.savePumpState()

                        if (checked) {
                            deps.runBackground {
                                HardwareController.setPumpProfile(
                                    deps.getPumpProfile()
                                )
                                deps.refreshStatus()
                            }
                        } else {
                            deps.setAutoPumpEnabled(false)
                            deps.saveAutoPumpState()
                            /*
                             * Let the Auto listener restore the manual
                             * controls. The unchanged Pump switch value
                             * prevents a recursive Pump callback.
                             */
                            autoPumpSwitch.isChecked = false

                            deps.runBackground {
                                deps.stopAutoPumpService()
                                HardwareController.enablePump(false)
                                deps.refreshStatus()
                            }
                        }

                        syncPumpSwitches()
                        refreshPumpDiagnostics()
                        deps.refreshSmartPumpStatusViews()
                    }
                }

                val powerRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(pumpPowerTitle)
                        addView(pumpPowerDesc)
                    }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    addView(pumpPowerSwitch)
                }

                addView(powerRow)
                addView(deps.spacer(deps.dp(14)))
                addView(deps.subtleLabel("Manual pump speed"))

                val speedRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                val chipParams = LinearLayout.LayoutParams(0, deps.dp(42), 1f)
                val gapParams = LinearLayout.LayoutParams(deps.dp(6), 1)

                val slowBtn = deps.segmentedChip("Slow", deps.getPumpProfile() == "slow") {
                    deps.applyPumpProfile("slow")
                    syncPumpSwitches()
                    refreshPumpDiagnostics()
                }

                val mediumBtn = deps.segmentedChip("Medium", deps.getPumpProfile() == "medium") {
                    deps.applyPumpProfile("medium")
                    syncPumpSwitches()
                    refreshPumpDiagnostics()
                }

                val quickBtn = deps.segmentedChip("Quick", deps.getPumpProfile() == "quick") {
                    deps.applyPumpProfile("quick")
                    syncPumpSwitches()
                    refreshPumpDiagnostics()
                }

                val experimentalBtn = deps.segmentedChip("OC", deps.getPumpProfile() == "experimental") {
                    deps.confirmExperimentalPumpThenApply {
                        syncPumpSwitches()
                        refreshPumpDiagnostics()
                    }
                }

                speedRow.addView(slowBtn, chipParams)
                speedRow.addView(deps.space(deps.dp(6)), gapParams)
                speedRow.addView(mediumBtn, chipParams)
                speedRow.addView(deps.space(deps.dp(6)), gapParams)
                speedRow.addView(quickBtn, chipParams)
                speedRow.addView(deps.space(deps.dp(6)), gapParams)
                speedRow.addView(experimentalBtn, chipParams)

                addView(speedRow)
                addView(deps.spacer(deps.dp(14)))

                val autoTitle = TextView(context).apply {
                    text = "Auto Pump Speed"
                    textSize = 15f
                    setTextColor(AppTheme.textPrimary)
                    typeface = Typeface.create(AppTheme.appTypeface, Typeface.BOLD)
                }

                val autoDesc = TextView(context).apply {
                    text = "Automatically adjusts pump speed based on device temperature and keeps running after app close."
                    textSize = 12f
                    setTextColor(AppTheme.textSecondary)
                    setPadding(0, deps.dp(4), 0, 0)
                }

                autoPumpSwitch = MaterialSwitch(context).apply {
                    isChecked = deps.getAutoPumpEnabled()
                    setOnCheckedChangeListener { _, checked ->
                        if (updatingPumpSwitches) {
                            return@setOnCheckedChangeListener
                        }

                        deps.setAutoPumpEnabled(checked)
                        deps.saveAutoPumpState()

                        if (checked) {
                            deps.setPumpEnabled(true)
                            deps.savePumpState()

                            deps.runBackground {
                                HardwareController.setPumpProfile(
                                    deps.getPumpProfile()
                                )
                                deps.startAutoPumpService()
                                deps.refreshStatus()
                            }
                        } else {
                            deps.stopAutoPumpService()
                        }

                        syncPumpSwitches()
                        setManualControlsEnabled(!checked)
                        refreshPumpDiagnostics()
                        deps.refreshSmartPumpStatusViews()
                    }
                }

                val autoRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(autoTitle)
                        addView(autoDesc)
                    }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    addView(autoPumpSwitch)
                }

                addView(autoRow)
                addView(deps.spacer(deps.dp(14)))
                addView(deps.subtleLabel("Live diagnostics"))

                smartPumpStatusView = TextView(context).apply {
                    textSize = 12f
                    setTextColor(AppTheme.textPrimary)
                    setPadding(0, deps.dp(6), 0, 0)
                }

                smartPumpSpeedView = TextView(context).apply {
                    textSize = 12f
                    setTextColor(AppTheme.textSecondary)
                    setPadding(0, deps.dp(4), 0, 0)
                }

                addView(smartPumpStatusView)
                addView(smartPumpSpeedView)

                setManualControlsEnabled(!deps.getAutoPumpEnabled())
                refreshPumpDiagnostics()
            }

            addView(deps.spacer(deps.dp(16)))
            addView(deps.sectionHeader("▦", "FAN CURVE"))
            addView(autoCurveCheck)
            addView(curveStatusText)
            addView(modeRow)
            addView(deps.subtleLabel("Auto mode ramps fan by temperature and disables manual curve cards"))
            addView(deps.subtleLabel("Quiet → low noise, stays between fan 0-1"))
            addView(deps.subtleLabel("Balanced → moderate cooling, stays between fan 2-3"))
            addView(deps.subtleLabel("Turbo → max cooling and sound, stays between fan 4-5"))
        }

        container.addView(coolingCard)
        container.addView(deps.spacer(deps.dp(16)))
        container.addView(pumpCard)

        return Result(
            view = container,
            refs = Refs(
                tempText = tempText,
                curveStatusText = curveStatusText,
                fanSeek = fanSeek,
                autoCurveCheck = autoCurveCheck,
                quietCardRef = quietCardRef,
                balancedCardRef = balancedCardRef,
                turboCardRef = turboCardRef,
                smartPumpStatusView = smartPumpStatusView,
                smartPumpSpeedView = smartPumpSpeedView
            )
        )
    }
}
