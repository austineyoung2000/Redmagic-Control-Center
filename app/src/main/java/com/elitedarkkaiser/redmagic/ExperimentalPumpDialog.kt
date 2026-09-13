package com.elitedarkkaiser.redmagic

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton

internal object ExperimentalPumpDialog {
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
        val space: (Int) -> View
    )

    fun show(
        activity: MainActivity,
        onCancel: () -> Unit,
        onConfirm: () -> Unit,
        deps: Deps
    ) {
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(deps.dp(22), deps.dp(18), deps.dp(22), deps.dp(12))
            background = deps.roundedBg(deps.panelColor, deps.borderColor, 22)
        }

        val titleView = TextView(activity).apply {
            text = "Experimental Pump Mode"
            textSize = 20f
            setTextColor(deps.textPrimary)
            setTypeface(deps.typeface, Typeface.BOLD)
        }

        val messageView = TextView(activity).apply {
            text = "This mode overclocks the liquid cooling pump beyond the standard profiles. It may provide thermal or performance benefits under heavy load, but it can also increase wear, instability, heat, noise, and possible pump failure or reduced lifespan.\n\nUse only if you understand the risks."
            textSize = 13f
            setTextColor(deps.textSecondary)
            setPadding(0, deps.dp(10), 0, deps.dp(16))
        }

        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val cancelBtn = MaterialButton(
            activity,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "Cancel"
            textSize = 13f
            isAllCaps = false
            setTextColor(deps.textPrimary)

            backgroundTintList =
                ColorStateList.valueOf(Color.TRANSPARENT)
            strokeWidth = deps.dp(1)
            strokeColor =
                ColorStateList.valueOf(deps.borderColor)
            rippleColor =
                ColorStateList.valueOf(Color.parseColor("#33445A"))
            cornerRadius = deps.dp(14)

            insetTop = 0
            insetBottom = 0
            minHeight = deps.dp(48)
            setPadding(
                deps.dp(18),
                deps.dp(10),
                deps.dp(18),
                deps.dp(10)
            )
        }

        val confirmBtn = MaterialButton(activity).apply {
            text = "I Understand"
            textSize = 13f
            isAllCaps = false
            setTextColor(deps.textPrimary)

            backgroundTintList =
                ColorStateList.valueOf(deps.panelPressed)
            rippleColor =
                ColorStateList.valueOf(Color.parseColor("#33445A"))
            cornerRadius = deps.dp(14)

            insetTop = 0
            insetBottom = 0
            minHeight = deps.dp(48)
            setPadding(
                deps.dp(18),
                deps.dp(10),
                deps.dp(18),
                deps.dp(10)
            )
        }

        buttonRow.addView(cancelBtn)
        buttonRow.addView(deps.space(deps.dp(10)))
        buttonRow.addView(confirmBtn)

        container.addView(titleView)
        container.addView(messageView)
        container.addView(buttonRow)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(container)
            .setCancelable(true)
            .create()

        cancelBtn.setOnClickListener {
            onCancel()
            dialog.dismiss()
        }

        confirmBtn.setOnClickListener {
            onConfirm()
            dialog.dismiss()
        }

        dialog.setOnCancelListener {
            onCancel()
        }

        dialog.show()

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.65f)
        }
    }
}
