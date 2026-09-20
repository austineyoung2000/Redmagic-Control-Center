package com.elitedarkkaiser.redmagic

import android.content.res.Configuration
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.elitedarkkaiser.redmagic.ui.AppTheme
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppTheme.configure(this)

        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        fun text(
            value: String,
            size: Float,
            secondary: Boolean = false,
            bold: Boolean = false
        ) = TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(
                if (secondary) AppTheme.textSecondary
                else AppTheme.textPrimary
            )
            if (bold) {
                setTypeface(AppTheme.appTypeface, Typeface.BOLD)
            }
        }

        fun panel() = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = AppTheme.roundedBg(
                AppTheme.panelColor,
                AppTheme.borderColor,
                dp(22).toFloat()
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(18))
        }

        val backButton = MaterialButton(this).apply {
            text = "‹"
            textSize = 28f
            contentDescription = "Back"
            minWidth = dp(48)
            minHeight = dp(48)
            insetTop = 0
            insetBottom = 0
            backgroundTintList = ColorStateList.valueOf(
                AppTheme.chipOnColor
            )
            setTextColor(AppTheme.textPrimary)
            cornerRadius = dp(16)
            setOnClickListener { finish() }
        }

        header.addView(backButton)
        header.addView(text("Settings", 23f, bold = true).apply {
            setPadding(dp(14), 0, 0, 0)
        })

        val temperatureSummary = text("", 12f, secondary = true)
        val temperatureSwitch = MaterialSwitch(this).apply {
            text = "Use Fahrenheit"
            textSize = 14f
            setTextColor(AppTheme.textPrimary)
            isChecked = isUseFahrenheitStorage(this@SettingsActivity)
        }

        fun updateTemperatureSummary(useFahrenheit: Boolean) {
            temperatureSummary.text = if (useFahrenheit) {
                "Temperatures are displayed in Fahrenheit (°F)."
            } else {
                "Temperatures are displayed in Celsius (°C)."
            }
        }

        updateTemperatureSummary(temperatureSwitch.isChecked)
        temperatureSwitch.setOnCheckedChangeListener { _, checked ->
            saveUseFahrenheitStorage(this, checked)
            updateTemperatureSummary(checked)
        }

        val temperaturePanel = panel().apply {
            addView(text("TEMPERATURE", 12f, secondary = true, bold = true))
            addView(temperatureSwitch)
            addView(temperatureSummary.apply {
                setPadding(0, dp(4), 0, 0)
            })
        }

        val hapticSummary = text("", 12f, secondary = true)
        var selectedHapticStrength = HapticFeedback.Strength.fromKey(
            HapticFeedback.read(this).strength
        )

        val hapticStrengthButtons = LinkedHashMap<
            HapticFeedback.Strength,
            MaterialButton
        >()

        fun refreshHapticStrengthButtons() {
            hapticStrengthButtons.forEach { (strength, button) ->
                val selected = strength == selectedHapticStrength
                button.backgroundTintList = ColorStateList.valueOf(
                    if (selected) {
                        AppTheme.chipActiveColor
                    } else {
                        AppTheme.chipOnColor
                    }
                )
                button.setTextColor(AppTheme.textPrimary)
            }

            hapticSummary.text =
                "Uses a short ${selectedHapticStrength.label.lowercase()} " +
                    "pulse for trigger actions, dual-app slider launches, " +
                    "and Master Profile application. No polling is used."
        }

        val hapticStrengthRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        HapticFeedback.Strength.entries.forEach { strength ->
            val button = MaterialButton(this).apply {
                text = strength.label
                textSize = 12f
                isAllCaps = false
                cornerRadius = dp(14)
                insetTop = 0
                insetBottom = 0
                minHeight = dp(44)
                setOnClickListener {
                    selectedHapticStrength = strength
                    HapticFeedback.setStrength(
                        this@SettingsActivity,
                        strength
                    )
                    refreshHapticStrengthButtons()

                    Thread(
                        {
                            HapticFeedback.testPulse(strength)
                        },
                        "RedMagicHapticTest"
                    ).start()
                }
            }

            hapticStrengthButtons[strength] = button
            hapticStrengthRow.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginEnd = if (
                        strength != HapticFeedback.Strength.HIGH
                    ) dp(6) else 0
                }
            )
        }

        val hapticSwitch = MaterialSwitch(this).apply {
            text = "Hardware action feedback"
            textSize = 14f
            setTextColor(AppTheme.textPrimary)
            isChecked = HapticFeedback.read(
                this@SettingsActivity
            ).enabled
            setOnCheckedChangeListener { _, checked ->
                HapticFeedback.setEnabled(
                    this@SettingsActivity,
                    checked
                )
            }
        }

        refreshHapticStrengthButtons()

        val hapticPanel = panel().apply {
            addView(text(
                "HAPTIC FEEDBACK",
                12f,
                secondary = true,
                bold = true
            ))
            addView(hapticSwitch)
            addView(text(
                "Strength",
                13f,
                secondary = true
            ).apply {
                setPadding(0, dp(8), 0, dp(6))
            })
            addView(hapticStrengthRow)
            addView(hapticSummary.apply {
                setPadding(0, dp(8), 0, 0)
            })
        }

        val nightMode = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK
        val appearanceName = if (
            nightMode == Configuration.UI_MODE_NIGHT_YES
        ) "Dark" else "Light"

        val appearancePanel = panel().apply {
            addView(text("APPEARANCE", 12f, secondary = true, bold = true))
            addView(text("Follow system theme", 14f, bold = true).apply {
                setPadding(0, dp(10), 0, 0)
            })
            addView(text(
                "Currently using $appearanceName mode. The app changes " +
                    "automatically with Android's light/dark setting.",
                12f,
                secondary = true
            ).apply { setPadding(0, dp(4), 0, 0) })
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(28))
            addView(header)
            addView(temperaturePanel)
            addView(hapticPanel)
            addView(appearancePanel)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(AppTheme.bgColor)
            addView(content)
        }

        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        setContentView(scroll)
    }
}
