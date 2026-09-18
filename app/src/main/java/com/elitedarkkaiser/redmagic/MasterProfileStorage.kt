package com.elitedarkkaiser.redmagic

import android.content.Context
import com.elitedarkkaiser.redmagic.state.LedState
import com.elitedarkkaiser.redmagic.state.PumpState
import org.json.JSONArray
import org.json.JSONObject

object MasterProfileStorage {
    private const val PREFS = "master_profiles"
    private const val KEY = "profiles"

    fun loadProfiles(context: Context): MutableList<MasterProfile> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<MasterProfile>()

        for (i in 0 until arr.length()) {
            out.add(arr.getJSONObject(i).toMasterProfile())
        }

        return out
    }

    fun saveProfiles(context: Context, profiles: List<MasterProfile>) {
        val arr = JSONArray()
        profiles.forEach { arr.put(it.toJson()) }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, arr.toString())
            .commit()
    }

    fun upsertProfile(context: Context, profile: MasterProfile) {
        val profiles = loadProfiles(context)
        val index = profiles.indexOfFirst { it.name == profile.name }

        if (index >= 0) {
            profiles[index] = profile
        } else {
            profiles.add(profile)
        }

        saveProfiles(context, profiles)
    }

    fun deleteProfile(context: Context, name: String) {
        saveProfiles(context, loadProfiles(context).filterNot { it.name == name })
    }

    private fun MasterProfile.toJson(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("hardware", hardware.toJson())
            put("gameMode", gameMode.toJson())
            put("gamePackages", JSONArray(gamePackages.toList()))
            put("chargingEnabled", chargingEnabled)
            put("chargingFanLed", chargingFanLed.toJson())
            put("chargingLogoLed", chargingLogoLed.toJson())
            put("chargingShoulderLed", chargingShoulderLed.toJson())

            put("callLightingEnabled", callLightingEnabled)
            put("pauseFanDuringCalls", pauseFanDuringCalls)
            put("incomingCallFanLed", incomingCallFanLed.toJson())
            put("incomingCallLogoLed", incomingCallLogoLed.toJson())
            put("incomingCallShoulderLed", incomingCallShoulderLed.toJson())
            put("connectedCallFanLed", connectedCallFanLed.toJson())
            put("connectedCallLogoLed", connectedCallLogoLed.toJson())
            put("connectedCallShoulderLed", connectedCallShoulderLed.toJson())

            put("pump", pump.toJson())
            put("selectedFanCurve", selectedFanCurve)
            put("autoFanEnabled", autoFanEnabled)
            put("realtimePreviewEnabled", realtimePreviewEnabled)
            put("triggers", triggers.toJson())
        }
    }

    private fun JSONObject.toMasterProfile(): MasterProfile {
        return MasterProfile(
            name = getString("name"),
            hardware = getJSONObject("hardware").toHardwareProfile(),
            gameMode = getJSONObject("gameMode").toGameModeProfile(),
            gamePackages = getJSONArray("gamePackages").toStringSet(),
            chargingEnabled = optBoolean("chargingEnabled", false),
            chargingFanLed = getJSONObject("chargingFanLed").toLedState(),
            chargingLogoLed = getJSONObject("chargingLogoLed").toLedState(),
            chargingShoulderLed = getJSONObject("chargingShoulderLed").toLedState(),

            callLightingEnabled = optBoolean("callLightingEnabled", false),
            pauseFanDuringCalls = optBoolean("pauseFanDuringCalls", false),
            incomingCallFanLed = optJSONObject("incomingCallFanLed")?.toLedState() ?: LedState(true, "flashing", 5),
            incomingCallLogoLed = optJSONObject("incomingCallLogoLed")?.toLedState() ?: LedState(true, "flashing", 1),
            incomingCallShoulderLed = optJSONObject("incomingCallShoulderLed")?.toLedState() ?: LedState(true, "flashing", 8),
            connectedCallFanLed = optJSONObject("connectedCallFanLed")?.toLedState() ?: LedState(true, "steady", 5),
            connectedCallLogoLed = optJSONObject("connectedCallLogoLed")?.toLedState() ?: LedState(true, "steady", 1),
            connectedCallShoulderLed = optJSONObject("connectedCallShoulderLed")?.toLedState() ?: LedState(true, "steady", 8),

            pump = getJSONObject("pump").toPumpState(),
            selectedFanCurve = optString("selectedFanCurve", "balanced"),
            autoFanEnabled = optBoolean("autoFanEnabled", false),
            realtimePreviewEnabled = optBoolean("realtimePreviewEnabled", true),
            triggers = getJSONObject("triggers").toTriggerPrefsSnapshot()
        )
    }

    private fun HardwareProfile.toJson(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("fanEnabled", fanEnabled)
            put("fanLevel", fanLevel)
            put("autoFanEnabled", autoFanEnabled)
            put("fanCurveMode", fanCurveMode)
            put("pumpEnabled", pumpEnabled)
            put("pumpProfile", pumpProfile)
            put("autoPumpEnabled", autoPumpEnabled)
            put("fanLedEnabled", fanLedEnabled)
            put("fanLedEffect", fanLedEffect)
            put("fanLedColor", fanLedColor)
            put("logoLedEnabled", logoLedEnabled)
            put("logoLedEffect", logoLedEffect)
            put("logoLedColor", logoLedColor)
            put("shoulderLedEnabled", shoulderLedEnabled)
            put("shoulderLedEffect", shoulderLedEffect)
            put("shoulderLedColor", shoulderLedColor)
            put("triggerEnabled", triggerEnabled)
            put("leftTriggerAction", leftTriggerAction)
            put("rightTriggerAction", rightTriggerAction)
            put("intentUnlockRightTrigger", intentUnlockRightTrigger)
            put("triggersAutoStart", triggersAutoStart)
        }
    }

    private fun JSONObject.toHardwareProfile(): HardwareProfile {
        return HardwareProfile(
            name = getString("name"),
            fanEnabled = getBoolean("fanEnabled"),
            fanLevel = getInt("fanLevel"),
            autoFanEnabled = getBoolean("autoFanEnabled"),
            fanCurveMode = getString("fanCurveMode"),
            pumpEnabled = getBoolean("pumpEnabled"),
            pumpProfile = getString("pumpProfile"),
            autoPumpEnabled = getBoolean("autoPumpEnabled"),
            fanLedEnabled = getBoolean("fanLedEnabled"),
            fanLedEffect = getString("fanLedEffect"),
            fanLedColor = getInt("fanLedColor"),
            logoLedEnabled = getBoolean("logoLedEnabled"),
            logoLedEffect = getString("logoLedEffect"),
            logoLedColor = getInt("logoLedColor"),
            shoulderLedEnabled = getBoolean("shoulderLedEnabled"),
            shoulderLedEffect = getString("shoulderLedEffect"),
            shoulderLedColor = getInt("shoulderLedColor"),
            triggerEnabled = optBoolean("triggerEnabled", false),
            leftTriggerAction = optString("leftTriggerAction", "NONE"),
            rightTriggerAction = optString("rightTriggerAction", "NONE"),
            intentUnlockRightTrigger = optBoolean("intentUnlockRightTrigger", true),
            triggersAutoStart = optBoolean("triggersAutoStart", false)
        )
    }

    private fun GameModeProfile.toJson(): JSONObject {
        return JSONObject().apply {
            put("fanEnabled", fanEnabled)
            put("fanLevel", fanLevel)
            put("pumpEnabled", pumpEnabled)
            put("pumpProfile", pumpProfile)
            put("fanLedEnabled", fanLedEnabled)
            put("fanLedEffect", fanLedEffect)
            put("fanLedColor", fanLedColor)
            put("logoLedEnabled", logoLedEnabled)
            put("logoLedEffect", logoLedEffect)
            put("logoLedColor", logoLedColor)
            put("shoulderLedEnabled", shoulderLedEnabled)
            put("shoulderLedEffect", shoulderLedEffect)
            put("shoulderLedColor", shoulderLedColor)
        }
    }

    private fun JSONObject.toGameModeProfile(): GameModeProfile {
        return GameModeProfile(
            fanEnabled = getBoolean("fanEnabled"),
            fanLevel = getInt("fanLevel"),
            pumpEnabled = getBoolean("pumpEnabled"),
            pumpProfile = getString("pumpProfile"),
            fanLedEnabled = getBoolean("fanLedEnabled"),
            fanLedEffect = getString("fanLedEffect"),
            fanLedColor = getInt("fanLedColor"),
            logoLedEnabled = getBoolean("logoLedEnabled"),
            logoLedEffect = getString("logoLedEffect"),
            logoLedColor = getInt("logoLedColor"),
            shoulderLedEnabled = getBoolean("shoulderLedEnabled"),
            shoulderLedEffect = getString("shoulderLedEffect"),
            shoulderLedColor = getInt("shoulderLedColor")
        )
    }

    private fun LedState.toJson(): JSONObject {
        return JSONObject().apply {
            put("enabled", enabled)
            put("effect", effect)
            put("color", color)
        }
    }

    private fun JSONObject.toLedState(): LedState {
        return LedState(
            enabled = optBoolean("enabled", false),
            effect = optString("effect", "steady"),
            color = optInt("color", 1)
        )
    }

    private fun PumpState.toJson(): JSONObject {
        return JSONObject().apply {
            put("enabled", enabled)
            put("profile", profile)
            put("autoEnabled", autoEnabled)
            put("experimentalAccepted", experimentalAccepted)
        }
    }

    private fun JSONObject.toPumpState(): PumpState {
        return PumpState(
            enabled = optBoolean("enabled", false),
            profile = optString("profile", "quick"),
            autoEnabled = optBoolean("autoEnabled", false),
            experimentalAccepted = optBoolean("experimentalAccepted", false)
        )
    }

    private fun TriggerPrefsSnapshot.toJson(): JSONObject {
        return JSONObject().apply {
            put("triggerEnabled", triggerEnabled)
            put("leftTriggerAction", leftTriggerAction)
            put("rightTriggerAction", rightTriggerAction)
            put("intentUnlockRightTrigger", intentUnlockRightTrigger)
            put("triggersAutoStart", triggersAutoStart)
        }
    }

    private fun JSONObject.toTriggerPrefsSnapshot(): TriggerPrefsSnapshot {
        return TriggerPrefsSnapshot(
            triggerEnabled = optBoolean("triggerEnabled", false),
            leftTriggerAction = optString("leftTriggerAction", "NONE"),
            rightTriggerAction = optString("rightTriggerAction", "NONE"),
            intentUnlockRightTrigger = optBoolean("intentUnlockRightTrigger", true),
            triggersAutoStart = optBoolean("triggersAutoStart", false)
        )
    }

    private fun JSONArray.toStringSet(): Set<String> {
        val out = mutableSetOf<String>()
        for (i in 0 until length()) {
            out.add(getString(i))
        }
        return out
    }
}
