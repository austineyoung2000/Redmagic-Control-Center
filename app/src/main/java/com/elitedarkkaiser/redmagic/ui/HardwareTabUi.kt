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

        val triggerCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("⌥", "TRIGGERS"))
            addView(deps.bodyText("Map shoulder triggers to quick actions or re-enable them if the system has disabled them."))
            addView(deps.space(deps.dp(10)))

            addView(switchRow(
                activity = activity,
                label = "Haptic feedback",
                prefsName = "triggers",
                key = "haptics_enabled",
                defaultValue = true,
                deps = deps
            ) { checked ->
                Toast.makeText(
                    activity,
                    "Haptics " + if (checked) "enabled" else "disabled",
                    Toast.LENGTH_SHORT
                ).show()
            })

            addView(deps.space(deps.dp(8)))

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
            addView(deps.bodyText("Automatically enable triggers and start the service on boot or when the app launches."))
            addView(deps.row(configureTriggersBtn, trigEnableBtn))
        }

        lateinit var vibrateBtn: Button
        vibrateBtn = deps.actionButton("TEST HAPTIC", false) {
            vibrateBtn.isEnabled = false
            vibrateBtn.text = "TESTING…"

            deps.testHaptic { sent ->
                vibrateBtn.isEnabled = true
                vibrateBtn.text = "TEST HAPTIC"

                Toast.makeText(
                    activity,
                    if (sent) {
                        "Haptic test sent"
                    } else {
                        "Haptic test failed"
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val hapticsCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("≈", "HAPTICS"))
            addView(deps.bodyText("Quick vibration test for hardware haptics."))
            addView(deps.space(deps.dp(10)))
            addView(deps.singleRow(vibrateBtn))
        }

        container.addView(triggerCard)
        container.addView(hapticsCard)

        val profilesCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("★", "HARDWARE PROFILES"))
            addView(deps.bodyText("Save and apply full hardware presets for fan, pump, LEDs, triggers, and haptics."))

            val profileList = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, deps.dp(10), 0, 0)
            }

            fun renderProfiles() {
                val profiles = deps.loadProfiles()
                ProfileDialogs.renderProfiles(
                    context = activity,
                    profileList = profileList,
                    profiles = profiles,
                    subtleLabel = { text -> deps.subtleLabel(text) },
                    actionButton = { text, isDanger, onClick -> deps.actionButton(text, isDanger, onClick) },
                    space = { value -> deps.space(value) },
                    dp = { value -> deps.dp(value) },
                    onApplyProfile = { profile ->
                        deps.applyHardwareProfile(profile) { applied ->
                            if (applied) {
                                deps.applyProfileToUiState(profile)
                            }

                            Toast.makeText(
                                activity,
                                if (applied) {
                                    "Applied ${profile.name}"
                                } else {
                                    "Failed to apply ${profile.name}"
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onDeleteProfile = { profile ->
                        deps.showDeleteProfileDialog(profile.name) {
                            renderProfiles()
                        }
                    }
                )
            }

            val saveBtn = deps.actionButton("SAVE CURRENT PROFILE", false) {
                deps.showSaveProfileDialog {
                    renderProfiles()
                }
            }

            addView(deps.singleRow(saveBtn))
            renderProfiles()
            addView(profileList)
        }

        container.addView(profilesCard)

        val masterProfilesCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("◆", "MASTER PROFILES"))
            addView(deps.bodyText("Save and restore a full app snapshot including hardware, Game Mode, charging LEDs, fan curves, pump, and triggers."))

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
            renderMasterProfiles()
            addView(masterProfileList)
        }

        container.addView(masterProfilesCard)

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
