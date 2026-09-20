package com.elitedarkkaiser.redmagic

import android.content.Context
import com.elitedarkkaiser.redmagic.storage.AppPrefs

data class MagicKeyShortcutTarget(
    val packageName: String,
    val shortcutId: String,
    val label: String
)

fun isValidMagicKeyShortcutId(value: String): Boolean {
    return value.isNotBlank() &&
        value.length <= 200 &&
        ';' !in value &&
        value.none { it.isISOControl() }
}

private const val MAGIC_KEY_SHORTCUT_PACKAGE =
    "magic_key_shortcut_package"
private const val MAGIC_KEY_SHORTCUT_ID =
    "magic_key_shortcut_id"
private const val MAGIC_KEY_SHORTCUT_LABEL =
    "magic_key_shortcut_label"

fun savedMagicKeyAppPackageStorage(context: Context): String? {
    return context
        .getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        .getString(AppPrefs.MAGIC_KEY_APP_PACKAGE, null)
}

fun saveMagicKeyAppPackageStorage(context: Context, pkg: String?) {
    context
        .getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(AppPrefs.MAGIC_KEY_APP_PACKAGE, pkg)
        .apply()
}

fun savedMagicKeyShortcutStorage(
    context: Context
): MagicKeyShortcutTarget? {
    val prefs = context.getSharedPreferences(
        AppPrefs.PREFS_NAME,
        Context.MODE_PRIVATE
    )
    val packageName = prefs.getString(
        MAGIC_KEY_SHORTCUT_PACKAGE,
        null
    )?.takeIf { it.isNotBlank() } ?: return null
    val shortcutId = prefs.getString(
        MAGIC_KEY_SHORTCUT_ID,
        null
    )?.takeIf { it.isNotBlank() } ?: return null
    val label = prefs.getString(
        MAGIC_KEY_SHORTCUT_LABEL,
        null
    )?.takeIf { it.isNotBlank() } ?: shortcutId

    return MagicKeyShortcutTarget(
        packageName = packageName,
        shortcutId = shortcutId,
        label = label
    )
}

fun saveMagicKeyShortcutStorage(
    context: Context,
    target: MagicKeyShortcutTarget?
) {
    val editor = context.getSharedPreferences(
        AppPrefs.PREFS_NAME,
        Context.MODE_PRIVATE
    ).edit()

    if (target == null) {
        editor
            .remove(MAGIC_KEY_SHORTCUT_PACKAGE)
            .remove(MAGIC_KEY_SHORTCUT_ID)
            .remove(MAGIC_KEY_SHORTCUT_LABEL)
    } else {
        editor
            .putString(
                MAGIC_KEY_SHORTCUT_PACKAGE,
                target.packageName
            )
            .putString(
                MAGIC_KEY_SHORTCUT_ID,
                target.shortcutId
            )
            .putString(
                MAGIC_KEY_SHORTCUT_LABEL,
                target.label
            )
    }

    editor.apply()
}
