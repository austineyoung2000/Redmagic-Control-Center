package com.elitedarkkaiser.redmagic

internal object ProfileStateHelpers {

    data class ProfileInputs(
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
        val shoulderLedColor: Int,

        val triggerEnabled: Boolean,
        val leftTriggerAction: String,
        val rightTriggerAction: String,
        val intentUnlockRightTrigger: Boolean,
        val triggersAutoStart: Boolean
    )

    fun buildCurrentHardwareProfile(
        name: String,
        input: ProfileInputs
    ): HardwareProfile {
        return HardwareProfile(
            name = name,

            fanEnabled = input.fanEnabled,
            fanLevel = input.fanLevel,
            autoFanEnabled = input.autoFanEnabled,
            fanCurveMode = input.fanCurveMode,

            pumpEnabled = input.pumpEnabled,
            pumpProfile = input.pumpProfile,
            autoPumpEnabled = input.autoPumpEnabled,

            fanLedEnabled = input.fanLedEnabled,
            fanLedEffect = input.fanLedEffect,
            fanLedColor = input.fanLedColor,

            logoLedEnabled = input.logoLedEnabled,
            logoLedEffect = input.logoLedEffect,
            logoLedColor = input.logoLedColor,

            shoulderLedEnabled = input.shoulderLedEnabled,
            shoulderLedEffect = input.shoulderLedEffect,
            shoulderLedColor = input.shoulderLedColor,

            triggerEnabled = input.triggerEnabled,
            leftTriggerAction = input.leftTriggerAction,
            rightTriggerAction = input.rightTriggerAction,
            intentUnlockRightTrigger = input.intentUnlockRightTrigger,
            triggersAutoStart = input.triggersAutoStart
        )
    }

    fun applyProfileToUiState(
        profile: HardwareProfile,

        setAutoFanEnabled: (Boolean) -> Unit,
        setFanCurveMode: (String) -> Unit,

        setPumpEnabled: (Boolean) -> Unit,
        setPumpProfile: (String) -> Unit,
        setAutoPumpEnabled: (Boolean) -> Unit,

        setFanLedEnabled: (Boolean) -> Unit,
        setFanLedEffect: (String) -> Unit,
        setFanLedColor: (Int) -> Unit,

        setLogoLedEnabled: (Boolean) -> Unit,
        setLogoLedEffect: (String) -> Unit,
        setLogoLedColor: (Int) -> Unit,

        setShoulderLedEnabled: (Boolean) -> Unit,
        setShoulderLedEffect: (String) -> Unit,
        setShoulderLedColor: (Int) -> Unit,

        setFanLevel: (Int) -> Unit,
        saveTriggerPrefs: (HardwareProfile) -> Unit,
        enableTriggersIfNeeded: (HardwareProfile) -> Unit,
        afterProfileApplied: (HardwareProfile) -> Unit
    ) {
        setAutoFanEnabled(profile.autoFanEnabled)
        setFanCurveMode(profile.fanCurveMode)

        setPumpEnabled(profile.pumpEnabled)
        setPumpProfile(profile.pumpProfile)
        setAutoPumpEnabled(profile.autoPumpEnabled)

        setFanLedEnabled(profile.fanLedEnabled)
        setFanLedEffect(profile.fanLedEffect)
        setFanLedColor(profile.fanLedColor)

        setLogoLedEnabled(profile.logoLedEnabled)
        setLogoLedEffect(profile.logoLedEffect)
        setLogoLedColor(profile.logoLedColor)

        setShoulderLedEnabled(profile.shoulderLedEnabled)
        setShoulderLedEffect(profile.shoulderLedEffect)
        setShoulderLedColor(profile.shoulderLedColor)

        setFanLevel(profile.fanLevel)
        saveTriggerPrefs(profile)
        enableTriggersIfNeeded(profile)
        afterProfileApplied(profile)
    }
}
