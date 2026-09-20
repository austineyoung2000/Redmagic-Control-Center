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
import com.google.android.material.materialswitch.MaterialSwitch

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
        val savedSafety = readTriggerSafetyConfig(activity)
        var safetyMode = savedSafety.mode
        var holdDurationMs = savedSafety.holdDurationMs
        var unlockTimeoutMs = savedSafety.unlockTimeoutMs
        var unlockTapCount = savedSafety.rightUnlockTapCount
        var leftUnlockTapCount = savedSafety.leftUnlockTapCount

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

        val safetyLabel = TextView(activity).apply {
            text = "Trigger Safety"
            textSize = 16f
            setTextColor(deps.textPrimary)
            setTypeface(deps.typeface, Typeface.BOLD)
            setPadding(0, deps.dp(18), 0, deps.dp(4))
        }

        val safetyDescription = TextView(activity).apply {
            text =
                "Filter accidental touches before an assigned action " +
                    "is allowed to run."
            textSize = 13f
            setTextColor(deps.textSecondary)
            setPadding(0, 0, 0, deps.dp(6))
        }

        val safetyModes = listOf(
            TriggerSafetyConfig.MODE_OFF to "Off",
            TriggerSafetyConfig.MODE_INTENT to "Intent Unlock",
            TriggerSafetyConfig.MODE_HOLD to "Hold to Activate",
            TriggerSafetyConfig.MODE_INTENT_HOLD to
                "Intent Unlock + Hold"
        )

        val safetyModeGroup = android.widget.RadioGroup(activity).apply {
            orientation = android.widget.RadioGroup.VERTICAL
        }

        safetyModes.forEach { (value, label) ->
            safetyModeGroup.addView(MaterialRadioButton(activity).apply {
                id = View.generateViewId()
                tag = value
                text = label
                textSize = 14f
                setTextColor(deps.textPrimary)
                buttonTintList = ColorStateList.valueOf(deps.accent)
                isChecked = value == safetyMode
            })
        }

        val holdDurationLabel = TextView(activity).apply {
            text = "Hold duration"
            textSize = 13f
            setTextColor(deps.textSecondary)
            setPadding(0, deps.dp(14), 0, deps.dp(6))
        }

        val holdDurationGroup = android.widget.RadioGroup(activity).apply {
            orientation = android.widget.RadioGroup.HORIZONTAL
        }

        TriggerSafetyConfig.ALLOWED_HOLD_DURATIONS
            .sorted()
            .forEach { duration ->
                holdDurationGroup.addView(
                    MaterialRadioButton(activity).apply {
                        id = View.generateViewId()
                        tag = duration
                        text = "${duration}ms"
                        textSize = 13f
                        setTextColor(deps.textPrimary)
                        buttonTintList =
                            ColorStateList.valueOf(deps.accent)
                        isChecked = duration == holdDurationMs
                    }
                )
            }

        holdDurationGroup.setOnCheckedChangeListener {
                group,
                checkedId ->
            val selected =
                group.findViewById<MaterialRadioButton>(checkedId)
            holdDurationMs = selected?.tag as? Int ?: holdDurationMs
        }

        val unlockTimeoutLabel = TextView(activity).apply {
            text = "Intent Unlock timeout"
            textSize = 13f
            setTextColor(deps.textSecondary)
            setPadding(0, deps.dp(14), 0, deps.dp(6))
        }

        val unlockTimeoutGroup = android.widget.RadioGroup(activity).apply {
            orientation = android.widget.RadioGroup.HORIZONTAL
        }

        val timeoutLabels = linkedMapOf(
            1_500L to "1.5s",
            2_500L to "2.5s",
            5_000L to "5s",
            10_000L to "10s"
        )

        timeoutLabels.forEach { (timeout, label) ->
            unlockTimeoutGroup.addView(
                MaterialRadioButton(activity).apply {
                    id = View.generateViewId()
                    tag = timeout
                    text = label
                    textSize = 13f
                    setTextColor(deps.textPrimary)
                    buttonTintList =
                        ColorStateList.valueOf(deps.accent)
                    isChecked = timeout == unlockTimeoutMs
                }
            )
        }

        unlockTimeoutGroup.setOnCheckedChangeListener {
                group,
                checkedId ->
            val selected =
                group.findViewById<MaterialRadioButton>(checkedId)
            unlockTimeoutMs =
                selected?.tag as? Long ?: unlockTimeoutMs
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

        fun safetySwitch(
            label: String,
            checked: Boolean
        ): MaterialSwitch {
            return MaterialSwitch(activity).apply {
                text = label
                textSize = 14f
                setTextColor(deps.textPrimary)
                isChecked = checked
                setPadding(0, deps.dp(5), 0, deps.dp(5))
            }
        }

        val lockScreenSwitch = safetySwitch(
            "Block actions on the lock screen",
            savedSafety.blockOnLockScreen
        )
        val gameModeOnlySwitch = safetySwitch(
            "Only while Game Mode is active",
            savedSafety.gameModeOnly
        )
        val leftUnlocksRightSwitch = safetySwitch(
            "Left trigger also unlocks right",
            savedSafety.leftUnlocksRight
        )

        val gameModeDescription = TextView(activity).apply {
            val selection = gameModeAppsSummaryStorage(activity)
            text =
                "Uses the existing Game Mode app selection. " +
                    "Current selection: $selection"
            textSize = 12f
            setTextColor(deps.textSecondary)
            setPadding(
                deps.dp(4),
                0,
                deps.dp(4),
                deps.dp(6)
            )
        }

        fun refreshSafetyControls() {
            val usesIntent =
                safetyMode == TriggerSafetyConfig.MODE_INTENT ||
                    safetyMode ==
                    TriggerSafetyConfig.MODE_INTENT_HOLD
            val usesHold =
                safetyMode == TriggerSafetyConfig.MODE_HOLD ||
                    safetyMode ==
                    TriggerSafetyConfig.MODE_INTENT_HOLD

            val intentVisibility =
                if (usesIntent) View.VISIBLE else View.GONE
            val holdVisibility =
                if (usesHold) View.VISIBLE else View.GONE

            leftUnlockTapLabel.visibility = intentVisibility
            leftUnlockTapGroup.visibility = intentVisibility
            unlockTapLabel.visibility = intentVisibility
            unlockTapGroup.visibility = intentVisibility
            unlockTimeoutLabel.visibility = intentVisibility
            unlockTimeoutGroup.visibility = intentVisibility
            leftUnlocksRightSwitch.visibility = intentVisibility
            holdDurationLabel.visibility = holdVisibility
            holdDurationGroup.visibility = holdVisibility
        }

        safetyModeGroup.setOnCheckedChangeListener {
                group,
                checkedId ->
            val selected =
                group.findViewById<MaterialRadioButton>(checkedId)
            safetyMode =
                selected?.tag as? String ?: safetyMode
            refreshSafetyControls()
        }

        refreshSafetyControls()

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
        container.addView(safetyLabel)
        container.addView(safetyDescription)
        container.addView(safetyModeGroup)
        container.addView(holdDurationLabel)
        container.addView(holdDurationGroup)
        container.addView(leftUnlockTapLabel)
        container.addView(leftUnlockTapGroup)
        container.addView(unlockTapLabel)
        container.addView(unlockTapGroup)
        container.addView(unlockTimeoutLabel)
        container.addView(unlockTimeoutGroup)
        container.addView(lockScreenSwitch)
        container.addView(gameModeOnlySwitch)
        container.addView(gameModeDescription)
        container.addView(leftUnlocksRightSwitch)
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
                .apply()

            saveTriggerSafetyConfig(
                activity,
                TriggerSafetyConfig(
                    mode = safetyMode,
                    holdDurationMs = holdDurationMs,
                    unlockTimeoutMs = unlockTimeoutMs,
                    blockOnLockScreen =
                        lockScreenSwitch.isChecked,
                    gameModeOnly =
                        gameModeOnlySwitch.isChecked,
                    leftUnlocksRight =
                        leftUnlocksRightSwitch.isChecked,
                    leftUnlockTapCount = leftUnlockTapCount,
                    rightUnlockTapCount = unlockTapCount
                )
            )

            Toast.makeText(
                activity,
                "Saved trigger mappings and safety controls",
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
