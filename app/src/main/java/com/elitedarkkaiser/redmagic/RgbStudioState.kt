package com.elitedarkkaiser.redmagic

data class RgbStudioState(
    val enabled: Boolean = false,
    val syncZones: Boolean = true,
    val effect: String = "steady",
    val colors: List<Int> = DEFAULT_COLORS,
    val logoSpeedMs: Long = DEFAULT_SPEED_MS,
    val shoulderSpeedMs: Long = DEFAULT_SPEED_MS,
    val fanSpeedMs: Long = DEFAULT_SPEED_MS,
    val screenOffTimeoutMinutes: Int = 0
) {
    companion object {
        val DEFAULT_COLORS = listOf(1, 3, 4, 5, 6, 7, 8, 9)
        const val DEFAULT_SPEED_MS = 1_500L
    }
}
