package com.elitedarkkaiser.redmagic

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

internal object ProfileDialogs {

    private data class NameInput(
        val layout: TextInputLayout,
        val editText: TextInputEditText
    )

    private fun createNameInput(
        context: Context,
        hint: String,
        textPrimary: Int,
        textSecondary: Int,
        borderColor: Int,
        dp: (Int) -> Int
    ): NameInput {
        val layout = TextInputLayout(
            context,
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            this.hint = hint
            boxBackgroundMode =
                TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = 0xFF121A27.toInt()
            boxStrokeColor = borderColor
            boxStrokeWidth = dp(1)
            boxStrokeWidthFocused = dp(2)
            defaultHintTextColor =
                ColorStateList.valueOf(textSecondary)
            setBoxCornerRadii(
                dp(18).toFloat(),
                dp(18).toFloat(),
                dp(18).toFloat(),
                dp(18).toFloat()
            )
        }

        val editText = TextInputEditText(layout.context).apply {
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
            textSize = 15f
            isSingleLine = true
            background = null
            setPadding(
                dp(16),
                dp(14),
                dp(16),
                dp(14)
            )
        }

        layout.addView(
            editText,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        return NameInput(layout, editText)
    }

    fun showDeleteProfileDialog(
        context: Context,
        profileName: String,
        onConfirmDelete: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Delete Profile")
            .setMessage("Delete $profileName?")
            .setPositiveButton("Delete") { _, _ ->
                onConfirmDelete()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun renderProfiles(
        context: Context,
        profileList: LinearLayout,
        profiles: List<HardwareProfile>,
        subtleLabel: (String) -> View,
        actionButton: (String, Boolean, () -> Unit) -> View,
        space: (Int) -> View,
        dp: (Int) -> Int,
        onApplyProfile: (HardwareProfile) -> Unit,
        onDeleteProfile: (HardwareProfile) -> Unit
    ) {
        profileList.removeAllViews()

        if (profiles.isEmpty()) {
            profileList.addView(subtleLabel("No saved profiles yet"))
            return
        }

        profiles.forEach { profile ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val applyBtn = actionButton(profile.name, false) {
                onApplyProfile(profile)
            }.apply {
                setPadding(dp(16), dp(10), dp(16), dp(10))
            }

            val deleteBtn = actionButton("DEL", true) {
                onDeleteProfile(profile)
            }.apply {
                setPadding(dp(14), dp(10), dp(14), dp(10))
            }

            row.addView(
                applyBtn,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            row.addView(space(dp(8)))
            row.addView(deleteBtn)

            profileList.addView(row)
            profileList.addView(space(dp(10)))
        }
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

        val nameInput = createNameInput(
            context = context,
            hint = hint,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            borderColor = borderColor,
            dp = dp
        )
        val input = nameInput.editText

        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(18), 0, 0)
        }

        val cancelBtn = actionButton("CANCEL", false) {}
        val saveBtn = actionButton("SAVE", false) {}

        buttonRow.addView(cancelBtn)
        buttonRow.addView(space(dp(10)))
        buttonRow.addView(saveBtn)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(16))
            background = roundedBg(panelColor, borderColor, 24)
            addView(titleView)
            addView(nameInput.layout)
            addView(buttonRow)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(container)
            .setCancelable(true)
            .create()

        cancelBtn.setOnClickListener { dialog.dismiss() }

        saveBtn.setOnClickListener {
            val name = input.text?.toString()?.trim().orEmpty()
            if (name.isBlank()) return@setOnClickListener
            onSave(name)
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setDimAmount(0.65f)
        }
    }

    fun showStyledSaveProfileDialog(
        context: Context,
        textPrimary: Int,
        textSecondary: Int,
        panelColor: Int,
        borderColor: Int,
        typeface: Typeface?,
        dp: (Int) -> Int,
        roundedBg: (Int, Int, Int) -> android.graphics.drawable.Drawable,
        actionButton: (String, Boolean, () -> Unit) -> View,
        space: (Int) -> View,
        buildProfile: (String) -> HardwareProfile,
        onSaved: () -> Unit
    ) {
        val titleView = TextView(context).apply {
            text = "Save Profile"
            textSize = 19f
            setTextColor(textPrimary)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(14))
        }

        val nameInput = createNameInput(
            context = context,
            hint = "Profile name",
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            borderColor = borderColor,
            dp = dp
        )
        val input = nameInput.editText

        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(18), 0, 0)
        }

        val cancelBtn = actionButton("CANCEL", false) {}.apply {
            alpha = 0.88f
        }

        val saveBtn = actionButton("SAVE", false) {}.apply {
            setPadding(dp(18), dp(10), dp(18), dp(10))
        }

        buttonRow.addView(cancelBtn)
        buttonRow.addView(space(dp(10)))
        buttonRow.addView(saveBtn)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(16))
            background = roundedBg(panelColor, borderColor, 24)
            addView(titleView)
            addView(nameInput.layout)
            addView(buttonRow)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(container)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        saveBtn.setOnClickListener {
            val name = input.text?.toString()?.trim().orEmpty()
            if (name.isBlank()) return@setOnClickListener

            val profile = buildProfile(name)
            ProfileActions.saveProfile(context, name, profile) {
                dialog.dismiss()
                onSaved()
            }
        }

        dialog.show()

        dialog.window?.apply {
            setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            )
            setDimAmount(0.65f)
        }
    }
}
