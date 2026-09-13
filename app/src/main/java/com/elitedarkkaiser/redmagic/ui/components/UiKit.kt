package com.elitedarkkaiser.redmagic.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.elitedarkkaiser.redmagic.ui.AppTheme
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

object UiKit {
    fun titleText(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(AppTheme.textPrimary)
            textSize = 22f
            typeface = Typeface.create(AppTheme.appTypeface, Typeface.BOLD)
        }
    }

    fun subtitleText(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(AppTheme.textSecondary)
            textSize = 14f
            typeface = AppTheme.appTypeface
        }
    }

    fun bodyText(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(AppTheme.textSecondary)
            textSize = 13f
            typeface = AppTheme.appTypeface
            setLineSpacing(0f, 1.08f)
        }
    }

    fun sectionPanel(context: Context): LinearLayout {
        val shapeModel = ShapeAppearanceModel.builder()
            .setAllCornerSizes(
                AppTheme.dp(context, 18).toFloat()
            )
            .build()

        val panelBackground = MaterialShapeDrawable(shapeModel).apply {
            initializeElevationOverlay(context)
            fillColor =
                ColorStateList.valueOf(AppTheme.panelColor)
            strokeWidth =
                AppTheme.dp(context, 1).toFloat()
            strokeColor =
                ColorStateList.valueOf(AppTheme.borderColor)
            elevation =
                AppTheme.dp(context, 2).toFloat()
        }

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                AppTheme.dp(context, 16),
                AppTheme.dp(context, 14),
                AppTheme.dp(context, 16),
                AppTheme.dp(context, 14)
            )
            background = panelBackground
            elevation = AppTheme.dp(context, 2).toFloat()
            clipToOutline = true
        }
    }

    fun actionButton(
        context: Context,
        text: String,
        isDanger: Boolean = false
    ): Button {
        return MaterialButton(context).apply {
            this.text = text
            isAllCaps = false
            gravity = Gravity.CENTER
            setTextColor(AppTheme.textPrimary)
            textSize = 12f
            typeface =
                Typeface.create(
                    AppTheme.appTypeface,
                    Typeface.BOLD
                )

            backgroundTintList = ColorStateList.valueOf(
                if (isDanger) {
                    AppTheme.dangerColor
                } else {
                    AppTheme.chipOnColor
                }
            )
            strokeWidth = AppTheme.dp(context, 1)
            strokeColor = ColorStateList.valueOf(
                if (isDanger) {
                    AppTheme.dangerColor
                } else {
                    AppTheme.borderColor
                }
            )
            rippleColor =
                ColorStateList.valueOf(
                    Color.parseColor("#33445A")
                )
            cornerRadius = AppTheme.dp(context, 14)

            insetTop = 0
            insetBottom = 0
            minHeight = AppTheme.dp(context, 48)
            setPadding(
                AppTheme.dp(context, 14),
                AppTheme.dp(context, 10),
                AppTheme.dp(context, 14),
                AppTheme.dp(context, 10)
            )
        }
    }
}
