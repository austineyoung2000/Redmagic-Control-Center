package com.elitedarkkaiser.redmagic

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ProfileManager {
    private const val PREFS = "hardware_profiles"
    private const val KEY = "profiles"

    fun loadProfiles(context: Context): MutableList<HardwareProfile> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<HardwareProfile>()

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                HardwareProfile(
                    name = o.getString("name"),

                    fanEnabled = o.getBoolean("fanEnabled"),
                    fanLevel = o.getInt("fanLevel"),
                    autoFanEnabled = o.getBoolean("autoFanEnabled"),
                    fanCurveMode = o.getString("fanCurveMode"),

                    pumpEnabled = o.getBoolean("pumpEnabled"),
                    pumpProfile = o.getString("pumpProfile"),
                    autoPumpEnabled = o.getBoolean("autoPumpEnabled"),

                    fanLedEnabled = o.getBoolean("fanLedEnabled"),
                    fanLedEffect = o.getString("fanLedEffect"),
                    fanLedColor = o.getInt("fanLedColor"),

                    logoLedEnabled = o.getBoolean("logoLedEnabled"),
                    logoLedEffect = o.getString("logoLedEffect"),
                    logoLedColor = o.getInt("logoLedColor"),

                    shoulderLedEnabled = o.getBoolean("shoulderLedEnabled"),
                    shoulderLedEffect = o.getString("shoulderLedEffect"),
                    shoulderLedColor = o.getInt("shoulderLedColor"),

                    triggerEnabled = o.optBoolean("triggerEnabled", false),
                    leftTriggerAction = o.optString("leftTriggerAction", "NONE"),
                    rightTriggerAction = o.optString("rightTriggerAction", "NONE"),
                    intentUnlockRightTrigger = o.optBoolean("intentUnlockRightTrigger", true),
                    triggersAutoStart = o.optBoolean("triggersAutoStart", false)
                )
            )
        }

        return out
    }

    fun saveProfiles(context: Context, profiles: List<HardwareProfile>) {
        val arr = JSONArray()

        profiles.forEach { p ->
            val o = JSONObject()
            o.put("name", p.name)

            o.put("fanEnabled", p.fanEnabled)
            o.put("fanLevel", p.fanLevel)
            o.put("autoFanEnabled", p.autoFanEnabled)
            o.put("fanCurveMode", p.fanCurveMode)

            o.put("pumpEnabled", p.pumpEnabled)
            o.put("pumpProfile", p.pumpProfile)
            o.put("autoPumpEnabled", p.autoPumpEnabled)

            o.put("fanLedEnabled", p.fanLedEnabled)
            o.put("fanLedEffect", p.fanLedEffect)
            o.put("fanLedColor", p.fanLedColor)

            o.put("logoLedEnabled", p.logoLedEnabled)
            o.put("logoLedEffect", p.logoLedEffect)
            o.put("logoLedColor", p.logoLedColor)

            o.put("shoulderLedEnabled", p.shoulderLedEnabled)
            o.put("shoulderLedEffect", p.shoulderLedEffect)
            o.put("shoulderLedColor", p.shoulderLedColor)

            o.put("triggerEnabled", p.triggerEnabled)
            o.put("leftTriggerAction", p.leftTriggerAction)
            o.put("rightTriggerAction", p.rightTriggerAction)
            o.put("intentUnlockRightTrigger", p.intentUnlockRightTrigger)
            o.put("triggersAutoStart", p.triggersAutoStart)

            arr.put(o)
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, arr.toString())
            .commit()
    }

    fun upsertProfile(context: Context, profile: HardwareProfile) {
        val profiles = loadProfiles(context)
        val idx = profiles.indexOfFirst { it.name == profile.name }
        if (idx >= 0) {
            profiles[idx] = profile
        } else {
            profiles.add(profile)
        }
        saveProfiles(context, profiles)
    }

    fun deleteProfile(context: Context, name: String) {
        val profiles = loadProfiles(context).filterNot { it.name == name }
        saveProfiles(context, profiles)
    }
}
