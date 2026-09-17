package com.elitedarkkaiser.redmagic

import android.content.Context

enum class LedOwner {
    NONE,
    NORMAL,
    GAME_MODE,
    CALL,
    CHARGING,
    RGB_CYCLE
}

object LedOwnership {
    fun current(context: Context): LedOwner {
        return when {
            ChargingLedState.isActive(context) -> LedOwner.CHARGING
            CallLightingState.isActive(context) -> LedOwner.CALL
            isGameModeLedOverrideActiveStorage(context) -> LedOwner.GAME_MODE
            RgbStudioStorage.isEnabled(context) -> LedOwner.RGB_CYCLE
            else -> LedOwner.NORMAL
        }
    }

    fun canNormalApply(context: Context): Boolean {
        return current(context) == LedOwner.NORMAL
    }

    fun canGameModeApply(context: Context): Boolean {
        val owner = current(context)
        return owner == LedOwner.NORMAL || owner == LedOwner.GAME_MODE
    }

    fun canCallApply(context: Context): Boolean {
        val owner = current(context)
        return owner != LedOwner.CHARGING
    }
}
