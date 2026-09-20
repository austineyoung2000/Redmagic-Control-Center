package com.elitedarkkaiser.redmagic.ui

import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.elitedarkkaiser.redmagic.DeviceCapabilities

data class ControlsTabDeps(
    val scrollTabContainer: () -> LinearLayout,
    val sectionPanel: () -> LinearLayout,
    val sectionHeader: (String, String) -> LinearLayout,
    val bodyText: (String) -> TextView,
    val subtleLabel: (String) -> TextView,
    val actionButton: (String, Boolean, () -> Unit) -> Button,
    val smallActionButton: (String, Boolean, () -> Unit) -> Button,
    val singleRow: (Button) -> LinearLayout,
    val row: (Button, Button) -> LinearLayout,
    val flowRow: (Array<out android.view.View>) -> LinearLayout,
    val space: (Int) -> TextView,
    val spacer: (Int) -> TextView,
    val dp: (Int) -> Int,
    val runBackground: (() -> Unit) -> Boolean,
    val capabilities: DeviceCapabilities,

    val refreshStatus: () -> Unit,
    val readMagicKeyModeLabel: () -> String,
    val applyStockMagicKeyMode: (String, () -> Boolean, TextView, Button?) -> Unit,
    val disableMagicKeyMode: (TextView, Button?) -> Unit,
    val resolveMagicKeyAppLabel: (String?) -> String,
    val savedMagicKeyAppPackage: () -> String?,
    val showMagicKeyAppPicker: (Button) -> Unit
)
