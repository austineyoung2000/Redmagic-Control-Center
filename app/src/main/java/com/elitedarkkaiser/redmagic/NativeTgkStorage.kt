package com.elitedarkkaiser.redmagic

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

enum class NativeTgkOrientation {
    PORTRAIT,
    LANDSCAPE
}

data class NativeTgkRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val captureWidth: Int,
    val captureHeight: Int
) {
    fun isValid(): Boolean {
        return captureWidth > 1 &&
            captureHeight > 1 &&
            left >= 0 &&
            top >= 0 &&
            right > left &&
            bottom > top &&
            right <= captureWidth &&
            bottom <= captureHeight
    }

    fun scaledTo(
        displayWidth: Int,
        displayHeight: Int
    ): IntArray {
        require(isValid()) {
            "Invalid stored TGK rectangle"
        }
        require(displayWidth > 1 && displayHeight > 1) {
            "Invalid target display size"
        }

        fun scale(
            value: Int,
            sourceSize: Int,
            targetSize: Int
        ): Int {
            return (
                value.toLong() * targetSize.toLong() /
                    sourceSize.toLong()
                ).toInt()
        }

        val scaledLeft = scale(
            left,
            captureWidth,
            displayWidth
        ).coerceIn(0, displayWidth - 2)

        val scaledTop = scale(
            top,
            captureHeight,
            displayHeight
        ).coerceIn(0, displayHeight - 2)

        val scaledRight = scale(
            right,
            captureWidth,
            displayWidth
        ).coerceIn(scaledLeft + 1, displayWidth - 1)

        val scaledBottom = scale(
            bottom,
            captureHeight,
            displayHeight
        ).coerceIn(scaledTop + 1, displayHeight - 1)

        return intArrayOf(
            scaledLeft,
            scaledTop,
            scaledRight,
            scaledBottom
        )
    }
}

data class NativeTgkOrientationMapping(
    val left: NativeTgkRect?,
    val right: NativeTgkRect?
) {
    fun isComplete(): Boolean {
        return left?.isValid() == true &&
            right?.isValid() == true
    }
}

data class NativeTgkProfile(
    val packageName: String,
    val appLabel: String,
    val enabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val showSavedTargets: Boolean = true,
    val savedTargetOpacityPercent: Int = 12,
    val leftRapidFireCount: Int = 0,
    val rightRapidFireCount: Int = 0,
    val portrait: NativeTgkOrientationMapping? = null,
    val landscape: NativeTgkOrientationMapping? = null
) {
    fun mappingFor(
        orientation: NativeTgkOrientation
    ): NativeTgkOrientationMapping? {
        return when (orientation) {
            NativeTgkOrientation.PORTRAIT -> portrait
            NativeTgkOrientation.LANDSCAPE -> landscape
        }
    }

    fun hasCompleteMapping(
        orientation: NativeTgkOrientation
    ): Boolean {
        return mappingFor(orientation)?.isComplete() == true
    }
}

object NativeTgkStorage {
    private const val TAG = "RedmagicNativeTgk"
    private const val PREFS_NAME = "native_tgk_profiles"
    private const val PROFILES_KEY = "profiles_json"
    private const val VERSION = 2

    val supportedRapidFireCounts = setOf(0, 2, 5, 10)

    private val packagePattern = Regex(
        """[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+"""
    )

    @Synchronized
    fun readProfiles(
        context: Context
    ): List<NativeTgkProfile> {
        val raw = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getString(PROFILES_KEY, null)
            ?: return emptyList()

        return try {
            val root = JSONObject(raw)
            val profiles = root.optJSONArray("profiles")
                ?: JSONArray()

            buildList {
                for (index in 0 until profiles.length()) {
                    val profile = profiles
                        .optJSONObject(index)
                        ?.toProfile()
                        ?: continue

                    add(profile)
                }
            }.distinctBy {
                it.packageName
            }.sortedBy {
                it.appLabel.lowercase()
            }
        } catch (error: Throwable) {
            Log.e(
                TAG,
                "Failed to read native TGK profiles",
                error
            )
            emptyList()
        }
    }

    @Synchronized
    fun getProfile(
        context: Context,
        packageName: String
    ): NativeTgkProfile? {
        return readProfiles(context).firstOrNull {
            it.packageName == packageName
        }
    }

    @Synchronized
    fun saveProfile(
        context: Context,
        profile: NativeTgkProfile
    ): Boolean {
        if (
            !packagePattern.matches(profile.packageName) ||
            profile.appLabel.isBlank() ||
            profile.savedTargetOpacityPercent !in 5..30 ||
            profile.leftRapidFireCount !in
                supportedRapidFireCounts ||
            profile.rightRapidFireCount !in
                supportedRapidFireCounts
        ) {
            return false
        }

        val profiles = readProfiles(context)
            .filterNot {
                it.packageName == profile.packageName
            }
            .plus(profile)
            .sortedBy {
                it.appLabel.lowercase()
            }

        return writeProfiles(context, profiles)
    }

    @Synchronized
    fun removeProfile(
        context: Context,
        packageName: String
    ): Boolean {
        val profiles = readProfiles(context)
        val updated = profiles.filterNot {
            it.packageName == packageName
        }

        if (updated.size == profiles.size) {
            return true
        }

        return writeProfiles(context, updated)
    }

    fun enabledPackages(
        context: Context
    ): Set<String> {
        return readProfiles(context)
            .asSequence()
            .filter { it.enabled }
            .filter {
                it.portrait?.isComplete() == true ||
                    it.landscape?.isComplete() == true
            }
            .map { it.packageName }
            .toSet()
    }

    fun hasEnabledProfile(
        context: Context,
        packageName: String
    ): Boolean {
        return getProfile(context, packageName)
            ?.let { profile ->
                profile.enabled &&
                    (
                        profile.portrait?.isComplete() == true ||
                            profile.landscape?.isComplete() == true
                        )
            } == true
    }

    private fun writeProfiles(
        context: Context,
        profiles: List<NativeTgkProfile>
    ): Boolean {
        return try {
            val root = JSONObject()
                .put("version", VERSION)
                .put(
                    "profiles",
                    JSONArray().apply {
                        profiles.forEach {
                            put(it.toJson())
                        }
                    }
                )

            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            ).edit()
                .putString(PROFILES_KEY, root.toString())
                .commit()
        } catch (error: Throwable) {
            Log.e(
                TAG,
                "Failed to save native TGK profiles",
                error
            )
            false
        }
    }

    private fun NativeTgkProfile.toJson(): JSONObject {
        return JSONObject()
            .put("packageName", packageName)
            .put("appLabel", appLabel)
            .put("enabled", enabled)
            .put("hapticsEnabled", hapticsEnabled)
            .put("showSavedTargets", showSavedTargets)
            .put(
                "savedTargetOpacityPercent",
                savedTargetOpacityPercent
            )
            .put("leftRapidFireCount", leftRapidFireCount)
            .put("rightRapidFireCount", rightRapidFireCount)
            .apply {
                portrait?.let {
                    put("portrait", it.toJson())
                }
                landscape?.let {
                    put("landscape", it.toJson())
                }
            }
    }

    private fun NativeTgkOrientationMapping.toJson(): JSONObject {
        return JSONObject().apply {
            left?.let {
                put("left", it.toJson())
            }
            right?.let {
                put("right", it.toJson())
            }
        }
    }

    private fun NativeTgkRect.toJson(): JSONObject {
        return JSONObject()
            .put("left", left)
            .put("top", top)
            .put("right", right)
            .put("bottom", bottom)
            .put("captureWidth", captureWidth)
            .put("captureHeight", captureHeight)
    }

    private fun JSONObject.toProfile(): NativeTgkProfile? {
        val packageName = optString("packageName")
        val appLabel = optString("appLabel")

        if (
            !packagePattern.matches(packageName) ||
            appLabel.isBlank()
        ) {
            return null
        }

        return NativeTgkProfile(
            packageName = packageName,
            appLabel = appLabel,
            enabled = optBoolean("enabled", true),
            hapticsEnabled = optBoolean(
                "hapticsEnabled",
                true
            ),
            showSavedTargets = optBoolean(
                "showSavedTargets",
                true
            ),
            savedTargetOpacityPercent = optInt(
                "savedTargetOpacityPercent",
                12
            ).coerceIn(5, 30),
            leftRapidFireCount = optInt(
                "leftRapidFireCount",
                0
            ).takeIf {
                it in supportedRapidFireCounts
            } ?: 0,
            rightRapidFireCount = optInt(
                "rightRapidFireCount",
                0
            ).takeIf {
                it in supportedRapidFireCounts
            } ?: 0,
            portrait = optJSONObject("portrait")
                ?.toOrientationMapping(),
            landscape = optJSONObject("landscape")
                ?.toOrientationMapping()
        )
    }

    private fun JSONObject.toOrientationMapping():
        NativeTgkOrientationMapping {
        return NativeTgkOrientationMapping(
            left = optJSONObject("left")?.toRect(),
            right = optJSONObject("right")?.toRect()
        )
    }

    private fun JSONObject.toRect(): NativeTgkRect? {
        val rect = NativeTgkRect(
            left = optInt("left", -1),
            top = optInt("top", -1),
            right = optInt("right", -1),
            bottom = optInt("bottom", -1),
            captureWidth = optInt("captureWidth", -1),
            captureHeight = optInt("captureHeight", -1)
        )

        return rect.takeIf {
            it.isValid()
        }
    }
}
