package com.elitedarkkaiser.redmagic

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.widget.Button
import com.google.android.material.button.MaterialButton

internal object PumpActions {

    fun applyPreviewSelection(
        profile: String,
        setPumpProfile: (String) -> Unit,
        setPumpEnabled: (Boolean) -> Unit,
        applyHardwareProfile: (String) -> Unit,
        refreshDialog: () -> Unit
    ) {
        setPumpProfile(profile)
        setPumpEnabled(true)
        applyHardwareProfile(profile)
        refreshDialog()
    }

    fun restoreOriginalState(
        originalEnabled: Boolean,
        originalProfile: String,
        setPumpEnabled: (Boolean) -> Unit,
        setPumpProfile: (String) -> Unit,
        applyHardwareProfile: (String) -> Unit,
        disablePump: () -> Unit
    ) {
        setPumpEnabled(originalEnabled)
        setPumpProfile(originalProfile)

        if (originalEnabled) {
            applyHardwareProfile(originalProfile)
        } else {
            disablePump()
        }
    }

    private fun updateProfileButton(
        button: Button,
        selected: Boolean,
        normalColor: Int,
        roundedFill: (Int, Int) -> Drawable,
        selectedColor: Int
    ) {
        val stateColor =
            if (selected) selectedColor else normalColor

        button.isSelected = selected

        if (button is MaterialButton) {
            button.backgroundTintList =
                ColorStateList.valueOf(stateColor)
            button.strokeColor =
                ColorStateList.valueOf(stateColor)
        } else {
            button.background = roundedFill(stateColor, 999)
        }
    }

    fun repaintButtons(
        selectedProfile: String,
        slowBtn: Button,
        mediumBtn: Button,
        quickBtn: Button,
        experimentalBtn: Button,
        roundedFill: (Int, Int) -> Drawable,
        selectedColor: Int,
        normalColor: Int,
        experimentalColor: Int
    ) {
        updateProfileButton(
            slowBtn,
            selectedProfile == "slow",
            normalColor,
            roundedFill,
            selectedColor
        )
        updateProfileButton(
            mediumBtn,
            selectedProfile == "medium",
            normalColor,
            roundedFill,
            selectedColor
        )
        updateProfileButton(
            quickBtn,
            selectedProfile == "quick",
            normalColor,
            roundedFill,
            selectedColor
        )
        updateProfileButton(
            experimentalBtn,
            selectedProfile == "experimental",
            experimentalColor,
            roundedFill,
            selectedColor
        )
    }
}
