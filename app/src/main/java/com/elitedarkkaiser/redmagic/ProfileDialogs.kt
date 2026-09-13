package com.elitedarkkaiser.redmagic

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

internal object ProfileDialogs {

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

        val input = EditText(context).apply {
            this.hint = hint
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
            textSize = 15f
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBg(0xFF121A27.toInt(), 0xFF263246.toInt(), 18)
        }

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
            addView(input)
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

        val input = EditText(context).apply {
            hint = "Profile name"
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
            textSize = 15f
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBg(0xFF121A27.toInt(), 0xFF263246.toInt(), 18)
        }

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
            addView(input)
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
