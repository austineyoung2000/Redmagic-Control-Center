package com.elitedarkkaiser.redmagic

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

internal object ProfileDialogs {
    private fun createNameInput(
        context: Context,
        hint: String,
        textPrimary: Int,
        textSecondary: Int,
        borderColor: Int,
        dp: (Int) -> Int
    ): Pair<TextInputLayout, TextInputEditText> {
        val layout = TextInputLayout(
            context,
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            this.hint = hint
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = 0xFF121A27.toInt()
            boxStrokeColor = borderColor
            boxStrokeWidth = dp(1)
            boxStrokeWidthFocused = dp(2)
            defaultHintTextColor = ColorStateList.valueOf(textSecondary)
            setBoxCornerRadii(
                dp(18).toFloat(), dp(18).toFloat(),
                dp(18).toFloat(), dp(18).toFloat()
            )
        }
        val input = TextInputEditText(layout.context).apply {
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
            textSize = 15f
            isSingleLine = true
            background = null
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        layout.addView(
            input,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        return layout to input
    }

    fun showDeleteProfileDialog(
        context: Context,
        profileName: String,
        onConfirmDelete: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Delete Profile")
            .setMessage("Delete $profileName?")
            .setPositiveButton("Delete") { _, _ -> onConfirmDelete() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showStyledNameOnlyDialog(
        context: Context,
        title: String,
        hint: String,
        textPrimary: Int,
        textSecondary: Int,
        panelColor: Int,
        borderColor: Int,
        dp: (Int) -> Int,
        roundedBg: (Int, Int, Int) -> android.graphics.drawable.Drawable,
        actionButton: (String, Boolean, () -> Unit) -> View,
        space: (Int) -> View,
        onSave: (String) -> Unit
    ) {
        val titleView = TextView(context).apply {
            text = title
            textSize = 19f
            setTextColor(textPrimary)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(14))
        }
        val (inputLayout, input) = createNameInput(
            context, hint, textPrimary, textSecondary, borderColor, dp
        )
        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(18), 0, 0)
        }
        val cancel = actionButton("CANCEL", false) {}
        val save = actionButton("SAVE", false) {}
        buttonRow.addView(cancel)
        buttonRow.addView(space(dp(10)))
        buttonRow.addView(save)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(16))
            background = roundedBg(panelColor, borderColor, 24)
            addView(titleView)
            addView(inputLayout)
            addView(buttonRow)
        }
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(container)
            .setCancelable(true)
            .create()
        cancel.setOnClickListener { dialog.dismiss() }
        save.setOnClickListener {
            val name = input.text?.toString()?.trim().orEmpty()
            if (name.isNotBlank()) {
                onSave(name)
                dialog.dismiss()
            }
        }
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.TRANSPARENT
                )
            )
            setDimAmount(0.65f)
        }
    }
}
