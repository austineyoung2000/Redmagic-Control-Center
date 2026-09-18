package com.elitedarkkaiser.redmagic.hardware

import com.elitedarkkaiser.redmagic.HardwareController

object TriggerController {
    fun enable(): Boolean {
        return HardwareController.enableTriggers()
    }

    fun disable(): Boolean {
        return HardwareController.disableTriggers()
    }

    fun injectTap(x: Int, y: Int): Boolean {
        return HardwareController.injectTap(x, y)
    }

}
