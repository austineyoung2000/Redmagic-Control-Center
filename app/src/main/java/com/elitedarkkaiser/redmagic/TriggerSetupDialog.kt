package com.elitedarkkaiser.redmagic

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.radiobutton.MaterialRadioButton

internal object TriggerSetupDialog {
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
        val roundedFill: (Int, Int) -> Drawable,
        val space: (Int) -> View
    )

    fun show(activity: MainActivity, deps: Deps) {
        val prefs = activity.getSharedPreferences("triggers", Context.MODE_PRIVATE)

        val labels = arrayOf(
            "None",
            "Volume Up",
            "Volume Down",
            "Play / Pause",
            "Next Track",
            "Previous Track"
        )
        val values = arrayOf(
            "NONE",
            "VOL_UP",
            "VOL_DOWN",
            "MEDIA_PLAY_PAUSE",
            "MEDIA_NEXT",
            "MEDIA_PREVIOUS"
        )

        fun indexOfValue(value: String): Int {
            val i = values.indexOf(value)
            return if (i >= 0) i else 0
        }

        var leftChoice = indexOfValue(prefs.getString("left_trigger", "VOL_DOWN") ?: "VOL_DOWN")
        var rightChoice = indexOfValue(prefs.getString("right_trigger", "VOL_UP") ?: "VOL_UP")
        var unlockTapCount = prefs.getInt("intent_unlock_tap_count", 2).coerceIn(2, 4)
        var leftUnlockTapCount = prefs.getInt("left_intent_unlock_tap_count", 1).coerceIn(1, 4)

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(deps.dp(22), deps.dp(18), deps.dp(22), deps.dp(12))
            background = deps.roundedBg(deps.panelColor, deps.borderColor, 22)
        }

        val titleView = TextView(activity).apply {
            text = "Trigger Mapping"
            textSize = 20f
            setTextColor(deps.textPrimary)
            setTypeface(deps.typeface, Typeface.BOLD)
        }

        val subtitleView = TextView(activity).apply {
            text = "Set left and right shoulder triggers independently."
            textSize = 13f
            setTextColor(deps.textSecondary)
            setPadding(0, deps.dp(8), 0, deps.dp(12))
        }

        val leftLabel = TextView(activity).apply {
            text = "Left Trigger (F7)"
            textSize = 13f
            setTextColor(deps.textSecondary)
            setPadding(0, 0, 0, deps.dp(6))
        }

        val leftGroup = android.widget.RadioGroup(activity).apply {
            orientation = android.widget.RadioGroup.VERTICAL
        }

        labels.forEachIndexed { index, label ->
            leftGroup.addView(MaterialRadioButton(activity).apply {
                id = View.generateViewId()
                tag = index
                text = label
                textSize = 14f
                setTextColor(deps.textPrimary)
                buttonTintList = ColorStateList.valueOf(deps.accent)
                isChecked = index == leftChoice
            })
        }
        leftGroup.setOnCheckedChangeListener { group, checkedId ->
            val selected = group.findViewById<MaterialRadioButton>(checkedId)
            leftChoice = selected?.tag as? Int ?: leftChoice
        }

        val rightLabel = TextView(activity).apply {
            text = "Right Trigger (F8)"
            textSize = 13f
            setTextColor(deps.textSecondary)
            setPadding(0, deps.dp(14), 0, deps.dp(6))
        }

        val rightGroup = android.widget.RadioGroup(activity).apply {
            orientation = android.widget.RadioGroup.VERTICAL
        }

        labels.forEachIndexed { index, label ->
            rightGroup.addView(MaterialRadioButton(activity).apply {
                id = View.generateViewId()
                tag = index
                text = label
                textSize = 14f
                setTextColor(deps.textPrimary)
                buttonTintList = ColorStateList.valueOf(deps.accent)
                isChecked = index == rightChoice
            })
        }
        rightGroup.setOnCheckedChangeListener { group, checkedId ->
            val selected = group.findViewById<MaterialRadioButton>(checkedId)
            rightChoice = selected?.tag as? Int ?: rightChoice
        }

        val leftUnlockTapLabel = TextView(activity).apply {
            text = "Left / Top Intent Unlock taps"
            textSize = 13f
            setTextColor(deps.textSecondary)
            setPadding(0, deps.dp(14), 0, deps.dp(6))
        }

        val leftUnlockTapGroup = android.widget.RadioGroup(activity).apply {
            orientation = android.widget.RadioGroup.HORIZONTAL
        }

        listOf(1, 2, 3, 4).forEach { count ->
            leftUnlockTapGroup.addView(MaterialRadioButton(activity).apply {
                id = View.generateViewId()
                tag = count
                text = if (count == 1) "Single" else "$count taps"
                textSize = 14f
                setTextColor(deps.textPrimary)
                buttonTintList = ColorStateList.valueOf(deps.accent)
                isChecked = count == leftUnlockTapCount
            })
        }

        leftUnlockTapGroup.setOnCheckedChangeListener { group, checkedId ->
            val selected = group.findViewById<MaterialRadioButton>(checkedId)
            leftUnlockTapCount = selected?.tag as? Int ?: leftUnlockTapCount
        }

        val unlockTapLabel = TextView(activity).apply {
            text = "Right / Bottom Intent Unlock taps"
            textSize = 13f
            setTextColor(deps.textSecondary)
            setPadding(0, deps.dp(14), 0, deps.dp(6))
        }

        val unlockTapGroup = android.widget.RadioGroup(activity).apply {
            orientation = android.widget.RadioGroup.HORIZONTAL
        }

        listOf(2, 3, 4).forEach { count ->
            unlockTapGroup.addView(MaterialRadioButton(activity).apply {
                id = View.generateViewId()
                tag = count
                text = "$count taps"
                textSize = 14f
                setTextColor(deps.textPrimary)
                buttonTintList = ColorStateList.valueOf(deps.accent)
                isChecked = count == unlockTapCount
            })
        }

        unlockTapGroup.setOnCheckedChangeListener { group, checkedId ->
            val selected = group.findViewById<MaterialRadioButton>(checkedId)
            unlockTapCount = selected?.tag as? Int ?: unlockTapCount
        }

        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, deps.dp(18), 0, 0)
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
                ColorStateList.valueOf(com.elitedarkkaiser.redmagic.ui.AppTheme.rippleColor)
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

        val saveBtn = MaterialButton(activity).apply {
            text = "Save"
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

        buttonRow.addView(cancelBtn)
        buttonRow.addView(deps.space(deps.dp(10)))
        buttonRow.addView(saveBtn)

        container.addView(titleView)
        container.addView(subtitleView)
        container.addView(leftLabel)
        container.addView(leftGroup)
        container.addView(rightLabel)
        container.addView(rightGroup)
        container.addView(leftUnlockTapLabel)
        container.addView(leftUnlockTapGroup)
        container.addView(unlockTapLabel)
        container.addView(unlockTapGroup)
        container.addView(buttonRow)

        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            addView(
                container,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(scroll)
            .setCancelable(true)
            .create()

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        saveBtn.setOnClickListener {
            prefs.edit()
                .putString("left_trigger", values[leftChoice])
                .putString("right_trigger", values[rightChoice])
                .putInt("intent_unlock_tap_count", unlockTapCount)
                .putInt("left_intent_unlock_tap_count", leftUnlockTapCount)
                .apply()

            Toast.makeText(
                activity,
                "Saved: Left = ${labels[leftChoice]}, Right = ${labels[rightChoice]}",
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.65f)
        }
    }
}
