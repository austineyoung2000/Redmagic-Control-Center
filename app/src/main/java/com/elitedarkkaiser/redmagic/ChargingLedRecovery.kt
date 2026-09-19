package com.elitedarkkaiser.redmagic

import android.content.Context

object ChargingLedRecovery {
    fun repairStaleChargingOwnership(
        context: Context
    ) {
        val active =
            ChargingLedState.isActive(context)
        val charging =
            ChargingLedState.isChargingNow(context)

        if (active && !charging) {
            ChargingLedState.setActive(
                context,
                false
            )

            ModeTransitionCoordinator
                .restoreEffectiveOwner(
                    context,
                    "stale-charging-owner"
                )

            android.util.Log.i(
                "RedmagicChargingLed",
                "cleared stale charging ownership " +
                    "and reconciled the effective owner"
            )
        }
    }
}
