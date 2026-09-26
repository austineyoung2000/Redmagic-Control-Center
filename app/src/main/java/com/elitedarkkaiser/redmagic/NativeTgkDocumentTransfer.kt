package com.elitedarkkaiser.redmagic

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object NativeTgkDocumentTransfer {
    const val EXPORT_REQUEST = 8211
    const val IMPORT_REQUEST = 8212

    private var exportRequested = false
    private var exportPackageName: String? = null

    fun requestExport(
        activity: Activity,
        packageName: String? = null,
        appLabel: String? = null
    ) {
        exportRequested = true
        exportPackageName = packageName

        val safeLabel = appLabel
            ?.lowercase()
            ?.replace(Regex("[^a-z0-9._-]+"), "-")
            ?.trim('-')
            ?.takeIf { it.isNotBlank() }
        val filename = if (packageName == null) {
            "redmagic-tgk-profiles.json"
        } else {
            "redmagic-tgk-${safeLabel ?: packageName}.json"
        }

        activity.startActivityForResult(
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, filename)
            },
            EXPORT_REQUEST
        )
    }

    fun requestImport(activity: Activity) {
        activity.startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            },
            IMPORT_REQUEST
        )
    }

    fun handleActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): Boolean {
        if (
            requestCode != EXPORT_REQUEST &&
            requestCode != IMPORT_REQUEST
        ) {
            return false
        }

        if (resultCode != Activity.RESULT_OK) {
            if (requestCode == EXPORT_REQUEST) {
                clearExportRequest()
            }
            return true
        }

        val uri = data?.data
        if (uri == null) {
            clearExportRequest()
            showToast(activity, "No document was selected")
            return true
        }

        if (requestCode == EXPORT_REQUEST) {
            handleExport(activity, uri)
        } else {
            handleImport(activity, uri)
        }

        return true
    }

    private fun handleExport(
        activity: Activity,
        uri: Uri
    ) {
        val packageName = exportPackageName
        val requested = exportRequested
        clearExportRequest()

        if (!requested) {
            showToast(activity, "TGK export request expired")
            return
        }

        Thread(
            {
                val error = runCatching {
                    val json = NativeTgkStorage.createExportJson(
                        activity,
                        packageName
                    )
                    activity.contentResolver.openOutputStream(
                        uri,
                        "wt"
                    )?.bufferedWriter()?.use {
                        it.write(json)
                    } ?: error("Unable to open export destination")
                }.exceptionOrNull()

                activity.runOnUiThread {
                    showToast(
                        activity,
                        error?.message ?: "TGK profiles exported"
                    )
                }
            },
            "RedMagicTgkExport"
        ).start()
    }

    private fun handleImport(
        activity: Activity,
        uri: Uri
    ) {
        Thread(
            {
                val loaded = runCatching {
                    val raw = activity.contentResolver
                        .openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: error("Unable to read TGK profile file")
                    val packages =
                        NativeTgkStorage.importedPackageNames(raw)
                    val existing = NativeTgkStorage
                        .readProfiles(activity)
                        .map { it.packageName }
                        .toSet()

                    ImportDocument(
                        raw = raw,
                        conflictCount =
                            packages.intersect(existing).size
                    )
                }

                activity.runOnUiThread {
                    loaded.onSuccess { document ->
                        if (document.conflictCount == 0) {
                            importDocument(
                                activity,
                                document.raw,
                                replaceExisting = true
                            )
                        } else {
                            showConflictDialog(activity, document)
                        }
                    }.onFailure {
                        showToast(
                            activity,
                            it.message ?: "TGK import failed"
                        )
                    }
                }
            },
            "RedMagicTgkImportRead"
        ).start()
    }

    private fun showConflictDialog(
        activity: Activity,
        document: ImportDocument
    ) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Existing TGK mappings found")
            .setMessage(
                "${document.conflictCount} imported app mapping(s) " +
                    "already exist. Replace them or keep the " +
                    "current versions?"
            )
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Keep existing") { _, _ ->
                importDocument(
                    activity,
                    document.raw,
                    replaceExisting = false
                )
            }
            .setPositiveButton("Replace") { _, _ ->
                importDocument(
                    activity,
                    document.raw,
                    replaceExisting = true
                )
            }
            .show()
    }

    private fun importDocument(
        activity: Activity,
        raw: String,
        replaceExisting: Boolean
    ) {
        Thread(
            {
                val result = runCatching {
                    NativeTgkRuntimeState.clear()
                    NativeTgkCoordinator.disable(
                        activity.applicationContext,
                        "TGK profiles imported"
                    )
                    NativeTgkStorage.importProfilesJson(
                        context = activity,
                        raw = raw,
                        replaceExisting = replaceExisting
                    )
                }

                activity.runOnUiThread {
                    result.onSuccess {
                        showToast(
                            activity,
                            "Imported ${it.importedCount}, " +
                                "replaced ${it.replacedCount}, " +
                                "skipped ${it.skippedCount} TGK profile(s)"
                        )
                        NativeTgkProfileDialog.show(activity)
                    }.onFailure {
                        showToast(
                            activity,
                            it.message ?: "TGK import failed"
                        )
                    }
                }
            },
            "RedMagicTgkImportApply"
        ).start()
    }

    private fun clearExportRequest() {
        exportRequested = false
        exportPackageName = null
    }

    private fun showToast(
        activity: Activity,
        message: String
    ) {
        Toast.makeText(
            activity,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private data class ImportDocument(
        val raw: String,
        val conflictCount: Int
    )
}
