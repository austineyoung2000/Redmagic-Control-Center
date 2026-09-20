package com.elitedarkkaiser.redmagic

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.widget.Button
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import android.widget.LinearLayout
import android.widget.TextView

internal object DeviceGateDialogs {
    data class Deps(
        val textPrimary: Int,
        val textSecondary: Int,
        val panelColor: Int,
        val borderColor: Int,
        val panelPressed: Int,
        val accent: Int,
        val typeface: Typeface?,
        val dp: (Int) -> Int,
        val roundedBg: (Int, Int, Int) -> Drawable,
        val roundedFill: (Int, Int) -> Drawable
    )

    fun showSupportedDeviceDialog(
        activity: MainActivity,
        dontShowAgainChecked: Boolean,
        onDontShowAgainChanged: (Boolean) -> Unit,
        onAcknowledge: () -> Unit,
        deps: Deps
    ) {
        val container = baseContainer(activity, deps)

        val titleView = title(activity, "Supported Device Detected", deps)
        val messageView = message(
            activity,
            "RedMagic 11 Pro hardware profile is active. Root hardware controls are available.",
            deps
        )

        val dontShowAgain = MaterialCheckBox(activity).apply {
            text = "Never show again"
            isChecked = dontShowAgainChecked
            textSize = 13f
            setTextColor(deps.textSecondary)
            setPadding(0, deps.dp(12), 0, deps.dp(6))
            setOnCheckedChangeListener { _, checked -> onDontShowAgainChanged(checked) }
        }

        val okButton = dialogButton(activity, "Acknowledge", deps)

        container.addView(titleView)
        container.addView(messageView)
        container.addView(dontShowAgain)
        container.addView(okButton)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(container)
            .setCancelable(false)
            .create()

        okButton.setOnClickListener {
            onDontShowAgainChanged(dontShowAgain.isChecked)
            onAcknowledge()
            dialog.dismiss()
        }

        showTransparent(dialog)
    }

    fun showRootRequiredDialog(
        activity: MainActivity,
        onClose: () -> Unit,
        deps: Deps
    ) {
        val container = baseContainer(activity, deps)

        val titleView = title(activity, "Root Required", deps)
        val messageView = message(
            activity,
            "Root access is required for RedMagic hardware controls. Grant root access from Magisk, KernelSU, or APatch, then reopen the app.",
            deps
        )

        val closeButton = dialogButton(activity, "Close", deps)

        container.addView(titleView)
        container.addView(messageView)
        container.addView(closeButton)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(container)
            .setCancelable(false)
            .create()

        closeButton.setOnClickListener {
            onClose()
            dialog.dismiss()
        }

        showTransparent(dialog)
    }

    fun showUnsupportedDeviceDialog(
        activity: MainActivity,
        model: String,
        onClose: () -> Unit,
        deps: Deps
    ) {
        val container = baseContainer(activity, deps)

        val titleView = title(activity, "Unsupported Device", deps)
        val messageView = message(
            activity,
            "This app is restricted to RedMagic 11 Pro / NX809J.\n\nDetected model: $model",
            deps
        )

        val closeButton = dialogButton(activity, "Close", deps)

        container.addView(titleView)
        container.addView(messageView)
        container.addView(closeButton)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(container)
            .setCancelable(false)
            .create()

        closeButton.setOnClickListener {
            onClose()
            dialog.dismiss()
        }

        showTransparent(dialog)
    }

    private fun baseContainer(activity: MainActivity, deps: Deps): LinearLayout {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(deps.dp(22), deps.dp(18), deps.dp(22), deps.dp(14))
            background = deps.roundedBg(deps.panelColor, deps.borderColor, 22)
        }
    }

    private fun title(activity: MainActivity, textValue: String, deps: Deps): TextView {
        return TextView(activity).apply {
            text = textValue
            textSize = 20f
            setTextColor(deps.textPrimary)
            setTypeface(deps.typeface, Typeface.BOLD)
        }
    }

    private fun message(activity: MainActivity, textValue: String, deps: Deps): TextView {
        return TextView(activity).apply {
            text = textValue
            textSize = 13f
            setTextColor(deps.textSecondary)
            setPadding(0, deps.dp(10), 0, deps.dp(16))
        }
    }

    private fun dialogButton(
        activity: MainActivity,
        textValue: String,
        deps: Deps
    ): Button {
        return MaterialButton(activity).apply {
            text = textValue
            textSize = 13f
            isAllCaps = false
            setTextColor(deps.textPrimary)

            backgroundTintList =
                ColorStateList.valueOf(deps.panelPressed)
            rippleColor =
                ColorStateList.valueOf(com.elitedarkkaiser.redmagic.ui.AppTheme.rippleColor)
            cornerRadius = deps.dp(14)

            insetTop = 0
            insetBottom = 0
            minHeight = deps.dp(48)
            setPadding(
                deps.dp(20),
                deps.dp(10),
                deps.dp(20),
                deps.dp(10)
            )
        }
    }

    private fun showTransparent(dialog: AlertDialog) {
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.65f)
        }
    }
}
