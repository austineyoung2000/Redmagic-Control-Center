package com.elitedarkkaiser.redmagic.ui

import android.app.Activity
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.materialswitch.MaterialSwitch

object LightingTabUi {
    fun create(activity: Activity, deps: LightingTabDeps): LinearLayout {
        val container = deps.scrollTabContainer()

        val previewCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("⚡", "PREVIEW"))
            addView(TextView(activity).apply {
                text = "Apply LED color/effect changes instantly while selecting"
                textSize = 13f
                setTextColor(AppTheme.textSecondary)
                setPadding(0, 0, 0, deps.dp(10))
            })

            val previewSwitch = MaterialSwitch(activity).apply {
                isChecked = deps.getRealTimePreviewEnabled()
                setOnCheckedChangeListener { _, checked ->
                    deps.setRealTimePreviewEnabled(checked)
                    deps.saveRealTimePreviewEnabled(checked)
                }
            }

            val previewRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(activity).apply {
                    text = "Real-time preview"
                    textSize = 14f
                    setTextColor(AppTheme.textPrimary)
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(previewSwitch)
            }

            addView(previewRow)
        }

        val zonesCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("✦", "LED ZONES"))
            addView(deps.bodyText("Configure fan LEDs, logo lighting, and shoulder strip effects separately."))
            addView(deps.singleRow(deps.actionButton("FAN LED", false) {
                deps.showFanLedDialog()
            }))
            addView(deps.row(
                deps.actionButton("LOGO LED", false) { deps.showLogoLedDialog() },
                deps.actionButton("SHOULDER LEDS", false) { deps.showShoulderLedDialog() }
            ))
        }

        val gameModeCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("🎮", "GAME MODE"))
            addView(deps.bodyText("Pick apps and configure a full hardware profile to apply while a game is active."))
            addView(deps.infoRow("Apps", deps.subtleLabel(deps.gameModeAppsSummary())))
            addView(deps.row(
                deps.actionButton("SELECT APPS", false) { deps.showGameModeAppPicker() },
                deps.actionButton("GAME PROFILE", false) { deps.showGameModeProfileDialog() }
            ))
        }

        val chargingModeCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("⚡", "CHARGING MODE"))
            addView(deps.bodyText("Applies only while the device is plugged in and charging. Charging Mode takes LED priority over Game Mode and normal LED profiles."))

            val chargingSwitch = MaterialSwitch(activity).apply {
                isChecked = deps.getChargingLedEnabled()
                setOnCheckedChangeListener { _, checked ->
                    deps.setChargingLedEnabled(checked)
                }
            }

            val chargingRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(activity).apply {
                    text = "Enable charging LED profile"
                    textSize = 14f
                    setTextColor(AppTheme.textPrimary)
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(chargingSwitch)
            }

            addView(chargingRow)
            addView(deps.infoRow("Priority", deps.subtleLabel("Charging Mode > Game Mode > Normal LEDs")))
            addView(deps.singleRow(deps.actionButton("CHARGING FAN LED", false) {
                deps.showChargingFanLedDialog()
            }))
            addView(deps.row(
                deps.actionButton("CHARGING LOGO LED", false) { deps.showChargingLogoLedDialog() },
                deps.actionButton("CHARGING SHOULDER LEDS", false) { deps.showChargingShoulderLedDialog() }
            ))
        }

        container.addView(previewCard)
        container.addView(zonesCard)
        val callLightingCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("☎", "CALL LIGHTING"))
            addView(deps.bodyText("Applies only during incoming calls and connected calls. Priority: Charging Mode > Call Lighting > Game Mode > Normal LEDs."))

            val callSwitch = MaterialSwitch(activity).apply {
                isChecked = deps.getCallLightingEnabled()
                setOnCheckedChangeListener { _, checked ->
                    deps.setCallLightingEnabled(checked)
                }
            }

            val callRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(activity).apply {
                    text = "Enable call lighting"
                    textSize = 14f
                    setTextColor(AppTheme.textPrimary)
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(callSwitch)
            }

            val pauseFanSwitch = MaterialSwitch(activity).apply {
                isChecked = deps.getPauseFanDuringCalls()
                setOnCheckedChangeListener { _, checked ->
                    deps.setPauseFanDuringCalls(checked)
                }
            }

            val pauseFanRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, deps.dp(8), 0, 0)
                addView(TextView(activity).apply {
                    text = "Pause fan during calls"
                    textSize = 14f
                    setTextColor(AppTheme.textPrimary)
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(pauseFanSwitch)
            }

            addView(callRow)
            addView(pauseFanRow)
            addView(deps.bodyText("Temporarily turns the fan off while a call is ringing or connected, then restores the previous fan state after the call."))
            addView(deps.row(
                deps.actionButton("INCOMING CALL PROFILE", false) { deps.showIncomingCallProfileDialog() },
                deps.actionButton("CONNECTED CALL PROFILE", false) { deps.showConnectedCallProfileDialog() }
            ))
        }

        container.addView(gameModeCard)
        container.addView(callLightingCard)
        container.addView(chargingModeCard)

        return container
    }
}
