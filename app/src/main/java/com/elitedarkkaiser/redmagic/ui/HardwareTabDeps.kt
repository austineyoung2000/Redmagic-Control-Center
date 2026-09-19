package com.elitedarkkaiser.redmagic.ui

import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.elitedarkkaiser.redmagic.MasterProfile

data class HardwareTabDeps(
    val scrollTabContainer: () -> LinearLayout,
    val sectionPanel: () -> LinearLayout,
    val sectionHeader: (String, String) -> LinearLayout,
    val bodyText: (String) -> TextView,
    val subtleLabel: (String) -> TextView,
    val actionButton: (String, Boolean, () -> Unit) -> Button,
    val singleRow: (Button) -> LinearLayout,
    val row: (Button, Button) -> LinearLayout,
    val space: (Int) -> TextView,
    val dp: (Int) -> Int,

    val showTriggerSetupDialog: () -> Unit,
    val enableTriggersAndService: ((Boolean) -> Unit) -> Unit,
    val disableTriggersAndService: ((Boolean) -> Unit) -> Unit,

    val loadMasterProfiles: () -> List<MasterProfile>,
    val saveMasterProfile:
        (String, (Boolean) -> Unit) -> Unit,
    val applyMasterProfile: (MasterProfile) -> Unit,
    val deleteMasterProfile: (String) -> Unit,
    val exportMasterBackup: () -> Unit,
    val importMasterBackup: () -> Unit
)
