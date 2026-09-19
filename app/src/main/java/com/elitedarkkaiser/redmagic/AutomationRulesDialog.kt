package com.elitedarkkaiser.redmagic

import android.app.Activity
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.elitedarkkaiser.redmagic.ui.AppTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object AutomationRulesDialog {
    fun show(
        activity: Activity,
        onSaved: () -> Unit
    ) {
        val profileNames = MasterProfileStorage
            .loadProfiles(activity)
            .map { it.name }
            .sortedBy { it.lowercase() }

        val options = listOf("Off") + profileNames
        val selections =
            linkedMapOf<AutomationRuleEvent, Spinner>()

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                AppTheme.dp(activity, 24),
                AppTheme.dp(activity, 8),
                AppTheme.dp(activity, 24),
                AppTheme.dp(activity, 8)
            )

            addView(TextView(activity).apply {
                text = if (profileNames.isEmpty()) {
                    "Save a Master Profile first. Rules remain off until a profile exists."
                } else {
                    "Choose a Master Profile for each event. Rules run only when Android reports that event and do not poll in the background."
                }
                textSize = 14f
                setTextColor(AppTheme.textSecondary)
                setPadding(
                    0,
                    0,
                    0,
                    AppTheme.dp(activity, 12)
                )
            })

            AutomationRuleEvent.entries.forEach { event ->
                addView(TextView(activity).apply {
                    text = event.title
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(AppTheme.textPrimary)
                    setPadding(
                        0,
                        AppTheme.dp(activity, 10),
                        0,
                        AppTheme.dp(activity, 4)
                    )
                })

                val spinner = Spinner(activity).apply {
                    adapter = ArrayAdapter(
                        activity,
                        android.R.layout
                            .simple_spinner_dropdown_item,
                        options
                    )

                    val saved =
                        AutomationRulesStorage
                            .profileName(
                                activity,
                                event
                            )
                    setSelection(
                        profileNames.indexOf(saved)
                            .takeIf { it >= 0 }
                            ?.plus(1)
                            ?: 0
                    )
                }

                selections[event] = spinner
                addView(
                    spinner,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }
        }

        val scroll = ScrollView(activity).apply {
            addView(content)
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("Automation Rules")
            .setView(scroll)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE") { _, _ ->
                selections.forEach { (event, spinner) ->
                    val index = spinner.selectedItemPosition
                    AutomationRulesStorage.saveProfileName(
                        activity,
                        event,
                        profileNames.getOrNull(index - 1)
                    )
                }
                onSaved()
            }
            .show()
    }
}
