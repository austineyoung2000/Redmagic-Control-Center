package com.elitedarkkaiser.redmagic

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
        val prefs = activity.getSharedPreferences(
            "triggers",
            Context.MODE_PRIVATE
        )
        val labels = listOf(
            "None",
            "Volume Up",
            "Volume Down",
            "Play / Pause",
            "Next Track",
            "Previous Track"
        )
        val values = listOf(
            "NONE",
            "VOL_UP",
            "VOL_DOWN",
            "MEDIA_PLAY_PAUSE",
            "MEDIA_NEXT",
            "MEDIA_PREVIOUS"
        )

        fun choiceIndex(value: String): Int {
            return values.indexOf(value).takeIf { it >= 0 } ?: 0
        }

        var leftChoice = choiceIndex(
            prefs.getString("left_trigger", "VOL_DOWN")
                ?: "VOL_DOWN"
        )
        var rightChoice = choiceIndex(
            prefs.getString("right_trigger", "VOL_UP")
                ?: "VOL_UP"
        )
        val style = TriggerDialogUi.Style(
            textPrimary = deps.textPrimary,
            textSecondary = deps.textSecondary,
            panelColor = deps.panelColor,
            borderColor = deps.borderColor,
            accent = deps.accent,
            typeface = deps.typeface,
            dp = deps.dp,
            roundedBg = deps.roundedBg
        )

        fun choiceSection(
            title: String,
            description: String,
            selected: () -> Int,
            update: (Int) -> Unit
        ): LinearLayout {
            val section = TriggerDialogUi.section(
                activity,
                style,
                title,
                description
            )
            val fixedChildren = section.childCount

            fun render() {
                if (section.childCount > fixedChildren) {
                    section.removeViews(
                        fixedChildren,
                        section.childCount - fixedChildren
                    )
                }

                labels.chunked(2).forEachIndexed {
                        rowIndex,
                        rowLabels ->
                    val row = TriggerDialogUi.optionRow(
                        activity,
                        style
                    )
                    rowLabels.forEachIndexed {
                            columnIndex,
                            label ->
                        val index = rowIndex * 2 + columnIndex
                        val button = TriggerDialogUi.optionButton(
                            activity,
                            style,
                            label,
                            index == selected()
                        ) {
                            update(index)
                            render()
                        }
                        TriggerDialogUi.addWeightedOption(
                            row,
                            button,
                            style,
                            addGap = columnIndex > 0
                        )
                    }
                    if (rowIndex > 0) {
                        section.addView(
                            TriggerDialogUi.space(
                                activity,
                                deps.dp(8)
                            )
                        )
                    }
                    section.addView(row)
                }
            }

            render()
            return section
        }

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                deps.dp(18),
                deps.dp(18),
                deps.dp(18),
                deps.dp(16)
            )
            background = deps.roundedBg(
                deps.panelColor,
                deps.borderColor,
                22
            )
            addView(
                TriggerDialogUi.title(
                    activity,
                    style,
                    "Trigger Mapping"
                )
            )
            addView(
                TriggerDialogUi.subtitle(
                    activity,
                    style,
                    "Assign an action to each shoulder trigger."
                )
            )
            addView(
                choiceSection(
                    "LEFT TRIGGER",
                    "Top shoulder trigger • F7",
                    { leftChoice },
                    { leftChoice = it }
                )
            )
            addView(
                TriggerDialogUi.space(
                    activity,
                    deps.dp(10)
                )
            )
            addView(
                choiceSection(
                    "RIGHT TRIGGER",
                    "Bottom shoulder trigger • F8",
                    { rightChoice },
                    { rightChoice = it }
                )
            )
        }

        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            addView(
                content,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        val root = TriggerDialogUi.dialogRoot(
            activity,
            style,
            scroll
        )
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(root)
            .setCancelable(true)
            .create()

        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, deps.dp(14), 0, 0)
        }
        val cancelButton = TriggerDialogUi.actionButton(
            activity,
            style,
            "Cancel",
            primary = false
        ) {
            dialog.dismiss()
        }
        val saveButton = TriggerDialogUi.actionButton(
            activity,
            style,
            "Save mapping",
            primary = true
        ) {
            prefs.edit()
                .putString("left_trigger", values[leftChoice])
                .putString("right_trigger", values[rightChoice])
                .apply()
            Toast.makeText(
                activity,
                "Trigger mapping saved",
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }
        TriggerDialogUi.addWeightedOption(
            buttonRow,
            cancelButton,
            style,
            addGap = false
        )
        TriggerDialogUi.addWeightedOption(
            buttonRow,
            saveButton,
            style,
            addGap = true
        )
        content.addView(buttonRow)

        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(
                ColorDrawable(android.graphics.Color.TRANSPARENT)
            )
            setDimAmount(0.65f)
        }
    }
}
