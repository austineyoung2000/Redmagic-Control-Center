package com.elitedarkkaiser.redmagic.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import kotlin.math.roundToInt

object AppTheme {
    var bgColor: Int = 0
        private set
    var panelColor: Int = 0
        private set
    var panelPressed: Int = 0
        private set
    var borderColor: Int = 0
        private set
    var accentColor: Int = 0
        private set
    var chipOnColor: Int = 0
        private set
    var chipActiveColor: Int = 0
        private set
    var dangerColor: Int = 0
        private set
    var textPrimary: Int = 0
        private set
    var textSecondary: Int = 0
        private set
    var highlightBorder: Int = 0
        private set
    var rippleColor: Int = 0
        private set
    val appTypeface: Typeface? = Typeface.SANS_SERIF

    fun configure(context: Context) {
        bgColor = context.getColor(
            com.elitedarkkaiser.redmagic.R.color.redmagic_background
        )
        panelColor = context.getColor(
            com.elitedarkkaiser.redmagic.R.color.redmagic_panel
        )
        panelPressed = context.getColor(
            com.elitedarkkaiser.redmagic.R.color.redmagic_panel_pressed
        )
        borderColor = context.getColor(
            com.elitedarkkaiser.redmagic.R.color.redmagic_border
        )
        accentColor = context.getColor(
            com.elitedarkkaiser.redmagic.R.color.redmagic_accent
        )
        chipOnColor = context.getColor(
            com.elitedarkkaiser.redmagic.R.color.redmagic_chip_on
        )
        chipActiveColor = context.getColor(
            com.elitedarkkaiser.redmagic.R.color.redmagic_chip_active
        )
        dangerColor = context.getColor(
            com.elitedarkkaiser.redmagic.R.color.redmagic_danger
        )
        textPrimary = context.getColor(
            com.elitedarkkaiser.redmagic.R.color.redmagic_text_primary
        )
        textSecondary = context.getColor(
            com.elitedarkkaiser.redmagic.R.color.redmagic_text_secondary
        )
        highlightBorder = context.getColor(
            com.elitedarkkaiser.redmagic.R.color.redmagic_highlight_border
        )
        rippleColor = context.getColor(
            com.elitedarkkaiser.redmagic.R.color.redmagic_ripple
        )
    }

    fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).roundToInt()
    }

    fun roundedBg(fill: Int, stroke: Int, radiusPx: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radiusPx
            setStroke(1, stroke)
        }
    }
}
