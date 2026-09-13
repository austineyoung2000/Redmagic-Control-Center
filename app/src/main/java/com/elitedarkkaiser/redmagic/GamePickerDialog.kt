package com.elitedarkkaiser.redmagic

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

private fun gpDp(context: Context, value: Int): Int {
    return (value * context.resources.displayMetrics.density).toInt()
}

private fun gpRoundedStroke(fill: Int, stroke: Int, radiusDp: Int, context: Context): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = gpDp(context, radiusDp).toFloat()
        setColor(fill)
        setStroke(gpDp(context, 1), stroke)
    }
}

fun showGamePickerDialogUI(
    context: Context,
    onSave: (Set<String>) -> Unit
) {
    val pm = context.packageManager

    val bgColor = Color.parseColor("#070B12")
    val panelColor = Color.parseColor("#101722")
    val panelStroke = Color.parseColor("#243041")
    val rowNormal = Color.parseColor("#121A27")
    val rowSelected = Color.parseColor("#1E2A3D")
    val textPrimary = Color.parseColor("#EAF1FF")
    val textSecondary = Color.parseColor("#9AA8BC")
    val accent = Color.parseColor("#4EA1FF")

    val apps = pm.getInstalledApplications(0)
        .filter { app ->
            app.packageName != context.packageName &&
            app.enabled &&
            (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 &&
            app.loadLabel(pm).toString().trim().isNotEmpty()
        }
        .sortedBy { it.loadLabel(pm).toString().lowercase() }

    val selected = getSavedGamePackagesStorage(context)

    val listView = ListView(context).apply {
        divider = null
        dividerHeight = 0
        setPadding(0, 0, 0, 0)
        clipToPadding = false
        isVerticalScrollBarEnabled = true
        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        background = null
    }

    val adapter = object : BaseAdapter() {
        override fun getCount() = apps.size
        override fun getItem(position: Int) = apps[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val app = apps[position]
            val isSelected = selected.contains(app.packageName)

            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(gpDp(context, 14), gpDp(context, 10), gpDp(context, 14), gpDp(context, 10))
                background = gpRoundedStroke(
                    if (isSelected) rowSelected else rowNormal,
                    panelStroke,
                    16,
                    context
                )

                val lp = AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                layoutParams = lp
            }

            val check = MaterialCheckBox(context).apply {
                isChecked = isSelected
                isClickable = false
                isFocusable = false
                buttonTintList = android.content.res.ColorStateList.valueOf(accent)
            }

            val icon = ImageView(context).apply {
                val size = gpDp(context, 40)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginStart = gpDp(context, 8)
                }
                setImageDrawable(app.loadIcon(pm))
            }

            val textWrap = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginStart = gpDp(context, 12)
                }
            }

            val label = TextView(context).apply {
                text = app.loadLabel(pm)
                textSize = 15f
                setTextColor(textPrimary)
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            val pkg = TextView(context).apply {
                text = app.packageName
                textSize = 11f
                setTextColor(textSecondary)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, gpDp(context, 2), 0, 0)
            }

            textWrap.addView(label)
            textWrap.addView(pkg)

            row.addView(check)
            row.addView(icon)
            row.addView(textWrap)

            row.setOnClickListener {
                if (selected.contains(app.packageName)) {
                    selected.remove(app.packageName)
                } else {
                    selected.add(app.packageName)
                }
                notifyDataSetChanged()
            }

            return row
        }
    }

    listView.adapter = adapter

    var dialogRef: AlertDialog? = null

    val title = TextView(context).apply {
        text = "Choose Games / Apps"
        textSize = 18f
        setTextColor(textPrimary)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(gpDp(context, 4), gpDp(context, 2), gpDp(context, 4), gpDp(context, 12))
    }

    val subtitle = TextView(context).apply {
        text = "Pick which launchable apps should trigger Game Mode"
        textSize = 12f
        setTextColor(textSecondary)
        setPadding(gpDp(context, 4), 0, gpDp(context, 4), gpDp(context, 12))
    }

    val saveBtn = MaterialButton(context).apply {
        text = "Save"
        textSize = 13f
        isAllCaps = false
        setTextColor(textPrimary)

        backgroundTintList =
            ColorStateList.valueOf(accent)
        rippleColor =
            ColorStateList.valueOf(Color.parseColor("#33445A"))
        cornerRadius = gpDp(context, 14)

        insetTop = 0
        insetBottom = 0
        minHeight = gpDp(context, 48)
        setPadding(
            gpDp(context, 18),
            gpDp(context, 10),
            gpDp(context, 18),
            gpDp(context, 10)
        )
        setOnClickListener {
            setSavedGamePackagesStorage(context, selected)
            onSave(selected)
            Toast.makeText(
                context,
                "Saved ${selected.size} apps",
                Toast.LENGTH_SHORT
            ).show()
            dialogRef?.dismiss()
        }
    }

    val cancelBtn = MaterialButton(
        context,
        null,
        com.google.android.material.R.attr.materialButtonOutlinedStyle
    ).apply {
        text = "Cancel"
        textSize = 13f
        isAllCaps = false
        setTextColor(textPrimary)

        backgroundTintList =
            ColorStateList.valueOf(Color.TRANSPARENT)
        strokeWidth = gpDp(context, 1)
        strokeColor =
            ColorStateList.valueOf(panelStroke)
        rippleColor =
            ColorStateList.valueOf(Color.parseColor("#33445A"))
        cornerRadius = gpDp(context, 14)

        insetTop = 0
        insetBottom = 0
        minHeight = gpDp(context, 48)
        setPadding(
            gpDp(context, 18),
            gpDp(context, 10),
            gpDp(context, 18),
            gpDp(context, 10)
        )
        setOnClickListener { dialogRef?.dismiss() }
    }

    val buttonRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.END
        setPadding(0, gpDp(context, 14), 0, 0)
        addView(cancelBtn)
        addView(TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(gpDp(context, 10), 1)
        })
        addView(saveBtn)
    }

    val listHolder = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = gpRoundedStroke(rowNormal, panelStroke, 18, context)
        setPadding(gpDp(context, 8), gpDp(context, 8), gpDp(context, 8), gpDp(context, 8))
        addView(
            listView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                gpDp(context, 420)
            )
        )
    }

    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(gpDp(context, 18), gpDp(context, 18), gpDp(context, 18), gpDp(context, 18))
        background = gpRoundedStroke(panelColor, panelStroke, 22, context)
        addView(title)
        addView(subtitle)
        addView(listHolder)
        addView(buttonRow)
    }

    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(gpDp(context, 12), gpDp(context, 12), gpDp(context, 12), gpDp(context, 12))
        setBackgroundColor(bgColor)
        addView(container)
    }

    val dialog = MaterialAlertDialogBuilder(context)
        .setView(root)
        .setCancelable(true)
        .create()

    dialogRef = dialog
    dialog.show()

    dialog.window?.setBackgroundDrawable(
        android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
    )
}
