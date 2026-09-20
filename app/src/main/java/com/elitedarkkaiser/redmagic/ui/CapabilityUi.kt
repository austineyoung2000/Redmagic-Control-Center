package com.elitedarkkaiser.redmagic.ui

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

internal object CapabilityUi {
    fun disableInteractions(view: View) {
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                disableInteractions(view.getChildAt(index))
            }
        }

        if (
            view is Button ||
            view is MaterialCheckBox ||
            view is MaterialSwitch ||
            view is Slider ||
            view.hasOnClickListeners()
        ) {
            view.isEnabled = false
            view.alpha = 0.5f
        }
    }
}
