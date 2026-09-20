package com.elitedarkkaiser.redmagic

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.radiobutton.MaterialRadioButton

internal object TriggerSafetyDialog {
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
        val space: (Int) -> View
    )

    fun show(
        activity: MainActivity,
        deps: Deps,
        onSaved: () -> Unit
    ) {
        val saved = readTriggerSafetyConfig(activity)
        var mode = saved.mode
        var holdDurationMs = saved.holdDurationMs
        var unlockTimeoutMs = saved.unlockTimeoutMs
        var leftTapCount = saved.leftUnlockTapCount
        var rightTapCount = saved.rightUnlockTapCount

        fun label(textValue: String, topPadding: Int = 14) =
            TextView(activity).apply {
                text = textValue
                textSize = 13f
                setTextColor(deps.textSecondary)
                setPadding(
                    0,
                    deps.dp(topPadding),
                    0,
                    deps.dp(6)
                )
            }

        fun radio(
            textValue: String,
            value: Any,
            selected: Boolean
        ) = MaterialRadioButton(activity).apply {
            id = View.generateViewId()
            tag = value
            text = textValue
            textSize = 14f
            setTextColor(deps.textPrimary)
            buttonTintList = ColorStateList.valueOf(deps.accent)
            isChecked = selected
        }

        fun safetySwitch(
            textValue: String,
            checked: Boolean
        ) = MaterialSwitch(activity).apply {
            text = textValue
            textSize = 14f
            setTextColor(deps.textPrimary)
            isChecked = checked
            setPadding(0, deps.dp(5), 0, deps.dp(5))
        }

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                deps.dp(22),
                deps.dp(18),
                deps.dp(22),
                deps.dp(12)
            )
            background = deps.roundedBg(
                deps.panelColor,
                deps.borderColor,
                22
            )
        }

        container.addView(TextView(activity).apply {
            text = "Trigger Safety"
            textSize = 20f
            setTextColor(deps.textPrimary)
            setTypeface(deps.typeface, Typeface.BOLD)
        })
        container.addView(TextView(activity).apply {
            text =
                "Choose how shoulder-trigger actions are protected " +
                    "from accidental touches."
            textSize = 13f
            setTextColor(deps.textSecondary)
            setPadding(0, deps.dp(8), 0, deps.dp(4))
        })

        val modeLabel = label("Protection mode", 8)
        val modeGroup = RadioGroup(activity).apply {
            orientation = RadioGroup.VERTICAL
        }
        listOf(
            TriggerSafetyConfig.MODE_OFF to "Off",
            TriggerSafetyConfig.MODE_INTENT to "Intent Unlock",
            TriggerSafetyConfig.MODE_HOLD to "Hold to Activate",
            TriggerSafetyConfig.MODE_INTENT_HOLD to
                "Intent Unlock + Hold"
        ).forEach { (value, textValue) ->
            modeGroup.addView(
                radio(textValue, value, value == mode)
            )
        }

        val holdLabel = label("Hold duration")
        val holdGroup = RadioGroup(activity).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        TriggerSafetyConfig.ALLOWED_HOLD_DURATIONS
            .sorted()
            .forEach { duration ->
                holdGroup.addView(
                    radio(
                        "${duration}ms",
                        duration,
                        duration == holdDurationMs
                    )
                )
            }
        holdGroup.setOnCheckedChangeListener { group, checkedId ->
            holdDurationMs = group
                .findViewById<MaterialRadioButton>(checkedId)
                ?.tag as? Int ?: holdDurationMs
        }

        val leftTapLabel = label("Left / Top unlock taps")
        val leftTapGroup = RadioGroup(activity).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        listOf(1, 2, 3, 4).forEach { count ->
            leftTapGroup.addView(
                radio(
                    if (count == 1) "Single" else "$count taps",
                    count,
                    count == leftTapCount
                )
            )
        }
        leftTapGroup.setOnCheckedChangeListener { group, checkedId ->
            leftTapCount = group
                .findViewById<MaterialRadioButton>(checkedId)
                ?.tag as? Int ?: leftTapCount
        }

        val rightTapLabel = label("Right / Bottom unlock taps")
        val rightTapGroup = RadioGroup(activity).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        listOf(2, 3, 4).forEach { count ->
            rightTapGroup.addView(
                radio("$count taps", count, count == rightTapCount)
            )
        }
        rightTapGroup.setOnCheckedChangeListener { group, checkedId ->
            rightTapCount = group
                .findViewById<MaterialRadioButton>(checkedId)
                ?.tag as? Int ?: rightTapCount
        }

        val timeoutLabel = label("Intent Unlock timeout")
        val timeoutGroup = RadioGroup(activity).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        linkedMapOf(
            1_500L to "1.5s",
            2_500L to "2.5s",
            5_000L to "5s",
            10_000L to "10s"
        ).forEach { (timeout, textValue) ->
            timeoutGroup.addView(
                radio(
                    textValue,
                    timeout,
                    timeout == unlockTimeoutMs
                )
            )
        }
        timeoutGroup.setOnCheckedChangeListener { group, checkedId ->
            unlockTimeoutMs = group
                .findViewById<MaterialRadioButton>(checkedId)
                ?.tag as? Long ?: unlockTimeoutMs
        }

        val lockScreenSwitch = safetySwitch(
            "Block actions on the lock screen",
            saved.blockOnLockScreen
        )
        val gameModeSwitch = safetySwitch(
            "Only while Game Mode is active",
            saved.gameModeOnly
        )
        val leftUnlockSwitch = safetySwitch(
            "Left trigger also unlocks right",
            saved.leftUnlocksRight
        )
        val gameModeSummary = TextView(activity).apply {
            text =
                "Uses the existing Game Mode selection: " +
                    gameModeAppsSummaryStorage(activity)
            textSize = 12f
            setTextColor(deps.textSecondary)
            setPadding(
                deps.dp(4),
                0,
                deps.dp(4),
                deps.dp(6)
            )
        }

        fun refreshVisibility() {
            val usesIntent =
                mode == TriggerSafetyConfig.MODE_INTENT ||
                    mode == TriggerSafetyConfig.MODE_INTENT_HOLD
            val usesHold =
                mode == TriggerSafetyConfig.MODE_HOLD ||
                    mode == TriggerSafetyConfig.MODE_INTENT_HOLD
            val intentVisibility =
                if (usesIntent) View.VISIBLE else View.GONE
            val holdVisibility =
                if (usesHold) View.VISIBLE else View.GONE

            holdLabel.visibility = holdVisibility
            holdGroup.visibility = holdVisibility
            leftTapLabel.visibility = intentVisibility
            leftTapGroup.visibility = intentVisibility
            rightTapLabel.visibility = intentVisibility
            rightTapGroup.visibility = intentVisibility
            timeoutLabel.visibility = intentVisibility
            timeoutGroup.visibility = intentVisibility
            leftUnlockSwitch.visibility = intentVisibility
        }

        modeGroup.setOnCheckedChangeListener { group, checkedId ->
            mode = group
                .findViewById<MaterialRadioButton>(checkedId)
                ?.tag as? String ?: mode
            refreshVisibility()
        }

        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, deps.dp(18), 0, 0)
        }
        val cancelButton = MaterialButton(
            activity,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "Cancel"
            isAllCaps = false
            setTextColor(deps.textPrimary)
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            strokeWidth = deps.dp(1)
            strokeColor = ColorStateList.valueOf(deps.borderColor)
            cornerRadius = deps.dp(14)
        }
        val saveButton = MaterialButton(activity).apply {
            text = "Save"
            isAllCaps = false
            setTextColor(deps.textPrimary)
            backgroundTintList =
                ColorStateList.valueOf(deps.panelPressed)
            cornerRadius = deps.dp(14)
        }
        buttonRow.addView(cancelButton)
        buttonRow.addView(deps.space(deps.dp(10)))
        buttonRow.addView(saveButton)

        container.addView(modeLabel)
        container.addView(modeGroup)
        container.addView(holdLabel)
        container.addView(holdGroup)
        container.addView(leftTapLabel)
        container.addView(leftTapGroup)
        container.addView(rightTapLabel)
        container.addView(rightTapGroup)
        container.addView(timeoutLabel)
        container.addView(timeoutGroup)
        container.addView(lockScreenSwitch)
        container.addView(gameModeSwitch)
        container.addView(gameModeSummary)
        container.addView(leftUnlockSwitch)
        container.addView(buttonRow)
        refreshVisibility()

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

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        saveButton.setOnClickListener {
            saveTriggerSafetyConfig(
                activity,
                TriggerSafetyConfig(
                    mode = mode,
                    holdDurationMs = holdDurationMs,
                    unlockTimeoutMs = unlockTimeoutMs,
                    blockOnLockScreen = lockScreenSwitch.isChecked,
                    gameModeOnly = gameModeSwitch.isChecked,
                    leftUnlocksRight = leftUnlockSwitch.isChecked,
                    leftUnlockTapCount = leftTapCount,
                    rightUnlockTapCount = rightTapCount
                )
            )
            onSaved()
            Toast.makeText(
                activity,
                "Trigger Safety settings saved",
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
