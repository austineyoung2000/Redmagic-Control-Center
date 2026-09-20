package com.elitedarkkaiser.redmagic

import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

internal object MagicKeyShortcutDialog {
    private const val TAG = "MagicKeyShortcuts"

    private data class ShortcutItem(
        val packageName: String,
        val shortcutId: String,
        val label: String
    )

    fun show(
        activity: MainActivity,
        targetButton: Button,
        appButton: Button?,
        statusLabel: TextView?,
        pickerDeps: MagicKeyAppPickerDialog.Deps,
        runBackground: (() -> Unit) -> Boolean,
        refreshStatus: () -> Unit
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

        val saved = savedMagicKeyShortcutStorage(activity)

        MagicKeyAppPickerDialog.chooseApp(
            activity = activity,
            title = "Choose Shortcut App",
            subtitle =
                "Pick an app, then choose one of its Android shortcuts",
            selectedPackage = saved?.packageName,
            deps = pickerDeps
        ) { app ->
            targetButton.isEnabled = false
            targetButton.text = "MAGIC KEY SHORTCUT: Loading…"

            val submitted = runBackground {
                val result = runCatching {
                    loadShortcuts(
                        packageName = app.pkg
                    )
                }

                targetButton.post {
                    if (
                        activity.isFinishing ||
                        activity.isDestroyed
                    ) {
                        return@post
                    }

                    restoreButton(activity, targetButton)

                    result.onSuccess { shortcuts ->
                        if (shortcuts.isEmpty()) {
                            Toast.makeText(
                                activity,
                                "${app.label} exposes no usable shortcuts",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            showShortcutSelection(
                                activity = activity,
                                appLabel = app.label,
                                shortcuts = shortcuts,
                                selected = saved,
                                onSelected = { shortcut ->
                                    MagicKeyActions
                                        .applyLaunchShortcutMode(
                                            activity = activity,
                                            target =
                                                MagicKeyShortcutTarget(
                                                    packageName =
                                                        shortcut.packageName,
                                                    shortcutId =
                                                        shortcut.shortcutId,
                                                    label =
                                                        shortcut.label
                                                ),
                                            statusLabel = status,
                                            shortcutButton =
                                                targetButton,
                                            appButton = appButton,
                                            runBackground =
                                                runBackground,
                                            refreshStatus =
                                                refreshStatus
                                        )
                                }
                            )
                        }
                    }.onFailure { error ->
                        android.util.Log.e(
                            TAG,
                            "Unable to load shortcuts for ${app.pkg}",
                            error
                        )
                        Toast.makeText(
                            activity,
                            "Unable to read ${app.label} shortcuts",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            if (!submitted) {
                restoreButton(activity, targetButton)
                Toast.makeText(
                    activity,
                    "Unable to start shortcut discovery",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadShortcuts(
        packageName: String
    ): List<ShortcutItem> {
        if (!packageName.matches(
                Regex("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+")
            )
        ) {
            return emptyList()
        }

        val output = RootShell.execForOutput(
            "cmd shortcut get-shortcuts --user 0 " +
                "--flags 25 $packageName"
        ) ?: return emptyList()

        val matches = Regex(
            "ShortcutInfo \\{id=([^,}\\n]+)"
        ).findAll(output).toList()

        return matches.mapNotNull { match ->
            val id = match.groupValues[1].trim()
            if (!isValidMagicKeyShortcutId(id)) {
                return@mapNotNull null
            }

            val end = matches
                .firstOrNull {
                    it.range.first > match.range.first
                }
                ?.range
                ?.first
                ?: output.length
            val block = output.substring(
                match.range.first,
                end
            )
            val label = Regex(
                "shortLabel=([^,\\n]+)"
            ).find(block)
                ?.groupValues
                ?.get(1)
                ?.trim()
                ?.takeIf {
                    it.isNotBlank() && it != "null"
                }
                ?: id

            ShortcutItem(
                packageName = packageName,
                shortcutId = id,
                label = label
            )
        }.distinctBy {
            it.shortcutId
        }.sortedWith(
            compareBy<ShortcutItem> {
                it.label.lowercase()
            }.thenBy {
                it.shortcutId.lowercase()
            }
        )
    }

    private fun showShortcutSelection(
        activity: MainActivity,
        appLabel: String,
        shortcuts: List<ShortcutItem>,
        selected: MagicKeyShortcutTarget?,
        onSelected: (ShortcutItem) -> Unit
    ) {
        val labels = shortcuts.map {
            "${it.label}\n${it.shortcutId}"
        }.toTypedArray()
        val selectedIndex = shortcuts.indexOfFirst {
            it.packageName == selected?.packageName &&
                it.shortcutId == selected.shortcutId
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("Choose $appLabel Shortcut")
            .setSingleChoiceItems(
                labels,
                selectedIndex
            ) { activeDialog, which ->
                activeDialog.dismiss()
                onSelected(shortcuts[which])
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun restoreButton(
        activity: MainActivity,
        button: Button
    ) {
        val saved = savedMagicKeyShortcutStorage(activity)
        button.isEnabled = true
        button.text =
            "MAGIC KEY SHORTCUT: " +
                (saved?.label ?: "Choose Shortcut")
    }
}
