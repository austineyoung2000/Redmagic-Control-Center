package com.elitedarkkaiser.redmagic

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

internal object MagicKeyAppPickerDialog {
    data class Deps(
        val textPrimary: Int,
        val textSecondary: Int,
        val panelColor: Int,
        val borderColor: Int,
        val typeface: Typeface?,
        val dp: (Int) -> Int,
        val roundedBg: (Int, Int, Int) -> Drawable,
        val roundedFill: (Int, Int) -> Drawable,
        val space: (Int) -> View
    )

    data class MagicKeyAppItem(
        val pkg: String,
        val label: String,
        val launchable: Boolean,
        val icon: Drawable?
    )

    fun show(
        activity: MainActivity,
        targetButton: Button,
        statusLabel: TextView?,
        applyLaunchAppMagicKeyMode: (String, String, TextView, Button) -> Unit,
        deps: Deps
    ) {
        val packageManager = activity.packageManager

        val allApps = packageManager.getInstalledApplications(0)
            .map { appInfo ->
                val pkg = appInfo.packageName
                val label = try {
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (_: Throwable) {
                    pkg
                }
                val icon = try {
                    packageManager.getApplicationIcon(appInfo)
                } catch (_: Throwable) {
                    null
                }
                MagicKeyAppItem(
                    pkg = pkg,
                    label = label,
                    launchable = packageManager.getLaunchIntentForPackage(pkg) != null,
                    icon = icon
                )
            }
            .sortedWith(
                compareBy<MagicKeyAppItem> { it.label.lowercase() }
                    .thenBy { it.pkg.lowercase() }
            )

        if (allApps.isEmpty()) {
            Toast.makeText(activity, "No installed apps found", Toast.LENGTH_SHORT).show()
            return
        }

        val titleView = TextView(activity).apply {
            text = "Choose Magic Key app"
            textSize = 20f
            setTextColor(deps.textPrimary)
            setTypeface(deps.typeface, Typeface.BOLD)
            setPadding(0, 0, 0, deps.dp(12))
        }

        val subtitleView = TextView(activity).apply {
            text = "Search by app name or package name. Lists user and system apps."
            textSize = 12f
            setTextColor(deps.textSecondary)
            setPadding(0, 0, 0, deps.dp(12))
        }

        val searchLayout = TextInputLayout(
            activity,
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = "Search apps or package names"
            boxBackgroundMode =
                TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = Color.parseColor("#121A27")
            boxStrokeColor = deps.borderColor
            boxStrokeWidth = deps.dp(1)
            boxStrokeWidthFocused = deps.dp(2)
            defaultHintTextColor =
                ColorStateList.valueOf(deps.textSecondary)
            setBoxCornerRadii(
                deps.dp(18).toFloat(),
                deps.dp(18).toFloat(),
                deps.dp(18).toFloat(),
                deps.dp(18).toFloat()
            )
        }

        val searchInput =
            TextInputEditText(searchLayout.context).apply {
                setTextColor(deps.textPrimary)
                setHintTextColor(deps.textSecondary)
                textSize = 14f
                isSingleLine = true
                background = null
                setPadding(
                    deps.dp(16),
                    deps.dp(12),
                    deps.dp(16),
                    deps.dp(12)
                )
            }

        searchLayout.addView(
            searchInput,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val listView = android.widget.ListView(activity).apply {
            divider = ColorDrawable(Color.parseColor("#263246"))
            dividerHeight = deps.dp(1)
            setBackgroundColor(Color.TRANSPARENT)
            isVerticalScrollBarEnabled = true
        }

        val filteredApps = ArrayList(allApps)

        val adapter = object : android.widget.BaseAdapter() {
            override fun getCount(): Int = filteredApps.size
            override fun getItem(position: Int): Any = filteredApps[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val item = filteredApps[position]

                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(deps.dp(14), deps.dp(12), deps.dp(14), deps.dp(12))
                    setBackgroundColor(Color.TRANSPARENT)
                }

                val iconView = ImageView(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(deps.dp(40), deps.dp(40))
                    setImageDrawable(item.icon)
                }

                val textWrap = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(deps.dp(12), 0, 0, 0)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                val labelView = TextView(activity).apply {
                    text = item.label
                    textSize = 14f
                    setTextColor(deps.textPrimary)
                    setTypeface(deps.typeface, Typeface.BOLD)
                }

                val pkgView = TextView(activity).apply {
                    text = if (item.launchable) item.pkg else "${item.pkg}  •  No launcher activity"
                    textSize = 11f
                    setTextColor(deps.textSecondary)
                    setLineSpacing(0f, 1.1f)
                }

                textWrap.addView(labelView)
                textWrap.addView(pkgView)

                row.addView(iconView)
                row.addView(textWrap)

                return row
            }
        }

        fun applyFilter(query: String) {
            val q = query.trim().lowercase()
            filteredApps.clear()
            if (q.isEmpty()) {
                filteredApps.addAll(allApps)
            } else {
                filteredApps.addAll(
                    allApps.filter {
                        it.label.lowercase().contains(q) || it.pkg.lowercase().contains(q)
                    }
                )
            }
            adapter.notifyDataSetChanged()
        }

        listView.adapter = adapter

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        val cancelBtn = MaterialButton(
            activity,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "Cancel"
            textSize = 13f
            isAllCaps = false
            setTextColor(deps.textPrimary)

            backgroundTintList =
                ColorStateList.valueOf(Color.TRANSPARENT)
            strokeWidth = deps.dp(1)
            strokeColor =
                ColorStateList.valueOf(deps.borderColor)
            rippleColor =
                ColorStateList.valueOf(Color.parseColor("#33445A"))
            cornerRadius = deps.dp(14)

            insetTop = 0
            insetBottom = 0
            minHeight = deps.dp(48)
            setPadding(
                deps.dp(18),
                deps.dp(10),
                deps.dp(18),
                deps.dp(10)
            )
        }

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(deps.dp(22), deps.dp(18), deps.dp(22), deps.dp(12))
            background = deps.roundedBg(deps.panelColor, deps.borderColor, 22)
            addView(titleView)
            addView(subtitleView)
            addView(searchLayout)
            addView(deps.space(deps.dp(12)))
            addView(
                listView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    deps.dp(420)
                )
            )
            addView(deps.space(deps.dp(12)))
            addView(LinearLayout(activity).apply {
                gravity = Gravity.END
                addView(cancelBtn)
            })
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(container)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        listView.setOnItemClickListener { _, _, which, _ ->
            val item = filteredApps[which]
            val status = statusLabel ?: return@setOnItemClickListener

            applyLaunchAppMagicKeyMode(
                item.pkg,
                item.label,
                status,
                targetButton
            )

            if (!item.launchable) {
                Toast.makeText(
                    activity,
                    "${item.label} saved, but it may not open because it has no launcher activity",
                    Toast.LENGTH_LONG
                ).show()
            }

            dialog.dismiss()
        }

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.65f)
        }
    }
}
