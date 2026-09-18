package com.elitedarkkaiser.redmagic.ui

import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.elitedarkkaiser.redmagic.DashboardSnapshot
import com.elitedarkkaiser.redmagic.R

object HomeTabUi {
    data class Refs(
        val deviceModelValue: TextView,
        val deviceRomValue: TextView,
        val deviceCpuValue: TextView,
        val deviceRamValue: TextView,
        val rootChip: TextView,
        val fanChip: TextView,
        val rpmChip: TextView,
        val tempChip: TextView
    )

    data class Result(
        val view: LinearLayout,
        val refs: Refs
    )

    fun create(deps: HomeTabDeps): Result {
        val container = deps.scrollTabContainer()

        if (!deps.hasUsageStatsPermission()) {
            val usageCard = deps.sectionPanel().apply {
                val title = TextView(context).apply {
                    text = "Game Mode Permission Required"
                    textSize = 16f
                    setTextColor(AppTheme.textPrimary)
                    typeface = android.graphics.Typeface.create(AppTheme.appTypeface, android.graphics.Typeface.BOLD)
                }

                val desc = TextView(context).apply {
                    text = "Grant Usage Access so Game Mode can detect running games."
                    textSize = 13f
                    setTextColor(AppTheme.textSecondary)
                    setPadding(0, deps.dp(6), 0, deps.dp(10))
                }

                val btn = deps.actionButton(
                    "Grant Usage Access",
                    false
                ) {
                    deps.openUsageStatsAccessSettings()
                }

                addView(title)
                addView(desc)
                addView(btn)
            }

            container.addView(usageCard)

            val gameSelectBtn = deps.actionButton(
                "Select Games for Game Mode",
                false
            ) {
                deps.showGamePickerDialog()
            }

            container.addView(gameSelectBtn)

            val gameStatus = TextView(container.context).apply {
                textSize = 13f
                setTextColor(AppTheme.textSecondary)
                setPadding(0, deps.dp(8), 0, deps.dp(8))
            }

            deps.updateGameModeStatusUI(gameStatus)
            container.addView(gameStatus)
        }

        val welcomeCard = deps.sectionPanel().apply {
            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val iconView = ImageView(context).apply {
                setImageResource(R.mipmap.ic_launcher)
                layoutParams = LinearLayout.LayoutParams(deps.dp(60), deps.dp(60))
            }

            val titleView = deps.ledTitleText("Redmagic Control Center")

            headerRow.addView(iconView)
            headerRow.addView(titleView)

            addView(headerRow)
            addView(deps.subtitleText("Cooling, lighting, triggers and hardware controls for Redmagic 11 Pro"))
        }

        val summaryCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("⌂", "WELCOME"))
            addView(deps.bodyText("RedMagic HW Controls is a root-powered control center for RedMagic 11 Pro that brings key hardware features into one place with a cleaner interface than stock tools."))
            addView(deps.bodyText("It lets you manage cooling behavior, fan profiles, micropump control, fan LED effects, logo lighting, shoulder LED strips, trigger tools, and slider actions directly from the app."))
            addView(deps.bodyText("The app is built around real device paths and behavior confirmed on hardware so the controls feel practical, focused, and close to an OEM-style utility."))

            val linksRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, deps.dp(14), 0, 0)
            }

            val githubBtn = deps.segmentedChip("GitHub", false) {
                deps.openUrl("https://github.com/austineyoung2000/Red")
            }

            val referenceBtn = deps.segmentedChip("Reference", false) {
                deps.openUrl("https://www.reddit.com/r/RedMagic/comments/1rtoako/red_magic_11_pro_hardware_control_guide_for/")
            }

            linksRow.addView(githubBtn)
            linksRow.addView(deps.space(deps.dp(8)))
            linksRow.addView(referenceBtn)

            addView(linksRow)
        }

        val deviceModelValue = deps.infoValue()
        val deviceRomValue = deps.infoValue()
        val deviceCpuValue = deps.infoValue()
        val deviceRamValue = deps.infoValue()

        val infoCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("ⓘ", "DEVICE INFO"))
            addView(deps.infoRow("Model", deviceModelValue))
            addView(deps.infoRow("ROM", deviceRomValue))
            addView(deps.infoRow("CPU", deviceCpuValue))
            addView(deps.infoRow("RAM", deviceRamValue))
        }

        val rootChip = deps.statusChip("ROOT --")
        val fanChip = deps.statusChip("FAN --")
        val rpmChip = deps.statusChip("RPM --")
        val tempChip = deps.statusChip("TEMP --")

        val statusRow = LinearLayout(container.context).apply {
            orientation = LinearLayout.HORIZONTAL

            addView(rootChip, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = deps.dp(6) })

            addView(fanChip, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = deps.dp(6) })

            addView(rpmChip, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = deps.dp(6) })

            addView(tempChip, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        val statusScroller = HorizontalScrollView(container.context).apply {
            isHorizontalScrollBarEnabled = false
            addView(statusRow)
        }

        val statusCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("◎", "LIVE STATUS"))
            addView(statusScroller)
        }

        val diagnosticsCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("⌁", "DIAGNOSTICS"))
            addView(deps.bodyText(deps.deviceScanSummary()))
        }

        container.addView(welcomeCard)

        val dashboardCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("◈", "LIVE DASHBOARD"))

            val dashboardText = TextView(context).apply {
                text = "Loading dashboard…"
                textSize = 13f
                setTextColor(AppTheme.textPrimary)
                setLineSpacing(0f, 1.15f)
                setPadding(0, 0, 0, deps.dp(12))
            }

            lateinit var refreshBtn: Button

            fun refreshDashboard() {
                refreshBtn.isEnabled = false
                refreshBtn.text = "REFRESHING…"

                val submitted = deps.runBackground {
                    val summary =
                        DashboardSnapshot.buildSummary(context)

                    dashboardText.post {
                        dashboardText.text = summary
                        refreshBtn.text = "REFRESH DASHBOARD"
                        refreshBtn.isEnabled = true
                    }
                }

                if (!submitted) {
                    dashboardText.text =
                        "Dashboard refresh unavailable"
                    refreshBtn.text = "REFRESH DASHBOARD"
                    refreshBtn.isEnabled = true
                }
            }

            refreshBtn = deps.actionButton(
                "REFRESH DASHBOARD",
                false
            ) {
                refreshDashboard()
            }

            addView(dashboardText)
            addView(deps.singleRow(refreshBtn))

            dashboardText.post {
                refreshDashboard()
            }
        }

        val automationCard = deps.sectionPanel().apply {
            addView(deps.sectionHeader("⚙", "AUTOMATION"))
            addView(deps.bodyText("Auto Pump uses safe temperature rules and automatically shifts between Slow, Medium, and Quick."))
        }

        container.addView(summaryCard)
        container.addView(infoCard)
        container.addView(statusCard)
        container.addView(diagnosticsCard)
        container.addView(dashboardCard)
        container.addView(automationCard)

        return Result(
            view = container,
            refs = Refs(
                deviceModelValue = deviceModelValue,
                deviceRomValue = deviceRomValue,
                deviceCpuValue = deviceCpuValue,
                deviceRamValue = deviceRamValue,
                rootChip = rootChip,
                fanChip = fanChip,
                rpmChip = rpmChip,
                tempChip = tempChip
            )
        )
    }
}
