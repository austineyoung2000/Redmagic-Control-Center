package com.elitedarkkaiser.redmagic.ui

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.elitedarkkaiser.redmagic.HardwareController
import com.elitedarkkaiser.redmagic.RootShell

object ControlsTabUi {
    data class Refs(
        val magicKeyStatusLabel: TextView
    )

    data class Result(
        val view: LinearLayout,
        val refs: Refs
    )

    fun create(activity: Activity, deps: ControlsTabDeps): Result {
        val container = deps.scrollTabContainer()

        val rootCheckBtn = deps.actionButton("CHECK ROOT", false) {
            val ok = RootShell.hasRoot()
            MaterialAlertDialogBuilder(activity)
                .setTitle("Root Status")
                .setMessage(
                    if (ok) "Root access granted\n\nApp is running as root"
                    else "Root access NOT granted\n\nCheck your root manager"
                )
                .setPositiveButton("OK", null)
                .show()
            deps.refreshStatus()
        }

        val refreshBtn = deps.actionButton("REFRESH STATUS", false) {
            deps.refreshStatus()
        }

        val systemCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("⚙", "SYSTEM"))
            addView(deps.row(rootCheckBtn, refreshBtn))
        }

        val magicKeyStatusLabel = deps.subtleLabel("Current: loading...")
        magicKeyStatusLabel.post {
            magicKeyStatusLabel.text = "Current: ${deps.readMagicKeyModeLabel()}"
        }

        var sliderAppBtnRef: Button? = null

        val cameraBtn = deps.smallActionButton("CAMERA", false) {
            deps.applyStockMagicKeyMode("Camera", { HardwareController.setSliderOpenCamera() }, magicKeyStatusLabel, sliderAppBtnRef)
        }

        val gameSpaceBtn = deps.smallActionButton("GAMESPACE", false) {
            deps.applyStockMagicKeyMode("GameSpace", { HardwareController.setSliderOpenGameSpace() }, magicKeyStatusLabel, sliderAppBtnRef)
        }

        val soundModeBtn = deps.smallActionButton("SOUND MODE", false) {
            deps.applyStockMagicKeyMode("Sound Mode", { HardwareController.setSliderSoundMode() }, magicKeyStatusLabel, sliderAppBtnRef)
        }

        val flashlightBtn = deps.smallActionButton("FLASHLIGHT", false) {
            deps.applyStockMagicKeyMode("Flashlight", { HardwareController.setSliderFlashlight() }, magicKeyStatusLabel, sliderAppBtnRef)
        }

        val recorderBtn = deps.smallActionButton("VOICE RECORDER", false) {
            deps.applyStockMagicKeyMode("Voice Recorder", { HardwareController.setSliderVoiceRecorder() }, magicKeyStatusLabel, sliderAppBtnRef)
        }

        val stockFunctionsCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("⌘", "MAGIC KEY FUNCTIONS"))
            addView(deps.bodyText("Choose one stock Magic Key action. Selecting a stock action disables app launch mode."))
            addView(magicKeyStatusLabel)
            addView(deps.space(deps.dp(8)))
            addView(deps.flowRow(arrayOf(cameraBtn, gameSpaceBtn, soundModeBtn)))
            addView(deps.spacer(deps.dp(8)))
            addView(deps.flowRow(arrayOf(flashlightBtn, recorderBtn)))
            addView(deps.spacer(deps.dp(8)))
            addView(deps.singleRow(deps.actionButton("DISABLE MAGIC KEY ACTION", true) {
                deps.disableMagicKeyMode(magicKeyStatusLabel, sliderAppBtnRef)
            }))
        }

        val sliderAppBtn = deps.actionButton(
            "MAGIC KEY APP: ${deps.resolveMagicKeyAppLabel(deps.savedMagicKeyAppPackage())}",
            false
        ) {}
        sliderAppBtnRef = sliderAppBtn
        sliderAppBtn.setOnClickListener {
            deps.showMagicKeyAppPicker(sliderAppBtn)
        }

        val clearSliderAppBtn = deps.actionButton("CLEAR APP SELECTION", true) {
            deps.disableMagicKeyMode(magicKeyStatusLabel, sliderAppBtn)
        }

        val sliderCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("↕", "SLIDER APP LAUNCH"))
            addView(deps.bodyText("Choose one app for Magic Key launch mode. Selecting an app disables stock Magic Key functions."))
            addView(deps.space(deps.dp(12)))
            addView(deps.singleRow(sliderAppBtn))
            addView(deps.space(deps.dp(12)))
            addView(deps.singleRow(clearSliderAppBtn))
            addView(deps.space(deps.dp(4)))
            setPadding(deps.dp(18), deps.dp(18), deps.dp(18), deps.dp(26))
        }

        container.addView(systemCard)
        container.addView(stockFunctionsCard)
        container.addView(sliderCard)

        return Result(
            view = container,
            refs = Refs(magicKeyStatusLabel = magicKeyStatusLabel)
        )
    }
}
