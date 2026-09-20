package com.elitedarkkaiser.redmagic

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.elitedarkkaiser.redmagic.ui.AppTheme
import com.google.android.material.button.MaterialButton

internal object TriggerDialogUi {
    data class Style(
        val textPrimary: Int,
        val textSecondary: Int,
        val panelColor: Int,
        val borderColor: Int,
        val accent: Int,
        val typeface: Typeface?,
        val dp: (Int) -> Int,
        val roundedBg: (Int, Int, Int) -> Drawable
    )

    fun title(
        activity: MainActivity,
        style: Style,
        textValue: String
    ) = TextView(activity).apply {
        text = textValue
        textSize = 20f
        setTextColor(style.textPrimary)
        setTypeface(style.typeface, Typeface.BOLD)
    }

    fun subtitle(
        activity: MainActivity,
        style: Style,
        textValue: String
    ) = TextView(activity).apply {
        text = textValue
        textSize = 13f
        setTextColor(style.textSecondary)
        setPadding(0, style.dp(6), 0, style.dp(14))
    }

    fun section(
        activity: MainActivity,
        style: Style,
        titleText: String,
        description: String? = null
    ) = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(
            style.dp(12),
            style.dp(12),
            style.dp(12),
            style.dp(12)
        )
        background = style.roundedBg(
            AppTheme.chipOnColor,
            style.borderColor,
            18
        )

        addView(TextView(activity).apply {
            text = titleText
            textSize = 14f
            setTextColor(style.textPrimary)
            setTypeface(style.typeface, Typeface.BOLD)
        })

        if (!description.isNullOrBlank()) {
            addView(TextView(activity).apply {
                text = description
                textSize = 12f
                setTextColor(style.textSecondary)
                setPadding(0, style.dp(3), 0, style.dp(8))
            })
        } else {
            addView(space(activity, style.dp(8)))
        }
    }

    fun optionButton(
        activity: MainActivity,
        style: Style,
        label: String,
        selected: Boolean,
        onClick: () -> Unit
    ) = MaterialButton(
        activity,
        null,
        com.google.android.material.R.attr.materialButtonOutlinedStyle
    ).apply {
        text = if (selected) "✓  $label" else label
        textSize = 12f
        isAllCaps = false
        maxLines = 1
        minWidth = 0
        minimumWidth = 0
        setTextColor(style.textPrimary)
        backgroundTintList = ColorStateList.valueOf(
            if (selected) {
                AppTheme.chipActiveColor
            } else {
                Color.TRANSPARENT
            }
        )
        strokeWidth = style.dp(1)
        strokeColor = ColorStateList.valueOf(
            if (selected) {
                AppTheme.highlightBorder
            } else {
                style.borderColor
            }
        )
        rippleColor = ColorStateList.valueOf(AppTheme.rippleColor)
        cornerRadius = style.dp(14)
        insetTop = 0
        insetBottom = 0
        minHeight = style.dp(46)
        setPadding(
            style.dp(8),
            style.dp(6),
            style.dp(8),
            style.dp(6)
        )
        setOnClickListener { onClick() }
    }

    fun optionRow(
        activity: MainActivity,
        style: Style
    ) = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun addWeightedOption(
        row: LinearLayout,
        button: MaterialButton,
        style: Style,
        addGap: Boolean
    ) {
        if (addGap) {
            row.addView(space(row.context, style.dp(8)))
        }
        row.addView(
            button,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )
    }

    fun actionButton(
        activity: MainActivity,
        style: Style,
        label: String,
        primary: Boolean,
        onClick: () -> Unit
    ) = MaterialButton(
        activity,
        null,
        if (primary) {
            com.google.android.material.R.attr.materialButtonStyle
        } else {
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        }
    ).apply {
        text = label
        textSize = 13f
        isAllCaps = false
        setTextColor(
            if (primary) Color.WHITE else style.textPrimary
        )
        backgroundTintList = ColorStateList.valueOf(
            if (primary) style.accent else Color.TRANSPARENT
        )
        strokeWidth = if (primary) 0 else style.dp(1)
        strokeColor = ColorStateList.valueOf(style.borderColor)
        rippleColor = ColorStateList.valueOf(AppTheme.rippleColor)
        cornerRadius = style.dp(14)
        insetTop = 0
        insetBottom = 0
        minHeight = style.dp(48)
        setOnClickListener { onClick() }
    }

    fun dialogRoot(
        activity: MainActivity,
        style: Style,
        content: View
    ) = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(
            style.dp(10),
            style.dp(10),
            style.dp(10),
            style.dp(10)
        )
        setBackgroundColor(AppTheme.bgColor)
        addView(
            content,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    fun space(context: android.content.Context, size: Int) =
        View(context).apply {
            layoutParams = LinearLayout.LayoutParams(size, size)
        }
}
