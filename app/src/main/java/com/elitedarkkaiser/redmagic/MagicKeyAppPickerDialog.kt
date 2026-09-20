package com.elitedarkkaiser.redmagic

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

internal object MagicKeyAppPickerDialog {
    private const val TAG = "MagicKeyAppPicker"

    data class Deps(
        val textPrimary: Int,
        val textSecondary: Int,
        val panelColor: Int,
        val borderColor: Int,
        val typeface: Typeface?,
        val dp: (Int) -> Int,
        val roundedBg: (Int, Int, Int) -> Drawable,
        val roundedFill: (Int, Int) -> Drawable,
        val space: (Int) -> View
    )

    data class MagicKeyAppItem(
        val pkg: String,
        val label: String
    )

    fun show(
        activity: MainActivity,
        targetButton: Button,
        statusLabel: TextView?,
        applyLaunchAppMagicKeyMode:
            (String, String, TextView, Button) -> Unit,
        deps: Deps
    ) {
        val status = statusLabel

        if (status == null) {
            Toast.makeText(
                activity,
                "Magic Key controls are not ready",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        targetButton.isEnabled = false
        targetButton.text = "MAGIC KEY APP: Loading…"

        Thread({
            val result = runCatching {
                loadLaunchableApps(activity)
            }

            activity.runOnUiThread {
                if (
                    activity.isFinishing ||
                    activity.isDestroyed
                ) {
                    return@runOnUiThread
                }

                targetButton.isEnabled = true
                targetButton.text =
                    "MAGIC KEY APP: " +
                        MagicKeyActions.resolveAppLabel(
                            activity,
                            savedMagicKeyAppPackageStorage(
                                activity
                            )
                        )

                result.onSuccess { apps ->
                    if (apps.isEmpty()) {
                        Toast.makeText(
                            activity,
                            "No launchable apps found",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@onSuccess
                    }

                    showSelectionDialog(
                        activity = activity,
                        targetButton = targetButton,
                        statusLabel = status,
                        apps = apps,
                        applyLaunchAppMagicKeyMode =
                            applyLaunchAppMagicKeyMode
                    )
                }.onFailure { error ->
                    android.util.Log.e(
                        TAG,
                        "Unable to load launchable apps",
                        error
                    )

                    Toast.makeText(
                        activity,
                        "Unable to load installed apps",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }, "RedMagicMagicKeyApps").apply {
            priority = Thread.NORM_PRIORITY - 1
            start()
        }
    }

    private fun loadLaunchableApps(
        activity: MainActivity
    ): List<MagicKeyAppItem> {
        val packageManager = activity.packageManager
        val launcherIntent = Intent(
            Intent.ACTION_MAIN
        ).addCategory(
            Intent.CATEGORY_LAUNCHER
        )

        @Suppress("DEPRECATION")
        return packageManager.queryIntentActivities(
            launcherIntent,
            0
        ).asSequence()
            .mapNotNull { info ->
                val pkg = info.activityInfo
                    ?.packageName
                    ?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null

                if (
                    pkg == activity.packageName ||
                    info.activityInfo?.enabled == false
                ) {
                    return@mapNotNull null
                }

                val label = runCatching {
                    info.loadLabel(packageManager)
                        .toString()
                        .trim()
                }.getOrDefault(pkg)

                MagicKeyAppItem(
                    pkg = pkg,
                    label = label.ifBlank { pkg }
                )
            }
            .distinctBy { it.pkg }
            .sortedWith(
                compareBy<MagicKeyAppItem> {
                    it.label.lowercase()
                }.thenBy {
                    it.pkg.lowercase()
                }
            )
            .toList()
    }

    private fun showSelectionDialog(
        activity: MainActivity,
        targetButton: Button,
        statusLabel: TextView,
        apps: List<MagicKeyAppItem>,
        applyLaunchAppMagicKeyMode:
            (String, String, TextView, Button) -> Unit
    ) {
        val labels = apps.map { app ->
            "${app.label}\n${app.pkg}"
        }.toTypedArray()

        val currentPackage =
            savedMagicKeyAppPackageStorage(activity)
        val currentIndex = apps.indexOfFirst {
            it.pkg == currentPackage
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("Choose Magic Key app")
            .setSingleChoiceItems(
                labels,
                currentIndex
            ) { activeDialog, which ->
                val selected = apps.getOrNull(which)
                    ?: return@setSingleChoiceItems

                activeDialog.dismiss()

                /*
                 * Launch App is a complete Magic Key mode. The
                 * hardware write changes the stock-function mode
                 * to 16 and replaces its package in one serialized
                 * root command, so the two modes cannot overlap.
                 */
                applyLaunchAppMagicKeyMode(
                    selected.pkg,
                    selected.label,
                    statusLabel,
                    targetButton
                )
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            if (
                activity.isFinishing ||
                activity.isDestroyed
            ) {
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
