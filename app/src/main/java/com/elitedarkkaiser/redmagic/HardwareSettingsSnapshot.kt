package com.elitedarkkaiser.redmagic

data class HardwareSettingsSnapshot(
    val fanEnabled: Boolean,
    val fanLevel: Int,
    val autoFanEnabled: Boolean,
    val fanCurveMode: String,
    val pumpEnabled: Boolean,
    val pumpProfile: String,
    val autoPumpEnabled: Boolean,
    val fanLedEnabled: Boolean,
    val fanLedEffect: String,
    val fanLedColor: Int,
    val logoLedEnabled: Boolean,
    val logoLedEffect: String,
    val logoLedColor: Int,
    val shoulderLedEnabled: Boolean,
    val shoulderLedEffect: String,
    val shoulderLedColor: Int
)
