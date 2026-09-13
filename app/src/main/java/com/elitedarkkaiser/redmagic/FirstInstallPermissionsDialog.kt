package com.elitedarkkaiser.redmagic

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.elitedarkkaiser.redmagic.ui.AppTheme

object FirstInstallPermissionsDialog {
    private fun roundedFill(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(color)
        }
    }

    fun show(
        activity: MainActivity,
        onSetupComplete: () -> Unit
    ) {
        val dp = { value: Int -> (value * activity.resources.displayMetrics.density).toInt() }

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(18))
            background = AppTheme.roundedBg(AppTheme.panelColor, AppTheme.borderColor, 24f)
        }

        val title = TextView(activity).apply {
            text = "Root permission setup"
            textSize = 20f
            setTextColor(AppTheme.textPrimary)
            setTypeface(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val body = TextView(activity).apply {
            text =
                "RedMagic Control is built for rooted RedMagic 11 Pro hardware control.\n\n" +
                    "Tap Grant with root to allow:\n" +
                    "• Usage Access for Game Mode detection\n" +
                    "• Display over other apps for trigger setup\n" +
                    "• Notifications for foreground services\n" +
                    "• Phone state for Call Lighting\n\n" +
                    "Your root manager should ask for superuser access before continuing."
            textSize = 14f
            setTextColor(AppTheme.textSecondary)
            setPadding(0, dp(12), 0, dp(18))
        }

        val grantButton = Button(activity).apply {
            text = "GRANT WITH ROOT"
            textSize = 13f
            setAllCaps(false)
            setTextColor(AppTheme.textPrimary)
            background = roundedFill(AppTheme.panelPressed, 16f)
            setPadding(dp(18), dp(12), dp(18), dp(12))
        }

        container.addView(title)
        container.addView(body)
        container.addView(grantButton)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(container)
            .setCancelable(false)
            .create()

        grantButton.setOnClickListener {
            val ok = RootShell.exec(
                "appops set ${activity.packageName} GET_USAGE_STATS allow; " +
                    "appops set ${activity.packageName} SYSTEM_ALERT_WINDOW allow; " +
                    "pm grant ${activity.packageName} android.permission.POST_NOTIFICATIONS || true; " +
                    "pm grant ${activity.packageName} android.permission.READ_PHONE_STATE || true; " +
                    "settings put secure accessibility_enabled 1; " +
                    "settings put secure enabled_accessibility_services ${activity.packageName}/com.elitedarkkaiser.redmagic.TriggerAccessibilityService"
            )

            if (ok && PermissionActions.hasUsageStatsPermission(activity)) {
                setFirstInstallPermissionsPromptedStorage(activity, true)
                Toast.makeText(activity, "Root permissions applied", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onSetupComplete()
            } else {
                Toast.makeText(
                    activity,
                    "Root permission setup failed. Check your root manager and try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.65f)
        }
    }
}
