package com.elitedarkkaiser.redmagic

import android.content.Context

private const val TRIGGER_PREFS_NAME = "triggers"
private const val LEFT_TRIGGER_KEY = "left_trigger"
private const val RIGHT_TRIGGER_KEY = "right_trigger"
private const val INTENT_UNLOCK_RIGHT_TRIGGER_KEY = "intent_unlock_right_trigger"
private const val TRIGGERS_AUTO_START_KEY = "triggers_auto_start"
private const val TRIGGERS_DISABLED_UNTIL_RESTART_KEY =
    "triggers_disabled_until_restart"

data class TriggerPrefsSnapshot(
    val triggerEnabled: Boolean,
    val leftTriggerAction: String,
    val rightTriggerAction: String,
    val intentUnlockRightTrigger: Boolean,
    val triggersAutoStart: Boolean
)

fun readTriggerPrefsSnapshot(context: Context): TriggerPrefsSnapshot {
    val prefs = context.getSharedPreferences(TRIGGER_PREFS_NAME, Context.MODE_PRIVATE)
    val autoStart = prefs.getBoolean(TRIGGERS_AUTO_START_KEY, false)
    return TriggerPrefsSnapshot(
        triggerEnabled = autoStart,
        leftTriggerAction = prefs.getString(LEFT_TRIGGER_KEY, "NONE") ?: "NONE",
        rightTriggerAction = prefs.getString(RIGHT_TRIGGER_KEY, "NONE") ?: "NONE",
        intentUnlockRightTrigger = prefs.getBoolean(INTENT_UNLOCK_RIGHT_TRIGGER_KEY, true),
        triggersAutoStart = autoStart
    )
}

fun saveTriggerPrefsStorage(context: Context, profile: HardwareProfile) {
    context.getSharedPreferences(TRIGGER_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(LEFT_TRIGGER_KEY, profile.leftTriggerAction)
        .putString(RIGHT_TRIGGER_KEY, profile.rightTriggerAction)
        .putBoolean(INTENT_UNLOCK_RIGHT_TRIGGER_KEY, profile.intentUnlockRightTrigger)
        .putBoolean(TRIGGERS_AUTO_START_KEY, profile.triggersAutoStart)
        .apply()
}

fun initDefaultTriggerMappingsStorage(context: Context) {
    val prefs = context.getSharedPreferences(TRIGGER_PREFS_NAME, Context.MODE_PRIVATE)
    if (!prefs.contains(LEFT_TRIGGER_KEY)) {
        prefs.edit()
            .putString(LEFT_TRIGGER_KEY, "VOL_DOWN")
            .putString(RIGHT_TRIGGER_KEY, "VOL_UP")
            .apply()
    }
}

fun triggersDisabledUntilRestartStorage(
    context: Context
): Boolean {
    return context.getSharedPreferences(
        TRIGGER_PREFS_NAME,
        Context.MODE_PRIVATE
    ).getBoolean(
        TRIGGERS_DISABLED_UNTIL_RESTART_KEY,
        false
    )
}

fun setTriggersDisabledUntilRestartStorage(
    context: Context,
    disabled: Boolean
) {
    context.getSharedPreferences(
        TRIGGER_PREFS_NAME,
        Context.MODE_PRIVATE
    ).edit()
        .putBoolean(
            TRIGGERS_DISABLED_UNTIL_RESTART_KEY,
            disabled
        )
        .apply()
}
