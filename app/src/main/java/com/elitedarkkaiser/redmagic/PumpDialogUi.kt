package com.elitedarkkaiser.redmagic

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

internal object PumpDialogUi {

    data class Deps(
        val textPrimary: Int,
        val textSecondary: Int,
        val panelColor: Int,
        val borderColor: Int,
        val panelPressed: Int,
        val typeface: Typeface?,
        val dp: (Int) -> Int,
        val roundedBg: (Int, Int, Int) -> Drawable,
        val roundedFill: (Int, Int) -> Drawable,
        val filterChip: (String, Boolean, () -> Unit) -> Button,
        val space: (Int) -> View
    )

    fun showPumpProfileDialog(
        activity: MainActivity,
        originalEnabled: Boolean,
        originalProfile: String,
        currentProfile: () -> String,
        setPumpEnabled: (Boolean) -> Unit,
        setPumpProfile: (String) -> Unit,
        applyHardwareProfile: (String) -> Unit,
        disablePump: () -> Unit,
        savePumpState: () -> Unit,
        confirmExperimentalPumpThenApply: () -> Unit,
        setDialogRefreshPump: (((() -> Unit)?) -> Unit),
        deps: Deps
    ) {
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(deps.dp(22), deps.dp(18), deps.dp(22), deps.dp(12))
            background = deps.roundedBg(deps.panelColor, deps.borderColor, 22)
        }

        val titleView = TextView(activity).apply {
            text = "Liquid Cooling Flow Rate"
            textSize = 20f
            setTextColor(deps.textPrimary)
            setTypeface(deps.typeface, Typeface.BOLD)
        }

        val subtitleView = TextView(activity).apply {
            text = "Choose a pump profile with instant preview"
            textSize = 13f
            setTextColor(deps.textSecondary)
            setPadding(0, deps.dp(8), 0, 0)
        }

        val profileLabel = TextView(activity).apply {
            text = "Flow rate"
            textSize = 12f
            setTextColor(deps.textSecondary)
            setPadding(0, deps.dp(16), 0, deps.dp(8))
        }

        val profilesRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val slowBtn = deps.filterChip("Slow", currentProfile() == "slow") {
            PumpActions.applyPreviewSelection(
                profile = "slow",
                setPumpProfile = setPumpProfile,
                setPumpEnabled = setPumpEnabled,
                applyHardwareProfile = applyHardwareProfile,
                refreshDialog = { /* set later */ }
            )
        }

        val mediumBtn = deps.filterChip("Medium", currentProfile() == "medium") {
            PumpActions.applyPreviewSelection(
                profile = "medium",
                setPumpProfile = setPumpProfile,
                setPumpEnabled = setPumpEnabled,
                applyHardwareProfile = applyHardwareProfile,
                refreshDialog = { /* set later */ }
            )
        }

        val quickBtn = deps.filterChip("Quick", currentProfile() == "quick") {
            PumpActions.applyPreviewSelection(
                profile = "quick",
                setPumpProfile = setPumpProfile,
                setPumpEnabled = setPumpEnabled,
                applyHardwareProfile = applyHardwareProfile,
                refreshDialog = { /* set later */ }
            )
        }

        val experimentalBtn = deps.filterChip("⚠ Exp", currentProfile() == "experimental") {
            confirmExperimentalPumpThenApply()
        }

        val chipParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val gapParams = LinearLayout.LayoutParams(deps.dp(6), ViewGroup.LayoutParams.WRAP_CONTENT)

        profilesRow.addView(slowBtn, chipParams)
        profilesRow.addView(deps.space(deps.dp(6)), gapParams)
        profilesRow.addView(mediumBtn, chipParams)
        profilesRow.addView(deps.space(deps.dp(6)), gapParams)
        profilesRow.addView(quickBtn, chipParams)
        profilesRow.addView(deps.space(deps.dp(6)), gapParams)
        profilesRow.addView(experimentalBtn, chipParams)

        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, deps.dp(18), 0, 0)
        }

        val cancelBtn = Button(activity).apply {
            text = "Cancel"
            textSize = 13f
            isAllCaps = false
            setTextColor(deps.textPrimary)
            background = deps.roundedFill(Color.parseColor("#1E2633"), 14)
            setPadding(deps.dp(18), deps.dp(10), deps.dp(18), deps.dp(10))
        }

        val saveBtn = Button(activity).apply {
            text = "Save"
            textSize = 13f
            isAllCaps = false
            setTextColor(deps.textPrimary)
            background = deps.roundedFill(deps.panelPressed, 14)
            setPadding(deps.dp(20), deps.dp(10), deps.dp(20), deps.dp(10))
        }

        buttonRow.addView(cancelBtn)
        buttonRow.addView(deps.space(deps.dp(10)))
        buttonRow.addView(saveBtn)

        container.addView(titleView)
        container.addView(subtitleView)
        container.addView(profileLabel)
        container.addView(profilesRow)
        container.addView(buttonRow)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(container)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )

        fun repaint() {
            PumpActions.repaintButtons(
                selectedProfile = currentProfile(),
                slowBtn = slowBtn,
                mediumBtn = mediumBtn,
                quickBtn = quickBtn,
                experimentalBtn = experimentalBtn,
                roundedFill = deps.roundedFill,
                selectedColor = deps.panelPressed,
                normalColor = Color.parseColor("#1E2633"),
                experimentalColor = Color.parseColor("#2A1D1D")
            )
        }

        setDialogRefreshPump { repaint() }

        val refreshWrapper = { repaint() }

        slowBtn.setOnClickListener {
            PumpActions.applyPreviewSelection(
                profile = "slow",
                setPumpProfile = setPumpProfile,
                setPumpEnabled = setPumpEnabled,
                applyHardwareProfile = applyHardwareProfile,
                refreshDialog = refreshWrapper
            )
        }
        mediumBtn.setOnClickListener {
            PumpActions.applyPreviewSelection(
                profile = "medium",
                setPumpProfile = setPumpProfile,
                setPumpEnabled = setPumpEnabled,
                applyHardwareProfile = applyHardwareProfile,
                refreshDialog = refreshWrapper
            )
        }
        quickBtn.setOnClickListener {
            PumpActions.applyPreviewSelection(
                profile = "quick",
                setPumpProfile = setPumpProfile,
                setPumpEnabled = setPumpEnabled,
                applyHardwareProfile = applyHardwareProfile,
                refreshDialog = refreshWrapper
            )
        }

        cancelBtn.setOnClickListener {
            PumpActions.restoreOriginalState(
                originalEnabled = originalEnabled,
                originalProfile = originalProfile,
                setPumpEnabled = setPumpEnabled,
                setPumpProfile = setPumpProfile,
                applyHardwareProfile = applyHardwareProfile,
                disablePump = disablePump
            )
            dialog.dismiss()
        }

        saveBtn.setOnClickListener {
            savePumpState()
            dialog.dismiss()
        }

        dialog.setOnCancelListener {
            PumpActions.restoreOriginalState(
                originalEnabled = originalEnabled,
                originalProfile = originalProfile,
                setPumpEnabled = setPumpEnabled,
                setPumpProfile = setPumpProfile,
                applyHardwareProfile = applyHardwareProfile,
                disablePump = disablePump
            )
        }

        repaint()
        dialog.show()

        dialog.window?.apply {
            setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            )
            setDimAmount(0.65f)
        }
    }
}
