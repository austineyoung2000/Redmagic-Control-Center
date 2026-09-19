package com.elitedarkkaiser.redmagic.ui

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.elitedarkkaiser.redmagic.ProfileDialogs
import com.google.android.material.materialswitch.MaterialSwitch

object HardwareTabUi {
    fun create(activity: Activity, deps: HardwareTabDeps): LinearLayout {
        val container = deps.scrollTabContainer()

        val configureTriggersBtn = deps.actionButton("CONFIGURE TRIGGERS", false) {
            deps.showTriggerSetupDialog()
        }

        lateinit var trigEnableBtn: Button
        trigEnableBtn = deps.actionButton(
            "ENABLE TRIGGERS",
            false
        ) {
            trigEnableBtn.isEnabled = false
            trigEnableBtn.text = "ENABLING…"

            deps.enableTriggersAndService { enabled ->
                trigEnableBtn.isEnabled = true
                trigEnableBtn.text = "ENABLE TRIGGERS"

                Toast.makeText(
                    activity,
                    if (enabled) {
                        "Triggers enabled"
                    } else {
                        "Failed to enable triggers"
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        lateinit var trigDisableBtn: Button
        trigDisableBtn = deps.actionButton(
            "DISABLE TRIGGERS",
            true
        ) {
            trigDisableBtn.isEnabled = false
            trigDisableBtn.text = "DISABLING…"

            deps.disableTriggersAndService { disabled ->
                trigDisableBtn.isEnabled = true
                trigDisableBtn.text = "DISABLE TRIGGERS"

                Toast.makeText(
                    activity,
                    if (disabled) {
                        "Triggers disabled until enabled or restarted"
                    } else {
                        "Service stopped, but hardware disable failed"
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val triggerCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("⌥", "TRIGGERS"))
            addView(deps.bodyText("Map shoulder triggers to quick actions or re-enable them if the system has disabled them."))
            addView(deps.space(deps.dp(10)))

            addView(switchRow(
                activity = activity,
                label = "Intent Unlock Trigger",
                prefsName = "triggers",
                key = "intent_unlock_right_trigger",
                defaultValue = true,
                deps = deps
            ) { checked ->
                Toast.makeText(
                    activity,
                    "Intent Unlock Trigger " + if (checked) "enabled" else "disabled",
                    Toast.LENGTH_SHORT
                ).show()
            })

            addView(deps.space(deps.dp(4)))
            addView(deps.bodyText("Prevents accidental touches. Double tap to activate the right trigger, then use it normally until it times out."))

            addView(deps.space(deps.dp(8)))

            addView(switchRow(
                activity = activity,
                label = "Auto-start triggers",
                prefsName = "triggers",
                key = "triggers_auto_start",
                defaultValue = false,
                deps = deps
            ) { checked ->
                if (checked) {
                    deps.enableTriggersAndService { enabled ->
                        Toast.makeText(
                            activity,
                            if (enabled) {
                                "Auto-start triggers enabled"
                            } else {
                                "Auto-start saved, but triggers " +
                                    "could not be enabled"
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        activity,
                        "Auto-start triggers disabled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

            addView(deps.space(deps.dp(4)))
            addView(deps.bodyText("Automatically enable triggers and start the service on boot or when the app launches. Manual Disable pauses Auto Start until Enable Triggers is pressed or the phone restarts."))
            addView(deps.singleRow(configureTriggersBtn))
            addView(deps.space(deps.dp(8)))
            addView(deps.row(trigEnableBtn, trigDisableBtn))
        }

        container.addView(triggerCard)

        val masterProfilesCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("◆", "MASTER PROFILES"))
            addView(deps.bodyText("Save complete app settings or export a portable JSON backup for another installation."))

            val masterProfileList = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, deps.dp(10), 0, 0)
            }

            fun renderMasterProfiles() {
                masterProfileList.removeAllViews()
                val profiles = deps.loadMasterProfiles()

                if (profiles.isEmpty()) {
                    masterProfileList.addView(deps.subtleLabel("No saved master profiles yet"))
                    return
                }

                profiles.forEach { profile ->
                    val row = LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val applyBtn = deps.actionButton(profile.name, false) {
                        deps.applyMasterProfile(profile)
                    }.apply {
                        setPadding(deps.dp(16), deps.dp(10), deps.dp(16), deps.dp(10))
                    }

                    val deleteBtn = deps.actionButton("DEL", true) {
                        deps.deleteMasterProfile(profile.name)
                        renderMasterProfiles()
                    }.apply {
                        setPadding(deps.dp(14), deps.dp(10), deps.dp(14), deps.dp(10))
                    }

                    row.addView(
                        applyBtn,
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    )
                    row.addView(deps.space(deps.dp(8)))
                    row.addView(deleteBtn)

                    masterProfileList.addView(row)
                    masterProfileList.addView(deps.space(deps.dp(10)))
                }
            }

            val saveMasterBtn = deps.actionButton("SAVE MASTER PROFILE", false) {
                ProfileDialogs.showStyledNameOnlyDialog(
                    context = activity,
                    title = "Save Master Profile",
                    hint = "Master profile name",
                    textPrimary = AppTheme.textPrimary,
                    textSecondary = AppTheme.textSecondary,
                    panelColor = AppTheme.panelColor,
                    borderColor = AppTheme.borderColor,
                    dp = { value -> deps.dp(value) },
                    roundedBg = { fill, stroke, radius -> AppTheme.roundedBg(fill, stroke, radius.toFloat()) },
                    actionButton = { text, isDanger, onClick -> deps.actionButton(text, isDanger, onClick) },
                    space = { value -> deps.space(value) },
                    onSave = { name ->
                        deps.saveMasterProfile(name) { saved ->
                            if (saved) {
                                renderMasterProfiles()
                            }
                        }
                    }
                )
            }

            addView(deps.singleRow(saveMasterBtn))
            addView(deps.space(deps.dp(8)))
            addView(deps.row(
                deps.actionButton("EXPORT BACKUP", false) {
                    deps.exportMasterBackup()
                },
                deps.actionButton("IMPORT BACKUP", false) {
                    deps.importMasterBackup()
                }
            ))
            renderMasterProfiles()
            addView(masterProfileList)
        }

        container.addView(masterProfilesCard)

        val automationSummary = deps.subtleLabel(
            deps.automationRulesSummary()
        )

        val automationCard = deps.sectionPanel().apply {
            addView(
                deps.sectionHeader(
                    "⚙",
                    "AUTOMATION RULES"
                )
            )
            addView(
                deps.bodyText(
                    "Apply saved Master Profiles when Android reports power, battery, or restart events. No continuous polling is used."
                )
            )
            addView(deps.space(deps.dp(8)))
            addView(automationSummary)
            addView(deps.space(deps.dp(8)))
            addView(
                deps.singleRow(
                    deps.actionButton(
                        "CONFIGURE AUTOMATION",
                        false
                    ) {
                        deps.showAutomationRulesDialog {
                            automationSummary.text =
                                deps.automationRulesSummary()
                        }
                    }
                )
            )
        }

        container.addView(automationCard)

        return container
    }

    private fun switchRow(
        activity: Activity,
        label: String,
        prefsName: String,
        key: String,
        defaultValue: Boolean,
        deps: HardwareTabDeps,
        onChanged: (Boolean) -> Unit
    ): LinearLayout {
        val prefs = activity.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        val switch = MaterialSwitch(activity).apply {
            isChecked = prefs.getBoolean(key, defaultValue)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(key, checked).apply()
                onChanged(checked)
            }
        }

        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, deps.dp(8), 0, 0)

            addView(TextView(activity).apply {
                text = label
                textSize = 14f
                setTextColor(AppTheme.textPrimary)
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            addView(switch)
        }
    }
}
